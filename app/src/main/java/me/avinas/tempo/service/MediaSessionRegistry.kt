package me.avinas.tempo.service

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Android MediaSession observation and callback-registration boundary.
 *
 * Tempo's tracking/content policy stays outside this class. The registry owns only platform
 * lifecycle: manager listener registration, one preferred token per package, token replacement,
 * callback attach/detach, rescans and teardown.
 */
internal class MediaSessionRegistry(
    context: Context,
    private val listenerComponent: ComponentName,
    private val onControllersChanged: (List<MediaController>) -> Unit,
) {
    private val appContext = context.applicationContext
    private var manager: MediaSessionManager? = null
    private var activeSessionsListener: MediaSessionManager.OnActiveSessionsChangedListener? = null
    private val controllers = ConcurrentHashMap<String, MediaController>()
    private val callbacks = ConcurrentHashMap<String, MediaController.Callback>()

    @Volatile
    var isInitialized: Boolean = false
        private set

    private var lastSessionChangeLog: Pair<Int, Long>? = null

    fun initialize(): Boolean {
        if (isInitialized) return true

        return try {
            val mediaManager =
                appContext.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val listener =
                MediaSessionManager.OnActiveSessionsChangedListener { active ->
                    dispatch(active.orEmpty())
                }

            mediaManager.addOnActiveSessionsChangedListener(listener, listenerComponent)
            manager = mediaManager
            activeSessionsListener = listener
            isInitialized = true

            val initial = mediaManager.getActiveSessions(listenerComponent).orEmpty()
            dispatch(initial)
            Log.i(TAG, "MediaSessionManager initialized with ${initial.size} active sessions")
            true
        } catch (failure: SecurityException) {
            release()
            Log.w(
                TAG,
                "MediaSessionManager access denied - notification access may not be granted",
                failure,
            )
            false
        } catch (failure: Exception) {
            release()
            Log.e(TAG, "Failed to initialize MediaSessionManager", failure)
            false
        }
    }

    fun rescan(): Boolean {
        if (!isInitialized && !initialize()) return false
        return try {
            val active = manager?.getActiveSessions(listenerComponent).orEmpty()
            Log.i(TAG, "Rescanning active media sessions: ${active.size} sessions found")
            dispatch(active)
            true
        } catch (failure: Exception) {
            Log.w(TAG, "Error rescanning active media sessions", failure)
            false
        }
    }

    operator fun get(packageName: String): MediaController? = controllers[packageName]

    fun contains(packageName: String): Boolean = controllers.containsKey(packageName)

    fun packagesSnapshot(): Set<String> = controllers.keys.toSet()

    fun entriesSnapshot(): List<Pair<String, MediaController>> = controllers.entries.map { it.key to it.value }

    fun valuesSnapshot(): List<MediaController> = controllers.values.toList()

    /**
     * Drop the old package registration only when Android replaced its MediaSession token.
     * Returns true when a replacement was detached.
     */
    fun detachIfTokenChanged(controller: MediaController): Boolean {
        val previous = controllers[controller.packageName] ?: return false
        if (previous.sessionToken == controller.sessionToken) return false
        detach(controller.packageName)
        Log.i(TAG, "MediaSession token changed for ${controller.packageName}; attaching new controller")
        return true
    }

    fun attach(
        controller: MediaController,
        callback: MediaController.Callback,
    ) {
        controller.registerCallback(callback)
        controllers[controller.packageName] = controller
        callbacks[controller.packageName] = callback
    }

    /**
     * Unregister and forget the current controller/callback for a package.
     *
     * Returns the detached controller, or null when no tracked registration existed.
     */
    fun detach(packageName: String): MediaController? {
        val controller = controllers.remove(packageName) ?: run {
            callbacks.remove(packageName)
            return null
        }
        callbacks.remove(packageName)?.let { callback ->
            try {
                controller.unregisterCallback(callback)
            } catch (failure: Exception) {
                Log.w(TAG, "MediaSession already gone for $packageName", failure)
            }
        }
        return controller
    }

    /**
     * Session-destroyed callbacks can race a replacement token. Remove only the callback that is
     * still current for this package; stale callbacks must not tear down the replacement.
     */
    fun detachIfCurrent(
        packageName: String,
        callback: MediaController.Callback,
    ): Boolean {
        if (callbacks[packageName] !== callback) return false
        detach(packageName)
        return true
    }

    fun release() {
        activeSessionsListener?.let { listener ->
            try {
                manager?.removeOnActiveSessionsChangedListener(listener)
            } catch (failure: Exception) {
                Log.w(TAG, "Could not remove MediaSession observer", failure)
            }
        }
        activeSessionsListener = null
        isInitialized = false
        manager = null

        controllers.keys.toList().forEach(::detach)
        callbacks.clear()
    }

    private fun dispatch(rawControllers: List<MediaController>) {
        // Tempo tracks at most one active token per package. Prefer the playing token when Android
        // exposes several tokens for a reconnect/cast handoff.
        val preferred =
            rawControllers
                .groupBy { it.packageName }
                .values
                .map { group ->
                    group.firstOrNull { controller ->
                        try {
                            controller.playbackState?.state == PlaybackState.STATE_PLAYING
                        } catch (_: Exception) {
                            false
                        }
                    } ?: group.first()
                }

        val now = System.currentTimeMillis()
        val (lastCount, lastTime) = lastSessionChangeLog ?: (-1 to 0L)
        if (preferred.size != lastCount || now - lastTime > SESSION_CHANGE_LOG_DEBOUNCE_MS) {
            Log.d(TAG, "Active sessions changed: ${preferred.size} sessions")
            lastSessionChangeLog = preferred.size to now
        }

        onControllersChanged(preferred)
    }

    private companion object {
        const val TAG = "MediaSessionRegistry"
        const val SESSION_CHANGE_LOG_DEBOUNCE_MS = 2_000L
    }
}
