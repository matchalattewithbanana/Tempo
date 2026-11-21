package me.avinas.tempo.ui.home

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import me.avinas.tempo.data.stats.DailyListening
import me.avinas.tempo.data.stats.TimeRange

/**
 * Pure chart shaping for Home.
 *
 * Repository data stays sparse (only days with listening); the chart needs a continuous
 * calendar series for short ranges and monthly buckets for long ranges. Keeping that calendar
 * policy here removes date-formatting and aggregation knowledge from the ViewModel and makes
 * the transformation testable without Android or Room.
 */
internal object HomeChartProcessor {
    data class Result(
        val data: List<DailyListening>,
        val labels: List<String>,
    )

    fun process(
        timeRange: TimeRange,
        rawData: List<DailyListening>,
        today: LocalDate = LocalDate.now(),
    ): Result =
        when (timeRange) {
            TimeRange.TODAY ->
                dailySeries(
                    start = today,
                    count = 1,
                    rawData = rawData,
                    label = { "Today" },
                )

            TimeRange.THIS_WEEK -> {
                val monday = today.minusDays(today.dayOfWeek.value.toLong() - 1)
                dailySeries(
                    start = monday,
                    count = today.dayOfWeek.value,
                    rawData = rawData,
                    label = { date ->
                        date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    },
                )
            }

            TimeRange.THIS_MONTH ->
                dailySeries(
                    start = today.withDayOfMonth(1),
                    count = today.dayOfMonth,
                    rawData = rawData,
                    label = { date -> "Day ${date.dayOfMonth}" },
                )

            TimeRange.THIS_YEAR ->
                if (today.dayOfYear > MONTHLY_AGGREGATION_DAY_LIMIT) {
                    monthlySeries(
                        start = YearMonth.of(today.year, 1),
                        end = YearMonth.from(today),
                        rawData = rawData,
                        currentYear = today.year,
                    )
                } else {
                    dailySeries(
                        start = today.withDayOfYear(1),
                        count = today.dayOfYear,
                        rawData = rawData,
                        label = { date -> shortDateLabel(date, today.year) },
                    )
                }

            TimeRange.ALL_TIME -> {
                val earliest =
                    rawData.asSequence()
                        .mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }
                        .minOrNull()
                        ?: today
                val totalDays =
                    (ChronoUnit.DAYS.between(earliest, today).toInt() + 1).coerceAtLeast(1)

                if (totalDays > MONTHLY_AGGREGATION_DAY_LIMIT) {
                    monthlySeries(
                        start = YearMonth.from(earliest),
                        end = YearMonth.from(today),
                        rawData = rawData,
                        currentYear = today.year,
                    )
                } else {
                    dailySeries(
                        start = earliest,
                        count = totalDays,
                        rawData = rawData,
                        label = { date -> shortDateLabel(date, today.year) },
                    )
                }
            }
        }

    private fun dailySeries(
        start: LocalDate,
        count: Int,
        rawData: List<DailyListening>,
        label: (LocalDate) -> String,
    ): Result {
        val byDate = rawData.associateBy(DailyListening::date)
        val days = List(count.coerceAtLeast(0)) { offset -> start.plusDays(offset.toLong()) }
        return Result(
            data =
                days.map { date ->
                    byDate[date.toString()] ?: emptyDay(date.toString())
                },
            labels = days.map(label),
        )
    }

    private fun monthlySeries(
        start: YearMonth,
        end: YearMonth,
        rawData: List<DailyListening>,
        currentYear: Int,
    ): Result {
        val byMonth = rawData.groupBy { it.date.take(YEAR_MONTH_PREFIX_LENGTH) }
        val months = buildList {
            var cursor = start
            while (!cursor.isAfter(end)) {
                add(cursor)
                cursor = cursor.plusMonths(1)
            }
        }

        return Result(
            data =
                months.map { month ->
                    val items = byMonth[month.toString()].orEmpty()
                    DailyListening(
                        date = "$month-01",
                        playCount = items.sumOf(DailyListening::playCount),
                        totalTimeMs = items.sumOf(DailyListening::totalTimeMs),
                        // These fields are already approximate monthly totals in the old Home
                        // implementation; preserve that behavior while centralizing it here.
                        uniqueTracks = items.sumOf(DailyListening::uniqueTracks),
                        uniqueArtists = items.sumOf(DailyListening::uniqueArtists),
                    )
                },
            labels =
                months.map { month ->
                    val short = month.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    if (month.year == currentYear) short else "$short ${month.year}"
                },
        )
    }

    private fun shortDateLabel(date: LocalDate, currentYear: Int): String {
        val month = date.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
        return if (date.year == currentYear) {
            "${date.dayOfMonth} $month"
        } else {
            "${date.dayOfMonth} $month ${date.year}"
        }
    }

    private fun emptyDay(date: String): DailyListening =
        DailyListening(
            date = date,
            playCount = 0,
            totalTimeMs = 0,
            uniqueTracks = 0,
            uniqueArtists = 0,
        )

    private const val MONTHLY_AGGREGATION_DAY_LIMIT = 60
    private const val YEAR_MONTH_PREFIX_LENGTH = 7
}
