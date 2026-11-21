package me.avinas.tempo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import me.avinas.tempo.ui.theme.DisplayFontFamily
import me.avinas.tempo.ui.theme.KickerSmall
import me.avinas.tempo.ui.theme.TextTertiary
import java.util.Locale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.avinas.tempo.ui.theme.GlassBorderMedium
import me.avinas.tempo.ui.theme.GlassBorderSoft
import me.avinas.tempo.ui.theme.GlassFrostMedium
import me.avinas.tempo.ui.theme.GlassFrostSoft
import me.avinas.tempo.ui.theme.GlassHighlightTop
import me.avinas.tempo.ui.theme.GlassShadowTeal
import me.avinas.tempo.ui.theme.GlassTintTeal
import me.avinas.tempo.ui.theme.LocalTempoPalette
import me.avinas.tempo.ui.appearance.*
import me.avinas.tempo.ui.theme.LocalTempoAppearance
import androidx.compose.ui.platform.LocalLayoutDirection
import me.avinas.tempo.ui.theme.TextPrimary
import me.avinas.tempo.ui.theme.premiumClickable
import me.avinas.tempo.ui.theme.TempoDarkSurface
import me.avinas.tempo.ui.theme.TempoDarkSurfaceElevated
import me.avinas.tempo.ui.theme.TempoDarkSurfaceSunken
import me.avinas.tempo.ui.theme.TempoPrimary

/* GlassCard surface roles (one per job, never mixed):
 *  - Obsidian     solid near-black anchor — structural stat blocks.
 *  - QuietGlass   ~90% opaque glass + ≤5% accent — analytic cards that must
 *                 stay legible over the art atmosphere.
 *  - TintedSolid  opaque dark + ~12% pastel tint — identity/emotion cards.
 *  - High/LowProminence — legacy translucent frost for the rest of the app.
 */

