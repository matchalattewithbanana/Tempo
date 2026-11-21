package me.avinas.tempo.ui.stats

import me.avinas.tempo.ui.theme.TextOnAccent
import me.avinas.tempo.ui.theme.LocalTempoAppearance
import me.avinas.tempo.ui.components.TempoDropdownMenu
import me.avinas.tempo.ui.components.TempoDropdownMenuItem
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.avinas.tempo.data.repository.SortBy
import me.avinas.tempo.data.stats.CombinedBasicStats
import me.avinas.tempo.data.stats.TimeRange
import me.avinas.tempo.data.stats.TopAlbum
import me.avinas.tempo.data.stats.TopArtist
import me.avinas.tempo.data.stats.TopTrack
import me.avinas.tempo.data.stats.StatItem
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import me.avinas.tempo.R

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
internal fun StatsSearchField(
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
internal fun SearchEmptyState(query: String) {
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
internal fun StatsErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
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
internal fun itemRank(item: StatItem): Int? = when (item) {
    is TopTrack -> item.rank
    is TopArtist -> item.rank
    is TopAlbum -> item.rank
}

internal fun itemKey(item: StatItem): String = when (item) {
    is TopTrack -> "track_${item.trackId}"
    is TopArtist -> "artist_${item.artistId ?: item.artist}"
    is TopAlbum -> "album_${item.album}_${item.artist}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StatsTopBar(
    isScrolled: Boolean,
    hasItems: Boolean,
    isSearchActive: Boolean,
    onOpenSearch: () -> Unit,
    onOpenShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val headerColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isScrolled) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            Color.Transparent
        },
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "statsHeaderColor",
    )

    Surface(
        color = headerColor,
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
internal fun StatsPeriodSummary(totals: CombinedBasicStats) {
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
