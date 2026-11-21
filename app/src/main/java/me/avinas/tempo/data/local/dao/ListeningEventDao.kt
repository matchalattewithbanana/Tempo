package me.avinas.tempo.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import me.avinas.tempo.data.local.EventFingerprint
import me.avinas.tempo.data.local.ListeningImportPolicy
import me.avinas.tempo.data.local.entities.ListeningEvent
import java.util.TreeMap

@Dao
interface ListeningEventDao {
    companion object {
        // SQLite variable limit is 999, ListeningEvent has ~12 columns
        const val BATCH_SIZE = 80

        const val DUPLICATE_TOLERANCE_MS = ListeningImportPolicy.SAME_SOURCE_TOLERANCE_MS
        private const val QUERY_JOIN_GAP_MS = 3_600_000L
    }

    @Query("SELECT * FROM listening_events WHERE id = :id")
    fun getById(id: Long): Flow<ListeningEvent?>

    @Query("SELECT * FROM listening_events WHERE track_id = :trackId ORDER BY timestamp DESC")
    fun eventsForTrack(trackId: Long): Flow<List<ListeningEvent>>

    @Query("SELECT * FROM listening_events WHERE track_id = :trackId ORDER BY timestamp DESC")
    suspend fun getEventsForTrack(trackId: Long): List<ListeningEvent>

    @Query("SELECT * FROM listening_events ORDER BY timestamp DESC")
    fun all(): Flow<List<ListeningEvent>>

    @Query("SELECT EXISTS(SELECT 1 FROM listening_events)")
    fun observeListeningEventWrites(): Flow<Boolean>

    @Query("SELECT * FROM listening_events ORDER BY timestamp DESC LIMIT :limit")
    fun recentEvents(limit: Int): Flow<List<ListeningEvent>>

    @Query("SELECT * FROM listening_events WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun eventsInRange(
        startTime: Long,
        endTime: Long,
    ): Flow<List<ListeningEvent>>

    @Query("SELECT * FROM listening_events WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getEventsInRange(
        startTime: Long,
        endTime: Long,
    ): List<ListeningEvent>

    /**
     * Load a bounded session page in timestamp and primary-key order.
     */
    @Query(
        """
        SELECT le.id, le.timestamp, le.playDuration, le.end_timestamp AS endTimestamp
        FROM listening_events le
        INNER JOIN tracks t ON t.id = le.track_id
        WHERE le.timestamp >= :afterTimestamp AND le.timestamp <= :endTime
            AND (le.timestamp > :afterTimestamp OR le.id > :afterId)
            AND (:filterPodcasts = 0 OR t.content_type IS NULL OR t.content_type != 'PODCAST')
            AND (:filterAudiobooks = 0 OR t.content_type IS NULL OR t.content_type != 'AUDIOBOOK')
        ORDER BY le.timestamp ASC, le.id ASC
        LIMIT :limit
        """,
    )
    suspend fun getSessionPointPage(
        afterTimestamp: Long,
        afterId: Long,
        endTime: Long,
        filterPodcasts: Boolean,
        filterAudiobooks: Boolean,
        limit: Int,
    ): List<me.avinas.tempo.data.stats.SessionPoint>

