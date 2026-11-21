package me.avinas.tempo.data.local.dao

// Album identity is the stored (title, artist) pair. The selected period and content
// filters must contain at least two distinct non-single tracks for that pair.
internal const val RANKED_ALBUM_GROUPS = """
    FROM listening_events le
    INNER JOIN tracks t ON le.track_id = t.id
    LEFT JOIN enriched_metadata em ON t.id = em.track_id
    WHERE le.timestamp >= :startTime AND le.timestamp <= :endTime
      AND t.album IS NOT NULL AND TRIM(t.album) != ''
      AND (em.release_type IS NULL OR LOWER(em.release_type) != 'single')
      AND (:filterPodcasts = 0 OR t.content_type IS NULL OR t.content_type != 'PODCAST')
      AND (:filterAudiobooks = 0 OR t.content_type IS NULL OR t.content_type != 'AUDIOBOOK')
    GROUP BY t.album, t.artist
    HAVING COUNT(DISTINCT t.id) > 1
"""
internal const val RANKED_ALBUM_COUNT =
    "SELECT COUNT(*) FROM (SELECT 1 $RANKED_ALBUM_GROUPS)"
