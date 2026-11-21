package me.avinas.tempo.data.spotify

import android.util.Log
import me.avinas.tempo.data.local.dao.EnrichedMetadataDao
import me.avinas.tempo.data.local.dao.ListeningEventDao
import me.avinas.tempo.data.local.dao.UserPreferencesDao
import me.avinas.tempo.data.local.entities.AlbumArtSource
import me.avinas.tempo.data.local.entities.EnrichedMetadata
import me.avinas.tempo.data.local.entities.EnrichmentStatus
import me.avinas.tempo.data.local.entities.ListeningEvent
import me.avinas.tempo.data.local.entities.SpotifyEnrichmentStatus
import me.avinas.tempo.data.local.entities.Track
import me.avinas.tempo.data.local.entities.UserPreferences
import me.avinas.tempo.data.remote.spotify.PlayHistoryObject
import me.avinas.tempo.data.remote.spotify.SpotifyApi
import me.avinas.tempo.data.remote.spotify.SpotifyAuthManager
import me.avinas.tempo.data.remote.spotify.SpotifyTrack
import me.avinas.tempo.data.repository.ArtistLinkingService
import me.avinas.tempo.data.repository.ListeningRepository
import me.avinas.tempo.data.repository.TrackRepository
import me.avinas.tempo.data.repository.TrackResolver
import me.avinas.tempo.data.repository.StatsRepository
import me.avinas.tempo.data.repository.RefreshCoordinator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for importing recently played tracks from Spotify Web API.
 */
