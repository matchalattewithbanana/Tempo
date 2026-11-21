package me.avinas.tempo.service

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.flow.first
import me.avinas.tempo.data.repository.TrackRepository

/**
 * Owns notification/media-session artwork extraction and Tempo's local fallback-art cache.
 *
 * Callers only provide media metadata. Bitmap decoding details, file naming/downsampling and the
 * "only fill a missing track art URL" policy remain hidden here.
 */
internal class LocalAlbumArtStore(
    context: Context,
    private val trackRepository: TrackRepository,
) {
    private val appContext = context.applicationContext
    private val directory = File(appContext.filesDir, ALBUM_ART_DIR)

    fun extractAndSave(
        notification: Notification,
        title: String,
        artist: String,
    ): String? = extract(notification)?.let { save(it, title, artist) }

    fun save(
        bitmap: Bitmap,
        title: String,
        artist: String,
    ): String? =
        try {
            if (!directory.isDirectory && !directory.mkdirs()) {
                throw java.io.IOException("Could not create local album-art directory")
            }

            val file = File(directory, "art_${PlaybackIdentityKey.of(title, artist)}.jpg")
            if (file.exists()) return "file://${file.absolutePath}"

            val toSave = downsample(bitmap, MAX_DIMENSION_PX)
            try {
                FileOutputStream(file).use { output ->
                    check(toSave.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                        "Bitmap encoder rejected local album art"
                    }
                    output.fd.sync()
                }
            } finally {
                if (toSave !== bitmap) toSave.recycle()
            }

            "file://${file.absolutePath}"
        } catch (failure: Exception) {
            Log.e(TAG, "Unable to save local album art", failure)
            null
        }

    suspend fun applyFallbackIfMissing(
        trackId: Long,
        localArtUrl: String,
    ) {
        try {
            val track = trackRepository.getById(trackId).first() ?: return
            if (track.albumArtUrl.isNullOrBlank()) {
                trackRepository.updateAutomaticAlbumArtUrl(trackId, localArtUrl)
            }
        } catch (failure: Exception) {
            Log.e(TAG, "Unable to apply local album-art fallback for track $trackId", failure)
        }
    }

    private fun extract(notification: Notification): Bitmap? =
        try {
            val extras = notification.extras

            @Suppress("DEPRECATION")
            extras.getParcelable<Bitmap>(Notification.EXTRA_LARGE_ICON)?.let { return it }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notification.getLargeIcon()?.let { icon ->
                    try {
                        icon.loadDrawable(appContext)?.let { return it.toBitmap() }
                    } catch (failure: Exception) {
                        Log.w(TAG, "Unable to load notification large-icon drawable", failure)
                    }
                }
            }

            @Suppress("DEPRECATION")
            notification.largeIcon
        } catch (failure: Exception) {
            Log.w(TAG, "Unable to extract notification album art", failure)
            null
        }

    private fun downsample(
        bitmap: Bitmap,
        maxDimension: Int,
    ): Bitmap {
        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()
        if (width <= maxDimension && height <= maxDimension) return bitmap

        val ratio = (maxDimension / maxOf(width, height)).coerceAtMost(1f)
        return Bitmap.createScaledBitmap(
            bitmap,
            (width * ratio).toInt().coerceAtLeast(1),
            (height * ratio).toInt().coerceAtLeast(1),
            true,
        )
    }

    private companion object {
        const val TAG = "LocalAlbumArtStore"
        const val ALBUM_ART_DIR = "album_art"
        const val MAX_DIMENSION_PX = 512
        const val JPEG_QUALITY = 85
    }
}
