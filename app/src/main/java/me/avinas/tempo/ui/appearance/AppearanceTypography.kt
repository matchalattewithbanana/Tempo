package me.avinas.tempo.ui.appearance

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import me.avinas.tempo.ui.theme.AppFontFamily

/**
 * Cached per selected enum by TempoTheme. Only headings change from the shipped
 * Tempo typography; all four choices preserve normal body readability and
 * Android system font scaling.
 */
object AppearanceTypography {
    fun resolve(style: AppearanceFontStyle, bold: Boolean = false): Typography {
        val original = me.avinas.tempo.ui.theme.Typography
        val base = if (!bold) original else original.copy(
            bodyLarge = original.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
            bodyMedium = original.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
            bodySmall = original.bodySmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
            labelLarge = original.labelLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            labelMedium = original.labelMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            labelSmall = original.labelSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
        )
        val headingFont = when (style) {
            AppearanceFontStyle.TEMPO -> return base
            AppearanceFontStyle.NEO_SANS -> AppFontFamily
            AppearanceFontStyle.ARCHIVE_SERIF -> FontFamily.Serif
            AppearanceFontStyle.STUDIO_MONO -> FontFamily.Monospace
        }
        return base.copy(
            displayLarge = base.displayLarge.copy(fontFamily = headingFont),
            displayMedium = base.displayMedium.copy(fontFamily = headingFont),
            displaySmall = base.displaySmall.copy(fontFamily = headingFont),
            headlineLarge = base.headlineLarge.copy(fontFamily = headingFont),
            headlineMedium = base.headlineMedium.copy(fontFamily = headingFont),
            headlineSmall = base.headlineSmall.copy(fontFamily = headingFont),
            titleLarge = base.titleLarge.copy(fontFamily = headingFont),
            displayLargeEmphasized = base.displayLargeEmphasized.copy(fontFamily = headingFont),
            displayMediumEmphasized = base.displayMediumEmphasized.copy(fontFamily = headingFont),
            headlineLargeEmphasized = base.headlineLargeEmphasized.copy(fontFamily = headingFont),
            headlineMediumEmphasized = base.headlineMediumEmphasized.copy(fontFamily = headingFont),
            titleLargeEmphasized = base.titleLargeEmphasized.copy(fontFamily = headingFont),
        )
    }
}
