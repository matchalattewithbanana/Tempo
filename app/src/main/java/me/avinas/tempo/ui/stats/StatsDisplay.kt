package me.avinas.tempo.ui.stats

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import me.avinas.tempo.R
import me.avinas.tempo.data.stats.CombinedBasicStats
import me.avinas.tempo.ui.appearance.AppearanceStatsDuration
import me.avinas.tempo.ui.appearance.AppearanceStatsSummary
import me.avinas.tempo.ui.details.formatListeningTime
import me.avinas.tempo.ui.theme.LocalTempoAppearance
import java.text.NumberFormat

@StringRes
internal fun statsDurationLabel(unit: AppearanceStatsDuration): Int = when (unit) {
    AppearanceStatsDuration.AUTO -> R.string.stats_duration_auto
    AppearanceStatsDuration.MINUTES -> R.string.stats_duration_minutes
    AppearanceStatsDuration.HOURS -> R.string.stats_duration_hours
    AppearanceStatsDuration.DAYS -> R.string.stats_duration_days
}

@StringRes
internal fun statsSummaryLabel(metric: AppearanceStatsSummary): Int = when (metric) {
    AppearanceStatsSummary.TRACKS -> R.string.stats_period_unique_songs
    AppearanceStatsSummary.ARTISTS -> R.string.stats_period_unique_artists
    AppearanceStatsSummary.ALBUMS -> R.string.stats_period_unique_albums
}

internal fun CombinedBasicStats.summaryCount(metric: AppearanceStatsSummary): Int = when (metric) {
    AppearanceStatsSummary.TRACKS -> uniqueTracks
    AppearanceStatsSummary.ARTISTS -> uniqueArtists
    AppearanceStatsSummary.ALBUMS -> uniqueAlbums
}

@Composable
internal fun displayStatsDuration(millis: Long): String {
    val context = LocalContext.current
    val unit = LocalTempoAppearance.current.statsDuration
    val locale = context.resources.configuration.locales[0]
    return remember(context, locale, unit, millis) { formatStatsDuration(context, millis, unit) }
}

internal fun formatStatsDuration(context: Context, millis: Long, unit: AppearanceStatsDuration): String {
    if (unit == AppearanceStatsDuration.AUTO) {
        return if (millis > 0) formatListeningTime(millis) else context.getString(R.string.stats_period_zero_time)
    }
    val value = millis.coerceAtLeast(0L)
    val (millisPerUnit, resource) = when (unit) {
        AppearanceStatsDuration.MINUTES -> 60_000L to R.string.stats_duration_minutes_value
        AppearanceStatsDuration.HOURS -> 3_600_000L to R.string.stats_duration_hours_value
        AppearanceStatsDuration.DAYS -> 86_400_000L to R.string.stats_duration_days_value
        AppearanceStatsDuration.AUTO -> error("Automatic formatting handled above")
    }
    val formatter = NumberFormat.getNumberInstance(context.resources.configuration.locales[0]).apply {
        maximumFractionDigits = 2
    }
    val belowPrecision = value in 1L until (millisPerUnit / 100)
    val number = formatter.format(if (belowPrecision) 0.01 else value.toDouble() / millisPerUnit)
    val duration = context.getString(resource, number)
    return if (belowPrecision) context.getString(R.string.stats_duration_less_than, duration) else duration
}
