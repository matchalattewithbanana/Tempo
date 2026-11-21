package me.avinas.tempo.data.local.dao

import androidx.room.Query
import me.avinas.tempo.data.local.LocalTimeSql
import me.avinas.tempo.data.stats.*

/** Query family inherited by [StatsDao]; split without changing SQL or repository call sites. */
interface StatsPartialArtistQueries {
    // Partial Match Artist Queries (for split multi-artist entries)

    /** Partial match, so "Artist1, Artist2" strings are counted too. Case-insensitive. */
    @Query(
        """
        SELECT COUNT(le.id) 
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        WHERE LOWER(t.artist) = LOWER(:artistName) OR LOWER(t.artist) LIKE '%' || LOWER(:artistName) || '%'
    """,
    )
    suspend fun getArtistPlayCountByPartialMatch(artistName: String): Int

    @Query(
        """
        SELECT COALESCE(SUM(le.playDuration), 0)
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        WHERE LOWER(t.artist) = LOWER(:artistName) OR LOWER(t.artist) LIKE '%' || LOWER(:artistName) || '%'
    """,
    )
    suspend fun getArtistTotalTimeByPartialMatch(artistName: String): Long

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
        WHERE LOWER(t.artist) = LOWER(:artistName) OR LOWER(t.artist) LIKE '%' || LOWER(:artistName) || '%'
        GROUP BY t.id
        ORDER BY play_count DESC
        LIMIT :limit
    """,
    )
    suspend fun getTopTracksForArtistPartialMatch(
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
        WHERE LOWER(t.artist) = LOWER(:artistName) OR LOWER(t.artist) LIKE '%' || LOWER(:artistName) || '%'
    """,
    )
    suspend fun getArtistFirstListenPartialMatch(artistName: String): FirstListen?

    /**
     * Excludes singles - only counts actual albums and EPs.
     * Uses case-insensitive matching.
     */
    @Query(
        """
        SELECT COUNT(DISTINCT t.album)
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        LEFT JOIN enriched_metadata em ON t.id = em.track_id
        WHERE (LOWER(t.artist) = LOWER(:artistName) OR LOWER(t.artist) LIKE '%' || LOWER(:artistName) || '%')
              AND t.album IS NOT NULL AND t.album != ''
              AND (em.release_type IS NULL OR LOWER(em.release_type) != 'single')
    """,
    )
    suspend fun getArtistUniqueAlbumsPlayedPartialMatch(artistName: String): Int

    @Query(
        """
        SELECT COUNT(DISTINCT t.id)
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        WHERE LOWER(t.artist) = LOWER(:artistName) OR LOWER(t.artist) LIKE '%' || LOWER(:artistName) || '%'
    """,
    )
    suspend fun getArtistUniqueTracksPlayedPartialMatch(artistName: String): Int

    @Query(
        """
        SELECT MIN(le.timestamp) as first_listened, MAX(le.timestamp) as last_listened
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        WHERE LOWER(t.artist) = LOWER(:artistName) OR LOWER(t.artist) LIKE '%' || LOWER(:artistName) || '%'
    """,
    )
    suspend fun getArtistListeningDatesPartialMatch(artistName: String): ArtistListeningDates?

    @Query(
        """
        SELECT CAST(strftime('%H', ${LocalTimeSql.LOCAL_DATETIME_LE}) AS INTEGER) as hour
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        WHERE LOWER(t.artist) = LOWER(:artistName) OR LOWER(t.artist) LIKE '%' || LOWER(:artistName) || '%'
        GROUP BY hour
        ORDER BY COUNT(*) DESC
        LIMIT 1
    """,
    )
    suspend fun getArtistPeakListeningHourPartialMatch(artistName: String): Int?

    /**
     * Excludes singles - only returns actual albums and EPs.
     * Uses case-insensitive matching.
     */
    @Query(
        """
        SELECT 
            t.album,
            t.artist,
            COALESCE(MAX(NULLIF(em.album_art_url, '')), MAX(NULLIF(t.album_art_url, ''))) as album_art_url,
            COUNT(le.id) as play_count,
            SUM(le.playDuration) as total_time_ms,
            COUNT(DISTINCT t.id) as unique_tracks
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        LEFT JOIN enriched_metadata em ON t.id = em.track_id
        WHERE (LOWER(t.artist) = LOWER(:artistName) OR LOWER(t.artist) LIKE '%' || LOWER(:artistName) || '%')
              AND t.album IS NOT NULL AND t.album != ''
              AND (em.release_type IS NULL OR LOWER(em.release_type) != 'single')
        GROUP BY t.album
        HAVING COUNT(DISTINCT t.id) > 1
        ORDER BY play_count DESC
        LIMIT :limit
    """,
    )
    suspend fun getTopAlbumsForArtistPartialMatch(
        artistName: String,
        limit: Int,
    ): List<TopAlbum>
}
