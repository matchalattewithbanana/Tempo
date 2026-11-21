package me.avinas.tempo.service

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Durable lifecycle boundary for active playback sessions.
 *
 * The tracking service owns live PlaybackSession objects; this journal owns every persistence
 * invariant around them: previous-run detection before marking the new run active, serialized
 * checkpoints, immediate + periodic autosave, and clearing recovery state only after recovery
 * or a successful clean-shutdown flush.
 */
internal class ActiveSessionJournal(
    context: Context,
    private val scope: CoroutineScope,
) {
    private val persistence = SessionPersistence(context.applicationContext)
    private val checkpointMutex = Mutex()
    private var autoSaveJob: Job? = null

    /**
     * Starts a service run and returns sessions only when the previous run was unclean.
     * The old marker is read before this run is marked active; reversing that order would make
     * every startup look like a crash recovery.
     */
    suspend fun beginRun(): List<SessionState> {
        val needsRecovery = persistence.wasUncleanShutdown()
        val recovered =
            if (needsRecovery) {
                persistence.loadSessions()
            } else {
                // A clean previous run should have cleared this already. Clear defensively before
                // marking the new run active so a crash during startup cannot resurrect stale data.
                persistence.clearSessions()
                emptyList()
            }
        persistence.markServiceActive()
        return recovered
    }

    suspend fun recoveryHandled() {
        checkpointMutex.withLock {
            persistence.clearSessions()
        }
    }

    fun ensureAutoSave(sessions: () -> Collection<PlaybackSession>) {
        if (autoSaveJob?.isActive == true) return
        autoSaveJob =
            scope.launch {
                // First checkpoint is immediate; the interval is only for ongoing progress.
                checkpoint(sessions())
                while (isActive) {
                    delay(AUTO_SAVE_INTERVAL_MS)
                    val snapshot = sessions()
                    if (snapshot.isEmpty()) break
                    checkpoint(snapshot)
                }
            }
    }

    fun stopAutoSaveAndCheckpoint(sessions: () -> Collection<PlaybackSession>) {
        // Null means this idle state was already finalized. Avoid a SharedPreferences commit on
        // every notification/lifecycle update while nothing is playing.
        val job = autoSaveJob ?: return
        job.cancel()
        autoSaveJob = null
        scope.launch { checkpoint(sessions()) }
    }

    fun cancelAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = null
    }

    suspend fun checkpoint(sessions: Collection<PlaybackSession>): Boolean =
        checkpointMutex.withLock {
            try {
                val snapshot =
                    sessions.associate { session ->
                        session.sessionId to session.recoverySnapshot()
                    }
                persistSnapshot(snapshot)
                true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Log.w(TAG, "Failed to checkpoint active sessions", failure)
                false
            }
        }

    /**
     * Persists an already-frozen snapshot, used by teardown after PlaybackSession clocks stop.
     */
    suspend fun checkpointSnapshot(snapshot: Map<String, SessionState>) {
        checkpointMutex.withLock {
            persistSnapshot(snapshot)
        }
    }

    /**
     * Call only after final listening events and the manager's pending queue are durable.
     */
    suspend fun completeCleanShutdown() {
        checkpointMutex.withLock {
            persistence.clearSessions()
            persistence.markServiceInactive()
        }
    }

    private suspend fun persistSnapshot(snapshot: Map<String, SessionState>) {
        if (snapshot.isEmpty()) {
            persistence.clearSessions()
        } else {
            persistence.saveSessions(snapshot)
        }
    }

    private companion object {
        const val TAG = "ActiveSessionJournal"
        const val AUTO_SAVE_INTERVAL_MS = 30_000L
    }
}
