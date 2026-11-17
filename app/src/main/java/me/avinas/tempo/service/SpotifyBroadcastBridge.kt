package me.avinas.tempo.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log

/**
 * Spotify's opt-in Device Broadcast Status receiver.
 * It is dynamically registered while Tempo's tracking service is alive.
 * A playback event alone does not contain track metadata: wait until both are known.
 */
internal class SpotifyBroadcastBridge(
    private val context: Context,
    private val onPlayback: (Track, Boolean, Long) -> Unit,
    private val onQueueChanged: () -> Unit,
    // Pre-Android-14 cannot reliably read the broadcast sender UID. Only
    // accept metadata/playback that agrees with Spotify's trusted MediaSession.
    // Rejection falls back to normal Android MediaSession/notification tracking.
    private val allowLegacyBroadcast: (Track?) -> Boolean = { false },
) {
    data class Track(
        val title: String,
        val artist: String,
        val album: String?,
        val durationMs: Long,
    )

    companion object {
        /** Album/edition and duration are part of recording identity for broadcast reconciliation. */
        internal fun spotifyMetadataChanged(previous: Track?, next: Track): Boolean =
            previous != next

        private const val TAG = "SpotifyBroadcastBridge"
        private const val PACKAGE = "com.spotify.music"
        private const val METADATA = "$PACKAGE.metadatachanged"
        private const val PLAYBACK = "$PACKAGE.playbackstatechanged"
        private const val QUEUE = "$PACKAGE.queuechanged"
    }

    private var registered = false
    private var receiverThread: HandlerThread? = null
    private var track: Track? = null
    private var playing: Boolean? = null
    private var positionMs = 0L
    private val lastSentAtByAction = mutableMapOf<String, Long>()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!SpotifyBroadcastPresence.enabled(context)) return
            val action = intent.action ?: return
            if (action != METADATA && action != PLAYBACK && action != QUEUE) return

            // Reject even an UNKNOWN sender on Android 14+. On older Android,
            // sender UID is unavailable so validate against a trusted Spotify
            // MediaSession *before* mutating receiver state.
            if (Build.VERSION.SDK_INT >= 34) {
                val senderUid = sentFromUid
                if (senderUid < 0 ||
                    context.packageManager.getPackagesForUid(senderUid)?.contains(PACKAGE) != true
                ) return
            }

            val now = System.currentTimeMillis()
            val sentAt = intent.getLongExtra("timeSent", now)
            // Metadata and playback broadcasts can be delivered out of order.
            // Only discard stale events within the SAME action type.
            if (sentAt < now - 120_000L || sentAt > now + 30_000L) return
            if (sentAt < (lastSentAtByAction[action] ?: 0L)) return

            // Don't advance the anti-stale cursor until the sender and its
            // metadata have passed validation. A forged broadcast could
            // otherwise preempt genuine messages for up to 30 seconds.
            when (action) {
                METADATA -> {
                    val title = intent.getStringExtra("track")?.trim()
                        ?.takeIf { it.isNotEmpty() && it.length <= 300 } ?: return
                    val artist = intent.getStringExtra("artist")?.trim()
                        ?.takeIf { it.isNotEmpty() && it.length <= 300 } ?: return
                    val candidate = Track(
                        title = title,
                        artist = artist,
                        album = intent.getStringExtra("album")?.take(300),
                        // Spotify's documentation says seconds; some versions
                        // report milliseconds. Reject implausibly long values.
                        durationMs = intent.getIntExtra("length", 0).coerceAtLeast(0).toLong()
                            .let { if (it in 1L..10_000L) it * 1000L else it }
                            .coerceIn(0L, 24 * 60 * 60_000L),
                    )
                    if (Build.VERSION.SDK_INT < 34 && !allowLegacyBroadcast(candidate)) return
                    // The last PLAYBACK state belongs to the *previous* track.
                    // Do not start a new track with stale playing=true or old position
                    // until the matching playback broadcast (or MediaSession) arrives.
                    if (spotifyMetadataChanged(track, candidate)) {
                        playing = null
                    }
                    track = candidate
                    positionMs = 0L
                    lastSentAtByAction[action] = sentAt
                }
                PLAYBACK -> {
                    if (Build.VERSION.SDK_INT < 34 && !allowLegacyBroadcast(track)) return
                    playing = intent.getBooleanExtra("playing", false)
                    positionMs = intent.getIntExtra("playbackPosition", 0)
                        .coerceIn(0, 24 * 60 * 60_000).toLong()
                }
                QUEUE -> {
                    if (Build.VERSION.SDK_INT < 34 && !allowLegacyBroadcast(track)) return
                    lastSentAtByAction[action] = sentAt
                    onQueueChanged()
                    return
                }
            }

            val currentTrack = track ?: return
            val isPlaying = playing ?: return
            lastSentAtByAction[action] = sentAt
            SpotifyBroadcastPresence.publish(currentTrack, isPlaying, positionMs)
            onPlayback(currentTrack, isPlaying, positionMs)
        }
    }

    fun start() {
        if (registered) return
        val filter = IntentFilter().apply {
            addAction(METADATA)
            addAction(PLAYBACK)
            addAction(QUEUE)
        }
        try {
            // Parse events and update the session pipeline off the main UI thread.
            val thread = HandlerThread("TempoSpotifyBroadcast").also { it.start() }
            receiverThread = thread
            val scheduler = Handler(thread.looper)
            // Cross-app broadcasts must be exported on recent Android versions.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, null, scheduler, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter, null, scheduler)
            }
            registered = true
        } catch (e: Exception) {
            receiverThread?.quitSafely()
            receiverThread = null
            Log.w(TAG, "Spotify broadcasts unavailable; MediaSession tracking remains active", e)
        }
    }

    fun stop() {
        if (!registered) return
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering Spotify receiver", e)
        } finally {
            registered = false
            receiverThread?.quitSafely()
            receiverThread = null
            SpotifyBroadcastPresence.clear()
        }
    }
}
