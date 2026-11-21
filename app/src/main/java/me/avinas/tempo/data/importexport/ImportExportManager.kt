package me.avinas.tempo.data.importexport

import android.content.Context
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.avinas.tempo.data.local.AppDatabase
import me.avinas.tempo.data.local.dao.isLocalBackupArtwork
import me.avinas.tempo.data.local.entities.*
import me.avinas.tempo.data.profile.ProfileIdentityManager
import me.avinas.tempo.utils.ImageUrlHostAllowlist
import com.squareup.moshi.JsonReader
import me.avinas.tempo.worker.PostRestoreCacheWorker
import okio.buffer
import okio.sink
import okio.source
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
            
            val databaseRestore = BackupDatabaseRestorer(
                database = database,
                moshi = moshi,
                onProgress = { _progress.value = it },
            ).restore(
                data = data,
                pathMapping = pathMapping,
                stagedEventsFile = stagedEventsFile,
                stagedArchiveFile = stagedArchiveFile,
                conflictStrategy = conflictStrategy,
                artworkStaging = artworkStaging,
            )

            
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
                tracksCount = databaseRestore.tracksAdded,
                artistsCount = databaseRestore.artistsAdded,
                albumsCount = databaseRestore.albumsAdded,
                eventsCount = databaseRestore.events.newlyAdded,
                eventsReplaced = databaseRestore.events.replaced,
                eventsSkipped = databaseRestore.events.skipped,
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
