package me.avinas.tempo.ui.stats

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import me.avinas.tempo.data.stats.CombinedBasicStats
import me.avinas.tempo.ui.appearance.AppearanceStatsDuration
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
    }

    @Test
    fun summaryUsesTheSelectedDistinctCount() {
        val totals = CombinedBasicStats(4_000L, 36, 25, 15, 10)
        assertEquals(25, totals.summaryCount(AppearanceStatsSummary.TRACKS))
        assertEquals(15, totals.summaryCount(AppearanceStatsSummary.ARTISTS))
        assertEquals(10, totals.summaryCount(AppearanceStatsSummary.ALBUMS))
    }
}
