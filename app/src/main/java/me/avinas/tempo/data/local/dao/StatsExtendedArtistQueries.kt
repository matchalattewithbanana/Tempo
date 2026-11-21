package me.avinas.tempo.data.local.dao

import androidx.room.Query
import me.avinas.tempo.data.local.LocalTimeSql
import me.avinas.tempo.data.stats.*

/** Query family inherited by [StatsDao]; split without changing SQL or repository call sites. */
interface StatsExtendedArtistQueries {
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
}
