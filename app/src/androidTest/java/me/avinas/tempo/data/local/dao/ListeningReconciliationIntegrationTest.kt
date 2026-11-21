package me.avinas.tempo.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import me.avinas.tempo.data.local.AppDatabase
import me.avinas.tempo.data.local.entities.ListeningEvent
import me.avinas.tempo.data.local.entities.Track
import me.avinas.tempo.data.repository.RoomListeningRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies local + Spotify API write order using actual Room transactions. */
@RunWith(AndroidJUnit4::class)
class ListeningReconciliationIntegrationTest {
    private lateinit var database: AppDatabase
    private lateinit var context: Context

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After fun tearDown() {
        database.close()
    }

    private suspend fun trackId(): Long = database.trackDao().insert(
        Track(
            title = "Same Song", artist = "Same Artist",
            album = null, duration = 180_000L, albumArtUrl = null,
            spotifyId = "spotify-track-1", musicbrainzId = null,
        )
    )

    private fun apiPlay(trackId: Long) = ListeningEvent(
        track_id = trackId,
        timestamp = 1_700_000_180_000L, // Spotify records the END of a play.
        playDuration = 0L, // Unknown, not zero observed music.
        completionPercentage = 0, // Unknown, not a verified skip.
        source = "com.spotify.music.import",
        wasSkipped = false,
    )

    private fun localPlay(trackId: Long) = ListeningEvent(
        track_id = trackId,
        timestamp = 1_700_000_120_000L, // Local records the START.
        playDuration = 60_000L,
        completionPercentage = 40,
        source = "com.spotify.music",
        wasSkipped = false,
        endTimestamp = 1_700_000_180_000L,
        sessionId = "test-live-session",
    )

    private fun repository() = RoomListeningRepository(
        dao = database.listeningEventDao(),
        trackDao = database.trackDao(),
        manualContentMarkDao = database.manualContentMarkDao(),
        enrichedMetadataDao = database.enrichedMetadataDao(),
        context = context,
    )

    @Test fun apiFirstThenLiveReplacesUnknownDuration() = runBlocking {
        val id = trackId()
        val dao = database.listeningEventDao()
        assertEquals(1, dao.insertAllBatchedWithDedup(listOf(apiPlay(id))).inserted)
        assertTrue(repository().insert(localPlay(id)) > 0L)

        val rows = dao.getEventsForTrack(id)
        assertEquals(1, rows.size)
        assertEquals("com.spotify.music", rows.single().source)
        assertEquals(60_000L, rows.single().playDuration)
    }

    @Test fun liveFirstThenApiKeepsLocalTime() = runBlocking {
        val id = trackId()
        val dao = database.listeningEventDao()
        assertTrue(repository().insert(localPlay(id)) > 0L)
        assertEquals(0, dao.insertAllBatchedWithDedup(listOf(apiPlay(id))).inserted)
        assertEquals(1, dao.getEventsForTrack(id).size)
    }

    @Test fun duplicateLiveCompletionIsNotRetriedAsFailedInsert() = runBlocking {
        val id = trackId()
        val live = localPlay(id)
        val listening = repository()
        assertTrue(listening.insert(live) > 0L)
        assertEquals(0L, listening.insert(live.copy()))
        assertEquals(1, database.listeningEventDao().getEventsForTrack(id).size)
    }

    @Test fun importedUnknownCompletionIsNotCountedAsSkipped() = runBlocking {
        val id = trackId()
        database.listeningEventDao().insertAllBatchedWithDedup(listOf(apiPlay(id)))
        assertEquals(0, database.statsDao().getSkipsCount(0L, Long.MAX_VALUE))
    }
    @Test fun historicalRepairOnlyResetsUnsupportedSpotifyApiValues() = runBlocking {
        val id = trackId()
        val dao = database.listeningEventDao()
        // Simulate an older importer that guessed a full song's listening
        // duration from the Spotify recently-played catalog duration.
        val badApi = apiPlay(id).copy(playDuration = 180_000L, completionPercentage = 100)
        val live = localPlay(id).copy(timestamp = badApi.timestamp - 1_000_000L,
            endTimestamp = badApi.timestamp - 940_000L)
        dao.insert(badApi)
        dao.insert(live)

        assertEquals(1, dao.countUnverifiableSpotifyApiPlays())
        assertEquals(1, dao.clearUnverifiableSpotifyApiEstimates())
        assertEquals(0, dao.countUnverifiableSpotifyApiPlays())

        val apiAfter = dao.getEventsForTrack(id).single { it.source == "com.spotify.music.import" }
        assertEquals(0L, apiAfter.playDuration)
        assertEquals(0, apiAfter.completionPercentage)
        assertEquals(id, apiAfter.track_id)
        assertEquals(badApi.timestamp, apiAfter.timestamp)
        assertEquals(60_000L, dao.getEventsForTrack(id)
            .single { it.source == "com.spotify.music" }.playDuration)
    }

