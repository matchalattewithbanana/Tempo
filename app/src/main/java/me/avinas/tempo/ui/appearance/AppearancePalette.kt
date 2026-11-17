package me.avinas.tempo.ui.appearance

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Color

/**
 * Shared design tokens. These intentionally keep the legacy Abyss values unchanged.
 * The six presets support dark and opt-in beta light/system surface roles.
 * Some artwork-specific illustrations intentionally retain their design colors.
 */
data class AppearancePalette(
    val preset: AppearancePreset,
    val accent: Color,
    val onAccent: Color,
    val background: Color,
    val topBackground: Color,
    val surface: Color,
    val elevatedSurface: Color,
    val surfaceSunken: Color,
    val onSurface: Color,
    val mutedText: Color,
    val border: Color,
    val isDark: Boolean = true,
) {
    fun materialColorScheme(): ColorScheme = (if (isDark) darkColorScheme() else lightColorScheme()).copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = elevatedSurface,
        onPrimaryContainer = onSurface,
        secondary = accent,
        onSecondary = onAccent,
        secondaryContainer = elevatedSurface,
        onSecondaryContainer = onSurface,
        tertiary = accent,
        onTertiary = onAccent,
        background = background,
        onBackground = onSurface,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = elevatedSurface,
        onSurfaceVariant = mutedText,
        surfaceContainerLowest = surfaceSunken,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = elevatedSurface,
        surfaceContainerHighest = elevatedSurface,
        surfaceDim = surfaceSunken,
        surfaceBright = elevatedSurface,
        inverseSurface = onSurface,
        inverseOnSurface = background,
        inversePrimary = accent,
        surfaceTint = accent,
        outline = border,
        outlineVariant = border.copy(alpha = 0.55f),
    )
}

object AppearancePaletteResolver {
    fun forPreset(preset: AppearancePreset): AppearancePalette = when (preset) {
        AppearancePreset.EMBER -> AppearancePalette(
            preset = preset, accent = Color(0xFFFF91A3), onAccent = Color(0xFF300D18),
            background = Color(0xFF1C0D15), topBackground = Color(0xFF301822),
            surface = Color(0xFF28151E), elevatedSurface = Color(0xFF3A202B),
            surfaceSunken = Color(0xFF150910), onSurface = Color(0xFFFFEDF1),
            mutedText = Color(0xFFD0AEB8), border = Color(0xFF70414D),
        )
        AppearancePreset.SOLSTICE -> AppearancePalette(
            preset = preset, accent = Color(0xFFF0B47D), onAccent = Color(0xFF291508),
            background = Color(0xFF1B1510), topBackground = Color(0xFF30251C),
            surface = Color(0xFF2A2119), elevatedSurface = Color(0xFF3B2E22),
            surfaceSunken = Color(0xFF14100C), onSurface = Color(0xFFFFF3E4),
            mutedText = Color(0xFFCAB9A4), border = Color(0xFF705A42),
        )
        AppearancePreset.ABYSS -> AppearancePalette(
            preset = preset,
            accent = Color(0xFF2FDBB8), onAccent = Color(0xFF0A0E0E),
            background = Color(0xFF0A0E0E), topBackground = Color(0xFF101414),
            surface = Color(0xFF101414), elevatedSurface = Color(0xFF141818),
            surfaceSunken = Color(0xFF080A0A),
            onSurface = Color(0xFFE6EAEA), mutedText = Color(0xFF9AA2A2),
            border = Color(0xFF33403E),
        )
        AppearancePreset.OBSIDIAN_GOLD -> AppearancePalette(
            preset = preset,
            accent = Color(0xFFDDBA70), onAccent = Color(0xFF17110A),
            background = Color(0xFF050608), topBackground = Color(0xFF17191B),
            surface = Color(0xFF17191B), elevatedSurface = Color(0xFF242528),
            surfaceSunken = Color(0xFF050506),
            onSurface = Color(0xFFF0E7D2), mutedText = Color(0xFFAEA595),
            border = Color(0xFF65553B),
        )
        AppearancePreset.AURORA -> AppearancePalette(
            preset = preset,
            accent = Color(0xFFA78BFA), onAccent = Color(0xFF120B2B),
            background = Color(0xFF0F102A), topBackground = Color(0xFF211C40),
            surface = Color(0xFF20203C), elevatedSurface = Color(0xFF302A54),
            surfaceSunken = Color(0xFF0B0A20),
            onSurface = Color(0xFFF2ECFF), mutedText = Color(0xFFB7AFD2),
            border = Color(0xFF544776),
        )
        // Glacier Night can also resolve to the experimental Glacier Light palette.
        AppearancePreset.GLACIER -> AppearancePalette(
            preset = preset,
            accent = Color(0xFF72D0F2), onAccent = Color(0xFF082638),
            background = Color(0xFF0A1A27), topBackground = Color(0xFF173449),
            surface = Color(0xFF142A3C), elevatedSurface = Color(0xFF213F53),
            surfaceSunken = Color(0xFF07151F),
            onSurface = Color(0xFFF1F9FF), mutedText = Color(0xFFA9C5D7),
            border = Color(0xFF436478),
        )
    }

