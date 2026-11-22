package me.avinas.tempo.service

import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.util.Log

/**
 * Classifies a MediaSession's shape after package allow/block policy has already run.
 *
 * User-controlled content/duration rules remain authoritative through [TrackingRuntimePolicy].
 * Generic MediaSession heuristics are only a fallback when policy defers.
 */
internal class MediaSessionContentClassifier(
    private val trackingPolicy: TrackingRuntimePolicy,
) {
    fun isMusic(controller: MediaController): Boolean {
        val packageName = controller.packageName
        val metadata = controller.metadata ?: return false

        val title =
            metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
                ?: return false
        val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)

        // Preserve the old service behavior: policy needs a meaningful title, while the generic
        // MediaSession heuristics below may still classify a blank-but-present title.
        if (title.isNotBlank()) {
            val artist =
                metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
                    ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                    ?: "Unknown Artist"
            when (
                trackingPolicy.evaluateMedia(
                    packageName = packageName,
                    title = title,
                    artist = artist,
                    durationMs = duration,
                )
            ) {
                TrackingRuntimePolicy.MediaVerdict.FORCE_ALLOW -> return true
                TrackingRuntimePolicy.MediaVerdict.REJECT -> return false
                TrackingRuntimePolicy.MediaVerdict.DEFER -> Unit
            }
        }

        if (duration in 1 until MIN_MUSIC_DURATION_MS) {
            Log.d(TAG, "Rejecting short content ($duration ms): '$title' from $packageName")
            return false
        }

        val album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM)
        val hasMusicMetadata =
            !metadata.getString(MediaMetadata.METADATA_KEY_ARTIST).isNullOrBlank() ||
                !metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).isNullOrBlank() ||
                !album.isNullOrBlank()

        val lowerPackage = packageName.lowercase()
        val looksLikeVideoApp =
            lowerPackage.contains("video") ||
                lowerPackage.contains("movie") ||
                lowerPackage.contains("tv") ||
                lowerPackage.contains("stream") ||
                (lowerPackage.contains("player") && !lowerPackage.contains("music"))

        if (looksLikeVideoApp && !hasMusicMetadata) {
            Log.d(TAG, "Rejecting likely video app without music metadata: $packageName")
            return false
        }

        return controller.playbackState?.state == PlaybackState.STATE_PLAYING || hasMusicMetadata
    }

    private companion object {
        const val TAG = "MediaSessionClassifier"
        const val MIN_MUSIC_DURATION_MS = 30_000L
    }
}
