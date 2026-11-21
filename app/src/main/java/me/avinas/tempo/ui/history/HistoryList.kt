package me.avinas.tempo.ui.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material3.*
import me.avinas.tempo.ui.components.TempoCircularLoadingIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.launch
import me.avinas.tempo.R
import me.avinas.tempo.data.local.dao.HistoryItem
import me.avinas.tempo.ui.components.*
import me.avinas.tempo.ui.theme.*
import java.util.Locale

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
                        iconTint = MaterialTheme.colorScheme.secondary
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
                                color = MaterialTheme.colorScheme.secondary,
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
    totalTracks: Int,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = MaterialTheme.shapes.medium,
            color = scheme.secondaryContainer,
            contentColor = scheme.onSecondaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.history_from_archive),
                style = MaterialTheme.typography.titleSmallEmphasized,
                color = scheme.onSurface,
            )
            Text(
                text = "Showing $itemCount of $totalTracks archived tracks",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
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
