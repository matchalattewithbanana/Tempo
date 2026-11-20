@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.avinas.tempo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Section identity for Settings pages, matching the bento entry tiles.
 * The large polygon remains static: opening a subpage should feel immediate,
 * not spend frames on another ambient spinner or alpha-heavy background.
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
    val (container, foreground, shape) = when (style) {
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
        color = container,
        contentColor = foreground,
    ) {
        Box(Modifier.fillMaxWidth().heightIn(min = 126.dp)) {
            Box(
                Modifier.align(Alignment.CenterEnd)
                    .offset(x = 30.dp)
                    .size(164.dp)
                    .background(foreground.copy(alpha = 0.075f), shape),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(66.dp),
                    shape = shape,
                    color = foreground.copy(alpha = 0.12f),
                    contentColor = foreground,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineSmallEmphasized,
                        color = foreground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = foreground.copy(alpha = 0.84f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
