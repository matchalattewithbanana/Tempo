package me.avinas.tempo.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalMediaMetadataAdvertisementTest {
    @Test
    fun spotifyProviderBrandingIsAnAdvertisement() {
        assertTrue(
            LocalMediaMetadata(
                title = "A message from Spotify",
                artist = "Spotify",
                album = "Spotify",
                sourcePackage = "com.spotify.music",
            ).isLikelyAdvertisement(),
        )
    }

    @Test
    fun youtubeMusicNotificationBrandingIsAnAdvertisementWithoutDuration() {
        assertTrue(
            LocalMediaMetadata(
                title = "Special offer",
                artist = "YouTube Music",
                sourcePackage = "com.google.android.apps.youtube.music",
            ).isLikelyAdvertisement(),
        )
    }

    @Test
    fun ordinaryMusicMetadataIsNotAnAdvertisement() {
        assertFalse(
            LocalMediaMetadata(
                title = "Hush",
                artist = "The Marias",
                album = "Submarine",
                durationMs = 180_000L,
                sourcePackage = "com.spotify.music",
            ).isLikelyAdvertisement(),
        )
    }
}
