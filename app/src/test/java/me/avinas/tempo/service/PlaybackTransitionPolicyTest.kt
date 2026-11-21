package me.avinas.tempo.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackTransitionPolicyTest {
    @Test
    fun notificationStartsNewSessionWhenNothingIsActive() {
        val decision =
            PlaybackTransitionPolicy.fromNotification(
                existing = null,
                title = "Hush",
                artist = "The Marias",
            )

        assertEquals(PlaybackTransitionPolicy.Action.NEW, decision.action)
        assertFalse(decision.replayDetected)
    }

    @Test
    fun notificationUpgradesUnknownArtistWithoutStartingNewSession() {
        val decision =
            PlaybackTransitionPolicy.fromNotification(
                existing = existing(title = "Hush", artist = "Unknown Artist"),
                title = "Hush",
                artist = "The Marias",
            )

        assertEquals(PlaybackTransitionPolicy.Action.UPDATE, decision.action)
    }

    @Test
    fun notificationDoesNotDowngradeKnownArtistToUnknown() {
        val decision =
            PlaybackTransitionPolicy.fromNotification(
                existing = existing(title = "Hush", artist = "The Marias"),
                title = "Hush",
                artist = "Unknown Artist",
            )

        assertEquals(PlaybackTransitionPolicy.Action.NEW, decision.action)
    }

    @Test
    fun notificationDetectsReplayWhenListeningExceedsKnownDuration() {
        val decision =
            PlaybackTransitionPolicy.fromNotification(
                existing =
                    existing(
                        title = "Hush",
                        artist = "The Marias",
                        listenedMs = 191_000L,
                        estimatedDurationMs = 180_000L,
                    ),
                title = "Hush",
                artist = "The Marias",
            )

        assertEquals(PlaybackTransitionPolicy.Action.NEW, decision.action)
        assertTrue(decision.replayDetected)
    }

    @Test
    fun mediaSessionTreatsTemporaryUnknownArtistAsSameTrack() {
        val decision =
            PlaybackTransitionPolicy.fromMediaSession(
                existing = existing(title = "Hush", artist = "The Marias"),
                title = "Hush",
                artist = "Unknown Artist",
                isPlaying = true,
                currentPositionMs = 75_000L,
                reportedDurationMs = 180_000L,
            )

        assertEquals(PlaybackTransitionPolicy.Action.UPDATE, decision.action)
    }

    @Test
    fun mediaSessionIgnoresDifferentTrackWhileNotPlaying() {
        val decision =
            PlaybackTransitionPolicy.fromMediaSession(
                existing = existing(title = "Old", artist = "Artist"),
                title = "New",
                artist = "Artist",
                isPlaying = false,
                currentPositionMs = 0L,
                reportedDurationMs = 180_000L,
            )

        assertEquals(PlaybackTransitionPolicy.Action.IGNORE, decision.action)
    }

    @Test
    fun mediaSessionDetectsPositionResetAfterMeaningfulProgress() {
        val decision =
            PlaybackTransitionPolicy.fromMediaSession(
                existing =
                    existing(
                        title = "Hush",
                        artist = "The Marias",
                        positionProgressMs = 100_000L,
                        lastKnownPositionMs = 175_000L,
                        estimatedDurationMs = 180_000L,
                    ),
                title = "Hush",
                artist = "The Marias",
                isPlaying = true,
                currentPositionMs = 1_500L,
                reportedDurationMs = 180_000L,
            )

        assertEquals(PlaybackTransitionPolicy.Action.NEW, decision.action)
        assertTrue(decision.replayDetected)
    }

    @Test
    fun mediaSessionDoesNotSplitTinyPositionReset() {
        val decision =
            PlaybackTransitionPolicy.fromMediaSession(
                existing =
                    existing(
                        title = "Hush",
                        artist = "The Marias",
                        positionProgressMs = 10_000L,
                        lastKnownPositionMs = 20_000L,
                        estimatedDurationMs = 180_000L,
                    ),
                title = "Hush",
                artist = "The Marias",
                isPlaying = true,
                currentPositionMs = 1_000L,
                reportedDurationMs = 180_000L,
            )

        assertEquals(PlaybackTransitionPolicy.Action.UPDATE, decision.action)
        assertFalse(decision.replayDetected)
    }

    private fun existing(
        title: String,
        artist: String,
        listenedMs: Long = 0L,
        positionProgressMs: Long = 0L,
        lastKnownPositionMs: Long = 0L,
        estimatedDurationMs: Long? = null,
    ) = PlaybackTransitionPolicy.Existing(
        title = title,
        artist = artist,
        listenedMs = listenedMs,
        positionProgressMs = positionProgressMs,
        lastKnownPositionMs = lastKnownPositionMs,
        estimatedDurationMs = estimatedDurationMs,
    )
}
