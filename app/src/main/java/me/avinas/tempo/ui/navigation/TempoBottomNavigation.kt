@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.avinas.tempo.ui.navigation

import me.avinas.tempo.R
import me.avinas.tempo.ui.theme.LocalTempoAppearance
import me.avinas.tempo.ui.appearance.AppearanceNowListening
import me.avinas.tempo.ui.appearance.AppearanceNavigationStyle
import me.avinas.tempo.ui.appearance.shouldShowCompactStrip
import me.avinas.tempo.service.SpotifyBroadcastPresence
import me.avinas.tempo.ui.components.AlbumArtImage
import me.avinas.tempo.ui.components.NowPlayingArtworkViewModel
import me.avinas.tempo.ui.components.NowPlayingArtworkIdentity
import androidx.hilt.navigation.compose.hiltViewModel

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy

import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi

/**
 * Stable Material navigation for the three top-level destinations.
 *
 * Destination labels stay visible as required by the Material navigation
 * contract. The compact now-playing strip remains independent so tracking
 * never waits on navigation or animation state.
 */
@Composable
fun TempoBottomNavigation(
    currentDestination: NavDestination?,
    onNavigateToHome: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val appearance = LocalTempoAppearance.current
    val floating = appearance.navigationStyle == AppearanceNavigationStyle.FLOATING
    val scheme = MaterialTheme.colorScheme
    val destinations = listOf(
        Triple(Screen.Home.route, Icons.Rounded.Home to Icons.Outlined.Home, stringResource(R.string.nav_home)),
        Triple(Screen.Stats.route, Icons.Rounded.Leaderboard to Icons.Outlined.Leaderboard, stringResource(R.string.nav_stats)),
        Triple(Screen.History.route, Icons.Rounded.History to Icons.Outlined.History, stringResource(R.string.nav_history)),
    )
    val actions = listOf(onNavigateToHome, onNavigateToStats, onNavigateToHistory)

    // A stable three-slot Material navigation bar avoids the experimental Short
    // Navigation Bar's disappearing sibling items on compact devices. No slot
    // depends on the selected destination's measured width or animation state.
    Column(modifier = modifier.fillMaxWidth()) {
        CompactNowPlayingStrip()
        Box(
            modifier = Modifier.fillMaxWidth().then(
                if (floating) {
                    Modifier.navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                } else {
                    Modifier
                },
            ),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = if (floating) Modifier.widthIn(max = 460.dp).fillMaxWidth()
                    else Modifier.fillMaxWidth(),
                shape = if (floating) {
                    MaterialTheme.shapes.extraExtraLarge
                } else {
                    androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
                },
                color = if (floating) scheme.surfaceContainerHigh else scheme.surfaceContainer,
            ) {
                androidx.compose.material3.NavigationBar(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    contentColor = scheme.onSurface,
                    windowInsets = if (floating) {
                        WindowInsets(0, 0, 0, 0)
                    } else {
                        androidx.compose.material3.NavigationBarDefaults.windowInsets
                    },
                    tonalElevation = 0.dp,
                ) {
                    Row(modifier = Modifier.fillMaxWidth().selectableGroup()) {
                        destinations.forEachIndexed { index, destination ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == destination.first
                        } == true
                        val selectedContent = if (selected) scheme.onSecondaryContainer else scheme.onSurfaceVariant
                        // Animate the indicator along its horizontal axis only. The
                        // theme's motion scheme becomes instant in reduced-motion mode.
                        val indicatorWidth by animateDpAsState(
                            targetValue = if (selected) 64.dp else 0.dp,
                            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                            label = "navigationIndicatorWidth",
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 80.dp)
                                .selectable(
                                    selected = selected,
                                    role = androidx.compose.ui.semantics.Role.Tab,
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        actions[index]()
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Box(
                                    modifier = Modifier.size(width = 64.dp, height = 32.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(indicatorWidth.coerceIn(0.dp, 64.dp))
                                            .height(32.dp)
                                            .clip(MaterialTheme.shapes.extraLarge)
                                            .background(scheme.secondaryContainer),
                                    )
                                    Icon(
                                        imageVector = if (selected) destination.second.first else destination.second.second,
                                        contentDescription = null,
                                        tint = selectedContent,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                                Text(
                                    text = destination.third,
                                    color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
                                    style = if (selected) MaterialTheme.typography.labelLargeEmphasized
                                        else MaterialTheme.typography.labelLarge,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun CompactNowPlayingStrip() {
    if (LocalTempoAppearance.current.nowListening != AppearanceNowListening.COMPACT) return
    val presence by SpotifyBroadcastPresence.presence.collectAsStateWithLifecycle()
    val current = presence ?: return
    val lifecycleOwner = LocalLifecycleOwner.current
    var checkedAtMs by remember(current.updatedAtMs) {
        mutableLongStateOf(System.currentTimeMillis())
    }
    // The strip is present on three destinations, but time checks must stop
    // while the activity is not visible. Playback tracking is unaffected.
    LaunchedEffect(lifecycleOwner, current.updatedAtMs) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            checkedAtMs = System.currentTimeMillis()
            while (true) {
                delay(60_000L)
                checkedAtMs = System.currentTimeMillis()
            }
        }
    }
    if (!shouldShowCompactStrip(
            AppearanceNowListening.COMPACT,
            current.title, current.updatedAtMs, checkedAtMs,
        )
    ) return

    val artworkViewModel: NowPlayingArtworkViewModel = hiltViewModel()
    val artwork by artworkViewModel.artwork.collectAsStateWithLifecycle()
    val choice = artwork?.takeIf {
        it.identity == NowPlayingArtworkIdentity(current.packageName, current.title, current.artist, current.album)
    }
    val preferredArtwork = current.albumArtUri ?: choice?.localUrl
    val scheme = MaterialTheme.colorScheme
    val iconEffects = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    val fresh = checkedAtMs - current.updatedAtMs <= 2 * 60_000L
    val playing = current.playing && fresh
    val color by animateColorAsState(
        targetValue = if (playing) scheme.primaryContainer else scheme.surfaceContainerHigh,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "compactPlaybackColor",
    )
    val foreground = if (playing) scheme.onPrimaryContainer else scheme.onSurface
    val supportingForeground = if (playing) scheme.onPrimaryContainer else scheme.onSurfaceVariant
    val stateLabel = when {
        !fresh -> stringResource(R.string.now_playing_last_detected)
        current.playing -> stringResource(R.string.now_playing_playing)
        else -> stringResource(R.string.now_playing_paused)
    }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = color,
        contentColor = foreground,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AlbumArtImage(
                albumArtUrl = preferredArtwork ?: choice?.remoteUrl,
                localArtUrl = if (preferredArtwork != null) choice?.remoteUrl else null,
                contentDescription = null,
                targetSizePx = 128,
                placeholderEmoji = "♫",
                modifier = Modifier.size(44.dp).clip(MaterialTheme.shapes.medium),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = current.title,
                    color = foreground,
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    text = current.artist,
                    color = supportingForeground,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    text = stateLabel,
                    color = supportingForeground,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
            Surface(
                modifier = Modifier.size(42.dp),
                shape = MaterialShapes.Cookie6Sided.toShape(),
                color = foreground.copy(alpha = 0.10f),
                contentColor = foreground,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AnimatedContent(
                        targetState = when {
                            playing -> 0
                            fresh -> 1
                            else -> 2
                        },
                        transitionSpec = {
                            fadeIn(animationSpec = iconEffects) togetherWith
                                fadeOut(animationSpec = iconEffects)
                        },
                        label = "compactPlaybackIcon",
                    ) { status ->
                        Icon(
                            imageVector = when (status) {
                                0 -> Icons.Rounded.GraphicEq
                                1 -> Icons.Rounded.PauseCircle
                                else -> Icons.Rounded.MusicNote
                            },
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}
