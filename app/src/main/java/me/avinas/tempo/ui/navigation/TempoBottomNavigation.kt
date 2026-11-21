package me.avinas.tempo.ui.navigation

import me.avinas.tempo.R
import me.avinas.tempo.ui.theme.TempoDarkSurface
import me.avinas.tempo.ui.theme.TempoDarkSurfaceElevated
import me.avinas.tempo.ui.theme.TextPrimary
import me.avinas.tempo.ui.theme.TextSecondary
import me.avinas.tempo.ui.theme.LocalTempoPalette
import me.avinas.tempo.ui.theme.LocalTempoAppearance
import me.avinas.tempo.ui.appearance.AppearanceNowListening
import me.avinas.tempo.ui.appearance.AppearanceNavigationStyle
import me.avinas.tempo.ui.appearance.shouldShowCompactStrip
import me.avinas.tempo.service.SpotifyBroadcastPresence
import me.avinas.tempo.ui.components.AlbumArtImage
import me.avinas.tempo.ui.components.NowPlayingArtworkViewModel
import me.avinas.tempo.ui.components.NowPlayingArtworkIdentity
import androidx.hilt.navigation.compose.hiltViewModel
import me.avinas.tempo.ui.theme.rememberReducedMotion

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.IconButtonDefaults

/**
 * M3 Expressive short navigation: one accessible destination component handles
 * selection, motion, large-font sizing, TalkBack, and Material ripple feedback.
 * The compact now-playing strip remains independent so tracking never waits on
 * an animation or navigation transition.
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
    val showLabels = LocalTempoAppearance.current.showNavigationLabels
    val destinations = listOf(
        Triple(Screen.Home.route, Icons.Rounded.Home to Icons.Outlined.Home, stringResource(R.string.nav_home)),
        Triple(Screen.Stats.route, Icons.Rounded.Leaderboard to Icons.Outlined.Leaderboard, stringResource(R.string.nav_stats)),
        Triple(Screen.History.route, Icons.Rounded.History to Icons.Outlined.History, stringResource(R.string.nav_history)),
    )
    val actions = listOf(onNavigateToHome, onNavigateToStats, onNavigateToHistory)
    Column(modifier = modifier.fillMaxWidth()) {
        CompactNowPlayingStrip()
        if (LocalTempoAppearance.current.navigationStyle == AppearanceNavigationStyle.FLOATING) {
            Box(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                HorizontalFloatingToolbar(
                    expanded = true,
                    colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
                        toolbarContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ) {
                    destinations.forEachIndexed { index, destination ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == destination.first
                        } == true
                        FilledIconToggleButton(
                            checked = selected,
                            onCheckedChange = {
                                if (!selected) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    actions[index]()
                                }
                            },
                            modifier = Modifier.size(
                                width = if (showLabels) 80.dp else 56.dp,
                                height = 62.dp,
                            ),
                            shapes = IconButtonDefaults.toggleableShapes(),
                            colors = IconButtonDefaults.filledIconToggleButtonColors(
                                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Icon(
                                    imageVector = if (selected) destination.second.first
                                        else destination.second.second,
                                    contentDescription = if (showLabels) null else destination.third,
                                    modifier = Modifier.size(23.dp),
                                )
                                if (showLabels) Text(
                                    destination.third,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        } else {
        // Native Material navigation stays compact and horizontally weighted.
        // The ShortNavigationBar layout in the reported screenshot inflated
        // the selected Home destination and obscured page content.
        NavigationBar(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            destinations.forEachIndexed { index, destination ->
                val selected = currentDestination?.hierarchy?.any {
                    it.route == destination.first
                } == true
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        if (!selected) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            actions[index]()
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = if (selected) destination.second.first
                                else destination.second.second,
                            contentDescription = if (showLabels) null else destination.third,
                        )
                    },
                    label = if (showLabels) {
                        { Text(destination.third, maxLines = 1) }
                    } else null,
                    alwaysShowLabel = showLabels,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
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
    val fresh = checkedAtMs - current.updatedAtMs <= 2 * 60_000L
    val playing = current.playing && fresh
    val color by animateColorAsState(
        targetValue = if (playing) scheme.primaryContainer else scheme.surfaceContainerHigh,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "compactPlaybackColor",
    )
    val foreground = if (playing) scheme.onPrimaryContainer else scheme.onSurface
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
                    color = foreground.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    text = stateLabel,
                    color = foreground.copy(alpha = 0.84f),
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
                    Icon(
                        imageVector = when {
                            playing -> Icons.Rounded.GraphicEq
                            fresh -> Icons.Rounded.PauseCircle
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