    @Test fun distinctFastRepeatsAreSeparateSessions() = runBlocking {
        val id = trackId()
        val listening = repository()
        val first = localPlay(id)
        val repeated = first.copy(
            timestamp = first.timestamp + 2_000L,
            endTimestamp = first.endTimestamp!! + 2_000L,
            sessionId = "second-distinct-session",
        )
        assertTrue(listening.insert(first) > 0L)
        assertTrue(listening.insert(repeated) > 0L)
        assertEquals(2, database.listeningEventDao().getEventsForTrack(id).size)
    }

    @Test fun concurrentDesktopAndAndroidListeningAreIndependent() = runBlocking {
        val id = trackId()
        val listening = repository()
        val phone = localPlay(id)
        val desktop = phone.copy(
            source = "desktop:SpotifyWeb",
            sessionId = "desktop-session",
            playDuration = 60_000L,
        )
        assertTrue(listening.insert(phone) > 0L)
        assertTrue(listening.insert(desktop) > 0L)
        assertEquals(2, database.listeningEventDao().getEventsForTrack(id).size)
    }

    @Test fun measuredAdjacentPlaysStaySeparateAcrossProviders() = runBlocking {
        val id = trackId()
        val dao = database.listeningEventDao()
        val first = localPlay(id).copy(
            source = "com.deezer.music.import.xlsx", playDuration = 20_000L,
            endTimestamp = localPlay(id).timestamp + 20_000L, sessionId = null,
        )
        val second = localPlay(id).copy(
            source = "com.spotify.music.import.json",
            timestamp = first.timestamp + 21_000L,
            endTimestamp = first.timestamp + 81_000L, sessionId = null,
        )
        assertEquals(1, dao.insertAllBatchedWithDedup(listOf(first)).inserted)
        assertEquals(1, dao.insertAllBatchedWithDedup(listOf(second)).inserted)
        assertEquals(2, dao.getEventsForTrack(id).size)
    }

    @Test fun closestImportIsReplacedAndEarlierRepeatSurvives() = runBlocking {
        val id = trackId()
        val dao = database.listeningEventDao()
        val live = localPlay(id)
        val close = live.copy(source = "fm.last.import", timestamp = live.timestamp - 5_000L,
            playDuration = 0L, endTimestamp = null, sessionId = null)
        val earlier = close.copy(timestamp = live.timestamp - 45_000L)
        dao.insertAllBatchedWithDedup(listOf(earlier, close))

        val result = dao.insertAllBatchedWithDedup(listOf(live))
        assertEquals(1, result.replaced)
        assertEquals(setOf(earlier.timestamp, live.timestamp), dao.getEventsForTrack(id).map { it.timestamp }.toSet())
    }

    @Test fun spotifyExportMatchesRecentlyPlayedByEndTime() = runBlocking {
        val id = trackId()
        val dao = database.listeningEventDao()
        val recent = apiPlay(id)
        val exported = recent.copy(timestamp = recent.timestamp - 180_000L, endTimestamp = recent.timestamp,
            source = "com.spotify.music.import.json", playDuration = 180_000L)
        dao.insertAllBatchedWithDedup(listOf(recent))
        val result = dao.insertAllBatchedWithDedup(listOf(exported))
        assertEquals(1, result.replaced)
        assertEquals(exported.source, dao.getEventsForTrack(id).single().source)
    }

