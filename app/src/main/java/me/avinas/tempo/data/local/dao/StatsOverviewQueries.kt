package me.avinas.tempo.data.local.dao

import androidx.room.Query
import me.avinas.tempo.data.local.LocalTimeSql
import me.avinas.tempo.data.stats.*

/** Query family inherited by [StatsDao]; split for reviewability without changing the Room API. */
interface StatsOverviewQueries {
    // Combined Stats Query (Single Round Trip)

    /**
     * Total time, play count and distinct track/artist/album counts in one round trip.
     */
    @Query(
        """
        SELECT 
            COALESCE(SUM(le.playDuration), 0) as total_time_ms,
            COUNT(le.id) as play_count,
            COUNT(DISTINCT le.track_id) as unique_tracks,
            COUNT(DISTINCT t.artist) as unique_artists,
            COUNT(DISTINCT CASE WHEN t.album IS NOT NULL THEN t.album END) as unique_albums
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        WHERE le.timestamp >= :startTime AND le.timestamp <= :endTime
    """,
    )
    suspend fun getCombinedBasicStats(
        startTime: Long,
        endTime: Long,
    ): CombinedBasicStats

    // Basic Stats Queries

    @Query(
        """
        SELECT COALESCE(SUM(playDuration), 0) 
        FROM listening_events 
        WHERE timestamp >= :startTime AND timestamp <= :endTime
    """,
    )
    suspend fun getTotalListeningTime(
        startTime: Long,
        endTime: Long,
    ): Long

    @Query(
        """
        SELECT COUNT(*) 
        FROM listening_events 
        WHERE timestamp >= :startTime AND timestamp <= :endTime
    """,
    )
    suspend fun getTotalPlayCount(
        startTime: Long,
        endTime: Long,
    ): Int

    @Query(
        """
        SELECT COUNT(DISTINCT track_id) 
        FROM listening_events 
        WHERE timestamp >= :startTime AND timestamp <= :endTime
    """,
    )
    suspend fun getUniqueTracksCount(
        startTime: Long,
        endTime: Long,
    ): Int

    @Query(
        """
        SELECT COUNT(DISTINCT t.artist) 
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        WHERE le.timestamp >= :startTime AND le.timestamp <= :endTime
    """,
    )
    suspend fun getUniqueArtistsCount(
        startTime: Long,
        endTime: Long,
    ): Int

    /**
     * Excludes singles - only counts actual albums and EPs.
     * Also requires an album to have 2+ distinct tracked songs, so a best-of
     * or compilation that only captured 1 of your scrobbled songs doesn't count.
     */
    @Query(
        """
        SELECT COUNT(*) FROM (
            SELECT t.album
            FROM listening_events le
            INNER JOIN tracks t ON le.track_id = t.id
            LEFT JOIN enriched_metadata em ON t.id = em.track_id
            WHERE le.timestamp >= :startTime AND le.timestamp <= :endTime
            AND t.album IS NOT NULL
            AND (em.release_type IS NULL OR LOWER(em.release_type) != 'single')
            GROUP BY t.album
            HAVING COUNT(DISTINCT t.id) > 1
        )
    """,
    )
    suspend fun getUniqueAlbumsCount(
        startTime: Long,
        endTime: Long,
    ): Int
}