enum class GlassCardVariant {
    HighProminence,
    LowProminence,
    Obsidian,
    QuietGlass,
    TintedSolid,
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = appearanceCardShape(),
    backgroundColor: Color = GlassFrostMedium,
    accentColor: Color? = null,
    accentStrength: Float = 0.07f,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    variant: GlassCardVariant = GlassCardVariant.HighProminence,
    contentAlignment: androidx.compose.ui.Alignment = androidx.compose.ui.Alignment.CenterStart,
    borderColor: Color? = null,
    borderWidth: Dp? = null,
    shadowElevation: Dp = 0.dp,
    shadowSpotColor: Color = GlassShadowTeal,
    fillMaxWidth: Boolean = true,
    content: @Composable () -> Unit,
) {
    val palette = LocalTempoPalette.current
    val appearance = LocalTempoAppearance.current
    val policy = LocalAppearanceRuntimePolicy.current
    val direction = LocalLayoutDirection.current
    val effectivePadding = if (appearance.density == AppearanceDensity.COMPACT && contentPadding == PaddingValues(16.dp)) PaddingValues(
        start = contentPadding.calculateStartPadding(direction) * 0.75f,
        top = contentPadding.calculateTopPadding() * 0.75f,
        end = contentPadding.calculateEndPadding(direction) * 0.75f,
        bottom = contentPadding.calculateBottomPadding() * 0.75f,
    ) else contentPadding
    // App-wide M3 migration: all unaccented High/LowProminence cards are native
    // tonal surfaces, rather than screen-specific gradients or drawn borders.
    // Explicit artwork backgrounds, custom accents and the deliberately styled
    // Obsidian/QuietGlass variants retain their original visual treatment.
    // Most Home, Stats, History, Profile, detail and Settings cards share this path.
    if ((variant == GlassCardVariant.LowProminence || variant == GlassCardVariant.HighProminence) &&
        backgroundColor == GlassFrostMedium &&
        accentColor == null &&
        borderColor == null &&
        borderWidth == null &&
        shadowElevation == 0.dp
    ) {
        Surface(
            modifier = modifier,
            shape = shape,
            color = when (variant) {
                GlassCardVariant.HighProminence -> MaterialTheme.colorScheme.surfaceContainerHigh
                else -> MaterialTheme.colorScheme.surfaceContainerLow
            },
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Box(
                modifier = Modifier
                    .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier)
                    .padding(effectivePadding),
                contentAlignment = contentAlignment,
            ) {
                content()
            }
        }
        return
    }

    val customPalette = palette != AppearancePaletteResolver.forPreset(AppearancePreset.ABYSS)
    val accent = accentColor ?: palette.accent

    val surface =
        remember(backgroundColor, accentColor, accentStrength, variant, palette, policy.solidSurfaces) {
            when {
                // Solid finish removes translucent layers but preserves explicit opaque designs.
                policy.solidSurfaces && backgroundColor.alpha < 1f -> palette.surface
                // Explicit surface wins; accent then only tints the border.
                backgroundColor != GlassFrostMedium -> backgroundColor

                customPalette && variant == GlassCardVariant.Obsidian -> palette.surfaceSunken
                customPalette && variant == GlassCardVariant.QuietGlass -> palette.surface
                customPalette && variant == GlassCardVariant.TintedSolid ->
                    lerp(palette.surface, accent, 0.12f)
                customPalette -> palette.elevatedSurface

                variant == GlassCardVariant.Obsidian -> TempoDarkSurfaceSunken

                variant == GlassCardVariant.QuietGlass ->
                    lerp(TempoDarkSurfaceElevated, accent, 0.05f).copy(alpha = 0.88f)

                variant == GlassCardVariant.TintedSolid ->
                    lerp(TempoDarkSurface, accent, 0.12f)

                accentColor != null -> accentColor.copy(alpha = accentStrength)

                variant == GlassCardVariant.HighProminence -> GlassFrostMedium

                else -> GlassFrostSoft
            }
        }

    val isTinted = accentColor != null || backgroundColor != GlassFrostMedium

    val borderBrush =
        remember(variant, borderColor, accentColor, isTinted, palette) {
            when {
                borderColor != null -> {
                    null
                }

                customPalette -> Brush.verticalGradient(
                    colors = listOf(
                        palette.border.copy(alpha = 0.65f),
                        palette.border.copy(alpha = 0.28f),
                    ),
                )

                variant == GlassCardVariant.Obsidian -> {
                    Brush.verticalGradient(
                        colors = listOf(GlassBorderMedium, GlassBorderSoft),
                    )
                }

                variant == GlassCardVariant.QuietGlass -> {
                    Brush.verticalGradient(
                        colors = listOf(accent.copy(alpha = 0.14f), accent.copy(alpha = 0.04f)),
                    )
                }

                variant == GlassCardVariant.TintedSolid -> {
                    Brush.verticalGradient(
                        colors = listOf(accent.copy(alpha = 0.30f), accent.copy(alpha = 0.10f)),
                    )
                }

                accentColor != null -> {
                    Brush.verticalGradient(
                        colors = listOf(accentColor.copy(alpha = 0.20f), accentColor.copy(alpha = 0.06f)),
                    )
                }

                isTinted && variant == GlassCardVariant.HighProminence -> {
                    Brush.verticalGradient(
                        colors = listOf(GlassBorderMedium, GlassBorderSoft),
                    )
                }

                isTinted -> {
                    Brush.verticalGradient(
                        colors = listOf(GlassBorderSoft, GlassBorderSoft),
                    )
                }

                variant == GlassCardVariant.HighProminence -> {
                    Brush.verticalGradient(
                        colors = listOf(Color(0x14E8EEEC), Color(0x08E8EEEC)),
                    )
                }

                else -> {
                    Brush.verticalGradient(
                        colors = listOf(Color(0x0DE8EEEC), Color(0x0DE8EEEC)),
                    )
                }
            }
        }

    val borderStroke =
        remember(variant, borderColor, borderWidth, borderBrush) {
            val width = borderWidth ?: (if (variant == GlassCardVariant.HighProminence) 1.dp else 0.5.dp)
            if (borderColor != null) {
                BorderStroke(width, borderColor)
            } else {
                BorderStroke(width, borderBrush!!)
            }
        }

    val bgBrush =
        remember(surface, variant, accentColor, palette, policy.solidSurfaces) {
            val bottom = when {
                policy.solidSurfaces -> surface
                customPalette -> palette.surfaceSunken
                variant == GlassCardVariant.QuietGlass -> TempoDarkSurfaceSunken.copy(alpha = 0.92f)
                variant == GlassCardVariant.TintedSolid -> lerp(TempoDarkSurfaceSunken, accent, 0.07f)
                variant == GlassCardVariant.Obsidian -> Color(0xFF060808)
                else -> GlassTintTeal.copy(alpha = 0.32f)
            }
            Brush.verticalGradient(
                colors = listOf(surface, bottom),
            )
        }

    val showsSheen = variant == GlassCardVariant.HighProminence ||
        variant == GlassCardVariant.Obsidian ||
        variant == GlassCardVariant.TintedSolid

    Box(
        modifier =
            modifier
                .shadow(elevation = if (policy.solidSurfaces) 0.dp else shadowElevation, shape = shape, ambientColor = Color.Transparent, spotColor = shadowSpotColor)
                .clip(shape)
                .background(bgBrush, shape)
                .drawBehind {
                    // Top sheen drawn in the same pass — no nested layout node
                    if (showsSheen && !policy.solidSurfaces) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, GlassHighlightTop, Color.Transparent),
                            ),
                            topLeft = Offset.Zero,
                            size = Size(size.width, 1.dp.toPx()),
                        )
                    }
                }
                .border(borderStroke, shape = shape),
        contentAlignment = contentAlignment,
    ) {
        Box(
            modifier =
                Modifier
                    .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier)
                    .padding(effectivePadding),
        ) {
            content()
        }
    }
}

