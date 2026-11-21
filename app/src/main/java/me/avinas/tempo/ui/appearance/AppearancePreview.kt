@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.avinas.tempo.ui.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
