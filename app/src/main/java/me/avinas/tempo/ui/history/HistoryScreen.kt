package me.avinas.tempo.ui.history

import me.avinas.tempo.ui.effects.tempoAmbientBackdrop
import me.avinas.tempo.ui.effects.tempoTiltSurface
import androidx.compose.material.icons.filled.Share
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import me.avinas.tempo.ui.components.TempoCircularLoadingIndicator
import me.avinas.tempo.ui.components.TempoLinearLoadingIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import me.avinas.tempo.R
import me.avinas.tempo.ui.appearance.AppearancePaletteResolver
import me.avinas.tempo.data.local.dao.HistoryItem
import me.avinas.tempo.ui.components.*
import me.avinas.tempo.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
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
            val headerAlpha by animateFloatAsState(
                targetValue = if (isScrolled) 1f else 0f,
                label = "historyHeaderAlpha"
            )

            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f * headerAlpha),
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

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (isFilterActive) {
                                            Modifier
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                                        )
                                                    )
                                                )
                                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape)
                                        } else {
                                            Modifier
                                                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f))
                                                .border(0.8.dp, GlassBorderSoft, CircleShape)
                                        }
                                    )
                                    .premiumClickable(
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            showFilterSheet = true
                                        },
                                        pressedScale = 0.94f
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.List,
                                    contentDescription = stringResource(R.string.history_filter_title),
                                    tint = if (isFilterActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )

                                // Active indicator dot
                                if (isFilterActive) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(top = 4.dp, end = 4.dp)
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                            .border(1.dp, MaterialTheme.colorScheme.background, CircleShape)
                                    )
                                }
                            }
                        }
                    }

                    if (isScrolled) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.8.dp)
                                .background(GlassBorderSoft.copy(alpha = 0.6f * headerAlpha))
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryFilterSheetContent(
    currentShowSkips: Boolean,
    currentStartDate: Long?,
    currentEndDate: Long?,
    onApply: (Long?, Long?, Boolean) -> Unit,
    onReset: () -> Unit
) {
    var showSkips by remember { mutableStateOf(currentShowSkips) }
    var selectedRange by remember { mutableStateOf(getRangeLabel(currentStartDate)) }
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 28.dp)
            .navigationBarsPadding()
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.history_filter_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.history_reset),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (selectedRange != "All Time" || !showSkips) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedRange = "All Time"
                        showSkips = true
                        onReset()
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Time Range Kicker
        Text(
            text = stringResource(R.string.history_time_range).uppercase(Locale.getDefault()),
            style = KickerSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Expressive segmented choices retain the History filter keys but
        // use native focus, selection and touch-target behavior.
        val ranges = listOf(
            "All Time" to stringResource(R.string.history_all_time),
            "Last 7 Days" to stringResource(R.string.history_7_days_abbr),
            "Last 30 Days" to stringResource(R.string.history_30_days_abbr),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(androidx.compose.material3.ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            ranges.forEachIndexed { index, (key, label) ->
                androidx.compose.material3.ToggleButton(
                    checked = selectedRange == key,
                    shapes = when (index) {
                        0 -> androidx.compose.material3.ButtonGroupDefaults.connectedLeadingButtonShapes()
                        ranges.lastIndex -> androidx.compose.material3.ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> androidx.compose.material3.ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                    onCheckedChange = {
                        if (selectedRange != key) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedRange = key
                        }
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        .semantics { role = Role.RadioButton },
                ) {
                    Text(label, style = MaterialTheme.typography.labelLarge,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Playback Kicker
        Text(
            text = stringResource(R.string.history_playback).uppercase(Locale.getDefault()),
            style = KickerSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    showSkips = !showSkips
                },
            shape = RoundedCornerShape(16.dp),
            backgroundColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.7f),
            variant = GlassCardVariant.LowProminence,
            borderColor = GlassBorderSoft,
            borderWidth = 0.8.dp,
            contentPadding = PaddingValues(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.history_show_skipped),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Include songs skipped during playback",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = showSkips,
                    onCheckedChange = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showSkips = it
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Apply Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primary)
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    ),
                    RoundedCornerShape(24.dp)
                )
                .premiumClickable(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val (start, end) = getRangeBounds(selectedRange)
                        onApply(start, end, showSkips)
                    },
                    pressedScale = 0.98f
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.history_apply_filters),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun getRangeLabel(start: Long?): String {
    if (start == null) return "All Time"
    val now = System.currentTimeMillis()
    val diff = now - start
    return when {
        diff <= 8 * 24 * 60 * 60 * 1000L -> "Last 7 Days"
        diff <= 31 * 24 * 60 * 60 * 1000L -> "Last 30 Days"
        else -> "All Time"
    }
}

fun getRangeBounds(label: String): Pair<Long?, Long?> {
    val now = System.currentTimeMillis()
    return when (label) {
        "Last 7 Days" -> Pair(now - 7 * 24 * 60 * 60 * 1000L, now)
        "Last 30 Days" -> Pair(now - 30 * 24 * 60 * 60 * 1000L, now)
        else -> Pair(null, null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteHistoryItem(
    item: HistoryItem,
    index: Int,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onMarkContent: ((Long, String, Boolean) -> Unit)? = null,
    onMarkArtist: ((Long, String, Boolean) -> Unit)? = null
) {
    // Use item.id as key for all state to prevent state reuse when items shift
    var showDeleteDialog by remember(item.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    
    // Reset dismiss state when item.id changes (item was deleted and a new one appeared)
    val dismissState = key(item.id) {
        rememberSwipeToDismissBoxState()
    }
    
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            showDeleteDialog = true
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        Dialog(onDismissRequest = { 
            showDeleteDialog = false 
            scope.launch { dismissState.snapTo(SwipeToDismissBoxValue.Settled) }
        }) {
            TempoDialogSurface {
                TempoDialogIcon(
                    icon = TempoIcons.Trash,
                    tint = TempoError,
                    size = 48
                )
                Spacer(modifier = Modifier.height(16.dp))
                TempoDialogTitle(text = stringResource(R.string.history_delete_dialog_title))
                Spacer(modifier = Modifier.height(8.dp))
                TempoDialogBody(text = stringResource(R.string.history_delete_dialog_message, item.title))
                Spacer(modifier = Modifier.height(24.dp))
                TempoDialogDangerButton(
                    text = stringResource(R.string.history_delete_button),
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    icon = TempoIcons.Trash
                )
                Spacer(modifier = Modifier.height(8.dp))
                TempoDialogSecondaryButton(
                    text = stringResource(R.string.common_cancel),
                    onClick = {
                        showDeleteDialog = false
                        scope.launch { dismissState.snapTo(SwipeToDismissBoxValue.Settled) }
                    }
                )
            }
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val isDismissing = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart ||
                    dismissState.currentValue == SwipeToDismissBoxValue.EndToStart

            if (isDismissing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.errorContainer,
                                    MaterialTheme.colorScheme.error
                                )
                            )
                        ),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = TempoIcons.Trash,
                        contentDescription = stringResource(R.string.history_delete_button),
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier
                            .padding(end = 24.dp)
                            .size(22.dp)
                    )
                }
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    ) {
        HistoryListItem(
            item = item, 
            index = index, 
            onClick = onClick,
            onDeleteClick = { showDeleteDialog = true },
            onMarkContent = { type, delete -> onMarkContent?.invoke(item.track_id, type, delete) },
            onMarkArtist = { type, delete -> onMarkArtist?.invoke(item.track_id, type, delete) }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryListContent(
    // Recent Activity section (live tracking, source != fm.last.import)
    groupedItems: Map<String, List<HistoryItem>>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    loadFailed: Boolean,
    pageFailed: Boolean,
    lastFmFailed: Boolean,
    onRetryLoad: () -> Unit,
    showCoachMark: Boolean,
    listState: LazyListState,
    viewModel: HistoryViewModel,
    onNavigateToTrack: (Long) -> Unit,
    onDismissCoachMark: () -> Unit,
    // View mode and archive data
    viewMode: HistoryViewMode = HistoryViewMode.UNIFIED,
    hasArchiveData: Boolean = false,
    archiveItems: List<ArchiveHistoryItem> = emptyList(),
    archiveTrackCount: Int = 0,
    archiveTotalPlays: Long = 0,
    // Last.fm History section (source == fm.last.import)
    lastFmGroupedItems: Map<String, List<HistoryItem>> = emptyMap(),
    isLoadingMoreLastFm: Boolean = false,
    hasMoreLastFm: Boolean = false,
    onLoadMoreLastFm: () -> Unit = {},
    onViewModeChange: (HistoryViewMode) -> Unit = {},
    searchQuery: String = "",
    onClearSearch: () -> Unit = {}
) {
    // NOTE: no manual scroll-to-item clamp here. The previous workaround
    // computed its own item count and issued requestScrollToItem() mid-fling,
    // fighting the fling's remeasure and triggering the subcompose
    // IllegalArgumentException it was meant to prevent. With stable item keys
    // Lazy clamps an out-of-range index internally; filter/search resets
    // recreate listState (see HistoryScreen remember keys) so the index is 0.

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            // Top padding for persistent search bar (status bar + 48dp bar + paddings), bottom for bottom nav.
            // Center the timeline on tablets so individual events stay scannable.
            contentPadding = PaddingValues(top = 96.dp, bottom = if (me.avinas.tempo.ui.theme.LocalTempoAppearance.current.nowListening == me.avinas.tempo.ui.appearance.AppearanceNowListening.COMPACT) 180.dp else 120.dp),
            modifier = Modifier.align(Alignment.TopCenter).widthIn(max = 960.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // View Mode Toggle Banner (only show if user has archive data in SEPARATED mode)
            if (hasArchiveData && viewMode == HistoryViewMode.SEPARATED) {
                item(key = "view_mode_banner") {
                    SeparatedModeInfoBanner(
                        archiveTrackCount = archiveTrackCount,
                        archiveTotalPlays = archiveTotalPlays
                    )
                }
            }
            
            // Empty State
            val allEmpty = groupedItems.isEmpty() && lastFmGroupedItems.isEmpty() && archiveItems.isEmpty()
            if (isLoading && allEmpty) {
                item(key = "history_skeleton") {
                    me.avinas.tempo.ui.components.TempoHistoryLoadingSkeleton()
                }
            }
            if (!isLoading && loadFailed) {
                item(key = "history_load_error") {
                    me.avinas.tempo.ui.components.LoadErrorNotice(
                        stringResource(if (allEmpty) R.string.history_load_failed else R.string.listen_refresh_failed),
                        onRetryLoad,
                    )
                }
            }
            if (!isLoading && allEmpty && !loadFailed && !lastFmFailed && !isLoadingMoreLastFm) {
                item(key = "empty_state") {
                    if (searchQuery.isNotBlank()) {
                        SearchEmptyState(
                            query = searchQuery,
                            onClear = onClearSearch
                        )
                    } else {
                        HistoryEmptyState()
                    }
                }
            }

            // SECTION 1: Recent Activity (Live Tracking)
            // In SEPARATED mode, show section header
            if (viewMode == HistoryViewMode.SEPARATED && groupedItems.isNotEmpty()) {
                item(key = "recent_activity_header") {
                    SectionDividerHeader(
                        title = stringResource(R.string.history_recent_activity),
                        subtitle = stringResource(R.string.history_recent_activity_subtitle),
                        icon = Icons.Default.PlayCircle,
                        iconTint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            groupedItems.entries.forEachIndexed { groupIndex, (header, itemsList) ->
                stickyHeader(key = "header_$header") {
                    HistorySectionHeader(header)
                }

                // itemsIndexed with an item-derived key (never items(count) with
                // itemsList[index] lookup): index-lookup keys resolve to a
                // different id when pagination/live inserts swap the list
                // mid-fling, reusing a slot twice -> subcompose
                // IllegalArgumentException (play-store crash).
                itemsIndexed(
                    items = itemsList,
                    key = { _, item -> item.id },
                    contentType = { _, _ -> "history_item" }
                ) { index, item ->
                    val isFirstItem = groupIndex == 0 && index == 0
                    
                    // Walkthrough Integration
                    val walkthroughController = me.avinas.tempo.ui.components.LocalWalkthroughController.current
                    
                    if (isFirstItem) {
                        LaunchedEffect(Unit) {
                            walkthroughController.checkAndTrigger(me.avinas.tempo.ui.components.WalkthroughStep.HISTORY_FILTER)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            
                            .then(
                                if (isFirstItem) {
                                    Modifier.onGloballyPositioned { coordinates ->
                                        walkthroughController.registerTarget(
                                            me.avinas.tempo.ui.components.WalkthroughStep.HISTORY_FILTER, 
                                            coordinates
                                        )
                                    }
                                } else Modifier
                            )
                    ) {
                        SwipeToDeleteHistoryItem(
                            item = item,
                            index = index,
                            onDelete = { viewModel.deleteListeningEvent(item.id) },
                            onClick = { onNavigateToTrack(item.track_id) },
                            onMarkContent = viewModel::markContent,
                            onMarkArtist = viewModel::markArtistContent
                        )
                    }
                }
            }

            // Loading more indicator for Recent Activity
            if (isLoadingMore) {
                item(key = "loading_more_recent") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TempoCircularLoadingIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                    }
                }
            }
            
            if (!isLoading && pageFailed) {
                item(key = "history_page_error") {
                    me.avinas.tempo.ui.components.LoadErrorNotice(
                        stringResource(R.string.history_page_failed),
                        viewModel::retryNextPage,
                    )
                }
            }

            // SECTION 2: Last.fm History (only in SEPARATED mode)
            if (viewMode == HistoryViewMode.SEPARATED && (lastFmGroupedItems.isNotEmpty() || archiveItems.isNotEmpty() || lastFmFailed || isLoadingMoreLastFm)) {
                // Section header for Last.fm History
                item(key = "lastfm_history_header") {
                    SectionDividerHeader(
                        title = stringResource(R.string.history_lastfm_history),
                        subtitle = stringResource(R.string.history_lastfm_subtitle),
                        icon = Icons.Default.History,
                        iconTint = GoldDark
                    )
                }
                
                // Last.fm imported events from listening_events table
                lastFmGroupedItems.entries.forEachIndexed { groupIndex, (header, itemsList) ->
                    stickyHeader(key = "lastfm_header_$header") {
                        HistorySectionHeader(header, isLastFmSection = true)
                    }

                    itemsIndexed(
                        items = itemsList,
                        key = { _, item -> "lastfm_${item.id}" },
                        contentType = { _, _ -> "history_item" }
                    ) { index, item ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            SwipeToDeleteHistoryItem(
                                item = item,
                                index = index,
                                onDelete = { viewModel.deleteListeningEvent(item.id) },
                                onClick = { onNavigateToTrack(item.track_id) },
                                onMarkContent = viewModel::markContent,
                                onMarkArtist = viewModel::markArtistContent
                            )
                        }
                    }
                }
                
                // Loading more indicator for Last.fm section
                if (isLoadingMoreLastFm) {
                    item(key = "loading_more_lastfm") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            TempoCircularLoadingIndicator(
                                color = GoldDark,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                        }
                    }
                }
                
                if (!isLoading && lastFmFailed) {
                    item(key = "lastfm_load_error") {
                        me.avinas.tempo.ui.components.LoadErrorNotice(
                            stringResource(R.string.history_lastfm_load_failed),
                            viewModel::retryLastFm,
                        )
                    }
                } else if (!isLoading && !isLoadingMoreLastFm && !loadFailed && hasMoreLastFm && lastFmGroupedItems.isNotEmpty()) {
                    item(key = "lastfm_load_more") {
                        TextButton(onClick = onLoadMoreLastFm, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.history_load_more))
                        }
                    }
                }

                // Archive items (long-tail tracks)
                if (archiveItems.isNotEmpty()) {
                    item(key = "archive_sub_header") {
                        ArchiveSectionHeader(
                            itemCount = archiveItems.size,
                            totalTracks = archiveTrackCount
                        )
                    }
                    
                    itemsIndexed(
                        items = archiveItems,
                        key = { _, item -> "archive_sep_${item.archiveId}" },
                        contentType = { _, _ -> "archive_item" }
                    ) { index, archiveItem ->
                        Box(Modifier) {
                            ArchiveHistoryListItem(item = archiveItem)
                        }
                    }
                }
            }
            
            // UNIFIED MODE: Archive section at bottom
            if (viewMode == HistoryViewMode.UNIFIED && archiveItems.isNotEmpty()) {
                item(key = "archive_header") {
                    ArchiveSectionHeader(
                        itemCount = archiveItems.size,
                        totalTracks = archiveTrackCount
                    )
                }
                
                itemsIndexed(
                    items = archiveItems,
                    key = { _, item -> "archive_uni_${item.archiveId}" },
                    contentType = { _, _ -> "archive_item" }
                ) { index, archiveItem ->
                    Box(Modifier) {
                        ArchiveHistoryListItem(item = archiveItem)
                    }
                }
            }
        }
    }
}

/**
 * Info banner shown in SEPARATED mode to explain the two-section layout.
 */
@Composable
fun SeparatedModeInfoBanner(
    archiveTrackCount: Int,
    archiveTotalPlays: Long,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = scheme.secondaryContainer,
        contentColor = scheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = scheme.onSecondaryContainer.copy(alpha = 0.10f),
                contentColor = scheme.onSecondaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.history_lastfm_imported),
                    style = MaterialTheme.typography.titleSmallEmphasized,
                )
                Text(
                    text = "${archiveTotalPlays.formatNumber()} plays • $archiveTrackCount tracks archived",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSecondaryContainer,
                )
            }
        }
    }
}

