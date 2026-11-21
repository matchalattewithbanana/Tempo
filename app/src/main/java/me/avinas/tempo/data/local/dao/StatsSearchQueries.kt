package me.avinas.tempo.data.local.dao

import androidx.room.Query
import me.avinas.tempo.data.local.LocalTimeSql
import me.avinas.tempo.data.stats.*

/** Query family inherited by [StatsDao]; split for reviewability without changing the Room API. */
interface StatsSearchQueries {
    // Ranking Search Queries
    // Live-filter the full ranking by a text query and report each match's
    // GLOBAL rank. The stats CTE is materialized once; the match filter and
    // LIMIT are applied first, in the same tie-break order the rank counts, so
    // the correlated count only runs for the LIMIT-ed rows (limit × n, not
    // matches × n and not n²). No window functions: minSdk 26 devices
    // (API 26-28) ship SQLite older than 3.25.

    /** Rows come back ordered by rank ascending; the caller assigns rank = position + 1. */
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
        top_matches AS (
            SELECT stats.*
            FROM stats
            WHERE stats.title LIKE '%' || :query || '%'
               OR stats.artist LIKE '%' || :query || '%'
               OR (stats.album IS NOT NULL AND stats.album LIKE '%' || :query || '%')
            ORDER BY stats.play_count DESC, stats.total_time_ms DESC, stats.track_id ASC
            LIMIT :limit
        )
        SELECT 
            top_matches.*,
            (SELECT COUNT(*) FROM stats s2
             WHERE s2.play_count > top_matches.play_count
                OR (s2.play_count = top_matches.play_count AND s2.total_time_ms > top_matches.total_time_ms)
                OR (s2.play_count = top_matches.play_count AND s2.total_time_ms = top_matches.total_time_ms AND s2.track_id < top_matches.track_id)
            ) + 1 as global_rank,
            NULL as combined_score
        FROM top_matches
        ORDER BY global_rank
        LIMIT :limit
    """,
    )
    suspend fun searchTopTracksByPlayCount(
        startTime: Long,
        endTime: Long,
        filterPodcasts: Boolean,
        filterAudiobooks: Boolean,
        query: String,
        limit: Int,
    ): List<RankedTopTrack>

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
        top_matches AS (
            SELECT stats.*
            FROM stats
            WHERE stats.title LIKE '%' || :query || '%'
               OR stats.artist LIKE '%' || :query || '%'
               OR (stats.album IS NOT NULL AND stats.album LIKE '%' || :query || '%')
            ORDER BY stats.total_time_ms DESC, stats.play_count DESC, stats.track_id ASC
            LIMIT :limit
        )
        SELECT 
            top_matches.*,
            (SELECT COUNT(*) FROM stats s2
             WHERE s2.total_time_ms > top_matches.total_time_ms
                OR (s2.total_time_ms = top_matches.total_time_ms AND s2.play_count > top_matches.play_count)
                OR (s2.total_time_ms = top_matches.total_time_ms AND s2.play_count = top_matches.play_count AND s2.track_id < top_matches.track_id)
            ) + 1 as global_rank,
            NULL as combined_score
        FROM top_matches
        ORDER BY global_rank
        LIMIT :limit
    """,
    )
    suspend fun searchTopTracksByTime(
        startTime: Long,
        endTime: Long,
        filterPodcasts: Boolean,
        filterAudiobooks: Boolean,
        query: String,
        limit: Int,
    ): List<RankedTopTrack>

    /**
     * The combined score is normalized over the FULL ranking, same as the list view, so a
     * match's score and rank agree with what the unfiltered chart shows.
     */
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
        ),
        scored AS (
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
        ),
        top_matches AS (
            SELECT scored.*
            FROM scored
            WHERE scored.title LIKE '%' || :query || '%'
               OR scored.artist LIKE '%' || :query || '%'
               OR (scored.album IS NOT NULL AND scored.album LIKE '%' || :query || '%')
            ORDER BY scored.combined_score DESC, scored.play_count DESC, scored.total_time_ms DESC, scored.track_id ASC
            LIMIT :limit
        )
        SELECT 
            top_matches.*,
            (SELECT COUNT(*) FROM scored s2
             WHERE s2.combined_score > top_matches.combined_score
                OR (s2.combined_score = top_matches.combined_score AND s2.play_count > top_matches.play_count)
                OR (s2.combined_score = top_matches.combined_score AND s2.play_count = top_matches.play_count AND s2.total_time_ms > top_matches.total_time_ms)
                OR (s2.combined_score = top_matches.combined_score AND s2.play_count = top_matches.play_count AND s2.total_time_ms = top_matches.total_time_ms AND s2.track_id < top_matches.track_id)
            ) + 1 as global_rank
        FROM top_matches
        ORDER BY global_rank
        LIMIT :limit
    """,
    )
    suspend fun searchTopTracksByCombinedScore(
        startTime: Long,
        endTime: Long,
        filterPodcasts: Boolean,
        filterAudiobooks: Boolean,
        query: String,
        limit: Int,
    ): List<RankedTopTrack>

    /** Same album eligibility rules as getTopAlbumsFiltered. */
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
        top_matches AS (
            SELECT stats.*
            FROM stats
            WHERE stats.album LIKE '%' || :query || '%'
               OR stats.artist LIKE '%' || :query || '%'
            ORDER BY stats.play_count DESC, stats.total_time_ms DESC, stats.album ASC, stats.artist ASC
            LIMIT :limit
        )
        SELECT 
            top_matches.*,
            (SELECT COUNT(*) FROM stats s2
             WHERE s2.play_count > top_matches.play_count
                OR (s2.play_count = top_matches.play_count AND s2.total_time_ms > top_matches.total_time_ms)
                OR (s2.play_count = top_matches.play_count AND s2.total_time_ms = top_matches.total_time_ms AND (s2.album < top_matches.album OR (s2.album = top_matches.album AND s2.artist < top_matches.artist)))
            ) + 1 as global_rank
        FROM top_matches
        ORDER BY global_rank
        LIMIT :limit
    """,
    )
    suspend fun searchTopAlbumsByPlayCount(
        startTime: Long,
        endTime: Long,
        filterPodcasts: Boolean,
        filterAudiobooks: Boolean,
        query: String,
        limit: Int,
    ): List<RankedTopAlbum>

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
        top_matches AS (
            SELECT stats.*
            FROM stats
            WHERE stats.album LIKE '%' || :query || '%'
               OR stats.artist LIKE '%' || :query || '%'
            ORDER BY stats.play_count DESC, stats.total_time_ms DESC, stats.album ASC, stats.artist ASC
            LIMIT :limit
        )
        SELECT 
            top_matches.*,
            (SELECT COUNT(*) FROM stats s2
             WHERE s2.play_count > top_matches.play_count
                OR (s2.play_count = top_matches.play_count AND s2.total_time_ms > top_matches.total_time_ms)
                OR (s2.play_count = top_matches.play_count AND s2.total_time_ms = top_matches.total_time_ms AND (s2.album < top_matches.album OR (s2.album = top_matches.album AND s2.artist < top_matches.artist)))
            ) + 1 as global_rank
        FROM top_matches
        ORDER BY global_rank
        LIMIT :limit
    """,
    )
    suspend fun searchTopAlbums(
        startTime: Long,
        endTime: Long,
        filterPodcasts: Boolean,
        filterAudiobooks: Boolean,
        query: String,
        limit: Int,
    ): List<RankedTopAlbum>

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
        top_matches AS (
            SELECT stats.*
            FROM stats
            WHERE stats.album LIKE '%' || :query || '%'
               OR stats.artist LIKE '%' || :query || '%'
            ORDER BY stats.total_time_ms DESC, stats.play_count DESC, stats.album ASC, stats.artist ASC
            LIMIT :limit
        )
        SELECT 
            top_matches.*,
            (SELECT COUNT(*) FROM stats s2
             WHERE s2.total_time_ms > top_matches.total_time_ms
                OR (s2.total_time_ms = top_matches.total_time_ms AND s2.play_count > top_matches.play_count)
                OR (s2.total_time_ms = top_matches.total_time_ms AND s2.play_count = top_matches.play_count AND (s2.album < top_matches.album OR (s2.album = top_matches.album AND s2.artist < top_matches.artist)))
            ) + 1 as global_rank
        FROM top_matches
        ORDER BY global_rank
        LIMIT :limit
    """,
    )
    suspend fun searchTopAlbumsByTime(
        startTime: Long,
        endTime: Long,
        filterPodcasts: Boolean,
        filterAudiobooks: Boolean,
        query: String,
        limit: Int,
    ): List<RankedTopAlbum>

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
        ),
        scored AS (
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
        ),
        top_matches AS (
            SELECT scored.*
            FROM scored
            WHERE scored.album LIKE '%' || :query || '%'
               OR scored.artist LIKE '%' || :query || '%'
            ORDER BY scored.combined_score DESC, scored.play_count DESC, scored.total_time_ms DESC, scored.album ASC, scored.artist ASC
            LIMIT :limit
        )
        SELECT 
            top_matches.album,
            top_matches.artist,
            top_matches.album_art_url,
            top_matches.play_count,
            top_matches.total_time_ms,
            top_matches.unique_tracks,
            (SELECT COUNT(*) FROM scored s2
             WHERE s2.combined_score > top_matches.combined_score
                OR (s2.combined_score = top_matches.combined_score AND s2.play_count > top_matches.play_count)
                OR (s2.combined_score = top_matches.combined_score AND s2.play_count = top_matches.play_count AND s2.total_time_ms > top_matches.total_time_ms)
                OR (s2.combined_score = top_matches.combined_score AND s2.play_count = top_matches.play_count AND s2.total_time_ms = top_matches.total_time_ms AND (s2.album < top_matches.album OR (s2.album = top_matches.album AND s2.artist < top_matches.artist)))
            ) + 1 as global_rank
        FROM top_matches
        ORDER BY global_rank
        LIMIT :limit
    """,
    )
    suspend fun searchTopAlbumsByCombinedScore(
        startTime: Long,
        endTime: Long,
        filterPodcasts: Boolean,
        filterAudiobooks: Boolean,
        query: String,
        limit: Int,
    ): List<RankedTopAlbum>
}
