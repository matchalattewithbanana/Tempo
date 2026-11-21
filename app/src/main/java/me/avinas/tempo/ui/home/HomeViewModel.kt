package me.avinas.tempo.ui.home

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import me.avinas.tempo.BuildConfig
import me.avinas.tempo.data.local.dao.ListeningEventDao
import me.avinas.tempo.data.repository.StatsRepository
import me.avinas.tempo.data.stats.TimeRange
import me.avinas.tempo.ui.onboarding.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import me.avinas.tempo.ui.appearance.AppearanceHomeSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val statsRepository: StatsRepository,
    private val preferencesRepository: me.avinas.tempo.data.repository.PreferencesRepository,
    private val listeningEventDao: ListeningEventDao,
    private val dashboardLoader: HomeDashboardLoader,
    val gamificationRepository: me.avinas.tempo.data.repository.GamificationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _flagsState = MutableStateFlow(HomeFlags())
    val flagsState: StateFlow<HomeFlags> = _flagsState.asStateFlow()

    private var loadJob: Job? = null
    private var todayDetailsJob: Job? = null
    private var visibleScope: CoroutineScope? = null
    private val extendedInsightsKey = booleanPreferencesKey("home_extended_insights")
    private var extendedInsightsVisible = false
    private var userChangedExpansion = false
    private val _expandedInsights = MutableStateFlow(false)
    val expandedInsights: StateFlow<Boolean> = _expandedInsights.asStateFlow()

    // Coalesced reactive reload signal (see observeDataChanges).
    private val reactiveReload = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    // Rate-app prompt is evaluated at most once per app session.
    private var rateAppChecked = false

    private var expansionRestored = false
    private var preferredPeriod: TimeRange? = null
    private var hiddenSections: Set<AppearanceHomeSection> = emptySet()
    private var showSummaryChart = true

    suspend fun observeWhileVisible(
        startPeriod: TimeRange,
        hiddenSections: Set<AppearanceHomeSection>,
        showSummaryChart: Boolean,
    ) {
        this.hiddenSections = hiddenSections
        this.showSummaryChart = showSummaryChart
        if (preferredPeriod != startPeriod) {
            preferredPeriod = startPeriod
            onTimeRangeSelected(startPeriod)
        }
        if (!expansionRestored) {
            // A one-shot persisted preference restore. A user tap during startup
            // always wins over an older preference read.
            val remembered = try {
                context.dataStore.data.first()[extendedInsightsKey] ?: false
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                false
            }
            if (!userChangedExpansion && remembered) {
                extendedInsightsVisible = true
                _expandedInsights.value = true
            }
            expansionRestored = true
        }
        try {
            coroutineScope {
                visibleScope = this
                observeDataChanges()
                checkSpotlightReminder()
                if (_uiState.value.hasData) maybeShowShareNudge()
            }
        } finally {
            visibleScope = null
            loadJob?.cancel()
            todayDetailsJob?.cancel()
            todayDetailsJob = null
            _flagsState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun CoroutineScope.observeDataChanges() {
        // History and metadata changes share a debounce before loading the dashboard.
        launch {
            var initialLoad = true
            reactiveReload
                .debounce { if (initialLoad) 0L else 350L }
                .collect {
                    initialLoad = false
                    loadData()?.join()
                }
        }

        launch {
            statsRepository.observeListeningStatsChanges().collect { reactiveReload.tryEmit(Unit) }
        }

        // Also observe metadata updates (album art, artist images) from enrichment
        launch {
            statsRepository.observeMetadataUpdates()
                .collect { reactiveReload.tryEmit(Unit) }
        }

        // Listen for preference changes (e.g. Gamification toggle, spotlight viewed state)
        launch {
            var lastGamificationState: Boolean? = null
            preferencesRepository.preferences().collect { prefs ->
                val state = prefs?.isGamificationEnabled ?: true
                if (lastGamificationState != null && lastGamificationState != state) {
                    loadData() // Reload if toggle changed (deliberate user action, no debounce)
                }
                lastGamificationState = state

                // Compute spotlight story viewed state
                val storyTimeRange = me.avinas.tempo.ui.spotlight.SpotlightPeriodFormatter.getDirectStoryTimeRange()
                val currentKey = if (storyTimeRange != null) {
                    me.avinas.tempo.ui.spotlight.SpotlightPeriodFormatter.storyPeriodKey(storyTimeRange)
                } else null
                val viewed = currentKey != null && currentKey == prefs?.lastSpotlightStoryViewed
                _flagsState.update { it.copy(spotlightStoryViewed = viewed) }
            }
        }
    }

    /** Persists the reader's preferred Home density without touching the library. */
    fun setExtendedInsightsVisible(visible: Boolean) {
        if (extendedInsightsVisible == visible) return
        userChangedExpansion = true
        extendedInsightsVisible = visible
        _expandedInsights.value = visible
        if (!visible) {
            _uiState.update {
                it.copy(topTrack = null, topArtist = null, spotlightTopTrack = null, insights = emptyList())
            }
        }
        // Replace any in-flight expanded load so the core cards still finish loading.
        loadData()
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[extendedInsightsKey] = visible
            }
        }
    }

    /**
     * Extended Today rankings are only needed inside the overview sheet. Keep
     * initial Home loads to one track and artist instead of scanning for five.
     */
    fun loadTodayDetails() {
        val scope = visibleScope ?: return
        todayDetailsJob?.cancel()
        todayDetailsJob =
            scope.launch {
                val today = java.time.LocalDate.now()
                try {
                    val details = dashboardLoader.loadTodayDetails()
                    if (java.time.LocalDate.now() == today) {
                        _uiState.update { state ->
                            state.copy(
                                todayTopTracks = details.tracks,
                                todayTopArtists = details.artists,
                                todayTopTrack = details.tracks.firstOrNull(),
                                todayTopArtist = details.artists.firstOrNull(),
                            )
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // The lightweight preview remains available if extended rankings fail.
                }
            }
    }

    fun onTimeRangeSelected(timeRange: TimeRange) {
        if (_uiState.value.selectedTimeRange == timeRange) return
        _uiState.update {
            it.copy(
                selectedTimeRange = timeRange,
                hasData = false,
                loadFailed = false,
                totalListeningTimeMs = 0L,
                periodComparison = null,
                dailyListening = emptyList(),
                chartLabels = emptyList(),
                dailyAvgMs = 0L,
                activeDaysCount = 0,
                totalDaysCount = 0,
                peakDayMs = 0L,
                insights = emptyList(),
            )
        }
        _flagsState.update { it.copy(isLoading = true) }
        loadData()
    }

    suspend fun refresh() {
        _flagsState.update { it.copy(isRefreshing = true, isLoading = true) }
        try {
            loadData()?.join()
        } finally {
            _flagsState.update { it.copy(isRefreshing = false) }
        }
    }

    /** Requery date-scoped Home cards after the local calendar day or zone changes. */
    fun refreshCalendarData() {
        statsRepository.invalidateCache()
        loadData()
    }

    private fun loadData(): Job? {
        val scope = visibleScope ?: return null
        loadJob?.cancel()
        return scope.launch { fetchData() }.also { loadJob = it }
    }

    private suspend fun fetchData() {
        try {
            val request =
                HomeDashboardLoader.Request(
                    timeRange = _uiState.value.selectedTimeRange,
                    extended = extendedInsightsVisible,
                    hiddenSections = hiddenSections,
                    showSummaryChart = showSummaryChart,
                )
            val data =
                dashboardLoader.load(request) { totals ->
                    // Keep the primary metric responsive while secondary rankings and insights
                    // are still loading.
                    if (totals.playCount > 0) {
                        _uiState.update {
                            it.copy(
                                totalListeningTimeMs = totals.totalTimeMs,
                                hasData = true,
                                loadFailed = false,
                            )
                        }
                    }
                }

            _uiState.update {
                it.copy(
                    totalListeningTimeMs = data.totals.totalTimeMs,
                    periodComparison = data.periodComparison,
                    chartLabels = data.chartLabels,
                    dailyListening = data.dailyListening,
                    dailyAvgMs = data.dailyAverageMs,
                    activeDaysCount = data.activeDaysCount,
                    totalDaysCount = data.totalDaysCount,
                    peakDayMs = data.peakDayMs,
                    topTrack = data.topTrack,
                    topArtist = data.topArtist,
                    spotlightTopTrack = data.spotlightTopTrack,
                    audioFeatures = data.audioFeatures,
                    insights = data.insights,
                    userName = data.userName,
                    profileImagePath = data.profileImagePath,
                    hasData = data.totals.playCount > 0,
                    loadFailed = false,
                    isNewUser = data.isNewUser,
                    isGamificationEnabled = data.isGamificationEnabled,
                    todayOverview = data.todayOverview,
                    todayTopTrack = data.todayTopTrack,
                    todayTopArtist = data.todayTopArtist,
                    todayTopTracks = listOfNotNull(data.todayTopTrack),
                    todayTopArtists = listOfNotNull(data.todayTopArtist),
                    todayHourlyDistribution = data.todayHourlyDistribution,
                    todayPeriodComparison = data.todayPeriodComparison,
                )
            }
            _flagsState.update { it.copy(isLoading = false) }

            if (!_flagsState.value.showRateAppPopup && shouldShowRateApp()) {
                _flagsState.update { it.copy(showRateAppPopup = true) }
            }
            maybeShowShareNudge()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            _uiState.update { it.copy(loadFailed = true) }
            _flagsState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun shouldShowRateApp(): Boolean {
        if (!BuildConfig.PLAY_STORE_REVIEWS) return false
        // Evaluated at most once per app session: the engagement gates are two full-table
        // DAO scans (getRealListeningTimeMs/getRealPlayCount) and must not run on every
        // data reload. If engagement is below threshold now, the prompt simply waits for
        // the next launch rather than re-scanning on every track change.
        if (rateAppChecked) return false
        rateAppChecked = true
        // Check cheapest gates first to short-circuit before hitting the DB.
        val preferences = context.dataStore.data.first()

        // 1. Already handled — permanent suppression.
        val hasRated = preferences[booleanPreferencesKey("rate_app_rated")] ?: false
        if (hasRated) return false

        // 2. Dismissed too many times.
        val dismissCount = preferences[intPreferencesKey("rate_app_dismiss_count")] ?: 0
        if (dismissCount >= 2) return false

        // 3. Cooldown: don't re-prompt within 3 days of the last showing.
        val lastShown = preferences[longPreferencesKey("rate_app_last_shown")] ?: 0L
        val threeDaysMs = 3 * 24 * 60 * 60 * 1000L
        if (System.currentTimeMillis() - lastShown <= threeDaysMs) return false

        // 4. Install age: only prompt users who have had the app for at least 7 days.
        //    firstInstallTime survives app updates but not reinstalls.
        val sevenDaysMs = 7 * 24 * 60 * 60 * 1000L
        val firstInstallTime = try {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .firstInstallTime
        } catch (_: Exception) {
            System.currentTimeMillis() // fail-open: don't block the prompt if we can't read
        }
        if (System.currentTimeMillis() - firstInstallTime < sevenDaysMs) return false

        // 5. Engagement: > 2 hours REAL listening AND > 50 REAL plays
        //    (excludes imported Spotify history — source NOT LIKE '%import%')
        val realListeningTimeMs = listeningEventDao.getRealListeningTimeMs()
        val realPlayCount = listeningEventDao.getRealPlayCount()
        if (realListeningTimeMs < 2 * 60 * 60 * 1000 || realPlayCount < 50) return false

        context.dataStore.edit { prefs ->
            prefs[longPreferencesKey("rate_app_last_shown")] = System.currentTimeMillis()
        }
        return true
    }
    
    fun onRateAppFlowHandled() {
        // Dismiss immediately so the user sees instant feedback.
        _flagsState.update { it.copy(showRateAppPopup = false) }
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                // The Play Review API does not tell us whether the user submitted a rating.
                // We use this flag only to suppress re-prompting after handling the CTA.
                prefs[booleanPreferencesKey("rate_app_rated")] = true
            }
        }
    }
    
    fun onRateAppDismissed() {
        // Dismiss immediately so the user sees instant feedback.
        _flagsState.update { it.copy(showRateAppPopup = false) }
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                val current = prefs[intPreferencesKey("rate_app_dismiss_count")] ?: 0
                prefs[intPreferencesKey("rate_app_dismiss_count")] = current + 1
            }
        }
    }
    
    /**
     * Session guard: the share nudge is evaluated at most once per app session so a
     * time-range change or pull-to-refresh can never re-trigger it mid-use.
     */
    private var shareNudgeEvaluated = false

    /**
     * Opens the artist share preview on the home screen at most once per session,
     * after home data settles, when every gate below passes (cheapest first):
     *  1. Dismissed 3 times — the user isn't interested; stop permanently.
     *  2. Cooldown — at most once every 7 days.
     *  3. Shared anything in the last 14 days — they already know the feature.
     *  4. Fewer than 20 real plays — nothing worth sharing yet (also skips new users).
     *  5. Never stacks on top of the rate popup or spotlight reminder.
     */
    private fun maybeShowShareNudge() {
        val scope = visibleScope ?: return
        if (shareNudgeEvaluated) return
        shareNudgeEvaluated = true
        scope.launch {
            try {
                // Let the home screen settle before any overlay appears.
                delay(1500)

                val flags = _flagsState.value
                if (flags.showRateAppPopup || flags.showSpotlightReminder || flags.showShareNudge) return@launch

                val preferences = context.dataStore.data.first()

                // 1. Dismissed too many times — permanent suppression.
                val dismissCount = preferences[intPreferencesKey("share_nudge_dismiss_count")] ?: 0
                if (dismissCount >= 3) return@launch

                // 2. Cooldown: at most once every 7 days.
                val lastShown = preferences[longPreferencesKey("share_nudge_last_shown")] ?: 0L
                if (System.currentTimeMillis() - lastShown <= 7 * 24 * 60 * 60 * 1000L) return@launch

                // 3. Shared in the last 14 days — they already know the feature.
                val lastShared = preferences[longPreferencesKey("share_last_success")] ?: 0L
                if (lastShared > 0L && System.currentTimeMillis() - lastShared <= 14 * 24 * 60 * 60 * 1000L) return@launch

                // 4. Engagement: enough real plays for a card worth sharing.
                if (listeningEventDao.getRealPlayCount() < 20) return@launch

                // Freshest recap period wins for the card; weekly otherwise.
                val today = java.time.LocalDate.now()
                val timeRange =
                    if (today.dayOfMonth == today.lengthOfMonth()) TimeRange.THIS_MONTH
                    else TimeRange.THIS_WEEK
                val artists = statsRepository.getTopArtists(
                    timeRange,
                    sortBy = me.avinas.tempo.data.repository.SortBy.COMBINED_SCORE,
                    pageSize = 10,
                    withLeeway = false
                ).items
                if (artists.isEmpty()) return@launch
                val totals = statsRepository.getListeningTotals(timeRange, withLeeway = false)
                if (totals.playCount <= 0) return@launch

                // Mark shown immediately so re-entry can't double-fire.
                context.dataStore.edit { prefs ->
                    prefs[longPreferencesKey("share_nudge_last_shown")] = System.currentTimeMillis()
                }

                _flagsState.update {
                    it.copy(
                        showShareNudge = true,
                        shareNudgeTimeRange = timeRange,
                        shareNudgeArtists = artists,
                        shareNudgeTotals = totals
                    )
                }
            } catch (cancelled: CancellationException) {
                shareNudgeEvaluated = false
                throw cancelled
            } catch (e: Exception) {
                android.util.Log.d("HomeViewModel", "Share nudge skipped: ${e.message}")
            }
        }
    }

    /** Counts toward the 3-dismiss permanent suppression. */
    fun onShareNudgeDismissed() {
        _flagsState.update {
            it.copy(
                showShareNudge = false,
                shareNudgeTimeRange = null,
                shareNudgeArtists = emptyList(),
                shareNudgeTotals = null
            )
        }
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                val current = prefs[intPreferencesKey("share_nudge_dismiss_count")] ?: 0
                prefs[intPreferencesKey("share_nudge_dismiss_count")] = current + 1
            }
        }
    }

    /**
     * Resets the dismiss counter so the nudge can return after the normal cooldown,
     * and records the share.
     */
    fun onShareNudgeShared() {
        _flagsState.update {
            it.copy(
                showShareNudge = false,
                shareNudgeTimeRange = null,
                shareNudgeArtists = emptyList(),
                shareNudgeTotals = null
            )
        }
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[intPreferencesKey("share_nudge_dismiss_count")] = 0
                prefs[longPreferencesKey("share_last_success")] = System.currentTimeMillis()
            }
        }
    }

    /**
     * Check if we should show a Spotlight Story reminder.
     * Shows reminder on:
     * - Sunday (for THIS_WEEK story)
     * - Last day of the month (for THIS_MONTH story)
     * - December 1st (for THIS_YEAR story)
     */
    private fun CoroutineScope.checkSpotlightReminder() {
        launch {
            val today = java.time.LocalDate.now()
            val todayString = today.toString() // YYYY-MM-DD format
            
            android.util.Log.d("HomeViewModel", "Checking Spotlight reminder for date: $todayString")
            
            val preferences = preferencesRepository.preferences().first() ?: return@launch

            // Check for monthly reminder (last day of month)
            val isLastDayOfMonth = today.dayOfMonth == today.lengthOfMonth()
            if (isLastDayOfMonth && preferences.lastMonthlyReminderShown != todayString) {
                android.util.Log.d("HomeViewModel", "Is last day of month, checking data availability...")
                val totals = statsRepository.getListeningTotals(TimeRange.THIS_MONTH)
                android.util.Log.d("HomeViewModel", "Monthly data: totalPlayCount=${totals.playCount}")
                if (totals.playCount > 0) {
                    android.util.Log.i("HomeViewModel", "✅ Showing MONTHLY Spotlight reminder")
                    _flagsState.update {
                        it.copy(
                            showSpotlightReminder = true,
                            reminderTimeRange = TimeRange.THIS_MONTH,
                            reminderType = me.avinas.tempo.ui.components.SpotlightReminderType.MONTHLY
                        )
                    }
                    return@launch
                } else {
                    android.util.Log.d("HomeViewModel", "❌ Skipping monthly reminder: no data for THIS_MONTH")
                }
            } else if (isLastDayOfMonth) {
                android.util.Log.d("HomeViewModel", "Last day of month, but already shown: ${preferences.lastMonthlyReminderShown}")
            }
            
            // Check for yearly reminder (December 1st)
            val isDecemberFirst = today.monthValue == 12 && today.dayOfMonth == 1
            if (isDecemberFirst && preferences.lastYearlyReminderShown != todayString) {
                android.util.Log.d("HomeViewModel", "Is December 1st, checking data availability...")
                val totals = statsRepository.getListeningTotals(TimeRange.THIS_YEAR)
                android.util.Log.d("HomeViewModel", "Yearly data: totalPlayCount=${totals.playCount}")
                if (totals.playCount > 0) {
                    android.util.Log.i("HomeViewModel", "✅ Showing YEARLY Spotlight reminder")
                    _flagsState.update {
                        it.copy(
                            showSpotlightReminder = true,
                            reminderTimeRange = TimeRange.THIS_YEAR,
                            reminderType = me.avinas.tempo.ui.components.SpotlightReminderType.YEARLY
                        )
                    }
                    return@launch
                } else {
                    android.util.Log.d("HomeViewModel", "❌ Skipping yearly reminder: no data for THIS_YEAR")
                }
            } else if (isDecemberFirst) {
                android.util.Log.d("HomeViewModel", "December 1st, but already shown: ${preferences.lastYearlyReminderShown}")
            }
            
            // Check for weekly reminder (Sunday)
            val isSunday = today.dayOfWeek == java.time.DayOfWeek.SUNDAY
            if (isSunday && preferences.lastWeeklyReminderShown != todayString) {
                android.util.Log.d("HomeViewModel", "Is Sunday, checking data availability...")
                val totals = statsRepository.getListeningTotals(TimeRange.THIS_WEEK)
                android.util.Log.d("HomeViewModel", "Weekly data: totalPlayCount=${totals.playCount}")
                if (totals.playCount > 0) {
                    android.util.Log.i("HomeViewModel", "✅ Showing WEEKLY Spotlight reminder")
                    _flagsState.update {
                        it.copy(
                            showSpotlightReminder = true,
                            reminderTimeRange = TimeRange.THIS_WEEK,
                            reminderType = me.avinas.tempo.ui.components.SpotlightReminderType.WEEKLY
                        )
                    }
                } else {
                    android.util.Log.d("HomeViewModel", "❌ Skipping weekly reminder: no data for THIS_WEEK")
                }
            } else if (isSunday) {
                android.util.Log.d("HomeViewModel", "Sunday, but already shown: ${preferences.lastWeeklyReminderShown}")
            }
        }
    }
    
    /**
     * Dismiss the Spotlight reminder and save state to prevent showing again.
     */
    fun dismissSpotlightReminder() {
        viewModelScope.launch {
            val today = java.time.LocalDate.now().toString()
            val preferences = preferencesRepository.preferences().first() ?: return@launch
            
            val updatedPrefs = when (_flagsState.value.reminderType) {
                me.avinas.tempo.ui.components.SpotlightReminderType.WEEKLY -> 
                    preferences.copy(lastWeeklyReminderShown = today)
                me.avinas.tempo.ui.components.SpotlightReminderType.MONTHLY -> 
                    preferences.copy(lastMonthlyReminderShown = today)
                me.avinas.tempo.ui.components.SpotlightReminderType.YEARLY -> 
                    preferences.copy(lastYearlyReminderShown = today)
                null -> preferences
            }
            
            preferencesRepository.upsert(updatedPrefs)
            
            _flagsState.update {
                it.copy(
                    showSpotlightReminder = false,
                    reminderTimeRange = null,
                    reminderType = null
                )
            }
        }
    }

    /**
     * Persists the current story period key so the ring reads as viewed until a new
     * story period arrives.
     */
    fun onSpotlightViewed() {
        viewModelScope.launch {
            val storyTimeRange = me.avinas.tempo.ui.spotlight.SpotlightPeriodFormatter.getDirectStoryTimeRange()
                ?: return@launch
            val key = me.avinas.tempo.ui.spotlight.SpotlightPeriodFormatter.storyPeriodKey(storyTimeRange)
            preferencesRepository.updateLastSpotlightStoryViewed(key)
        }
    }
}

