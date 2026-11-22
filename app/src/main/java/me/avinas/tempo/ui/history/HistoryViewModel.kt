package me.avinas.tempo.ui.history

import me.avinas.tempo.data.local.entities.ManualContentRuleResolver
import me.avinas.tempo.data.local.DatabaseTransactionRunner
import android.util.Log
import me.avinas.tempo.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import me.avinas.tempo.data.local.dao.HistoryItem
import me.avinas.tempo.data.local.dao.LastFmImportMetadataDao
import me.avinas.tempo.data.local.dao.ScrobbleArchiveDao
import me.avinas.tempo.data.local.entities.ScrobbleArchive
import me.avinas.tempo.data.repository.ListeningRepository
import me.avinas.tempo.data.repository.RefreshCoordinator
import me.avinas.tempo.data.repository.StatsRepository
import me.avinas.tempo.data.repository.TrackRepository
import me.avinas.tempo.data.stats.TimeRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import me.avinas.tempo.data.analytics.AnalyticsTracker
import me.avinas.tempo.data.analytics.FeatureFirstUsed
import me.avinas.tempo.data.analytics.TempoFeature
import java.util.Locale

private val GroupDateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val statsRepository: StatsRepository,
    private val listeningRepository: ListeningRepository,
    private val trackRepository: TrackRepository,
    private val manualContentMarkDao: me.avinas.tempo.data.local.dao.ManualContentMarkDao,
    private val userPreferencesDao: me.avinas.tempo.data.local.dao.UserPreferencesDao,
    private val lastFmImportMetadataDao: LastFmImportMetadataDao,
    private val scrobbleArchiveDao: ScrobbleArchiveDao,
    private val refreshCoordinator: RefreshCoordinator,
    private val tracker: AnalyticsTracker,
    private val transactionRunner: DatabaseTransactionRunner,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var searchJob: kotlinx.coroutines.Job? = null
    private var historyLoadJob: kotlinx.coroutines.Job? = null
    private var archiveLoadJob: kotlinx.coroutines.Job? = null
    private var loadGeneration = 0L

    private fun invalidateHistoryLoads() {
        loadGeneration++
        historyLoadJob?.cancel()
        archiveLoadJob?.cancel()
        // Canceled jobs no longer get to clear their transient loading flags.
        _uiState.update {
            it.copy(isLoadingMore = false, isLoadingMoreLastFm = false, isRefreshing = false)
        }
    }

    // Cached import boundary (calculated once on init)
    private var importBoundaryTimestamp: Long? = null
    private var hasLastFmImport: Boolean = false
    private var viewModeInitialized = false
    private var viewModeInitJob: kotlinx.coroutines.Job? = null

    init {
        initializeViewMode()
        observeDataChanges()
    }
    
    /**
     * Initialize view mode based on import status.
     * If user has Last.fm import, use SEPARATED mode with two sections.
     * Otherwise, use UNIFIED mode showing all history together.
     */
    private fun initializeViewMode() {
        if (viewModeInitJob?.isActive == true) return
        viewModeInitJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Check for completed Last.fm import
                val lastImport = lastFmImportMetadataDao.getLatestCompleted()

                if (lastImport != null) {
                    hasLastFmImport = true
                    importBoundaryTimestamp = lastImport.importStartedAt

                    // Check archive size for UI hints
                    val archiveCount = scrobbleArchiveDao.getTotalCount()
                    val archivePlays = scrobbleArchiveDao.getTotalPlayCount()

                    _uiState.update { it.copy(
                        viewMode = HistoryViewMode.SEPARATED,
                        hasArchiveData = archiveCount > 0,
                        archiveTrackCount = archiveCount,
                        archiveTotalPlays = archivePlays,
                        recentBoundaryDate = importBoundaryTimestamp
                    ) }

                    Log.d(TAG, "Last.fm import found. Using SEPARATED mode. " +
                            "Archive: $archiveCount tracks, $archivePlays plays")
                } else {
                    // No import - use unified mode
                    hasLastFmImport = false
                    importBoundaryTimestamp = null
                    _uiState.update { it.copy(
                        viewMode = HistoryViewMode.UNIFIED,
                        hasArchiveData = false
                    ) }
                }

                viewModeInitialized = true
                loadHistory()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Log.e(TAG, "Error initializing history", failure)
                _uiState.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }

    private fun observeDataChanges() {
        viewModelScope.launch {
            merge(
                listeningRepository.recentEvents(1).map { Unit },
                refreshCoordinator.refreshEvents.map { Unit },
            )
                .debounce(300)
                .collect {
                    if (_uiState.value.page == 0 && !suppressObserverRefresh) {
                        loadHistory()
                    }
                }
        }
    }

    /** Pull-to-refresh handler. */
    suspend fun refresh() {
        if (!viewModeInitialized) {
            initializeViewMode()
            viewModeInitJob?.join()
            return
        }
        searchJob?.cancel()
        invalidateHistoryLoads()
        val generation = loadGeneration
        _uiState.update { it.copy(isRefreshing = true, searchPending = false) }
        val startTime = System.currentTimeMillis()
        try {
            _uiState.update { it.copy(isLoading = true) }
            // Fetch data inline so we actually wait for it before clearing isRefreshing
            fetchHistoryForRefresh(generation)
        } finally {
            // Keep spinner visible for at least 600ms to prevent flicker
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < 600) delay(600 - elapsed)
            if (generation == loadGeneration) {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    /**
     * Awaitable version of history loading used by pull-to-refresh.
     * Mirrors the logic in loadHistory() / loadLastFmHistory() but runs sequentially.
     */
    private suspend fun fetchHistoryForRefresh(generation: Long) {
        var loadingLastFm = false
        try {
            val currentState = _uiState.value
            val prefs = userPreferencesDao.getSync() ?: me.avinas.tempo.data.local.entities.UserPreferences()
            val filterPodcasts = prefs.filterPodcasts
            val filterAudiobooks = prefs.filterAudiobooks
            val searchQuery = currentState.searchQuery.takeIf { it.isNotBlank() }

            if (currentState.viewMode == HistoryViewMode.SEPARATED && hasLastFmImport) {
                val result = statsRepository.getHistoryExcludingLastFm(
                    searchQuery = searchQuery,
                    startTime = currentState.startDate,
                    endTime = currentState.endDate,
                    includeSkips = currentState.showSkips,
                    filterPodcasts = filterPodcasts,
                    filterAudiobooks = filterAudiobooks,
                    page = 0
                )
                val shouldShowCoachMark = checkShouldShowCoachMark(result.items)
                if (generation != loadGeneration) return
                _uiState.update { state ->
                    if (generation != loadGeneration) return@update state
                    state.copy(
                        rawItems = result.items,
                        groupedItems = groupHistoryItems(result.items),
                        isLoading = false,
                        isLoadingMore = false,
                        loadFailed = false,
                        pageFailed = false,
                        page = 0,
                        hasMore = result.hasMore,
                        showCoachMark = shouldShowCoachMark
                    )
                }

                loadingLastFm = true
                val lastFmResult = statsRepository.getHistoryLastFmOnly(
                    searchQuery = searchQuery,
                    startTime = currentState.startDate,
                    endTime = currentState.endDate,
                    includeSkips = currentState.showSkips,
                    filterPodcasts = filterPodcasts,
                    filterAudiobooks = filterAudiobooks,
                    page = 0
                )
                val archiveItems = loadArchiveItems(
                    query = searchQuery,
                    startTime = currentState.startDate,
                    endTime = currentState.endDate,
                    page = 0
                )
                if (generation != loadGeneration) return
                _uiState.update { state ->
                    if (generation != loadGeneration) return@update state
                    state.copy(
                        lastFmItems = lastFmResult.items,
                        lastFmGroupedItems = groupHistoryItems(lastFmResult.items),
                        archiveItems = archiveItems,
                        isLoadingMoreLastFm = false,
                        lastFmFailedPage = null,
                        lastFmPage = 0,
                        hasMoreLastFm = lastFmResult.hasMore
                    )
                }
            } else {
                val result = statsRepository.getHistory(
                    timeRange = TimeRange.ALL_TIME,
                    searchQuery = searchQuery,
                    startTime = currentState.startDate,
                    endTime = currentState.endDate,
                    includeSkips = currentState.showSkips,
                    filterPodcasts = filterPodcasts,
                    filterAudiobooks = filterAudiobooks,
                    page = 0
                )
                val shouldShowCoachMark = checkShouldShowCoachMark(result.items)
                if (generation != loadGeneration) return
                _uiState.update { state ->
                    if (generation != loadGeneration) return@update state
                    state.copy(
                        rawItems = result.items,
                        groupedItems = groupHistoryItems(result.items),
                        isLoading = false,
                        isLoadingMore = false,
                        loadFailed = false,
                        pageFailed = false,
                        page = 0,
                        hasMore = result.hasMore,
                        showCoachMark = shouldShowCoachMark
                    )
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Error during pull-to-refresh", e)
            if (generation == loadGeneration) {
                _uiState.update {
                    if (loadingLastFm) it.copy(isLoading = false, lastFmFailedPage = 0)
                    else it.copy(isLoading = false, isLoadingMore = false, loadFailed = true)
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        if (_uiState.value.searchQuery == query) return
        searchJob?.cancel()
        invalidateHistoryLoads()
        _uiState.update { it.copy(searchQuery = query, searchPending = true) }
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(500) // Debounce
            _uiState.update { it.copy(
                searchPending = false,
                page = 0,
                rawItems = emptyList(),
                groupedItems = emptyMap(),
                loadFailed = false,
                pageFailed = false,
                lastFmFailedPage = null,
                // Reset Last.fm section too
                lastFmItems = emptyList(),
                lastFmGroupedItems = emptyMap(),
                lastFmPage = 0,
                hasMoreLastFm = true,
                archiveItems = emptyList(),
                isLoading = true
            ) }
            loadHistory()
        }
    }

    fun onFilterChanged(startTime: Long?, endTime: Long?, showSkips: Boolean) {
        searchJob?.cancel()
        // Deliberate (the filter sheet was applied), unlike the per-keystroke search callback.
        tracker.track(FeatureFirstUsed(TempoFeature.HISTORY_FILTER))
        _uiState.update { it.copy(
            startDate = startTime,
            endDate = endTime,
            searchPending = false,
            showSkips = showSkips,
            page = 0,
            rawItems = emptyList(),
            groupedItems = emptyMap(),
            loadFailed = false,
            pageFailed = false,
            lastFmFailedPage = null,
            // Reset Last.fm section too
            lastFmItems = emptyList(),
            lastFmGroupedItems = emptyMap(),
            lastFmPage = 0,
            hasMoreLastFm = true,
            archiveItems = emptyList(),
            isLoading = true
        ) }
        loadHistory()
    }
    
    /**
     * Switch between RECENT and ALL_TIME view modes.
     */
    fun setViewMode(mode: HistoryViewMode) {
        tracker.track(FeatureFirstUsed(TempoFeature.HISTORY_VIEW_MODE))
        if (_uiState.value.viewMode == mode) return
        searchJob?.cancel()
        
        _uiState.update { it.copy(
            viewMode = mode,
            searchPending = false,
            page = 0,
            rawItems = emptyList(),
            groupedItems = emptyMap(),
            loadFailed = false,
            pageFailed = false,
            lastFmFailedPage = null,
            // Reset Last.fm section too
            lastFmItems = emptyList(),
            lastFmGroupedItems = emptyMap(),
            lastFmPage = 0,
            hasMoreLastFm = true,
            archiveItems = emptyList(),
            isLoading = true
        ) }
        loadHistory()
    }

    fun loadMore() {
        if (_uiState.value.let { it.isLoading || it.isLoadingMore || it.loadFailed || it.pageFailed || !it.hasMore }) return
        _uiState.update { it.copy(isLoadingMore = true) }
        loadHistory(isLoadMore = true)
    }
    
    fun retryNextPage() {
        if (_uiState.value.isLoading || !_uiState.value.pageFailed) return
        _uiState.update { it.copy(pageFailed = false) }
        loadMore()
    }

    fun retryLastFm() {
        if (_uiState.value.isLoading || _uiState.value.isRefreshing) return
        val page = _uiState.value.lastFmFailedPage ?: return
        _uiState.update { it.copy(lastFmFailedPage = null, isLoadingMoreLastFm = true) }
        loadLastFmHistory(isLoadMore = page > 0)
    }

    /**
     * Load more items for the Last.fm history section (separate pagination).
     */
    fun loadMoreLastFmHistory() {
        if (_uiState.value.isLoading || _uiState.value.isLoadingMoreLastFm || !_uiState.value.hasMoreLastFm || _uiState.value.lastFmFailedPage != null) return
        _uiState.update { it.copy(isLoadingMoreLastFm = true) }
        loadLastFmHistory(isLoadMore = true)
    }

    /**
     * Main history loading function.
     * In SEPARATED mode: Loads "Recent Activity" (non-Last.fm events)
     * In UNIFIED mode: Loads all events combined
     */
    private fun loadHistory(isLoadMore: Boolean = false) {
        if (!isLoadMore) invalidateHistoryLoads()
        val generation = loadGeneration
        historyLoadJob = viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val page = if (isLoadMore) currentState.page + 1 else 0
                
                // Get user preferences for content filtering
                val prefs = userPreferencesDao.getSync() ?: me.avinas.tempo.data.local.entities.UserPreferences()
                val filterPodcasts = prefs.filterPodcasts
                val filterAudiobooks = prefs.filterAudiobooks
                
                val searchQuery = currentState.searchQuery.takeIf { it.isNotBlank() }
                
                if (currentState.viewMode == HistoryViewMode.SEPARATED && hasLastFmImport) {
                    // SEPARATED MODE: Load only non-Last.fm events for "Recent Activity"
                    val result = statsRepository.getHistoryExcludingLastFm(
                        searchQuery = searchQuery,
                        startTime = currentState.startDate,
                        endTime = currentState.endDate,
                        includeSkips = currentState.showSkips,
                        filterPodcasts = filterPodcasts,
                        filterAudiobooks = filterAudiobooks,
                        page = page
                    )
                    
                    // Offset pagination shifts under live inserts, so page N+1 can
                    // overlap page N's tail. Dedup by id: duplicates become
                    // duplicate Lazy keys -> subcompose crash mid-fling.
                    val newItems = if (currentState.rawItems.isEmpty() || !isLoadMore)
                        result.items else (currentState.rawItems + result.items).distinctBy { it.id }

                    val shouldShowCoachMark = checkShouldShowCoachMark(newItems)
                    if (generation != loadGeneration) return@launch

                    _uiState.update { state ->
                        if (generation != loadGeneration) return@update state
                        val grouped = groupHistoryItems(newItems)
                        state.copy(
                            rawItems = newItems,
                            groupedItems = grouped,
                            isLoading = false,
                            isLoadingMore = false,
                            loadFailed = false,
                            pageFailed = false,
                            page = page,
                            hasMore = result.hasMore,
                            showCoachMark = shouldShowCoachMark
                        )
                    }

                    // Also load Last.fm history section if first load
                    if (!isLoadMore && generation == loadGeneration) {
                        loadLastFmHistory(isLoadMore = false)
                    }

                } else {
                    // UNIFIED MODE: Load all events combined (original behavior)
                    val result = statsRepository.getHistory(
                        timeRange = TimeRange.ALL_TIME,
                        searchQuery = searchQuery,
                        startTime = currentState.startDate,
                        endTime = currentState.endDate,
                        includeSkips = currentState.showSkips,
                        filterPodcasts = filterPodcasts,
                        filterAudiobooks = filterAudiobooks,
                        page = page
                    )

                    val newItemsUnified = if (currentState.rawItems.isEmpty() || !isLoadMore)
                        result.items else (currentState.rawItems + result.items).distinctBy { it.id }

                    val shouldShowCoachMark = checkShouldShowCoachMark(newItemsUnified)
                    if (generation != loadGeneration) return@launch

                    _uiState.update { state ->
                        if (generation != loadGeneration) return@update state
                        val grouped = groupHistoryItems(newItemsUnified)
                        state.copy(
                            rawItems = newItemsUnified,
                            groupedItems = grouped,
                            isLoading = false,
                            isLoadingMore = false,
                            loadFailed = false,
                            pageFailed = false,
                            page = page,
                            hasMore = result.hasMore,
                            showCoachMark = shouldShowCoachMark
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e(TAG, "Error loading history", e)
                if (generation != loadGeneration) return@launch
                _uiState.update {
                    if (generation != loadGeneration) return@update it 
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        loadFailed = !isLoadMore,
                        pageFailed = isLoadMore,
                    ) 
                }
            }
        }
    }
    
    /**
     * Load Last.fm history section (imported events + archive).
     * Only used in SEPARATED mode.
     */
    private fun loadLastFmHistory(isLoadMore: Boolean = false) {
        archiveLoadJob?.cancel()
        val generation = loadGeneration
        archiveLoadJob = viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val page = if (isLoadMore) currentState.lastFmPage + 1 else 0
                
                val prefs = userPreferencesDao.getSync() ?: me.avinas.tempo.data.local.entities.UserPreferences()
                val filterPodcasts = prefs.filterPodcasts
                val filterAudiobooks = prefs.filterAudiobooks
                val searchQuery = currentState.searchQuery.takeIf { it.isNotBlank() }
                
                // Load Last.fm imported events from listening_events table
                val result = statsRepository.getHistoryLastFmOnly(
                    searchQuery = searchQuery,
                    startTime = currentState.startDate,
                    endTime = currentState.endDate,
                    includeSkips = currentState.showSkips,
                    filterPodcasts = filterPodcasts,
                    filterAudiobooks = filterAudiobooks,
                    page = page
                )
                
                val newLastFmItems = if (currentState.lastFmItems.isEmpty() || !isLoadMore)
                    result.items else (currentState.lastFmItems + result.items).distinctBy { it.id }
                
                // Also load archive items (first page only, grouped by track)
                var archiveItems = currentState.archiveItems
                if (!isLoadMore) {
                    archiveItems = loadArchiveItems(
                        query = searchQuery,
                        startTime = currentState.startDate,
                        endTime = currentState.endDate,
                        page = 0
                    )
                }
                
                if (generation != loadGeneration) return@launch
                _uiState.update { state ->
                    if (generation != loadGeneration) return@update state
                    val groupedLastFm = groupHistoryItems(newLastFmItems)
                    state.copy(
                        lastFmItems = newLastFmItems,
                        lastFmGroupedItems = groupedLastFm,
                        archiveItems = archiveItems,
                        isLoadingMoreLastFm = false,
                        lastFmFailedPage = null,
                        lastFmPage = page,
                        hasMoreLastFm = result.hasMore
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e(TAG, "Error loading Last.fm history", e)
                if (generation == loadGeneration) {
                    _uiState.update { it.copy(
                        isLoadingMoreLastFm = false,
                        lastFmFailedPage = if (isLoadMore) it.lastFmPage + 1 else 0,
                    ) }
                }
            }
        }
    }
    
    /**
     * Load archive items with pagination support.
     */
    private suspend fun loadArchiveItems(
        query: String?,
        startTime: Long?,
        endTime: Long?,
        page: Int,
        pageSize: Int = 50
    ): List<ArchiveHistoryItem> {
        if (!hasLastFmImport) return emptyList()
        
        val archives = scrobbleArchiveDao.getArchivePaginated(
            searchQuery = query,
            startTime = startTime,
            endTime = endTime,
            limit = pageSize,
            offset = page * pageSize
        )
        
        return archives.map { archive ->
            ArchiveHistoryItem(
                archiveId = archive.id,
                timestamp = archive.lastScrobble,
                trackTitle = archive.trackTitle,
                artistName = archive.artistName,
                albumName = archive.albumName,
                albumArtUrl = archive.albumArtUrl,
                playCount = archive.playCount,
                isFromArchive = true
            )
        }
    }

    private fun groupHistoryItems(items: List<HistoryItem>): Map<String, List<HistoryItem>> {
        val today = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate()
        val yesterday = today.minusDays(1)
        val dateFormatter = GroupDateFormatter

        return items.groupBy { item ->
            val itemDate = Instant.ofEpochMilli(item.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            when {
                itemDate.isEqual(today) -> "Today"
                itemDate.isEqual(yesterday) -> "Yesterday"
                else -> itemDate.format(dateFormatter)
            }
        }
    }

    // Track pending deletes to prevent double-delete attempts
    private val pendingDeletes = mutableSetOf<Long>()
    
    // Suppress observer-triggered refresh during delete operations
    // Deletion updates the visible rows directly after the write succeeds.
    @Volatile
    private var suppressObserverRefresh = false

    /**
     * Delete a listening event from the database.
     * Also deletes the associated track if it has no other listening events.
     */
    fun deleteListeningEvent(id: Long) {
        if (!pendingDeletes.add(id)) return
        suppressObserverRefresh = true
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val item = (state.rawItems + state.lastFmItems).firstOrNull { it.id == id }
                if (item == null) {
                    _uiState.update { it.copy(mutationError = R.string.history_delete_failed) }
                    return@launch
                }

                val deleted = transactionRunner.run {
                    if (listeningRepository.deleteById(id) <= 0) {
                        false
                    } else {
                        // Keep orphan cleanup in the same Room transaction as the event delete.
                        // A concurrent tracker insert cannot slip between the emptiness check and
                        // track removal and accidentally lose a newly recorded play.
                        if (listeningRepository.getEventsForTrack(item.track_id).isEmpty()) {
                            trackRepository.deleteById(item.track_id)
                        }
                        true
                    }
                }
                if (!deleted) {
                    _uiState.update { it.copy(mutationError = R.string.history_delete_failed) }
                    return@launch
                }

                if (historyLoadJob?.isActive == true || archiveLoadJob?.isActive == true || _uiState.value.isRefreshing) {
                    invalidateHistoryLoads()
                    _uiState.update { it.copy(isLoading = false) }
                }
                _uiState.update { current ->
                    val recent = current.rawItems.filterNot { it.id == id }
                    val lastFm = current.lastFmItems.filterNot { it.id == id }
                    current.copy(
                        rawItems = recent,
                        groupedItems = groupHistoryItems(recent),
                        lastFmItems = lastFm,
                        lastFmGroupedItems = groupHistoryItems(lastFm),
                    )
                }
                statsRepository.invalidateCache()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Log.e(TAG, "Error deleting listening event", failure)
                _uiState.update { it.copy(mutationError = R.string.history_delete_failed) }
            } finally {
                pendingDeletes.remove(id)
                suppressObserverRefresh = pendingDeletes.isNotEmpty()
            }
        }
    }

    /** Save a title exception and correct matching history without deleting the track or rule. */
    fun markContent(trackId: Long, contentType: String, deleteFromHistory: Boolean) {
        tracker.track(FeatureFirstUsed(TempoFeature.MANUAL_CONTENT_MARK))
        viewModelScope.launch {
            _uiState.update { it.copy(isMarking = true) }
            try {
                val track = trackRepository.getById(trackId).first() ?: run {
                    _uiState.update { it.copy(isMarking = false, mutationError = R.string.history_correction_failed) }
                    return@launch
                }
                
                // 1. Save the block pattern for future content. "Unknown Artist" is a
                // transient metadata placeholder, so binding an exact TITLE_ARTIST rule to it
                // would stop matching as soon as the real artist/channel is discovered.
                val hasStableArtist = !me.avinas.tempo.utils.ArtistParser.isUnknownArtist(track.artist)
                val mark = me.avinas.tempo.data.local.entities.ManualContentMark(
                    targetTrackId = trackId,
                    patternType = if (hasStableArtist) "TITLE_ARTIST" else "TITLE",
                    originalTitle = track.title,
                    originalArtist = if (hasStableArtist) track.artist else "",
                    patternValue = track.title,
                    contentType = contentType,
                    markedAt = System.currentTimeMillis()
                )
                transactionRunner.run {
                    manualContentMarkDao.insertMark(mark)
                    Log.d(TAG, "Saved block pattern for '${track.title}' by '${track.artist}' as $contentType")

                    // Enable content filtering if not already enabled.
                    val prefs = userPreferencesDao.getSync() ?: me.avinas.tempo.data.local.entities.UserPreferences()
                    val updatedPrefs = when (contentType) {
                        "PODCAST" -> if (!prefs.filterPodcasts) prefs.copy(filterPodcasts = true) else prefs
                        "AUDIOBOOK" -> if (!prefs.filterAudiobooks) prefs.copy(filterAudiobooks = true) else prefs
                        else -> prefs
                    }
                    val finalPrefs = if (!prefs.hasSeenHistoryCoachMark) {
                        updatedPrefs.copy(hasSeenHistoryCoachMark = true)
                    } else {
                        updatedPrefs
                    }
                    userPreferencesDao.upsert(finalPrefs)

                    val marks = manualContentMarkDao.getAllSync()
                    trackRepository.all().first()
                        .filter { ManualContentRuleResolver.matches(mark, it.title, it.artist) }
                        .forEach { matchingTrack ->
                            val effectiveType = resolveEffectiveContentType(matchingTrack.title, matchingTrack.artist, marks)
                            if (effectiveType == "ALWAYS_MUSIC") {
                                trackRepository.update(matchingTrack.copy(contentType = "MUSIC"))
                            } else if (deleteFromHistory) {
                                listeningRepository.deleteByTrackId(matchingTrack.id)
                            } else {
                                trackRepository.update(matchingTrack.copy(contentType = effectiveType ?: contentType))
                            }
                        }
                }
                val feedbackMsg = if (contentType == "ALWAYS_MUSIC") {
                    "Always Music saved for \"${track.title}\""
                } else if (deleteFromHistory) {
                    "Blocked \"${track.title}\" - removed matching plays from history & stats"
                } else {
                    "Classification updated for \"${track.title}\""
                }
                _uiState.update { it.copy(showCoachMark = false, feedbackMessage = feedbackMsg, isMarking = false) }

                // Invalidate stats cache
                statsRepository.invalidateCache()
                
                // Refresh history
                loadHistory()

            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e(TAG, "Error marking content", e)
                _uiState.update { it.copy(mutationError = R.string.history_correction_failed, isMarking = false) }
            }
        }
    }

    /** Apply an artist correction while preserving more-specific Always Music exceptions. */
    fun markArtistContent(trackId: Long, contentType: String, deleteFromHistory: Boolean) {
        tracker.track(FeatureFirstUsed(TempoFeature.MANUAL_ARTIST_MARK))
        viewModelScope.launch {
            _uiState.update { it.copy(isMarking = true) }
            try {
                val track = trackRepository.getById(trackId).first() ?: run {
                    _uiState.update { it.copy(isMarking = false, mutationError = R.string.history_correction_failed) }
                    return@launch
                }
                
                val artistName = track.artist
                if (me.avinas.tempo.utils.ArtistParser.isUnknownArtist(artistName)) {
                    _uiState.update {
                        it.copy(
                            feedbackMessage = "Artist/channel is unknown — use the track-level correction instead",
                            isMarking = false
                        )
                    }
                    return@launch
                }

                // 1. Save the artist-level block pattern for future content
                val mark = me.avinas.tempo.data.local.entities.ManualContentMark(
                    targetTrackId = trackId,
                    patternType = "ARTIST",
                    originalTitle = "", // Empty - we match by artist only
                    originalArtist = artistName,
                    patternValue = artistName,
                    contentType = contentType,
                    markedAt = System.currentTimeMillis()
                )
                var affectedTracks = 0
                var protectedTracks = 0

                transactionRunner.run {
                    manualContentMarkDao.insertMark(mark)
                    Log.d(TAG, "Saved artist block pattern for '$artistName' as $contentType")

                    val prefs = userPreferencesDao.getSync() ?: me.avinas.tempo.data.local.entities.UserPreferences()
                    val updatedPrefs = when (contentType) {
                        "PODCAST" -> if (!prefs.filterPodcasts) prefs.copy(filterPodcasts = true) else prefs
                        "AUDIOBOOK" -> if (!prefs.filterAudiobooks) prefs.copy(filterAudiobooks = true) else prefs
                        else -> prefs
                    }
                    val finalPrefs = if (!prefs.hasSeenHistoryCoachMark) {
                        updatedPrefs.copy(hasSeenHistoryCoachMark = true)
                    } else {
                        updatedPrefs
                    }
                    userPreferencesDao.upsert(finalPrefs)

                    val artistTracks = trackRepository.all().first()
                        .filter { ManualContentRuleResolver.matches(mark, it.title, it.artist) }
                    val allMarks = manualContentMarkDao.getAllSync()
                    for (artistTrack in artistTracks) {
                        val effectiveType = resolveEffectiveContentType(
                            artistTrack.title,
                            artistTrack.artist,
                            allMarks
                        )

                        if (effectiveType == "ALWAYS_MUSIC") {
                            trackRepository.update(artistTrack.copy(contentType = "MUSIC"))
                            protectedTracks++
                            continue
                        }

                        if (deleteFromHistory) {
                            listeningRepository.deleteByTrackId(artistTrack.id)
                        } else {
                            trackRepository.update(
                                artistTrack.copy(contentType = effectiveType ?: contentType)
                            )
                        }
                        affectedTracks++
                    }
                }

                Log.d(
                    TAG,
                    "Applied artist correction '$contentType' to $affectedTracks track(s) for '$artistName'; " +
                        "preserved $protectedTracks more-specific ALWAYS_MUSIC exception(s)"
                )
                
                // Show success feedback
                val feedbackMsg = if (contentType == "ALWAYS_MUSIC") {
                    "Always Music saved for \"$artistName\"; more-specific exceptions kept"
                } else if (!deleteFromHistory) {
                    "Classification updated for \"$artistName\""
                } else if (protectedTracks > 0) {
                    "Blocked \"$artistName\" - kept $protectedTracks specific Always Music exception${if (protectedTracks == 1) "" else "s"}"
                } else {
                    "Blocked \"$artistName\" - removed all matching content from history & stats"
                }
                _uiState.update { it.copy(showCoachMark = false, feedbackMessage = feedbackMsg, isMarking = false) }

                // Invalidate stats cache
                statsRepository.invalidateCache()

                // Refresh history
                loadHistory()

            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e(TAG, "Error blocking artist", e)
                _uiState.update { it.copy(mutationError = R.string.history_correction_failed, isMarking = false) }
            }
        }
    }
    
    /** Resolve manual content rules exactly like the tracking service. */
    private fun resolveEffectiveContentType(
        title: String,
        artist: String,
        marks: List<me.avinas.tempo.data.local.entities.ManualContentMark>
    ): String? = ManualContentRuleResolver.resolve(marks, title, artist)?.contentType?.uppercase()

    private suspend fun checkShouldShowCoachMark(history: List<HistoryItem>): Boolean {
        if (history.isEmpty()) {
            Log.d(TAG, "CoachMark: Not showing - history is empty")
            return false
        }
        val prefs = userPreferencesDao.getSync()
        if (prefs == null) {
            Log.d(TAG, "CoachMark: Not showing - prefs is null")
            return false
        }
        val shouldShow = prefs.hasSeenHistoryCoachMark == false
        Log.d(TAG, "CoachMark: hasSeenHistoryCoachMark=${prefs.hasSeenHistoryCoachMark}, shouldShow=$shouldShow")
        return shouldShow
    }

    fun dismissCoachMark() {
        viewModelScope.launch {
            val prefs = userPreferencesDao.getSync() ?: return@launch
            userPreferencesDao.upsert(prefs.copy(hasSeenHistoryCoachMark = true))
            _uiState.update { it.copy(showCoachMark = false) }
        }
    }

    fun clearMutationError() {
        _uiState.update { it.copy(mutationError = null) }
    }

    fun clearFeedbackMessage() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    companion object {
        private const val TAG = "HistoryViewModel"
    }

}

