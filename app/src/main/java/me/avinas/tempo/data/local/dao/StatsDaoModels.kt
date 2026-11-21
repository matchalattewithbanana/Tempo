package me.avinas.tempo.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Embedded
import me.avinas.tempo.data.stats.*

data class TrackWithStatsRaw(
    @Embedded val track: me.avinas.tempo.data.local.entities.Track,
    val play_count: Int,
    val total_time_ms: Long,
)

/** TopTrack plus its GLOBAL rank: the SQL-computed rank needs its own column, TopTrack.rank is @Ignore. */
data class RankedTopTrack(
    @Embedded val item: TopTrack,
    @ColumnInfo(name = "global_rank") val globalRank: Int,
)

/**
 * Search result row: a top-album entry plus its GLOBAL rank in the full ranking.
 */
data class RankedTopAlbum(
    @Embedded val item: TopAlbum,
    @ColumnInfo(name = "global_rank") val globalRank: Int,
)

data class BingeDetectionEvent(
    val id: Long,
    val track_id: Long,
    val timestamp: Long,
    val artist: String,
    val album: String?,
) {
    val trackId: Long get() = track_id
}

data class HourlyCompletionStats(
    val hour: Int,
    val avg_completion: Float,
    val skip_count: Int,
) {
    val avgCompletion: Float get() = avg_completion
    val skipCount: Int get() = skip_count
}

data class HistoryItem(
    val id: Long,
    val track_id: Long,
    val timestamp: Long,
    val playDuration: Long,
    val completionPercentage: Int,
    val title: String,
    val artist: String,
    val album: String?,
    val content_type: String = "MUSIC",
    val album_art_url: String?,
    val source: String = "",
)

data class YearStatsRaw(
    val play_count: Int,
    val total_time_ms: Long,
    val unique_artists: Int,
)

data class AudioFeaturesRaw(
    val avg_energy: Float?,
    val avg_danceability: Float?,
    val avg_valence: Float?,
    val avg_tempo: Float?,
    val avg_acousticness: Float?,
    val avg_instrumentalness: Float?,
    val avg_speechiness: Float?,
    val avg_loudness: Float?,
    val tracks_count: Int,
)

data class TempoDistributionRaw(
    val bucket_label: String,
    val track_count: Int,
    val total_plays: Int,
)

data class TrackAudioFeaturesRaw(
    val energy: Float?,
    val danceability: Float?,
    val valence: Float?,
    val tempo: Float?,
    val acousticness: Float?,
    val instrumentalness: Float?,
    val speechiness: Float?,
    val liveness: Float?,
    val loudness: Float?,
    val key: Int?,
    val mode: Int?,
    val timeSignature: Int?,
)

data class ArtistListeningDates(
    val first_listened: Long?,
    val last_listened: Long?,
)

data class ArtistPlayCountResult(
    val artistId: Long,
    val playCount: Int,
)
