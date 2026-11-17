package me.avinas.tempo.ui.home

import me.avinas.tempo.data.stats.DailyListening
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeeklyListeningChartTest {
    private fun day(date: String, minutes: Long, plays: Int = 1) = DailyListening(
        date = date,
        playCount = plays,
        totalTimeMs = minutes * 60_000L,
        uniqueTracks = plays,
        uniqueArtists = plays,
    )

    @Test
    fun `current week begins on Monday and always has seven columns`() {
        val today = LocalDate.of(2026, 11, 17) // Tuesday
        val result = buildWeeklyListeningDays(today, listOf(
            day("2026-11-15", 200), // last week's Sunday must not leak in
            day("2026-11-16", 25, 3),
            day("2026-11-17", 54, 5),
            day("2026-11-19", 60), // future-dated data must not appear
        ))

        assertEquals(7, result.size)
        assertEquals(LocalDate.of(2026, 11, 16), result.first().date)
        assertEquals(LocalDate.of(2026, 11, 22), result.last().date)
        assertEquals(25 * 60_000L, result[0].listeningMs)
        assertEquals(3, result[0].playCount)
        assertEquals(54 * 60_000L, result[1].listeningMs)
        assertTrue(result.drop(2).all { it.listeningMs == 0L && it.playCount == 0 })
    }

    @Test
    fun `Sunday is still part of the same local calendar week`() {
        val today = LocalDate.of(2026, 11, 22)
        val result = buildWeeklyListeningDays(today, listOf(day("2026-11-22", 10)))
        assertEquals(6, result.indexOfFirst { it.date == today })
        assertEquals(10 * 60_000L, result.last().listeningMs)
    }

    @Test
    fun `missing and malformed duration records never create fake bars`() {
        val result = buildWeeklyListeningDays(
            LocalDate.of(2026, 11, 17),
            listOf(day("2026-11-16", -12, -4)),
        )
        assertTrue(result.all { it.listeningMs == 0L && it.playCount == 0 })
    }

    @Test
    fun `listening durations are floored instead of rounded up`() {
        assertEquals("0m", formatDashboardDuration(0L))
        assertEquals("0m", formatDashboardDuration(59_999L))
        assertEquals("1m", formatDashboardDuration(60_000L))
        assertEquals("1h", formatDashboardDuration(3_600_000L))
        assertEquals("2h 34m", formatDashboardDuration((2 * 60 + 34) * 60_000L))
    }
}
