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

/**
 * Result types for card generation to replace silent failures.
 */
private sealed class CardGenerationResult {
    data class Success(val card: SpotlightCardData) : CardGenerationResult()
    data class InsufficientData(val cardType: String, val reason: String) : CardGenerationResult()
    data class Error(val cardType: String, val exception: Exception) : CardGenerationResult()
}

/**
 * Pre-fetched data used by multiple card generators.
 * Fetching this once before parallel card generation reduces duplicate DB queries
 * and significantly improves Spotlight screen loading time.
 * 
 * Performance Optimization v2: Now includes batch-fetched artist data to eliminate
 * N+1 query problems in generateEarlyAdopter, generateNewObsession, and generateArtistLoyalty.
 */
private data class PrefetchedData(
    val overview: ListeningOverview,
    val hourlyDist: List<HourlyDistribution>,
    val dayOfWeekDist: List<DayOfWeekDistribution>,
    val discoveryStats: DiscoveryStats,
    val topTracks: PaginatedResult<TopTrack>,
    val artistFirstListens: List<FirstListen>,
    val artistLoyalty: List<ArtistLoyalty>,
    val topGenres: List<TopGenre>,
    // Batch-fetched data to avoid N+1 queries
    val discoveryArtistPlayCounts: Map<String, Int>,  // Play counts for all first-listen artists
    val allArtistImageUrls: Map<String, String?>,  // Image URLs for ALL artists (discovery + loyalty + topNew)
    val topNewArtistPlayCount: Int  // Play count for the top new artist
)

