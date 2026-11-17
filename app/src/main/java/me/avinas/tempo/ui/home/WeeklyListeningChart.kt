package me.avinas.tempo.ui.home

import me.avinas.tempo.data.stats.DailyListening
import java.time.LocalDate

/** A single local-calendar day in the current Monday–Sunday week. */
internal data class WeeklyListeningDay(
    val date: LocalDate,
    val listeningMs: Long,
    val playCount: Int,
)

/**
 * Keep the Home snapshot aligned to the current calendar week, not an arbitrary
 * seven-row SQL result. Missing/future days are zero, never invented playback.
 * This pure projection can be tested without Android or Compose.
 */
internal fun buildWeeklyListeningDays(
    today: LocalDate,
    daily: List<DailyListening>,
): List<WeeklyListeningDay> {
    val monday = today.minusDays(today.dayOfWeek.value.toLong() - 1L)
    val byDate = daily.associateBy { it.date }
    return (0L..6L).map { offset ->
        val date = monday.plusDays(offset)
        val record = byDate[date.toString()].takeUnless { date.isAfter(today) }
        WeeklyListeningDay(
            date = date,
            listeningMs = (record?.totalTimeMs ?: 0L).coerceAtLeast(0L),
            playCount = (record?.playCount ?: 0).coerceAtLeast(0),
        )
    }
}
