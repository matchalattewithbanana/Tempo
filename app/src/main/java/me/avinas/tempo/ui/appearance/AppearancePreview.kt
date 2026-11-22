@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.avinas.tempo.ui.appearance

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.avinas.tempo.R
import me.avinas.tempo.ui.components.TempoLinearLoadingIndicator
import me.avinas.tempo.ui.components.appearanceBackdropBrush
import me.avinas.tempo.ui.theme.LocalTempoPalette
import me.avinas.tempo.ui.theme.TempoTheme

private enum class AppearancePreviewTarget { HOME, STATS, PROFILE }

@Composable
internal fun AppearancePreviewPanel(settings: AppearanceSettings) {
    var target by rememberSaveable { mutableStateOf(AppearancePreviewTarget.HOME) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppearancePreviewTarget.entries.forEach { item ->
                FilterChip(
                    selected = target == item,
                    onClick = { target = item },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    label = {
                        Text(
                            stringResource(
                                when (item) {
                                    AppearancePreviewTarget.HOME -> R.string.nav_home
                                    AppearancePreviewTarget.STATS -> R.string.nav_stats
                                    AppearancePreviewTarget.PROFILE -> R.string.settings_profile
                                },
                            ),
                            maxLines = 1,
                        )
                    },
                )
            }
        }

        TempoTheme(appearance = settings, preview = true) {
            val palette = LocalTempoPalette.current
            val brush = remember(palette, settings.backdrop) {
                appearanceBackdropBrush(palette, settings.backdrop)
            }
            Surface(shape = MaterialTheme.shapes.extraLarge, color = palette.background) {
                Column(
                    modifier = Modifier.fillMaxWidth().background(brush).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    when (target) {
                        AppearancePreviewTarget.HOME -> HomeAppearancePreview()
                        AppearancePreviewTarget.STATS -> StatsAppearancePreview()
                        AppearancePreviewTarget.PROFILE -> ProfileAppearancePreview()
                    }
                    NavigationAppearancePreview(
                        style = settings.navigationStyle,
                        selectedStats = target == AppearancePreviewTarget.STATS,
                    )
                    Text(
                        stringResource(R.string.appearance_preview_pending),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Lightweight, non-interactive navigation sample in Appearance Studio.
 * Keep the dock geometry recognizable without starting playback collectors,
 * creating a NavController, or installing click targets in a visual preview.
 */
@Composable
private fun NavigationAppearancePreview(
    style: AppearanceNavigationStyle,
    selectedStats: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val items = listOf(
        Triple(
            stringResource(R.string.nav_home),
            Icons.Rounded.Home to Icons.Outlined.Home,
            !selectedStats,
        ),
        Triple(
            stringResource(R.string.nav_stats),
            Icons.Rounded.Leaderboard to Icons.Outlined.Leaderboard,
            selectedStats,
        ),
        Triple(
            stringResource(R.string.nav_history),
            Icons.Rounded.History to Icons.Outlined.History,
            false,
        ),
    )
    Text(
        text = stringResource(R.string.appearance_navigation_style),
        style = MaterialTheme.typography.labelMedium,
        color = colors.onSurfaceVariant,
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = if (style == AppearanceNavigationStyle.BAR) RoundedCornerShape(8.dp)
            else MaterialTheme.shapes.extraExtraLarge,
        color = if (style == AppearanceNavigationStyle.BAR) colors.surfaceContainer
            else colors.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(
                horizontal = if (style == AppearanceNavigationStyle.EXPRESSIVE) 10.dp else 4.dp,
                vertical = 8.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { (label, icons, selected) ->
                val corner by androidx.compose.animation.core.animateDpAsState(
                    targetValue = if (style == AppearanceNavigationStyle.EXPRESSIVE && selected) 15.dp
                        else 24.dp,
                    animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                    label = "previewDockCorner",
                )
                val color by androidx.compose.animation.animateColorAsState(
                    targetValue = if (selected) colors.secondaryContainer else Color.Transparent,
                    animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
                    label = "previewDockIndicator",
                )
                Column(
                    modifier = Modifier.weight(1f).heightIn(min = 72.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier.size(if (style == AppearanceNavigationStyle.EXPRESSIVE) 48.dp else 52.dp)
                            .background(color, RoundedCornerShape(corner)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (selected) icons.first else icons.second,
                            contentDescription = null,
                            tint = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Text(
                        text = label,
                        style = if (selected) MaterialTheme.typography.labelLargeEmphasized
                            else MaterialTheme.typography.labelLarge,
                        color = if (selected) colors.onSurface else colors.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeAppearancePreview() {
    Text(
        stringResource(R.string.appearance_preview_song),
        style = MaterialTheme.typography.headlineSmallEmphasized,
    )
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.appearance_preview_artist),
                style = MaterialTheme.typography.titleMediumEmphasized,
            )
            TempoLinearLoadingIndicator(progress = { 0.64f }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PreviewMetric(R.string.period_today, "42", Modifier.weight(1f))
                PreviewMetric(R.string.nav_history, "18", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatsAppearancePreview() {
    Text(stringResource(R.string.nav_stats), style = MaterialTheme.typography.headlineSmallEmphasized)
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            repeat(3) { index ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.labelLargeEmphasized,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.appearance_preview_artist),
                            style = if (index == 0) MaterialTheme.typography.titleSmallEmphasized
                                else MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            stringResource(R.string.stats_sort_total_time),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (index < 2) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun ProfileAppearancePreview() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {}
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.settings_profile), style = MaterialTheme.typography.titleLargeEmphasized)
            Text(
                stringResource(R.string.profile_level_progress),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TempoLinearLoadingIndicator(progress = { 0.72f }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PreviewMetric(R.string.profile_badges, "12", Modifier.weight(1f))
                PreviewMetric(R.string.share_profile_challenges, "4", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PreviewMetric(label: Int, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, style = MaterialTheme.typography.titleMediumEmphasized)
            Text(
                stringResource(label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