    /** Return the actual ID of an accepted, fingerprinted event after the
     * authority-aware reconciliation transaction, or null when absent. */
    @Query("SELECT id FROM listening_events WHERE content_fingerprint = :fingerprint ORDER BY id DESC LIMIT 1")
    suspend fun findIdByFingerprint(fingerprint: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: ListeningEvent): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<ListeningEvent>): List<Long>

    // A restore's explicit REPLACE choice updates exact matching events in place.
    // Preserving the primary key prevents accidental duplication of references.
    @Update
    suspend fun updateExistingForRestore(events: List<ListeningEvent>): Int

    /**
     * Check if an event already exists for this track within the tolerance window.
     * Used for deduplication during imports.
     */
    @Query(
        """
        SELECT COUNT(*) FROM listening_events 
        WHERE track_id = :trackId 
        AND timestamp BETWEEN :timestampMin AND :timestampMax
    """,
    )
    suspend fun countEventsNearTimestamp(
        trackId: Long,
        timestampMin: Long,
        timestampMax: Long,
    ): Int

    /**
     * Get all existing timestamps for a set of tracks (for batch deduplication).
     * Returns pairs of (track_id, timestamp) for efficient lookup.
     */
    @Query(
        """
        SELECT track_id, timestamp FROM listening_events 
        WHERE track_id IN (:trackIds)
        ORDER BY track_id, timestamp
    """,
    )
    suspend fun getTimestampsForTracks(trackIds: List<Long>): List<TrackTimestamp>

    /**
     * Layer 1: return the set of content fingerprints already present for the
     * given fingerprints. Used to drop exact re-import duplicates in O(1) per hit.
     */
    @Query(
        """
        SELECT DISTINCT content_fingerprint FROM listening_events
        WHERE content_fingerprint IN (:fingerprints)
    """,
    )
    suspend fun getExistingFingerprints(fingerprints: List<String>): List<String>

    @Query("SELECT * FROM listening_events WHERE content_fingerprint IN (:fingerprints)")
    suspend fun getEventsByFingerprints(fingerprints: List<String>): List<ListeningEvent>

    /** Reconcile semantically identical track rows that entered through different imports.
     * Never equate two non-null, differing provider identifiers or explicit albums.
     */
    @Query(
        """
        SELECT candidate.id FROM tracks AS candidate
        INNER JOIN tracks AS anchor ON anchor.id = :trackId
        WHERE candidate.title = anchor.title COLLATE NOCASE
          AND candidate.artist = anchor.artist COLLATE NOCASE
          AND (candidate.album IS NULL OR anchor.album IS NULL
               OR TRIM(candidate.album) = '' OR TRIM(anchor.album) = ''
               OR candidate.album = anchor.album COLLATE NOCASE)
          AND (candidate.spotify_id IS NULL OR anchor.spotify_id IS NULL
               OR candidate.spotify_id = anchor.spotify_id)
          AND (candidate.youtube_id IS NULL OR anchor.youtube_id IS NULL
               OR candidate.youtube_id = anchor.youtube_id)
          AND (candidate.musicbrainz_id IS NULL OR anchor.musicbrainz_id IS NULL
               OR candidate.musicbrainz_id = anchor.musicbrainz_id)
        """,
    )
    suspend fun getEquivalentTrackIds(trackId: Long): List<Long>

    /**
     * Layer 2: fetch a lightweight view of existing events for one track within a
     * time range, for cross-source temporal reconciliation. Bounded by the
     * (track_id, timestamp) index so it stays cheap even for large libraries.
     */
    @Query(
        """
        SELECT id, track_id, timestamp, playDuration, source, content_fingerprint, end_timestamp, session_id
        FROM listening_events
        WHERE track_id IN (:trackIds)
        AND timestamp BETWEEN :tsMin AND :tsMax
    """,
    )
    suspend fun getEventsForReconciliation(
        trackIds: List<Long>,
        tsMin: Long,
        tsMax: Long,
    ): List<ExistingEventRef>

    /**
     * Delete a batch of events by id (used to remove lower-authority events that
     * a higher-authority incoming event replaces during reconciliation).
     */
    @Query("DELETE FROM listening_events WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>): Int

    /**
     * Batch insert with automatic, source-aware deduplication. This is the single
     * chokepoint every import path (Spotify JSON, Last.fm, YouTube Music, ZIP
     * restore) routes through, so the protection lives here.
     *
     * Layer 1 — content fingerprint (idempotency): a deterministic SHA-256 of
     *   (source|track_id|timestamp|playDuration|endTimestamp) is computed for
     *   every incoming event. Any event whose fingerprint already exists in the
     *   DB (or earlier in this same batch) is dropped. This makes re-importing
     *   the same file a complete no-op, immune to timestamp drift.
     *
     * Layer 2 compares nearby plays on the same track using timing evidence and source
     * authority. Explicit playback intervals protect adjacent plays. Spotify's
     * recently-played timestamp is compared against the other recording's end.
     * Only the closest lower-authority match can be replaced.
     */
    @Transaction
    suspend fun insertAllBatchedWithDedup(
        events: List<ListeningEvent>,
        replaceExistingOnMatch: Boolean = false,
    ): InsertResult {
        if (events.isEmpty()) return InsertResult(0, 0)

        // ── Layer 1: fingerprint every incoming event ──────────────────────
        val withFp = events.map { event ->
            val normalized = ListeningImportPolicy.normalizeTiming(event)
            // Recompute after local track-id remapping and duration repair. An
            // exported fingerprint belongs to the original DB, not this one.
            normalized.copy(contentFingerprint = EventFingerprint.compute(normalized))
        }

        val incomingFps = withFp.mapNotNull { it.contentFingerprint }.distinct()
        val existingFps: Set<String> =
            if (incomingFps.isEmpty()) {
                emptySet()
            } else {
                incomingFps.chunked(900).flatMap { getExistingFingerprints(it) }.toSet()
            }

        // In SKIP mode, an exact reimport is a no-op. Under explicit REPLACE,
        // refresh changed metadata on the existing play without changing its ID.
        val existingRows = if (replaceExistingOnMatch && existingFps.isNotEmpty()) {
            existingFps.chunked(900).flatMap { getEventsByFingerprints(it) }
                .mapNotNull { old -> old.contentFingerprint?.let { it to old } }.toMap()
        } else emptyMap()
        val toUpdate = ArrayList<ListeningEvent>()
        val seenFp = HashSet<String>(incomingFps.size)
        val layer1Survivors = ArrayList<ListeningEvent>(withFp.size)
        var skipped = 0
        for (e in withFp) {
            val fp = e.contentFingerprint
            if (fp != null && !seenFp.add(fp)) {
                skipped++
            } else if (fp != null && fp in existingFps) {
                if (replaceExistingOnMatch) {
                    val old = existingRows[fp]
                    if (old != null && old.source == e.source) {
                        val candidate = e.copy(id = old.id)
                        if (candidate != old) toUpdate.add(candidate)
                    }
                }
                skipped++
            } else {
                layer1Survivors.add(e)
            }
        }
        if (toUpdate.isNotEmpty()) updateExistingForRestore(toUpdate)

        // ── Layer 2: cross-source temporal reconciliation ──────────────────
        val toInsert = ArrayList<ListeningEvent>(layer1Survivors.size)
        val toDelete = mutableSetOf<Long>()

        for ((trackId, trackEvents) in layer1Survivors.groupBy { it.track_id }) {
            val equivalentTrackIds = if (trackEvents.any { it.source.contains("import") }) {
                getEquivalentTrackIds(trackId).ifEmpty { listOf(trackId) }
            } else listOf(trackId)
            val candidates = TreeMap<Long, MutableList<Slot>>()
            fun addSlot(slot: Slot) {
                candidates.getOrPut(slot.play.timestamp) { mutableListOf() }.add(slot)
            }
            for (window in reconciliationWindows(trackEvents)) {
                for (existing in getEventsForReconciliation(equivalentTrackIds, window.first, window.last)) {
                    addSlot(Slot(
                        ListeningImportPolicy.Play(
                            existing.timestamp, existing.end_timestamp, existing.playDuration,
                            existing.source, existing.session_id,
                        ),
                        existing.id,
                    ))
                }
            }

            val incoming = trackEvents.map { it to ListeningImportPolicy.Play(it) }
                .sortedWith(compareByDescending<Pair<ListeningEvent, ListeningImportPolicy.Play>> { it.second.authority }
                    .thenBy { it.second.timestamp })

            for ((event, play) in incoming) {
                val range = reconciliationRange(play.timestamp, play.source)
                val nearby = candidates.subMap(range.first, true, range.last, true)
                var closest: Slot? = null
                var shortest = Long.MAX_VALUE
                for (slots in nearby.values) {
                    for (slot in slots) {
                        val distance = ListeningImportPolicy.matchDistance(slot.play, play) ?: continue
                        val previous = closest
                        if (previous == null || distance < shortest ||
                            (distance == shortest && slot.play.authority > previous.play.authority) ||
                            (distance == shortest && slot.play.authority == previous.play.authority &&
                                (slot.existingId ?: Long.MAX_VALUE) < (previous.existingId ?: Long.MAX_VALUE))
                        ) {
                            closest = slot
                            shortest = distance
                        }
                    }
                }

                val conflict = closest
                val existingId = conflict?.existingId
                if (conflict != null) {
                    // REPLACE is meaningful for matching plays recorded by the
                    // same source; never overwrite a higher-authority live event.
                    val canReplace = existingId != null && (
                        play.authority > conflict.play.authority ||
                            (replaceExistingOnMatch && play.authority == conflict.play.authority &&
                                play.source == conflict.play.source)
                        )
                    if (!canReplace) {
                        skipped++
                        continue
                    }
                    toDelete.add(existingId)
                    candidates[conflict.play.timestamp]?.remove(conflict)
                }
                toInsert.add(event)
                addSlot(Slot(play))
            }
        }

        // Delete the lower-authority events that were replaced.
        if (toDelete.isNotEmpty()) {
            toDelete.chunked(900).forEach { ids -> deleteByIds(ids) }
        }

        // Insert the survivors (fingerprint already set on each).
        val inserted =
            if (toInsert.isEmpty()) {
                0
            } else {
                toInsert.chunked(BATCH_SIZE).sumOf { batch -> insertAll(batch).size }
            }

        return InsertResult(inserted = inserted, skipped = skipped,
            replaced = toDelete.size + toUpdate.size)
    }

    private data class Slot(val play: ListeningImportPolicy.Play, val existingId: Long? = null)

    private fun reconciliationRange(timestamp: Long, source: String): LongRange {
        val padding = ListeningImportPolicy.searchPadding(source)
        return (timestamp.coerceAtLeast(Long.MIN_VALUE + padding) - padding)..
            (timestamp.coerceAtMost(Long.MAX_VALUE - padding) + padding)
    }

    /**
     * Read separate indexed slices for sparse imports instead of fetching all history
     * between the first and last event when a batch spans months or years.
     */
    private fun reconciliationWindows(events: List<ListeningEvent>): List<LongRange> {
        val windows = ArrayList<LongRange>()
        for (range in events.map { reconciliationRange(it.timestamp, it.source) }.sortedBy { it.first }) {
            val previous = windows.lastOrNull()
            if (previous != null && range.first <= previous.last.coerceAtMost(Long.MAX_VALUE - QUERY_JOIN_GAP_MS) + QUERY_JOIN_GAP_MS) {
                windows[windows.lastIndex] = previous.first..maxOf(previous.last, range.last)
            } else {
                windows.add(range)
            }
        }
        return windows
    }

    /**
     * Batch insert with chunking for large imports.
     */
    @Transaction
    suspend fun insertAllBatched(events: List<ListeningEvent>): List<Long> {
        val results = mutableListOf<Long>()
        events.chunked(BATCH_SIZE).forEach { batch ->
            results.addAll(insertAll(batch))
        }
        return results
    }

    /**
     * Result of a deduplicating insert operation.
     *
     * @param inserted  events newly written to the database
     * @param skipped   incoming events dropped because an equal/higher-authority
     *                  duplicate already existed (fingerprint or temporal match)
     * @param replaced  existing lower-authority events deleted because a more
     *                  trustworthy incoming event represented the same play
     *                   (cross-source reconciliation). Always 0 for same-source imports.
     */
    data class InsertResult(
        val inserted: Int,
        val skipped: Int,
        val replaced: Int = 0,
    ) {
        val total: Int get() = inserted + skipped
    }

    /**
     * Simple data class for timestamp lookup.
     */
    data class TrackTimestamp(
        val track_id: Long,
        val timestamp: Long,
    )

    /**
     * Lightweight view of an existing event used for cross-source reconciliation.
     * Field names match the underlying column names so Room can map them directly.
     */
    data class ExistingEventRef(
        val id: Long,
        val track_id: Long,
        val timestamp: Long,
        val playDuration: Long,
        val source: String,
        val content_fingerprint: String?,
        val end_timestamp: Long?,
        val session_id: String?,
    )

    /** Desktop source → count breakdown. */
    data class SourceCount(
        val source: String,
        val cnt: Int,
    )

    /** Artist name → count. */
    data class ArtistCount(
        val artist: String,
        val cnt: Int,
    )

    /** Track title + artist → count. */
    data class TrackCount(
        val title: String,
        val artist: String,
        val cnt: Int,
    )

    @Delete
    suspend fun delete(event: ListeningEvent)

    @Query("DELETE FROM listening_events WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM listening_events WHERE track_id = :trackId")
    suspend fun deleteByTrackId(trackId: Long)

    /**
     * Delete all listening events for one track.
     * Used by track merge after the events have been re-inserted against
     * the target track through the dedup pipeline.
     */
    @Query("DELETE FROM listening_events WHERE track_id = :trackId")
    suspend fun deleteEventsForTrack(trackId: Long): Int

    /**
     * Keyset page of events for backup export. Rows are fetched in id order,
     * page after page, so the full history is never materialized in memory at
     * once. Pass the last seen id as [afterId], starting at 0.
     *
     * [maxId] caps the scan at the snapshot boundary captured before the export
     * began. Live tracking keeps inserting rows during a backup; rows above the
     * boundary are excluded so the archive never contains events whose track row
     * is missing from the same snapshot (those would be silently dropped on
     * restore).
     */
    @Query("SELECT * FROM listening_events WHERE id > :afterId AND id <= :maxId ORDER BY id ASC LIMIT :limit")
    suspend fun getEventsPage(
        afterId: Long,
        maxId: Long,
        limit: Int,
    ): List<ListeningEvent>

    /** Highest event id at the moment of the call — export snapshot boundary. */
    @Query("SELECT COALESCE(MAX(id), 0) FROM listening_events")
    suspend fun getMaxEventId(): Long

    // Enhanced Engagement Queries

    /**
     * Get total skip count for a track.
     */
    @Query("SELECT COUNT(*) FROM listening_events WHERE track_id = :trackId AND was_skipped = 1")
    suspend fun getSkipCountForTrack(trackId: Long): Int

    /**
     * Get total replay count for a track.
     */
    @Query("SELECT COUNT(*) FROM listening_events WHERE track_id = :trackId AND is_replay = 1")
    suspend fun getReplayCountForTrack(trackId: Long): Int

    /**
     * Get average completion percentage for a track.
     */
    @Query("SELECT AVG(CASE WHEN completionPercentage = 0 AND was_skipped = 0 AND (source LIKE '%.import%' OR source LIKE 'desktop:%') THEN NULL ELSE completionPercentage END) FROM listening_events WHERE track_id = :trackId")
    suspend fun getAverageCompletionForTrack(trackId: Long): Float?

    /**
     * Get full play count for a track (completion >= 80%).
     */
    @Query("SELECT COUNT(*) FROM listening_events WHERE track_id = :trackId AND completionPercentage >= 80")
    suspend fun getFullPlayCountForTrack(trackId: Long): Int

    /**
     * Get last play timestamp for a track.
     */
    @Query("SELECT MAX(timestamp) FROM listening_events WHERE track_id = :trackId")
    suspend fun getLastPlayTimestampForTrack(trackId: Long): Long?

    /**
     * Get first play timestamp for a track.
     */
    @Query("SELECT MIN(timestamp) FROM listening_events WHERE track_id = :trackId")
    suspend fun getFirstPlayTimestampForTrack(trackId: Long): Long?

    /**
     * Get total play count for a specific track.
     */
    @Query("SELECT COUNT(*) FROM listening_events WHERE track_id = :trackId")
    suspend fun countByTrackId(trackId: Long): Int

    /**
     * Check if a track was recently played (within specified milliseconds).
     * Used for replay detection.
     */
    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM listening_events 
            WHERE track_id = :trackId 
            AND timestamp >= :sinceTimestamp
        )
    """,
    )
    suspend fun wasRecentlyPlayed(
        trackId: Long,
        sinceTimestamp: Long,
    ): Boolean

    /**
     * Get events by session ID.
     */
    @Query("SELECT * FROM listening_events WHERE session_id = :sessionId ORDER BY timestamp ASC")
    suspend fun getEventsBySessionId(sessionId: String): List<ListeningEvent>

    /**
     * Get total listening time in a time range.
     */
    @Query("SELECT COALESCE(SUM(playDuration), 0) FROM listening_events WHERE timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getTotalListeningTime(
        startTime: Long,
        endTime: Long,
    ): Long

    /**
     * Get skip rate for a time range.
     */
    @Query(
        """
        SELECT CAST(SUM(CASE WHEN was_skipped = 1 THEN 1 ELSE 0 END) AS FLOAT) / COUNT(*) 
        FROM listening_events 
        WHERE timestamp >= :startTime AND timestamp <= :endTime
    """,
    )
    suspend fun getSkipRate(
        startTime: Long,
        endTime: Long,
    ): Float?

    /**
     * Get average completion for a time range.
     */
    @Query("SELECT AVG(CASE WHEN completionPercentage = 0 AND was_skipped = 0 AND (source LIKE '%.import%' OR source LIKE 'desktop:%') THEN NULL ELSE completionPercentage END) FROM listening_events WHERE timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getAverageCompletion(
        startTime: Long,
        endTime: Long,
    ): Float?

    /**
     * Historical Spotify /recently-played never reported listening duration.
     * Older builds incorrectly invented play minutes and completion for this
     * exact source. Preview the count before any user-authorized correction.
     */
    @Query("""
        SELECT COUNT(*) FROM listening_events
        WHERE source = 'com.spotify.music.import'
          AND (playDuration > 0 OR completionPercentage != 0 OR was_skipped = 1)
    """)
    suspend fun countUnverifiableSpotifyApiPlays(): Int

    @Query("""
        SELECT COUNT(*) FROM listening_events
        WHERE source LIKE '%reconstructed%'
    """)
    suspend fun countReconstructedHistoryPlays(): Int

    /**
     * Opt-in lossless-for-play-count repair: keep IDs, track links, timestamps,
     * and source; clear only API fields that the endpoint never supplied.
     * Clear fingerprint so future reconciliation can rebuild it; legacy null
     * fingerprints remain supported by the temporal-dedup layer.
     */
    @Query("""
        UPDATE listening_events
        SET playDuration = 0,
            completionPercentage = 0,
            was_skipped = 0,
            content_fingerprint = NULL
        WHERE source = 'com.spotify.music.import'
          AND (playDuration > 0 OR completionPercentage != 0 OR was_skipped = 1)
    """)
    suspend fun clearUnverifiableSpotifyApiEstimates(): Int

    @Query("SELECT COUNT(*) FROM listening_events")
    suspend fun getCount(): Int

    /**
     * Delete all listening events for tracks belonging to a specific artist.
     * Returns the number of deleted rows.
     */
    @Query(
        """
        DELETE FROM listening_events 
        WHERE track_id IN (SELECT id FROM tracks WHERE LOWER(artist) = LOWER(:artistName))
    """,
    )
    suspend fun deleteByArtist(artistName: String): Int

    /**
     * Get the timestamp of the very first listening event (earliest data point).
     */
    @Query("SELECT MIN(timestamp) FROM listening_events")
    suspend fun getEarliestEventTimestamp(): Long?

    /**
     * Get total listening time excluding imported events.
     * Only counts events from actual app usage (real-time tracking).
     */
    @Query(
        """
        SELECT COALESCE(SUM(playDuration), 0) FROM listening_events 
        WHERE source NOT LIKE '%import%'
    """,
    )
    suspend fun getRealListeningTimeMs(): Long

    /**
     * Get total play count excluding imported events.
     * Only counts events from actual app usage (real-time tracking).
     */
    @Query(
        """
        SELECT COUNT(*) FROM listening_events 
        WHERE source NOT LIKE '%import%'
    """,
    )
    suspend fun getRealPlayCount(): Int

    /**
     * Counts for the anonymous daily listening-activity aggregate.
     *
     * Both return numbers only — never which track, artist or app — so the aggregate cannot
     * carry listening content even by accident. Imported history is excluded so the figure
     * reflects tracked listening rather than a one-off bulk import.
     *
     * The window is bounded rather than open-ended so one day's figure describes exactly one
     * calendar day. A rolling `>= since` window drifts with whenever the worker happened to
     * run — a 09:00 run covers 09:00-to-09:00, mixing two afternoons and double-counting
     * yesterday's tail — and it files the result under a day it does not describe.
     */
    @Query(
        """
        SELECT COUNT(*) FROM listening_events
        WHERE timestamp >= :fromMillis AND timestamp < :toMillis AND source NOT LIKE '%import%'
    """,
    )
    suspend fun getTrackedPlayCountBetween(fromMillis: Long, toMillis: Long): Int

    @Query(
        """
        SELECT COUNT(DISTINCT source) FROM listening_events
        WHERE timestamp >= :fromMillis AND timestamp < :toMillis AND source NOT LIKE '%import%'
    """,
    )
    suspend fun getDistinctTrackedSourcesBetween(fromMillis: Long, toMillis: Long): Int

    // ─── Desktop-specific stats queries ──────────────────────────────────────

    /**
     * Total number of plays received from the desktop satellite.
     */
    @Query("SELECT COUNT(*) FROM listening_events WHERE source LIKE 'desktop:%'")
    suspend fun getDesktopPlayCount(): Int

    /**
     * Total listening time (ms) from desktop sources.
     */
    @Query("SELECT COALESCE(SUM(playDuration), 0) FROM listening_events WHERE source LIKE 'desktop:%'")
    suspend fun getDesktopListeningTimeMs(): Long

    /**
     * Breakdown of desktop plays grouped by source app (e.g., "desktop:Spotify Desktop").
     * Returns source → count pairs, ordered by count descending.
     */
    @Query(
        """
        SELECT source, COUNT(*) as cnt 
        FROM listening_events 
        WHERE source LIKE 'desktop:%'
        GROUP BY source 
        ORDER BY cnt DESC
    """,
    )
    suspend fun getDesktopSourceBreakdown(): List<SourceCount>

    /**
     * Top artist played from desktop, by count.
     */
    @Query(
        """
        SELECT t.artist, COUNT(*) as cnt
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        WHERE le.source LIKE 'desktop:%'
        GROUP BY t.artist
        ORDER BY cnt DESC
        LIMIT 1
    """,
    )
    suspend fun getDesktopTopArtist(): ArtistCount?

    /**
     * Top track played from desktop, by count.
     */
    @Query(
        """
        SELECT t.title, t.artist, COUNT(*) as cnt
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        WHERE le.source LIKE 'desktop:%'
        GROUP BY t.title, t.artist
        ORDER BY cnt DESC
        LIMIT 1
    """,
    )
    suspend fun getDesktopTopTrack(): TrackCount?

    /**
     * Count of desktop plays in a given time range.
     */
    @Query(
        """
        SELECT COUNT(*) FROM listening_events 
        WHERE source LIKE 'desktop:%' 
        AND timestamp >= :startTime AND timestamp <= :endTime
    """,
    )
    suspend fun getDesktopPlayCountInRange(
        startTime: Long,
        endTime: Long,
    ): Int

    /**
     * Total desktop listening time (ms) in a given time range.
     */
    @Query(
        """
        SELECT COALESCE(SUM(playDuration), 0) FROM listening_events 
        WHERE source LIKE 'desktop:%' 
        AND timestamp >= :startTime AND timestamp <= :endTime
    """,
    )
    suspend fun getDesktopListeningTimeMsInRange(
        startTime: Long,
        endTime: Long,
    ): Long
}
