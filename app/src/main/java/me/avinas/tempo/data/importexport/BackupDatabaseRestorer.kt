package me.avinas.tempo.data.importexport

import android.util.Log
import androidx.room.withTransaction
import me.avinas.tempo.data.local.AppDatabase
import me.avinas.tempo.data.local.ArchiveTimestampCodec
import me.avinas.tempo.data.local.ReconciliationTrackScope
import me.avinas.tempo.data.local.dao.ListeningEventDao
import me.avinas.tempo.data.local.entities.Album
import me.avinas.tempo.data.local.entities.Artist
import me.avinas.tempo.data.local.entities.EnrichedMetadata
import me.avinas.tempo.data.local.entities.ListeningEvent
import me.avinas.tempo.data.local.entities.ScrobbleArchive
import me.avinas.tempo.data.local.entities.Track

/**
 * Owns Room restore policy for a parsed Tempo backup.
 *
 * The caller handles archive parsing and post-commit side effects. All related
 * database writes remain under one transaction so a failed restore cannot expose
 * partially remapped entities.
 */
internal class BackupDatabaseRestorer(
    private val database: AppDatabase,
    private val onProgress: (ImportExportProgress) -> Unit,
) {
    data class Result(
        val artistsAdded: Int,
        val albumsAdded: Int,
        val tracksAdded: Int,
        val events: ListeningEventDao.InsertResult,
    )

    private data class EntityIds(
        val ids: MutableMap<Long, Long>,
        val added: Int,
    )

    suspend fun restore(
        data: TempoExportData,
        pathMapping: Map<String, String>,
        stagingStore: RestoreStagingStore,
        conflictStrategy: ImportConflictStrategy,
        artworkStaging: RestoreArtworkStaging,
    ): Result = database.withTransaction {
        onProgress(ImportExportProgress("Importing artists...", 15, 100))
        val artists = restoreArtists(data, pathMapping, conflictStrategy)

        onProgress(ImportExportProgress("Importing albums...", 30, 100))
        val albums = restoreAlbums(
            data = data,
            pathMapping = pathMapping,
            artistIds = artists.ids,
            conflictStrategy = conflictStrategy,
        )

        onProgress(ImportExportProgress("Importing tracks...", 45, 100))
        val tracks = restoreTracks(
            data = data,
            pathMapping = pathMapping,
            artistIds = artists.ids,
            conflictStrategy = conflictStrategy,
        )

        onProgress(ImportExportProgress("Importing track-artist links...", 55, 100))
        restoreTrackArtists(data, tracks.ids, artists.ids)

        onProgress(ImportExportProgress("Importing listening history...", 65, 100))
        val events = replayStagedEvents(
            stagingStore = stagingStore,
            trackIdMap = tracks.ids,
            conflictStrategy = conflictStrategy,
        )
        Log.i(
            TAG,
            "Restored listening events: ${events.newlyAdded} new, " +
                "${events.replaced} replaced, ${events.skipped} skipped",
        )

        onProgress(ImportExportProgress("Importing metadata...", 80, 100))
        restoreMetadata(data, pathMapping, tracks.ids, conflictStrategy)

        onProgress(ImportExportProgress("Importing preferences...", 90, 100))
        restorePreferencesAndGamification(data, conflictStrategy)
        restoreAliasesAndContentMarks(data, artists.ids, tracks.ids, conflictStrategy)

        val archiveStats = replayStagedArchive(stagingStore, conflictStrategy)
        Log.i(
            TAG,
            "Imported scrobble archive: ${archiveStats.first} new, " +
                "${archiveStats.second} merged/skipped",
        )
        restoreLastFmImportMetadata(data, conflictStrategy)

        // Keep files before commit can expose their paths. If Room's return is
        // cancelled after commit, cleanup must not remove artwork now referenced
        // by committed database rows.
        artworkStaging.retainForCommit()

        Result(
            artistsAdded = artists.added,
            albumsAdded = albums.added,
            tracksAdded = tracks.added,
            events = events,
        )
    }

    private suspend fun restoreArtists(
        data: TempoExportData,
        pathMapping: Map<String, String>,
        conflictStrategy: ImportConflictStrategy,
    ): EntityIds {
        val ids = mutableMapOf<Long, Long>()
        var added = 0
        for (artist in data.artists) {
            val remapped = remapImagePath(artist, pathMapping)
            val existing = database.artistDao().getArtistByNormalizedName(remapped.normalizedName)
            if (existing != null) {
                if (conflictStrategy == ImportConflictStrategy.REPLACE) {
                    val safe = remapped.copy(
                        musicbrainzId = remapped.musicbrainzId?.let { mbid ->
                            val holder = database.artistDao().getArtistByMusicBrainzId(mbid)
                            if (holder == null || holder.id == existing.id) mbid else null
                        },
                    )
                    database.artistDao().update(safe.copy(id = existing.id))
                }
                ids[artist.id] = existing.id
                continue
            }

            val newId = database.artistDao().insert(remapped.copy(id = 0))
            if (newId > 0) {
                ids[artist.id] = newId
                added++
            } else {
                database.artistDao().getArtistByNormalizedName(remapped.normalizedName)
                    ?.let { ids[artist.id] = it.id }
            }
        }
        return EntityIds(ids, added)
    }

    private suspend fun restoreAlbums(
        data: TempoExportData,
        pathMapping: Map<String, String>,
        artistIds: Map<Long, Long>,
        conflictStrategy: ImportConflictStrategy,
    ): EntityIds {
        val ids = mutableMapOf<Long, Long>()
        var added = 0
        val artistNameById = LinkedHashMap<Long, String>(data.artists.size)
        data.artists.forEach { artistNameById.putIfAbsent(it.id, it.name) }

        for (album in data.albums) {
            val newArtistId = artistIds[album.artistId] ?: continue
            val artistName = artistNameById[album.artistId]
            if (artistName == null) {
                Log.w(TAG, "Skipping album '${album.title}': artist ID ${album.artistId} not found in export")
                continue
            }

            val remapped = remapImagePath(album, pathMapping).copy(artistId = newArtistId)
            val existing = database.albumDao().getAlbumByTitleAndArtist(album.title, artistName)
            if (existing != null) {
                if (conflictStrategy == ImportConflictStrategy.REPLACE) {
                    val safe = remapped.copy(
                        musicbrainzId = remapped.musicbrainzId?.let { mbid ->
                            val holder = database.albumDao().getAlbumByMusicBrainzId(mbid)
                            if (holder == null || holder.id == existing.id) mbid else null
                        },
                    )
                    database.albumDao().update(safe.copy(id = existing.id))
                }
                ids[album.id] = existing.id
                continue
            }

            val newId = database.albumDao().insert(remapped.copy(id = 0))
            if (newId > 0) {
                ids[album.id] = newId
                added++
            } else {
                database.albumDao().getAlbumByTitleAndArtist(album.title, artistName)
                    ?.let { ids[album.id] = it.id }
            }
        }
        return EntityIds(ids, added)
    }

    private suspend fun restoreTracks(
        data: TempoExportData,
        pathMapping: Map<String, String>,
        artistIds: Map<Long, Long>,
        conflictStrategy: ImportConflictStrategy,
    ): EntityIds {
        val ids = mutableMapOf<Long, Long>()
        var added = 0
        for (track in data.tracks) {
            val remapped = remapImagePath(track, pathMapping).copy(
                primaryArtistId = track.primaryArtistId?.let(artistIds::get),
            )
            val existing = track.spotifyId?.takeIf(String::isNotBlank)
                ?.let { database.trackDao().findBySpotifyId(it) }
                ?: track.musicbrainzId?.takeIf(String::isNotBlank)
                    ?.let { database.trackDao().findByMusicBrainzId(it) }
                ?: database.trackDao().findByTitleAndArtist(track.title, track.artist)

            if (existing != null) {
                if (conflictStrategy == ImportConflictStrategy.REPLACE) {
                    val safe = remapped.copy(
                        spotifyId = remapped.spotifyId?.let { sid ->
                            val holder = database.trackDao().findBySpotifyId(sid)
                            if (holder == null || holder.id == existing.id) sid else null
                        },
                        youtubeId = remapped.youtubeId?.let { yid ->
                            val holder = database.trackDao().findByYoutubeId(yid)
                            if (holder == null || holder.id == existing.id) yid else null
                        },
                    )
                    database.trackDao().update(safe.copy(id = existing.id))
                } else {
                    val backfilled = existing.copy(
                        spotifyId = existing.spotifyId ?: remapped.spotifyId?.let { sid ->
                            if (database.trackDao().findBySpotifyId(sid) == null) sid else null
                        },
                        musicbrainzId = existing.musicbrainzId ?: remapped.musicbrainzId,
                        youtubeId = existing.youtubeId ?: remapped.youtubeId?.let { yid ->
                            if (database.trackDao().findByYoutubeId(yid) == null) yid else null
                        },
                        album = existing.album ?: remapped.album,
                        albumArtUrl = existing.albumArtUrl ?: remapped.albumArtUrl,
                        duration = existing.duration ?: remapped.duration,
                    )
                    if (backfilled != existing) database.trackDao().update(backfilled)
                }
                ids[track.id] = existing.id
                continue
            }

            val newId = database.trackDao().insert(remapped.copy(id = 0))
            if (newId > 0) {
                ids[track.id] = newId
                added++
            } else {
                val recovered = remapped.spotifyId?.takeIf(String::isNotBlank)
                    ?.let { database.trackDao().findBySpotifyId(it) }
                    ?: remapped.youtubeId?.takeIf(String::isNotBlank)
                        ?.let { database.trackDao().findByYoutubeId(it) }
                    ?: database.trackDao().findByTitleAndArtist(remapped.title, remapped.artist)
                recovered?.let { ids[track.id] = it.id }
            }
        }
        return EntityIds(ids, added)
    }

    private suspend fun restoreTrackArtists(
        data: TempoExportData,
        trackIds: Map<Long, Long>,
        artistIds: Map<Long, Long>,
    ) {
        val remapped = data.trackArtists.mapNotNull { link ->
            val trackId = trackIds[link.trackId] ?: return@mapNotNull null
            val artistId = artistIds[link.artistId] ?: return@mapNotNull null
            link.copy(trackId = trackId, artistId = artistId)
        }
        database.trackArtistDao().insertAllBatched(remapped)
    }

    private suspend fun restoreMetadata(
        data: TempoExportData,
        pathMapping: Map<String, String>,
        trackIds: Map<Long, Long>,
        conflictStrategy: ImportConflictStrategy,
    ) {
        for (meta in data.enrichedMetadata) {
            val trackId = trackIds[meta.trackId] ?: continue
            val remapped = remapImagePath(meta, pathMapping).copy(trackId = trackId)
            val existing = database.enrichedMetadataDao().forTrackSync(trackId)
            if (existing == null) {
                database.enrichedMetadataDao().upsert(remapped.copy(id = 0))
            } else if (conflictStrategy == ImportConflictStrategy.REPLACE) {
                database.enrichedMetadataDao().update(remapped.copy(id = existing.id))
            }
        }

        // Track.albumArtUrl mirrors the final metadata state used by the UI.
        for (trackId in trackIds.values.toSet()) {
            val track = database.trackDao().getTrackById(trackId) ?: continue
            val metadata = database.enrichedMetadataDao().forTrackSync(trackId)
            val artwork = resolveRestoredTrackArtwork(
                albumArtSource = metadata?.albumArtSource,
                metadataArtUrl = metadata?.albumArtUrl,
                trackArtUrl = track.albumArtUrl,
            )
            if (artwork != track.albumArtUrl) {
                database.trackDao().updateAlbumArtUrl(trackId, artwork)
            }
        }
    }

    private suspend fun restorePreferencesAndGamification(
        data: TempoExportData,
        conflictStrategy: ImportConflictStrategy,
    ) {
        if (conflictStrategy == ImportConflictStrategy.REPLACE) {
            data.userPreferences?.let { prefs ->
                database.userPreferencesDao().upsert(
                    prefs.copy(
                        spotifyLinked = false,
                        spotifyApiOnlyMode = false,
                        spotifyImportCursor = null,
                        lastSpotifyImportTimestamp = null,
                    ),
                )
            }
        }

        data.userLevel?.let { level ->
            if (
                conflictStrategy == ImportConflictStrategy.REPLACE ||
                database.gamificationDao().getUserLevel() == null
            ) {
                database.gamificationDao().upsertUserLevel(level)
                Log.i(TAG, "Restored backup level: ${level.currentLevel} (${level.totalXp} XP)")
            }
        }

        if (data.badges.isNotEmpty()) {
            val incoming = if (conflictStrategy == ImportConflictStrategy.REPLACE) {
                data.badges
            } else {
                data.badges.filter { database.gamificationDao().getBadgeById(it.badgeId) == null }
            }
            if (incoming.isNotEmpty()) database.gamificationDao().upsertBadges(incoming)
            Log.i(TAG, "Restored ${incoming.size} new/replaced badges")
        }

        data.userKnownArtists.forEach { known ->
            database.userKnownArtistDao().insert(known.copy(id = 0))
        }
        if (data.userKnownArtists.isNotEmpty()) {
            Log.i(TAG, "Imported ${data.userKnownArtists.size} user-known artists")
        }

        if (data.dailyChallenges.isNotEmpty()) {
            val existingKeys = database.gamificationDao().getAllCompletedChallenges()
                .map { it.date to it.challengeId }
                .toSet()
            val incoming = data.dailyChallenges.filter { it.date to it.challengeId !in existingKeys }
            if (incoming.isNotEmpty()) {
                database.gamificationDao().upsertChallenges(incoming.map { it.copy(id = 0) })
            }
            val completed = incoming.count { it.isCompleted }
            Log.i(
                TAG,
                "Imported ${incoming.size} daily challenges ($completed completed, " +
                    "${data.dailyChallenges.size - incoming.size} skipped as duplicates)",
            )
        }
    }

    private suspend fun restoreAliasesAndContentMarks(
        data: TempoExportData,
        artistIds: Map<Long, Long>,
        trackIds: Map<Long, Long>,
        conflictStrategy: ImportConflictStrategy,
    ) {
        var artistAliasesAdded = 0
        for (alias in data.artistAliases) {
            val targetId = artistIds[alias.targetArtistId] ?: continue
            if (database.artistAliasDao().findAlias(alias.originalNameNormalized) == null) {
                database.artistAliasDao().insertAlias(alias.copy(id = 0, targetArtistId = targetId))
                artistAliasesAdded++
            }
        }
        Log.i(TAG, "Imported $artistAliasesAdded artist aliases")

        var trackAliasesAdded = 0
        for (alias in data.trackAliases) {
            val targetId = trackIds[alias.targetTrackId] ?: continue
            if (database.trackAliasDao().findAlias(alias.originalTitle, alias.originalArtist) == null) {
                database.trackAliasDao().insertAlias(alias.copy(id = 0, targetTrackId = targetId))
                trackAliasesAdded++
            }
        }
        Log.i(TAG, "Imported $trackAliasesAdded track aliases")

        var marksAdded = 0
        for (mark in data.manualContentMarks) {
            val trackId = trackIds[mark.targetTrackId] ?: continue
            val remapped = mark.copy(id = 0, targetTrackId = trackId)
            val existing = database.manualContentMarkDao().findMatchingMark(
                remapped.originalTitle,
                remapped.originalArtist,
            )
            if (existing == null) {
                database.manualContentMarkDao().insertMark(remapped)
                marksAdded++
            }
        }
        Log.i(TAG, "Imported $marksAdded manual content marks")

        if (data.appPreferences.isNotEmpty()) {
            if (conflictStrategy == ImportConflictStrategy.REPLACE) {
                data.appPreferences.forEach { database.appPreferenceDao().upsert(it) }
            } else {
                database.appPreferenceDao().insertAll(data.appPreferences)
            }
            Log.i(
                TAG,
                "Imported ${data.appPreferences.size} app preferences using $conflictStrategy",
            )
        }
    }

    private suspend fun restoreLastFmImportMetadata(
        data: TempoExportData,
        conflictStrategy: ImportConflictStrategy,
    ) {
        for (metadata in data.lastFmImportMetadata) {
            val incoming = metadata.copy(id = 0)
            val existing = database.lastFmImportMetadataDao()
                .getLatestForUsername(incoming.lastfmUsername)
            if (existing == null) {
                database.lastFmImportMetadataDao().insert(incoming)
            } else if (conflictStrategy == ImportConflictStrategy.REPLACE) {
                database.lastFmImportMetadataDao().update(incoming.copy(id = existing.id))
            }
        }
        if (data.lastFmImportMetadata.isNotEmpty()) {
            Log.i(
                TAG,
                "Imported ${data.lastFmImportMetadata.size} Last.fm import metadata records",
            )
        }
    }

    private suspend fun replayStagedEvents(
        stagingStore: RestoreStagingStore,
        trackIdMap: Map<Long, Long>,
        conflictStrategy: ImportConflictStrategy,
    ): ListeningEventDao.InsertResult {
        var inserted = 0
        var skipped = 0
        var replaced = 0
        var updated = 0
        var chunk = ArrayList<ListeningEvent>(EVENT_IMPORT_CHUNK)

        suspend fun flush() {
            if (chunk.isEmpty()) return
            val result = database.listeningEventDao().insertAllBatchedWithDedup(
                chunk,
                replaceExistingOnMatch = conflictStrategy == ImportConflictStrategy.REPLACE,
                trackScope = ReconciliationTrackScope.COMPATIBLE_ALIASES,
            )
            inserted += result.inserted
            skipped += result.skipped
            replaced += result.replaced
            updated += result.updated
            chunk = ArrayList(EVENT_IMPORT_CHUNK)
        }

        stagingStore.forEachEvent { event ->
            val trackId = trackIdMap[event.track_id] ?: return@forEachEvent
            chunk.add(
                event.copy(
                    id = 0,
                    track_id = trackId,
                    contentFingerprint = null,
                ),
            )
            if (chunk.size >= EVENT_IMPORT_CHUNK) flush()
        }
        flush()
        return ListeningEventDao.InsertResult(inserted, skipped, replaced, updated)
    }

    private suspend fun replayStagedArchive(
        stagingStore: RestoreStagingStore,
        conflictStrategy: ImportConflictStrategy,
    ): Pair<Int, Int> {
        var newRows = 0
        var mergedOrSkipped = 0
        var chunk = ArrayList<ScrobbleArchive>(ARCHIVE_IMPORT_CHUNK)

        suspend fun flush() {
            if (chunk.isEmpty()) return
            when (conflictStrategy) {
                ImportConflictStrategy.SKIP -> {
                    val results = database.scrobbleArchiveDao()
                        .insertAllIgnore(chunk.map { it.copy(id = 0) })
                    val inserted = results.count { it > 0 }
                    newRows += inserted
                    mergedOrSkipped += chunk.size - inserted
                }
                ImportConflictStrategy.REPLACE -> {
                    for (row in chunk) {
                        val (id, _) = database.scrobbleArchiveDao().upsertWithMerge(
                            row.copy(id = 0),
                            ArchiveTimestampCodec::decompress,
                            ArchiveTimestampCodec::compress,
                        )
                        if (id > 0) newRows++ else mergedOrSkipped++
                    }
                }
            }
            chunk = ArrayList(ARCHIVE_IMPORT_CHUNK)
        }

        stagingStore.forEachArchiveRow { row ->
            chunk.add(row)
            if (chunk.size >= ARCHIVE_IMPORT_CHUNK) flush()
        }
        flush()
        return newRows to mergedOrSkipped
    }

    private fun remapImagePath(track: Track, pathMapping: Map<String, String>): Track =
        track.copy(albumArtUrl = remapRestoredImageUrl(track.albumArtUrl, pathMapping))

    private fun remapImagePath(artist: Artist, pathMapping: Map<String, String>): Artist =
        artist.copy(imageUrl = remapRestoredImageUrl(artist.imageUrl, pathMapping))

    private fun remapImagePath(album: Album, pathMapping: Map<String, String>): Album =
        album.copy(artworkUrl = remapRestoredImageUrl(album.artworkUrl, pathMapping))

    private fun remapImagePath(
        metadata: EnrichedMetadata,
        pathMapping: Map<String, String>,
    ): EnrichedMetadata =
        metadata.copy(
            albumArtUrl = remapRestoredImageUrl(metadata.albumArtUrl, pathMapping),
            albumArtUrlSmall = remapRestoredImageUrl(metadata.albumArtUrlSmall, pathMapping),
            albumArtUrlLarge = remapRestoredImageUrl(metadata.albumArtUrlLarge, pathMapping),
            spotifyArtistImageUrl = remapRestoredImageUrl(metadata.spotifyArtistImageUrl, pathMapping),
            iTunesArtistImageUrl = remapRestoredImageUrl(metadata.iTunesArtistImageUrl, pathMapping),
            deezerArtistImageUrl = remapRestoredImageUrl(metadata.deezerArtistImageUrl, pathMapping),
            lastFmArtistImageUrl = remapRestoredImageUrl(metadata.lastFmArtistImageUrl, pathMapping),
        )

    private companion object {
        const val TAG = "BackupDatabaseRestore"
        const val EVENT_IMPORT_CHUNK = 5000
        const val ARCHIVE_IMPORT_CHUNK = 500
    }
}