/** Compute from unrounded milliseconds, including sub-minute listening. */
internal fun preciseDailyAverageMs(totalMillis: Long, activeDays: Int): Long =
    if (activeDays > 0) totalMillis.coerceAtLeast(0L) / activeDays else 0L

@Immutable
data class HomeUiState(
    val loadFailed: Boolean = false,
    val selectedTimeRange: TimeRange = TimeRange.THIS_WEEK,
    val hasData: Boolean = false,
    val isNewUser: Boolean = false,
    val isGamificationEnabled: Boolean = true,
    
    // Data fields
    val totalListeningTimeMs: Long = 0,
    val periodComparison: me.avinas.tempo.data.stats.PeriodComparison? = null,
    val dailyListening: List<me.avinas.tempo.data.stats.DailyListening> = emptyList(),
    val chartLabels: List<String> = emptyList(),
    val dailyAvgMs: Long = 0L,
    val activeDaysCount: Int = 0,
    val totalDaysCount: Int = 0,
    val peakDayMs: Long = 0L,
    val topTrack: me.avinas.tempo.data.stats.TopTrack? = null,
    val topArtist: me.avinas.tempo.data.stats.TopArtist? = null,
    val audioFeatures: me.avinas.tempo.data.stats.AudioFeaturesStats? = null,
    val insights: List<me.avinas.tempo.data.stats.InsightCardData> = emptyList(),
    val userName: String? = null,
    val profileImagePath: String? = null,
    
    // Spotlight story top track (correct period, with leeway)
    val spotlightTopTrack: me.avinas.tempo.data.stats.TopTrack? = null,
    
    // Today's Listen Widget
    val todayOverview: me.avinas.tempo.data.stats.ListeningOverview? = null,
    val todayTopTrack: me.avinas.tempo.data.stats.TopTrack? = null,
    val todayTopArtist: me.avinas.tempo.data.stats.TopArtist? = null,
    val todayTopTracks: List<me.avinas.tempo.data.stats.TopTrack> = emptyList(),
    val todayTopArtists: List<me.avinas.tempo.data.stats.TopArtist> = emptyList(),
    val todayHourlyDistribution: List<me.avinas.tempo.data.stats.HourlyDistribution> = emptyList(),
    val todayPeriodComparison: me.avinas.tempo.data.stats.PeriodComparison? = null
)

/**
 * Transient UI flags that toggle independently of the stats payload. Split out of
 * [HomeUiState] so that a pull-to-refresh spinner toggle or popup dismissal no longer
 * recomposes the whole data feed (which shares a single StateFlow).
 */
@Immutable
data class HomeFlags(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val showRateAppPopup: Boolean = false,
    val showShareNudge: Boolean = false,
    val shareNudgeTimeRange: TimeRange? = null,
    val shareNudgeArtists: List<me.avinas.tempo.data.stats.TopArtist> = emptyList(),
    val shareNudgeTotals: me.avinas.tempo.data.stats.CombinedBasicStats? = null,
    val showSpotlightReminder: Boolean = false,
    val reminderTimeRange: TimeRange? = null,
    val reminderType: me.avinas.tempo.ui.components.SpotlightReminderType? = null,
    val spotlightStoryViewed: Boolean = false
)
