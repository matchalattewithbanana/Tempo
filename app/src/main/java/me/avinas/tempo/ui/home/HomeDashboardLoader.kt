package me.avinas.tempo.ui.home

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import me.avinas.tempo.data.profile.ProfileIdentityManager
import me.avinas.tempo.data.remote.spotify.SpotifyTokenStorage
import me.avinas.tempo.data.repository.GamificationRepository
import me.avinas.tempo.data.repository.PreferencesRepository
import me.avinas.tempo.data.repository.SortBy
import me.avinas.tempo.data.repository.StatsRepository
import me.avinas.tempo.data.stats.AudioFeaturesStats
import me.avinas.tempo.data.stats.CombinedBasicStats
import me.avinas.tempo.data.stats.DailyListening
import me.avinas.tempo.data.stats.HourlyDistribution
import me.avinas.tempo.data.stats.InsightCardData
import me.avinas.tempo.data.stats.InsightPayload
import me.avinas.tempo.data.stats.InsightType
import me.avinas.tempo.data.stats.ListeningOverview
import me.avinas.tempo.data.stats.PeriodComparison
import me.avinas.tempo.data.stats.TimeRange
import me.avinas.tempo.data.stats.TopArtist
import me.avinas.tempo.data.stats.TopTrack
import me.avinas.tempo.ui.appearance.AppearanceHomeSection
import me.avinas.tempo.ui.spotlight.SpotlightPeriodFormatter

/**
 * Owns Home's database fan-out and pacing policy.
 *
 * Home asks for one semantic dashboard snapshot; callers do not need to know how many Room
 * statements, which queries can share results, or how concurrency is bounded. That keeps query
 * executor tuning and dashboard composition in one place instead of leaking them into UI state.
 */
