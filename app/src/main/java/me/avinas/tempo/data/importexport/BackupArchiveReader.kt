package me.avinas.tempo.data.importexport

import android.content.Context
import android.net.Uri
import android.util.Log
import com.squareup.moshi.JsonReader
import java.io.BufferedInputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import okio.buffer
import okio.source

/**
 * Reads and validates one untrusted Tempo backup archive.
 *
 * Bounded entities stay in the decoded [TempoExportData]. Unbounded listening
 * events and scrobble rows are streamed into a caller-owned SQLite staging store,
 * so large histories never have to live in memory and are not reparsed inside the
 * main Room restore transaction.
 */
internal class BackupArchiveReader(
    private val context: Context,
    private val codec: TempoExportJsonCodec,
) {
    sealed interface Result {
        data class Success(
            val data: TempoExportData,
            val extractedImages: Map<String, String>,
            val pathMapping: Map<String, String>,
            val stagedEventCount: Int,
            val stagedArchiveCount: Int,
        ) : Result

        data class Error(val message: String) : Result
    }

    fun read(
        uri: Uri,
        stagingStore: RestoreStagingStore,
        artworkStaging: RestoreArtworkStaging,
    ): Result {
        var exportData: TempoExportData? = null
        var dataEntrySeen = false
        var totalExtractedImageBytes = 0L
        var stagedEventCount = 0
        var stagedArchiveCount = 0

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                while (entry != null) {
                    when {
                        entry.name == TempoExportData.DATA_FILENAME -> {
                            if (dataEntrySeen) {
                                throw SecurityException("Backup contains duplicate ${TempoExportData.DATA_FILENAME} entries")
                            }
                            dataEntrySeen = true
                            val reader = JsonReader.of(
                                LimitedEntryInputStream(
                                    zipIn,
                                    BackupArchivePolicy.MAX_DATA_JSON_BYTES,
                                ).source().buffer(),
                            )
                            exportData =
                                stagingStore.writeBatch {
                                    codec.read(
                                        reader,
                                        TempoExportJsonCodec.StreamHandlers(
                                            onListeningEvent = { event ->
                                                if (
                                                    stagedEventCount >=
                                                    BackupArchivePolicy.MAX_IMPORTED_EVENTS
                                                ) {
                                                    throw SecurityException(
                                                        "Backup contains too many listening events",
                                                    )
                                                }
                                                stagingStore.stage(event)
                                                stagedEventCount++
                                            },
                                            onScrobbleArchiveRow = { row ->
                                                if (
                                                    stagedArchiveCount >=
                                                    BackupArchivePolicy.MAX_IMPORTED_ARCHIVE_ROWS
                                                ) {
                                                    throw SecurityException(
                                                        "Backup contains too many archive rows",
                                                    )
                                                }
                                                stagingStore.stage(row)
                                                stagedArchiveCount++
                                            },
                                        ),
                                    )
                                }
                        }

                        entry.name.startsWith(BackupArchivePolicy.IMAGES_DIR) &&
                            !entry.isDirectory -> {
                            val bundledName =
                                entry.name.removePrefix(BackupArchivePolicy.IMAGES_DIR)
                            val safeName =
                                BackupArchivePolicy.sanitizeBundledImageName(bundledName)
                            if (safeName == null) {
                                Log.w(TAG, "Skipping unsafe bundled image name in import")
                            } else {
                                val remainingBudget =
                                    BackupArchivePolicy.MAX_TOTAL_IMAGE_BYTES -
                                        totalExtractedImageBytes
                                totalExtractedImageBytes += artworkStaging.stage(
                                    safeName,
                                    zipIn,
                                    minOf(
                                        BackupArchivePolicy.MAX_BUNDLED_IMAGE_BYTES,
                                        remainingBudget,
                                    ),
                                )
                            }
                        }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
        }

        val data = exportData
            ?: return Result.Error("Invalid backup file: no data found")
        if (data.version > TempoExportData.CURRENT_VERSION) {
            return Result.Error("Backup is from a newer app version. Please update Tempo.")
        }

        // Promotion happens only after parsing, limits and version validation succeed.
        val extractedImages = artworkStaging.promote()
        val pathMapping = data.localImageManifest.mapNotNull { (bundledName, originalPath) ->
            extractedImages[bundledName]?.let { newPath -> originalPath to newPath }
        }.toMap()

        return Result.Success(
            data = data,
            extractedImages = extractedImages,
            pathMapping = pathMapping,
            stagedEventCount = stagedEventCount,
            stagedArchiveCount = stagedArchiveCount,
        )
    }

    private companion object {
        const val TAG = "BackupArchiveReader"
    }
}

/** Enforces a decompressed ZIP entry limit without closing its owning ZIP stream. */
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
