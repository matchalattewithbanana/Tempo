package me.avinas.tempo.ui.home

import java.time.LocalDate
import me.avinas.tempo.data.stats.DailyListening
import me.avinas.tempo.data.stats.TimeRange
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeChartProcessorTest {
    @Test
    fun weekFillsMissingDaysWithoutChangingObservedTotals() {
        val today = LocalDate.of(2026, 11, 22) // Sunday
        val result =
            HomeChartProcessor.process(
                timeRange = TimeRange.THIS_WEEK,
                rawData =
                    listOf(
                        day("2026-11-16", plays = 2, millis = 120_000L),
                        day("2026-11-22", plays = 3, millis = 180_000L),
                    ),
                today = today,
            )

        assertEquals(7, result.data.size)
        assertEquals(listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"), result.labels)
        assertEquals(5, result.data.sumOf { it.playCount })
        assertEquals(300_000L, result.data.sumOf { it.totalTimeMs })
        assertEquals(0, result.data[1].playCount)
    }

    @Test
    fun longAllTimeRangeAggregatesByMonthInCalendarOrder() {
        val today = LocalDate.of(2026, 4, 15)
        val result =
            HomeChartProcessor.process(
                timeRange = TimeRange.ALL_TIME,
                rawData =
                    listOf(
                        day("2025-12-30", plays = 1, millis = 60_000L),
                        day("2026-01-01", plays = 2, millis = 120_000L),
                        day("2026-01-31", plays = 3, millis = 180_000L),
                        day("2026-04-15", plays = 4, millis = 240_000L),
                    ),
                today = today,
            )

        assertEquals(
            listOf("2025-12-01", "2026-01-01", "2026-02-01", "2026-03-01", "2026-04-01"),
            result.data.map { it.date },
        )
        assertEquals(listOf("Dec 2025", "Jan", "Feb", "Mar", "Apr"), result.labels)
        assertEquals(5, result.data[1].playCount)
        assertEquals(0, result.data[2].playCount)
    }

    @Test
    fun todayUsesOnePointAndStableLabel() {
        val today = LocalDate.of(2026, 11, 22)
        val result =
            HomeChartProcessor.process(
                TimeRange.TODAY,
                listOf(day(today.toString(), plays = 7, millis = 420_000L)),
                today,
            )

        assertEquals(listOf("Today"), result.labels)
        assertEquals(7, result.data.single().playCount)
    }

    private fun day(
        date: String,
        plays: Int,
        millis: Long,
    ) =
        DailyListening(
            date = date,
            playCount = plays,
            totalTimeMs = millis,
            uniqueTracks = plays,
            uniqueArtists = plays,
        )
}
