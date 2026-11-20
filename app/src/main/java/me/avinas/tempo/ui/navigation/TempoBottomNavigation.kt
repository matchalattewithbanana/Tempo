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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy

import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarArrangement
import androidx.compose.material3.NavigationItemIconPosition

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
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val wide = maxWidth >= 600.dp
            ShortNavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (wide) 28.dp else 14.dp, vertical = 8.dp)
                    .clip(MaterialTheme.shapes.extraLarge),
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
                arrangement = if (wide) ShortNavigationBarArrangement.Centered
                    else ShortNavigationBarArrangement.EqualWeight,
            ) {
                destinations.forEachIndexed { index, destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.first } == true
                    ShortNavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                actions[index]()
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) destination.second.first else destination.second.second,
                                contentDescription = if (showLabels) null else destination.third,
                            )
                        },
                        label = { if (showLabels) Text(destination.third, maxLines = 1) },
                        iconPosition = if (wide) NavigationItemIconPosition.Start
                            else NavigationItemIconPosition.Top,
                    )
                }
            }
        }
        }
    }
}

@Composable
internal fun CompactNowPlayingStrip() {
    if (LocalTempoAppearance.current.nowListening != AppearanceNowListening.COMPACT) return
    val presence by SpotifyBroadcastPresence.presence.collectAsStateWithLifecycle()
    val current = presence ?: return
    var checkedAtMs by remember(current.updatedAtMs) {
        mutableLongStateOf(System.currentTimeMillis())
    }
    LaunchedEffect(current.updatedAtMs) {
        while (true) {
            delay(60_000L)
            checkedAtMs = System.currentTimeMillis()
        }
    }
    // A remote or paused snapshot can outlive the playback session; never show
    // the mini-player indefinitely just because it was the last observed track.
    if (!shouldShowCompactStrip(AppearanceNowListening.COMPACT, current.title, current.updatedAtMs, checkedAtMs)) return

    val palette = LocalTempoPalette.current
    val artworkViewModel: NowPlayingArtworkViewModel = hiltViewModel()
    val artwork by artworkViewModel.artwork.collectAsStateWithLifecycle()
    val choice = artwork?.takeIf {
        it.identity == NowPlayingArtworkIdentity(current.packageName, current.title, current.artist, current.album)
    }
    val preferredArtwork = current.albumArtUri ?: choice?.localUrl
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(palette.elevatedSurface)
            .border(1.dp, palette.border.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AlbumArtImage(
            albumArtUrl = preferredArtwork ?: choice?.remoteUrl,
            localArtUrl = if (preferredArtwork != null) choice?.remoteUrl else null,
            contentDescription = null,
            targetSizePx = 128,
            placeholderEmoji = "♫",
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = current.title,
                color = palette.onSurface,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Text(
                text = current.artist,
                color = palette.mutedText,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
        if (checkedAtMs - current.updatedAtMs > 2 * 60_000L) {
            Text(
                text = stringResource(R.string.now_playing_last_detected),
                color = palette.mutedText,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        } else {
            Box(
                modifier = Modifier.size(7.dp).background(
                    if (current.playing) palette.accent else palette.mutedText,
                    CircleShape,
                ),
            )
        }
    }
}

