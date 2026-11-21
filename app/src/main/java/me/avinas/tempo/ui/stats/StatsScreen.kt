@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.avinas.tempo.ui.stats

import me.avinas.tempo.ui.effects.tempoAmbientBackdrop
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle

import me.avinas.tempo.ui.theme.TextOnAccent
import me.avinas.tempo.ui.theme.GoldDark
import me.avinas.tempo.ui.theme.LocalTempoAppearance
import me.avinas.tempo.ui.theme.LocalTempoPalette
import me.avinas.tempo.ui.theme.rememberReducedMotion
import me.avinas.tempo.ui.components.TempoDropdownMenu
import me.avinas.tempo.ui.components.TempoDropdownMenuItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material3.*
import me.avinas.tempo.ui.components.TempoCircularLoadingIndicator
import me.avinas.tempo.ui.components.TempoLinearLoadingIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import me.avinas.tempo.ui.components.CachedAsyncImage
import me.avinas.tempo.data.repository.SortBy
import me.avinas.tempo.data.stats.CombinedBasicStats
import me.avinas.tempo.data.stats.TimeRange
import me.avinas.tempo.data.stats.TopAlbum
import me.avinas.tempo.data.stats.TopArtist
import me.avinas.tempo.data.stats.TopTrack
import me.avinas.tempo.data.stats.StatItem
import me.avinas.tempo.ui.components.TimePeriodSelector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import me.avinas.tempo.R
import me.avinas.tempo.ui.appearance.AppearancePaletteResolver
import me.avinas.tempo.ui.components.LoadErrorNotice

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel(),
    onNavigateToTrack: (Long) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToAlbum: suspend (title: String, artist: String) -> Boolean,
    onNavigateToSupportedApps: () -> Unit,
    widgetPeriod: TimeRange? = null,
    onWidgetPeriodHandled: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val appearance = LocalTempoAppearance.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, viewModel) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.observeWhileVisible()
        }
    }
    // New tab/range/sort/search = new list: recreate state so scroll resets to
    // 0 synchronously before the next measure. The old manual scroll-to-item
    // clamp fought flings mid-measure (subcompose crash trigger) and is gone.
    val listState = remember(uiState.selectedTab, uiState.selectedTimeRange, uiState.selectedSortBy, uiState.searchQuery) { LazyListState() }
    val scope = rememberCoroutineScope()
    var showShareDialog by remember { mutableStateOf(false) }
    var showAlbumPoster by rememberSaveable { mutableStateOf(false) }
    var openingAlbum by remember { mutableStateOf(false) }
    val albumSnackbar = remember { SnackbarHostState() }
    val albumNotFound = stringResource(R.string.album_not_found)
    val albumLookupFailed = stringResource(R.string.stats_error_title)

    // Search drawer state: closed = search icon only; open = field slides over the top.
    var searchOpen by rememberSaveable { mutableStateOf(uiState.searchQuery.isNotBlank()) }
    val searchFocusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val reducedMotion = rememberReducedMotion()
    // Wait only for the search field to enter composition. The previous fixed
    // 280ms delay made every search feel unresponsive and did not scale with
    // reduced motion, UI load or keyboard animation duration.
    LaunchedEffect(searchOpen) {
        if (searchOpen) {
            androidx.compose.runtime.withFrameNanos { }
            searchFocusRequester.requestFocus()
            keyboard?.show()
        }
    }
    val closeSearch = {
        keyboard?.hide()
        searchOpen = false
        viewModel.onSearchQueryChanged("")
    }
    BackHandler(enabled = searchOpen, onBack = closeSearch)
    LaunchedEffect(widgetPeriod) {
        if (widgetPeriod != null) {
            keyboard?.hide()
            searchOpen = false
            viewModel.openWidgetPeriod(widgetPeriod)
            onWidgetPeriodHandled()
        }
    }

    // NOTE: the old totalItemCount + scrollToItem workaround was removed: it
    // issued a scroll during flings, fighting remeasure and triggering the
    // subcompose IllegalArgumentException. Stable keys + state recreation
    // above make it unnecessary (Lazy clamps internally).
    val walkthroughController = me.avinas.tempo.ui.components.LocalWalkthroughController.current

    // Pagination Logic - simplified for better scroll performance
    val isAtBottom by remember(listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.totalItemsCount == 0) {
                false
            } else {
                // Trigger pagination 3 items before reaching end for smoother loading
                val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisibleIndex >= layoutInfo.totalItemsCount - 3
            }
        }
    }

    LaunchedEffect(listState, isAtBottom, uiState.isLoading, uiState.page) {
        if (isAtBottom) {
            viewModel.loadMore()
        }
    }
    val onStatItemClick: (StatItem) -> Unit = { clickedItem ->
        walkthroughController.dismiss()
        resolveNavigation(clickedItem, onNavigateToTrack, onNavigateToArtist) { title, artist ->
            if (!openingAlbum) {
                openingAlbum = true
                scope.launch {
                    val error = try {
                        if (onNavigateToAlbum(title, artist)) null else albumNotFound
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        android.util.Log.w("StatsScreen", "Could not open album", failure)
                        albumLookupFailed
                    } finally {
                        openingAlbum = false
                    }
                    if (error != null) {
                        albumSnackbar.currentSnackbarData?.dismiss()
                        albumSnackbar.showSnackbar(error)
                    }
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().tempoAmbientBackdrop()) {
        Box(modifier = Modifier.fillMaxSize()) {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = {
                    scope.launch {
                        viewModel.refresh()
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(top = 100.dp, bottom = if (me.avinas.tempo.ui.theme.LocalTempoAppearance.current.nowListening == me.avinas.tempo.ui.appearance.AppearanceNowListening.COMPACT) 176.dp else 116.dp),
                modifier = Modifier.align(Alignment.TopCenter)
                    .widthIn(max = 960.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp) // Increased spacing for vertical rhythm
            ) {
                // 1. Sticky Tab Selector + Search
                stickyHeader(key = "sticky_tab_selector") {
                     Column(
                         modifier = Modifier
                             .fillMaxWidth()
                             .padding(bottom = 12.dp)
                     ) {
                         StatsTabSelector(
                             selectedTab = uiState.selectedTab,
                             onTabSelected = viewModel::onTabSelected
                         )
                     }
                }

                item(key = "time_range_selector") {
                    // Non-sticky row keeps the ranking tabs accessible without
                    // pinning two full rows to the viewport.
                    TimePeriodSelector(
                        selectedRange = uiState.selectedTimeRange,
                        onRangeSelected = viewModel::onTimeRangeSelected,
                        availableRanges = StatsAvailableRanges,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                if (uiState.selectedTab == StatsTab.TOP_ALBUMS && uiState.searchQuery.isBlank()) {
                    item(key = "album_poster") {
                        OutlinedButton(
                            onClick = { showAlbumPoster = true },
                            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                        ) {
                            Icon(Icons.Rounded.Album, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.album_poster_title))
                        }
                    }
                }

                if (appearance.showStatsSummary && uiState.searchQuery.isBlank()) {
                    uiState.periodTotals?.let { totals ->
                        item(key = "period_summary") {
                            StatsPeriodSummary(totals)
                        }
                    }
                }

                item(key = "sort_by_selector") {
                    // Sort By Selector
                    LaunchedEffect(uiState.selectedTab) {
                        walkthroughController.checkAndTrigger(me.avinas.tempo.ui.components.WalkthroughStep.STATS_SORT)
                    }
                    
                    SortBySelector(
                        selectedSortBy = uiState.selectedSortBy,
                        onSortBySelected = { 
                            walkthroughController.dismiss()
                            viewModel.onSortBySelected(it) 
                        },
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            walkthroughController.registerTarget(
                                me.avinas.tempo.ui.components.WalkthroughStep.STATS_SORT,
                                coordinates
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 2. Stats Items: paint layout-matched placeholders while
                // the first page is loading, never a completely empty viewport.
                if (uiState.isLoading && uiState.items.isEmpty()) {
                    item(key = "stats_skeleton") {
                        me.avinas.tempo.ui.components.TempoStatsLoadingSkeleton()
                    }
                } else if (!uiState.isLoading && uiState.items.isEmpty()) {
                    item(key = "empty_state") {
                        if (uiState.error != null) {
                            // Display error state if load failed
                            StatsErrorState(
                                message = uiState.error.orEmpty(),
                                onRetry = { viewModel.retry() },
                                modifier = Modifier.fillParentMaxHeight(0.7f)
                            )
                        } else if (uiState.searchQuery.isNotBlank()) {
                            SearchEmptyState(query = uiState.searchQuery)
                        } else {
                            me.avinas.tempo.ui.components.EmptyState(
                                 modifier = Modifier
                                     .fillMaxWidth()
                                     .fillParentMaxHeight(0.7f),
                                 timeRange = uiState.selectedTimeRange,
                                 onCheckSupportedApps = onNavigateToSupportedApps
                            )
                        }
                    }
                } else if (!uiState.isLoading && uiState.items.isNotEmpty()) {
                    if (uiState.error != null) {
                        item(key = "refresh_error") {
                            LoadErrorNotice(
                                message = stringResource(R.string.listen_refresh_failed),
                                onRetry = { scope.launch { viewModel.refresh() } },
                            )
                        }
                    }

                    // In search mode there is no hero: every match is shown with its
                    // global ranking position, which is the point of the search.
                    val isSearching = uiState.searchQuery.isNotBlank()
                    val firstItem = uiState.items.firstOrNull().takeIf { appearance.highlightTopResult && !isSearching }
                    val remainingItems = if (firstItem == null) uiState.items else uiState.items.drop(1)


                    // Rank 1 Hero
                    if (firstItem != null) {
                        item(key = "hero_item") {
                            LaunchedEffect(firstItem) {
                                // Try to trigger, controller handles priority/dismissal state
                                walkthroughController.checkAndTrigger(me.avinas.tempo.ui.components.WalkthroughStep.STATS_ITEM_CLICK)
                            }
                            
                            Box(
                                modifier = Modifier.onGloballyPositioned { coordinates ->
                                    walkthroughController.registerTarget(
                                        me.avinas.tempo.ui.components.WalkthroughStep.STATS_ITEM_CLICK,
                                        coordinates
                                    )
                                }
                            ) {

                                HeroStatItem(
                                    item = firstItem,
                                    onNavigate = { onStatItemClick(firstItem) }
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Remaining Items
                    itemsIndexed(
                        items = remainingItems,
                        key = { _, item -> itemKey(item) },
                        contentType = { _, item ->
                            when (item) {
                                is TopTrack -> "track"
                                is TopArtist -> "artist"
                                is TopAlbum -> "album"
                            }
                        }
                    ) { index, item ->
                        val rank = if (isSearching) (itemRank(item) ?: (index + 1)) else (index + if (firstItem == null) 1 else 2) // Search shows global rank
                        Box {
                            RankedStatItem(
                                rank = rank,
                                item = item,
                                onClick = onStatItemClick,
                            )
                        }
                    }
                }

                // Loading More Indicator
                if (uiState.isLoadingMore) {
                    item(key = "loading_more") {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            TempoCircularLoadingIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                }
            } // end PullToRefreshBox

            // Top Bar
            val isScrolled by remember(listState) {
                derivedStateOf {
                    listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
                }
            }
            if (!searchOpen) {
                StatsTopBar(
                    isScrolled = isScrolled,
                    hasItems = uiState.items.isNotEmpty(),
                    isSearchActive = uiState.searchQuery.isNotBlank(),
                    onOpenSearch = { searchOpen = true },
                    onOpenShare = { showShareDialog = true },
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
                )
            }

            if (uiState.isLoading) {
                TempoCircularLoadingIndicator(modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary)
            }

            // Search drawer: reveals horizontally from the search-icon side (End)
            // instead of dropping from the top, and stays minimal — just the
            // field itself, no full-width background block covering content.
            // Declared last so it draws above the top bar and list content.
            AnimatedVisibility(
                visible = searchOpen,
                enter = if (reducedMotion) EnterTransition.None else expandHorizontally(
                    expandFrom = Alignment.End,
                    animationSpec = MaterialTheme.motionScheme.fastSpatialSpec<androidx.compose.ui.unit.IntSize>()
                ) + fadeIn(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()),
                exit = if (reducedMotion) ExitTransition.None else shrinkHorizontally(
                    shrinkTowards = Alignment.End,
                    animationSpec = MaterialTheme.motionScheme.fastSpatialSpec<androidx.compose.ui.unit.IntSize>()
                ) + fadeOut(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    StatsSearchField(
                        query = uiState.searchQuery,
                        onQueryChange = viewModel::onSearchQueryChanged,
                        placeholder = stringResource(
                            when (uiState.selectedTab) {
                                StatsTab.TOP_SONGS -> R.string.stats_search_hint_songs
                                StatsTab.TOP_ARTISTS -> R.string.stats_search_hint_artists
                                StatsTab.TOP_ALBUMS -> R.string.stats_search_hint_albums
                            }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        focusRequester = searchFocusRequester,
                        onClose = closeSearch
                    )
                }
            }
            if (openingAlbum) {
                val loadingDescription = stringResource(R.string.ui_loading)
                TempoLinearLoadingIndicator(
                    modifier = Modifier.align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .semantics { contentDescription = loadingDescription },
                )
            }
            SnackbarHost(
                hostState = albumSnackbar,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 108.dp),
            )
        }

        if (showAlbumPoster) {
            AlbumPosterEditor(
                period = uiState.selectedTimeRange,
                sortBy = uiState.selectedSortBy,
                onDismiss = { showAlbumPoster = false },
            )
        }

        if (showShareDialog) {
            StatsShareDialog(
                tab = uiState.selectedTab,
                timeRange = uiState.selectedTimeRange,
                items = uiState.items,
                totals = uiState.periodTotals,
                onDismiss = { showShareDialog = false }
            )
        }
    }
}

// Helper for Navigation
internal fun resolveNavigation(
    item: StatItem,
    onTrack: (Long) -> Unit,
    onArtist: (String) -> Unit,
    onAlbum: (title: String, artist: String) -> Unit
) {
    when (item) {
        is TopTrack -> onTrack(item.trackId)
        is TopArtist -> if (item.artistId != null && item.artistId > 0) onArtist("id:${item.artistId}") else onArtist(item.artist)
        is TopAlbum -> onAlbum(item.album, item.artist)
    }
}

// Components
