package me.avinas.tempo.data.local.dao

import androidx.room.Query
import me.avinas.tempo.data.local.LocalTimeSql
import me.avinas.tempo.data.stats.*

/** Query family inherited by [StatsDao]; split without changing SQL or repository call sites. */
interface StatsFilteredQueries {
    // Content-Type Filtered Stats Queries
    // These exclude podcasts/audiobooks from stats when user has marked them

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
            AND (:filterPodcasts = 0 OR t.content_type IS NULL OR t.content_type != 'PODCAST')
            AND (:filterAudiobooks = 0 OR t.content_type IS NULL OR t.content_type != 'AUDIOBOOK')
        GROUP BY t.id
        ORDER BY play_count DESC, total_time_ms DESC
        LIMIT :limit OFFSET :offset
    """,
    )
    suspend fun getTopTracksByPlayCountFiltered(
        startTime: Long,
        endTime: Long,
        filterPodcasts: Boolean,
        filterAudiobooks: Boolean,
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
            AND (:filterPodcasts = 0 OR t.content_type IS NULL OR t.content_type != 'PODCAST')
            AND (:filterAudiobooks = 0 OR t.content_type IS NULL OR t.content_type != 'AUDIOBOOK')
        GROUP BY t.id
        ORDER BY total_time_ms DESC, play_count DESC
        LIMIT :limit OFFSET :offset
    """,
    )
    suspend fun getTopTracksByTimeFiltered(
        startTime: Long,
        endTime: Long,
        filterPodcasts: Boolean,
        filterAudiobooks: Boolean,
        limit: Int,
        offset: Int,
    ): List<TopTrack>

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
                AND (:filterPodcasts = 0 OR t.content_type IS NULL OR t.content_type != 'PODCAST')
                AND (:filterAudiobooks = 0 OR t.content_type IS NULL OR t.content_type != 'AUDIOBOOK')
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
    suspend fun getTopTracksByCombinedScoreFiltered(
        startTime: Long,
        endTime: Long,
        filterPodcasts: Boolean,
        filterAudiobooks: Boolean,
        limit: Int,
        offset: Int,
    ): List<TopTrack>

    @Query(
        """
        SELECT COUNT(DISTINCT t.id) 
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        WHERE le.timestamp >= :startTime AND le.timestamp <= :endTime
            AND (:filterPodcasts = 0 OR t.content_type IS NULL OR t.content_type != 'PODCAST')
            AND (:filterAudiobooks = 0 OR t.content_type IS NULL OR t.content_type != 'AUDIOBOOK')
    """,
    )
    suspend fun getUniqueTracksPlayedCountFiltered(
        startTime: Long,
        endTime: Long,
        filterPodcasts: Boolean,
        filterAudiobooks: Boolean,
    ): Int

    /**
     * Raw rows for splitting multi-artist entries and re-aggregating. Capped at the top N
     * artists by play count.
     */
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
            AND (:filterPodcasts = 0 OR t.content_type IS NULL OR t.content_type != 'PODCAST')
            AND (:filterAudiobooks = 0 OR t.content_type IS NULL OR t.content_type != 'AUDIOBOOK')
        GROUP BY t.artist
        ORDER BY play_count DESC
        LIMIT :maxArtists
    """,
    )
    suspend fun getAllArtistStatsRawFiltered(
        startTime: Long,
        endTime: Long,
        filterPodcasts: Boolean,
        filterAudiobooks: Boolean,
        maxArtists: Int = 500,
    ): List<RawArtistStats>

    @Query(
        """
        SELECT 
            t.album,
            t.artist,
            COALESCE(MAX(NULLIF(em.album_art_url, '')), MAX(NULLIF(t.album_art_url, ''))) as album_art_url,
            COUNT(le.id) as play_count,
            SUM(le.playDuration) as total_time_ms,
            COUNT(DISTINCT t.id) as unique_tracks
        $RANKED_ALBUM_GROUPS
        ORDER BY play_count DESC, total_time_ms DESC, t.album ASC, t.artist ASC
        LIMIT :limit OFFSET :offset
    """,
    )
    suspend fun getTopAlbumsByPlayCountFiltered(
        startTime: Long,
        endTime: Long,
        filterPodcasts: Boolean,
        filterAudiobooks: Boolean,
        limit: Int,
        offset: Int,
    ): List<TopAlbum>

    @Query(
        """
        SELECT 
            t.album,
            t.artist,
            COALESCE(MAX(NULLIF(em.album_art_url, '')), MAX(NULLIF(t.album_art_url, ''))) as album_art_url,
            COUNT(le.id) as play_count,
            SUM(le.playDuration) as total_time_ms,
            COUNT(DISTINCT t.id) as unique_tracks
        $RANKED_ALBUM_GROUPS
        ORDER BY play_count DESC, total_time_ms DESC, t.album ASC, t.artist ASC
        LIMIT :limit OFFSET :offset
    """,
    )
    suspend fun getTopAlbumsFiltered(
        startTime: Long,
        endTime: Long,
        filterPodcasts: Boolean,
        filterAudiobooks: Boolean,
        limit: Int,
        offset: Int,
    ): List<TopAlbum>

    @Query(
        """
        SELECT 
            t.album,
            t.artist,
            COALESCE(MAX(NULLIF(em.album_art_url, '')), MAX(NULLIF(t.album_art_url, ''))) as album_art_url,
            COUNT(le.id) as play_count,
            SUM(le.playDuration) as total_time_ms,
            COUNT(DISTINCT t.id) as unique_tracks
        $RANKED_ALBUM_GROUPS
        ORDER BY total_time_ms DESC, play_count DESC, t.album ASC, t.artist ASC
        LIMIT :limit OFFSET :offset
    """,
    )
    suspend fun getTopAlbumsByTimeFiltered(
        startTime: Long,
        endTime: Long,
        filterPodcasts: Boolean,
        filterAudiobooks: Boolean,
        limit: Int,
        offset: Int,
    ): List<TopAlbum>

    @SuppressWarnings("RoomWarnings.QUERY_MISMATCH")
    @Query(
        """
        WITH stats AS (
            SELECT 
                t.album,
                t.artist,
                COALESCE(MAX(NULLIF(em.album_art_url, '')), MAX(NULLIF(t.album_art_url, ''))) as album_art_url,
                COUNT(le.id) as play_count,
                SUM(le.playDuration) as total_time_ms,
                COUNT(DISTINCT t.id) as unique_tracks
            $RANKED_ALBUM_GROUPS
        ),
        max_values AS (
            SELECT 
                MAX(play_count) as max_plays,
                MAX(total_time_ms) as max_time
            FROM stats
        )
        SELECT 
            stats.*
        FROM stats
        CROSS JOIN max_values
        ORDER BY 
            (CASE 
                WHEN max_values.max_plays > 0 AND max_values.max_time > 0 THEN
                    (0.5 * CAST(stats.play_count AS REAL) / max_values.max_plays) + 
                    (0.5 * CAST(stats.total_time_ms AS REAL) / max_values.max_time)
                ELSE 0.0
            END) DESC,
            stats.play_count DESC,
            stats.total_time_ms DESC, stats.album ASC, stats.artist ASC
        LIMIT :limit OFFSET :offset
    """,
    )
    suspend fun getTopAlbumsByCombinedScoreFiltered(
        startTime: Long,
        endTime: Long,
        filterPodcasts: Boolean,
        filterAudiobooks: Boolean,
        limit: Int,
        offset: Int,
    ): List<TopAlbum>

    @Query(
        """
        $RANKED_ALBUM_COUNT
    """,
    )
    suspend fun getUniqueAlbumsCountFiltered(
        startTime: Long,
        endTime: Long,
        filterPodcasts: Boolean,
        filterAudiobooks: Boolean,
    ): Int

    @Query(
        """
        SELECT 
            COALESCE(SUM(le.playDuration), 0) as total_time_ms,
            COUNT(le.id) as play_count,
            COUNT(DISTINCT le.track_id) as unique_tracks,
            COUNT(DISTINCT t.artist) as unique_artists,
            ($RANKED_ALBUM_COUNT) as unique_albums
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        WHERE le.timestamp >= :startTime AND le.timestamp <= :endTime
            AND (:filterPodcasts = 0 OR t.content_type IS NULL OR t.content_type != 'PODCAST')
            AND (:filterAudiobooks = 0 OR t.content_type IS NULL OR t.content_type != 'AUDIOBOOK')
    """,
    )
    suspend fun getCombinedBasicStatsFiltered(
        startTime: Long,
        endTime: Long,
        filterPodcasts: Boolean,
        filterAudiobooks: Boolean,
    ): CombinedBasicStats
}