internal class HomeDashboardLoader @Inject constructor(
    private val statsRepository: StatsRepository,
    private val tokenStorage: SpotifyTokenStorage,
    private val preferencesRepository: PreferencesRepository,
    private val profileIdentityManager: ProfileIdentityManager,
    private val gamificationRepository: GamificationRepository,
) {
    data class Request(
        val timeRange: TimeRange,
        val extended: Boolean,
        val hiddenSections: Set<AppearanceHomeSection>,
        val showSummaryChart: Boolean,
    )

    data class Data(
        val totals: CombinedBasicStats,
        val periodComparison: PeriodComparison?,
        val dailyListening: List<DailyListening>,
        val chartLabels: List<String>,
        val activeDaysCount: Int,
        val totalDaysCount: Int,
        val peakDayMs: Long,
        val topTrack: TopTrack?,
        val topArtist: TopArtist?,
        val spotlightTopTrack: TopTrack?,
        val audioFeatures: AudioFeaturesStats?,
        val insights: List<InsightCardData>,
        val userName: String,
        val profileImagePath: String?,
        val isNewUser: Boolean,
        val isGamificationEnabled: Boolean,
        val todayOverview: ListeningOverview?,
        val todayTopTrack: TopTrack?,
        val todayTopArtist: TopArtist?,
        val todayHourlyDistribution: List<HourlyDistribution>,
        val todayPeriodComparison: PeriodComparison?,
    ) {
        val dailyAverageMs: Long
            get() = preciseDailyAverageMs(totals.totalTimeMs, activeDaysCount)
    }

    data class TodayDetails(
        val tracks: List<TopTrack>,
        val artists: List<TopArtist>,
    )

    /**
     * Called as soon as the primary totals query completes, before secondary insights finish.
     * This preserves Home's fast first metric without exposing the rest of the query graph.
     */
    suspend fun load(
        request: Request,
        onTotalsReady: (CombinedBasicStats) -> Unit,
    ): Data = coroutineScope {
        val loadSummary = AppearanceHomeSection.SUMMARY !in request.hiddenSections
        val loadChart = loadSummary && request.showSummaryChart
        val loadToday = AppearanceHomeSection.TODAY !in request.hiddenSections
        val loadQuickStats =
            request.extended && AppearanceHomeSection.QUICK_STATS !in request.hiddenSections
        val loadInsights =
            request.extended && AppearanceHomeSection.INSIGHTS !in request.hiddenSections

        val totalsDeferred = async { db { statsRepository.getListeningTotals(request.timeRange, withLeeway = false) } }
        val periodComparisonDeferred =
            if (loadSummary) {
                async { db { statsRepository.getPeriodComparison(request.timeRange, withLeeway = false) } }
            } else {
                null
            }

        val chartPointLimit =
            when (request.timeRange) {
                TimeRange.TODAY, TimeRange.THIS_WEEK -> 7
                TimeRange.THIS_MONTH -> 31
                TimeRange.THIS_YEAR -> 366
                TimeRange.ALL_TIME -> 5_000
            }
        val rawDailyDeferred =
            if (loadChart) {
                async {
                    db {
                        statsRepository.getDailyListening(
                            request.timeRange,
                            chartPointLimit,
                            withLeeway = false,
                        )
                    }
                }
            } else {
                null
            }
        val activeDaysDeferred =
            if (loadChart) {
                async { db { statsRepository.getActiveDaysCount(request.timeRange, withLeeway = false) } }
            } else {
                null
            }
        val peakDayDeferred =
            if (loadChart) {
                async { db { statsRepository.getPeakDayListeningMs(request.timeRange, withLeeway = false) } }
            } else {
                null
            }
        val topTrackDeferred =
            if (loadQuickStats) {
                async {
                    db {
                        statsRepository.getTopTracks(
                            request.timeRange,
                            sortBy = SortBy.COMBINED_SCORE,
                            pageSize = 1,
                            withLeeway = false,
                        )
                    }
                }
            } else {
                null
            }
        val topArtistDeferred =
            if (loadQuickStats) {
                async {
                    db {
                        statsRepository.getTopArtists(
                            request.timeRange,
                            sortBy = SortBy.COMBINED_SCORE,
                            pageSize = 1,
                            withLeeway = false,
                        )
                    }
                }
            } else {
                null
            }
        val audioFeaturesDeferred =
            async { db { statsRepository.getAudioFeaturesStats(request.timeRange, withLeeway = false) } }
        val insightsDeferred =
            if (loadInsights) {
                async { db { statsRepository.getInsights(request.timeRange, withLeeway = false) } }
            } else {
                null
            }
        val profileDeferred = async { profileIdentityManager.getProfileIdentity() }
        val earliestDeferred = async { db { statsRepository.getEarliestDataTimestamp() } }

        val todayOverviewDeferred =
            if (loadToday) async { db { statsRepository.getListeningOverview(TimeRange.TODAY) } }
            else null
        val todayTrackDeferred =
            if (loadToday) {
                async {
                    db {
                        statsRepository.getTopTracks(
                            TimeRange.TODAY,
                            sortBy = SortBy.PLAY_COUNT,
                            pageSize = 1,
                        )
                    }
                }
            } else null
        val todayArtistDeferred =
            if (loadToday) {
                async {
                    db {
                        statsRepository.getTopArtists(
                            TimeRange.TODAY,
                            sortBy = SortBy.PLAY_COUNT,
                            pageSize = 1,
                        )
                    }
                }
            } else null
        val todayHourlyDeferred =
            if (loadToday) async { db { statsRepository.getHourlyDistribution(TimeRange.TODAY) } }
            else null
        val todayComparisonDeferred =
            when {
                !loadToday -> null
                request.timeRange == TimeRange.TODAY && periodComparisonDeferred != null ->
                    periodComparisonDeferred
                else ->
                    async {
                        db {
                            statsRepository.getPeriodComparison(
                                TimeRange.TODAY,
                                withLeeway = false,
                            )
                        }
                    }
            }

        val gamificationEnabledDeferred =
            async {
                db {
                    preferencesRepository.preferences().first()?.isGamificationEnabled ?: true
                }
            }

        val spotlightTimeRange = SpotlightPeriodFormatter.getDirectStoryTimeRange()
        val spotlightTrackDeferred =
            if (
                request.extended &&
                    AppearanceHomeSection.SPOTLIGHT !in request.hiddenSections &&
                    spotlightTimeRange != null
            ) {
                async {
                    db {
                        statsRepository.getTopTracks(
                            spotlightTimeRange,
                            sortBy = SortBy.COMBINED_SCORE,
                            pageSize = 1,
                            withLeeway = true,
                        )
                    }
                }
            } else {
                null
            }

        val totals = totalsDeferred.await()
        onTotalsReady(totals)

        val profile = profileDeferred.await()
        val rawDaily = rawDailyDeferred?.await()
        val chart =
            if (rawDaily != null) {
                withContext(Dispatchers.Default) {
                    HomeChartProcessor.process(request.timeRange, rawDaily)
                }
            } else {
                HomeChartProcessor.Result(emptyList(), emptyList())
            }

        val gamificationEnabled = gamificationEnabledDeferred.await()
        val combinedInsights = mutableListOf<InsightCardData>()
        if (gamificationEnabled && loadInsights) {
            val level = db { gamificationRepository.getUserLevel() }
            val nextBadge = db { gamificationRepository.getNextEarnableBadge() }
            combinedInsights +=
                InsightCardData(
                    title = "Level Progress",
                    description = "Your current level and next badge",
                    type = InsightType.LOYALTY,
                    payload =
                        InsightPayload.GamificationProgress(
                            level = level,
                            nextBadge = nextBadge,
                        ),
                )
        }
        combinedInsights += insightsDeferred?.await().orEmpty()

        val earliestTimestamp = earliestDeferred.await()
        val activeDays = activeDaysDeferred?.await() ?: 0
        val today = LocalDate.now()

        Data(
            totals = totals,
            periodComparison = periodComparisonDeferred?.await(),
            dailyListening = chart.data,
            chartLabels = chart.labels,
            activeDaysCount = activeDays,
            totalDaysCount = totalDays(request.timeRange, today, earliestTimestamp),
            peakDayMs = peakDayDeferred?.await() ?: 0L,
            topTrack = topTrackDeferred?.await()?.items?.firstOrNull(),
            topArtist = topArtistDeferred?.await()?.items?.firstOrNull(),
            spotlightTopTrack = spotlightTrackDeferred?.await()?.items?.firstOrNull(),
            audioFeatures = audioFeaturesDeferred.await(),
            insights = combinedInsights,
            userName =
                profile.userName.takeIf(String::isNotBlank)
                    ?: tokenStorage.getUserDisplayName()?.split(" ")?.firstOrNull()
                    ?: "User",
            profileImagePath = profile.profileImagePath,
            isNewUser = earliestTimestamp == null,
            isGamificationEnabled = gamificationEnabled,
            todayOverview = todayOverviewDeferred?.await(),
            todayTopTrack = todayTrackDeferred?.await()?.items?.firstOrNull(),
            todayTopArtist = todayArtistDeferred?.await()?.items?.firstOrNull(),
            todayHourlyDistribution = todayHourlyDeferred?.await().orEmpty(),
            todayPeriodComparison = todayComparisonDeferred?.await(),
        )
    }

    suspend fun loadTodayDetails(): TodayDetails = coroutineScope {
        val tracks =
            async {
                db {
                    statsRepository.getTopTracks(
                        TimeRange.TODAY,
                        sortBy = SortBy.PLAY_COUNT,
                        pageSize = 5,
                    )
                }
            }
        val artists =
            async {
                db {
                    statsRepository.getTopArtists(
                        TimeRange.TODAY,
                        sortBy = SortBy.PLAY_COUNT,
                        pageSize = 5,
                    )
                }
            }
        TodayDetails(
            tracks = tracks.await().items.take(5),
            artists = artists.await().items.take(5),
        )
    }

    private suspend fun <T> db(block: suspend () -> T): T =
        dbSemaphore.withPermit { block() }

    private fun totalDays(
        timeRange: TimeRange,
        today: LocalDate,
        earliestTimestamp: Long?,
    ): Int =
        when (timeRange) {
            TimeRange.TODAY -> 1
            TimeRange.THIS_WEEK -> today.dayOfWeek.value
            TimeRange.THIS_MONTH -> today.dayOfMonth
            TimeRange.THIS_YEAR -> today.dayOfYear
            TimeRange.ALL_TIME -> {
                if (earliestTimestamp == null) {
                    1
                } else {
                    val earliestDate =
                        Instant.ofEpochMilli(earliestTimestamp)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    (ChronoUnit.DAYS.between(earliestDate, today).toInt() + 1).coerceAtLeast(1)
                }
            }
        }

    /**
     * Match Room's default query executor instead of launching the dashboard's whole query burst
     * into its queue at once. One loader instance belongs to one HomeViewModel, so overlapping
     * refresh/cancellation paths share the same permits.
     */
    private val dbSemaphore = Semaphore(permits = 4)
}
