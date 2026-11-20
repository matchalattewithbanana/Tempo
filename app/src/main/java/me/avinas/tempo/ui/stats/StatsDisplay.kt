package me.avinas.tempo.ui.stats

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import me.avinas.tempo.R
import me.avinas.tempo.data.stats.CombinedBasicStats
import me.avinas.tempo.ui.appearance.AppearanceStatsDuration
import me.avinas.tempo.ui.appearance.AppearanceDurationPrecision
import me.avinas.tempo.ui.appearance.AppearanceStatsSummary
import me.avinas.tempo.ui.details.formatListeningTime
import me.avinas.tempo.ui.theme.LocalTempoAppearance
import java.text.NumberFormat
import java.math.RoundingMode
import java.util.Locale

@StringRes
internal fun statsDurationLabel(unit: AppearanceStatsDuration): Int = when (unit) {
    AppearanceStatsDuration.AUTO -> R.string.stats_duration_auto
    AppearanceStatsDuration.MINUTES -> R.string.stats_duration_minutes
    AppearanceStatsDuration.HOURS -> R.string.stats_duration_hours
    AppearanceStatsDuration.DAYS -> R.string.stats_duration_days
    AppearanceStatsDuration.CLOCK -> R.string.stats_duration_clock
    AppearanceStatsDuration.CLOCK_SECONDS -> R.string.stats_duration_clock_seconds
}

@StringRes
internal fun statsDurationPrecisionLabel(value: AppearanceDurationPrecision): Int = when (value) {
    AppearanceDurationPrecision.WHOLE -> R.string.stats_precision_whole
    AppearanceDurationPrecision.ONE -> R.string.stats_precision_one
    AppearanceDurationPrecision.TWO -> R.string.stats_precision_two
    AppearanceDurationPrecision.THREE -> R.string.stats_precision_three
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
    val appearance = LocalTempoAppearance.current
    val locale = LocalConfiguration.current.locales[0]
    return remember(context, locale, appearance.statsDuration, appearance.statsDurationPrecision, millis) {
        formatStatsDuration(context, millis, appearance.statsDuration, appearance.statsDurationPrecision)
    }
}

/** Convert only at presentation time; stored durations retain full millisecond precision. */
internal fun formatStatsDuration(
    context: Context, millis: Long, unit: AppearanceStatsDuration,
    precision: AppearanceDurationPrecision = AppearanceDurationPrecision.TWO,
): String {
    if (unit == AppearanceStatsDuration.AUTO) {
        return if (millis > 0L) formatListeningTime(millis) else context.getString(R.string.stats_period_zero_time)
    }
    if (unit == AppearanceStatsDuration.CLOCK || unit == AppearanceStatsDuration.CLOCK_SECONDS) {
        if (millis <= 0L) return context.getString(R.string.stats_period_zero_time)
        val seconds = millis / 1_000L
        if (seconds == 0L) return context.getString(R.string.stats_duration_less_than,
            context.getString(R.string.stats_clock_seconds_value, 1L))
        val hours = seconds / 3_600L
        val minutes = (seconds / 60L) % 60L
        val remaining = seconds % 60L
        return if (unit == AppearanceStatsDuration.CLOCK_SECONDS) when {
            hours > 0L -> context.getString(R.string.stats_clock_hours_minutes_seconds, hours, minutes, remaining)
            seconds >= 60L -> context.getString(R.string.stats_clock_minutes_seconds, seconds / 60L, remaining)
            else -> context.getString(R.string.stats_clock_seconds_value, remaining)
        } else when {
            hours > 0L -> context.getString(R.string.stats_clock_hours_minutes, hours, minutes)
            seconds >= 60L -> context.getString(R.string.stats_clock_minutes_value, seconds / 60L)
            else -> context.getString(R.string.stats_duration_less_than,
                context.getString(R.string.stats_clock_minutes_value, 1L))
        }
    }
    val value = millis.coerceAtLeast(0L)
    val (millisPerUnit, res) = when (unit) {
        AppearanceStatsDuration.MINUTES -> 60_000L to R.string.stats_duration_minutes_value
        AppearanceStatsDuration.HOURS -> 3_600_000L to R.string.stats_duration_hours_value
        AppearanceStatsDuration.DAYS -> 86_400_000L to R.string.stats_duration_days_value
        else -> error("Handled above")
    }
    val scale = when (precision) {
        AppearanceDurationPrecision.WHOLE -> 1L
        AppearanceDurationPrecision.ONE -> 10L
        AppearanceDurationPrecision.TWO -> 100L
        AppearanceDurationPrecision.THREE -> 1_000L
    }
    val cutoff = millisPerUnit / scale
    val tiny = value > 0L && value < cutoff
    val formatter = durationNumberFormatter(context.resources.configuration.locales[0], precision)
    val rendered = context.getString(res, formatter.format(if (tiny) cutoff.toDouble() / millisPerUnit else value.toDouble() / millisPerUnit))
    return if (tiny) context.getString(R.string.stats_duration_less_than, rendered) else rendered
}

// NumberFormat is not thread-safe. Cache one per thread+locale+precision to avoid
// repeatedly allocating ICU formatters as Home, Stats, sharing and widgets render.
private data class CachedDurationFormatter(
    val locale: Locale,
    val precision: AppearanceDurationPrecision,
    val formatter: NumberFormat,
)
private val threadDurationFormatter = ThreadLocal<CachedDurationFormatter>()

private fun durationNumberFormatter(locale: Locale, precision: AppearanceDurationPrecision): NumberFormat {
    val cached = threadDurationFormatter.get()
    if (cached != null && cached.locale == locale && cached.precision == precision) return cached.formatter
    val formatter = NumberFormat.getNumberInstance(locale).apply {
        maximumFractionDigits = precision.digits
        minimumFractionDigits = 0
        roundingMode = RoundingMode.DOWN
    }
    threadDurationFormatter.set(CachedDurationFormatter(locale, precision, formatter))
    return formatter
}
