package me.avinas.tempo.data.local.dao

import androidx.room.Query
import me.avinas.tempo.data.local.LocalTimeSql
import me.avinas.tempo.data.stats.*

/** Query family inherited by [StatsDao]; split for reviewability without changing the Room API. */
interface StatsTemporalQueries {
    // Temporal Analysis - Hour of Day

    @Query(
        """
        SELECT 
            CAST(strftime('%H', ${LocalTimeSql.LOCAL_DATETIME}) AS INTEGER) as hour,
            COUNT(*) as play_count,
            SUM(playDuration) as total_time_ms
        FROM listening_events
        WHERE timestamp >= :startTime AND timestamp <= :endTime
        GROUP BY hour
        ORDER BY hour ASC
    """,
    )
    suspend fun getHourlyDistribution(
        startTime: Long,
        endTime: Long,
    ): List<HourlyDistribution>

    @Query(
        """
        SELECT 
            CAST(strftime('%H', ${LocalTimeSql.LOCAL_DATETIME}) AS INTEGER) as hour,
            COUNT(*) as play_count,
            SUM(playDuration) as total_time_ms
        FROM listening_events
        WHERE timestamp >= :startTime AND timestamp <= :endTime
        GROUP BY hour
        ORDER BY play_count DESC
        LIMIT 1
    """,
    )
    suspend fun getMostActiveHour(
        startTime: Long,
        endTime: Long,
    ): HourlyDistribution?

    /**
     * Average of each day's earliest listen: when the user typically *starts* listening,
     * not when they are most active overall.
     */
    @Query(
        """
        SELECT
            CAST(AVG(first_hour) AS INTEGER) as hour,
            COUNT(*) as play_count,
            0 as total_time_ms
        FROM (
            SELECT
                MIN(CAST(strftime('%H', ${LocalTimeSql.LOCAL_DATETIME}) AS INTEGER)) as first_hour
            FROM listening_events
            WHERE timestamp >= :startTime AND timestamp <= :endTime
            GROUP BY strftime('%Y-%m-%d', ${LocalTimeSql.LOCAL_DATETIME})
        )
    """,
    )
    suspend fun getTypicalStartHour(
        startTime: Long,
        endTime: Long,
    ): HourlyDistribution?

    // Temporal Analysis - Day of Week

    /**
     * SQLite strftime %w returns 0-6 (Sunday-Saturday), we convert to 1-7 (Monday-Sunday).
     */
    @Query(
        """
        SELECT 
            CASE 
                WHEN CAST(strftime('%w', ${LocalTimeSql.LOCAL_DATETIME}) AS INTEGER) = 0 THEN 7
                ELSE CAST(strftime('%w', ${LocalTimeSql.LOCAL_DATETIME}) AS INTEGER)
            END as day_of_week,
            COUNT(*) as play_count,
            SUM(playDuration) as total_time_ms
        FROM listening_events
        WHERE timestamp >= :startTime AND timestamp <= :endTime
        GROUP BY day_of_week
        ORDER BY day_of_week ASC
    """,
    )
    suspend fun getDayOfWeekDistribution(
        startTime: Long,
        endTime: Long,
    ): List<DayOfWeekDistribution>

    @Query(
        """
        SELECT 
            CASE 
                WHEN CAST(strftime('%w', ${LocalTimeSql.LOCAL_DATETIME}) AS INTEGER) = 0 THEN 7
                ELSE CAST(strftime('%w', ${LocalTimeSql.LOCAL_DATETIME}) AS INTEGER)
            END as day_of_week,
            COUNT(*) as play_count,
            SUM(playDuration) as total_time_ms
        FROM listening_events
        WHERE timestamp >= :startTime AND timestamp <= :endTime
        GROUP BY day_of_week
        ORDER BY play_count DESC
        LIMIT 1
    """,
    )
    suspend fun getMostActiveDay(
        startTime: Long,
        endTime: Long,
    ): DayOfWeekDistribution?

    // Temporal Analysis - Daily Aggregations