    @Test fun importDurationIsBoundedByElapsedTimeAndRetryIsIdempotent() = runBlocking {
        val id = trackId()
        val dao = database.listeningEventDao()
        val imported = localPlay(id).copy(source = "com.deezer.music.import.xlsx",
            playDuration = 240_000L, completionPercentage = 100, sessionId = null)
        assertEquals(1, dao.insertAllBatchedWithDedup(listOf(imported)).inserted)
        assertEquals(1, dao.insertAllBatchedWithDedup(listOf(imported)).skipped)
        val row = dao.getEventsForTrack(id).single()
        assertEquals(60_000L, row.playDuration)
        assertEquals(25, row.completionPercentage)
    }

    @Test fun youtubeReimportRecognizesTheOriginalActivityTimestamp() = runBlocking {
        val id = trackId()
        val dao = database.listeningEventDao()
        val end = localPlay(id).endTimestamp!!
        val unknown = ListeningEvent(track_id = id, timestamp = end, endTimestamp = end,
            playDuration = 0L, completionPercentage = 0, source = "com.google.android.apps.youtube.music.import.json")
        val legacy = unknown.copy(timestamp = end - 210_000L, playDuration = 210_000L)
        dao.insertAllBatchedWithDedup(listOf(legacy))
        assertEquals(1, dao.insertAllBatchedWithDedup(listOf(unknown)).skipped)
        val repeat = unknown.copy(timestamp = end + 2_000L, endTimestamp = end + 2_000L)
        assertEquals(1, dao.insertAllBatchedWithDedup(listOf(repeat)).inserted)
        assertEquals(2, dao.getEventsForTrack(id).size)
    }

    @Test fun longOffsetMeasuredOverlapFromDifferentExportsDoesNotInflateHistory() = runBlocking {
        val id = trackId()
        val dao = database.listeningEventDao()
        val start = 1_700_100_000_000L
        val spotify = ListeningEvent(
            track_id = id, timestamp = start, endTimestamp = start + 180_000,
            playDuration = 180_000, completionPercentage = 0,
            source = "com.spotify.music.import.json",
        )
        val apple = spotify.copy(
            timestamp = start + 82_000, endTimestamp = start + 262_000,
            source = "com.apple.android.music.import.csv",
        )
        assertEquals(1, dao.insertAllBatchedWithDedup(listOf(spotify)).inserted)
        assertEquals(1, dao.insertAllBatchedWithDedup(listOf(apple)).skipped)
        assertEquals(1, dao.getEventsForTrack(id).size)
    }

    @Test fun adjacentMeasuredCrossSourceSessionsRemainDistinct() = runBlocking {
        val id = trackId()
        val dao = database.listeningEventDao()
        val start = 1_700_100_000_000L
        val spotify = ListeningEvent(
            track_id = id, timestamp = start, endTimestamp = start + 40_000,
            playDuration = 40_000, completionPercentage = 0,
            source = "com.spotify.music.import.json",
        )
        val apple = spotify.copy(
            timestamp = start + 40_000, endTimestamp = start + 80_000,
            source = "com.apple.android.music.import.csv",
        )
        assertEquals(1, dao.insertAllBatchedWithDedup(listOf(spotify)).inserted)
        assertEquals(1, dao.insertAllBatchedWithDedup(listOf(apple)).inserted)
        assertEquals(2, dao.getEventsForTrack(id).size)
    }

    @Test fun duplicateAcrossEquivalentTrackIdsIsReconciled() = runBlocking {
        val id = trackId()
        val siblingId = database.trackDao().insert(
            Track(
                title = "Same Song", artist = "Same Artist", album = null,
                duration = 180_000L, albumArtUrl = null,
                spotifyId = null, musicbrainzId = null,
            )
        )
        val dao = database.listeningEventDao()
        val start = 1_700_100_000_000L
        val lastFm = ListeningEvent(
            track_id = id, timestamp = start, playDuration = 0,
            completionPercentage = 0, source = "fm.last.import",
        )
        val spotify = ListeningEvent(
            track_id = siblingId, timestamp = start + 2_000,
            endTimestamp = start + 182_000, playDuration = 180_000,
            completionPercentage = 0, source = "com.spotify.music.import.json",
        )
        assertEquals(1, dao.insertAllBatchedWithDedup(listOf(lastFm)).inserted)
        val result = dao.insertAllBatchedWithDedup(listOf(spotify))
        assertEquals(1, result.replaced)
        assertEquals(0, result.newlyAdded)
        assertEquals(1, dao.getCount())
        assertEquals(spotify.source, dao.getEventsForTrack(siblingId).single().source)
    }