/**
 * Section divider header for Recent Activity / Last.fm History sections.
 */
@Composable
fun SectionDividerHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = MaterialTheme.shapes.medium,
                color = scheme.surfaceContainerHigh,
                contentColor = iconTint,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = scheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(color = scheme.outlineVariant)
    }
}

/**
 * Header for the archive section in history.
 */
@Composable
fun ArchiveSectionHeader(
    itemCount: Int,
    totalTracks: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(GoldDark.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = GoldLight,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = stringResource(R.string.history_from_archive).uppercase(Locale.getDefault()),
                style = KickerSmall,
                fontWeight = FontWeight.Bold,
                color = GoldLight,
                letterSpacing = 1.sp
            )
        }
        Text(
            text = "Showing $itemCount of $totalTracks archived tracks",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
            modifier = Modifier.padding(start = 36.dp, top = 2.dp)
        )
    }
}

/**
 * Display item for archived scrobbles.
 * Visually distinct with gold telemetry accents.
 */
@Composable
fun ArchiveHistoryListItem(item: ArchiveHistoryItem) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.large,
        color = scheme.surfaceContainerLow,
        contentColor = scheme.onSurface,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CachedAsyncImage(
                imageUrl = item.albumArtUrl.takeIf {
                    me.avinas.tempo.ui.theme.LocalTempoAppearance.current.showHistoryArtwork
                },
                contentDescription = "Album art for ${item.trackTitle}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(52.dp).clip(MaterialTheme.shapes.medium),
                targetSizeDp = 52,
                placeholder = {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = scheme.surfaceContainerHighest,
                        contentColor = scheme.onSurfaceVariant,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Album,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                },
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.trackTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.artistName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Surface(
                shape = MaterialTheme.shapes.small,
                color = scheme.secondaryContainer,
                contentColor = scheme.onSecondaryContainer,
            ) {
                Text(
                    text = "${item.playCount} plays",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                )
            }
        }
    }
}