    @Query(
        """
        SELECT 
            strftime('%Y-%m-%d', ${LocalTimeSql.LOCAL_DATETIME_LE}) as date,
            COUNT(le.id) as play_count,
            SUM(le.playDuration) as total_time_ms,
            COUNT(DISTINCT le.track_id) as unique_tracks,
            COUNT(DISTINCT t.artist) as unique_artists
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        WHERE le.timestamp >= :startTime AND le.timestamp <= :endTime
            AND (:filterPodcasts = 0 OR t.content_type IS NULL OR t.content_type != 'PODCAST')
            AND (:filterAudiobooks = 0 OR t.content_type IS NULL OR t.content_type != 'AUDIOBOOK')
        GROUP BY date
        ORDER BY date DESC
        LIMIT :limit
    """,
    )
    suspend fun getDailyListening(
        startTime: Long,
        endTime: Long,
        limit: Int,
        filterPodcasts: Boolean = false,
        filterAudiobooks: Boolean = false,
    ): List<DailyListening>

    // Temporal Analysis - Monthly Aggregations

    @Query(
        """
        SELECT 
            CAST(strftime('%Y', ${LocalTimeSql.LOCAL_DATETIME_LE}) AS INTEGER) as year,
            CAST(strftime('%m', ${LocalTimeSql.LOCAL_DATETIME_LE}) AS INTEGER) as month,
            COUNT(le.id) as play_count,
            SUM(le.playDuration) as total_time_ms,
            COUNT(DISTINCT le.track_id) as unique_tracks,
            COUNT(DISTINCT t.artist) as unique_artists
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        WHERE le.timestamp >= :startTime AND le.timestamp <= :endTime
        GROUP BY year, month
        ORDER BY year DESC, month DESC
    """,
    )
    suspend fun getMonthlyListening(
        startTime: Long,
        endTime: Long,
    ): List<MonthlyListening>

    // Listening Streaks

    @Query(
        """
        SELECT DISTINCT strftime('%Y-%m-%d', ${LocalTimeSql.LOCAL_DATETIME}) as date
        FROM listening_events
        ORDER BY date ASC
    """,
    )
    suspend fun getAllListeningDates(): List<String>

