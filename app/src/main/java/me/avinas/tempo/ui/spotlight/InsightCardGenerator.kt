package me.avinas.tempo.ui.spotlight

import android.util.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import me.avinas.tempo.data.local.entities.Badge
import me.avinas.tempo.data.local.entities.UserLevel
import me.avinas.tempo.data.repository.GamificationRepository
import me.avinas.tempo.data.repository.StatsRepository
import me.avinas.tempo.data.repository.SortBy
import me.avinas.tempo.data.stats.ArtistLoyalty
import me.avinas.tempo.data.stats.DayOfWeekDistribution
import me.avinas.tempo.data.stats.DiscoveryStats
import me.avinas.tempo.data.stats.FirstListen
import me.avinas.tempo.data.stats.HourlyDistribution
import me.avinas.tempo.data.stats.ListeningOverview
import me.avinas.tempo.data.stats.ListeningStreak
import me.avinas.tempo.data.stats.PaginatedResult
import me.avinas.tempo.data.stats.TimeRange
import me.avinas.tempo.data.stats.TopGenre
import me.avinas.tempo.data.stats.TopTrack
import me.avinas.tempo.data.stats.GamificationEngine
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import me.avinas.tempo.R
import kotlin.random.Random
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import me.avinas.tempo.ui.onboarding.dataStore
import kotlinx.coroutines.flow.first

class InsightCardGenerator @Inject constructor(
    @param:ApplicationContext context: Context,
    repository: StatsRepository,
    gamificationRepository: GamificationRepository,
) {
    private val dashboardCards = SpotlightDashboardCardGenerator(context, repository)
    private val storyGenerator = SpotlightStoryGenerator(context, repository, gamificationRepository)

    suspend fun generateCards(timeRange: TimeRange): List<SpotlightCardData> =
        dashboardCards.generateCards(timeRange)

    suspend fun generateStory(timeRange: TimeRange): List<SpotlightStoryPage> =
        storyGenerator.generateStory(timeRange)
}
