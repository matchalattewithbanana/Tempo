package me.avinas.tempo.data.local.dao

import androidx.room.Query
import me.avinas.tempo.data.local.LocalTimeSql
import me.avinas.tempo.data.stats.*

/** Query family inherited by [StatsDao]; split without changing SQL or repository call sites. */
interface StatsArtistIdQueries {
    // ARTIST ID-BASED QUERIES (using track_artists junction table)
    // These provide proper relational lookups using artist IDs

    @Query(
        """
        SELECT COUNT(le.id) 
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        INNER JOIN track_artists ta ON ta.track_id = t.id
        WHERE ta.artist_id = :artistId
    """,
    )
    suspend fun getArtistPlayCountById(artistId: Long): Int

    @Query(
        """
        SELECT ta.artist_id AS artistId, COUNT(le.id) AS playCount
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        INNER JOIN track_artists ta ON ta.track_id = t.id
        WHERE ta.artist_id IN (:artistIds)
        GROUP BY ta.artist_id
    """,
    )
    suspend fun getArtistPlayCountsByIds(artistIds: List<Long>): List<ArtistPlayCountResult>

    @Query(
        """
        SELECT COALESCE(SUM(le.playDuration), 0)
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        INNER JOIN track_artists ta ON ta.track_id = t.id
        WHERE ta.artist_id = :artistId
    """,
    )
    suspend fun getArtistTotalTimeById(artistId: Long): Long

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
        INNER JOIN track_artists ta ON ta.track_id = t.id
        LEFT JOIN enriched_metadata em ON t.id = em.track_id
        WHERE ta.artist_id = :artistId
        GROUP BY t.id
        ORDER BY play_count DESC
        LIMIT :limit
    """,
    )
    suspend fun getTopTracksForArtistById(
        artistId: Long,
        limit: Int,
    ): List<TopTrack>

    @Query(
        """
        SELECT 
            t.title as name,
            MIN(le.timestamp) as first_listen_timestamp,
            'artist' as type
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        INNER JOIN track_artists ta ON ta.track_id = t.id
        INNER JOIN artists a ON ta.artist_id = a.id
        WHERE ta.artist_id = :artistId
    """,
    )
    suspend fun getArtistFirstListenById(artistId: Long): FirstListen?

    @Query(
        """
        SELECT COUNT(DISTINCT t.album)
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        INNER JOIN track_artists ta ON ta.track_id = t.id
        LEFT JOIN enriched_metadata em ON t.id = em.track_id
        WHERE ta.artist_id = :artistId AND t.album IS NOT NULL AND t.album != ''
        AND (em.release_type IS NULL OR LOWER(em.release_type) != 'single')
    """,
    )
    suspend fun getArtistUniqueAlbumsPlayedById(artistId: Long): Int

    @Query(
        """
        SELECT COUNT(DISTINCT t.id)
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        INNER JOIN track_artists ta ON ta.track_id = t.id
        WHERE ta.artist_id = :artistId
    """,
    )
    suspend fun getArtistUniqueTracksPlayedById(artistId: Long): Int

    @Query(
        """
        SELECT MIN(le.timestamp) as first_listened, MAX(le.timestamp) as last_listened
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        INNER JOIN track_artists ta ON ta.track_id = t.id
        WHERE ta.artist_id = :artistId
    """,
    )
    suspend fun getArtistListeningDatesById(artistId: Long): ArtistListeningDates?

    @Query(
        """
        SELECT CAST(strftime('%H', ${LocalTimeSql.LOCAL_DATETIME_LE}) AS INTEGER) as hour
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        INNER JOIN track_artists ta ON ta.track_id = t.id
        WHERE ta.artist_id = :artistId
        GROUP BY hour
        ORDER BY COUNT(*) DESC
        LIMIT 1
    """,
    )
    suspend fun getArtistPeakListeningHourById(artistId: Long): Int?

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
        INNER JOIN track_artists ta ON ta.track_id = t.id
        LEFT JOIN enriched_metadata em ON t.id = em.track_id
        WHERE ta.artist_id = :artistId AND t.album IS NOT NULL AND t.album != ''
        AND (em.release_type IS NULL OR LOWER(em.release_type) != 'single')
        GROUP BY t.album
        HAVING COUNT(DISTINCT t.id) > 1
        ORDER BY play_count DESC
        LIMIT :limit
    """,
    )
    suspend fun getTopAlbumsForArtistById(
        artistId: Long,
        limit: Int,
    ): List<TopAlbum>
}