    /**
     * Timezone offset of the most recent play, used to anchor "today" for the streak.
     *
     * Counting against the device's date alone drops the newest day when a traveller's recorded
     * day is ahead of the phone's (flying west) and can skip one when it is behind (flying east).
     * Indexed by `index_listening_events_timestamp_track_id`, so this is a single-row lookup.
     */
    @Query("SELECT tz_offset_minutes FROM listening_events ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestListeningOffsetMinutes(): Int?

    @Query(
        """
        SELECT COUNT(DISTINCT strftime('%Y-%m-%d', ${LocalTimeSql.LOCAL_DATETIME}))
        FROM listening_events
        WHERE timestamp >= :startTime AND timestamp <= :endTime
    """,
    )
    suspend fun getActiveDaysCount(
        startTime: Long,
        endTime: Long,
    ): Int

    @Query(
        """
        SELECT SUM(playDuration)
        FROM listening_events
        WHERE timestamp >= :startTime AND timestamp <= :endTime
        GROUP BY strftime('%Y-%m-%d', ${LocalTimeSql.LOCAL_DATETIME})
        ORDER BY SUM(playDuration) DESC
        LIMIT 1
    """,
    )
    suspend fun getPeakDayListeningMs(
        startTime: Long,
        endTime: Long,
    ): Long?

    // Discovery Metrics

    @Query(
        """
        SELECT 
            t.artist as name,
            MIN(le.timestamp) as first_listen_timestamp,
            'artist' as type
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        GROUP BY t.artist
        ORDER BY first_listen_timestamp DESC
    """,
    )
    suspend fun getArtistFirstListens(): List<FirstListen>

    @Query(
        """
        SELECT COUNT(DISTINCT t.artist)
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        WHERE le.timestamp >= :startTime AND le.timestamp <= :endTime
        AND t.artist NOT IN (
            SELECT DISTINCT t2.artist
            FROM listening_events le2
            INNER JOIN tracks t2 ON le2.track_id = t2.id
            WHERE le2.timestamp < :startTime
        )
    """,
    )
    suspend fun getNewArtistsCount(
        startTime: Long,
        endTime: Long,
    ): Int

    @Query(
        """
        SELECT COUNT(DISTINCT le.track_id)
        FROM listening_events le
        WHERE le.timestamp >= :startTime AND le.timestamp <= :endTime
        AND le.track_id NOT IN (
            SELECT DISTINCT track_id
            FROM listening_events
            WHERE timestamp < :startTime
        )
    """,
    )
    suspend fun getNewTracksCount(
        startTime: Long,
        endTime: Long,
    ): Int

    @Query(
        """
        SELECT COUNT(*)
        FROM (
            SELECT track_id, COUNT(*) as cnt
            FROM listening_events
            WHERE timestamp >= :startTime AND timestamp <= :endTime
            GROUP BY track_id
            HAVING cnt > 1
        )
    """,
    )
    suspend fun getRepeatTracksCount(
        startTime: Long,
        endTime: Long,
    ): Int

    @Query(
        """
        SELECT 
            t.artist,
            COUNT(le.id) as total_plays,
            SUM(CASE WHEN le.id > first_le.first_id THEN 1 ELSE 0 END) as repeat_plays,
            COUNT(DISTINCT t.id) as unique_tracks_played,
            MIN(le.timestamp) as first_listen,
            CAST((julianday('now') - julianday(datetime(MIN(le.timestamp)/1000, 'unixepoch'))) AS INTEGER) as days_since_first_listen
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        INNER JOIN (
            SELECT track_id, MIN(id) as first_id
            FROM listening_events
            GROUP BY track_id
        ) first_le ON le.track_id = first_le.track_id
        WHERE le.timestamp >= :startTime AND le.timestamp <= :endTime
        GROUP BY t.artist
        HAVING total_plays >= :minPlays
        ORDER BY total_plays DESC
        LIMIT :limit
    """,
    )
    suspend fun getArtistLoyalty(
        startTime: Long,
        endTime: Long,
        minPlays: Int,
        limit: Int,
    ): List<ArtistLoyalty>

    @Query(
        """
        SELECT COUNT(*) 
        FROM (
            SELECT COUNT(le.id) as play_count 
            FROM listening_events le
            INNER JOIN tracks t ON le.track_id = t.id
            WHERE le.timestamp >= :startTime AND le.timestamp <= :endTime
            GROUP BY t.artist
            HAVING play_count > :playCount
        )
    """,
    )
    suspend fun countArtistsWithPlayCountMoreThan(
        playCount: Int,
        startTime: Long,
        endTime: Long,
    ): Int

    @Query(
        """
        SELECT COUNT(*) + 1
        FROM (
            SELECT COUNT(le.id) as play_count
            FROM listening_events le
            GROUP BY le.track_id
            HAVING play_count > :playCount
        )
    """,
    )
    suspend fun getTrackRankAllTime(playCount: Int): Int

    @Query(
        """
        SELECT MIN(le.timestamp)
        FROM listening_events le
        INNER JOIN track_artists ta ON le.track_id = ta.track_id
        WHERE ta.artist_id = :artistId
    """,
    )
    suspend fun getArtistDiscoveryDate(artistId: Long): Long?

    // Engagement Metrics

    @Query(
        """
        SELECT AVG(CASE WHEN completionPercentage = 0 AND was_skipped = 0 AND (source LIKE '%.import%' OR source LIKE 'desktop:%') THEN NULL ELSE completionPercentage END) 
        FROM listening_events 
        WHERE timestamp >= :startTime AND timestamp <= :endTime
    """,
    )
    suspend fun getAverageCompletionRate(
        startTime: Long,
        endTime: Long,
    ): Double?

    @Query(
        """
        SELECT COUNT(*) 
        FROM listening_events 
        WHERE timestamp >= :startTime AND timestamp <= :endTime
        AND completionPercentage >= 80
    """,
    )
    suspend fun getFullListensCount(
        startTime: Long,
        endTime: Long,
    ): Int

    /**
     * Falls back to completion < 30% for events that predate `was_skipped`.
     */
    @Query(
        """
        SELECT COUNT(*) 
        FROM listening_events 
        WHERE timestamp >= :startTime AND timestamp <= :endTime
        AND (was_skipped = 1 OR (completionPercentage < 30 AND NOT (completionPercentage = 0 AND was_skipped = 0 AND (source LIKE '%.import%' OR source LIKE 'desktop:%'))))
    """,
    )
    suspend fun getSkipsCount(
        startTime: Long,
        endTime: Long,
    ): Int

    @Query(
        """
        SELECT COUNT(*) 
        FROM listening_events 
        WHERE timestamp >= :startTime AND timestamp <= :endTime
        AND is_replay = 1
    """,
    )
    suspend fun getReplayCount(
        startTime: Long,
        endTime: Long,
    ): Int

    // Insights Queries

    /**
     * Artists played repeatedly in a short session: consecutive plays of the same artist
     * within 3 hours.
     */
    @Query(
        """
        SELECT 
            t.artist,
            COUNT(*) as session_play_count,
            SUM(le.playDuration) as session_duration_ms,
            MIN(le.timestamp) as session_start
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        WHERE le.timestamp >= :startTime AND le.timestamp <= :endTime
        AND (le.timestamp - (
            SELECT le2.timestamp 
            FROM listening_events le2 
            WHERE le2.timestamp < le.timestamp 
            ORDER BY le2.timestamp DESC LIMIT 1
        )) < 10800000 -- 3 hours gap max break
        GROUP BY t.artist
        HAVING session_play_count >= 5 -- Minimum plays to count as binge
        ORDER BY session_play_count DESC
        LIMIT 5
    """,
    )
    suspend fun getBingeListeningSessions(
        startTime: Long,
        endTime: Long,
    ): List<BingeSession>

    /**
     * Audio features JSONs for aggregation, processed in the repository because SQLite JSON
     * support varies.
     */
    @Query(
        """
        SELECT 
            em.audio_features_json,
            em.tags,
            em.genres
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        LEFT JOIN enriched_metadata em ON t.id = em.track_id
        WHERE le.timestamp >= :startTime AND le.timestamp <= :endTime
        AND (
            em.audio_features_json IS NOT NULL 
            OR (em.tags IS NOT NULL AND em.tags != '' AND em.tags != '[]')
            OR (em.genres IS NOT NULL AND em.genres != '' AND em.genres != '[]')
        )
    """,
    )
    suspend fun getMoodRawData(
        startTime: Long,
        endTime: Long,
    ): List<MoodRawData>

    /**
     * New artist discovery over time, grouped by month for the long-term trend.
     */
    @Query(
        """
        SELECT 
            ${LocalTimeSql.LOCAL_DISCOVERY_MONTH} as month,
            COUNT(min_timestamps.artist) as new_artists_count
        FROM (
            SELECT t.artist, MIN(le.timestamp) as first_listen, le.tz_offset_minutes
            FROM listening_events le
            INNER JOIN tracks t ON le.track_id = t.id
            GROUP BY t.artist
        ) min_timestamps
        WHERE min_timestamps.first_listen >= :startTime 
        AND min_timestamps.first_listen <= :endTime
        GROUP BY month
        ORDER BY month DESC
    """,
    )
    suspend fun getNewArtistDiscoveryTrend(
        startTime: Long,
        endTime: Long,
    ): List<DiscoveryTrend>

    @Query(
        """
        SELECT COUNT(*) 
        FROM listening_events 
        WHERE timestamp >= :startTime AND timestamp <= :endTime
        AND completionPercentage >= 30 AND completionPercentage < 80
    """,
    )
    suspend fun getPartialPlaysCount(
        startTime: Long,
        endTime: Long,
    ): Int

    @Query(
        """
        SELECT AVG(CAST(pause_count AS FLOAT)) 
        FROM listening_events 
        WHERE timestamp >= :startTime AND timestamp <= :endTime
    """,
    )
    suspend fun getAveragePauseCount(
        startTime: Long,
        endTime: Long,
    ): Float?

    @Query(
        """
        SELECT COUNT(DISTINCT session_id) 
        FROM listening_events 
        WHERE timestamp >= :startTime AND timestamp <= :endTime
        AND session_id IS NOT NULL
    """,
    )
    suspend fun getUniqueSessionsCount(
        startTime: Long,
        endTime: Long,
    ): Int

    @Query(
        """
        SELECT 
            t.id as track_id,
            t.title,
            t.artist,
            AVG(CASE WHEN le.completionPercentage = 0 AND le.was_skipped = 0 AND (le.source LIKE '%.import%' OR le.source LIKE 'desktop:%') THEN NULL ELSE le.completionPercentage END) as average_completion,
            SUM(CASE WHEN le.completionPercentage >= 80 THEN 1 ELSE 0 END) as full_listens,
            SUM(CASE WHEN le.was_skipped = 1 OR (le.completionPercentage < 30 AND NOT (le.completionPercentage = 0 AND le.was_skipped = 0 AND (le.source LIKE '%.import%' OR le.source LIKE 'desktop:%'))) THEN 1 ELSE 0 END) as skips,
            COUNT(le.id) as total_plays
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        WHERE le.timestamp >= :startTime AND le.timestamp <= :endTime
        GROUP BY t.id
        HAVING total_plays >= :minPlays
        ORDER BY average_completion DESC
        LIMIT :limit
    """,
    )
    suspend fun getTrackCompletionStats(
        startTime: Long,
        endTime: Long,
        minPlays: Int,
        limit: Int,
    ): List<TrackCompletion>

    @Query(
        """
        SELECT 
            t.id as track_id,
            t.title,
            t.artist,
            AVG(CASE WHEN le.completionPercentage = 0 AND le.was_skipped = 0 AND (le.source LIKE '%.import%' OR le.source LIKE 'desktop:%') THEN NULL ELSE le.completionPercentage END) as average_completion,
            SUM(CASE WHEN le.completionPercentage >= 80 THEN 1 ELSE 0 END) as full_listens,
            SUM(CASE WHEN le.was_skipped = 1 OR (le.completionPercentage < 30 AND NOT (le.completionPercentage = 0 AND le.was_skipped = 0 AND (le.source LIKE '%.import%' OR le.source LIKE 'desktop:%'))) THEN 1 ELSE 0 END) as skips,
            COUNT(le.id) as total_plays
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        WHERE le.timestamp >= :startTime AND le.timestamp <= :endTime
        GROUP BY t.id
        HAVING skips > 0
        ORDER BY skips DESC, average_completion ASC
        LIMIT :limit
    """,
    )
    suspend fun getMostSkippedTracks(
        startTime: Long,
        endTime: Long,
        limit: Int,
    ): List<TrackCompletion>

    @Query(
        """
        SELECT 
            t.id as track_id,
            t.title,
            t.artist,
            SUM(CASE WHEN le.is_replay = 1 THEN 1 ELSE 0 END) as replay_count,
            COUNT(le.id) as total_plays,
            AVG(CASE WHEN le.completionPercentage = 0 AND le.was_skipped = 0 AND (le.source LIKE '%.import%' OR le.source LIKE 'desktop:%') THEN NULL ELSE le.completionPercentage END) as average_completion
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        WHERE le.timestamp >= :startTime AND le.timestamp <= :endTime
        GROUP BY t.id
        HAVING replay_count > 0
        ORDER BY replay_count DESC
        LIMIT :limit
    """,
    )
    suspend fun getMostReplayedTracks(
        startTime: Long,
        endTime: Long,
        limit: Int,
    ): List<ReplayedTrackStats>

    @Query(
        """
        SELECT le.id, le.track_id, le.timestamp, t.artist, t.album
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        WHERE le.timestamp >= :startTime AND le.timestamp <= :endTime
        ORDER BY le.timestamp ASC
    """,
    )
    suspend fun getEventsForBingeDetection(
        startTime: Long,
        endTime: Long,
    ): List<BingeDetectionEvent>

    @Query(
        """
        SELECT 
            CAST(strftime('%H', ${LocalTimeSql.LOCAL_DATETIME}) AS INTEGER) as hour,
            AVG(CASE WHEN completionPercentage = 0 AND was_skipped = 0 AND (source LIKE '%.import%' OR source LIKE 'desktop:%') THEN NULL ELSE completionPercentage END) as avg_completion,
            SUM(CASE WHEN was_skipped = 1 OR (completionPercentage < 30 AND NOT (completionPercentage = 0 AND was_skipped = 0 AND (source LIKE '%.import%' OR source LIKE 'desktop:%'))) THEN 1 ELSE 0 END) as skip_count
        FROM listening_events
        WHERE timestamp >= :startTime AND timestamp <= :endTime
        GROUP BY hour
        ORDER BY hour
    """,
    )
    suspend fun getHourlyCompletionStats(
        startTime: Long,
        endTime: Long,
    ): List<HourlyCompletionStats>

    // Year-over-Year Comparison

    @Query(
        """
        SELECT 
            COUNT(le.id) as play_count,
            SUM(le.playDuration) as total_time_ms,
            COUNT(DISTINCT t.artist) as unique_artists
        FROM listening_events le
        INNER JOIN tracks t ON le.track_id = t.id
        WHERE strftime('%Y', ${LocalTimeSql.LOCAL_DATETIME_LE}) = :year
    """,
    )
    suspend fun getYearStats(year: String): YearStatsRaw

    // Spotify Audio Features Stats

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
            COUNT(DISTINCT le.track_id) as tracks_count
        FROM listening_events le
        INNER JOIN enriched_metadata em ON le.track_id = em.track_id
        WHERE le.timestamp >= :startTime AND le.timestamp <= :endTime
        AND em.audio_features_json IS NOT NULL
    """,
    )
    suspend fun getAverageAudioFeatures(
        startTime: Long,
        endTime: Long,
    ): AudioFeaturesRaw?

    @Query(
        """
        SELECT 
            strftime('%Y-%m-%d', ${LocalTimeSql.LOCAL_DATETIME_LE}) as date,
            AVG(CAST(json_extract(em.audio_features_json, '$.valence') AS REAL)) as avg_valence,
            AVG(CAST(json_extract(em.audio_features_json, '$.energy') AS REAL)) as avg_energy,
            AVG(CAST(json_extract(em.audio_features_json, '$.danceability') AS REAL)) as avg_danceability,
            COUNT(DISTINCT le.track_id) as track_count
        FROM listening_events le
        INNER JOIN enriched_metadata em ON le.track_id = em.track_id
        WHERE le.timestamp >= :startTime AND le.timestamp <= :endTime
        AND em.audio_features_json IS NOT NULL
        GROUP BY date
        ORDER BY date ASC
    """,
    )
    suspend fun getMoodTrends(
        startTime: Long,
        endTime: Long,
    ): List<MoodTrend>

    @Query(
        """
        SELECT 
            CASE 
                WHEN CAST(json_extract(em.audio_features_json, '$.tempo') AS REAL) < 80 THEN 'Slow (<80 BPM)'
                WHEN CAST(json_extract(em.audio_features_json, '$.tempo') AS REAL) < 100 THEN 'Moderate (80-100 BPM)'
                WHEN CAST(json_extract(em.audio_features_json, '$.tempo') AS REAL) < 120 THEN 'Upbeat (100-120 BPM)'
                WHEN CAST(json_extract(em.audio_features_json, '$.tempo') AS REAL) < 140 THEN 'Fast (120-140 BPM)'
                ELSE 'Very Fast (140+ BPM)'
            END as bucket_label,
            COUNT(DISTINCT le.track_id) as track_count,
            COUNT(le.id) as total_plays
        FROM listening_events le
        INNER JOIN enriched_metadata em ON le.track_id = em.track_id
        WHERE le.timestamp >= :startTime AND le.timestamp <= :endTime
        AND em.audio_features_json IS NOT NULL
        GROUP BY bucket_label
        ORDER BY track_count DESC
    """,
    )
    suspend fun getTempoDistributionRaw(
        startTime: Long,
        endTime: Long,
    ): List<TempoDistributionRaw>
}
