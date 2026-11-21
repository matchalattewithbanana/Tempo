@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.avinas.tempo.ui.history

import me.avinas.tempo.ui.effects.tempoAmbientBackdrop
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import me.avinas.tempo.ui.components.TempoCircularLoadingIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import me.avinas.tempo.R
import me.avinas.tempo.ui.components.*
import me.avinas.tempo.ui.theme.*
import java.util.Locale

private val RelativeDateFormatter = java.time.format.DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onNavigateToTrack: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var shareHistory by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    if (shareHistory.isNotEmpty()) {
        SnapshotShareDialog(
            snapshot = ShareSnapshot(
                title = stringResource(R.string.share_history_title),
                rows = shareHistory,
            ),
            onDismiss = { shareHistory = emptyList() },
        )
    }
    // New search = new list: reset scroll synchronously via state recreation
    // (parent remember includes searchQuery). No manual scroll-to-item here:
    // issuing a scroll request mid-fling fights the fling's own remeasure and
    // was itself a trigger for the subcompose IllegalArgumentException.
    val listState = remember(uiState.viewMode, uiState.startDate, uiState.endDate, uiState.showSkips, uiState.searchQuery) { LazyListState() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    
    // Show feedback snackbar when message is set
    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearFeedbackMessage()
        }
    }
    
    val mutationError = uiState.mutationError?.let { stringResource(it) }
    LaunchedEffect(mutationError) {
        if (mutationError != null) {
            snackbarHostState.showSnackbar(mutationError, duration = SnackbarDuration.Short)
            viewModel.clearMutationError()
        }
    }

    // Pagination — prefetch 4 items before end to prevent scrolling stalls and avoid per-pixel offset recalculation
    val isAtBottom by remember(listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.totalItemsCount == 0) {
                false
            } else {
                val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisibleIndex >= layoutInfo.totalItemsCount - 4
            }
        }
    }

    val sheetState = rememberModalBottomSheetState()
    var showFilterSheet by remember { mutableStateOf(false) }

    LaunchedEffect(listState, isAtBottom) {
        if (isAtBottom) {
            viewModel.loadMore()
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.extraLarge,
            dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle() }
        ) {
            HistoryFilterSheetContent(
                currentShowSkips = uiState.showSkips,
                currentStartDate = uiState.startDate,
                currentEndDate = uiState.endDate,
                onApply = { start, end, skips ->
                    viewModel.onFilterChanged(start, end, skips)
                    showFilterSheet = false
                },
                onReset = {
                    viewModel.onFilterChanged(null, null, true)
                    showFilterSheet = false
                }
            )
        }
    }

    if (uiState.isMarking) {
        val loading = stringResource(R.string.ui_loading)
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Box(Modifier.padding(22.dp), contentAlignment = Alignment.Center) {
                    TempoCircularLoadingIndicator(
                        modifier = Modifier.size(36.dp).semantics { contentDescription = loading },
                        strokeWidth = 3.dp,
                    )
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
                // List Content
                HistoryListContent(
                    // Recent Activity section
                    groupedItems = uiState.groupedItems,
                    isLoading = uiState.isLoading,
                    isLoadingMore = uiState.isLoadingMore,
                    loadFailed = uiState.loadFailed,
                    pageFailed = uiState.pageFailed,
                    lastFmFailed = uiState.lastFmFailedPage != null,
                    onRetryLoad = { scope.launch { viewModel.refresh() } },
                    showCoachMark = uiState.showCoachMark,
                    listState = listState,
                    viewModel = viewModel,
                    onNavigateToTrack = onNavigateToTrack,
                    onDismissCoachMark = viewModel::dismissCoachMark,
                    // Last.fm History section
                    viewMode = uiState.viewMode,
                    hasArchiveData = uiState.hasArchiveData,
                    lastFmGroupedItems = uiState.lastFmGroupedItems,
                    archiveItems = uiState.archiveItems,
                    archiveTrackCount = uiState.archiveTrackCount,
                    archiveTotalPlays = uiState.archiveTotalPlays,
                    isLoadingMoreLastFm = uiState.isLoadingMoreLastFm,
                    hasMoreLastFm = uiState.hasMoreLastFm,
                    onLoadMoreLastFm = viewModel::loadMoreLastFmHistory,
                    onViewModeChange = viewModel::setViewMode,
                    searchQuery = uiState.searchQuery,
                    onClearSearch = { viewModel.onSearchQueryChanged("") }
                )

                // Snackbar for feedback messages
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = if (me.avinas.tempo.ui.theme.LocalTempoAppearance.current.nowListening == me.avinas.tempo.ui.appearance.AppearanceNowListening.COMPACT) 160.dp else 100.dp), // Above nav bar
                    snackbar = { snackbarData ->
                        TempoSnackbar(snackbarData)
                    }
                )
                
            } // end PullToRefreshBox

            // Top Bar with scroll-aware elevation and studio glass styling
            val isScrolled by remember(listState) {
                derivedStateOf {
                    listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
                }
            }
            val headerColor by androidx.compose.animation.animateColorAsState(
                targetValue = if (isScrolled) {
                    MaterialTheme.colorScheme.surfaceContainer
                } else {
                    Color.Transparent
                },
                animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
                label = "historyHeaderColor",
            )

            Surface(
                color = headerColor,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .zIndex(10f)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Expressive native search field: a single tonal
                            // surface instead of nested glass, shadow and manual text UI.
                            val focusManager = LocalFocusManager.current
                            TextField(
                                value = uiState.searchQuery,
                                onValueChange = viewModel::onSearchQueryChanged,
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = MaterialTheme.shapes.extraLarge,
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.history_search_hint),
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null)
                                },
                                trailingIcon = {
                                    if (uiState.searchQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = { viewModel.onSearchQueryChanged("") }
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = stringResource(R.string.stats_search_clear),
                                            )
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

                            IconButton(
                                enabled = !uiState.isLoading && !uiState.isRefreshing && !uiState.isMarking &&
                                    !uiState.searchPending && !uiState.loadFailed &&
                                    (uiState.rawItems.isNotEmpty() || uiState.lastFmItems.isNotEmpty() || uiState.archiveItems.isNotEmpty()),
                                onClick = {
                                    val events = (uiState.rawItems.take(5) + uiState.lastFmItems.take(5))
                                        .distinctBy { it.id }.map { it.timestamp to (it.title to it.artist) }
                                    val archived = uiState.archiveItems.take(5)
                                        .map { it.timestamp to (it.trackTitle to it.artistName) }
                                    shareHistory = (events + archived).sortedByDescending { it.first }.take(5).map { it.second }
                                },
                            ) {
                                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share_history))
                            }

                            // Filter Button
                            val isFilterActive = uiState.startDate != null || !uiState.showSkips

                            IconButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showFilterSheet = true
                                },
                                modifier = Modifier.size(48.dp),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (isFilterActive) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    },
                                    contentColor = if (isFilterActive) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.List,
                                    contentDescription = stringResource(R.string.history_filter_title),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }

                }
            }
        }
    }
}
