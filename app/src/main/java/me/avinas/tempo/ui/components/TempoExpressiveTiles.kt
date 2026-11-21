@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.avinas.tempo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.avinas.tempo.ui.theme.LocalTempoPalette
import me.avinas.tempo.ui.theme.LocalTempoAppearance
import me.avinas.tempo.ui.appearance.AppearanceDensity

/**
 * Tempo's own music-first tonal tiles, not a copied visual asset from another app.
 *
 * A multi-accent surface is deliberately different from a monochrome Material
 * card, but all text remains onSurface for legibility in Light, Dark, OLED,
 * wallpaper-derived and user-created palettes.
 */
enum class TempoTileTone { AQUA, SKY, LILAC, ROSE, AMBER }

data class TempoTileSwatch(
    val container: Color,
    val iconContainer: Color,
    val foreground: Color,
    val shape: Shape,
)

@Composable
fun tempoTileSwatch(tone: TempoTileTone): TempoTileSwatch {
    val scheme = MaterialTheme.colorScheme
    val dark = LocalTempoPalette.current.isDark
    val seed = when (tone) {
        TempoTileTone.AQUA -> Color(0xFF6FCEB5)
        TempoTileTone.SKY -> Color(0xFF83AFF1)
        TempoTileTone.LILAC -> Color(0xFFC29DE9)
        TempoTileTone.ROSE -> Color(0xFFE69DBD)
        TempoTileTone.AMBER -> Color(0xFFF3C079)
    }
    val shape = when (tone) {
        TempoTileTone.AQUA -> MaterialShapes.Clover4Leaf.toShape()
        TempoTileTone.SKY -> MaterialShapes.Cookie6Sided.toShape()
        TempoTileTone.LILAC -> MaterialShapes.Flower.toShape()
        TempoTileTone.ROSE -> MaterialShapes.Cookie9Sided.toShape()
        TempoTileTone.AMBER -> MaterialShapes.Sunny.toShape()
    }
    val base = scheme.surfaceContainerLow
    val container = lerp(base, seed, if (dark) 0.21f else 0.32f)
    val iconContainer = lerp(container, seed, if (dark) 0.24f else 0.36f)
    return TempoTileSwatch(container, iconContainer, scheme.onSurface, shape)
}

/** Shared, responsive metric tile used by Profile and listening analytics. */
@Composable
fun TempoMetricTile(
    value: String,
    label: String,
    icon: ImageVector,
    tone: TempoTileTone,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    minimumHeight: Dp = 144.dp,
    onClick: (() -> Unit)? = null,
) {
    val swatch = tempoTileSwatch(tone)
    val dense = LocalTempoAppearance.current.density == AppearanceDensity.COMPACT
    val tileHeight = if (dense) (minimumHeight - 20.dp).coerceAtLeast(108.dp) else minimumHeight
    Surface(
        modifier = modifier
            .heightIn(min = tileHeight)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        shape = MaterialTheme.shapes.extraLarge,
        color = swatch.container,
        contentColor = swatch.foreground,
    ) {
        Box(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge)) {
            Box(
                Modifier.align(Alignment.BottomEnd)
                    .offset(x = 33.dp, y = 37.dp)
                    .size(116.dp)
                    .background(swatch.foreground.copy(alpha = 0.055f), swatch.shape),
            )
            Column(
                Modifier.fillMaxWidth().padding(if (dense) 12.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    modifier = Modifier.size(if (dense) 38.dp else 42.dp),
                    shape = swatch.shape,
                    color = swatch.iconContainer,
                    contentColor = swatch.foreground,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(23.dp))
                    }
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    color = swatch.foreground,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = swatch.foreground,
                    maxLines = 2,
                )
                if (!supportingText.isNullOrBlank()) {
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.labelSmall,
                        color = swatch.foreground.copy(alpha = 0.8f),
                        maxLines = 2,
                    )
                }
            }
        }
    }
}
