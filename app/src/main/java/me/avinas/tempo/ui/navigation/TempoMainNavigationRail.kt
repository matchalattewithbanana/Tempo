@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.avinas.tempo.ui.navigation

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import me.avinas.tempo.R

/**
 * Collapsed expressive navigation rail for expanded window layouts.
 * No separate route state: selection is derived directly from NavController.
 */
@Composable
internal fun TempoMainNavigationRail(
    currentDestination: NavDestination?,
    onNavigateToHome: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    data class Destination(
        val route: String,
        val selectedIcon: ImageVector,
        val unselectedIcon: ImageVector,
        val label: String,
        val onClick: () -> Unit,
    )
    val destinations = listOf(
        Destination(Screen.Home.route, Icons.Rounded.Home, Icons.Outlined.Home,
            stringResource(R.string.nav_home), onNavigateToHome),
        Destination(Screen.Stats.route, Icons.Rounded.Leaderboard, Icons.Outlined.Leaderboard,
            stringResource(R.string.nav_stats), onNavigateToStats),
        Destination(Screen.History.route, Icons.Rounded.History, Icons.Outlined.History,
            stringResource(R.string.nav_history), onNavigateToHistory),
    )
    WideNavigationRail(modifier = modifier.fillMaxHeight()) {
        destinations.forEach { destination ->
            val selected = currentDestination?.hierarchy?.any {
                it.route == destination.route
            } == true
            WideNavigationRailItem(
                railExpanded = false,
                selected = selected,
                onClick = {
                    if (!selected) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        destination.onClick()
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = null,
                    )
                },
                label = {
                    Text(
                        destination.label,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 2,
                    )
                },
            )
        }
    }
}