internal class SpotlightDashboardCardGenerator(
    private val context: Context,
    private val repository: StatsRepository,
) {
    companion object {
        private const val TAG = "SpotlightDashboardCards"
    }

    suspend fun generateCards(timeRange: TimeRange): List<SpotlightCardData> = coroutineScope {
        // PHASE 1: Batch pre-fetch all shared data in parallel
        // This replaces individual calls within each generator, reducing DB round-trips
        val overviewDeferred = async { repository.getListeningOverview(timeRange) }
        val hourlyDistDeferred = async { repository.getHourlyDistribution(timeRange) }
        val dayOfWeekDistDeferred = async { repository.getDayOfWeekDistribution(timeRange) }
        val discoveryStatsDeferred = async { repository.getDiscoveryStats(timeRange) }
        val topTracksDeferred = async { repository.getTopTracks(timeRange, SortBy.PLAY_COUNT, pageSize = 20) }
        val artistFirstListensDeferred = async { repository.getArtistFirstListens() }
        val artistLoyaltyDeferred = async { repository.getArtistLoyalty(timeRange, minPlays = 10, limit = 5) }
        val topGenresDeferred = async { repository.getTopGenres(timeRange, limit = 5) }
        
        // Wait for base data first (needed to determine batch fetch targets)
        val discoveryStats = discoveryStatsDeferred.await()
        val artistFirstListens = artistFirstListensDeferred.await()
        val artistLoyalty = artistLoyaltyDeferred.await()
        
        // PHASE 1.5: Batch pre-fetch artist data to eliminate N+1 queries
        // Collect all artist names we'll need play counts for (from first listens)
        val startTs = timeRange.getStartTimestamp()
        val recentDiscoveryArtistNames = artistFirstListens
            .filter { it.firstListenTimestamp >= startTs && it.type == "artist" }
            .map { it.name }
            .take(30)  // Limit to avoid excessive queries
        
        // Collect loyalty candidate names for image URL fetching
        val loyaltyArtistNames = artistLoyalty.map { it.artist }
        
        // Combine all artist names that need image URLs (discovery + loyalty + topNewArtist)
        val allArtistNamesForImages = (recentDiscoveryArtistNames + loyaltyArtistNames + listOfNotNull(discoveryStats.topNewArtist)).distinct()
        
        // Batch fetch in parallel
        val discoveryPlayCountsDeferred = async { 
            repository.getArtistPlayCountsBatch(recentDiscoveryArtistNames) 
        }
        // Now fetches images for ALL artists who need them (discovery + loyalty)
        val allArtistImagesDeferred = async { 
            repository.getArtistImageUrlsBatch(allArtistNamesForImages) 
        }
        val topNewArtistPlayCountDeferred = async {
            discoveryStats.topNewArtist?.let { name ->
                repository.getArtistPlayCountsBatch(listOf(name))[name] ?: 0
            } ?: 0
        }
        
        // Await all pre-fetched data
        val prefetchedData = PrefetchedData(
            overview = overviewDeferred.await(),
            hourlyDist = hourlyDistDeferred.await(),
            dayOfWeekDist = dayOfWeekDistDeferred.await(),
            discoveryStats = discoveryStats,
            topTracks = topTracksDeferred.await(),
            artistFirstListens = artistFirstListens,
            artistLoyalty = artistLoyalty,
            topGenres = topGenresDeferred.await(),
            // Batch-fetched data
            discoveryArtistPlayCounts = discoveryPlayCountsDeferred.await(),
            allArtistImageUrls = allArtistImagesDeferred.await(),  // Now includes all artists
            topNewArtistPlayCount = topNewArtistPlayCountDeferred.await()
        )
        
        Log.d(TAG, "Pre-fetched all data for card generation (including ${recentDiscoveryArtistNames.size} discovery artists)")

        // PHASE 2: Launch all card generators in parallel with pre-fetched data
        val deferreds = listOf(
            async { generateCosmicClock(timeRange, prefetchedData) },
            async { generateWeekendWarrior(timeRange, prefetchedData) },
            async { generateForgottenFavorite(timeRange, prefetchedData) },
            async { generateDeepDive(timeRange, prefetchedData) },
            async { generateNewObsession(timeRange, prefetchedData) },
            async { generateEarlyAdopter(timeRange, prefetchedData) },
            async { generateArtistLoyalty(timeRange, prefetchedData) },
            async { generateDiscovery(timeRange, prefetchedData) }
        )

        // Await all results
        val results = deferreds.awaitAll()

        // Log failures for debugging
        results.forEach { result ->
            when (result) {
                is CardGenerationResult.InsufficientData -> 
                    Log.d(TAG, "Card skipped: ${result.cardType} - ${result.reason}")
                is CardGenerationResult.Error -> 
                    Log.e(TAG, "Card generation failed: ${result.cardType}", result.exception)
                is CardGenerationResult.Success -> 
                    Log.d(TAG, "Card generated: ${result.card.id}")
            }
        }

        // Extract successful cards and shuffle
        results
            .filterIsInstance<CardGenerationResult.Success>()
            .map { it.card }
            .shuffled()
    }


    // Individual Card Generators with Validation

    private fun generateCosmicClock(timeRange: TimeRange, data: PrefetchedData): CardGenerationResult {
        return try {
            val hourlyDist = data.hourlyDist
            var dayPlays = 0
            var nightPlays = 0
            
            hourlyDist.forEach { 
                if (it.hour in 6..17) dayPlays += it.playCount
                else nightPlays += it.playCount
            }

            val total = dayPlays + nightPlays
            val hoursWithData = hourlyDist.count { it.playCount > 0 }
            
            // Time-range-specific thresholds (Phase 2)
            val minPlays = when (timeRange) {
                TimeRange.THIS_WEEK -> 3
                TimeRange.THIS_MONTH -> 5  // More lenient for new users
                TimeRange.THIS_YEAR -> 8
                else -> 10
            }
            
            val minHours = when (timeRange) {
                TimeRange.THIS_WEEK -> 2
                TimeRange.THIS_MONTH -> 2
                else -> 3
            }
            
            // Validation: Minimum plays across distinct hours
            if (total < minPlays) {
                return CardGenerationResult.InsufficientData(
                    "cosmic_clock",
                    "Need at least $minPlays plays for $timeRange (has $total)"
                )
            }
            
            if (hoursWithData < minHours) {
                return CardGenerationResult.InsufficientData(
                    "cosmic_clock",
                    "Need activity across at least $minHours hours (has $hoursWithData)"
                )
            }
            
            // Get top genre from pre-fetched data
            val genre = data.topGenres.firstOrNull()?.genre ?: "Music"

            // Create 24-hour distribution array with better normalization
            // Calculate confidence score based on data amount (Phase 3)
            val confidence = when {
                total >= 50 && hoursWithData >= 6 -> "HIGH"
                total >= 20 && hoursWithData >= 4 -> "MEDIUM"
                else -> "LOW"
            }
            
            val maxCount = hourlyDist.maxOfOrNull { it.playCount } ?: 1
            val hourlyLevels = List(24) { hour ->
                val count = hourlyDist.find { it.hour == hour }?.playCount ?: 0
                if (maxCount < 5) {
                    // If max is very small, use absolute scaling to avoid noise
                    (count * 20).coerceAtMost(100)
                } else {
                    ((count.toDouble() / maxCount) * 100).toInt()
                }
            }

            val dayPct = ((dayPlays.toDouble() / total) * 100).toInt()
            val nightPct = ((nightPlays.toDouble() / total) * 100).toInt()

            CardGenerationResult.Success(
                SpotlightCardData.CosmicClock(
                    dayPercentage = dayPct,
                    nightPercentage = nightPct,
                    dayTopGenre = "Music",
                    nightTopGenre = "Music",
                    sunListenerType = if (dayPct > nightPct) context.getString(R.string.insight_sun_chaser) else context.getString(R.string.insight_moon_owl),
                    hourlyLevels = hourlyLevels,
                    confidence = confidence // Phase 3: Data quality indicator
                )
            )
        } catch (e: Exception) {
            CardGenerationResult.Error("cosmic_clock", e)
        }
    }

    private fun generateWeekendWarrior(timeRange: TimeRange, data: PrefetchedData): CardGenerationResult {
        return try {
            val dailyDist = data.dayOfWeekDist
            var weekdayPlays = 0
            var weekendPlays = 0
            
            dailyDist.forEach {
                if (it.dayOfWeek in 1..5) weekdayPlays += it.playCount
                else weekendPlays += it.playCount
            }
            
            // Count actual days with data (not theoretical 5/2)
            val weekdayDaysWithData = dailyDist.count { it.dayOfWeek in 1..5 && it.playCount > 0 }
            val weekendDaysWithData = dailyDist.count { it.dayOfWeek in 6..7 && it.playCount > 0 }
            
            // Validation: Require minimum data
            if (weekdayDaysWithData < 2 && weekendDaysWithData < 1) {
                return CardGenerationResult.InsufficientData(
                    "weekend_warrior",
                    "Need at least 2 weekdays and 1 weekend day with activity"
                )
            }
            
            // Use actual days with data for averaging
            val weekdayAvg = if (weekdayDaysWithData > 0) weekdayPlays / weekdayDaysWithData else 0
            val weekendAvg = if (weekendDaysWithData > 0) weekendPlays / weekendDaysWithData else 0
            
            // Time-range-specific pattern strength (Phase 2)
            val minDifference = when (timeRange) {
                TimeRange.THIS_WEEK -> 2
                TimeRange.THIS_MONTH -> 3  // More lenient for new users
                else -> 5
            }
            
            // Require significant difference
            if (kotlin.math.abs(weekendAvg - weekdayAvg) < minDifference) {
                return CardGenerationResult.InsufficientData(
                    "weekend_warrior",
                    "Pattern not strong enough (difference < $minDifference plays/day)"
                )
            }
            
            // Better warrior type logic with edge case handling
            val warriorType = when {
                weekdayAvg == 0 && weekendAvg > 10 -> context.getString(R.string.insight_pure_weekend_warrior)
                weekendAvg == 0 && weekdayAvg > 10 -> context.getString(R.string.insight_weekday_only)
                weekendAvg > (weekdayAvg * 1.5) -> context.getString(R.string.insight_weekend_warrior)
                weekdayAvg > (weekendAvg * 1.5) -> context.getString(R.string.insight_daily_grinder)
                else -> context.getString(R.string.insight_consistent_vibez)
            }
            
            // Safe percentage calculation
            // Calculate confidence based on data spread (Phase 3)
            val totalDaysWithData = weekdayDaysWithData + weekendDaysWithData
            val confidence = when {
                totalDaysWithData >= 14 && weekdayDaysWithData >= 8 && weekendDaysWithData >= 4 -> "HIGH"
                totalDaysWithData >= 7 && weekdayDaysWithData >= 4 && weekendDaysWithData >= 2 -> "MEDIUM"
                else -> "LOW"
            }
            
            val weekdayPct = if (weekdayPlays + weekendPlays > 0) {
                (weekdayPlays.toDouble() / (weekdayPlays + weekendPlays) * 100).toInt()
            } else {
                50
            }
            val weekendPct = 100 - weekdayPct
            
            CardGenerationResult.Success(
                SpotlightCardData.WeekendWarrior(
                    weekdayAverage = weekdayAvg,
                    weekendAverage = weekendAvg,
                    warriorType = warriorType,
                    percentageDifference = kotlin.math.abs(weekdayPct - weekendPct),
                    confidence = confidence // Phase 3: Data quality indicator
                )
            )
        } catch (e: Exception) {
            CardGenerationResult.Error("weekend_warrior", e)
        }

    }

    private fun generateForgottenFavorite(timeRange: TimeRange, data: PrefetchedData): CardGenerationResult {
        return try {
            // Only show for time ranges where "forgetting" makes sense
            if (timeRange == TimeRange.ALL_TIME) {
                return CardGenerationResult.InsufficientData(
                    "forgotten_favorite",
                    "Not applicable for ALL_TIME range"
                )
            }
            
            // Use pre-fetched top tracks (Note: ideally we'd fetch ALL_TIME separately, but this is a reasonable approximation)
            val allTimeTop = data.topTracks.items
            val currentThresholdDate = timeRange.getStartTimestamp()
            
            // Time-range-specific requirements  (Phase 2)
            val minPlayCount = when (timeRange) {
                TimeRange.THIS_WEEK -> 5
                TimeRange.THIS_MONTH -> 8
                else -> 10
            }
            
            val minRelationshipDays = when (timeRange) {
                TimeRange.THIS_WEEK -> 3L   // 3 days
                TimeRange.THIS_MONTH -> 7L  // 1 week
                else -> 14L  // 2 weeks
            }
            
            val forgotten = allTimeTop.firstOrNull { track ->
                track.playCount >= minPlayCount &&
                track.lastPlayed < currentThresholdDate &&
                (track.lastPlayed - track.firstPlayed) > minRelationshipDays * 24 * 60 * 60 * 1000
            }

            if (forgotten == null) {
                return CardGenerationResult.InsufficientData(
                    "forgotten_favorite",
                    "No qualifying forgotten tracks found"
                )
            }
            
            val daysSince = ((System.currentTimeMillis() - forgotten.lastPlayed) / (1000 * 60 * 60 * 24)).toInt()
            
            // More gradual peak date descriptions (Phase 2)
            val peakDate = when {
                daysSince < 30 -> context.getString(R.string.insight_last_month)
                daysSince < 60 -> context.getString(R.string.insight_couple_months_ago)
                daysSince < 90 -> context.getString(R.string.insight_few_months_ago)
                daysSince < 180 -> context.getString(R.string.insight_earlier_year)
                daysSince < 365 -> context.getString(R.string.insight_last_year)
                else -> context.getString(R.string.insight_while_ago)
            }

            CardGenerationResult.Success(
                SpotlightCardData.ForgottenFavorite(
                    songTitle = forgotten.title,
                    artistName = forgotten.artist,
                    albumArtUrl = forgotten.albumArtUrl ?: "",
                    peakDate = peakDate,
                    daysSinceLastPlay = daysSince
                )
            )
        } catch (e: Exception) {
            CardGenerationResult.Error("forgotten_favorite", e)
        }
    }

    private fun generateDeepDive(timeRange: TimeRange, data: PrefetchedData): CardGenerationResult {
        return try {
            if (data.overview.longestSessionMs < 30 * 60 * 1000) {
                return CardGenerationResult.InsufficientData(
                    "deep_dive",
                    "Longest session too short (< 30 mins)"
                )
            }

            CardGenerationResult.Success(
                SpotlightCardData.DeepDive(
                    durationMinutes = (data.overview.longestSessionMs / 60000).toInt(),
                    date = context.getString(R.string.insight_recently),
                    timeOfDay = context.getString(R.string.insight_the_zone),
                    topArtist = null
                )
            )
        } catch (e: Exception) {
            CardGenerationResult.Error("deep_dive", e)
        }
    }

    /**
     * Generate New Obsession card - Optimized to use pre-fetched play count.
     * No longer calls getArtistDetailsByName during parallel execution.
     */
    private suspend fun generateNewObsession(timeRange: TimeRange, data: PrefetchedData): CardGenerationResult {
        return try {
            val discovery = data.discoveryStats
            
            if (discovery.topNewArtist == null) {
                return CardGenerationResult.InsufficientData(
                    "new_obsession",
                    "No new artists discovered in period"
                )
            }
            
            // Use pre-fetched play count instead of expensive getArtistDetailsByName call
            val playCount = data.topNewArtistPlayCount
            val minPlays = when (timeRange) {
                TimeRange.THIS_WEEK -> 2
                TimeRange.THIS_MONTH -> 3
                TimeRange.THIS_YEAR -> 6
                else -> 12
            }
            
            if (playCount < minPlays) {
                return CardGenerationResult.InsufficientData(
                    "new_obsession",
                    "Artist doesn't meet minimum play count ($minPlays)"
                )
            }
            
            val totalPlaysInPeriod = data.overview.totalPlayCount
            val percentOfListening = if (totalPlaysInPeriod > 0) {
                (playCount.toDouble() / totalPlaysInPeriod * 100).toInt()
            } else {
                0
            }
            
            if (percentOfListening < 3) {
                return CardGenerationResult.InsufficientData(
                    "new_obsession",
                    "Artist isn't significant enough (< 3% of listening)"
                )
            }
            
            // Get image URL from batch-fetched data or fallback
            val imageUrl = data.allArtistImageUrls[discovery.topNewArtist]
                ?: repository.getArtistImageUrl(discovery.topNewArtist) ?: ""
            
            // Estimate days known from first listen data
            val firstListen = data.artistFirstListens.find { it.name == discovery.topNewArtist }
            val daysKnown = if (firstListen != null) {
                ((System.currentTimeMillis() - firstListen.firstListenTimestamp) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
            } else {
                1
            }
            
            val confidence = when {
                playCount >= 30 && percentOfListening >= 10 -> "HIGH"
                playCount >= 10 && percentOfListening >= 5 -> "MEDIUM"
                else -> "LOW"
            }

            CardGenerationResult.Success(
                SpotlightCardData.NewObsession(
                    artistName = discovery.topNewArtist,
                    artistImageUrl = imageUrl,
                    playCount = playCount,
                    daysKnown = daysKnown,
                    percentOfListening = percentOfListening,
                    confidence = confidence
                )
            )
        } catch (e: Exception) {
            CardGenerationResult.Error("new_obsession", e)
        }
    }

    /**
     * Generate Early Adopter card - Optimized with pre-fetched play counts.
     * Eliminated N+1 query problem by using batch-fetched discoveryArtistPlayCounts.
     */
    private fun generateEarlyAdopter(timeRange: TimeRange, data: PrefetchedData): CardGenerationResult {
        return try {
            if (timeRange == TimeRange.ALL_TIME) {
                return CardGenerationResult.InsufficientData(
                    "early_adopter",
                    "Not applicable for ALL_TIME range"
                )
            }
            
            val firstListens = data.artistFirstListens
            val startTs = timeRange.getStartTimestamp()
            val recentDiscoveries = firstListens.filter { 
                it.firstListenTimestamp >= startTs && it.type == "artist"
            }
            
            if (recentDiscoveries.isEmpty()) {
                return CardGenerationResult.InsufficientData(
                    "early_adopter",
                    "No new artists discovered in this period"
                )
            }
            
            // Use pre-fetched play counts to find qualifying discovery (NO N+1 QUERIES!)
            val discovery = recentDiscoveries.firstOrNull { disc ->
                val playCount = data.discoveryArtistPlayCounts[disc.name] ?: 0
                playCount >= 8
            }
            
            if (discovery == null) {
                return CardGenerationResult.InsufficientData(
                    "early_adopter",
                    "No discoveries with significant engagement"
                )
            }
            
            // Use pre-fetched image URL from batch data
            val artistImageUrl = data.discoveryArtistPlayCounts[discovery.name]?.let {
                data.allArtistImageUrls[discovery.name] ?: ""
            } ?: ""

            CardGenerationResult.Success(
                SpotlightCardData.EarlyAdopter(
                    artistName = discovery.name,
                    artistImageUrl = artistImageUrl,
                    discoveryDate = discovery.firstListenDate.toString(),
                    daysBeforeMainstream = 0
                )
            )
        } catch (e: Exception) {
            CardGenerationResult.Error("early_adopter", e)
        }
    }

    private fun generateListeningPeak(timeRange: TimeRange, data: PrefetchedData): CardGenerationResult {
        return try {
            val hourlyDist = data.hourlyDist
            val maxHour = hourlyDist.maxByOrNull { it.playCount }
            // Validation: Minimum threshold (lowered for new users)
            if (maxHour == null || maxHour.playCount < 5) {
                return CardGenerationResult.InsufficientData(
                    "listening_peak",
                    "Peak hour doesn't meet minimum threshold"
                )
            }
            
            val peakHourData = hourlyDist.filter { dist ->
                val h = dist.hour
                when {
                    maxHour.hour < 2 -> h >= maxHour.hour + 22 || h <= maxHour.hour + 2
                    maxHour.hour > 21 -> h >= maxHour.hour - 2 || h <= (maxHour.hour + 2) % 24
                    else -> h in (maxHour.hour - 2)..(maxHour.hour + 2)
                }
            }
            
            val totalMinutes = (peakHourData.sumOf { it.totalTimeMs } / 60000).toInt()
            val start = peakHourData.minOfOrNull { it.hour } ?: maxHour.hour
            val end = peakHourData.maxOfOrNull { it.hour } ?: maxHour.hour

            CardGenerationResult.Success(
                SpotlightCardData.ListeningPeak(
                    peakTime = "%02d:00".format(maxHour.hour),
                    peakTimeRange = "%02d:00 - %02d:00".format(start, end),
                    totalMinutes = totalMinutes
                )
            )
        } catch (e: Exception) {
            CardGenerationResult.Error("listening_peak", e)
        }
    }


    private suspend fun generateArtistLoyalty(timeRange: TimeRange, data: PrefetchedData): CardGenerationResult {
        return try {
            // "The Deep Cut" - Focus on Catalog Depth (Unique Tracks)
            // We want artists where the user explores deeply, not just plays hits.
            
            val loyaltyStats = data.artistLoyalty
            
            if (loyaltyStats.isEmpty()) {
                return CardGenerationResult.InsufficientData(
                    "artist_loyalty",
                    "No artists meet minimum play threshold for loyalty analysis"
                )
            }
            
            // Filter for "Deep Cut" candidates: High unique track count
            // We prioritize breadth (unique tracks) over raw volume.
            val candidate = loyaltyStats.maxByOrNull { it.uniqueTracksPlayed }
            
            if (candidate == null || candidate.uniqueTracksPlayed < 5) { // Minimum 5 unique tracks to be a "deep cut" fan
                 return CardGenerationResult.InsufficientData(
                    "artist_loyalty",
                    "No artist with sufficient catalog depth (> 4 unique tracks)"
                )
            }
            
            // Use pre-fetched artist image from batch data (no DB call during parallel execution)
            val artistImageUrl = data.allArtistImageUrls[candidate.artist]

            // Calculate "Loyalty Score" based on depth
            // 20+ unique tracks = 100%
            // 5 unique tracks = 25% represents start
            val loyaltyScore = ((candidate.uniqueTracksPlayed / 20.0) * 100).toInt().coerceAtMost(100)
            
            val confidence = when {
                candidate.uniqueTracksPlayed >= 20 -> "HIGH"
                candidate.uniqueTracksPlayed >= 10 -> "MEDIUM"
                else -> "LOW"
            }

            CardGenerationResult.Success(
                SpotlightCardData.ArtistLoyalty(
                    artistName = candidate.artist,
                    artistImageUrl = artistImageUrl,
                    uniqueTrackCount = candidate.uniqueTracksPlayed,
                    topTrackName = "Unknown Track",
                    loyaltyScore = loyaltyScore,
                    confidence = confidence
                )
            )
        } catch (e: Exception) {
            CardGenerationResult.Error("artist_loyalty", e)
        }
    }

    private fun generateDiscovery(timeRange: TimeRange, data: PrefetchedData): CardGenerationResult {
        return try {
            // "The Sonic Horizon" - Focus on New vs Repeat (Discovery)
            val discoveryStats = data.discoveryStats
            
            // Check sufficiency
            if (discoveryStats.newTracksCount == 0 && discoveryStats.repeatListensCount == 0) {
                 return CardGenerationResult.InsufficientData(
                    "discovery",
                    "No discovery data available"
                )
            }
            
            val totalListens = (discoveryStats.newTracksCount + discoveryStats.repeatListensCount).toDouble()
            val newRatio = if (totalListens > 0) discoveryStats.newTracksCount / totalListens else 0.0
            val newPercent = (newRatio * 100).toInt()
            
            // Determine Type
            val type = when {
                newPercent >= 70 -> context.getString(R.string.insight_explorer)
                newPercent <= 30 -> context.getString(R.string.insight_time_traveler)
                else -> context.getString(R.string.insight_orbiter)
            }
            
            val description = when {
                newPercent >= 70 -> {
                    if (discoveryStats.topNewArtist != null)
                        context.getString(R.string.insight_desc_explorer_with_artist, newPercent, discoveryStats.topNewArtist)
                    else
                        context.getString(R.string.insight_desc_explorer_no_artist, newPercent)
                }
                newPercent <= 30 -> {
                    if (discoveryStats.topNewArtist != null)
                        context.getString(R.string.insight_desc_time_traveler_with_artist, 100 - newPercent, discoveryStats.topNewArtist)
                    else
                        context.getString(R.string.insight_desc_time_traveler_no_artist, 100 - newPercent)
                }
                else -> {
                    if (discoveryStats.topNewArtist != null)
                        context.getString(R.string.insight_desc_orbiter_with_artist, discoveryStats.topNewArtist.toString(), 100 - newPercent)
                    else
                        context.getString(R.string.insight_desc_orbiter_no_artist, 100 - newPercent)
                }
            }

            CardGenerationResult.Success(
                SpotlightCardData.Discovery(
                    discoveryType = type,
                    newContentPercentage = newPercent,
                    varietyScore = (discoveryStats.varietyScore * 10).toInt().coerceIn(0, 100),
                    topNewArtist = discoveryStats.topNewArtist,
                    description = description
                )
            )
        } catch (e: Exception) {
            CardGenerationResult.Error("discovery", e)
        }
    }

}
