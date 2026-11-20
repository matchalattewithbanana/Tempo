package me.avinas.tempo.ui.stats

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.merge
import me.avinas.tempo.ui.appearance.*
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.avinas.tempo.data.repository.SortBy
import me.avinas.tempo.data.repository.StatsRepository
import me.avinas.tempo.data.stats.CombinedBasicStats
import me.avinas.tempo.data.stats.StatItem
import me.avinas.tempo.data.stats.TimeRange
import me.avinas.tempo.data.stats.TopAlbum
import me.avinas.tempo.data.stats.TopArtist
import me.avinas.tempo.data.stats.TopTrack
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class StatsViewModel
    @Inject
    constructor(
        private val statsRepository: StatsRepository,
        private val appearancePreferences: AppearancePreferencesRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(StatsUiState())
        val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

        private var loadJob: Job? = null
        private var visibleScope: CoroutineScope? = null
        private var loadMoreJob: Job? = null

        // A filter/search switch must make in-flight query results obsolete.
        // Cancellation alone is insufficient for repositories that complete
        // work just as a cancellation is delivered.
        private var resultGeneration = 0L
        private var userSelectedFilters = false
        private var widgetPeriodApplied = false

        private fun invalidateInFlightQueries() {
            resultGeneration++
            loadJob?.cancel()
            loadMoreJob?.cancel()
            // A canceled pagination/refresh job won't reach its cleanup path.
            // Reset transient indicators whenever a newer request supersedes it.
            _uiState.update { it.copy(isLoadingMore = false, isRefreshing = false) }
        }

        private var defaultsLoaded = false

        suspend fun observeWhileVisible() {
            if (!defaultsLoaded && !userSelectedFilters) {
                val defaults = appearancePreferences.settings.first()
                if (!userSelectedFilters) _uiState.update { state -> state.copy(
                    selectedTab = when (defaults.statsTab) {
                        AppearanceStatsTab.SONGS -> StatsTab.TOP_SONGS
                        AppearanceStatsTab.ARTISTS -> StatsTab.TOP_ARTISTS
                        AppearanceStatsTab.ALBUMS -> StatsTab.TOP_ALBUMS
                    },
                    selectedSortBy = when (defaults.statsSort) {
                        AppearanceStatsSort.COMBINED -> SortBy.COMBINED_SCORE
                        AppearanceStatsSort.PLAYS -> SortBy.PLAY_COUNT
                        AppearanceStatsSort.TIME -> SortBy.TOTAL_TIME
                    },
                    selectedTimeRange = if (widgetPeriodApplied) state.selectedTimeRange else when (defaults.statsRange) {
                        AppearanceStatsRange.TODAY -> TimeRange.TODAY
                        AppearanceStatsRange.WEEK -> TimeRange.THIS_WEEK
                        AppearanceStatsRange.MONTH -> TimeRange.THIS_MONTH
                        AppearanceStatsRange.YEAR -> TimeRange.THIS_YEAR
                        AppearanceStatsRange.ALL_TIME -> TimeRange.ALL_TIME
                    },
                ) }
                defaultsLoaded = true
            }
            try {
                coroutineScope {
                    visibleScope = this
                    var initialLoad = true
                    merge(statsRepository.observeListeningStatsChanges(), statsRepository.observeMetadataUpdates())
                        .debounce { if (initialLoad) 0L else 300L }
                        .collect {
                            initialLoad = false
                            searchJob?.cancel()
                            invalidateInFlightQueries()
                            loadJob = loadData()
                        }
                }
            } finally {
                visibleScope = null
                searchJob?.cancel()
                invalidateInFlightQueries()
            }
        }

        fun onTabSelected(tab: StatsTab) {
            userSelectedFilters = true
            if (_uiState.value.selectedTab == tab && _uiState.value.items.isNotEmpty()) return
            searchJob?.cancel()
            invalidateInFlightQueries()
            _uiState.update { it.copy(selectedTab = tab, isLoading = true, items = emptyList(), page = 0, hasMore = true) }
            loadJob = loadData()
        }

        fun onTimeRangeSelected(timeRange: TimeRange) {
            userSelectedFilters = true
            if (_uiState.value.selectedTimeRange == timeRange && _uiState.value.items.isNotEmpty()) return
            searchJob?.cancel()
            invalidateInFlightQueries()
            _uiState.update { it.copy(
                selectedTimeRange = timeRange, periodTotals = null,
                isLoading = true, items = emptyList(), page = 0, hasMore = true,
            ) }
            loadJob = loadData()
        }

        fun openWidgetPeriod(timeRange: TimeRange) {
            widgetPeriodApplied = true
            searchJob?.cancel()
            invalidateInFlightQueries()
            _uiState.update { state ->
                state.copy(
                    selectedTimeRange = timeRange,
                    searchQuery = "",
                    periodTotals = state.periodTotals.takeIf { state.selectedTimeRange == timeRange },
                    isLoading = true,
                    items = emptyList(),
                    page = 0,
                    hasMore = true,
                )
            }
            loadJob = loadData()
        }

        fun onSortBySelected(sortBy: SortBy) {
            userSelectedFilters = true
            if (_uiState.value.selectedSortBy == sortBy && _uiState.value.items.isNotEmpty()) return
            searchJob?.cancel()
            invalidateInFlightQueries()
            _uiState.update { it.copy(selectedSortBy = sortBy, isLoading = true, items = emptyList(), page = 0, hasMore = true) }
            loadJob = loadData()
        }

        // Search state: debounced query that live-filters the current ranking.
        // While active, results come from searchTop* (global ranks, capped) and
        // pagination is disabled — search results are a bounded, complete set.
        private var searchJob: Job? = null

        fun onSearchQueryChanged(query: String) {
            if (_uiState.value.searchQuery == query) return
            searchJob?.cancel()
            invalidateInFlightQueries()
            _uiState.update { it.copy(searchQuery = query) }
            searchJob =
                visibleScope?.launch {
                    if (query.isNotBlank()) delay(300) // Debounce typing, but clear immediately.
                    _uiState.update { it.copy(isLoading = true, items = emptyList(), page = 0, hasMore = false) }
                    loadJob = loadData()
                }
        }

        fun loadMore() {
            if (visibleScope == null) return
            val state = _uiState.value
            if (state.isLoading || state.isLoadingMore || state.isRefreshing || state.error != null || !state.hasMore) return
            if (state.searchQuery.isNotBlank()) return // Search results are not paginated
            _uiState.update { it.copy(isLoadingMore = true) }
            loadMoreJob = loadData(isLoadMore = true)
        }

        /** Retries loading stats data after a failure. */
        fun retry() {
            searchJob?.cancel()
            invalidateInFlightQueries()
            _uiState.update { it.copy(error = null, isLoading = true, items = emptyList(), page = 0, hasMore = true) }
            loadJob = loadData()
        }

        private fun loadData(isLoadMore: Boolean = false): Job? {
            val generation = resultGeneration
            return visibleScope?.launch {
                try {
                    val current = _uiState.value
                    val page = if (isLoadMore) current.page + 1 else 0
                    val timeRange = current.selectedTimeRange
                    val sortBy = current.selectedSortBy
                    val query = current.searchQuery

                    // The summary is independent of ranking/search pagination. Start its
                    // query immediately so a slow rankings page cannot delay the period total.
                    if (page == 0) launch { updatePeriodTotals(timeRange, generation) }

                    val items: List<StatItem>
                    val hasMore: Boolean
                    if (query.isNotBlank()) {
                        items = when (current.selectedTab) {
                            StatsTab.TOP_SONGS -> statsRepository.searchTopTracks(timeRange, sortBy, query, withLeeway = false)
                            StatsTab.TOP_ARTISTS -> statsRepository.searchTopArtists(timeRange, sortBy, query, withLeeway = false)
                            StatsTab.TOP_ALBUMS -> statsRepository.searchTopAlbums(timeRange, sortBy, query, withLeeway = false)
                        }
                        hasMore = false
                    } else {
                        val result = when (current.selectedTab) {
                            StatsTab.TOP_SONGS -> statsRepository.getTopTracks(timeRange, sortBy, page, withLeeway = false)
                            StatsTab.TOP_ARTISTS -> statsRepository.getTopArtists(timeRange, sortBy, page, withLeeway = false)
                            StatsTab.TOP_ALBUMS -> statsRepository.getTopAlbums(timeRange, sortBy, page, withLeeway = false)
                        }
                        items = result.items
                        hasMore = result.hasMore && items.isNotEmpty()
                    }

                    if (generation != resultGeneration) return@launch
                    _uiState.update { state ->
                        val newItems = if (isLoadMore) {
                            (state.items + items).distinctBy { item ->
                                when (item) {
                                    is TopTrack -> "track_${item.trackId}"
                                    is TopArtist -> "artist_${item.artistId ?: item.artist}"
                                    is TopAlbum -> "album_${item.album}_${item.artist}"
                                }
                            }
                        } else items
                        state.copy(
                            items = newItems,
                            isLoading = false,
                            isLoadingMore = false,
                            isRefreshing = false,
                            page = page,
                            hasMore = hasMore,
                            error = null,
                        )
                    }

                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    if (generation == resultGeneration) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isLoadingMore = false,
                                isRefreshing = false,
                                error = failure.localizedMessage ?: failure.javaClass.simpleName,
                            )
                        }
                    }
                }
            }
        }

        private suspend fun updatePeriodTotals(timeRange: TimeRange, generation: Long) {
            try {
                val totals = statsRepository.getListeningTotals(timeRange, withLeeway = false)
                if (generation == resultGeneration) _uiState.update { it.copy(periodTotals = totals) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                android.util.Log.w("StatsViewModel", "Could not load period totals", failure)
            }
        }

        /** Refresh the rankings while leaving the previous results visible. */
        suspend fun refresh() {
            if (visibleScope == null || _uiState.value.isRefreshing) return
            searchJob?.cancel()
            invalidateInFlightQueries()
            statsRepository.invalidateCache()
            _uiState.update { it.copy(isRefreshing = true) }
            loadJob = loadData()
            loadJob?.join()
        }
    }

enum class StatsTab {
    TOP_SONGS,
    TOP_ARTISTS,
    TOP_ALBUMS,
}

@Immutable
data class StatsUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val selectedTab: StatsTab = StatsTab.TOP_SONGS,
    val selectedTimeRange: TimeRange = TimeRange.THIS_WEEK,
    val selectedSortBy: SortBy = SortBy.COMBINED_SCORE, // Default to combined score
    val items: List<StatItem> = emptyList(), // Can be TopTrack, TopArtist, or TopAlbum
    val searchQuery: String = "",
    val page: Int = 0,
    val hasMore: Boolean = true,
    val periodTotals: CombinedBasicStats? = null,
)

@Immutable
data class AnalyticsUiData(
    val overview: me.avinas.tempo.data.stats.ListeningOverview,
    val hourlyDistribution: List<me.avinas.tempo.data.stats.HourlyDistribution>,
    val insightCards: List<me.avinas.tempo.data.stats.InsightCardData>,
)
