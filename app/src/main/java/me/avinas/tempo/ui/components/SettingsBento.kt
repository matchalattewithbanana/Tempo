@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.avinas.tempo.ui.components

import androidx.compose.foundation.background
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

/** A single featured destination plus compact, scannable category rows. */
data class TempoSettingsDestination(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val style: TempoEmblemStyle,
    val onClick: () -> Unit,
    val featured: Boolean = false,
)

/** Don't squeeze two text-heavy Settings destinations onto a phone screen. */
internal fun settingsBentoColumns(widthDp: Float, fontScale: Float): Int =
    if (widthDp < 680f || fontScale >= 1.3f) 1 else 2

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
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            featured?.let {
                TempoBentoDestination(
                    destination = it,
                    hero = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            regular.chunked(columns).forEach { group ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    group.forEach { destination ->
                        TempoBentoDestination(
                            destination = destination,
                            hero = false,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (group.size < columns) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TempoBentoDestination(
    destination: TempoSettingsDestination,
    hero: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val (accentContainer, accentForeground, emblemShape) = when (destination.style) {
        TempoEmblemStyle.PULSE -> Triple(
            scheme.primaryContainer, scheme.onPrimaryContainer, MaterialShapes.Cookie9Sided.toShape(),
        )
        TempoEmblemStyle.DISCOVERY -> Triple(
            scheme.tertiaryContainer, scheme.onTertiaryContainer, MaterialShapes.Sunny.toShape(),
        )
        TempoEmblemStyle.MEMORY -> Triple(
            scheme.secondaryContainer, scheme.onSecondaryContainer, MaterialShapes.Clover4Leaf.toShape(),
        )
        TempoEmblemStyle.WARNING -> Triple(
            scheme.errorContainer, scheme.onErrorContainer, MaterialShapes.SoftBurst.toShape(),
        )
    }
    val foreground = if (hero) accentForeground else scheme.onSurface
    Surface(
        onClick = destination.onClick,
        modifier = modifier.heightIn(min = if (hero) 124.dp else 76.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = if (hero) accentContainer else scheme.surfaceContainerLow,
        contentColor = foreground,
    ) {
        Box(Modifier.fillMaxWidth()) {
            if (hero) {
                Box(
                    modifier = Modifier.align(Alignment.CenterEnd)
                        .offset(x = 34.dp, y = 28.dp)
                        .size(130.dp)
                        .background(accentForeground.copy(alpha = 0.07f), emblemShape),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = if (hero) 20.dp else 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(if (hero) 56.dp else 46.dp),
                    shape = emblemShape,
                    color = if (hero) accentForeground.copy(alpha = 0.12f) else accentContainer,
                    contentColor = accentForeground,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = null,
                            modifier = Modifier.size(if (hero) 28.dp else 23.dp),
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        destination.title,
                        style = if (hero) MaterialTheme.typography.titleLargeEmphasized
                            else MaterialTheme.typography.titleMediumEmphasized,
                        color = foreground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        destination.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hero) foreground.copy(alpha = 0.82f) else scheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = foreground.copy(alpha = 0.70f),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
