package me.avinas.tempo.data.importexport

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import me.avinas.tempo.data.local.entities.ListeningEvent
import me.avinas.tempo.data.local.entities.ScrobbleArchive
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RestoreStagingStoreInstrumentedTest {
    private lateinit var file: File
    private lateinit var store: RestoreStagingStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        file = File(context.cacheDir, "restore-stage-test-${UUID.randomUUID()}.db")
        store = RestoreStagingStore(file)
    }

    @After
    fun tearDown() {
        runCatching { store.closeAndDelete() }
        file.delete()
    }

    @Test
    fun roundTripsListeningAndArchiveRowsWithoutJsonReparse() = runBlocking {
        val event =
            ListeningEvent(
                track_id = 42L,
                timestamp = 1_700_000_000_123L,
                playDuration = 183_456L,
                completionPercentage = 87,
                source = "com.spotify.music",
                wasSkipped = false,
                isReplay = true,
                estimatedDurationMs = 210_000L,
                pauseCount = 2,
                sessionId = "session-1",
                endTimestamp = 1_700_000_183_579L,
                totalPauseDurationMs = 2_100L,
                seekCount = 1,
                positionUpdatesCount = 17,
                wasInterrupted = true,
                volumeLevel = 7,
                contentFingerprint = "ignored-on-restore",
                tzOffsetMinutes = 330,
            )
        val blob = byteArrayOf(1, 2, 3, 4, 5)
        val archive =
            ScrobbleArchive(
                trackHash = "hash",
                trackTitle = "Track",
                artistName = "Artist",
                artistNameNormalized = "artist",
                albumName = null,
                musicbrainzId = "mbid",
                timestampsBlob = blob,
                playCount = 3,
                firstScrobble = 1_600_000_000_000L,
                lastScrobble = 1_700_000_000_000L,
                albumArtUrl = "https://example.invalid/art.jpg",
                wasLoved = true,
                importId = 8L,
                createdAt = 11L,
                updatedAt = 12L,
            )

        store.writeBatch {
            store.stage(event)
            store.stage(archive)
        }

        val events = mutableListOf<ListeningEvent>()
        store.forEachEvent { events.add(it) }
        assertEquals(1, events.size)
        assertEquals(event.copy(id = 0, contentFingerprint = null), events.single())

        val archives = mutableListOf<ScrobbleArchive>()
        store.forEachArchiveRow { archives.add(it) }
        assertEquals(1, archives.size)
        val restored = archives.single()
        assertEquals(0L, restored.id)
        assertEquals(archive.trackHash, restored.trackHash)
        assertEquals(archive.trackTitle, restored.trackTitle)
        assertEquals(archive.artistName, restored.artistName)
        assertEquals(archive.artistNameNormalized, restored.artistNameNormalized)
        assertNull(restored.albumName)
        assertEquals(archive.musicbrainzId, restored.musicbrainzId)
        assertEquals(archive.playCount, restored.playCount)
        assertEquals(archive.firstScrobble, restored.firstScrobble)
        assertEquals(archive.lastScrobble, restored.lastScrobble)
        assertEquals(archive.albumArtUrl, restored.albumArtUrl)
        assertEquals(archive.wasLoved, restored.wasLoved)
        assertEquals(archive.importId, restored.importId)
        assertEquals(archive.createdAt, restored.createdAt)
        assertEquals(archive.updatedAt, restored.updatedAt)
        assertArrayEquals(blob, restored.timestampsBlob)
    }

    @Test
    fun bulkWriteRemainsReadableAcrossInternalCommitRotation() = runBlocking {
        store.writeBatch {
            repeat(5_001) { index ->
                store.stage(
                    ListeningEvent(
                        track_id = 1L,
                        timestamp = 1_700_000_000_000L + index,
                        playDuration = 60_000L,
                        completionPercentage = 50,
                        source = "test",
                    ),
                )
            }
        }

        var count = 0
        var lastTimestamp = 0L
        store.forEachEvent {
            count++
            lastTimestamp = it.timestamp
        }

        assertEquals(5_001, count)
        assertEquals(1_700_000_005_000L, lastTimestamp)
    }
}