    fun resolve(settings: AppearanceSettings, systemDark: Boolean = true): AppearancePalette {
        val dark = when (settings.tone) {
            AppearanceTone.LIGHT -> false
            AppearanceTone.SYSTEM -> systemDark
            else -> true
        }
        val base = forPreset(settings.preset)
        if (settings.tone == AppearanceTone.OLED) return base.copy(
            background = Color.Black, topBackground = Color.Black, surfaceSunken = Color.Black,
        )
        if (dark) return base
        val warm = settings.preset == AppearancePreset.SOLSTICE || settings.preset == AppearancePreset.EMBER
        val accent = when (settings.preset) {
            AppearancePreset.ABYSS -> Color(0xFF006B57)
            AppearancePreset.OBSIDIAN_GOLD -> Color(0xFF73530B)
            AppearancePreset.AURORA -> Color(0xFF6541A5)
            AppearancePreset.GLACIER -> Color(0xFF16618C)
            AppearancePreset.EMBER -> Color(0xFFA22B48)
            AppearancePreset.SOLSTICE -> Color(0xFF994421)
        }
        return base.copy(
            accent = accent, onAccent = Color.White,
            background = if (warm) Color(0xFFFFF7EA) else Color(0xFFEFF6FC),
            topBackground = if (warm) Color(0xFFFFFBF4) else Color(0xFFF8FCFF),
            surface = if (warm) Color(0xFFFFFBF4) else Color(0xFFF8FCFF),
            elevatedSurface = if (warm) Color(0xFFF3E7D5) else Color(0xFFE1EDF5),
            surfaceSunken = if (warm) Color(0xFFF0E2CF) else Color(0xFFDCE8F1),
            onSurface = if (warm) Color(0xFF2B2118) else Color(0xFF172C3C),
            mutedText = if (warm) Color(0xFF61503F) else Color(0xFF455967),
            border = if (warm) Color(0xFF8A745D) else Color(0xFF647E91),
            isDark = false,
        )
    }

    /** Adjust decorative accent until it is readable against its actual surface. */
    fun withAccent(palette: AppearancePalette, candidate: Color): AppearancePalette {
        val opaque = candidate.copy(alpha = 1f)
        val target = if (palette.isDark) Color.White else Color.Black
        var accent = opaque
        for (step in 0..20) {
            accent = lerp(opaque, target, step / 20f)
            if (contrast(accent, palette.surface) >= 4.5f) break
        }
        val onAccent = if (contrast(Color.Black, accent) >= contrast(Color.White, accent)) Color.Black else Color.White
        return palette.copy(accent = accent, onAccent = onAccent)
    }

    private fun contrast(a: Color, b: Color): Float {
        val x = a.luminance(); val y = b.luminance()
        return (maxOf(x, y) + 0.05f) / (minOf(x, y) + 0.05f)
    }

}
