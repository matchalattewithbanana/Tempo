package me.avinas.tempo.ui.navigation

import me.avinas.tempo.R
import me.avinas.tempo.ui.theme.TempoDarkSurface
import me.avinas.tempo.ui.theme.TempoDarkSurfaceElevated
import me.avinas.tempo.ui.theme.TextPrimary
import me.avinas.tempo.ui.theme.TextSecondary
import me.avinas.tempo.ui.theme.LocalTempoPalette
import me.avinas.tempo.ui.theme.LocalTempoAppearance
import me.avinas.tempo.ui.appearance.AppearanceNowListening
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

@Composable
fun TempoBottomNavigation(
    currentDestination: NavDestination?,
    onNavigateToHome: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalTempoPalette.current
    Column(modifier = modifier.fillMaxWidth()) {
        CompactNowPlayingStrip()
        Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(36.dp),
                ambientColor = Color.Transparent,
                spotColor = Color.Black.copy(alpha = 0.35f)
            )
            .height(72.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(
                // Vertical gradient fill
                Brush.verticalGradient(
                    colors = listOf(palette.elevatedSurface, palette.surface)
                )
            )
            .drawBehind {
                // Top border highlight line
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, palette.onSurface.copy(alpha = 0.10f), Color.Transparent)
                    ),
                    topLeft = Offset.Zero,
                    size = Size(size.width, 1.dp.toPx())
                )
            }
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        palette.border.copy(alpha = 0.65f),
                        palette.border.copy(alpha = 0.20f)
                    )
                ),
                shape = RoundedCornerShape(36.dp)
            )
    ) {
        // Glassmorphism overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.onSurface.copy(alpha = 0.02f))
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TempoNavItem(
                selected = currentDestination?.hierarchy?.any { it.route == Screen.Home.route } == true,
                onClick = onNavigateToHome,
                icon = Icons.Rounded.Home,
                unselectedIcon = Icons.Outlined.Home,
                label = stringResource(R.string.nav_home)
            )
            
            TempoNavItem(
                selected = currentDestination?.hierarchy?.any { it.route == Screen.Stats.route } == true,
                onClick = onNavigateToStats,
                icon = Icons.Rounded.Leaderboard,
                unselectedIcon = Icons.Outlined.Leaderboard,
                label = stringResource(R.string.nav_stats)
            )
            
            TempoNavItem(
                selected = currentDestination?.hierarchy?.any { it.route == Screen.History.route } == true,
                onClick = onNavigateToHistory,
                icon = Icons.Rounded.History,
                unselectedIcon = Icons.Outlined.History,
                label = stringResource(R.string.nav_history)
            )
        }
        }
    }
}

@Composable
private fun CompactNowPlayingStrip() {
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

@Composable
private fun TempoNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    unselectedIcon: ImageVector,
    label: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    val isSelected = selected

    val palette = LocalTempoPalette.current
    val reducedMotion = rememberReducedMotion()
    val selectedColor = palette.accent
    val unselectedColor = palette.mutedText

    val iconColor by animateColorAsState(
        targetValue = if (selected) selectedColor else unselectedColor,
        label = "iconColor"
    )

    // Separate selection bounce and touch-press animations
    val selectionScale by animateFloatAsState(
        targetValue = if (!reducedMotion && selected) 1.03f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "selectionScale"
    )
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (!reducedMotion && pressed) 0.88f else 1f,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "pressScale"
    )

    // Labels make destinations discoverable; selected icon/text color is the indicator.
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Scale press feedback instead of ripple
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .semantics {
                role = Role.Tab
                this.selected = isSelected
            }
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .scale(selectionScale * pressScale),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(
                imageVector = if (selected) icon else unselectedIcon,
                contentDescription = null, // Text supplies the accessible label.
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                color = iconColor,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }
}
