package me.avinas.tempo.ui.stats

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import me.avinas.tempo.data.stats.CombinedBasicStats
import me.avinas.tempo.ui.appearance.AppearanceStatsDuration
import me.avinas.tempo.ui.appearance.AppearanceDurationPrecision
import me.avinas.tempo.ui.appearance.AppearanceStatsSummary
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class StatsDisplayTest {
    @Test
    fun durationUnitsUseElapsedListeningTimeAndPreserveNonzeroPlays() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val english = Configuration(base.resources.configuration).apply { setLocale(Locale.US) }
        val context = base.createConfigurationContext(english)

        assertEquals("90 min", formatStatsDuration(context, 5_400_000L, AppearanceStatsDuration.MINUTES))
        assertEquals("1.5 h", formatStatsDuration(context, 5_400_000L, AppearanceStatsDuration.HOURS))
        assertEquals("1.25 d", formatStatsDuration(context, 108_000_000L, AppearanceStatsDuration.DAYS))
        assertEquals("<0.01 d", formatStatsDuration(context, 1_000L, AppearanceStatsDuration.DAYS))
        assertEquals("0 d", formatStatsDuration(context, 0L, AppearanceStatsDuration.DAYS))
        assertEquals("9h 49m 57s", formatStatsDuration(context, 35_397_000L, AppearanceStatsDuration.CLOCK_SECONDS))
        assertEquals("9h 49m", formatStatsDuration(context, 35_397_000L, AppearanceStatsDuration.CLOCK))
        assertEquals("59.98 min", formatStatsDuration(context, 3_599_000L, AppearanceStatsDuration.MINUTES))
        assertEquals("59 min", formatStatsDuration(context, 3_599_000L, AppearanceStatsDuration.MINUTES, AppearanceDurationPrecision.WHOLE))
        assertEquals("<1 min", formatStatsDuration(context, 59_000L, AppearanceStatsDuration.MINUTES, AppearanceDurationPrecision.WHOLE))
        assertEquals("589.95 min", formatStatsDuration(context, 35_397_000L, AppearanceStatsDuration.MINUTES))
        // Never prematurely round up at a unit boundary.
        assertEquals("0.99 h", formatStatsDuration(context, 3_599_999L, AppearanceStatsDuration.HOURS))
        assertEquals("0.99 min", formatStatsDuration(context, 59_999L, AppearanceStatsDuration.MINUTES))
        assertEquals("59m 59s", formatStatsDuration(context, 3_599_999L, AppearanceStatsDuration.CLOCK_SECONDS))
        assertEquals("1s", formatStatsDuration(context, 1_000L, AppearanceStatsDuration.CLOCK_SECONDS))
    }

    @Test
    fun summaryUsesTheSelectedDistinctCount() {
        val totals = CombinedBasicStats(4_000L, 36, 25, 15, 10)
        assertEquals(25, totals.summaryCount(AppearanceStatsSummary.TRACKS))
        assertEquals(15, totals.summaryCount(AppearanceStatsSummary.ARTISTS))
        assertEquals(10, totals.summaryCount(AppearanceStatsSummary.ALBUMS))
    }
}