@Singleton
class SpotifyImportService @Inject constructor(
    private val spotifyApi: SpotifyApi,
    private val authManager: SpotifyAuthManager,
    private val trackRepository: TrackRepository,
    private val listeningRepository: ListeningRepository,
    private val userPreferencesDao: UserPreferencesDao,
    private val artistLinkingService: ArtistLinkingService,
    private val enrichedMetadataDao: EnrichedMetadataDao,
    private val trackResolver: TrackResolver,
    private val listeningEventDao: ListeningEventDao,
    private val statsRepository: StatsRepository,
    private val refreshCoordinator: RefreshCoordinator,
) {
    // One manual sync and one periodic job can arrive together. Serialize the
    // full import (including track creation and cursor movement), not just the
    // insert transaction, so both calls cannot independently create tracks.
    private val importMutex = Mutex()

    companion object {
        private const val TAG = "SpotifyImportService"
        
        // Rate limit protection
        private const val CURSOR_OVERLAP_MS = 30 * 60 * 1000L
        private const val MAX_PAGES_PER_SYNC = SpotifyRecentHistoryPagingPolicy.MAX_PAGES
        private const val PAGE_SIZE = SpotifyRecentHistoryPagingPolicy.PAGE_LIMIT
        private const val WRITE_BATCH_SIZE = 100
        
        // Source identifier for imported events
        const val IMPORT_SOURCE = "com.spotify.music.import"
        
        // Default assumed duration percentage for imported plays
        // We can't know actual play duration from recently-played API
    }
    
    /**
     * Result of an import operation.
     */
    data class ImportResult(
        val tracksImported: Int,
        val eventsCreated: Int,
        val duplicatesSkipped: Int,
        val errors: List<String>,
        val newCursor: String? = null // For incremental polling
    ) {
        val isSuccess: Boolean get() = errors.isEmpty()
        val totalProcessed: Int get() = eventsCreated + duplicatesSkipped
        
        companion object {
            fun notConnected() = ImportResult(
                tracksImported = 0,
                eventsCreated = 0,
                duplicatesSkipped = 0,
                errors = listOf("Spotify not connected")
            )
            
            fun error(message: String) = ImportResult(
                tracksImported = 0,
                eventsCreated = 0,
                duplicatesSkipped = 0,
                errors = listOf(message)
            )
        }
    }
    
    /**
     * Import recently played tracks. With no [afterCursor] this fetches the last [limit] plays
     * (the API caps it at 50); with one, only plays newer than that Unix timestamp.
     */
    suspend fun importRecentlyPlayed(
        limit: Int = 50,
        afterCursor: Long? = null,
        onProgress: ((imported: Int, total: Int) -> Unit)? = null
    ): ImportResult = importMutex.withLock {
        importRecentlyPlayedLocked(limit, afterCursor, onProgress)
    }

    private suspend fun importRecentlyPlayedLocked(
        limit: Int,
        afterCursor: Long?,
        onProgress: ((imported: Int, total: Int) -> Unit)?
    ): ImportResult {
        // A Spotify account must be connected; Spotify itself controls API allowlisting.
        if (!isAvailable()) {
            Log.w(TAG, "Cannot import: Spotify account not connected")
            return ImportResult.notConnected()
        }
        
        // Get access token
        val accessToken = authManager.getValidAccessToken()
        if (accessToken == null) {
            Log.e(TAG, "Failed to get valid access token")
            return ImportResult.error("Failed to authenticate with Spotify")
        }
        
        val authHeader = "Bearer $accessToken"
        
        return try {
            Log.i(TAG, "Starting Spotify import (limit=$limit, afterCursor=$afterCursor)")
            
            // Spotify returns at most 50 items per request. Walk backward using
            // the before cursor until reaching the previous successful cursor.
            // Never supply before and after in the same request (Spotify rejects it).
            val lowerBound = afterCursor?.let { (it - CURSOR_OVERLAP_MS).coerceAtLeast(0L) }
            var before: Long? = null
            var latestSeen: Long? = null
            var pageCount = 0
            var reachedBoundary = false
            var incomplete = false
            val allItems = ArrayList<PlayHistoryObject>()
            val seenPlays = HashSet<String>()

            while (pageCount < MAX_PAGES_PER_SYNC) {
                val response = spotifyApi.getRecentlyPlayed(
                    authorization = authHeader,
                    limit = PAGE_SIZE,
                    after = if (before == null) lowerBound else null,
                    before = before,
                )
                if (!response.isSuccessful) {
                    // Do not advance cursor after partially successful pages.
                    val code = response.code()
                    Log.w(TAG, "Spotify recently-played API returned HTTP $code (page ${pageCount + 1})")
                    return when (code) {
                        401 -> ImportResult.error("Spotify session expired. Please reconnect.")
                        403 -> ImportResult.error("Spotify API error: 403 (scope or developer access unavailable)")
                        429 -> ImportResult.error("Rate limited by Spotify. Try again later.")
                        else -> ImportResult.error("Spotify API error: $code")
                    }
                }
                val page = response.body()
                    ?: return ImportResult.error("Spotify recently-played returned an empty response")
                pageCount++
                val records = page.items
                if (records.isEmpty()) {
                    reachedBoundary = true
                    break
                }
                val valid = records.mapNotNull { entry ->
                    entry.playedAtMillis.takeIf { it > 0L }?.let { it to entry }
                }
                if (valid.isEmpty()) {
                    incomplete = true
                    break
                }

                val oldest = valid.minOf { it.first }
                latestSeen = maxOf(latestSeen ?: 0L, valid.maxOf { it.first })
                valid.forEach { (timestamp, entry) ->
                    // Repeated cursor edges can return the same item. Dedup here
                    // too so progress/UI totals are not inflated within a sync.
                    if ((lowerBound == null || timestamp > lowerBound) &&
                        seenPlays.add("${entry.track.id}|${entry.playedAt}")
                    ) {
                        allItems.add(entry)
                    }
                }
                val decision = SpotifyRecentHistoryPagingPolicy.afterPage(
                    count = records.size,
                    oldestValidPlayedAt = oldest,
                    requestedBefore = before,
                    lowerBound = lowerBound,
                    pagesFetched = pageCount,
                )
                if (decision.complete) {
                    reachedBoundary = true
                    break
                }
                if (decision.incomplete) {
                    incomplete = true
                    break
                }
                before = decision.nextBefore
            }
            if (!reachedBoundary && pageCount >= MAX_PAGES_PER_SYNC) incomplete = true
            if (incomplete) {
                Log.w(TAG, "Spotify history pagination incomplete. Importing accessible items but NOT advancing cursor.")
            }
            val items = allItems.sortedBy { it.playedAtMillis }
            Log.i(TAG, "Fetched ${items.size} unique recent tracks in $pageCount Spotify page(s)")
            if (items.isEmpty()) {
                if (incomplete) {
                    return ImportResult.error("Spotify recently-played pages were malformed or inaccessible")
                }
                return ImportResult(0, 0, 0, emptyList(), latestSeen?.toString())
            }

            // Process each play history item
            var tracksImported = 0
            var eventsCreated = 0
            var eventsReconciled = 0
            var duplicatesSkipped = 0
            val errors = mutableListOf<String>()
            if (incomplete) {
                errors.add(
                    "Spotify's history window is incomplete; imported available items only. " +
                        "Use the extended-history export to recover older plays."
                )
            }

            val resolvedTracks = mutableMapOf<String, Long>()
            val pending = ArrayList<ListeningEvent>(WRITE_BATCH_SIZE)
            suspend fun flush() {
                if (pending.isEmpty()) return
                try {
                    val result = listeningEventDao.insertAllBatchedWithDedup(pending)
                    eventsCreated += result.newlyAdded
                    eventsReconciled += result.replaced
                    duplicatesSkipped += result.skipped
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Could not save a batch of Spotify plays", e)
                    errors.add("Failed to save a batch of Spotify plays. Retry to recover it.")
                } finally {
                    pending.clear()
                }
            }
            items.forEachIndexed { index, playHistory ->
                try {
                    if (playHistory.playedAtMillis <= 0L) {
                        duplicatesSkipped++
                    } else {
                        val track = playHistory.track
                        val spotifyId = track.id.takeIf { it.isNotBlank() }
                        val cachedId = spotifyId?.let(resolvedTracks::get)
                        val trackId = cachedId ?: resolveTrack(track).let { resolution ->
                            if (resolution.isNewTrack) tracksImported++
                            if (spotifyId != null) resolvedTracks[spotifyId] = resolution.trackId
                            resolution.trackId
                        }
                        pending.add(createListeningEvent(playHistory, trackId))
                        if (pending.size == WRITE_BATCH_SIZE) flush()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing Spotify play", e)
                    errors.add("Failed to import a Spotify play")
                }
                onProgress?.invoke(index + 1, items.size)
            }
            flush()

            // Save cursor for future polling
            val newCursor = latestSeen?.let { latest ->
                maxOf(latest, afterCursor ?: 0L).toString()
            }
            if (newCursor != null && errors.isEmpty() && !incomplete) {
                saveCursor(newCursor)
            }
            
            // Imported plays are persisted directly via the DAO (not through the
            // local MusicTrackingManager). Drop the long-lived aggregate caches
            // once per sync, after all inserts, so Home/History/Stats show the
            // new plays immediately instead of serving stale five-minute totals.
            if (eventsCreated > 0 || eventsReconciled > 0) {
                statsRepository.invalidateCache()
                refreshCoordinator.notifyNewTrackRecorded()
            }

            Log.i(TAG, "Import complete: $tracksImported new tracks, $eventsCreated new plays, $eventsReconciled upgraded plays, $duplicatesSkipped duplicates")
            
            ImportResult(
                tracksImported = tracksImported,
                eventsCreated = eventsCreated,
                duplicatesSkipped = duplicatesSkipped,
                errors = errors,
                newCursor = newCursor
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            ImportResult.error("Import failed: ${e.message}")
        }
    }
    
    /**
     * Result of processing a single play history item.
     */
    private suspend fun resolveTrack(spotifyTrack: SpotifyTrack): TrackResolver.Resolution {
        // Resolve against local MediaSession/Last.fm/Spotify tracks as well as API
        // tracks. A Spotify ID is stronger identity than a fuzzy title match.
        val resolution = trackResolver.resolve(
            TrackResolver.Query(
                title = spotifyTrack.name,
                artist = spotifyTrack.allArtistNames,
                album = spotifyTrack.album.name,
                albumArtUrl = spotifyTrack.album.mediumImageUrl ?: spotifyTrack.album.largeImageUrl,
                duration = spotifyTrack.durationMs.takeIf { it > 0L },
                spotifyId = spotifyTrack.id,
            )
        )
        if (resolution.track.primaryArtistId == null) {
            try {
                artistLinkingService.linkArtistsForTrack(resolution.track)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Unable to link imported Spotify artists", e)
            }
        }
        if (resolution.isNewTrack) createEnrichedMetadata(resolution.trackId, spotifyTrack)

        return resolution
    }

    private fun createListeningEvent(playHistory: PlayHistoryObject, trackId: Long): ListeningEvent {
        // /recently-played supplies an exact played_at timestamp and catalog duration
        // but NO observed listening duration or completion percentage. Do not
        // invent an 80% play or add fictional listening minutes.
        return ListeningEvent(
            track_id = trackId,
            timestamp = playHistory.playedAtMillis,
            playDuration = 0L,
            completionPercentage = 0,
            source = IMPORT_SOURCE,
            wasSkipped = false,
            isReplay = false,
            estimatedDurationMs = playHistory.track.durationMs.takeIf { it > 0L },
            pauseCount = 0,
            sessionId = null,
            endTimestamp = null
        )
    }

    /**
     * Store Spotify ids, album art with its source, and artist ids. `spotifyEnrichmentStatus` is
     * ENRICHED so this data is never fetched again, while `enrichmentStatus` stays PENDING for the
     * genres and tags other providers supply.
     */
    private suspend fun createEnrichedMetadata(trackId: Long, spotifyTrack: SpotifyTrack) {
        try {
            val primaryArtistId = spotifyTrack.artists.firstOrNull()?.id
            val allArtistIds = spotifyTrack.artists.map { it.id }.joinToString(",")
            val primaryArtistName = spotifyTrack.artists.firstOrNull()?.name
            
            val metadata = EnrichedMetadata(
                trackId = trackId,
                // Spotify IDs
                spotifyId = spotifyTrack.id,
                spotifyArtistId = primaryArtistId,
                spotifyArtistIds = allArtistIds,
                spotifyVerifiedArtist = primaryArtistName,
                spotifyTrackUrl = spotifyTrack.externalUrls.spotify,
                spotifyPreviewUrl = spotifyTrack.previewUrl,
                // Album info
                albumTitle = spotifyTrack.album.name,
                releaseDate = spotifyTrack.album.releaseDate,
                releaseYear = spotifyTrack.album.releaseDate.take(4).toIntOrNull(),
                releaseType = spotifyTrack.album.albumType,
                // Album art with source tracking
                albumArtUrl = spotifyTrack.album.mediumImageUrl,
                albumArtUrlSmall = spotifyTrack.album.smallImageUrl,
                albumArtUrlLarge = spotifyTrack.album.largeImageUrl,
                albumArtSource = if (spotifyTrack.album.mediumImageUrl != null) AlbumArtSource.SPOTIFY else AlbumArtSource.NONE,
                // Track info
                trackDurationMs = spotifyTrack.durationMs,
                isrc = spotifyTrack.externalIds?.isrc,
                previewUrl = spotifyTrack.previewUrl,
                // Artist info (from simplified artist object - images fetched separately)
                artistName = primaryArtistName,
                // Enrichment status - mark as having Spotify data but needing additional enrichment
                enrichmentStatus = EnrichmentStatus.PENDING, // Still needs genre/tags from Last.fm etc.
                spotifyEnrichmentStatus = SpotifyEnrichmentStatus.ENRICHED,
                spotifyLastAttempt = System.currentTimeMillis(),
                // Timestamp
                cacheTimestamp = System.currentTimeMillis()
            )
            
            enrichedMetadataDao.upsertFromAutomaticEnrichment(metadata)
            Log.d(TAG, "Created enriched metadata for track $trackId (spotify: ${spotifyTrack.id})")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create enriched metadata for track $trackId", e)
            // Non-fatal - track is still imported, metadata can be enriched later
        }
    }
    
    /**
     * Save cursor for future incremental polling.
     */
    private suspend fun saveCursor(cursor: String) {
        try {
            val prefs = userPreferencesDao.getSync() ?: UserPreferences()
            userPreferencesDao.upsert(
                prefs.copy(
                    spotifyImportCursor = cursor,
                    lastSpotifyImportTimestamp = System.currentTimeMillis()
                )
            )
            Log.d(TAG, "Saved import cursor: $cursor")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save import cursor", e)
        }
    }
    
    /**
     * Get saved cursor for incremental polling.
     * Returns the cursor as a Long timestamp for the Spotify API's 'after' parameter.
     */
    suspend fun getSavedCursor(): Long? {
        return try {
            val prefs = userPreferencesDao.getSync()
            // spotifyImportCursor is the actual cursor from Spotify API
            // It's stored as a String but represents a Unix timestamp
            prefs?.spotifyImportCursor?.toLongOrNull()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get saved cursor", e)
            null
        }
    }
    
    /**
     * Check if import is available (Spotify connected and has valid token).
     */
    fun isAvailable(): Boolean {
        // DISABLED: Spotify API requires allowlisted users (max 5) in development mode.
        // Use SpotifyJsonImportService for JSON data export imports instead.
        return authManager.isConnected()
    }
}
