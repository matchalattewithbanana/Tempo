package me.avinas.tempo.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Durable storage for [MusicTrackingManager]'s offline event queue.
 *
 * Events land in the offline queue only when a database insert failed. Keeping
 * them on disk means an OEM kill, crash, or reboot can no longer discard plays
 * that were already recorded but not yet persisted — they are restored into
 * the queue on the next process start and retried.
 *
 * Uses SharedPreferences + Moshi, matching [SessionPersistence].
 */
class OfflineEventStore(context: Context) {

    companion object {
        private const val TAG = "OfflineEventStore"
        private const val PREFS_NAME = "tempo_offline_event_queue"
        private const val KEY_EVENTS = "offline_events"
        // This key is intentionally NOT cleared by normal queue saves. It
        // preserves the raw bytes of malformed pending history for recovery.
        private const val KEY_QUARANTINED = "offline_events_corrupt_backup"
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val listType = Types.newParameterizedType(List::class.java, PendingEvent::class.java)
    private val adapter = moshi.adapter<List<PendingEvent>>(listType)

    /**
     * Persist the full offline queue (replaces whatever was stored before).
     * An empty collection removes the stored entry.
     */
    @Synchronized fun save(events: Collection<PendingEvent>) {
        try {
            if (events.isEmpty()) {
                check(prefs.edit().remove(KEY_EVENTS).commit()) { "Unable to clear offline events" }
                return
            }
            check(prefs.edit()
                .putString(KEY_EVENTS, adapter.toJson(events.toList()))
                .commit()) { "Unable to persist offline events" }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save offline events", e)
            throw e
        }
    }

    /**
     * Restore the pending queue. Never silently discard malformed stored data:
     * preserve the original JSON under a separate key *before* allowing a
     * new queue snapshot to replace it.
     *
     * An unreadable preference or failed quarantine write is fatal to queue
     * restoration; callers must avoid overwriting the original entry.
     */
    @Synchronized fun load(): List<PendingEvent> {
        val json = prefs.getString(KEY_EVENTS, null) ?: return emptyList()
        return try {
            adapter.fromJson(json) ?: error("Offline event JSON unexpectedly null")
        } catch (e: Exception) {
            val alreadyPreserved = prefs.contains(KEY_QUARANTINED)
            if (!alreadyPreserved) {
                check(prefs.edit().putString(KEY_QUARANTINED, json).commit()) {
                    "Unable to quarantine the unreadable offline event queue"
                }
            }
            Log.e(TAG, "Offline listening events could not be decoded; original queue preserved for recovery", e)
            emptyList()
        }
    }

    /** Used by tracking diagnostics to distinguish corruption from no events. */
    @Synchronized fun hasQuarantinedEvents(): Boolean =
        prefs.contains(KEY_QUARANTINED)

    @Synchronized fun clear() {
        check(prefs.edit().remove(KEY_EVENTS).commit()) { "Unable to clear offline events" }
    }
}
