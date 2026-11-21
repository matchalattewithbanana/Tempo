package me.avinas.tempo.data.local.dao

import androidx.room.Query
import me.avinas.tempo.data.local.LocalTimeSql
import me.avinas.tempo.data.stats.*

/** Query family inherited by [StatsDao]; split without changing SQL or repository call sites. */
interface StatsSingleEntityQueries {
    // Single Entity Stats

    @Query("SELECT COUNT(*) FROM listening_events WHERE track_id = :trackId")
    suspend fun getTrackPlayCount(trackId: Long): Int

    @Query("SELECT COALESCE(SUM(playDuration), 0) FROM listening_events WHERE track_id = :trackId")
    suspend fun getTrackTotalTime(trackId: Long): Long

    @Query("SELECT MIN(timestamp) FROM listening_events WHERE track_id = :trackId")
    suspend fun getTrackFirstPlayed(trackId: Long): Long?

    @Query("SELECT MAX(timestamp) FROM listening_events WHERE track_id = :trackId")
    suspend fun getTrackLastPlayed(trackId: Long): Long?

    // Optimized Track Engagement Queries

    /**
     * Reads the indexed `was_skipped`, `is_replay` and `pause_count` columns in one pass.
     */
    @Query(
        """
        SELECT 
            :trackId as track_id,
            COUNT(*) as play_count,
            COALESCE(SUM(playDuration), 0) as total_listening_time_ms,
            AVG(CASE WHEN completionPercentage = 0 AND was_skipped = 0 AND (source LIKE '%.import%' OR source LIKE 'desktop:%') THEN NULL ELSE completionPercentage END) as average_completion_percent,
            SUM(CASE WHEN completionPercentage >= 80 THEN 1 ELSE 0 END) as full_plays_count,
            SUM(CASE WHEN completionPercentage >= 30 AND completionPercentage < 80 THEN 1 ELSE 0 END) as partial_plays_count,
            SUM(CASE WHEN was_skipped = 1 OR (completionPercentage < 30 AND NOT (completionPercentage = 0 AND was_skipped = 0 AND (source LIKE '%.import%' OR source LIKE 'desktop:%'))) THEN 1 ELSE 0 END) as skips_count,
            SUM(CASE WHEN is_replay = 1 THEN 1 ELSE 0 END) as replay_count,
            MAX(timestamp) as last_played_timestamp,
            MIN(timestamp) as first_played_timestamp,
            AVG(CAST(pause_count AS FLOAT)) as average_pause_count,
            SUM(pause_count) as total_pause_count,
            COUNT(DISTINCT session_id) as unique_sessions_count
        FROM listening_events 
        WHERE track_id = :trackId
    """,
    )
    suspend fun getTrackEngagementStats(trackId: Long): TrackEngagementStats?

    @Query("SELECT COUNT(*) FROM listening_events WHERE track_id = :trackId AND (was_skipped = 1 OR (completionPercentage < 30 AND NOT (completionPercentage = 0 AND was_skipped = 0 AND (source LIKE '%.import%' OR source LIKE 'desktop:%'))))")
    suspend fun getTrackSkipCount(trackId: Long): Int

    @Query("SELECT COUNT(*) FROM listening_events WHERE track_id = :trackId AND is_replay = 1")
    suspend fun getTrackReplayCount(trackId: Long): Int

    @Query("SELECT COUNT(*) FROM listening_events WHERE track_id = :trackId AND completionPercentage >= 80")
    suspend fun getTrackFullPlaysCount(trackId: Long): Int

    @Query("SELECT AVG(CASE WHEN completionPercentage = 0 AND was_skipped = 0 AND (source LIKE '%.import%' OR source LIKE 'desktop:%') THEN NULL ELSE completionPercentage END) FROM listening_events WHERE track_id = :trackId")
    suspend fun getTrackAverageCompletion(trackId: Long): Float?

    @Query("SELECT AVG(CAST(pause_count AS FLOAT)) FROM listening_events WHERE track_id = :trackId")
    suspend fun getTrackAveragePauseCount(trackId: Long): Float?

    @Query("SELECT COALESCE(SUM(pause_count), 0) FROM listening_events WHERE track_id = :trackId")
    suspend fun getTrackTotalPauseCount(trackId: Long): Int

    @Query(
        """
        SELECT 
            strftime('%Y-%m-%d', ${LocalTimeSql.LOCAL_DATETIME}) as date,
            COUNT(*) as play_count,
            SUM(playDuration) as total_time_ms,
            1 as unique_tracks,
            1 as unique_artists
        FROM listening_events
        WHERE track_id = :trackId AND timestamp >= :startTime AND timestamp <= :endTime
        GROUP BY date
        ORDER BY date ASC
    """,
    )
    suspend fun getTrackListeningHistory(
        trackId: Long,
        startTime: Long,
        endTime: Long,
    ): List<DailyListening>

    @Query(
        """
        SELECT COUNT(le.id) 
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        WHERE t.artist = :artistName
    """,
    )
    suspend fun getArtistPlayCount(artistName: String): Int

    @Query(
        """
        SELECT COALESCE(SUM(le.playDuration), 0)
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        WHERE t.artist = :artistName
    """,
    )
    suspend fun getArtistTotalTime(artistName: String): Long

    @SuppressWarnings("RoomWarnings.QUERY_MISMATCH")
    @Query(
        """
        SELECT 
            t.id as track_id,
            t.title,
            t.artist,
            t.album,
            COALESCE(NULLIF(em.album_art_url, ''), NULLIF(t.album_art_url, '')) as album_art_url,
            COUNT(le.id) as play_count,
            SUM(le.playDuration) as total_time_ms,
            MIN(le.timestamp) as first_played,
            MAX(le.timestamp) as last_played,
            em.preview_url as preview_url,
            NULL as combined_score
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        LEFT JOIN enriched_metadata em ON t.id = em.track_id
        WHERE t.artist = :artistName
        GROUP BY t.id
        ORDER BY play_count DESC
        LIMIT :limit
    """,
    )
    suspend fun getTopTracksForArtist(
        artistName: String,
        limit: Int,
    ): List<TopTrack>

    @Query(
        """
        SELECT 
            t.artist as name,
            MIN(le.timestamp) as first_listen_timestamp,
            'artist' as type
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        WHERE t.artist = :artistName
    """,
    )
    suspend fun getArtistFirstListen(artistName: String): FirstListen?
}
