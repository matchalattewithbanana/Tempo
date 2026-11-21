@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.avinas.tempo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Category navigation is informational, not a second grid of tall hero cards. */
data class TempoSettingsDestination(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val style: TempoEmblemStyle,
    val onClick: () -> Unit,
    val featured: Boolean = false,
    val tone: TempoTileTone = TempoTileTone.SKY,
)

/** Compact windows must keep search and category labels readable at full width. */
internal fun settingsBentoColumns(widthDp: Float, fontScale: Float): Int =
    if (widthDp < 600f || fontScale >= 1.3f) 1 else 2

@Composable
fun TempoSettingsBento(
    destinations: List<TempoSettingsDestination>,
    modifier: Modifier = Modifier,
) {
    if (destinations.isEmpty()) return
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columns = settingsBentoColumns(maxWidth.value, LocalDensity.current.fontScale)
        Column(
            modifier = Modifier.widthIn(max = 850.dp).fillMaxWidth().align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Keep source order stable. Previously the featured tile jumped out
            // of the category sequence and a bento forced some destinations to
            // occupy several hundred dp, pushing practical Settings offscreen.
            destinations.chunked(columns).forEach { group ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    group.forEach { destination ->
                        SettingsCategoryDestination(
                            destination = destination,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(columns - group.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryDestination(
    destination: TempoSettingsDestination,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.heightIn(min = 82.dp),
        onClick = destination.onClick,
        shape = MaterialTheme.shapes.large,
        color = if (destination.featured) scheme.secondaryContainer else scheme.surfaceContainerLow,
        contentColor = if (destination.featured) scheme.onSecondaryContainer else scheme.onSurface,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (destination.featured) scheme.onSecondaryContainer.copy(alpha = 0.10f)
                    else scheme.secondaryContainer,
                contentColor = if (destination.featured) scheme.onSecondaryContainer else scheme.onSecondaryContainer,
                modifier = Modifier.size(42.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(destination.icon, contentDescription = null, modifier = Modifier.size(22.dp))
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = destination.title,
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = destination.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (destination.featured) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
