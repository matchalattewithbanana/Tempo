@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.avinas.tempo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Independent music-first bento tiles, using Tempo's Material color roles. */
data class TempoSettingsDestination(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val style: TempoEmblemStyle,
    val onClick: () -> Unit,
)

/** Pure breakpoint policy; small phones and large-text users get full-width cards. */
internal fun settingsBentoColumns(widthDp: Float, fontScale: Float): Int =
    if (widthDp < 370f || fontScale >= 1.3f) 1 else 2

/**
 * A prominent Appearance Studio destination followed by Settings categories.
 * At accessibility font scales or on narrow phones, every destination becomes
 * a full-width row rather than an unreadable two-column card.
 */
@Composable
fun TempoSettingsBento(
    destinations: List<TempoSettingsDestination>,
    modifier: Modifier = Modifier,
) {
    if (destinations.isEmpty()) return
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val singleColumn = settingsBentoColumns(maxWidth.value, LocalDensity.current.fontScale) == 1
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TempoBentoDestination(
                destination = destinations.first(),
                hero = true,
                modifier = Modifier.fillMaxWidth().heightIn(min = 142.dp),
            )
            destinations.drop(1).chunked(if (singleColumn) 1 else 2).forEach { group ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    group.forEach { destination ->
                        TempoBentoDestination(
                            destination = destination,
                            hero = false,
                            modifier = Modifier.weight(1f)
                                .heightIn(min = if (singleColumn || group.size == 1) 104.dp else 164.dp),
                        )
                    }
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
    val (background, foreground, decorativeShape) = when (destination.style) {
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
        onClick = destination.onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = background,
        contentColor = foreground,
        tonalElevation = 0.dp,
    ) {
        Box(Modifier.fillMaxSize()) {
            // Low-contrast native polygon replaces a bitmap, looping spinner,
            // graphicsLayer blur or an uncacheable radial shader.
            Box(
                modifier = Modifier.align(Alignment.BottomEnd)
                    .offset(x = 28.dp, y = 26.dp)
                    .size(if (hero) 148.dp else 90.dp)
                    .background(foreground.copy(alpha = 0.085f), decorativeShape),
            )
            Column(
                modifier = Modifier.fillMaxSize()
                    .padding(horizontal = if (hero) 22.dp else 16.dp, vertical = 17.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier.size(if (hero) 48.dp else 38.dp)
                        .background(foreground.copy(alpha = 0.12f), decorativeShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = null,
                        modifier = Modifier.size(if (hero) 26.dp else 21.dp),
                        tint = foreground,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
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
                        color = foreground.copy(alpha = 0.82f),
                        maxLines = if (hero) 2 else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (hero) {
                Row(
                    modifier = Modifier.align(Alignment.TopEnd).padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy((-7).dp),
                ) {
                    listOf(scheme.primary, scheme.secondary, scheme.tertiary).forEach { color ->
                        Box(Modifier.size(19.dp)
                            .background(background, CircleShape)
                            .padding(2.dp)
                            .background(color, CircleShape))
                    }
                }
            }
        }
    }
}
