package me.avinas.tempo.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

/** The Home average must not lose seconds before dividing by active days. */
class HomeListeningDurationTest {
    @Test
    fun averageUsesFullMilliseconds() {
        assertEquals(45_000L, preciseDailyAverageMs(90_000L, 2))
        assertEquals(35_397_000L, preciseDailyAverageMs(35_397_000L, 1))
        assertEquals(0L, preciseDailyAverageMs(59_999L, 0))
        assertEquals(0L, preciseDailyAverageMs(-1L, 3))
    }
}
