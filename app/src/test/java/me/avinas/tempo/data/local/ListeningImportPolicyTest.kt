package me.avinas.tempo.data.local

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Pure policy checks; instrumented Room tests cover the actual DB transaction. */
class ListeningImportPolicyTest {
    private val start = 1_700_100_000_000L

    private fun play(
        source: String,
        startMs: Long = start,
        durationMs: Long = 180_000L,
        endMs: Long? = startMs + durationMs,
        session: String? = null,
    ) = ListeningImportPolicy.Play(startMs, endMs, durationMs, source, session)

    @Test fun overlappingMeasuredExportsWithShiftedStartsAreOnePlay() {
        val spotify = play("com.spotify.music.import.json")
        val apple = play("com.apple.android.music.import.csv", startMs = start + 80_000L)
        assertNotNull(ListeningImportPolicy.matchDistance(spotify, apple))
        assertNotNull(ListeningImportPolicy.matchDistance(apple, spotify))
    }

    @Test fun measuredAdjacentReplaysRemainIndependent() {
        val first = play("com.spotify.music.import.json", durationMs = 45_000L)
        val second = play("com.apple.android.music.import.csv",
            startMs = start + 45_000L, durationMs = 45_000L)
        assertNull(ListeningImportPolicy.matchDistance(first, second))
    }

    @Test fun partialOverlapBelowHalfDoesNotEraseARealReplay() {
        val spotify = play("com.spotify.music.import.json")
        val next = play("com.apple.android.music.import.csv", startMs = start + 170_000L)
        assertNull(ListeningImportPolicy.matchDistance(spotify, next))
    }

    @Test fun minutePrecisionSpotifyEndIsMatchedAgainstExtendedEnd() {
        val older = play("com.spotify.music.import.json", startMs = start - 180_000L,
            endMs = start)
        val extended = play("com.spotify.music.import.json",
            startMs = start + 41_000L - 180_000L,
            endMs = start + 41_000L)
        assertNotNull(ListeningImportPolicy.matchDistance(older, extended))
    }

    @Test fun secondPrecisionSpotifySessionsWithOverlappingIntervalsAreNotAutoMerged() {
        val source = "com.spotify.music.import.json"
        val unrounded = start + 123L
        val first = play(source, startMs = unrounded)
        val another = play(source, startMs = unrounded + 40_000L)
        assertNull(ListeningImportPolicy.matchDistance(first, another))
    }

    @Test fun unrelatedObservedSessionsFromIndependentDevicesStaySeparate() {
        val android = play("com.spotify.music", session = "android-1")
        val desktop = play("desktop:SpotifyWeb", session = "desktop-1")
        assertNull(ListeningImportPolicy.matchDistance(android, desktop))
    }

    @Test fun distinctSessionIdsKeepRapidRepeats() {
        val first = play("com.spotify.music", session = "session-1")
        val second = play("com.spotify.music", startMs = start + 1_000L,
            session = "session-2")
        assertNull(ListeningImportPolicy.matchDistance(first, second))
    }

    @Test fun spotifyApiEndAnchorMatchesAnExportWithoutDurationEstimation() {
        val unknownApi = play("com.spotify.music.import",
            startMs = start + 180_000L, durationMs = 0L, endMs = null)
        val exported = play("com.spotify.music.import.json")
        assertNotNull(ListeningImportPolicy.matchDistance(unknownApi, exported))
    }
}
