package me.avinas.tempo.data.local.dao

import androidx.room.Query
import me.avinas.tempo.data.local.LocalTimeSql
import me.avinas.tempo.data.stats.*

/** Query family inherited by [StatsDao]; split for reviewability without changing the Room API. */
interface StatsHistoryQueries {
    // History

    /** Listening history with track metadata, optionally excluding podcast/audiobook content. */
    @Query(
        """
        SELECT 
            le.id,
            le.track_id,
            le.timestamp,
            le.playDuration,
            le.completionPercentage,
            t.title,
            t.artist,
            t.album,
            t.content_type,
            COALESCE(NULLIF(em.album_art_url, ''), NULLIF(t.album_art_url, '')) as album_art_url,
            le.source
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        LEFT JOIN enriched_metadata em ON t.id = em.track_id
        WHERE 
            (:startTime IS NULL OR le.timestamp >= :startTime) AND
            (:endTime IS NULL OR le.timestamp <= :endTime) AND
            (:includeSkips = 1 OR (le.was_skipped = 0 AND (le.completionPercentage >= 30 OR (le.completionPercentage = 0 AND (le.source LIKE '%.import%' OR le.source LIKE 'desktop:%'))))) AND
            (:searchQuery IS NULL OR :searchQuery = '' OR 
                t.title LIKE '%' || :searchQuery || '%' OR 
                t.artist LIKE '%' || :searchQuery || '%' OR 
                t.album LIKE '%' || :searchQuery || '%') AND
            (:filterPodcasts = 0 OR t.content_type != 'PODCAST') AND
            (:filterAudiobooks = 0 OR t.content_type != 'AUDIOBOOK')
        ORDER BY le.timestamp DESC
        LIMIT :limit OFFSET :offset
    """,
    )
    suspend fun getHistory(
        searchQuery: String? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        includeSkips: Boolean = true,
        filterPodcasts: Boolean = true,
        filterAudiobooks: Boolean = true,
        limit: Int,
        offset: Int,
    ): List<HistoryItem>

    /**
     * Excludes Last.fm imported events: only live activity from Spotify/notification tracking,
     * for the "Recent Activity" section when a Last.fm import exists.
     */
    @Query(
        """
        SELECT 
            le.id,
            le.track_id,
            le.timestamp,
            le.playDuration,
            le.completionPercentage,
            t.title,
            t.artist,
            t.album,
            t.content_type,
            COALESCE(NULLIF(em.album_art_url, ''), NULLIF(t.album_art_url, '')) as album_art_url,
            le.source
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        LEFT JOIN enriched_metadata em ON t.id = em.track_id
        WHERE 
            le.source != 'fm.last.import' AND
            (:startTime IS NULL OR le.timestamp >= :startTime) AND
            (:endTime IS NULL OR le.timestamp <= :endTime) AND
            (:includeSkips = 1 OR (le.was_skipped = 0 AND (le.completionPercentage >= 30 OR (le.completionPercentage = 0 AND (le.source LIKE '%.import%' OR le.source LIKE 'desktop:%'))))) AND
            (:searchQuery IS NULL OR :searchQuery = '' OR 
                t.title LIKE '%' || :searchQuery || '%' OR 
                t.artist LIKE '%' || :searchQuery || '%' OR 
                t.album LIKE '%' || :searchQuery || '%') AND
            (:filterPodcasts = 0 OR t.content_type != 'PODCAST') AND
            (:filterAudiobooks = 0 OR t.content_type != 'AUDIOBOOK')
        ORDER BY le.timestamp DESC
        LIMIT :limit OFFSET :offset
    """,
    )
    suspend fun getHistoryExcludingLastFm(
        searchQuery: String? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        includeSkips: Boolean = true,
        filterPodcasts: Boolean = true,
        filterAudiobooks: Boolean = true,
        limit: Int,
        offset: Int,
    ): List<HistoryItem>

    /** Only Last.fm imported events: the "active set" (top/loved tracks) for "Last.fm History". */
    @Query(
        """
        SELECT 
            le.id,
            le.track_id,
            le.timestamp,
            le.playDuration,
            le.completionPercentage,
            t.title,
            t.artist,
            t.album,
            t.content_type,
            COALESCE(NULLIF(em.album_art_url, ''), NULLIF(t.album_art_url, '')) as album_art_url,
            le.source
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        LEFT JOIN enriched_metadata em ON t.id = em.track_id
        WHERE 
            le.source = 'fm.last.import' AND
            (:startTime IS NULL OR le.timestamp >= :startTime) AND
            (:endTime IS NULL OR le.timestamp <= :endTime) AND
            (:includeSkips = 1 OR (le.was_skipped = 0 AND (le.completionPercentage >= 30 OR (le.completionPercentage = 0 AND (le.source LIKE '%.import%' OR le.source LIKE 'desktop:%'))))) AND
            (:searchQuery IS NULL OR :searchQuery = '' OR 
                t.title LIKE '%' || :searchQuery || '%' OR 
                t.artist LIKE '%' || :searchQuery || '%' OR 
                t.album LIKE '%' || :searchQuery || '%') AND
            (:filterPodcasts = 0 OR t.content_type != 'PODCAST') AND
            (:filterAudiobooks = 0 OR t.content_type != 'AUDIOBOOK')
        ORDER BY le.timestamp DESC
        LIMIT :limit OFFSET :offset
    """,
    )
    suspend fun getHistoryLastFmOnly(
        searchQuery: String? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        includeSkips: Boolean = true,
        filterPodcasts: Boolean = true,
        filterAudiobooks: Boolean = true,
        limit: Int,
        offset: Int,
    ): List<HistoryItem>

    @Query(
        """
        SELECT COUNT(*) FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        WHERE 
            le.source = 'fm.last.import' AND
            (:startTime IS NULL OR le.timestamp >= :startTime) AND
            (:endTime IS NULL OR le.timestamp <= :endTime) AND
            (:searchQuery IS NULL OR :searchQuery = '' OR 
                t.title LIKE '%' || :searchQuery || '%' OR 
                t.artist LIKE '%' || :searchQuery || '%' OR 
                t.album LIKE '%' || :searchQuery || '%')
    """,
    )
    suspend fun countLastFmHistory(
        searchQuery: String? = null,
        startTime: Long? = null,
        endTime: Long? = null,
    ): Int

    @Query(
        """
        SELECT COUNT(*) FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        WHERE 
            le.source != 'fm.last.import' AND
            (:startTime IS NULL OR le.timestamp >= :startTime) AND
            (:endTime IS NULL OR le.timestamp <= :endTime) AND
            (:searchQuery IS NULL OR :searchQuery = '' OR 
                t.title LIKE '%' || :searchQuery || '%' OR 
                t.artist LIKE '%' || :searchQuery || '%' OR 
                t.album LIKE '%' || :searchQuery || '%')
    """,
    )
    suspend fun countLiveHistory(
        searchQuery: String? = null,
        startTime: Long? = null,
        endTime: Long? = null,
    ): Int
}
