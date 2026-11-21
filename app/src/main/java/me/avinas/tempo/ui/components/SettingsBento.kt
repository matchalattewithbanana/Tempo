@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.avinas.tempo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.avinas.tempo.ui.theme.LocalTempoAppearance
import me.avinas.tempo.ui.appearance.AppearanceDensity

/** Tempo's original featured/staggered Settings mosaic, preserved from the shipped beta. */
data class TempoSettingsDestination(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val style: TempoEmblemStyle,
    val onClick: () -> Unit,
    val featured: Boolean = false,
    val tone: TempoTileTone = TempoTileTone.SKY,
)

/** Compact phones and accessibility text scales always get full-width cards. */
internal fun settingsBentoColumns(widthDp: Float, fontScale: Float): Int =
    if (widthDp < 360f || fontScale >= 1.3f) 1 else 2

/**
 * Original staggered Material bento. A featured full-width tile is followed
 * by one tall destination beside two compact ones and a final paired row.
 * All data, navigation actions and accessibility labels remain intact.
 */
@Composable
fun TempoSettingsBento(
    destinations: List<TempoSettingsDestination>,
    modifier: Modifier = Modifier,
) {
    if (destinations.isEmpty()) return
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columns = settingsBentoColumns(maxWidth.value, LocalDensity.current.fontScale)
        val featured = destinations.firstOrNull { it.featured }
        val regular = destinations.filterNot { it.featured }
        Column(
            modifier = Modifier.widthIn(max = 850.dp).fillMaxWidth().align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            featured?.let {
                TempoBentoDestination(
                    destination = it,
                    kind = BentoKind.FEATURED,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (columns == 1) {
                regular.forEach { destination ->
                    TempoBentoDestination(
                        destination = destination,
                        kind = BentoKind.SINGLE,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                if (regular.size >= 3) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        TempoBentoDestination(
                            destination = regular[0],
                            kind = BentoKind.TALL,
                            modifier = Modifier.weight(1f),
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            regular.subList(1, 3).forEach { destination ->
                                TempoBentoDestination(
                                    destination = destination,
                                    kind = BentoKind.SHORT,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
                val remaining = if (regular.size >= 3) regular.drop(3) else regular
                remaining.chunked(2).forEach { group ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        group.forEach { destination ->
                            TempoBentoDestination(
                                destination = destination,
                                kind = BentoKind.SQUARE,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (group.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private enum class BentoKind { FEATURED, TALL, SHORT, SQUARE, SINGLE }

@Composable
private fun TempoBentoDestination(
    destination: TempoSettingsDestination,
    kind: BentoKind,
    modifier: Modifier = Modifier,
) {
    val swatch = tempoTileSwatch(destination.tone)
    val dense = LocalTempoAppearance.current.density == AppearanceDensity.COMPACT
    val regularHeight = when (kind) {
        BentoKind.FEATURED -> 168.dp
        BentoKind.TALL -> 266.dp
        BentoKind.SHORT -> 128.dp
        BentoKind.SQUARE -> 168.dp
        BentoKind.SINGLE -> 104.dp
    }
    val height = if (dense) (regularHeight - 14.dp).coerceAtLeast(92.dp) else regularHeight
    Surface(
        onClick = destination.onClick,
        modifier = modifier.heightIn(min = height),
        shape = MaterialTheme.shapes.extraExtraLarge,
        // One featured destination earns the accent surface. Other destinations
        // use a quieter Material surface so hierarchy survives custom palettes.
        color = if (kind == BentoKind.FEATURED) swatch.container
            else MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Box(
            Modifier.fillMaxWidth().heightIn(min = height)
                .clip(MaterialTheme.shapes.extraExtraLarge),
        ) {
            when (kind) {
                BentoKind.SHORT, BentoKind.SINGLE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        BentoIcon(destination.icon, swatch, 40.dp)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            BentoLabel(destination.title, false, MaterialTheme.colorScheme.onSurface)
                            Text(
                                destination.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (kind == BentoKind.SINGLE) 2 else 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                else -> {
                    Column(
                        Modifier.fillMaxWidth().heightIn(min = height).padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        BentoIcon(
                            destination.icon,
                            swatch,
                            if (kind == BentoKind.FEATURED) 52.dp else 46.dp,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            BentoLabel(destination.title, kind == BentoKind.FEATURED, if (kind == BentoKind.FEATURED) swatch.foreground else MaterialTheme.colorScheme.onSurface)
                            Text(
                                destination.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (kind == BentoKind.FEATURED) swatch.foreground
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (kind == BentoKind.TALL) 4 else 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BentoLabel(title: String, featured: Boolean, foreground: androidx.compose.ui.graphics.Color) {
    Text(
        text = title,
        style = if (featured) MaterialTheme.typography.headlineSmallEmphasized
            else MaterialTheme.typography.titleMediumEmphasized,
        color = foreground,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun BentoIcon(
    icon: ImageVector,
    swatch: TempoTileSwatch,
    size: androidx.compose.ui.unit.Dp,
) {
    Surface(
        modifier = Modifier.size(size),
        shape = swatch.shape,
        color = swatch.iconContainer,
        contentColor = swatch.foreground,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(size * 0.47f))
        }
    }
}
