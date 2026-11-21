package me.avinas.tempo.data.local.dao

import androidx.room.Query
import me.avinas.tempo.data.local.LocalTimeSql
import me.avinas.tempo.data.stats.*

/** Query family inherited by [StatsDao]; split for reviewability without changing the Room API. */
interface StatsEntityQueries {
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

    // Extended Artist Stats

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
            WHERE t.artist = :artistName AND t.album IS NOT NULL AND t.album != ''
            AND (em.release_type IS NULL OR LOWER(em.release_type) != 'single')
            GROUP BY t.album
            HAVING COUNT(DISTINCT t.id) > 1
        )
    """,
    )
    suspend fun getArtistUniqueAlbumsPlayed(artistName: String): Int

    @Query(
        """
        SELECT COUNT(DISTINCT t.id)
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        WHERE t.artist = :artistName
    """,
    )
    suspend fun getArtistUniqueTracksPlayed(artistName: String): Int

    @Query(
        """
        SELECT 
            AVG(CAST(json_extract(em.audio_features_json, '$.energy') AS REAL)) as avg_energy,
            AVG(CAST(json_extract(em.audio_features_json, '$.danceability') AS REAL)) as avg_danceability,
            AVG(CAST(json_extract(em.audio_features_json, '$.valence') AS REAL)) as avg_valence,
            AVG(CAST(json_extract(em.audio_features_json, '$.tempo') AS REAL)) as avg_tempo,
            AVG(CAST(json_extract(em.audio_features_json, '$.acousticness') AS REAL)) as avg_acousticness,
            AVG(CAST(json_extract(em.audio_features_json, '$.instrumentalness') AS REAL)) as avg_instrumentalness,
            AVG(CAST(json_extract(em.audio_features_json, '$.speechiness') AS REAL)) as avg_speechiness,
            AVG(CAST(json_extract(em.audio_features_json, '$.loudness') AS REAL)) as avg_loudness,
            COUNT(*) as tracks_count
        FROM enriched_metadata em
        INNER JOIN tracks t ON em.track_id = t.id
        WHERE t.artist = :artistName AND em.audio_features_json IS NOT NULL
    """,
    )
    suspend fun getArtistAudioFeatures(artistName: String): AudioFeaturesRaw?

    @Query(
        """
        SELECT MIN(le.timestamp) as first_listened, MAX(le.timestamp) as last_listened
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        WHERE t.artist = :artistName
    """,
    )
    suspend fun getArtistListeningDates(artistName: String): ArtistListeningDates?

    @Query(
        """
        SELECT CAST(strftime('%H', ${LocalTimeSql.LOCAL_DATETIME_LE}) AS INTEGER) as hour
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        WHERE t.artist = :artistName
        GROUP BY hour
        ORDER BY COUNT(*) DESC
        LIMIT 1
    """,
    )
    suspend fun getArtistPeakListeningHour(artistName: String): Int?

    /** Searches the artist name inside multi-artist strings with `LIKE`. */
    @Query(
        """
        SELECT em.artist_country
        FROM enriched_metadata em
        INNER JOIN tracks t ON em.track_id = t.id
        WHERE (LOWER(t.artist) = LOWER(:artistName) OR LOWER(t.artist) LIKE '%' || LOWER(:artistName) || '%') 
              AND em.artist_country IS NOT NULL AND em.artist_country != ''
        LIMIT 1
    """,
    )
    suspend fun getArtistCountry(artistName: String): String?

    @Query(
        """
        SELECT COALESCE(
            NULLIF(em.spotify_artist_image_url, ''),
            NULLIF(em.itunes_artist_image_url, ''),
            NULLIF(em.lastfm_artist_image_url, ''),
            NULLIF(em.deezer_artist_image_url, '')
        )
        FROM enriched_metadata em
        INNER JOIN tracks t ON em.track_id = t.id
        INNER JOIN track_artists ta ON ta.track_id = t.id
        INNER JOIN artists a ON a.id = ta.artist_id
        WHERE LOWER(a.name) = LOWER(:artistName)
              AND ta.role = 'PRIMARY'
              AND (em.spotify_artist_image_url IS NOT NULL OR em.itunes_artist_image_url IS NOT NULL 
                   OR em.lastfm_artist_image_url IS NOT NULL OR em.deezer_artist_image_url IS NOT NULL)
        LIMIT 1
    """,
    )
    suspend fun getArtistImageAsPrimaryArtist(artistName: String): String?

    @Query(
        """
        SELECT COALESCE(
            NULLIF(em.spotify_artist_image_url, ''),
            NULLIF(em.itunes_artist_image_url, ''),
            NULLIF(em.lastfm_artist_image_url, ''),
            NULLIF(em.deezer_artist_image_url, '')
        )
        FROM enriched_metadata em
        INNER JOIN tracks t ON em.track_id = t.id
        INNER JOIN track_artists ta ON ta.track_id = t.id
        INNER JOIN artists a ON a.id = ta.artist_id
        WHERE (
            LOWER(a.name) = LOWER(:artistName)
            -- Artist is first, followed by a separator (legacy support)
            OR LOWER(t.artist) LIKE LOWER(:artistName) || ',%'
            OR LOWER(t.artist) LIKE LOWER(:artistName) || ' &%'
            OR LOWER(t.artist) LIKE LOWER(:artistName) || ' and %'
            OR LOWER(t.artist) LIKE LOWER(:artistName) || ' feat%'
            OR LOWER(t.artist) LIKE LOWER(:artistName) || ' ft%'
            OR LOWER(t.artist) LIKE LOWER(:artistName) || ' x %'
            OR LOWER(t.artist) LIKE LOWER(:artistName) || ' /%'
            OR LOWER(t.artist) LIKE LOWER(:artistName) || ' +%'
        )
        AND ta.role = 'PRIMARY'
        AND (em.spotify_artist_image_url IS NOT NULL OR em.itunes_artist_image_url IS NOT NULL 
             OR em.lastfm_artist_image_url IS NOT NULL OR em.deezer_artist_image_url IS NOT NULL)
        LIMIT 1
    """,
    )
    suspend fun getArtistImageAsFirstArtist(artistName: String): String?

    /**
     * Fallback artist image URL from enriched metadata for any PRIMARY artist track.
     * Checks sources in priority order: Spotify > iTunes > Last.fm > Deezer.
     */
    @Query(
        """
        SELECT COALESCE(
            NULLIF(em.spotify_artist_image_url, ''),
            NULLIF(em.itunes_artist_image_url, ''),
            NULLIF(em.lastfm_artist_image_url, ''),
            NULLIF(em.deezer_artist_image_url, '')
        )
        FROM enriched_metadata em
        INNER JOIN tracks t ON em.track_id = t.id
        INNER JOIN track_artists ta ON ta.track_id = t.id
        INNER JOIN artists a ON a.id = ta.artist_id
        WHERE LOWER(a.name) = LOWER(:artistName)
              AND ta.role = 'PRIMARY'
              AND (em.spotify_artist_image_url IS NOT NULL OR em.itunes_artist_image_url IS NOT NULL 
                   OR em.lastfm_artist_image_url IS NOT NULL OR em.deezer_artist_image_url IS NOT NULL)
        LIMIT 1
    """,
    )
    suspend fun getArtistImageFromEnrichedMetadata(artistName: String): String?

    /**
     * Only returns images from tracks where this artist is PRIMARY and the row's
     * `spotify_artist_id` matches this artist's `spotify_id`, which stops a mismatch such as
     * Spotify reporting KR$NA as primary for a KARMA track. Sources are checked in priority
     * order: Spotify > iTunes > Last.fm > Deezer.
     */
    @Query(
        """
        SELECT COALESCE(
            NULLIF(em.spotify_artist_image_url, ''),
            NULLIF(em.itunes_artist_image_url, ''),
            NULLIF(em.lastfm_artist_image_url, ''),
            NULLIF(em.deezer_artist_image_url, '')
        )
        FROM enriched_metadata em
        INNER JOIN track_artists ta ON ta.track_id = em.track_id
        INNER JOIN artists a ON a.id = ta.artist_id
        WHERE ta.artist_id = :artistId
              AND ta.role = 'PRIMARY'
              AND (
                  -- Verify: enriched_metadata's Spotify artist ID matches this artist's Spotify ID
                  -- This prevents returning another artist's image from a collab/mismatched track
                  (a.spotify_id IS NOT NULL AND em.spotify_artist_id IS NOT NULL AND em.spotify_artist_id = a.spotify_id)
                  -- OR: no Spotify IDs to compare, and this is the ONLY artist on the track
                  OR (a.spotify_id IS NULL AND (SELECT COUNT(*) FROM track_artists ta2 WHERE ta2.track_id = ta.track_id) = 1)
              )
              AND (em.spotify_artist_image_url IS NOT NULL OR em.itunes_artist_image_url IS NOT NULL 
                   OR em.lastfm_artist_image_url IS NOT NULL OR em.deezer_artist_image_url IS NOT NULL)
        LIMIT 1
    """,
    )
    suspend fun getArtistImageByArtistId(artistId: Long): String?

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
        WHERE t.artist = :artistName AND t.album IS NOT NULL AND t.album != ''
        AND (em.release_type IS NULL OR LOWER(em.release_type) != 'single')
        GROUP BY t.album
        HAVING COUNT(DISTINCT t.id) > 1
        ORDER BY play_count DESC
        LIMIT :limit
    """,
    )
    suspend fun getTopAlbumsForArtist(
        artistName: String,
        limit: Int,
    ): List<TopAlbum>

    @Query(
        """
        SELECT COUNT(le.id)
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        WHERE t.album = :albumName
        AND (t.primary_artist_id = :artistId OR LOWER(t.artist) = LOWER(:artistName))
    """,
    )
    suspend fun getAlbumPlayCount(
        albumName: String,
        artistName: String,
        artistId: Long,
    ): Int

    @Query(
        """
        SELECT COALESCE(SUM(le.playDuration), 0)
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        WHERE t.album = :albumName
        AND (t.primary_artist_id = :artistId OR LOWER(t.artist) = LOWER(:artistName))
    """,
    )
    suspend fun getAlbumTotalTime(
        albumName: String,
        artistName: String,
        artistId: Long,
    ): Long

    @Query(
        """
        SELECT 
            t.id,
            t.title,
            t.artist,
            t.album,
            t.duration,
            t.primary_artist_id,
            COALESCE(NULLIF(em.album_art_url, ''), NULLIF(t.album_art_url, '')) as album_art_url,
            t.spotify_id,
            t.youtube_id,
            t.musicbrainz_id,
            t.content_type,
            COUNT(le.id) as play_count,
            COALESCE(SUM(le.playDuration), 0) as total_time_ms
        FROM tracks t
        LEFT JOIN listening_events le ON t.id = le.track_id
        LEFT JOIN enriched_metadata em ON t.id = em.track_id
        WHERE t.album = :albumName
        AND (t.primary_artist_id = :artistId OR LOWER(t.artist) = LOWER(:artistName))
        GROUP BY t.id
        ORDER BY t.title ASC
    """,
    )
    suspend fun getTracksForAlbumWithStats(
        albumName: String,
        artistName: String,
        artistId: Long,
    ): List<TrackWithStatsRaw>

    @Query(
        """
        SELECT 
            CAST(json_extract(em.audio_features_json, '$.energy') AS REAL) as energy,
            CAST(json_extract(em.audio_features_json, '$.danceability') AS REAL) as danceability,
            CAST(json_extract(em.audio_features_json, '$.valence') AS REAL) as valence,
            CAST(json_extract(em.audio_features_json, '$.tempo') AS REAL) as tempo,
            CAST(json_extract(em.audio_features_json, '$.acousticness') AS REAL) as acousticness,
            CAST(json_extract(em.audio_features_json, '$.instrumentalness') AS REAL) as instrumentalness,
            CAST(json_extract(em.audio_features_json, '$.speechiness') AS REAL) as speechiness,
            CAST(json_extract(em.audio_features_json, '$.liveness') AS REAL) as liveness,
            CAST(json_extract(em.audio_features_json, '$.loudness') AS REAL) as loudness,
            CAST(json_extract(em.audio_features_json, '$.key') AS INTEGER) as `key`,
            CAST(json_extract(em.audio_features_json, '$.mode') AS INTEGER) as mode,
            CAST(json_extract(em.audio_features_json, '$.time_signature') AS INTEGER) as timeSignature
        FROM enriched_metadata em
        WHERE em.track_id = :trackId
        AND em.audio_features_json IS NOT NULL
        LIMIT 1
    """,
    )
    suspend fun getTrackAudioFeaturesRaw(trackId: Long): TrackAudioFeaturesRaw?

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
