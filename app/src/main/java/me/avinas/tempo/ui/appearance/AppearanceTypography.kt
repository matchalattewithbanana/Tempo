@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

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
            bodyLargeEmphasized = original.bodyLargeEmphasized.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            bodyMediumEmphasized = original.bodyMediumEmphasized.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            bodySmallEmphasized = original.bodySmallEmphasized.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            labelLargeEmphasized = original.labelLargeEmphasized.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            labelMediumEmphasized = original.labelMediumEmphasized.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            labelSmallEmphasized = original.labelSmallEmphasized.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
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
            displaySmallEmphasized = base.displaySmallEmphasized.copy(fontFamily = headingFont),
            headlineLargeEmphasized = base.headlineLargeEmphasized.copy(fontFamily = headingFont),
            headlineMediumEmphasized = base.headlineMediumEmphasized.copy(fontFamily = headingFont),
            headlineSmallEmphasized = base.headlineSmallEmphasized.copy(fontFamily = headingFont),
            // M3 brand fonts belong to display, headline and title-large.
            // Title-medium/small (including emphasized) retain the plain
            // readable typeface so selection never swaps fonts mid-layout.
            titleLargeEmphasized = base.titleLargeEmphasized.copy(fontFamily = headingFont),
        )
    }
}
