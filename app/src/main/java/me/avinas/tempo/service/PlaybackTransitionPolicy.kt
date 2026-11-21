package me.avinas.tempo.service

import me.avinas.tempo.utils.ArtistParser

/**
 * Pure policy for deciding whether an incoming playback observation updates the current session,
 * starts a new session, or should be ignored.
 *
 * Android callback synchronization and mutation stay in [MusicTrackingService]. This module owns
 * the replay/same-track rules so notification and MediaSession paths cannot drift independently.
 */
internal object PlaybackTransitionPolicy {
    enum class Action {
        UPDATE,
        NEW,
        IGNORE,
    }

    data class Existing(
        val title: String,
        val artist: String,
        val listenedMs: Long,
        val positionProgressMs: Long,
        val lastKnownPositionMs: Long,
        val estimatedDurationMs: Long?,
    )

    data class Decision(
        val action: Action,
        val replayDetected: Boolean = false,
    )

    fun fromNotification(
        existing: Existing?,
        title: String,
        artist: String,
    ): Decision {
        if (existing == null) return Decision(Action.NEW)

        val sameTrack =
            sameTrack(
                existing = existing,
                title = title,
                artist = artist,
                allowUnknownIncomingArtist = false,
            )
        if (!sameTrack) return Decision(Action.NEW)

        val replay =
            existing.estimatedDurationMs
                ?.takeIf { it > 0L }
                ?.let { existing.listenedMs > it + REPLAY_DURATION_LEEWAY_MS }
                ?: false

        return Decision(
            action = if (replay) Action.NEW else Action.UPDATE,
            replayDetected = replay,
        )
    }

    fun fromMediaSession(
        existing: Existing?,
        title: String,
        artist: String,
        isPlaying: Boolean,
        currentPositionMs: Long,
        reportedDurationMs: Long,
    ): Decision {
        if (existing == null) {
            return Decision(if (isPlaying) Action.NEW else Action.IGNORE)
        }

        val sameTrack =
            sameTrack(
                existing = existing,
                title = title,
                artist = artist,
                allowUnknownIncomingArtist = true,
            )
        if (!sameTrack) {
            return Decision(if (isPlaying) Action.NEW else Action.IGNORE)
        }

        val duration =
            existing.estimatedDurationMs
                ?: reportedDurationMs.takeIf { it > 0L }

        val replay =
            if (!isPlaying) {
                false
            } else {
                val positionWentBack =
                    existing.lastKnownPositionMs > POSITION_RESET_THRESHOLD_MS &&
                        currentPositionMs < POSITION_RESET_THRESHOLD_MS
                val completion =
                    duration
                        ?.takeIf { it > 0L }
                        ?.let { existing.positionProgressMs.toDouble() / it.toDouble() }
                        ?: 0.0
                val resetAfterMeaningfulPlay =
                    positionWentBack &&
                        existing.positionProgressMs >= MIN_PROGRESS_FOR_REPLAY_MS &&
                        completion >= MIN_COMPLETION_FOR_REPLAY
                val playedExceedsDuration =
                    duration
                        ?.takeIf { it > 0L }
                        ?.let { existing.positionProgressMs > it + REPLAY_DURATION_LEEWAY_MS }
                        ?: false
                resetAfterMeaningfulPlay || playedExceedsDuration
            }

        return Decision(
            action =
                when {
                    replay -> Action.NEW
                    sameTrack -> Action.UPDATE
                    isPlaying -> Action.NEW
                    else -> Action.IGNORE
                },
            replayDetected = replay,
        )
    }

    private fun sameTrack(
        existing: Existing,
        title: String,
        artist: String,
        allowUnknownIncomingArtist: Boolean,
    ): Boolean {
        if (existing.title != title) return false
        if (existing.artist == artist) return true

        val existingUnknown = ArtistParser.isUnknownArtist(existing.artist)
        val incomingUnknown = ArtistParser.isUnknownArtist(artist)

        if (existingUnknown && !incomingUnknown) return true
        if (allowUnknownIncomingArtist && !existingUnknown && incomingUnknown) return true
        return false
    }

    private const val POSITION_RESET_THRESHOLD_MS = 5_000L
    private const val MIN_PROGRESS_FOR_REPLAY_MS = 30_000L
    private const val MIN_COMPLETION_FOR_REPLAY = 0.5
    private const val REPLAY_DURATION_LEEWAY_MS = 10_000L
}
