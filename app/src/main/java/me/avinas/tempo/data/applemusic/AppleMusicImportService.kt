package me.avinas.tempo.data.applemusic

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import me.avinas.tempo.data.local.AppDatabase
import me.avinas.tempo.data.local.DeviceTimeZone
import me.avinas.tempo.data.local.SourceAuthority
import me.avinas.tempo.data.local.dao.ListeningEventDao
import me.avinas.tempo.data.local.entities.ListeningEvent
import me.avinas.tempo.data.preferences.TrackingRulesPreferences
import me.avinas.tempo.data.repository.ArtistLinkingService
import me.avinas.tempo.data.repository.StatsRepository
import me.avinas.tempo.data.repository.TrackResolver
import java.nio.file.Files
import java.io.IOException
import javax.inject.Inject

class AppleMusicImportService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val trackResolver: TrackResolver,
    private val artistLinkingService: ArtistLinkingService,
    private val listeningEventDao: ListeningEventDao,
    private val statsRepository: StatsRepository,
) {
    data class Summary(
        val imported: Int = 0,
        val duplicates: Int = 0,
        val shortPlays: Int = 0,
        val missingArtist: Int = 0,
        val invalid: Int = 0,
    )

    enum class Problem { MISSING_ACTIVITY, UNSUPPORTED_COLUMNS, TOO_LARGE, UNREADABLE }
    class ImportException(val problem: Problem) : IOException()

    suspend fun importHistory(uris: List<Uri>, onProgress: (Summary) -> Unit): Summary =
        withContext(Dispatchers.IO) {
            val directory = Files.createTempDirectory(context.cacheDir.toPath(), "apple-music-").toFile()

            val checkActive = { coroutineContext.ensureActive() }
            var summary = Summary()
            var attemptedWrite = false
            val pending = ArrayList<Play>(BATCH_SIZE)

            suspend fun flush() {
                if (pending.isEmpty()) return
                attemptedWrite = true
                val result = database.withTransaction {
                    val trackIds = mutableMapOf<TrackResolver.Query, Long>()
                    val events = pending.map { play ->
                        val trackId = trackIds[play.track] ?: trackResolver.resolve(play.track).let { resolved ->
                            if (resolved.track.primaryArtistId == null) {
                                artistLinkingService.linkUnlinkedTrack(resolved.trackId)
                            }
                            trackIds[play.track] = resolved.trackId
                            resolved.trackId
                        }
                        play.toEvent(trackId)
                    }
                    listeningEventDao.insertAllBatchedWithDedup(events)
                }
                summary = summary.copy(
                    imported = summary.imported + result.newlyAdded,
                    duplicates = summary.duplicates + result.skipped,
                )
                pending.clear()
                onProgress(summary)
            }

            try {
                val files = AppleMusicImportFiles(directory, checkActive)
                uris.distinct().forEach { files.add(context.contentResolver, it) }
                if (files.activity.isEmpty()) throw ImportException(Problem.MISSING_ACTIVITY)

                // Validate every selected play log before writing the first batch.
                files.activity.forEach { file ->
                    file.bufferedReader().use { reader ->
                        AppleMusicCsv(reader).use { csv ->
                            if (!csv.isPlayActivity()) throw ImportException(Problem.UNSUPPORTED_COLUMNS)
                        }
                    }
                }

                val artists = ArtistIndex()
                // Learn both the older logs' explicit artist credits and the companion files.
                // Conflicting credits disable title-only fallback for that title.
                (files.activity + files.metadata).forEach { file ->
                    file.bufferedReader().use { reader ->
                        AppleMusicCsv(reader).use { csv ->
                            while (true) {
                                checkActive()
                                val row = csv.next() ?: break
                                artists.add(row)
                            }
                        }
                    }
                }

                val minimumPlay = TrackingRulesPreferences(context).minimumPlayDurationMs
                var rowsRead = 0
                for (file in files.activity) {
                    file.bufferedReader().use { reader ->
                        AppleMusicCsv(reader).use { csv ->
                            while (true) {
                                checkActive()
                                val row = csv.next() ?: break
                                if (++rowsRead % 1_000 == 0) onProgress(summary)
                                if (!row.isPlayback()) continue

                                val duration = row.playedMs()
                                val start = duration?.let { row.startMs(it) }
                                val title = row.title()
                                if (duration == null || start == null || start < EARLIEST_TIMESTAMP ||
                                    start > System.currentTimeMillis() || title.isBlank()
                                ) {
                                    summary = summary.copy(invalid = summary.invalid + 1)
                                    continue
                                }
                                if (duration == 0L || duration < minimumPlay) {
                                    summary = summary.copy(shortPlays = summary.shortPlays + 1)
                                    continue
                                }
                                val artist = artists.resolve(row)
                                if (artist.isNullOrBlank()) {
                                    summary = summary.copy(missingArtist = summary.missingArtist + 1)
                                    continue
                                }

                                pending += Play(
                                    track = TrackResolver.Query(
                                        title = title,
                                        artist = artist,
                                        album = row.album().ifBlank { null },
                                        duration = row.trackMs(),
                                    ),
                                    start = start,
                                    duration = duration,
                                    end = row.endMs()?.takeIf { it >= start },
                                    offsetMinutes = row.offsetMinutes(),
                                    endReason = row.get("End Reason Type"),
                                )
                                if (pending.size == BATCH_SIZE) flush()
                            }
                        }
                    }
                }
                flush()
                summary
            } catch (_: AppleMusicImportFiles.TooLarge) {
                throw ImportException(Problem.TOO_LARGE)
            } finally {
                directory.deleteRecursively()
                if (attemptedWrite) statsRepository.invalidateCache()
            }
        }

    private class ArtistIndex {
        private val byTitle = mutableMapOf<String, String>()
        private val byAlbumAndTitle = mutableMapOf<Pair<String, String>, String>()

        fun add(row: AppleMusicCsv.Row) {
            val artist = usableArtist(row.artist()) ?: return
            val title = row.title().lowercase()
            if (title.isBlank() || title.length > MAX_NAME_LENGTH || artist.length > MAX_NAME_LENGTH) return
            remember(byTitle, title, artist)
            val album = row.album().lowercase()
            if (album.isNotBlank() && album.length <= MAX_NAME_LENGTH) remember(byAlbumAndTitle, album to title, artist)
        }

        fun resolve(row: AppleMusicCsv.Row): String? {
            usableArtist(row.artist())?.let { return it }
            val title = row.title().lowercase()
            val albumArtist = byAlbumAndTitle[row.album().lowercase() to title]
            return (albumArtist ?: byTitle[title])?.takeIf { it.isNotBlank() }
        }

        private fun usableArtist(value: String): String? =
            value.takeIf { it.isNotBlank() && !it.equals("Unknown Artist", ignoreCase = true) }

        private fun <K> remember(index: MutableMap<K, String>, key: K, artist: String) {
            val previous = index[key]
            if (previous != null && !previous.equals(artist, ignoreCase = true)) {
                index[key] = ""
            } else if (previous == null && index.size < MAX_ARTIST_KEYS) {
                index[key] = artist
            }
        }
    }

    private data class Play(
        val track: TrackResolver.Query,
        val start: Long,
        val duration: Long,
        val end: Long?,
        val offsetMinutes: Int?,
        val endReason: String,
    ) {
        fun toEvent(trackId: Long): ListeningEvent {
            val completion = when {
                track.duration != null -> (duration * 100 / track.duration).toInt().coerceIn(1, 100)
                endReason.equals("NATURAL_END_OF_TRACK", ignoreCase = true) -> 100
                else -> 0
            }
            return ListeningEvent(
                track_id = trackId,
                timestamp = start,
                playDuration = duration,
                completionPercentage = completion,
                source = SourceAuthority.APPLE_MUSIC_EXPORT,
                wasSkipped = endReason.startsWith("TRACK_SKIPPED", ignoreCase = true) ||
                    completion in 1..29,
                endTimestamp = end ?: (start + duration),
                tzOffsetMinutes = offsetMinutes ?: DeviceTimeZone.offsetMinutesAt(start),
            )
        }
    }

    private companion object {
        const val BATCH_SIZE = 250
        const val MAX_NAME_LENGTH = 512
        const val MAX_ARTIST_KEYS = 100_000
        const val EARLIEST_TIMESTAMP = 1_420_070_400_000L // 2015-01-01 UTC
    }
}
