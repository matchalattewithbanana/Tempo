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

/*
 * Compatibility names retained for existing callers. Routine variants now map
 * to Material surface-container roles; only explicit caller-supplied colors or
 * shadows opt into custom artwork treatment. This keeps migration source-safe
 * without maintaining a second app-wide glass surface system.
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
    val scheme = MaterialTheme.colorScheme
    val appearance = LocalTempoAppearance.current
    val runtimePolicy = LocalAppearanceRuntimePolicy.current
    val palette = LocalTempoPalette.current
    val direction = LocalLayoutDirection.current
    val effectivePadding =
        if (appearance.density == AppearanceDensity.COMPACT && contentPadding == PaddingValues(16.dp)) {
            PaddingValues(
                start = contentPadding.calculateStartPadding(direction) * 0.75f,
                top = contentPadding.calculateTopPadding() * 0.75f,
                end = contentPadding.calculateEndPadding(direction) * 0.75f,
                bottom = contentPadding.calculateBottomPadding() * 0.75f,
            )
        } else {
            contentPadding
        }

    // Compatibility surface: old callers retain their contract while routine
    // app content resolves to stable Material tonal surfaces.
    val accent = accentColor ?: scheme.primary
    val color = when {
        backgroundColor != GlassFrostMedium && runtimePolicy.solidSurfaces && backgroundColor.alpha < 1f ->
            palette.surface
        backgroundColor != GlassFrostMedium -> backgroundColor
        variant == GlassCardVariant.Obsidian -> scheme.surfaceContainerLowest
        variant == GlassCardVariant.TintedSolid ->
            lerp(scheme.surfaceContainerHigh, accent, accentStrength.coerceIn(0.08f, 0.18f))
        variant == GlassCardVariant.HighProminence -> scheme.surfaceContainerHigh
        variant == GlassCardVariant.LowProminence || variant == GlassCardVariant.QuietGlass ->
            scheme.surfaceContainerLow
        else -> scheme.surfaceContainer
    }
    val border = when {
        borderColor != null -> BorderStroke(borderWidth ?: 1.dp, borderColor)
        borderWidth != null -> BorderStroke(borderWidth, scheme.outline)
        else -> null
    }
    val surfaceModifier =
        if (shadowElevation > 0.dp) {
            modifier.shadow(
                elevation = shadowElevation,
                shape = shape,
                ambientColor = Color.Transparent,
                spotColor = shadowSpotColor,
            )
        } else modifier

    Surface(
        modifier = surfaceModifier,
        shape = shape,
        color = color,
        contentColor = scheme.onSurface,
        border = border,
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
}

/**
 * Accessible Material icon button shared by all app bars and artwork overlays.
 * The outer click target is 48dp; custom container colors remain opt-in for
 * artwork overlay controls. Material ripple/focus/semantics come from Surface.
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
    val scheme = MaterialTheme.colorScheme
    val materialSurface = containerColor == GlassFrostMedium
    val color = if (materialSurface) scheme.surfaceContainerHigh else containerColor
    val foreground = if (materialSurface && iconTint == LocalTempoPalette.current.onSurface)
        scheme.onSurface else iconTint
    Surface(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        color = color,
        contentColor = foreground,
        border = if (borderColor == GlassBorderSoft) null
            else BorderStroke(1.dp, borderColor),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = foreground,
                modifier = Modifier.size(22.dp),
            )
        }
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
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Box(
            modifier = Modifier
                .width(10.dp)
                .height(0.8.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        Text(
            text = label.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.2.sp,
            maxLines = 2,
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
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
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
            maxLines = 2,
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