/**
 * Format large numbers with K/M suffixes.
 */
private fun Long.formatNumber(): String {
    return when {
        this >= 1_000_000 -> String.format(Locale.US, "%.1fM", this / 1_000_000.0)
        this >= 1_000 -> String.format(Locale.US, "%.1fK", this / 1_000.0)
        else -> this.toString()
    }
}

@Composable
fun HistorySectionHeader(title: String, isLastFmSection: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = scheme.surface,
        contentColor = scheme.onSurface,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                modifier = Modifier.size(width = 4.dp, height = 18.dp),
                shape = CircleShape,
                color = if (isLastFmSection) scheme.secondary else scheme.primary,
                content = {},
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmallEmphasized,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryListItem(
    item: HistoryItem, 
    index: Int, 
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMarkContent: ((String, Boolean) -> Unit)? = null, // type, deleteFromHistory
    onMarkArtist: ((String, Boolean) -> Unit)? = null // type, deleteFromHistory - artist level
) {
    var showMenu by remember { mutableStateOf(false) }
    val walkthroughController = me.avinas.tempo.ui.components.LocalWalkthroughController.current
    val haptics = LocalHapticFeedback.current

    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .combinedClickable(
                    onClick = {
                        walkthroughController.dismiss()
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    },
                    onLongClick = {
                        walkthroughController.dismiss()
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = true
                    },
                ),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    if (!me.avinas.tempo.ui.theme.LocalTempoAppearance.current.showHistoryArtwork || item.album_art_url.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        CachedAsyncImage(
                            imageUrl = item.album_art_url,
                            contentDescription = "Album art for ${item.title}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            targetSizeDp = 52
                        )
                    }
                    
                    // Visual Badge for Non-Music Content on Art
                    if (item.content_type != "MUSIC") {
                        val badgeColor = when(item.content_type) {
                            "PODCAST" -> TempoCyan
                            "AUDIOBOOK" -> GoldenAmber
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                        }
                        
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(badgeColor)
                                .size(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (item.content_type == "PODCAST") 
                                    TempoIcons.Podcast 
                                else 
                                    TempoIcons.Audiobook,
                                contentDescription = item.content_type,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(14.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        
                        // Desktop source tag
                        if (item.source.startsWith("desktop:")) {
                            val palette = LocalTempoPalette.current
                            val badgeColor = remember(palette) {
                                AppearancePaletteResolver.readableAccent(palette, Color(0xFFA78BFA), palette.surfaceSunken)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(badgeColor.copy(alpha = 0.10f))
                                    .border(0.8.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "DESK",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                                    color = badgeColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.width(10.dp))
    
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.height(52.dp)
                ) {
                    Text(
                        text = formatRelativeTime(item.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                        fontWeight = FontWeight.Medium
                    )
                    
                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDeleteClick()
                        },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = TempoIcons.Trash,
                            contentDescription = stringResource(R.string.history_delete_button),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
        
        // Context Menu for Blocking Content
        TempoDropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            TempoMenuKicker(text = stringResource(R.string.tracking_always_music))
            TempoDropdownMenuItem(
                title = stringResource(R.string.history_always_music_track),
                leadingIcon = Icons.Rounded.MusicNote,
                leadingIconTint = MaterialTheme.colorScheme.primary,
                onClick = {
                    onMarkContent?.invoke("ALWAYS_MUSIC", false)
                    showMenu = false
                }
            )
            TempoDropdownMenuItem(
                title = stringResource(R.string.history_always_music_artist, item.artist),
                leadingIcon = Icons.Rounded.MusicNote,
                leadingIconTint = MaterialTheme.colorScheme.primary,
                enabled = !me.avinas.tempo.utils.ArtistParser.isUnknownArtist(item.artist),
                onClick = {
                    onMarkArtist?.invoke("ALWAYS_MUSIC", false)
                    showMenu = false
                }
            )

            TempoMenuDivider()

            TempoMenuKicker(text = stringResource(R.string.history_block_track))
            TempoDropdownMenuItem(
                title = stringResource(R.string.history_its_a_podcast),
                subtitle = stringResource(R.string.history_remove_and_block),
                leadingIcon = TempoIcons.Podcast,
                leadingIconTint = TempoCyan,
                onClick = {
                    onMarkContent?.invoke("PODCAST", true)
                    showMenu = false
                }
            )
            TempoDropdownMenuItem(
                title = stringResource(R.string.history_its_an_audiobook),
                subtitle = stringResource(R.string.history_remove_and_block),
                leadingIcon = TempoIcons.Audiobook,
                leadingIconTint = GoldenAmber,
                onClick = {
                    onMarkContent?.invoke("AUDIOBOOK", true)
                    showMenu = false
                }
            )
            TempoDropdownMenuItem(
                title = stringResource(R.string.history_video_non_music_track),
                subtitle = stringResource(R.string.history_video_non_music_track_description),
                leadingIcon = Icons.Default.PlayCircle,
                leadingIconTint = TempoError,
                isDestructive = true,
                onClick = {
                    onMarkContent?.invoke("NON_MUSIC", true)
                    showMenu = false
                }
            )

            TempoMenuDivider()

            TempoMenuKicker(
                text = stringResource(R.string.history_block_entire_artist),
                color = TempoError.copy(alpha = 0.8f)
            )
            TempoDropdownMenuItem(
                title = stringResource(R.string.history_artist_is_podcast, item.artist),
                subtitle = stringResource(R.string.history_remove_all_from_source),
                leadingIcon = TempoIcons.Podcast,
                leadingIconTint = TempoError,
                isDestructive = true,
                onClick = {
                    onMarkArtist?.invoke("PODCAST", true)
                    showMenu = false
                }
            )
            TempoDropdownMenuItem(
                title = stringResource(R.string.history_artist_is_audiobook, item.artist),
                subtitle = stringResource(R.string.history_remove_all_from_source),
                leadingIcon = TempoIcons.Audiobook,
                leadingIconTint = TempoError,
                isDestructive = true,
                onClick = {
                    onMarkArtist?.invoke("AUDIOBOOK", true)
                    showMenu = false
                }
            )
            TempoDropdownMenuItem(
                title = stringResource(R.string.history_video_non_music_artist, item.artist),
                subtitle = stringResource(R.string.history_video_non_music_artist_description),
                leadingIcon = Icons.Default.Close,
                leadingIconTint = TempoError,
                isDestructive = true,
                onClick = {
                    onMarkArtist?.invoke("NON_MUSIC", true)
                    showMenu = false
                }
            )

        }
    }
}

fun formatRelativeTime(timestamp: Long): String {
    val now = Instant.now()
    val time = Instant.ofEpochMilli(timestamp)
    val diff = ChronoUnit.MINUTES.between(time, now)

    return when {
        diff < 1 -> "Just now"
        diff < 60 -> "${diff}m ago"
        diff < 24 * 60 -> "${diff / 60}h ago"
        else -> {
            val date = time.atZone(ZoneId.systemDefault()).toLocalDate()
            val today = now.atZone(ZoneId.systemDefault()).toLocalDate()
            if (date.isEqual(today.minusDays(1))) "Yesterday"
            else date.format(RelativeDateFormatter)
        }
    }
}

@Composable
fun HistoryEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp, horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.history_empty_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.history_empty_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SearchEmptyState(query: String, onClear: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp, horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(GlassFrostSoft)
                    .border(1.dp, GlassBorderSoft, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No scrobbles found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "No matches for \"$query\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .premiumClickable(onClick = onClear)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.stats_search_clear),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
