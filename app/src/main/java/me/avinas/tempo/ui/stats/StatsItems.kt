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

    Surface(
        onClick = onNavigate,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = scheme.surfaceContainerHigh,
        contentColor = scheme.onSurface,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (LocalTempoAppearance.current.showStatsArtwork) {
                // Big Image
                Box(contentAlignment = Alignment.Center) {
                    // Artwork is the visual anchor; avoid a second glow layer.
                    if (imageUrl != null) {
                        CachedAsyncImage(
                            imageUrl = imageUrl,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp).clip(MaterialTheme.shapes.large),
                            contentScale = ContentScale.Crop,
                            targetSizeDp = 80 // Downsample for faster decode & less memory
                        )
                    } else {
                         Box(modifier = Modifier.size(80.dp).clip(MaterialTheme.shapes.large).background(scheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
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
fun RankedStatItem(rank: Int, item: StatItem, onClick: (StatItem) -> Unit) {
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
