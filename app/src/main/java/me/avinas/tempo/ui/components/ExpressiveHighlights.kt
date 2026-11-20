package me.avinas.tempo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tempo-owned editorial accent system, inspired by the Material 3 Expressive
 * visual language. Shapes are native Material polygons, not raster overlays.
 * Keeping these decorative surfaces static avoids continuous animations in lists.
 */
enum class TempoEmblemStyle { PULSE, DISCOVERY, MEMORY, WARNING }

@Composable
fun TempoEmblem(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    style: TempoEmblemStyle = TempoEmblemStyle.PULSE,
) {
    val colors = MaterialTheme.colorScheme
    val (shape, background, foreground) = when (style) {
        TempoEmblemStyle.PULSE -> Triple(
            MaterialShapes.Cookie9Sided.toShape(),
            colors.primaryContainer, colors.onPrimaryContainer,
        )
        TempoEmblemStyle.DISCOVERY -> Triple(
            MaterialShapes.Sunny.toShape(),
            colors.tertiaryContainer, colors.onTertiaryContainer,
        )
        TempoEmblemStyle.MEMORY -> Triple(
            MaterialShapes.Clover4Leaf.toShape(),
            colors.secondaryContainer, colors.onSecondaryContainer,
        )
        TempoEmblemStyle.WARNING -> Triple(
            MaterialShapes.SoftBurst.toShape(),
            colors.errorContainer, colors.onErrorContainer,
        )
    }
    Surface(
        modifier = modifier.size(size),
        shape = shape,
        color = background,
        contentColor = foreground,
        tonalElevation = 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon, contentDescription = null,
                modifier = Modifier.size(size * 0.42f),
                tint = foreground,
            )
        }
    }
}

/** A reusable hierarchy for listening insights and collections.
 * The optional action is a real Material button with TalkBack labeling.
 */
@Composable
fun TempoSectionHeading(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    style: TempoEmblemStyle = TempoEmblemStyle.PULSE,
    actionDescription: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TempoEmblem(icon = icon, size = 48.dp, style = style)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLargeEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (onAction != null && !actionDescription.isNullOrBlank()) {
            FilledTonalIconButton(
                onClick = onAction,
                shapes = IconButtonDefaults.shapes(),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, actionDescription)
            }
        }
    }
}
