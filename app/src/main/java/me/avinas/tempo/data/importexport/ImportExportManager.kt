package me.avinas.tempo.data.importexport

import android.content.Context
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.room.withTransaction
import me.avinas.tempo.data.local.AppDatabase
import me.avinas.tempo.data.local.dao.ListeningEventDao
import me.avinas.tempo.data.local.dao.isLocalBackupArtwork
import me.avinas.tempo.data.local.entities.*
import me.avinas.tempo.data.profile.ProfileIdentityManager
import me.avinas.tempo.utils.ImageUrlHostAllowlist
import com.squareup.moshi.JsonReader
import me.avinas.tempo.worker.PostRestoreCacheWorker
import okio.buffer
import okio.sink
import okio.source
import me.avinas.tempo.data.local.ArchiveTimestampCodec
import java.io.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Import and export of Tempo's backup ZIP: `data.json` with every entity, plus an optional
 * `images/` folder of local album art (file:// URLs only). Import remaps IDs and restores
 * those images; hotlinked ones are pre-cached afterwards.
 */
@Singleton
class ImportExportManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val profileIdentityManager: ProfileIdentityManager,
    private val statsRepository: me.avinas.tempo.data.repository.StatsRepository,
    private val appearanceRepository: me.avinas.tempo.ui.appearance.AppearancePreferencesRepository,
) {
    companion object {
        private const val TAG = "ImportExportManager"
        private const val ALBUM_ART_DIR = "album_art"
        private const val IMAGES_DIR = "images/"
        private const val MAX_BUNDLED_IMAGE_BYTES = 10L * 1024L * 1024L
        private const val MAX_TOTAL_IMAGE_BYTES = 50L * 1024L * 1024L
        // ZIP entry sizes are attacker-controlled metadata. Cap the *decompressed*
        // JSON stream and the two staged row sets before they can exhaust disk.
        private const val MAX_DATA_JSON_BYTES = 1024L * 1024L * 1024L
        private const val MAX_IMPORTED_EVENTS = 5_000_000
        private const val MAX_IMPORTED_ARCHIVE_ROWS = 2_000_000

        // Chunk size for feeding streamed import rows into the dedup pipeline.
        private const val EVENT_IMPORT_CHUNK = 5000
        private const val ARCHIVE_IMPORT_CHUNK = 500
    }
    
    private val moshi = buildImportExportMoshi()
    private val codec = TempoExportJsonCodec(moshi)

    // ImportExportManager is a singleton and both export and import mutate shared
    // state (progress flow) while hammering the DB. A manual backup and a scheduled
    // worker backup must never interleave, so only one operation runs at a time.
    private val operationGate = ImportExportOperationGate()
    
    private val _progress = MutableStateFlow<ImportExportProgress?>(null)
    val progress: StateFlow<ImportExportProgress?> = _progress.asStateFlow()
    
    /**
     * Export all data to a ZIP at [uri].
     *
     * The archive is fully written and validated into a private cache file FIRST,
     * and only then copied to [uri], so a failed or interrupted export can never
     * truncate an existing backup file the user picked as the overwrite target.
     */
    suspend fun exportData(
        uri: Uri,
        includeLocalImages: Boolean = true
    ): ImportExportResult = withContext(Dispatchers.IO) {
        val operationLease = operationGate.tryAcquire()
        if (operationLease == null) {
            return@withContext ImportExportResult.Error(
                "Another backup/restore is already in progress. Please wait for it to finish."
            )
        }

        val stagingFile = File(context.cacheDir, "export_staging_${System.currentTimeMillis()}.tempo")
        try {
            val result = writeBackupArchive(stagingFile, includeLocalImages)
            if (result is ImportExportResult.Error) return@withContext result

            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    stagingFile.inputStream().use { it.copyTo(outputStream) }
                } ?: return@withContext ImportExportResult.Error("Could not open file for writing")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy staged backup to destination", e)
                return@withContext ImportExportResult.Error(
                    "Could not write backup file: ${e.message}",
                    e
                )
            }

            result
        } finally {
            stagingFile.delete()
            _progress.value = null
            // Clear shared progress before releasing the gate. Otherwise a new
            // operation can start and have its progress erased by this one.
            operationLease.release()
        }
    }

    /**
     * Export directly into a caller-owned local file (Google Drive upload path).
     * The target must be a fresh/throwaway cache file — unlike [exportData]
     * there is no pre-existing user content to protect, so no staging copy.
     */
    suspend fun exportToFile(
        target: File,
        includeLocalImages: Boolean = true
    ): ImportExportResult = withContext(Dispatchers.IO) {
        val operationLease = operationGate.tryAcquire()
        if (operationLease == null) {
            return@withContext ImportExportResult.Error(
                "Another backup/restore is already in progress. Please wait for it to finish."
            )
        }
        try {
            writeBackupArchive(target, includeLocalImages)
        } finally {
            _progress.value = null
            operationLease.release()
        }
    }

    /** Stages consistent database JSON first; image copying and ZIP compression hold no DB lock. */
    private suspend fun writeBackupArchive(
        target: File,
        includeLocalImages: Boolean
    ): ImportExportResult = withContext(Dispatchers.IO) {
        val snapshotFile = File(context.cacheDir, "export_snapshot_${java.util.UUID.randomUUID()}.json")
        try {
            _progress.value = ImportExportProgress("Collecting data...", 0, 100, true)
            // DataStore is a separate persistence system; capture identity once, outside Room.
            val profile = profileIdentityManager.getProfileIdentity()
            val appearance = appearanceRepository.exportBackup()
            val imageSources = linkedMapOf<String, File>()
            val result = BackupSnapshotWriter(database, codec).write(snapshotFile, profile) { shell ->
                val (localPaths, hotlinks) = collectImageUrls(
                    shell.tracks, shell.artists, shell.albums, shell.enrichedMetadata
                )
                val paths = buildSet {
                    if (includeLocalImages) addAll(localPaths)
                    profile.profileImagePath?.takeIf { it.startsWith("file://") }?.let(::add)
                }
                val manifest = linkedMapOf<String, String>()
                paths.forEachIndexed { index, path ->
                    val file = resolveExportableLocalImage(path) ?: return@forEachIndexed
                    // Keep archive names within the restore format's length/safety limits.
                    val name = "img_${index}_${file.name}".take(128)
                    if (sanitizeBundledImageName(name) != null) {
                        imageSources[name] = file
                        manifest[name] = path
                    }
                }
                shell.copy(localImageManifest = manifest, hotlinkedUrls = hotlinks, appearance = appearance)
            }

            _progress.value = ImportExportProgress("Creating backup...", 60, 100)
            target.outputStream().use { output ->
                ZipOutputStream(BufferedOutputStream(output)).use { zip ->
                    for ((name, file) in imageSources) {
                        currentCoroutineContext().ensureActive()
                        zip.putNextEntry(ZipEntry("$IMAGES_DIR$name"))
                        // A disappearing image aborts the backup, rather than publishing a
                        // manifest that claims a missing/partially copied image was included.
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                    currentCoroutineContext().ensureActive()
                    zip.putNextEntry(ZipEntry(TempoExportData.DATA_FILENAME))
                    snapshotFile.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
            currentCoroutineContext().ensureActive()
            _progress.value = ImportExportProgress("Validating archive...", 95, 100)
            validateBackupArchive(target)?.let { message ->
                target.delete()
                return@withContext ImportExportResult.Error(message)
            }
            _progress.value = ImportExportProgress("Export complete!", 100, 100)
            result.copy(appearanceStatus = BackupAppearanceStatus.INCLUDED)
        } catch (e: CancellationException) {
            target.delete()
            throw e
        } catch (e: Exception) {
            target.delete()
            Log.e(TAG, "Export failed", e)
            ImportExportResult.Error("Export failed: ${e::class.java.simpleName}: ${e.message}", e)
        } finally {
            snapshotFile.delete()
        }
    }

    suspend fun importData(
        uri: Uri,
        conflictStrategy: ImportConflictStrategy
    ): ImportExportResult = withContext(Dispatchers.IO) {
        val operationLease = operationGate.tryAcquire()
        if (operationLease == null) {
            return@withContext ImportExportResult.Error(
                "Another backup/restore is already in progress. Please wait for it to finish."
            )
        }

        // The two unbounded arrays (listening events, scrobble archive) are streamed
        // out of the ZIP into JSON-lines staging files during pass 1, then replayed
        // chunk by chunk inside the DB transaction. Neither table is ever held in
        // memory whole, so restores of huge libraries cannot OOM.
        val stagedEventsFile = File(context.cacheDir, "import_events.jsonl")
        val stagedArchiveFile = File(context.cacheDir, "import_archive.jsonl")
        stagedEventsFile.delete()
        stagedArchiveFile.delete()
        val artworkStaging = RestoreArtworkStaging(context.cacheDir, File(context.filesDir, ALBUM_ART_DIR))

        try {
            _progress.value = ImportExportProgress("Reading backup file...", 0, 100, true)
            
            // First pass: read data.json to get manifest
            var exportData: TempoExportData? = null
            var totalExtractedImageBytes = 0L
            var stagedEventCount = 0
            var stagedArchiveCount = 0
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                    var entry: ZipEntry? = zipIn.nextEntry
                    while (entry != null) {
                        when {
                            entry.name == TempoExportData.DATA_FILENAME -> {
                                // Stream-parse the document: bounded tables are collected,
                                // unbounded arrays are staged row by row.
                                val reader = JsonReader.of(LimitedEntryInputStream(zipIn, MAX_DATA_JSON_BYTES).source().buffer())
                                val eventAdapter = moshi.adapter(ListeningEvent::class.java)
                                val archiveAdapter = moshi.adapter(ScrobbleArchive::class.java)
                                val eventSink = stagedEventsFile.sink().buffer()
                                val archiveSink = stagedArchiveFile.sink().buffer()
                                try {
                                    exportData = codec.read(
                                        reader,
                                        TempoExportJsonCodec.StreamHandlers(
                                            onListeningEvent = { event ->
                                                if (stagedEventCount >= MAX_IMPORTED_EVENTS) {
                                                    throw SecurityException("Backup contains too many listening events")
                                                }
                                                eventAdapter.toJson(eventSink, event)
                                                eventSink.writeUtf8("\n")
                                                stagedEventCount++
                                            },
                                            onScrobbleArchiveRow = { row ->
                                                if (stagedArchiveCount >= MAX_IMPORTED_ARCHIVE_ROWS) {
                                                    throw SecurityException("Backup contains too many archive rows")
                                                }
                                                archiveAdapter.toJson(archiveSink, row)
                                                archiveSink.writeUtf8("\n")
                                                stagedArchiveCount++
                                            }
                                        )
                                    )
                                } finally {
                                    // flush only — close() would close the ZipInputStream
                                    eventSink.flush()
                                    archiveSink.flush()
                                }
                            }
                            entry.name.startsWith(IMAGES_DIR) && !entry.isDirectory -> {
                                // Extract only into this operation's private staging directory.
                                val bundledName = entry.name.removePrefix(IMAGES_DIR)
                                val safeBundledName = sanitizeBundledImageName(bundledName)
                                if (safeBundledName == null) {
                                    Log.w(TAG, "Skipping unsafe bundled image name in import")
                                } else {
                                    val remainingBudget = MAX_TOTAL_IMAGE_BYTES - totalExtractedImageBytes
                                    totalExtractedImageBytes += artworkStaging.stage(
                                        safeBundledName, zipIn,
                                        minOf(MAX_BUNDLED_IMAGE_BYTES, remainingBudget)
                                    )
                                }
                            }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            }
            
            val data = exportData ?: return@withContext ImportExportResult.Error("Invalid backup file: no data found")
            
            // Validate version
            if (data.version > TempoExportData.CURRENT_VERSION) {
                return@withContext ImportExportResult.Error(
                    "Backup is from a newer app version. Please update Tempo."
                )
            }
            
            // Archive parsing, limits and version validation have succeeded. Promote to a
            // unique namespace before DB references are written; rollback removes only our files.
            val extractedImages = artworkStaging.promote()
            Log.i(TAG, "Extracted ${extractedImages.size} images, staged $stagedEventCount events and $stagedArchiveCount archive rows")
            
            // Build path mapping: old file:// path -> new file:// path
            val pathMapping = data.localImageManifest.mapNotNull { (bundledName, originalPath) ->
                extractedImages[bundledName]?.let { newPath -> originalPath to newPath }
            }.toMap()
            
            _progress.value = ImportExportProgress("Importing artists...", 15, 100)
            
            // ID mappings for foreign keys (mutable maps modified inside the transaction)
            val artistIdMap = mutableMapOf<Long, Long>()
            val trackIdMap = mutableMapOf<Long, Long>()
            val albumIdMap = mutableMapOf<Long, Long>()
            
            // Counters collected inside the transaction
            var importedArtists = 0
            var importedAlbums = 0
            var importedTracks = 0
            var importedEvents = 0
            var replacedEvents = 0
            var skippedEvents = 0
            
            // Wrap all Room DB writes in a single transaction so partial failures are rolled back
            database.withTransaction {
            
            // Import Artists
            for (artist in data.artists) {
                val remappedArtist = remapImagePath(artist, pathMapping)
                val existingArtist = database.artistDao().getArtistByNormalizedName(remappedArtist.normalizedName)
                
                if (existingArtist != null) {
                    if (conflictStrategy == ImportConflictStrategy.REPLACE) {
                        // Guard the unique musicbrainz_id index: if the backup's MBID
                        // already belongs to a DIFFERENT artist row, applying it here
                        // would throw and abort the whole restore. Drop it instead.
                        val safeArtist = remappedArtist.copy(
                            musicbrainzId = remappedArtist.musicbrainzId?.let { mbid ->
                                val holder = database.artistDao().getArtistByMusicBrainzId(mbid)
                                if (holder == null || holder.id == existingArtist.id) mbid else null
                            }
                        )
                        database.artistDao().update(safeArtist.copy(id = existingArtist.id))
                    }
                    artistIdMap[artist.id] = existingArtist.id
                } else {
                    val newId = database.artistDao().insert(remappedArtist.copy(id = 0))
                    if (newId > 0) {
                        artistIdMap[artist.id] = newId
                        importedArtists++
                    } else {
                        // IGNORE'd insert (unique constraint raced) — recover the
                        // mapping via lookup so dependent rows are not orphaned.
                        database.artistDao().getArtistByNormalizedName(remappedArtist.normalizedName)
                            ?.let { artistIdMap[artist.id] = it.id }
                    }
                }
            }
            
            _progress.value = ImportExportProgress("Importing albums...", 30, 100)

            // Resolved once rather than per album. This lookup used to be
            // `data.artists.find { it.id == album.artistId }` *inside* the loop below, which is a
            // linear scan of every exported artist for every album — O(albums x artists), i.e.
            // ~4x10^8 comparisons for a 20k-album / 20k-artist library. The whole restore runs
            // inside a single `withTransaction`, so that CPU time was being spent while holding
            // the database writer, directly lengthening the window the user waits through.
            //
            // `putIfAbsent` keeps first-match-wins semantics, which is what the `find` it
            // replaces did, so a malformed export carrying duplicate artist ids behaves as before.
            val artistNameById = LinkedHashMap<Long, String>(data.artists.size)
            for (exportedArtist in data.artists) {
                artistNameById.putIfAbsent(exportedArtist.id, exportedArtist.name)
            }

            // Import Albums
            for (album in data.albums) {
                val newArtistId = artistIdMap[album.artistId] ?: continue
                val remappedAlbum = remapImagePath(album, pathMapping).copy(artistId = newArtistId)
                val artistName = artistNameById[album.artistId]
                if (artistName == null) {
                    Log.w(TAG, "Skipping album '${album.title}': artist ID ${album.artistId} not found in export")
                    continue
                }
                val existingAlbum = database.albumDao().getAlbumByTitleAndArtist(
                    album.title,
                    artistName
                )
                
                if (existingAlbum != null) {
                    if (conflictStrategy == ImportConflictStrategy.REPLACE) {
                        // Guard the unique musicbrainz_id index (see artist guard above).
                        val safeAlbum = remappedAlbum.copy(
                            musicbrainzId = remappedAlbum.musicbrainzId?.let { mbid ->
                                val holder = database.albumDao().getAlbumByMusicBrainzId(mbid)
                                if (holder == null || holder.id == existingAlbum.id) mbid else null
                            }
                        )
                        database.albumDao().update(safeAlbum.copy(id = existingAlbum.id))
                    }
                    albumIdMap[album.id] = existingAlbum.id
                } else {
                    val newId = database.albumDao().insert(remappedAlbum.copy(id = 0))
                    if (newId > 0) {
                        albumIdMap[album.id] = newId
                        importedAlbums++
                    } else {
                        // IGNORE'd insert — recover the mapping via lookup.
                        database.albumDao().getAlbumByTitleAndArtist(album.title, artistName)
                            ?.let { albumIdMap[album.id] = it.id }
                    }
                }
            }
            
            _progress.value = ImportExportProgress("Importing tracks...", 45, 100)
            
            // Import Tracks
            for (track in data.tracks) {
                val newPrimaryArtistId = track.primaryArtistId?.let { artistIdMap[it] }
                val remappedTrack = remapImagePath(track, pathMapping).copy(primaryArtistId = newPrimaryArtistId)
                
                val existingTrack = track.spotifyId?.takeIf { it.isNotBlank() }?.let { database.trackDao().findBySpotifyId(it) }
                    ?: track.musicbrainzId?.takeIf { it.isNotBlank() }?.let { database.trackDao().findByMusicBrainzId(it) }
                    ?: database.trackDao().findByTitleAndArtist(track.title, track.artist)
                
                if (existingTrack != null) {
                    if (conflictStrategy == ImportConflictStrategy.REPLACE) {
                        // Guard the unique spotify_id / youtube_id indices: if the
                        // backup's IDs already belong to a DIFFERENT track row,
                        // applying them would throw and abort the whole restore.
                        val safeTrack = remappedTrack.copy(
                            spotifyId = remappedTrack.spotifyId?.let { sid ->
                                val holder = database.trackDao().findBySpotifyId(sid)
                                if (holder == null || holder.id == existingTrack.id) sid else null
                            },
                            youtubeId = remappedTrack.youtubeId?.let { yid ->
                                val holder = database.trackDao().findByYoutubeId(yid)
                                if (holder == null || holder.id == existingTrack.id) yid else null
                            }
                        )
                        database.trackDao().update(safeTrack.copy(id = existingTrack.id))
                    } else {
                        // Backfill missing fields, but never steal a unique ID that
                        // another track already owns.
                        val backfilled = existingTrack.copy(
                            spotifyId = existingTrack.spotifyId ?: remappedTrack.spotifyId?.let { sid ->
                                if (database.trackDao().findBySpotifyId(sid) == null) sid else null
                            },
                            musicbrainzId = existingTrack.musicbrainzId ?: remappedTrack.musicbrainzId,
                            youtubeId = existingTrack.youtubeId ?: remappedTrack.youtubeId?.let { yid ->
                                if (database.trackDao().findByYoutubeId(yid) == null) yid else null
                            },
                            album = existingTrack.album ?: remappedTrack.album,
                            albumArtUrl = existingTrack.albumArtUrl ?: remappedTrack.albumArtUrl,
                            duration = existingTrack.duration ?: remappedTrack.duration
                        )
                        if (backfilled != existingTrack) {
                            database.trackDao().update(backfilled)
                        }
                    }
                    trackIdMap[track.id] = existingTrack.id
                } else {
                    val newId = database.trackDao().insert(remappedTrack.copy(id = 0))
                    if (newId > 0) {
                        trackIdMap[track.id] = newId
                        importedTracks++
                    } else {
                        // IGNORE'd insert (unique spotify/youtube id raced) — recover
                        // the mapping via lookup so listening events are not dropped.
                        val recovered = remappedTrack.spotifyId?.takeIf { it.isNotBlank() }
                            ?.let { database.trackDao().findBySpotifyId(it) }
                            ?: remappedTrack.youtubeId?.takeIf { it.isNotBlank() }
                                ?.let { database.trackDao().findByYoutubeId(it) }
                            ?: database.trackDao().findByTitleAndArtist(remappedTrack.title, remappedTrack.artist)
                        recovered?.let { trackIdMap[track.id] = it.id }
                    }
                }
            }
            
            _progress.value = ImportExportProgress("Importing track-artist links...", 55, 100)
            
            // Import TrackArtists
            val remappedTrackArtists = data.trackArtists.mapNotNull { ta ->
                val newTrackId = trackIdMap[ta.trackId] ?: return@mapNotNull null
                val newArtistId = artistIdMap[ta.artistId] ?: return@mapNotNull null
                ta.copy(trackId = newTrackId, artistId = newArtistId)
            }
            database.trackArtistDao().insertAllBatched(remappedTrackArtists)
            
            _progress.value = ImportExportProgress("Importing listening history...", 65, 100)
            
            // Import ListeningEvents from the staged JSON-lines file, chunk by chunk,
            // through the dedup pipeline. Rows are remapped to local track IDs; rows
            // whose track could not be mapped are skipped. The pipeline deduplicates
            // against existing rows (fingerprint + temporal reconciliation), so a
            // re-import is a no-op and real listening data is never overwritten.
            val eventResult = replayStagedEvents(stagedEventsFile, trackIdMap, conflictStrategy)
            importedEvents = eventResult.newlyAdded
            replacedEvents = eventResult.replaced
            skippedEvents = eventResult.skipped
            Log.i(TAG, "Restored listening events: $importedEvents new, $replacedEvents replaced, $skippedEvents skipped")
            
            _progress.value = ImportExportProgress("Importing metadata...", 80, 100)
            
            // Import EnrichedMetadata
            for (meta in data.enrichedMetadata) {
                val newTrackId = trackIdMap[meta.trackId] ?: continue
                val remappedMeta = remapImagePath(meta, pathMapping).copy(trackId = newTrackId)
                val existingMeta = database.enrichedMetadataDao().forTrackSync(newTrackId)
                
                if (existingMeta == null) {
                    database.enrichedMetadataDao().upsert(remappedMeta.copy(id = 0))
                } else if (conflictStrategy == ImportConflictStrategy.REPLACE) {
                    database.enrichedMetadataDao().update(remappedMeta.copy(id = existingMeta.id))
                }
            }

            // Track.albumArtUrl is a denormalized mirror used by the UI. Restoring tracks
            // before enriched metadata can temporarily reintroduce artwork that contradicts
            // a local/imported USER_RESET tombstone, especially with "Skip Duplicates".
            // Reconcile the mirror from the final metadata state before this transaction
            // commits so callers can never observe a split-brain artwork preference.
            for (restoredTrackId in trackIdMap.values.toSet()) {
                val restoredTrack = database.trackDao().getTrackById(restoredTrackId) ?: continue
                val restoredMetadata = database.enrichedMetadataDao().forTrackSync(restoredTrackId)
                val reconciledArtwork = resolveRestoredTrackArtwork(
                    albumArtSource = restoredMetadata?.albumArtSource,
                    metadataArtUrl = restoredMetadata?.albumArtUrl,
                    trackArtUrl = restoredTrack.albumArtUrl,
                )
                if (reconciledArtwork != restoredTrack.albumArtUrl) {
                    database.trackDao().updateAlbumArtUrl(restoredTrackId, reconciledArtwork)
                }
            }
            
            _progress.value = ImportExportProgress("Importing preferences...", 90, 100)
            
            // Import UserPreferences — REPLACE strategy only. SKIP means "keep
            // what this device has", so restoring must not clobber local settings.
            // Sanitize device-specific / auth-bound fields before upserting so that a backup
            // restored on a *different* (or freshly reinstalled) device does not carry over
            // Spotify polling state that has no valid tokens on the new device.  Leaving
            // spotifyApiOnlyMode=true would (a) permanently suppress Spotify notification
            // tracking and (b) schedule SpotifyPollingWorker in a perpetual failure loop.
            if (conflictStrategy == ImportConflictStrategy.REPLACE) {
                data.userPreferences?.let { prefs ->
                    database.userPreferencesDao().upsert(
                        prefs.copy(
                            spotifyLinked = false,
                            spotifyApiOnlyMode = false,
                            spotifyImportCursor = null,
                            lastSpotifyImportTimestamp = null
                        )
                    )
                }
            }

            // v6: Import UserLevel (gamification data)
            data.userLevel?.let { level ->
                if (conflictStrategy == ImportConflictStrategy.REPLACE ||
                    database.gamificationDao().getUserLevel() == null
                ) {
                    database.gamificationDao().upsertUserLevel(level)
                    Log.i(TAG, "Restored backup level: ${level.currentLevel} (${level.totalXp} XP)")
                }
            }
            
            // v6: Import Badges
            if (data.badges.isNotEmpty()) {
                val incomingBadges = if (conflictStrategy == ImportConflictStrategy.REPLACE) data.badges
                    else data.badges.filter { database.gamificationDao().getBadgeById(it.badgeId) == null }
                if (incomingBadges.isNotEmpty()) database.gamificationDao().upsertBadges(incomingBadges)
                Log.i(TAG, "Restored ${incomingBadges.size} new/replaced badges")
            }

            // v8: Import UserKnownArtists
            if (data.userKnownArtists.isNotEmpty()) {
                for (known in data.userKnownArtists) {
                    database.userKnownArtistDao().insert(known.copy(id = 0))
                }
                Log.i(TAG, "Imported ${data.userKnownArtists.size} user-known artists")
            }

            // v8: Import DailyChallenges - deduplicate by (date, challengeId)
            if (data.dailyChallenges.isNotEmpty()) {
                val existingKeys = database.gamificationDao().getAllCompletedChallenges()
                    .map { it.date to it.challengeId }.toSet()
                val toInsert = data.dailyChallenges.filter { it.date to it.challengeId !in existingKeys }
                if (toInsert.isNotEmpty()) {
                    database.gamificationDao().upsertChallenges(toInsert.map { it.copy(id = 0) })
                }
                val completedCount = toInsert.count { it.isCompleted }
                Log.i(TAG, "Imported ${toInsert.size} daily challenges ($completedCount completed, ${data.dailyChallenges.size - toInsert.size} skipped as duplicates)")
            }
            
            // Import Artist Aliases (for merged artists)
            var importedAliases = 0
            for (alias in data.artistAliases) {
                val newTargetId = artistIdMap[alias.targetArtistId]
                if (newTargetId != null) {
                    val remappedAlias = alias.copy(id = 0, targetArtistId = newTargetId)
                    val existingAlias = database.artistAliasDao().findAlias(alias.originalNameNormalized)
                    if (existingAlias == null) {
                        database.artistAliasDao().insertAlias(remappedAlias)
                        importedAliases++
                    }
                }
            }
            Log.i(TAG, "Imported $importedAliases artist aliases")
            
            // v5: Import Track Aliases (for merged tracks)
            var importedTrackAliases = 0
            for (alias in data.trackAliases) {
                val newTargetTrackId = trackIdMap[alias.targetTrackId]
                if (newTargetTrackId != null) {
                    val remappedAlias = alias.copy(id = 0, targetTrackId = newTargetTrackId)
                    val existingAlias = database.trackAliasDao().findAlias(alias.originalTitle, alias.originalArtist)
                    if (existingAlias == null) {
                        database.trackAliasDao().insertAlias(remappedAlias)
                        importedTrackAliases++
                    }
                }
            }
            Log.i(TAG, "Imported $importedTrackAliases track aliases")
            
            // v5: Import Manual Content Marks (podcast/audiobook filters)
            var importedContentMarks = 0
            for (mark in data.manualContentMarks) {
                // Remap track ID - skip if target track wasn't imported
                val newTrackId = trackIdMap[mark.targetTrackId] ?: continue
                val remappedMark = mark.copy(id = 0, targetTrackId = newTrackId)
                // Check if same pattern already exists
                val existingMark = database.manualContentMarkDao().findMatchingMark(
                    remappedMark.originalTitle,
                    remappedMark.originalArtist
                )
                if (existingMark == null) {
                    database.manualContentMarkDao().insertMark(remappedMark)
                    importedContentMarks++
                }
            }
            Log.i(TAG, "Imported $importedContentMarks manual content marks")
            
            // SKIP preserves this device's supported-app choices; REPLACE applies
            // the backed-up choices. The old unconditional IGNORE meant that
            // "Replace Existing" could not restore any app-specific setting.
            if (data.appPreferences.isNotEmpty()) {
                if (conflictStrategy == ImportConflictStrategy.REPLACE) {
                    data.appPreferences.forEach { database.appPreferenceDao().upsert(it) }
                } else {
                    database.appPreferenceDao().insertAll(data.appPreferences)
                }
                Log.i(TAG, "Imported ${data.appPreferences.size} app preferences using $conflictStrategy")
            }
            
            // v5: Import Scrobble Archive (Last.fm compressed history) from the
            // staged JSON-lines file, chunk by chunk. Conflict handling:
            //  - SKIP: never touch existing rows (IGNORE insert). An older backup
            //    can no longer regress newer play counts / timestamp blobs.
            //  - REPLACE: merge timestamp sets so the union of both histories is
            //    kept instead of one side silently overwriting the other.
            val archiveStats = replayStagedArchive(stagedArchiveFile, conflictStrategy)
            Log.i(TAG, "Imported scrobble archive: ${archiveStats.first} new, ${archiveStats.second} merged/skipped")
            
            // v5: Import Last.fm Import Metadata.
            // The table has no unique index; it is keyed by username in practice.
            // REPLACE updates the local session row in place (keeps local id, no
            // duplicate accumulation on repeated restores). SKIP leaves an existing
            // session's state (sync cursors, resume points) untouched and only adds
            // usernames this device has never imported.
            if (data.lastFmImportMetadata.isNotEmpty()) {
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
                Log.i(TAG, "Imported ${data.lastFmImportMetadata.size} Last.fm import metadata records")
            }
            
            // Keep files before commit can expose their paths. If Room's return is cancelled
            // after commit, cleanup must not remove artwork referenced by committed rows.
            artworkStaging.retainForCommit()
            } // end database.withTransaction
            
            // Preserve identity on SKIP; a Drive backup must not rename a
            // currently configured profile unless replacement was requested.
            if (conflictStrategy == ImportConflictStrategy.REPLACE) {
                data.userName?.takeIf { it.isNotBlank() }?.let { name ->
                    profileIdentityManager.updateUserName(name)
                    Log.i(TAG, "Restored user name: $name")
                }
                val restoredProfileImagePath = resolveRestoredProfileImagePath(
                    exportedProfileImagePath = data.userProfileImagePath,
                    pathMapping = pathMapping,
                )
                profileIdentityManager.restoreProfileImagePath(restoredProfileImagePath)
                Log.i(TAG, "Restored profile image path present=${!restoredProfileImagePath.isNullOrBlank()}")
            }
            val restoredAppearance = try {
                data.appearance?.let {
                    appearanceRepository.restoreBackup(it, replace = conflictStrategy == ImportConflictStrategy.REPLACE)
                } ?: false
            } catch (e: java.io.IOException) {
                Log.w(TAG, "Library restored but appearance settings could not be written", e)
                false
            }
            
            // Schedule pre-caching of hotlinked images. The list comes from the
            // restored backup file and is therefore untrusted input: a crafted
            // backup must not be able to make Tempo bulk-fetch arbitrary
            // attacker-chosen URLs in the background (covert beacon). Only known
            // music-art CDN hosts are pre-cached; per-image UI loading is unaffected.
            val cacheableHotlinks = ImageUrlHostAllowlist.filterAllowed(data.hotlinkedUrls)
            val droppedHotlinkCount = data.hotlinkedUrls.size - cacheableHotlinks.size
            if (droppedHotlinkCount > 0) {
                Log.w(TAG, "Dropped $droppedHotlinkCount non-allowlisted hotlinked URLs from the restored backup")
            }
            if (cacheableHotlinks.isNotEmpty()) {
                _progress.value = ImportExportProgress("Scheduling image cache...", 95, 100)
                PostRestoreCacheWorker.schedule(context, cacheableHotlinks)
            }
            
            // Invalidate stats cache to force UI refresh (fixes "New User" state persisting)
            statsRepository.invalidateCache()
            
            _progress.value = ImportExportProgress("Import complete!", 100, 100)
            
            ImportExportResult.Success(
                tracksCount = importedTracks,
                artistsCount = importedArtists,
                albumsCount = importedAlbums,
                eventsCount = importedEvents,
                eventsReplaced = replacedEvents,
                eventsSkipped = skippedEvents,
                imagesCount = extractedImages.size,
                appearanceStatus = if (restoredAppearance) BackupAppearanceStatus.RESTORED else BackupAppearanceStatus.UNCHANGED,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            ImportExportResult.Error("Import failed: ${e.message}", e)
        } finally {
            stagedEventsFile.delete()
            stagedArchiveFile.delete()
            runCatching { artworkStaging.close() }
                .onFailure { Log.w(TAG, "Unable to clean restore artwork staging", it) }
            _progress.value = null
            operationLease.release()
        }
    }

    /**
     * Replays staged listening events (one JSON object per line) in chunks through the dedup
     * pipeline, skipping rows whose track could not be mapped. Returns how many were new.
     */
    private suspend fun replayStagedEvents(
        stagedFile: File,
        trackIdMap: Map<Long, Long>,
        conflictStrategy: ImportConflictStrategy,
    ): ListeningEventDao.InsertResult {
        if (!stagedFile.exists() || stagedFile.length() == 0L) {
            return ListeningEventDao.InsertResult(0, 0)
        }

        val eventAdapter = moshi.adapter(ListeningEvent::class.java)
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
                // Backups retain original live package sources, so inferred
                // import-source checks alone cannot activate cross-track alias
                // reconciliation after restoring into a different library.
                forceAliasLookup = true,
            )
            inserted += result.inserted
            skipped += result.skipped
            replaced += result.replaced
            updated += result.updated
            chunk = ArrayList(EVENT_IMPORT_CHUNK)
        }

        stagedFile.bufferedReader().useLines { lines ->
            for (line in lines) {
                if (line.isBlank()) continue
                val event = try {
                    eventAdapter.fromJson(line)
                } catch (e: Exception) {
                    Log.w(TAG, "Skipping malformed staged listening event", e)
                    null
                } ?: continue
                val newTrackId = trackIdMap[event.track_id] ?: continue
                // A fingerprint from a backup was hashed with its OLD track ID.
                // The dedup pipeline recalculates it against the local identity.
                chunk.add(event.copy(id = 0, track_id = newTrackId, contentFingerprint = null))
                if (chunk.size >= EVENT_IMPORT_CHUNK) {
                    flush()
                }
            }
        }
        flush()
        return ListeningEventDao.InsertResult(inserted, skipped, replaced, updated)
    }

    /**
     * Replays staged scrobble archive rows (one JSON object per line) in chunks.
     *
     * - [ImportConflictStrategy.SKIP]: existing rows are never touched (IGNORE
     *   insert), so an older backup cannot regress newer play counts.
     * - [ImportConflictStrategy.REPLACE]: timestamp sets are merged (union) so
     *   neither side of the history is silently lost.
     *
     * Returns (newRows, mergedOrSkippedRows).
     */
    private suspend fun replayStagedArchive(
        stagedFile: File,
        conflictStrategy: ImportConflictStrategy
    ): Pair<Int, Int> {
        if (!stagedFile.exists() || stagedFile.length() == 0L) return 0 to 0

        val archiveAdapter = moshi.adapter(ScrobbleArchive::class.java)
        var newRows = 0
        var mergedOrSkipped = 0
        var chunk = ArrayList<ScrobbleArchive>(ARCHIVE_IMPORT_CHUNK)

        suspend fun flush() {
            if (chunk.isEmpty()) return
            when (conflictStrategy) {
                ImportConflictStrategy.SKIP -> {
                    // IGNORE insert returns -1 for rows skipped by the unique
                    // track_hash index, so the return values tell us exactly
                    // how many were new vs. skipped.
                    val results = database.scrobbleArchiveDao().insertAllIgnore(chunk.map { it.copy(id = 0) })
                    val inserted = results.count { it > 0 }
                    newRows += inserted
                    mergedOrSkipped += chunk.size - inserted
                }
                ImportConflictStrategy.REPLACE -> {
                    for (row in chunk) {
                        val (id, _) = database.scrobbleArchiveDao().upsertWithMerge(
                            row.copy(id = 0),
                            ArchiveTimestampCodec::decompress,
                            ArchiveTimestampCodec::compress
                        )
                        if (id > 0) newRows++ else mergedOrSkipped++
                    }
                }
            }
            chunk = ArrayList(ARCHIVE_IMPORT_CHUNK)
        }

        stagedFile.bufferedReader().useLines { lines ->
            for (line in lines) {
                if (line.isBlank()) continue
                val row = try {
                    archiveAdapter.fromJson(line)
                } catch (e: Exception) {
                    Log.w(TAG, "Skipping malformed staged scrobble archive row", e)
                    null
                } ?: continue
                chunk.add(row)
                if (chunk.size >= ARCHIVE_IMPORT_CHUNK) {
                    flush()
                }
            }
        }
        flush()
        return newRows to mergedOrSkipped
    }
    
    private fun collectImageUrls(
        tracks: List<Track>,
        artists: List<Artist>,
        albums: List<Album>,
        enrichedMetadata: List<EnrichedMetadata>
    ): Pair<List<String>, List<String>> {
        val localPaths = mutableSetOf<String>()
        val hotlinkUrls = mutableSetOf<String>()
        
        fun classify(url: String?) {
            url?.let {
                when {
                    it.startsWith("file://") -> localPaths.add(it)
                    it.startsWith("http://") || it.startsWith("https://") -> hotlinkUrls.add(it)
                }
            }
        }
        
        tracks.forEach { classify(it.albumArtUrl) }
        artists.forEach { classify(it.imageUrl) }
        albums.forEach { classify(it.artworkUrl) }
        
        enrichedMetadata.forEach { meta ->
            classify(meta.albumArtUrl)
            classify(meta.albumArtUrlSmall)
            classify(meta.albumArtUrlLarge)
            classify(meta.spotifyArtistImageUrl)
            classify(meta.iTunesArtistImageUrl)
            classify(meta.deezerArtistImageUrl)
            classify(meta.lastFmArtistImageUrl)
        }
        
        return localPaths.toList() to hotlinkUrls.toList()
    }
    
    private fun sanitizeBundledImageName(bundledName: String): String? {
        val candidate = bundledName.trim()
        if (candidate.isBlank() || candidate.length > 128) return null
        if (candidate.contains('/') || candidate.contains('\\') || candidate == "." || candidate == "..") return null
        if (candidate.contains("..")) return null
        return candidate
    }

    private fun resolveExportableLocalImage(fileUri: String): File? {
        if (!fileUri.startsWith("file://")) return null

        val file = File(fileUri.removePrefix("file://"))
        if (!file.exists() || !file.isFile) return null

        val canonicalFile = file.canonicalFile
        val allowedRoots = listOf(context.filesDir, context.cacheDir).map { it.canonicalFile }
        val isAllowed = allowedRoots.any { root ->
            canonicalFile.path == root.path || canonicalFile.path.startsWith(root.path + File.separator)
        }
        if (!isAllowed) return null
        if (canonicalFile.length() > MAX_BUNDLED_IMAGE_BYTES) return null
        return canonicalFile
    }
    
    // Path remapping functions for different entity types
    
    private fun remapImagePath(track: Track, pathMapping: Map<String, String>): Track =
        track.copy(albumArtUrl = remapRestoredImageUrl(track.albumArtUrl, pathMapping))
    
    private fun remapImagePath(artist: Artist, pathMapping: Map<String, String>): Artist =
        artist.copy(imageUrl = remapRestoredImageUrl(artist.imageUrl, pathMapping))
    
    private fun remapImagePath(album: Album, pathMapping: Map<String, String>): Album =
        album.copy(artworkUrl = remapRestoredImageUrl(album.artworkUrl, pathMapping))
    
    private fun remapImagePath(meta: EnrichedMetadata, pathMapping: Map<String, String>): EnrichedMetadata =
        meta.copy(
            albumArtUrl = remapRestoredImageUrl(meta.albumArtUrl, pathMapping),
            albumArtUrlSmall = remapRestoredImageUrl(meta.albumArtUrlSmall, pathMapping),
            albumArtUrlLarge = remapRestoredImageUrl(meta.albumArtUrlLarge, pathMapping),
            spotifyArtistImageUrl = remapRestoredImageUrl(meta.spotifyArtistImageUrl, pathMapping),
            iTunesArtistImageUrl = remapRestoredImageUrl(meta.iTunesArtistImageUrl, pathMapping),
            deezerArtistImageUrl = remapRestoredImageUrl(meta.deezerArtistImageUrl, pathMapping),
            lastFmArtistImageUrl = remapRestoredImageUrl(meta.lastFmArtistImageUrl, pathMapping),
        )
    /**
     * Post-write integrity check: the archive must be a readable ZIP that
     * actually contains data.json. Returns null when valid, otherwise a
     * human-readable failure reason.
     */
    private fun validateBackupArchive(file: File): String? = try {
        java.util.zip.ZipFile(file).use { zip ->
            if (zip.getEntry(TempoExportData.DATA_FILENAME) == null)
                "Backup archive is missing its data payload"
            else null
        }
    } catch (e: Exception) {
        "Backup archive failed validation: ${e.message}"
    }

}

/**
 * Single-owner gate for the singleton import/export manager.
 *
 * A lease can release the gate only once. Keeping the lease in the public
 * operation prevents a nested helper from accidentally clearing the shared
 * gate while its caller is still copying a staged archive to the destination.
 */
internal class ImportExportOperationGate {
    private val active = AtomicBoolean(false)

    fun tryAcquire(): Lease? =
        if (active.compareAndSet(false, true)) Lease(this) else null

    private fun release() {
        active.set(false)
    }

    class Lease internal constructor(
        private val owner: ImportExportOperationGate
    ) {
        private val released = AtomicBoolean(false)

        fun release() {
            if (released.compareAndSet(false, true)) owner.release()
        }
    }
}

/** Enforces a decompressed ZIP entry limit without closing its owning ZipInputStream. */
private class LimitedEntryInputStream(
    private val source: InputStream,
    private val limit: Long,
) : InputStream() {
    private var bytesRead = 0L

    override fun read(): Int {
        if (bytesRead >= limit) {
            val extra = source.read()
            if (extra < 0) return -1
            throw SecurityException("Backup JSON exceeds the decompressed size limit")
        }
        val value = source.read()
        if (value >= 0) bytesRead++
        return value
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRead >= limit) return if (read() < 0) -1 else error("unreachable")
        val allowed = minOf(length.toLong(), limit - bytesRead).toInt()
        val count = source.read(buffer, offset, allowed)
        if (count > 0) bytesRead += count
        return count
    }

    override fun close() = Unit
}

internal fun remapRestoredImageUrl(
    exportedUrl: String?,
    pathMapping: Map<String, String>,
): String? {
    if (exportedUrl.isNullOrBlank()) return exportedUrl
    if (exportedUrl.startsWith("content://")) {
        // Content-provider grants are device/session specific and are not
        // transferable through a Tempo backup.
        return null
    }
    if (!exportedUrl.startsWith("file://")) return exportedUrl

    // file:// paths are device-private. They are usable after restore only when
    // the corresponding image was actually bundled and extracted successfully.
    return pathMapping[exportedUrl]
}

internal fun resolveRestoredTrackArtwork(
    albumArtSource: AlbumArtSource?,
    metadataArtUrl: String?,
    trackArtUrl: String?,
): String? =
    when (albumArtSource) {
        AlbumArtSource.USER_RESET -> null
        AlbumArtSource.USER_SELECTED ->
            metadataArtUrl?.takeIf { it.isNotBlank() }
                ?: trackArtUrl?.takeIf { it.isNotBlank() }
        else ->
            when {
                isLocalBackupArtwork(trackArtUrl) -> trackArtUrl
                !metadataArtUrl.isNullOrBlank() -> metadataArtUrl
                else -> trackArtUrl?.takeIf { it.isNotBlank() }
            }
    }

internal fun resolveRestoredProfileImagePath(
    exportedProfileImagePath: String?,
    pathMapping: Map<String, String>
): String? {
    if (exportedProfileImagePath.isNullOrBlank()) return null
    // A legitimate exported profile image is always a file:// URI inside the
    // app's private storage (ProfileIdentityManager.updateProfileImage only ever
    // saves that form, and the export bundles + remaps it via the image
    // manifest). A backup file is untrusted input, so any other value — an
    // http(s) URL, a content:// URI, or an arbitrary device path — is discarded
    // instead of being persisted as-is.
    if (!exportedProfileImagePath.startsWith("file://")) return null
    return pathMapping[exportedProfileImagePath]
}
