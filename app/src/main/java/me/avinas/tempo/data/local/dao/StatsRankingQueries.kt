package me.avinas.tempo.data.local.dao

import androidx.room.Query
import me.avinas.tempo.data.local.LocalTimeSql
import me.avinas.tempo.data.stats.*

/** Query family inherited by [StatsDao]; split for reviewability without changing the Room API. */
interface StatsRankingQueries {
    // Top Charts - Tracks

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
        WHERE le.timestamp >= :startTime AND le.timestamp <= :endTime
        GROUP BY t.id
        ORDER BY play_count DESC, total_time_ms DESC
        LIMIT :limit OFFSET :offset
    """,
    )
    suspend fun getTopTracksByPlayCount(
        startTime: Long,
        endTime: Long,
        limit: Int,
        offset: Int,
    ): List<TopTrack>

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
        WHERE le.timestamp >= :startTime AND le.timestamp <= :endTime
        GROUP BY t.id
        ORDER BY total_time_ms DESC, play_count DESC
        LIMIT :limit OFFSET :offset
    """,
    )
    suspend fun getTopTracksByTime(
        startTime: Long,
        endTime: Long,
        limit: Int,
        offset: Int,
    ): List<TopTrack>

    /** 50/50 weighting of normalized play_count and normalized total_time. */
    @SuppressWarnings("RoomWarnings.QUERY_MISMATCH")
    @Query(
        """
        WITH stats AS (
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
                em.preview_url as preview_url
            FROM listening_events le
            INNER JOIN tracks t ON le.track_id = t.id
            LEFT JOIN enriched_metadata em ON t.id = em.track_id
            WHERE le.timestamp >= :startTime AND le.timestamp <= :endTime
            GROUP BY t.id
        ),
        max_values AS (
            SELECT 
                MAX(play_count) as max_plays,
                MAX(total_time_ms) as max_time
            FROM stats
        )
        SELECT 
            stats.*,
            -- Combined score: weighted average of normalized metrics
            -- 0.5 * (play_count / max_plays) + 0.5 * (total_time_ms / max_time)
            CASE 
                WHEN max_values.max_plays > 0 AND max_values.max_time > 0 THEN
                    (0.5 * CAST(stats.play_count AS REAL) / max_values.max_plays) + 
                    (0.5 * CAST(stats.total_time_ms AS REAL) / max_values.max_time)
                ELSE 0.0
            END as combined_score
        FROM stats
        CROSS JOIN max_values
        ORDER BY combined_score DESC, play_count DESC, total_time_ms DESC
        LIMIT :limit OFFSET :offset
    """,
    )
    suspend fun getTopTracksByCombinedScore(
        startTime: Long,
        endTime: Long,
        limit: Int,
        offset: Int,
    ): List<TopTrack>

    @Query(
        """
        SELECT COUNT(DISTINCT track_id) 
        FROM listening_events 
        WHERE timestamp >= :startTime AND timestamp <= :endTime
    """,
    )
    suspend fun getUniqueTracksPlayedCount(
        startTime: Long,
        endTime: Long,
    ): Int

    // Top Charts - Artists

    /**
     * Joins through track_artists where available, falling back to string matching for
     * unmigrated tracks.
     *
     * Image URL priority:
     * 1. artists.image_url (always correct for specific artist)
     * 2. enriched_metadata fallback ONLY when artist is PRIMARY (prevents featured artist getting main artist's image)
     */
    @Query(
        """
        SELECT 
            a.id as artist_id,
            a.name as artist,
            COUNT(le.id) as play_count,
            SUM(le.playDuration) as total_time_ms,
            COUNT(DISTINCT t.id) as unique_tracks,
            MIN(le.timestamp) as first_played,
            MAX(le.timestamp) as last_played,
            COALESCE(a.image_url, 
                (SELECT em2.spotify_artist_image_url FROM enriched_metadata em2 
                 INNER JOIN track_artists ta2 ON ta2.track_id = em2.track_id
                 WHERE ta2.artist_id = a.id
                 AND ta2.role = 'PRIMARY'
                 AND em2.spotify_artist_image_url IS NOT NULL LIMIT 1)
            ) as image_url,
            COALESCE(a.country,
                (SELECT em.artist_country FROM enriched_metadata em 
                 INNER JOIN track_artists ta3 ON ta3.track_id = em.track_id
                 WHERE ta3.artist_id = a.id
                 AND em.artist_country IS NOT NULL LIMIT 1)
            ) as country
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        INNER JOIN track_artists ta ON ta.track_id = t.id
        INNER JOIN artists a ON ta.artist_id = a.id
        WHERE le.timestamp >= :startTime AND le.timestamp <= :endTime
        GROUP BY a.id
        ORDER BY play_count DESC, total_time_ms DESC
        LIMIT :limit OFFSET :offset
    """,
    )
    suspend fun getTopArtistsByPlayCount(
        startTime: Long,
        endTime: Long,
        limit: Int,
        offset: Int,
    ): List<TopArtist>

    /**
     * Joins through track_artists where available.
     *
     * Image URL priority:
     * 1. artists.image_url (always correct for specific artist)
     * 2. enriched_metadata fallback ONLY when artist is PRIMARY (prevents featured artist getting main artist's image)
     */
    @Query(
        """
        SELECT 
            a.id as artist_id,
            a.name as artist,
            COUNT(le.id) as play_count,
            SUM(le.playDuration) as total_time_ms,
            COUNT(DISTINCT t.id) as unique_tracks,
            MIN(le.timestamp) as first_played,
            MAX(le.timestamp) as last_played,
            COALESCE(a.image_url, 
                (SELECT em2.spotify_artist_image_url FROM enriched_metadata em2 
                 INNER JOIN track_artists ta2 ON ta2.track_id = em2.track_id
                 WHERE ta2.artist_id = a.id
                 AND ta2.role = 'PRIMARY'
                 AND em2.spotify_artist_image_url IS NOT NULL LIMIT 1)
            ) as image_url,
            COALESCE(a.country,
                (SELECT em.artist_country FROM enriched_metadata em 
                 INNER JOIN track_artists ta3 ON ta3.track_id = em.track_id
                 WHERE ta3.artist_id = a.id
                 AND em.artist_country IS NOT NULL LIMIT 1)
            ) as country
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        INNER JOIN track_artists ta ON ta.track_id = t.id
        INNER JOIN artists a ON ta.artist_id = a.id
        WHERE le.timestamp >= :startTime AND le.timestamp <= :endTime
        GROUP BY a.id
        ORDER BY total_time_ms DESC, play_count DESC
        LIMIT :limit OFFSET :offset
    """,
    )
    suspend fun getTopArtistsByTime(
        startTime: Long,
        endTime: Long,
        limit: Int,
        offset: Int,
    ): List<TopArtist>

    @Query(
        """
        SELECT COUNT(DISTINCT a.id) 
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        INNER JOIN track_artists ta ON ta.track_id = t.id
        INNER JOIN artists a ON ta.artist_id = a.id
        WHERE le.timestamp >= :startTime AND le.timestamp <= :endTime
    """,
    )
    suspend fun getUniqueArtistsPlayedCount(
        startTime: Long,
        endTime: Long,
    ): Int

    @Query(
        """
        SELECT 
            t.artist,
            COUNT(le.id) as play_count,
            SUM(le.playDuration) as total_time_ms,
            COUNT(DISTINCT t.id) as unique_tracks,
            MIN(le.timestamp) as first_played,
            MAX(le.timestamp) as last_played
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        WHERE le.timestamp >= :startTime AND le.timestamp <= :endTime
        GROUP BY t.artist
    """,
    )
    suspend fun getAllArtistStatsRaw(
        startTime: Long,
        endTime: Long,
    ): List<RawArtistStats>

    @Query(
        """
        SELECT 
            a.id as artist_id,
            a.name as artist,
            COUNT(le.id) as play_count,
            SUM(le.playDuration) as total_time_ms,
            COUNT(DISTINCT t.id) as unique_tracks,
            MIN(le.timestamp) as first_played,
            MAX(le.timestamp) as last_played,
            a.image_url as image_url,
            a.country as country
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        INNER JOIN track_artists ta ON ta.track_id = t.id
        INNER JOIN artists a ON ta.artist_id = a.id
        WHERE a.id = :artistId
        AND le.timestamp >= :startTime AND le.timestamp <= :endTime
        GROUP BY a.id
    """,
    )
    suspend fun getArtistStatsById(
        artistId: Long,
        startTime: Long,
        endTime: Long,
    ): TopArtist?

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
        AND le.timestamp >= :startTime AND le.timestamp <= :endTime
        GROUP BY t.id
        ORDER BY play_count DESC
        LIMIT :limit
    """,
    )
    suspend fun getTracksByArtistId(
        artistId: Long,
        startTime: Long,
        endTime: Long,
        limit: Int,
    ): List<TopTrack>

    @Query(
        """
        SELECT DISTINCT 
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
        WHERE ta.artist_id = :artistId
        AND le.timestamp >= :startTime AND le.timestamp <= :endTime
        AND t.album IS NOT NULL AND t.album != ''
        GROUP BY t.album
        HAVING COUNT(DISTINCT t.id) > 1
        ORDER BY play_count DESC
        LIMIT :limit
    """,
    )
    suspend fun getAlbumsByArtistId(
        artistId: Long,
        startTime: Long,
        endTime: Long,
        limit: Int,
    ): List<TopAlbum>

    // Top Charts - Albums

    /**
     * Excludes Singles - only includes releases marked as Album, EP, or unknown/null release types.
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
        WHERE le.timestamp >= :startTime AND le.timestamp <= :endTime
        AND t.album IS NOT NULL AND t.album != ''
        AND (em.release_type IS NULL OR em.release_type NOT IN ('Single', 'single'))
        GROUP BY t.album, t.artist
        HAVING COUNT(DISTINCT t.id) > 1
        ORDER BY play_count DESC, total_time_ms DESC
        LIMIT :limit OFFSET :offset
    """,
    )
    suspend fun getTopAlbums(
        startTime: Long,
        endTime: Long,
        limit: Int,
        offset: Int,
    ): List<TopAlbum>

    // Top Charts - Genres (from MusicBrainz tags)

    /**
     * Both the `genres` field (Spotify) and `tags` field (MusicBrainz) are read; genres win,
     * being the more reliable source. Both are `|||`-delimited.
     */
    @Query(
        """
        SELECT 
            COALESCE(
                NULLIF(em.genres, ''),
                NULLIF(em.tags, '')
            ) as genre,
            COUNT(le.id) as play_count,
            SUM(le.playDuration) as total_time_ms,
            COUNT(DISTINCT t.artist) as unique_artists
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        INNER JOIN enriched_metadata em ON t.id = em.track_id
        WHERE le.timestamp >= :startTime AND le.timestamp <= :endTime
        AND (
            (em.genres IS NOT NULL AND em.genres != '' AND em.genres != '[]')
            OR (em.tags IS NOT NULL AND em.tags != '' AND em.tags != '[]')
        )
        GROUP BY genre
        ORDER BY play_count DESC
        LIMIT :limit
    """,
    )
    suspend fun getTopGenresRaw(
        startTime: Long,
        endTime: Long,
        limit: Int,
    ): List<TopGenre>
}
