package me.avinas.tempo.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyBroadcastIdentityTest {
    private fun track(album: String?, duration: Long) =
        SpotifyBroadcastBridge.Track("Same title", "Same artist", album, duration)

    @Test fun `same recording metadata retains state`() {
        assertFalse(SpotifyBroadcastBridge.spotifyMetadataChanged(
            track("Album", 180_000L), track("Album", 180_000L)
        ))
    }

    @Test fun `different edition invalidates playback status`() {
        assertTrue(SpotifyBroadcastBridge.spotifyMetadataChanged(
            track("Studio", 180_000L), track("Live", 180_000L)
        ))
        assertTrue(SpotifyBroadcastBridge.spotifyMetadataChanged(
            track("Album", 180_000L), track("Album", 200_000L)
        ))
    }

    @Test fun `first metadata must wait for matching playback`() {
        assertTrue(SpotifyBroadcastBridge.spotifyMetadataChanged(null, track("Album", 180_000L)))
    }
}