/**
 * Shared frosted-glass circular icon button for top bars — the single spec
 * every screen floating over artwork/scrolling content must use:
 * 40dp circle, GlassFrostMedium fill, 0.8dp GlassBorderSoft border, 18dp icon.
 */
@Composable
fun FrostedIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = LocalTempoPalette.current.onSurface,
    containerColor: Color = GlassFrostMedium,
    borderColor: Color = GlassBorderSoft,
) {
    val palette = LocalTempoPalette.current
    // Theme-aware defaults keep icon controls readable over pale glass.
    // Explicit caller colors are preserved for artwork overlays.
    val effectiveContainer = if (!palette.isDark && containerColor == GlassFrostMedium)
        palette.elevatedSurface else containerColor
    val effectiveBorder = if (!palette.isDark && borderColor == GlassBorderSoft)
        palette.border.copy(alpha = 0.75f) else borderColor
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(effectiveContainer)
            .border(0.8.dp, effectiveBorder, CircleShape)
            .premiumClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * Shared catalog kicker for section indexing across Tempo screens.
 * Displays bold number, fine horizontal hairline, and uppercase tracked label.
 */
@Composable
fun SectionCatalogKicker(
    number: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val palette = LocalTempoPalette.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = number,
            style = KickerSmall,
            fontWeight = FontWeight.Bold,
            color = palette.onSurface,
        )
        Box(
            modifier = Modifier
                .width(10.dp)
                .height(0.8.dp)
                .background(GlassBorderMedium),
        )
        Text(
            text = label.uppercase(Locale.getDefault()),
            style = KickerSmall,
            color = palette.mutedText,
            letterSpacing = 1.2.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Shared telemetry cell for structured stat breakdowns.
 * Kicker label over display-family metric value.
 */
@Composable
fun TelemetryMiniCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = LocalTempoPalette.current.onSurface,
) {
    val palette = LocalTempoPalette.current
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label.uppercase(Locale.getDefault()),
            style = KickerSmall,
            color = palette.mutedText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.Bold,
            ),
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Shared cards use user-selected corners; components with explicit silhouettes keep them. */
@Composable
fun appearanceCardShape(): RoundedCornerShape = RoundedCornerShape(
    when (LocalTempoAppearance.current.shape) {
        AppearanceShape.SOFT -> 20.dp
        AppearanceShape.MODERATE -> 12.dp
        AppearanceShape.SHARP -> 4.dp
    }
)
