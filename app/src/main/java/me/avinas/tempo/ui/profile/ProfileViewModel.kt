package me.avinas.tempo.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import me.avinas.tempo.data.local.entities.Badge
import me.avinas.tempo.data.local.entities.DailyChallenge
import me.avinas.tempo.data.local.entities.UserLevel
import me.avinas.tempo.data.profile.ProfileIdentityManager
import me.avinas.tempo.data.repository.ChallengeRepository
import me.avinas.tempo.data.repository.GamificationRepository
import me.avinas.tempo.data.repository.RefreshCoordinator
import me.avinas.tempo.data.stats.GamificationEngine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import me.avinas.tempo.data.repository.observeChallengeDays
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import me.avinas.tempo.data.analytics.AnalyticsTracker
import me.avinas.tempo.data.analytics.FeatureFirstUsed
import me.avinas.tempo.data.analytics.TempoFeature
import androidx.compose.runtime.Immutable
import java.time.ZoneId

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val gamificationRepository: GamificationRepository,
    private val challengeRepository: ChallengeRepository,
    private val refreshCoordinator: RefreshCoordinator,
    private val profileIdentityManager: ProfileIdentityManager,
    private val tracker: AnalyticsTracker
) : ViewModel() {
    
    
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    private val retryRequests = MutableStateFlow(0L)

    suspend fun observeWhileVisible(): Unit = coroutineScope {
        observeData()
        launch { refreshProfileData(refreshChallenges = false) }
        observeRefreshEvents()
    }

    suspend fun observeChallenges(context: Context) {
        observeChallengeDays(context).combine(retryRequests) { day, _ -> day }.collectLatest { day ->
            _uiState.update { state ->
                val date = day.date.toString()
                if (state.challengeDate != date) {
                    state.copy(challenges = emptyList(), challengeXpTotal = 0, challengesLoaded = false, challengeDate = date)
                } else state
            }
            coroutineScope {
                var refreshCompleted = false
                launch {
                    challengeRepository.observeChallenges(day.date).collectProfile(ProfileProblem.CHALLENGES) { challenges ->
                        _uiState.update { state ->
                            state.copy(
                                challenges = challenges,
                                challengeXpTotal = challenges.sumOf { it.xpReward },
                                challengesLoaded = refreshCompleted || challenges.isNotEmpty(),
                            )
                        }
                    }
                }
                try {
                    challengeRepository.generateDailyChallengesIfNeeded(day.date)
                    challengeRepository.refreshChallengeProgress(day)
                    refreshCompleted = true
                    setProblem(ProfileProblem.CHALLENGE_REFRESH, false)
                    _uiState.update { it.copy(challengesLoaded = true) }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    reportProblem(ProfileProblem.CHALLENGE_REFRESH, failure)
                }
            }
        }
    }

    private suspend fun <T> Flow<T>.collectProfile(problem: ProfileProblem, publish: suspend (T) -> Unit) {
        onEach { value ->
            publish(value)
            setProblem(problem, false)
        }.retryWhen { failure, _ ->
            if (failure !is Exception || failure is CancellationException) throw failure
            val request = retryRequests.value
            reportProblem(problem, failure)
            retryRequests.first { it != request }
            true
        }.collect {}
    }

    private fun setProblem(problem: ProfileProblem, failed: Boolean) {
        _uiState.update { it.copy(problems = if (failed) it.problems + problem else it.problems - problem) }
    }

    private fun reportProblem(problem: ProfileProblem, failure: Exception) {
        Log.w("ProfileViewModel", "Profile operation failed: $problem", failure)
        setProblem(problem, true)
    }

    private fun CoroutineScope.observeData() {
        launch {
            gamificationRepository.observeUserLevel().collectProfile(ProfileProblem.LEVEL) { level ->
                val currentLevel = level ?: UserLevel()
                val previousLevel = _uiState.value.userLevel.currentLevel
                val shouldCelebrate = level != null && previousLevel > 0 && level.currentLevel > previousLevel
                // Render the initial hero as soon as its level row arrives. Later
                // level changes and celebrations publish together with their title.
                if (!_uiState.value.levelLoaded) {
                    _uiState.update { it.copy(userLevel = currentLevel, levelLoaded = true) }
                }
                val uniqueArtists = if (currentLevel.currentLevel >= 5) {
                    gamificationRepository.getUniqueArtistCount()
                } else 0
                val calculatedTitle = GamificationEngine.computeTitle(currentLevel.currentLevel, uniqueArtists)
                val streakZone = gamificationRepository.getStreakAnchorZone()
                _uiState.update {
                    it.copy(
                        userLevel = currentLevel,
                        userTitle = calculatedTitle,
                        streakAnchorZone = streakZone,
                        showLevelUpCelebration = shouldCelebrate || it.showLevelUpCelebration,
                    )
                }
            }
        }
        
        launch {
            gamificationRepository.observeAllBadges().collectProfile(ProfileProblem.BADGES) { badges ->
                _uiState.update { state ->
                    val filteredBadges = if (state.selectedCategory == null) badges
                        else badges.filter { it.category == state.selectedCategory }
                    state.copy(
                        allBadges = badges,
                        badgesLoaded = true,
                        filteredBadges = filteredBadges,
                        earnedCount = badges.count { it.isEarned },
                        totalCount = badges.size,
                        categories = badges.map { it.category }.distinct().sorted(),
                        almostUnlockedBadges = ProfileUiState.computeAlmostUnlockedBadges(badges),
                        totalStars = ProfileUiState.computeTotalStars(badges),
                        maxPossibleStars = ProfileUiState.computeMaxPossibleStars(badges)
                    )
                }
            }
        }
        
        launch {
            gamificationRepository.observeUnacknowledgedBadges().collectProfile(ProfileProblem.CELEBRATIONS) { badges ->
                _uiState.update { it.copy(unacknowledgedBadges = badges) }
            }
        }
        
        launch {
            profileIdentityManager.profileIdentity.collectProfile(ProfileProblem.IDENTITY) { identity ->
                _uiState.update {
                    it.copy(
                        userName = identity.userName,
                        profileImagePath = identity.profileImagePath
                    )
                }
            }
        }
    }
    
    
    /**
     * Listen for "new track recorded" events from MusicTrackingService.
     * Uses debounce to avoid processing rapid-fire events (e.g., skips).
     */
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun CoroutineScope.observeRefreshEvents() {
        launch {
            refreshCoordinator.refreshEvents
                .debounce(1_000) // Wait 1s to batch rapid events
                .collect {
                    silentRefresh()
                }
        }
    }
    
    /** Refresh gamification without showing the full loading indicator. */
    private suspend fun silentRefresh() {
        try {
            challengeRepository.refreshChallengeProgress()
            gamificationRepository.fullRefresh()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Silent — don't show errors for background refreshes
        }
    }
    
    /** Pull-to-refresh also releases observers waiting to be retried. */
    suspend fun refresh() {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true) }
        try {
            retryRequests.update { it + 1 }
            refreshProfileData()
        } finally {
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private suspend fun refreshProfileData(refreshChallenges: Boolean = true) {
        try {
            if (refreshChallenges) challengeRepository.refreshChallengeProgress()
            gamificationRepository.fullRefresh()
            setProblem(ProfileProblem.REFRESH, false)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            reportProblem(ProfileProblem.REFRESH, failure)
        }
    }

    fun onCategorySelected(category: String?) {
        _uiState.update { state ->
            val filteredBadges = if (category == null) state.allBadges
                else state.allBadges.filter { it.category == category }
            state.copy(selectedCategory = category, filteredBadges = filteredBadges)
        }
    }
    
    fun dismissLevelUpCelebration() {
        _uiState.update { it.copy(showLevelUpCelebration = false) }
    }
    
    fun acknowledgeBadges(badgeIds: List<String>) {
        if (badgeIds.isEmpty() || _uiState.value.acknowledgingBadges) return
        _uiState.update { it.copy(acknowledgingBadges = true, problems = it.problems - ProfileProblem.ACKNOWLEDGEMENT) }
        viewModelScope.launch {
            try {
                gamificationRepository.markBadgesAsAcknowledged(badgeIds)
                _uiState.update { state ->
                    state.copy(unacknowledgedBadges = state.unacknowledgedBadges.filterNot { it.badgeId in badgeIds })
                }
                runCatching { tracker.track(FeatureFirstUsed(TempoFeature.GAMIFICATION_PROFILE)) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                reportProblem(ProfileProblem.ACKNOWLEDGEMENT, failure)
            } finally {
                _uiState.update { it.copy(acknowledgingBadges = false) }
            }
        }
    }
}

enum class ProfileProblem { LEVEL, BADGES, IDENTITY, CELEBRATIONS, CHALLENGES, CHALLENGE_REFRESH, REFRESH, ACKNOWLEDGEMENT }

@Immutable
data class ProfileUiState(
    val levelLoaded: Boolean = false,
    val badgesLoaded: Boolean = false,
    val challengesLoaded: Boolean = false,
    val challengeDate: String? = null,
    val isRefreshing: Boolean = false,
    val problems: Set<ProfileProblem> = emptySet(),
    val acknowledgingBadges: Boolean = false,
    val userLevel: UserLevel = UserLevel(),
    val userTitle: String = "Newcomer",
    val userName: String = "User",
    val profileImagePath: String? = null,
    val allBadges: List<Badge> = emptyList(),
    val challenges: List<DailyChallenge> = emptyList(),
    val selectedCategory: String? = null,
    val showLevelUpCelebration: Boolean = false,
    val unacknowledgedBadges: List<Badge> = emptyList(),
    val challengeXpTotal: Int = 0,
    val streakAnchorZone: ZoneId? = null,
    val filteredBadges: List<Badge> = emptyList(),
    val earnedCount: Int = 0,
    val totalCount: Int = 0,
    val categories: List<String> = emptyList(),
    val almostUnlockedBadges: List<Badge> = computeAlmostUnlockedBadges(allBadges),
    val totalStars: Int = computeTotalStars(allBadges),
    val maxPossibleStars: Int = computeMaxPossibleStars(allBadges)
) {
    val isLoading: Boolean get() = !levelLoaded && problems.isEmpty()

    companion object {
        fun computeAlmostUnlockedBadges(allBadges: List<Badge>): List<Badge> =
            allBadges.filter { !it.isMaxed && it.progressFraction >= 0.7f }

        fun computeTotalStars(allBadges: List<Badge>): Int =
            allBadges.filter { it.badgeId !in GamificationEngine.BEGINNER_BADGES }.sumOf { it.stars }

        fun computeMaxPossibleStars(allBadges: List<Badge>): Int =
            allBadges.filter { it.badgeId !in GamificationEngine.BEGINNER_BADGES }.size * 5
    }
}

internal fun selectFeaturedBadge(badges: List<Badge>): Badge? =
    badges.asSequence()
        .filter { !it.isMaxed && it.badgeId !in GamificationEngine.BEGINNER_BADGES && it.progressFraction >= 0.5f }
        .maxWithOrNull(compareBy<Badge> { !it.isEarned }.thenBy { it.progressFraction })
