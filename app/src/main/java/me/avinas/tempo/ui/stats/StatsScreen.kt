@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.avinas.tempo.ui.stats

import me.avinas.tempo.ui.effects.tempoAmbientBackdrop
import me.avinas.tempo.ui.effects.tempoTiltSurface
import me.avinas.tempo.ui.effects.tempoListMotion
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle

import me.avinas.tempo.ui.theme.TempoDarkBackground
import me.avinas.tempo.ui.theme.TempoDarkSurfaceSunken
import me.avinas.tempo.ui.theme.TempoAccentBright
import me.avinas.tempo.ui.theme.TextOnAccent
import me.avinas.tempo.ui.theme.GoldDark
import me.avinas.tempo.ui.theme.TextPrimary
import me.avinas.tempo.ui.theme.TextSecondary
import me.avinas.tempo.ui.theme.LocalTempoAppearance
import me.avinas.tempo.ui.theme.LocalTempoPalette
import me.avinas.tempo.ui.theme.innerShadow
import me.avinas.tempo.ui.theme.premiumClickable
import me.avinas.tempo.ui.theme.rememberReducedMotion
import me.avinas.tempo.ui.components.TempoDropdownMenu
import me.avinas.tempo.ui.components.TempoDropdownMenuItem
import me.avinas.tempo.ui.components.TempoIcons
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Brush

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Bolt
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import me.avinas.tempo.ui.components.CachedAsyncImage
import me.avinas.tempo.ui.components.MusicNotePlaceholder
import me.avinas.tempo.data.repository.SortBy
import me.avinas.tempo.data.stats.CombinedBasicStats
import me.avinas.tempo.data.stats.TimeRange
import me.avinas.tempo.data.stats.TopAlbum
import me.avinas.tempo.data.stats.TopArtist
import me.avinas.tempo.data.stats.TopTrack
import me.avinas.tempo.data.stats.StatItem
import me.avinas.tempo.ui.components.GlassCard
import me.avinas.tempo.ui.components.TimePeriodSelector
import me.avinas.tempo.ui.theme.TempoSecondary
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
                        Box(Modifier.tempoListMotion(index)) {
                            GlassStatItem(
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



@Composable
fun HeroStatItem(item: StatItem, onNavigate: () -> Unit) {
    val title = when (item) {
        is TopTrack -> item.title
        is TopArtist -> item.artist
        is TopAlbum -> item.album
    }
    val subtitle = when (item) {
        is TopTrack -> item.artist
        is TopArtist -> "${item.playCount} plays"
        is TopAlbum -> item.artist
    }
    val imageUrl = when (item) {
        is TopTrack -> item.albumArtUrl
        is TopArtist -> item.imageUrl
        is TopAlbum -> item.albumArtUrl
    }
    val label = when (item) {
        is TopTrack -> stringResource(R.string.stats_rank_1_track)
        is TopArtist -> stringResource(R.string.stats_rank_1_artist)
        is TopAlbum -> stringResource(R.string.stats_rank_1_album)
    }

    val palette = LocalTempoPalette.current
    val rankColor = remember(palette) { AppearancePaletteResolver.readableAccent(palette, GoldDark, palette.surfaceSunken) }
    val scheme = MaterialTheme.colorScheme

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .tempoTiltSurface()
            .premiumClickable(onClick = onNavigate),
        backgroundColor = rankColor.copy(alpha = 0.12f), // Reduced from 0.25f for better blend
        contentPadding = PaddingValues(16.dp) // Reduced from 24.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (LocalTempoAppearance.current.showStatsArtwork) {
                // Big Image
                Box(contentAlignment = Alignment.Center) {
                    // Glow Layer
                    Box(modifier = Modifier.size(90.dp).clip(CircleShape).background(rankColor.copy(alpha = 0.22f))) // Reduced glow size

                    // Image Layer
                    if (imageUrl != null) {
                        CachedAsyncImage(
                            imageUrl = imageUrl,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            targetSizeDp = 80 // Downsample for faster decode & less memory
                        )
                    } else {
                         Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(scheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                             Text(title.firstOrNull()?.toString() ?: "?", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                         }
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

            }
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = rankColor, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(title, style = MaterialTheme.typography.titleLargeEmphasized, color = MaterialTheme.colorScheme.onSurface, maxLines = 2)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }

            val timeMs = when (item) {
                is TopTrack -> item.totalTimeMs
                is TopArtist -> item.totalTimeMs
                is TopAlbum -> item.totalTimeMs
            }
            Spacer(modifier = Modifier.width(12.dp))
            Surface(
                color = scheme.secondaryContainer,
                contentColor = scheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Text(
                    text = displayStatsDuration(timeMs),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun GlassStatItem(rank: Int, item: StatItem, onClick: (StatItem) -> Unit) {
    val title = when (item) {
        is TopTrack -> item.title
        is TopArtist -> item.artist
        is TopAlbum -> item.album
    }
    val subtitle = when (item) {
        is TopTrack -> item.artist
        is TopArtist -> "${item.playCount} plays"
        is TopAlbum -> item.artist
    }
    val imageUrl = when (item) {
        is TopTrack -> item.albumArtUrl
        is TopArtist -> item.imageUrl
        is TopAlbum -> item.albumArtUrl
    }
    val timeMs = when (item) {
        is TopTrack -> item.totalTimeMs
        is TopArtist -> item.totalTimeMs
        is TopAlbum -> item.totalTimeMs
    }

    val scheme = MaterialTheme.colorScheme
    // A ranked result is a readable, tappable Material row, not a shadow
    // and alpha-gradient stack repeated dozens of times.
    val featured = rank in 1..3
    val container = if (featured) scheme.surfaceContainerHigh else scheme.surfaceContainerLow
    val rankTint = when (rank) {
        1 -> scheme.primary
        2 -> scheme.secondary
        3 -> scheme.tertiary
        else -> scheme.onSurfaceVariant
    }

    Surface(
        onClick = { onClick(item) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.large,
        color = container,
        contentColor = scheme.onSurface,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "#$rank",
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = rankTint,
                modifier = Modifier.width(36.dp),
            )
            if (LocalTempoAppearance.current.showStatsArtwork) {
                Box(
                    modifier = Modifier.size(48.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(scheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!imageUrl.isNullOrBlank()) {
                        CachedAsyncImage(
                            imageUrl = imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            targetSizeDp = 48,
                        )
                    } else {
                        Icon(
                            Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = scheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                // Settings may display time in days/hours/minutes: give the
                // duration its own full-width line rather than forcing it
                // beside an already narrow song or artist title.
                Text(
                    text = displayStatsDuration(timeMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = rankTint,
                )
            }
        }
    }
}

@Composable
fun EmptyStatsState() {
     Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            me.avinas.tempo.ui.components.TempoEmblem(
                icon = Icons.AutoMirrored.Filled.TrendingDown,
                size = 92.dp,
                style = me.avinas.tempo.ui.components.TempoEmblemStyle.MEMORY,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.stats_no_stats_yet), style = MaterialTheme.typography.titleLargeEmphasized, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.stats_start_listening), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Material 3 Expressive primary tabs: native keyboard/reader semantics,
 * adaptive indicator motion, and no per-tab shadow layers on scroll.
 */
@Composable
fun StatsTabSelector(selectedTab: StatsTab, onTabSelected: (StatsTab) -> Unit) {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        androidx.compose.material3.PrimaryTabRow(
            selectedTabIndex = StatsTab.entries.indexOf(selectedTab),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            StatsTab.entries.forEach { tab ->
                androidx.compose.material3.Tab(
                    selected = tab == selectedTab,
                    onClick = { if (tab != selectedTab) onTabSelected(tab) },
                    text = {
                        Text(
                            when (tab) {
                                StatsTab.TOP_SONGS -> stringResource(R.string.stats_tab_songs)
                                StatsTab.TOP_ARTISTS -> stringResource(R.string.stats_tab_artists)
                                StatsTab.TOP_ALBUMS -> stringResource(R.string.stats_tab_albums)
                            },
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                )
            }
        }
    }
}

/**
 * Native Material 3 Expressive search field for songs, artists and albums.
 * This keeps input focus, IME handling and clear/close actions accessible
 * without a nested glass surface or a separate shadow-render pass.
 */
@Composable
private fun StatsSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onClose: (() -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        placeholder = {
            Text(placeholder, maxLines = 1,
                style = MaterialTheme.typography.bodyLarge,
                overflow = TextOverflow.Ellipsis)
        },
        leadingIcon = {
            if (onClose != null) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.stats_search_close))
                }
            } else {
                Icon(Icons.Default.Search, contentDescription = null)
            }
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close,
                        contentDescription = stringResource(R.string.stats_search_clear))
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun SearchEmptyState(query: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp, horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.stats_search_no_results, query),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Error state view with retry action. */
@Composable
private fun StatsErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.stats_error_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = TextOnAccent
                )
            ) {
                Text(stringResource(R.string.stats_error_retry))
            }
        }
    }
}

/** Global ranking position carried by search results (null outside search mode). */
private fun itemRank(item: StatItem): Int? = when (item) {
    is TopTrack -> item.rank
    is TopArtist -> item.rank
    is TopAlbum -> item.rank
}

private fun itemKey(item: StatItem): String = when (item) {
    is TopTrack -> "track_${item.trackId}"
    is TopArtist -> "artist_${item.artistId ?: item.artist}"
    is TopAlbum -> "album_${item.album}_${item.artist}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatsTopBar(
    isScrolled: Boolean,
    hasItems: Boolean,
    isSearchActive: Boolean,
    onOpenSearch: () -> Unit,
    onOpenShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val headerAlpha by animateFloatAsState(targetValue = if (isScrolled) 1f else 0f, label = "headerAlpha")

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = headerAlpha),
        shadowElevation = 0.dp,
        modifier = modifier
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    stringResource(R.string.stats_screen_title),
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            actions = {
                IconButton(onClick = onOpenSearch) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.stats_search_open),
                        // Tinted while a search filter is active so search mode is never invisible.
                        tint = if (isSearchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (hasItems) {
                    IconButton(onClick = onOpenShare) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.stats_share),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
    }
}

private val StatsAvailableRanges = TimeRange.entries.toList()

/**
 * Listening analytics deserve a visual dashboard rather than three tightly
 * packed values. The time metric leads, while counts become responsive tiles.
 */
@Composable
private fun StatsPeriodSummary(totals: CombinedBasicStats) {
    val summary = LocalTempoAppearance.current.statsSummary
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        me.avinas.tempo.ui.components.TempoMetricTile(
            value = displayStatsDuration(totals.totalTimeMs),
            label = stringResource(R.string.stats_period_listening_time),
            icon = Icons.Rounded.Headphones,
            tone = me.avinas.tempo.ui.components.TempoTileTone.AQUA,
            modifier = Modifier.fillMaxWidth(),
            minimumHeight = 132.dp,
        )
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val single = maxWidth < 360.dp ||
                androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.3f
            val cells: List<Triple<String, String, androidx.compose.ui.graphics.vector.ImageVector>> = listOf(
                Triple(
                    stringResource(R.string.stats_period_number, totals.playCount),
                    stringResource(R.string.stats_period_plays),
                    Icons.Rounded.PlayArrow,
                ),
                Triple(
                    stringResource(R.string.stats_period_number, totals.summaryCount(summary)),
                    stringResource(statsSummaryLabel(summary)),
                    Icons.Rounded.Equalizer,
                ),
            )
            if (single) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    cells.forEachIndexed { index, (value, label, icon) ->
                        me.avinas.tempo.ui.components.TempoMetricTile(
                            value = value, label = label, icon = icon,
                            tone = if (index == 0) me.avinas.tempo.ui.components.TempoTileTone.SKY
                                else me.avinas.tempo.ui.components.TempoTileTone.LILAC,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    cells.forEachIndexed { index, (value, label, icon) ->
                        me.avinas.tempo.ui.components.TempoMetricTile(
                            value = value, label = label, icon = icon,
                            tone = if (index == 0) me.avinas.tempo.ui.components.TempoTileTone.SKY
                                else me.avinas.tempo.ui.components.TempoTileTone.LILAC,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SortBySelector(
    selectedSortBy: SortBy, 
    onSortBySelected: (SortBy) -> Unit,
    modifier: Modifier = Modifier
) {
     var expanded by remember { mutableStateOf(false) }
    
    Row(
        modifier = modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = stringResource(R.string.stats_sort_by), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            TextButton(
                onClick = { expanded = true },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = when (selectedSortBy) {
                        SortBy.COMBINED_SCORE -> stringResource(R.string.stats_sort_combined)
                        SortBy.PLAY_COUNT -> stringResource(R.string.stats_sort_play_count)
                        SortBy.TOTAL_TIME -> stringResource(R.string.stats_sort_total_time)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            TempoDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                SortBy.entries.forEach { sortBy ->
                    val title = when (sortBy) {
                        SortBy.COMBINED_SCORE -> stringResource(R.string.stats_sort_combined)
                        SortBy.PLAY_COUNT -> stringResource(R.string.stats_sort_play_count)
                        SortBy.TOTAL_TIME -> stringResource(R.string.stats_sort_total_time)
                    }
                    val icon = when (sortBy) {
                        SortBy.COMBINED_SCORE -> Icons.Rounded.AutoAwesome
                        SortBy.PLAY_COUNT -> Icons.Rounded.PlayArrow
                        SortBy.TOTAL_TIME -> Icons.Rounded.Schedule
                    }
                    TempoDropdownMenuItem(
                        title = title,
                        onClick = {
                            expanded = false
                            onSortBySelected(sortBy)
                        },
                        leadingIcon = icon,
                        isSelected = sortBy == selectedSortBy
                    )
                }
            }
        }
    }
}
