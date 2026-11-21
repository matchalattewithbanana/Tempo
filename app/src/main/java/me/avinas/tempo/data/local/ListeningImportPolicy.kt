package me.avinas.tempo.data.local

import me.avinas.tempo.data.local.entities.ListeningEvent

internal object ListeningImportPolicy {
    const val SAME_SOURCE_TOLERANCE_MS = 5_000L
    private const val CROSS_SOURCE_TOLERANCE_MS = 60_000L
    private const val MEASURED_OVERLAP_MIN_MS = 15_000L
    private const val IMPORT_RECONCILIATION_LOOKBACK_MS = 900_000L
    private const val SPOTIFY_LOOKBACK_MS = 900_000L
    private const val SPOTIFY_END_TOLERANCE_MS = 15_000L
    private const val YOUTUBE_LOOKBACK_MS = 240_000L
    private const val SPOTIFY_LIVE = "com.spotify.music"
    private const val SPOTIFY_RECENT = "com.spotify.music.import"
    private const val SPOTIFY_EXPORT = "com.spotify.music.import.json"
    const val YOUTUBE_EXPORT = "com.google.android.apps.youtube.music.import.json"

    data class Play(
        val timestamp: Long,
        val endTimestamp: Long?,
        val duration: Long,
        val source: String,
        val sessionId: String?,
    ) {
        val authority: Int = SourceAuthority.rank(source)

        constructor(event: ListeningEvent) : this(
            event.timestamp, event.endTimestamp, event.playDuration, event.source, event.sessionId,
        )
    }

    fun searchPadding(source: String): Long = when (source) {
        SPOTIFY_LIVE, SPOTIFY_RECENT, SPOTIFY_EXPORT -> SPOTIFY_LOOKBACK_MS
        YOUTUBE_EXPORT -> YOUTUBE_LOOKBACK_MS
        // Query an indexed window large enough to compare actual intervals.
        // The old 60-second lookup hid overlapping plays whose starts drifted
        // farther apart even when both sources described the same listening time.
        else -> if (SourceAuthority.rank(source) < 90) IMPORT_RECONCILIATION_LOOKBACK_MS
            else CROSS_SOURCE_TOLERANCE_MS
    }

    /**
     * Retain the play when an imported duration contradicts its explicit time bounds.
     * A missing duration remains unknown. Observed phone and desktop sessions carry
     * their own timing semantics and do not go through this repair.
     */
    fun normalizeTiming(event: ListeningEvent): ListeningEvent {
        if (SourceAuthority.rank(event.source) >= 90) return event
        val end = event.endTimestamp
        val validEnd = end?.takeIf { it >= event.timestamp }
        val elapsed =
            if (validEnd != null && event.timestamp >= 0L) validEnd - event.timestamp
            else if (end != null && validEnd == null) 0L
            else Long.MAX_VALUE
        val duration = event.playDuration.coerceIn(0L, elapsed)
        if (duration == event.playDuration && end == validEnd) return event

        val completion = when {
            duration == 0L -> 0
            else -> (event.completionPercentage * (duration.toDouble() / event.playDuration))
                .toInt().coerceIn(0, 100)
        }
        return event.copy(playDuration = duration, endTimestamp = validEnd, completionPercentage = completion)
    }

    /** A smaller distance is a closer match. Null means the candidate is a separate play. */
    fun matchDistance(first: Play, second: Play): Long? {
        val sameSource = first.source == second.source
        val startDistance = distance(first.timestamp, second.timestamp)
        if (sameSource) {
            if (first.source.startsWith("desktop:")) {
                return startDistance.takeIf { DesktopObservedReplayPolicy.isSamePlay(first.timestamp, second.timestamp) }
            }
            if (!first.sessionId.isNullOrBlank() && !second.sessionId.isNullOrBlank() &&
                first.sessionId != second.sessionId
            ) return null
            if (first.source == SPOTIFY_EXPORT && first.endTimestamp != null &&
                second.endTimestamp != null && first.endTimestamp == second.endTimestamp &&
                // Some overlapping exports represent one play with a shifted start.
                // Do not merge distinct durations from a batched offline upload.
                distance(first.duration, second.duration) <= maxOf(20_000L, minOf(first.duration, second.duration) / 5L)
            ) return 0L
            if (first.source == YOUTUBE_EXPORT && (first.duration == 0L || second.duration == 0L)) {
                // Older imports shifted timestamp back by a guessed duration. The Takeout
                // timestamp in endTimestamp still identifies the original activity.
                val firstAnchor = first.endTimestamp ?: first.timestamp
                val secondAnchor = second.endTimestamp ?: second.timestamp
                return 0L.takeIf { firstAnchor == secondAnchor }
            }
            return startDistance.takeIf { it <= SAME_SOURCE_TOLERANCE_MS }
        }

        // Independent live clients can really play concurrently.
        if (first.authority >= 90 && second.authority >= 90) return null

        if (first.source == SPOTIFY_RECENT || second.source == SPOTIFY_RECENT) {
            val recent = if (first.source == SPOTIFY_RECENT) first else second
            val measured = if (first.source == SPOTIFY_RECENT) second else first
            if (measured.source == SPOTIFY_LIVE || measured.source == SPOTIFY_EXPORT) {
                val end = measured.endTimestamp?.takeIf { it > measured.timestamp }
                    ?: measured.duration.takeIf { it > 0L && measured.timestamp <= Long.MAX_VALUE - it }
                        ?.let { measured.timestamp + it }
                if (end != null) {
                    return distance(recent.timestamp, end).takeIf { it <= SPOTIFY_END_TOLERANCE_MS }
                }
            }
        }

        val firstEnd = explicitMeasuredEnd(first)
        val secondEnd = explicitMeasuredEnd(second)
        if (firstEnd != null && secondEnd != null) {
            val overlap = minOf(firstEnd, secondEnd) - maxOf(first.timestamp, second.timestamp)
            val shorter = minOf(first.duration, second.duration)
            // A timestamp near the other play's start is not sufficient: two
            // consecutive sessions can occur within 60 seconds. Demand a
            // meaningful interval overlap to preserve real fast replays.
            if (overlap < MEASURED_OVERLAP_MIN_MS || overlap < shorter / 2L) return null
            return minOf(startDistance, distance(firstEnd, secondEnd))
        }
        // A scrobble with no measured interval cannot establish whether a
        // farther-away timestamp describes the same play or an actual replay.
        return startDistance.takeIf { it <= CROSS_SOURCE_TOLERANCE_MS }
    }

    private fun explicitMeasuredEnd(play: Play): Long? = play.endTimestamp?.takeIf {
        play.authority >= 70 && play.duration > 0L && play.timestamp >= 0L && it > play.timestamp
    }

    private fun distance(first: Long, second: Long): Long {
        val difference = maxOf(first, second) - minOf(first, second)
        return if (difference < 0L) Long.MAX_VALUE else difference
    }
}