    @Test fun differentKnownProviderIdsDoNotFalselyCollapseTracks() = runBlocking {
        val id = trackId()
        val other = database.trackDao().insert(
            Track(
                title = "Same Song", artist = "Same Artist", album = null,
                duration = 180_000L, albumArtUrl = null,
                spotifyId = "different-spotify-recording", musicbrainzId = null,
            )
        )
        val dao = database.listeningEventDao()
        val start = 1_700_100_000_000L
        val first = ListeningEvent(
            track_id = id, timestamp = start, endTimestamp = start + 180_000,
            playDuration = 180_000, completionPercentage = 0,
            source = "com.spotify.music.import.json",
        )
        val second = first.copy(track_id = other, source = "com.deezer.music.import.xlsx")
        assertEquals(1, dao.insertAllBatchedWithDedup(listOf(first)).inserted)
        assertEquals(1, dao.insertAllBatchedWithDedup(listOf(second)).inserted)
        assertEquals(2, dao.getCount())
    }

    @Test fun skipAndReplaceAreDifferentAndPreserveEventPrimaryKeys() = runBlocking {
        val id = trackId()
        val dao = database.listeningEventDao()
        val start = 1_700_100_000_000L
        val original = ListeningEvent(
            track_id = id, timestamp = start, endTimestamp = start + 120_000,
            playDuration = 120_000, completionPercentage = 0,
            source = "com.spotify.music.import.json", wasSkipped = false,
        )
        assertEquals(1, dao.insertAllBatchedWithDedup(listOf(original)).inserted)
        val before = dao.getEventsForTrack(id).single()
        val backup = original.copy(
            // Simulates an export created on a different DB, where stored fingerprints
            // and primary keys describe that DB rather than the local database.
            id = 999L, contentFingerprint = "stale-source-fingerprint",
            wasSkipped = true,
        )
        assertEquals(1, dao.insertAllBatchedWithDedup(listOf(backup)).skipped)
        assertEquals(false, dao.getEventsForTrack(id).single().wasSkipped)
        val replaced = dao.insertAllBatchedWithDedup(
            listOf(backup), replaceExistingOnMatch = true,
        )
        assertEquals(0, replaced.inserted)
        assertEquals(1, replaced.replaced)
        assertEquals(1, replaced.updated)
        assertEquals(0, replaced.newlyAdded)
        val after = dao.getEventsForTrack(id).single()
        assertEquals(before.id, after.id)
        assertTrue(after.wasSkipped)
        assertEquals(1, dao.getCount())
    }

    @Test fun explicitRestoreReplaceNeverDowngradesObservedSession() = runBlocking {
        val id = trackId()
        val dao = database.listeningEventDao()
        val live = localPlay(id)
        assertEquals(1, dao.insertAllBatchedWithDedup(listOf(live)).inserted)
        val imported = live.copy(
            source = "com.spotify.music.import.json", sessionId = null,
        )
        val result = dao.insertAllBatchedWithDedup(
            listOf(imported), replaceExistingOnMatch = true,
        )
        assertEquals(0, result.inserted)
        assertEquals(1, dao.getEventsForTrack(id).size)
        assertEquals(live.source, dao.getEventsForTrack(id).single().source)
    }

    @Test fun equivalentTrackIdsInsideTheSameBatchDoNotBothCount() = runBlocking {
        val id = trackId()
        val siblingId = database.trackDao().insert(
            Track(
                title = "Same Song", artist = "Same Artist", album = null,
                duration = 180_000L, albumArtUrl = null,
                spotifyId = null, musicbrainzId = null,
            )
        )
        val start = 1_700_100_000_000L
        val fromSpotify = ListeningEvent(
            track_id = id, timestamp = start,
            endTimestamp = start + 180_000,
            playDuration = 180_000, completionPercentage = 0,
            source = "com.spotify.music.import.json",
        )
        val fromLastFm = fromSpotify.copy(
            track_id = siblingId, source = "fm.last.import",
            timestamp = start + 3_000, endTimestamp = null,
            playDuration = 0,
        )
        val dao = database.listeningEventDao()
        val result = dao.insertAllBatchedWithDedup(listOf(fromLastFm, fromSpotify))
        assertEquals(1, result.inserted)
        assertEquals(1, result.skipped)
        assertEquals(1, dao.getCount())
    }

}
