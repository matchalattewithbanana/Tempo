@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.avinas.tempo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Compact context strip for Settings subpages.
 *
 * Their TopAppBar already states the destination title. Repeating it as a
 * second enormous heading in a decorative hero wastes most of the first screen.
 * Keep the category identity through its Material shape and a short description.
 */
@Composable
fun TempoSettingsCategoryHero(
    title: String,
    subtitle: String,
    icon: ImageVector,
    style: TempoEmblemStyle,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val (container, foreground, emblemShape) = when (style) {
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
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = scheme.surfaceContainerLow,
        contentColor = scheme.onSurface,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                color = container,
                contentColor = foreground,
                shape = emblemShape,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(23.dp),
                    )
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
