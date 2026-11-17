package me.avinas.tempo.ui.journal

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.avinas.tempo.ui.appearance.AppearanceShape
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * The opt-in Listening Journal v2 visual foundation.
 *
 * Intentionally isolated from the existing TempoTheme: no current screens
 * change appearance until the user approves rendered reference screenshots.
 * These are semantic roles, not a set of arbitrary per-screen accent colors.
 */
internal enum class JournalTone { INK, PAPER, OLED }

internal data class JournalPalette(
    val canvas: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val onCanvas: Color,
    val onSurface: Color,
    val supportingText: Color,
    val faintText: Color,
    val accent: Color,
    val onAccent: Color,
    val separator: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val chartPrimary: Color,
    val chartSecondary: Color,
    val chartTertiary: Color,
)

/** Design directions are separate from existing user-defined Appearance presets. */
internal object JournalPalettes {
    val ink = JournalPalette(
        canvas = Color(0xFF101013),
        surface = Color(0xFF19191E),
        surfaceRaised = Color(0xFF25242A),
        onCanvas = Color(0xFFF5F0EA),
        onSurface = Color(0xFFF5F0EA),
        supportingText = Color(0xFFC6BDC0),
        faintText = Color(0xFFB2AAB0),
        accent = Color(0xFFE4B48A),
        onAccent = Color(0xFF27170D),
        separator = Color(0xFF49444B),
        success = Color(0xFF9DDDC0),
        warning = Color(0xFFEAC67F),
        error = Color(0xFFFFA9A8),
        chartPrimary = Color(0xFFE4B48A),
        chartSecondary = Color(0xFF9BBEC8),
        chartTertiary = Color(0xFFC8AED3),
    )

    val paper = JournalPalette(
        canvas = Color(0xFFFBF8F3),
        surface = Color(0xFFFFFFFF),
        surfaceRaised = Color(0xFFF1EBE4),
        onCanvas = Color(0xFF241D1D),
        onSurface = Color(0xFF241D1D),
        supportingText = Color(0xFF625B5A),
        faintText = Color(0xFF6C6566),
        accent = Color(0xFF82522F),
        onAccent = Color(0xFFFFFFFF),
        separator = Color(0xFFB6ADA7),
        success = Color(0xFF286E51),
        warning = Color(0xFF8B5C12),
        error = Color(0xFF9C3033),
        chartPrimary = Color(0xFF82522F),
        chartSecondary = Color(0xFF3A6877),
        chartTertiary = Color(0xFF815B89),
    )

    val oled = ink.copy(
        canvas = Color.Black,
        surface = Color(0xFF121216),
        surfaceRaised = Color(0xFF242329),
    )

    fun forTone(tone: JournalTone): JournalPalette = when (tone) {
        JournalTone.INK -> ink
        JournalTone.PAPER -> paper
        JournalTone.OLED -> oled
    }
}

internal object JournalSpace {
    val micro: Dp = 4.dp
    val small: Dp = 8.dp
    val medium: Dp = 12.dp
    val standard: Dp = 16.dp
    val section: Dp = 24.dp
    val large: Dp = 32.dp
}

/**
 * One shape taxonomy respected by all new components, derived from existing
 * saved shape preferences so the Journal v2 never silently discards user choices.
 */
internal fun journalCardShape(shape: AppearanceShape): RoundedCornerShape = when (shape) {
    AppearanceShape.SHARP -> RoundedCornerShape(8.dp)
    AppearanceShape.MODERATE -> RoundedCornerShape(16.dp)
    AppearanceShape.SOFT -> RoundedCornerShape(22.dp)
}

internal fun journalChipShape(shape: AppearanceShape): RoundedCornerShape = when (shape) {
    AppearanceShape.SHARP -> RoundedCornerShape(6.dp)
    AppearanceShape.MODERATE -> RoundedCornerShape(12.dp)
    AppearanceShape.SOFT -> RoundedCornerShape(percent = 50)
}
