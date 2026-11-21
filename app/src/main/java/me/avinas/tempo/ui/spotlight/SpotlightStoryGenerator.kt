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

internal class SpotlightStoryGenerator(
    private val context: Context,
    private val repository: StatsRepository,
    private val gamificationRepository: GamificationRepository,
) {
    companion object {
        private const val TAG = "SpotlightStoryGenerator"
    }

    suspend fun generateStory(timeRange: TimeRange): List<SpotlightStoryPage> = kotlinx.coroutines.coroutineScope {
        val storyPages = mutableListOf<SpotlightStoryPage>()
        // Collects eligible optional slides; 2–4 are selected per session via seeded random.
        // LevelUp / TitleEarned are inserted directly into storyPages (milestone-triggered permanent).
        val optionalPool = mutableListOf<SpotlightStoryPage>()

        // PARALLEL DATA FETCH: All repository calls run concurrently
        val topTracksDeferred = async { 
            repository.getTopTracks(timeRange, sortBy = me.avinas.tempo.data.repository.SortBy.COMBINED_SCORE, pageSize = 20) 
        }
        val overviewDeferred = async { repository.getListeningOverview(timeRange) }
        val topArtistsDeferred = async { 
            repository.getTopArtists(timeRange, sortBy = me.avinas.tempo.data.repository.SortBy.COMBINED_SCORE, pageSize = 10) 
        }
        val topGenresDeferred = async { repository.getTopGenres(timeRange, limit = 5) }
        // NEW: Additional parallel fetches for new pages
        val streakDeferred = async { repository.getListeningStreak() }
        val hourlyDistDeferred = async { repository.getHourlyDistribution(timeRange) }
        val topAlbumsDeferred = async { repository.getTopAlbums(timeRange, pageSize = 5) }
        val discoveryStatsDeferred = async { repository.getDiscoveryStats(timeRange) }
        val audioFeaturesDeferred = async { repository.getAudioFeaturesStats(timeRange) }
        val dayOfWeekDistDeferred = async { repository.getDayOfWeekDistribution(timeRange) }
        
        // Await all data in parallel
        val topTracksResult = topTracksDeferred.await()
        val overview = overviewDeferred.await()
        val topArtistsResult = topArtistsDeferred.await()
        val topGenres = topGenresDeferred.await()
        // NEW: Await additional data
        val streak = try { streakDeferred.await() } catch (e: Exception) { null }
        val hourlyDist = try { hourlyDistDeferred.await() } catch (e: Exception) { emptyList() }
        val topAlbumsList = try { topAlbumsDeferred.await().items } catch (e: Exception) { emptyList() }
        val discoveryStats = try { discoveryStatsDeferred.await() } catch (e: Exception) { null }
        val audioFeatures = try { audioFeaturesDeferred.await() } catch (e: Exception) { null }
        val dayOfWeekDist = try { dayOfWeekDistDeferred.await() } catch (e: Exception) { emptyList() }
        
        val topTracksList = topTracksResult.items
        
        // "Soundtrack Pool": Tracks that strictly HAVE a previewUrl.
        // Prioritize iTunes (stable URLs) over Deezer (expire quickly with auth tokens).
        val soundtrackList = topTracksList
            .filter { it.previewUrl != null }
            .sortedBy { if (it.previewUrl!!.contains("itunes.apple.com")) 0 else 1 }
        
        // Critical: The "Reveal" MUST be the actual #1 track (visuals), even if it has no audio.
        val trueTopTrack = topTracksList.firstOrNull()

        // 1. Listening Minutes (using pre-fetched overview)
        val totalMinutes = (overview.totalListeningTimeMs / 60000).toInt()
        
        val minutesText = when {
            totalMinutes > 100000 -> "Music isn't just a hobby, it's your oxygen."
            totalMinutes > 50000 -> "That's more than most people listen in a lifetime."
            totalMinutes > 20000 -> "A steady rhythm, not background noise."
            totalMinutes > 5000 -> "You kept the vibe going strong."
            else -> "Quality over quantity, always."
        }
        
        storyPages.add(
            SpotlightStoryPage.ListeningMinutes(
                conversationalText = minutesText,
                totalMinutes = totalMinutes,
                userName = "User",
                year = java.time.LocalDate.now().year,
                timeRange = timeRange,
                comparativeText = when {
                    totalMinutes > 50000 -> context.getString(R.string.spotlight_compare_top_percent, 99)
                    totalMinutes > 30000 -> context.getString(R.string.spotlight_compare_top_percent, 95)
                    totalMinutes > 20000 -> context.getString(R.string.spotlight_compare_top_percent, 90)
                    totalMinutes > 10000 -> context.getString(R.string.spotlight_compare_top_percent, 75)
                    else -> null
                },
                previewUrl = null
            )
        )

        // 1b. Listening Streak (fire-themed, right after minutes)
        if (streak != null && streak.currentStreakDays >= 1) {
            val streakText = when {
                streak.currentStreakDays >= 30 -> "Unstoppable. Music is part of you."
                streak.currentStreakDays >= 14 -> "Two weeks strong — the habit has formed."
                streak.currentStreakDays >= 7 -> "A full week. Your rhythm is solid."
                streak.currentStreakDays >= 3 -> "Three days and counting. Keep the fire alive."
                else -> "Every day counts. Keep listening."
            }
            optionalPool.add(
                SpotlightStoryPage.ListeningStreak(
                    conversationalText = streakText,
                    currentStreakDays = streak.currentStreakDays,
                    longestStreakDays = streak.longestStreakDays,
                    totalActiveDays = streak.totalActiveDays,
                    comparativeText = when {
                        streak.currentStreakDays >= 30 -> context.getString(R.string.spotlight_compare_streak_percent, 95)
                        streak.currentStreakDays >= 14 -> context.getString(R.string.spotlight_compare_streak_percent, 85)
                        streak.currentStreakDays >= 7 -> context.getString(R.string.spotlight_compare_streak_percent, 70)
                        else -> null
                    },
                    previewUrl = null
                )
            )
        }

        // 1b-extra. Binge Session (estimated from top artist listening data)
        if (topArtistsResult.items.isNotEmpty()) {
            val topBingeArtist = topArtistsResult.items.firstOrNull()
            if (topBingeArtist != null) {
                val bingeCount = (topBingeArtist.totalTimeMs / 210_000L)
                    .toInt().coerceIn(3, 80)
                val bingeMinutes = (topBingeArtist.totalTimeMs / 60_000L).toInt().coerceAtMost(600)
                if (bingeCount >= 5) {
                    val bingeText = when {
                        bingeCount >= 30 -> "That's dedication. Or obsession. Probably both."
                        bingeCount >= 15 -> "You found your groove and stayed there."
                        else -> "When you love an artist, you commit."
                    }
                    optionalPool.add(
                        SpotlightStoryPage.BingeSession(
                            conversationalText = bingeText,
                            artistName = topBingeArtist.artist,
                            bingeCount = bingeCount,
                            totalBingeMinutes = bingeMinutes,
                            previewUrl = null
                        )
                    )
                }
            }
        }

        // 1c. Listening Clock (24h radial visualization)
        if (hourlyDist.isNotEmpty() && hourlyDist.any { it.playCount > 0 }) {
            val maxCount = hourlyDist.maxOfOrNull { it.playCount } ?: 1
            val hourlyLevels = List(24) { hour ->
                val count = hourlyDist.find { it.hour == hour }?.playCount ?: 0
                ((count.toDouble() / maxCount) * 100).toInt()
            }
            val peakHour = hourlyDist.maxByOrNull { it.playCount }?.hour ?: 20
            val peakHourLabel = when {
                peakHour == 0 -> "12 AM"
                peakHour < 12 -> "$peakHour AM"
                peakHour == 12 -> "12 PM"
                else -> "${peakHour - 12} PM"
            }
            val listenerType = when {
                peakHour >= 22 || peakHour < 4 -> "Night Owl 🦉"
                peakHour in 4..8 -> "Early Bird 🐦"
                peakHour in 9..12 -> "Morning Listener ☀️"
                peakHour in 13..17 -> "Afternoon Groover 🎧"
                else -> "Evening Vibes 🌙"
            }
            val clockText = when (peakHour) {
                in 22..23, in 0..3 -> "You come alive when the world sleeps."
                in 4..8 -> "The early hours belong to you and your music."
                in 9..12 -> "Mornings hit different with the right soundtrack."
                in 13..17 -> "Afternoons fuel the groove."
                else -> "The evening is your listening hour."
            }
            optionalPool.add(
                SpotlightStoryPage.ListeningClock(
                    conversationalText = clockText,
                    hourlyLevels = hourlyLevels,
                    peakHour = peakHour,
                    peakHourLabel = peakHourLabel,
                    listenerType = listenerType,
                    previewUrl = null
                )
            )
        }

        // 1d. Weekday vs Weekend (from pre-fetched day-of-week distribution)
        if (dayOfWeekDist.isNotEmpty()) {
            val weekdayEntries = dayOfWeekDist.filter { it.dayOfWeek in 1..5 }
            val weekendEntries = dayOfWeekDist.filter { it.dayOfWeek in 6..7 }
            val weekdayAvg = if (weekdayEntries.isNotEmpty())
                (weekdayEntries.sumOf { it.totalTimeMs / 60_000L } / weekdayEntries.size).toInt() else 0
            val weekendAvg = if (weekendEntries.isNotEmpty())
                (weekendEntries.sumOf { it.totalTimeMs / 60_000L } / weekendEntries.size).toInt() else 0
            if (weekdayAvg > 0 || weekendAvg > 0) {
                val maxMinutes = dayOfWeekDist.maxOfOrNull { it.totalTimeMs / 60_000L }?.toFloat() ?: 1f
                val intensities = (1..7).map { d ->
                    val entry = dayOfWeekDist.find { it.dayOfWeek == d }
                    ((entry?.totalTimeMs?.toFloat()?.div(60_000f) ?: 0f) / maxMinutes * 100).toInt()
                }
                val dominant = if (weekendAvg > weekdayAvg) "weekend" else "weekday"
                val wkdLabel = when {
                    weekdayAvg > 60 -> "Daily Grinder 💼"
                    weekdayAvg > 30 -> "Work Soundtrack"
                    else -> "Casual Weekdayer"
                }
                val wkndLabel = when {
                    weekendAvg > 90 -> "Weekend Binger 🎉"
                    weekendAvg > 45 -> "Relaxed Weekender"
                    else -> "Balanced Listener"
                }
                val wkText = when (dominant) {
                    "weekend" -> "Weekends are your real listening sessions."
                    else -> "You keep the music going even on busy days."
                }
                optionalPool.add(
                    SpotlightStoryPage.WeekdayVsWeekend(
                        conversationalText = wkText,
                        weekdayAvgMinutes = weekdayAvg,
                        weekendAvgMinutes = weekendAvg,
                        weekdayLabel = wkdLabel,
                        weekendLabel = wkndLabel,
                        dominantSide = dominant,
                        dailyIntensity = intensities,
                        previewUrl = null
                    )
                )
            }
        }

        // 2. Top Artists (using pre-fetched data)
        val topArtistsList = topArtistsResult.items
        
        if (topArtistsList.isNotEmpty()) {
            val topArtist = topArtistsList.first()
            
            val timeText = when (timeRange) {
                TimeRange.THIS_WEEK -> "this week"
                TimeRange.THIS_MONTH -> "this month"
                TimeRange.ALL_TIME -> "of all time"
                else -> "this year"
            }
            val artistText = "They defined your sound $timeText."

            val topArtistEntry = SpotlightStoryPage.TopArtist(
                conversationalText = artistText,
                topArtistName = topArtist.artist,
                topArtistImageUrl = topArtist.imageUrl,
                topArtistPercentage = 0,
                topArtists = topArtistsList.mapIndexed { index, artist ->
                    SpotlightStoryPage.TopArtist.ArtistEntry(
                        rank = index + 1,
                        name = artist.artist,
                        hoursListened = (artist.totalTimeMs / 3600000).toInt(),
                        imageUrl = artist.imageUrl
                    )
                },
                previewUrl = null
            )
            storyPages.add(topArtistEntry)
        }

        // 2b. Top Album (cinematic reveal) — skip single-track albums (they're just songs)
        val topAlbumCandidate = topAlbumsList.firstOrNull { it.uniqueTracks > 1 }
        if (topAlbumCandidate != null) {
            val albumText = when {
                topAlbumCandidate.playCount > 50 -> "You lived inside this album."
                topAlbumCandidate.playCount > 20 -> "This was your go-to record."
                else -> "This album resonated with you."
            }
            optionalPool.add(
                SpotlightStoryPage.TopAlbum(
                    conversationalText = albumText,
                    albumName = topAlbumCandidate.album,
                    artistName = topAlbumCandidate.artist,
                    albumArtUrl = topAlbumCandidate.albumArtUrl,
                    playCount = topAlbumCandidate.playCount,
                    totalTimeMs = topAlbumCandidate.totalTimeMs,
                    uniqueTracksPlayed = topAlbumCandidate.uniqueTracks,
                    previewUrl = null
                )
            )
        }

        // 3. Top Songs
        if (topTracksList.isNotEmpty() && trueTopTrack != null) {
            val topTrack = trueTopTrack
            
            // 3a. Top Track Setup (Slide 1: Audio starts gently)
            val setupText = "But one song set the tone."
            val setupEntry = SpotlightStoryPage.TopTrackSetup(
                conversationalText = setupText,
                topSongTitle = topTrack.title,
                topSongArtist = topTrack.artist,
                topSongImageUrl = topTrack.albumArtUrl,
                timeRange = timeRange,
                previewUrl = null // Assigned in smart song pass
            )
            storyPages.add(setupEntry)

            // 3b. Top Songs Highlight (Slide 2: Volume ramps up)
            val songText = if (topTrack.playCount > 50) 
                "You clearly couldn't get enough of this one."
            else 
                "The track that stuck with you."

            val topSongsEntry = SpotlightStoryPage.TopSongs(
                conversationalText = songText,
                topSongTitle = topTrack.title,
                topSongArtist = topTrack.artist,
                topSongImageUrl = topTrack.albumArtUrl,
                playCount = topTrack.playCount,
                totalTimeMs = topTrack.totalTimeMs,
                topSongs = topTracksList.take(10).mapIndexed { index, track ->
                    SpotlightStoryPage.TopSongs.SongEntry(
                        rank = index + 1,
                        title = track.title,
                        artist = track.artist,
                        playCount = track.playCount,
                        imageUrl = track.albumArtUrl
                    )
                },
                previewUrl = trueTopTrack.previewUrl
            )
            storyPages.add(topSongsEntry)
        }

        // 4. Top Genres (using pre-fetched data)
        if (topGenres.isNotEmpty()) {
            val topGenre = topGenres.first()
            val totalGenreTime = topGenres.sumOf { it.totalTimeMs }
            val topGenrePercentage = if (overview.totalListeningTimeMs > 0) 
                (topGenre.totalTimeMs.toDouble() / overview.totalListeningTimeMs * 100).toInt() 
            else 0
            
            val genreText = "This vibe was your home base."
            
            val genreEntry = SpotlightStoryPage.TopGenres(
                conversationalText = genreText,
                topGenre = topGenre.genre,
                topGenrePercentage = topGenrePercentage,
                genres = topGenres.mapIndexed { index, genre ->
                    SpotlightStoryPage.TopGenres.GenreEntry(
                        rank = index + 1,
                        name = genre.genre,
                        percentage = if (overview.totalListeningTimeMs > 0) 
                            (genre.totalTimeMs.toDouble() / overview.totalListeningTimeMs * 100).toInt() 
                        else 0
                    )
                },
                previewUrl = null
            )
            storyPages.add(genreEntry)
        }

        // 4b. Discovery Count (unique universe page)
        if (overview.uniqueArtistsCount > 0 || overview.uniqueTracksCount > 0) {
            val timeRangeLabel = when (timeRange) {
                TimeRange.THIS_WEEK -> "this week"
                TimeRange.THIS_MONTH -> "this month"
                TimeRange.THIS_YEAR -> "this year"
                TimeRange.ALL_TIME -> "all time"
                else -> "recently"
            }
            val discoveryText = when {
                overview.uniqueArtistsCount > 200 -> "Your musical world knows no limits."
                overview.uniqueArtistsCount > 100 -> "You've built quite a universe."
                overview.uniqueArtistsCount > 50 -> "A rich and varied collection."
                else -> "Every artist counts in your world."
            }
            optionalPool.add(
                SpotlightStoryPage.DiscoveryCount(
                    conversationalText = discoveryText,
                    uniqueArtists = overview.uniqueArtistsCount,
                    uniqueTracks = overview.uniqueTracksCount,
                    newArtistsThisPeriod = discoveryStats?.newArtistsCount ?: 0,
                    timeRangeLabel = timeRangeLabel,
                    comparativeText = when {
                        overview.uniqueArtistsCount > 200 -> context.getString(R.string.spotlight_compare_discovery_percent, 95)
                        overview.uniqueArtistsCount > 100 -> context.getString(R.string.spotlight_compare_discovery_percent, 85)
                        overview.uniqueArtistsCount > 50 -> context.getString(R.string.spotlight_compare_discovery_percent, 70)
                        else -> null
                    },
                    previewUrl = null
                )
            )
        }

        // 4c. Time of Day Vibes (derived from hourlyDist already fetched)
        if (hourlyDist.isNotEmpty() && hourlyDist.any { it.playCount > 0 }) {
            val totalPlays = hourlyDist.sumOf { it.playCount }.coerceAtLeast(1)
            fun pctForHours(hours: IntRange) = ((hourlyDist
                .filter { it.hour in hours }.sumOf { it.playCount }.toDouble() / totalPlays) * 100).toInt()
            val morningPct   = pctForHours(5..11)
            val afternoonPct = pctForHours(12..17)
            val eveningPct   = pctForHours(18..21)
            val nightPct     = (100 - morningPct - afternoonPct - eveningPct).coerceAtLeast(0)
            val domMap = mapOf(
                "morning" to morningPct, "afternoon" to afternoonPct,
                "evening" to eveningPct, "night" to nightPct
            )
            val dominantPeriod = domMap.maxByOrNull { it.value }?.key ?: "evening"
            val vibeText = when (dominantPeriod) {
                "morning"   -> "You start the day with a soundtrack."
                "afternoon" -> "The midday slump doesn't stand a chance."
                "evening"   -> "Your best music moments happen as the sun sets."
                else        -> "The night is yours — and so is the music."
            }
            optionalPool.add(
                SpotlightStoryPage.TimeOfDayVibes(
                    conversationalText = vibeText,
                    morningPercent = morningPct,
                    afternoonPercent = afternoonPct,
                    eveningPercent = eveningPct,
                    nightPercent = nightPct,
                    dominantPeriod = dominantPeriod,
                    previewUrl = null
                )
            )
        }

        // 5. Personality (reuses pre-fetched audioFeatures and discoveryStats — no redundant queries)
        val varietyScore = discoveryStats?.varietyScore ?: 0.0
        val topGenreNames = topGenres.map { it.genre }
        
        // Pair<stable English key, localized (name, description, tag)>
        val personalityResult = if (audioFeatures != null) {
            determineMusicalPersonality(
                energy = audioFeatures.averageEnergy,
                valence = audioFeatures.averageValence,
                danceability = audioFeatures.averageDanceability,
                topGenres = topGenreNames,
                newArtistCount = discoveryStats?.newArtistsCount ?: 0,
                varietyScore = varietyScore
            )
        } else {
            if (topGenreNames.isNotEmpty()) {
                 determineMusicalPersonality(0.5f, 0.5f, 0.5f, topGenreNames, discoveryStats?.newArtistsCount ?: 0, varietyScore)
            } else {
                 "The Melophile" to Triple("The Melophile", "You simply love music in all its forms.", "Good music is good music, period.")
            }
        }

        // 5a. Audio Mood page (only if Spotify audio features are available)
        if (audioFeatures != null && audioFeatures.tracksWithFeatures >= 3) {
            val dominantMood = when {
                audioFeatures.averageValence >= 0.65f && audioFeatures.averageEnergy >= 0.65f -> "Euphoric"
                audioFeatures.averageValence >= 0.6f && audioFeatures.averageDanceability >= 0.6f -> "Joyful"
                audioFeatures.averageEnergy >= 0.7f && audioFeatures.averageValence < 0.4f -> "Intense"
                audioFeatures.averageValence < 0.35f && audioFeatures.averageEnergy < 0.4f -> "Melancholic"
                audioFeatures.averageAcousticness >= 0.5f -> "Acoustic Soul"
                audioFeatures.averageDanceability >= 0.65f -> "Rhythm-Driven"
                audioFeatures.averageEnergy >= 0.6f -> "High Energy"
                audioFeatures.averageValence >= 0.5f -> "Positive Vibes"
                else -> "Deeply Emotive"
            }
            val moodText = when (dominantMood) {
                "Euphoric" -> "Your playlist is basically serotonin."
                "Joyful" -> "Good music and good moods go hand in hand."
                "Intense" -> "You channel raw emotion through sound."
                "Melancholic" -> "You feel music where words fall short."
                "Acoustic Soul" -> "Stripped-back sounds speak to you loudest."
                "Rhythm-Driven" -> "Your body moves even before you notice."
                "High Energy" -> "You run at full volume."
                else -> "Your sound has real emotional depth."
            }
            optionalPool.add(
                SpotlightStoryPage.AudioMood(
                    conversationalText = moodText,
                    energyPercent = (audioFeatures.averageEnergy * 100).toInt(),
                    valencePercent = (audioFeatures.averageValence * 100).toInt(),
                    danceabilityPercent = (audioFeatures.averageDanceability * 100).toInt(),
                    acousticnessPercent = (audioFeatures.averageAcousticness * 100).toInt(),
                    dominantMood = dominantMood,
                    previewUrl = null
                )
            )
        }
        
        storyPages.add(
            SpotlightStoryPage.Personality(
                conversationalText = personalityResult.second.third,
                personalityType = personalityResult.second.first,
                personalityKey = personalityResult.first,
                description = personalityResult.second.second,
                previewUrl = null
            )
        )

        // 5b. Gamification Pages (Badges, Level, Title)
        val LAST_STORY_LEVEL_KEY = intPreferencesKey("spotlight_last_level")
        val LAST_STORY_TITLE_KEY = stringPreferencesKey("spotlight_last_title")
        try {
            // Parallelize gamification + DataStore reads
            val userLevelDeferred = async { gamificationRepository.getUserLevel() }
            val allBadgesDeferred = async { try { gamificationRepository.getAllBadgesSnapshot() } catch (_: Exception) { emptyList() } }
            val uniqueArtistsDeferred = async { try { gamificationRepository.getUniqueArtistCount() } catch (_: Exception) { 0 } }
            val prefsDeferred = async { context.dataStore.data.first() }
            
            val userLevel = userLevelDeferred.await()
            val allBadges = allBadgesDeferred.await()
            val uniqueArtists = uniqueArtistsDeferred.await()
            val prefs = prefsDeferred.await()
            val lastSeenLevel = prefs[LAST_STORY_LEVEL_KEY] ?: -1
            val lastSeenTitle = prefs[LAST_STORY_TITLE_KEY] ?: ""
            val currentTitle = GamificationEngine.computeTitle(userLevel.currentLevel, uniqueArtists)

            val recentlyEarned = allBadges
                .filter { it.isEarned }
                .sortedByDescending { it.earnedAt }
                .take(6)
            if (recentlyEarned.isNotEmpty()) {
                val totalEarned = allBadges.count { it.isEarned }
                val totalPossible = allBadges.size
                val badgeText = when {
                    totalEarned >= totalPossible -> "You've earned every badge. Absolute legend."
                    totalEarned > 10 -> "Your trophy case is looking impressive."
                    totalEarned > 5 -> "You've been earning your stripes."
                    else -> "Every badge tells a story about your listening."
                }
                optionalPool.add(
                    SpotlightStoryPage.BadgesEarned(
                        conversationalText = badgeText,
                        badges = recentlyEarned.map { badge ->
                            SpotlightStoryPage.BadgesEarned.BadgeEntry(
                                name = badge.name,
                                description = badge.description,
                                iconName = badge.iconName,
                                category = badge.category,
                                stars = badge.stars,
                                isNewThisPeriod = badge.earnedAt > 0
                            )
                        },
                        totalEarned = totalEarned,
                        totalPossible = totalPossible,
                        previewUrl = null
                    )
                )
            }

            val isNewLevel = userLevel.currentLevel > lastSeenLevel && lastSeenLevel >= 0
            if (isNewLevel) {
                val xpForCurrentLevel = userLevel.xpForCurrentLevel
                val xpEarnedThisPeriod = when (timeRange) {
                    TimeRange.THIS_WEEK -> (overview.totalPlayCount * 7L).coerceAtMost(userLevel.totalXp)
                    TimeRange.THIS_MONTH -> (overview.totalPlayCount * 8L).coerceAtMost(userLevel.totalXp)
                    else -> (overview.totalPlayCount * 9L).coerceAtMost(userLevel.totalXp)
                }
                val levelText = when {
                    userLevel.currentLevel >= 50 -> "You're operating at an elite level."
                    userLevel.currentLevel >= 25 -> "Quarter century of levels. You're dedicated."
                    userLevel.currentLevel >= 10 -> "Double digits. You're no casual listener."
                    userLevel.currentLevel >= 5  -> "Level ${userLevel.currentLevel}. The journey is just beginning."
                    else -> "Level ${userLevel.currentLevel} — keep listening, keep growing."
                }
                storyPages.add(
                    SpotlightStoryPage.LevelUp(
                        conversationalText = levelText,
                        currentLevel = userLevel.currentLevel,
                        currentTitle = userLevel.title,
                        totalXp = userLevel.totalXp,
                        xpForNextLevel = userLevel.xpForNextLevel,
                        levelProgress = userLevel.levelProgress,
                        xpEarnedThisPeriod = xpEarnedThisPeriod.coerceAtLeast(0),
                        previewUrl = null
                    )
                )
            }

            val isTitleNew = currentTitle != lastSeenTitle && lastSeenTitle.isNotEmpty() && currentTitle != "Newcomer"
            if (isTitleNew) {
                val prevLevel = (userLevel.currentLevel - 1).coerceAtLeast(0)
                val prevTitle = GamificationEngine.computeTitle(prevLevel, (uniqueArtists - 5).coerceAtLeast(0))
                val titleText = when (currentTitle) {
                    "Sound God" -> "You have transcended the ordinary. This is your legacy."
                    "Audiophile" -> "Only the most devoted reach this. You've arrived."
                    "Music Legend" -> "Legends aren't born. They're made, track by track."
                    "Music Connoisseur" -> "Your ears have developed a taste few can match."
                    "Dedicated Listener" -> "Music isn't just entertainment for you — it's a ritual."
                    "Music Enthusiast" -> "You're more than a fan. Music is part of how you live."
                    "Music Fan" -> "Your library speaks for itself."
                    else -> "You've earned your place in the listener's hall."
                }
                storyPages.add(
                    SpotlightStoryPage.TitleEarned(
                        conversationalText = titleText,
                        newTitle = currentTitle,
                        previousTitle = lastSeenTitle,
                        currentLevel = userLevel.currentLevel,
                        uniqueArtists = uniqueArtists,
                        previewUrl = null
                    )
                )
            }

            context.dataStore.edit { settings ->
                settings[LAST_STORY_LEVEL_KEY] = userLevel.currentLevel
                settings[LAST_STORY_TITLE_KEY] = currentTitle
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Gamification pages skipped: ${e.message}")
        }

        // 6. Conclusion
        storyPages.add(
            SpotlightStoryPage.Conclusion(
                conversationalText = "See you next time.", 
                totalMinutes = totalMinutes,
                personalityType = personalityResult.second.first,
                personalityKey = personalityResult.first,
                topArtists = topArtistsList.take(5).map { 
                    SpotlightStoryPage.Conclusion.ArtistEntry(it.artist, it.imageUrl) 
                },
                topSongs = topTracksList.take(5).map { 
                    SpotlightStoryPage.Conclusion.SongEntry(it.title, it.albumArtUrl) 
                },
                topGenres = topGenres.take(3).map { it.genre },
                timeRange = timeRange,
                previewUrl = null
            )
        )

        // Deterministic slide selection seeded by period key: consistent within same period,
        // updates when the period rolls over.
        val now = java.time.LocalDate.now()
        val periodKey: Long = when (timeRange) {
            TimeRange.THIS_WEEK  -> now.year * 100L + now.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)
            TimeRange.THIS_MONTH -> now.year * 100L + now.monthValue
            TimeRange.THIS_YEAR  -> now.year.toLong()
            else                 -> now.year.toLong() // ALL_TIME: stable per year
        }
        val seed = timeRange.ordinal * 10_000L + periodKey
        val rng = kotlin.random.Random(seed)
        val maxOptionalCount = rng.nextInt(3) + 2
        val selectedOptional = optionalPool.shuffled(rng).take(maxOptionalCount)
        // Merge selected slides into storyPages at their natural narrative positions
        mergeOptionalIntoStory(storyPages, selectedOptional)

        // SMART SONG ASSIGNMENT PASS
        // Assign audio previews:
        // - Contextual tracks for special slides
        // - Pairs of adjacent slides cycle through remaining tracks
        assignPreviewUrlsDynamically(storyPages, soundtrackList, trueTopTrack, topTracksList)
        
        storyPages  // Implicit return for coroutineScope
    }

    /**
     * Returns the id of the permanent anchor slide that this optional slide should be inserted
     * after, keeping the story's narrative arc intact regardless of which optional subset is shown.
     */
    private fun optionalSlideAnchorId(page: SpotlightStoryPage): String = when (page) {
        // Intro-zone: right after the listening-minutes opener
        is SpotlightStoryPage.ListeningStreak  -> "listening_minutes"
        is SpotlightStoryPage.BingeSession     -> "listening_minutes"
        is SpotlightStoryPage.ListeningClock   -> "listening_minutes"
        is SpotlightStoryPage.WeekdayVsWeekend -> "listening_minutes"
        // Artist-zone: after the TopArtist reveal (album is a natural follow-up)
        is SpotlightStoryPage.TopAlbum         -> "top_artist"
        // Discovery-zone: after TopGenres (thematically connected to breadth of taste)
        is SpotlightStoryPage.DiscoveryCount   -> "top_genres"
        is SpotlightStoryPage.TimeOfDayVibes   -> "top_genres"
        is SpotlightStoryPage.AudioMood        -> "top_genres"
        // Gamification-zone: after Personality (reward / celebration moment)
        is SpotlightStoryPage.BadgesEarned     -> "personality"
        else                                   -> "personality"
    }

    /**
     * Inserts [selectedOptional] slides in-place into [permanentPages] immediately after their
     * designated anchor slide (see [optionalSlideAnchorId]).  Multiple slides sharing the same
     * anchor are inserted together, preserving their relative order from [selectedOptional].
     */
    private fun mergeOptionalIntoStory(
        permanentPages: MutableList<SpotlightStoryPage>,
        selectedOptional: List<SpotlightStoryPage>
    ) {
        if (selectedOptional.isEmpty()) return
        // Group by anchor id, preserving selection order within each group
        val byAnchor = selectedOptional.groupBy { optionalSlideAnchorId(it) }
        // Walk forward; track a running offset as earlier insertions shift later indices
        var offset = 0
        val originalSize = permanentPages.size
        for (i in 0 until originalSize) {
            val page = permanentPages[i + offset]
            val toInsert = byAnchor[page.id] ?: continue
            permanentPages.addAll(i + offset + 1, toInsert)
            offset += toInsert.size
        }
    }

    // Returns Pair<stable English key, localized (name, description, tag)>
    private fun determineMusicalPersonality(
        energy: Float,
        valence: Float,
        danceability: Float,
        topGenres: List<String> = emptyList(),
        newArtistCount: Int = 0,
        varietyScore: Double = 0.0
    ): Pair<String, Triple<String, String, String>> {
        // 1. Genre-Based Personalities (Prioritize strong genre affinity)
        val mainGenre = topGenres.firstOrNull()?.lowercase() ?: ""
        
        if (mainGenre.isNotEmpty()) {
            val genrePersonality = when {
                mainGenre.contains("hip hop") || mainGenre.contains("rap") -> "Hip Hop Head"
                mainGenre.contains("pop") -> "Pop Icon"
                mainGenre.contains("rock") || mainGenre.contains("punk") -> "Rock Star"
                mainGenre.contains("metal") -> "Metalhead"
                mainGenre.contains("r&b") || mainGenre.contains("soul") -> "R&B Soul"
                mainGenre.contains("jazz") || mainGenre.contains("blues") -> "Jazz Cat"
                mainGenre.contains("electronic") || mainGenre.contains("house") || mainGenre.contains("techno") || mainGenre.contains("edm") -> "Electronic Voyager"
                mainGenre.contains("classical") || mainGenre.contains("orchestra") -> "Maestro"
                mainGenre.contains("indie") || mainGenre.contains("alternative") -> "Indie Spirit"
                else -> null
            }
            
            if (genrePersonality != null) {
                return genrePersonality to getDynamicPersonalityText(genrePersonality)
            }
        }

        // 2. Audio-Feature Personalities (If no strong genre match)
        val key = when {
            // High Energy + Happy
            energy >= 0.7f && valence >= 0.6f && danceability >= 0.6f -> "Party Starter"
            // High Energy + Dark/Intense
            energy >= 0.7f && valence < 0.4f -> "Intense Soul"
            // Low Energy + Happy/Calm
            energy < 0.4f && valence >= 0.6f -> "Peaceful Optimist"
            // Low Energy + Dark/Sad
            energy < 0.4f && valence < 0.4f -> "Deep Thinker"
            // High Danceability
            danceability >= 0.7f -> "Dance Floor Regular"
            // Balanced
            energy >= 0.4f && energy <= 0.7f && valence >= 0.4f && valence <= 0.7f -> "Balanced Enthusiast"
            // 3. Discovery/Explorer (Requires High Variety + Discovery)
            // Variety Score (Entropy): typically 0.0 to 3.0+. > 2.0 implies listening to many artists evenly.
            varietyScore > 2.0 && newArtistCount > 25 -> "The Explorer"
            // 4. Default Fallback
            else -> "The Melophile"
        }
        return key to getDynamicPersonalityText(key)
    }

    private fun getDynamicPersonalityText(type: String): Triple<String, String, String> {
        return when (type) {
            "Hip Hop Head" -> Triple(context.getString(R.string.personality_hip_hop_head_name), context.getString(R.string.pers_desc_hip_hop), context.getString(R.string.pers_tag_hip_hop))
            "Pop Icon" -> Triple(context.getString(R.string.personality_pop_icon_name), context.getString(R.string.pers_desc_pop), context.getString(R.string.pers_tag_pop))
            "Rock Star" -> Triple(context.getString(R.string.personality_rock_star_name), context.getString(R.string.pers_desc_rock), context.getString(R.string.pers_tag_rock))
            "Metalhead" -> Triple(context.getString(R.string.personality_metalhead_name), context.getString(R.string.pers_desc_metal), context.getString(R.string.pers_tag_metal))
            "R&B Soul" -> Triple(context.getString(R.string.personality_rb_soul_name), context.getString(R.string.pers_desc_rb), context.getString(R.string.pers_tag_rb))
            "Electronic Voyager" -> Triple(context.getString(R.string.personality_electronic_voyager_name), context.getString(R.string.pers_desc_electronic), context.getString(R.string.pers_tag_electronic))
            "Jazz Cat" -> Triple(context.getString(R.string.personality_jazz_cat_name), context.getString(R.string.pers_desc_jazz), context.getString(R.string.pers_tag_jazz))
            "Maestro" -> Triple(context.getString(R.string.personality_maestro_name), context.getString(R.string.pers_desc_maestro), context.getString(R.string.pers_tag_maestro))
            "Indie Spirit" -> Triple(context.getString(R.string.personality_indie_spirit_name), context.getString(R.string.pers_desc_indie), context.getString(R.string.pers_tag_indie))
            "Party Starter" -> Triple(context.getString(R.string.personality_party_starter_name), context.getString(R.string.pers_desc_party), context.getString(R.string.pers_tag_party))
            "Intense Soul" -> Triple(context.getString(R.string.personality_intense_soul_name), context.getString(R.string.pers_desc_intense), context.getString(R.string.pers_tag_intense))
            "Peaceful Optimist" -> Triple(context.getString(R.string.personality_peaceful_optimist_name), context.getString(R.string.pers_desc_peaceful), context.getString(R.string.pers_tag_peaceful))
            "Deep Thinker" -> Triple(context.getString(R.string.personality_deep_thinker_name), context.getString(R.string.pers_desc_deep_thinker), context.getString(R.string.pers_tag_deep_thinker))
            "Dance Floor Regular" -> Triple(context.getString(R.string.personality_dance_floor_regular_name), context.getString(R.string.pers_desc_dance), context.getString(R.string.pers_tag_dance))
            "Balanced Enthusiast" -> Triple(context.getString(R.string.personality_balanced_enthusiast_name), context.getString(R.string.pers_desc_balanced), context.getString(R.string.pers_tag_balanced))
            "The Explorer" -> Triple(context.getString(R.string.personality_the_explorer_name), context.getString(R.string.pers_desc_explorer), context.getString(R.string.pers_tag_explorer))
            "The Melophile" -> Triple(context.getString(R.string.personality_the_melophile_name), context.getString(R.string.pers_desc_melophile), context.getString(R.string.pers_tag_melophile))
            else -> Triple(type, context.getString(R.string.pers_desc_default), context.getString(R.string.pers_tag_default))
        }
    }

    private fun getCurrentSeason(): String {
        val month = java.time.LocalDate.now().monthValue
        
        if (isTropical()) {
            return getTropicalSeason(month)
        }

        val isSouthern = isSouthernHemisphere()
        
        return when (month) {
            in 3..5 -> if (isSouthern) "Autumn" else "Spring"
            in 6..8 -> if (isSouthern) "Winter" else "Summer"
            in 9..11 -> if (isSouthern) "Spring" else "Autumn"
            else -> if (isSouthern) "Summer" else "Winter"
        }
    }

    private fun getTropicalSeason(month: Int): String {
        return when (month) {
            in 3..5 -> "Early Year"
            in 6..8 -> "Mid-Year"
            in 9..11 -> "Late Year"
            else -> "Year-End"
        }
    }

    private fun isTropical(): Boolean {
        val country = java.util.Locale.getDefault().country.uppercase()
        val tropicalCodes = setOf(
            "ID", "SG", "MY", "TH", "VN", "PH", "BR", "CO", "VE", "EC", "PE", 
            "NG", "GH", "KE", "TZ", "LK", "BD", "MX", "JM", "DO", "PR"
        )
        return tropicalCodes.contains(country)
    }

    private fun isSouthernHemisphere(): Boolean {
        val country = java.util.Locale.getDefault().country.uppercase()
        val southernCodes = setOf(
            "AR", "AU", "CL", "NZ", "ZA", "UY" 
        )
        return southernCodes.contains(country)
    }

    /**
     * Smart song assignment: rebuilds pages with preview URLs.
     * - Special slides (TopTrackSetup, TopSongs) use their contextual song (#1 track)
     *   If #1 has no preview, finds another track from the same artist
     *   If that artist also has no previews, finds the highest-ranked artist that does
     * - TopArtist uses a song from that artist if available, otherwise uses reveal song
     * - TopAlbum uses a song from that album if available, otherwise uses reveal song
     * - Remaining slides pair 2-per-song from the pool, cycling through
     * - Every slide gets a song (fallback to pool[0] if pool is empty)
     * - The #1 track is NEVER used outside TopTrackSetup/TopSongs (protects the reveal)
     * - No song repeats unless the pool is exhausted
     */
    private fun assignPreviewUrlsDynamically(
        pages: MutableList<SpotlightStoryPage>,
        soundtrackPool: List<TopTrack>,
        trueTopTrack: TopTrack?,
        allTopTracks: List<TopTrack>
    ) {
        if (pages.isEmpty()) return

        // Pool for general slides: EXCLUDE the #1 track to protect the reveal
        val generalPool = soundtrackPool.filter { it.title != trueTopTrack?.title }

        // Fallback URL: first track from the general pool (never the #1 track)
        val fallbackUrl = generalPool.firstOrNull()?.previewUrl
            ?: soundtrackPool.firstOrNull()?.previewUrl

        // Build a map of artist -> tracks with previews (for contextual matching)
        val tracksByArtist = soundtrackPool.groupBy { it.artist.lowercase() }

        // Helper: find a preview URL for a given artist, skipping already-used URLs
        fun findPreviewForArtist(artistName: String, skipUrls: Set<String?>): String? {
            return tracksByArtist[artistName.lowercase()]
                ?.firstOrNull { it.previewUrl !in skipUrls }
                ?.previewUrl
        }

        // Compute the reveal song ONCE for TopTrackSetup/TopSongs:
        // 1. #1 track's own preview
        // 2. Another track from #1's artist
        // 3. Highest-ranked artist (from top tracks list) that has a preview
        // 4. Fallback to general pool
        val revealSongUrl = trueTopTrack?.previewUrl
            ?: findPreviewForArtist(trueTopTrack?.artist ?: "", emptySet())
            ?: run {
                allTopTracks
                    .filter { it.artist != trueTopTrack?.artist && it.previewUrl != null }
                    .firstOrNull()
                    ?.previewUrl
            }
            ?: fallbackUrl

        // Track used URLs to avoid repetition
        val usedUrls = mutableSetOf<String?>()

        // Identify special vs normal pages
        val specialIndices = mutableListOf<Int>()
        val normalIndices = mutableListOf<Int>()

        for (i in pages.indices) {
            when (pages[i]) {
                is SpotlightStoryPage.TopTrackSetup,
                is SpotlightStoryPage.TopSongs,
                is SpotlightStoryPage.TopArtist,
                is SpotlightStoryPage.TopAlbum -> specialIndices.add(i)
                else -> normalIndices.add(i)
            }
        }

        // STEP 1: Assign special pages FIRST (contextual songs)
        for (i in specialIndices) {
            val page = pages[i]
            val assignedUrl = when (page) {
                is SpotlightStoryPage.TopTrackSetup -> revealSongUrl
                is SpotlightStoryPage.TopSongs -> revealSongUrl
                is SpotlightStoryPage.TopArtist -> {
                    val artistSong = findPreviewForArtist(page.topArtistName, usedUrls)
                    
                    if (artistSong != null) {
                        artistSong // Found a song from this artist
                    } else {
                        // No songs from this artist — pick next available from pool
                        generalPool.firstOrNull { it.previewUrl !in usedUrls }?.previewUrl
                            ?: revealSongUrl // Absolute fallback if pool exhausted
                    }
                }
                is SpotlightStoryPage.TopAlbum -> {
                    val albumTracks = soundtrackPool.filter {
                        it.artist.lowercase() == page.artistName.lowercase() && it.title != trueTopTrack?.title
                    }
                    albumTracks.firstOrNull { it.previewUrl !in usedUrls }?.previewUrl
                        ?: revealSongUrl
                }
                else -> null
            }
            if (assignedUrl != null) {
                pages[i] = pages[i].copyWithPreview(assignedUrl)
                usedUrls.add(assignedUrl)
            }
        }

        // STEP 2: Assign normal pages from remaining pool (2 per song, cycling)
        val availablePool = generalPool.filter { it.previewUrl !in usedUrls }
        var poolIndex = 0

        for (i in normalIndices) {
            val songIndex = poolIndex / 2
            val url = if (availablePool.isNotEmpty()) {
                val wrappedIndex = songIndex % availablePool.size
                availablePool[wrappedIndex].previewUrl
            } else {
                // Pool exhausted — cycle through all general pool (repeats are OK now)
                val wrappedIndex = if (generalPool.isNotEmpty()) songIndex % generalPool.size else 0
                generalPool.getOrNull(wrappedIndex)?.previewUrl ?: fallbackUrl
            }
            pages[i] = pages[i].copyWithPreview(url)
            poolIndex++
        }
    }

    /** Recreates [SpotlightStoryPage] variants with the updated [previewUrl]. */
    private fun SpotlightStoryPage.copyWithPreview(newPreviewUrl: String?): SpotlightStoryPage {
        return when (this) {
            is SpotlightStoryPage.ListeningMinutes -> copy(previewUrl = newPreviewUrl)
            is SpotlightStoryPage.TopArtist -> copy(previewUrl = newPreviewUrl)
            is SpotlightStoryPage.TopTrackSetup -> copy(previewUrl = newPreviewUrl)
            is SpotlightStoryPage.TopSongs -> copy(previewUrl = newPreviewUrl)
            is SpotlightStoryPage.TopGenres -> copy(previewUrl = newPreviewUrl)
            is SpotlightStoryPage.Personality -> copy(previewUrl = newPreviewUrl)
            is SpotlightStoryPage.ListeningStreak -> copy(previewUrl = newPreviewUrl)
            is SpotlightStoryPage.ListeningClock -> copy(previewUrl = newPreviewUrl)
            is SpotlightStoryPage.TopAlbum -> copy(previewUrl = newPreviewUrl)
            is SpotlightStoryPage.DiscoveryCount -> copy(previewUrl = newPreviewUrl)
            is SpotlightStoryPage.AudioMood -> copy(previewUrl = newPreviewUrl)
            is SpotlightStoryPage.WeekdayVsWeekend -> copy(previewUrl = newPreviewUrl)
            is SpotlightStoryPage.BingeSession -> copy(previewUrl = newPreviewUrl)
            is SpotlightStoryPage.TimeOfDayVibes -> copy(previewUrl = newPreviewUrl)
            is SpotlightStoryPage.BadgesEarned -> copy(previewUrl = newPreviewUrl)
            is SpotlightStoryPage.LevelUp -> copy(previewUrl = newPreviewUrl)
            is SpotlightStoryPage.TitleEarned -> copy(previewUrl = newPreviewUrl)
            is SpotlightStoryPage.Conclusion -> copy(previewUrl = newPreviewUrl)
        }
    }
}
