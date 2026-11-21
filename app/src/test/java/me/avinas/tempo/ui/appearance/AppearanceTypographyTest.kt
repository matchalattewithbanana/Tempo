package me.avinas.tempo.ui.appearance

import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceTypographyTest {
    @Test fun customDisplayFontsDoNotReplacePlainBodyAndCompactTitleRoles() {
        val original = AppearanceTypography.resolve(AppearanceFontStyle.TEMPO)
        listOf(
            AppearanceFontStyle.NEO_SANS,
            AppearanceFontStyle.ARCHIVE_SERIF,
            AppearanceFontStyle.STUDIO_MONO,
        ).forEach { style ->
            val typography = AppearanceTypography.resolve(style)
            assertEquals(original.bodyMedium.fontFamily, typography.bodyMedium.fontFamily)
            assertEquals(original.labelLarge.fontFamily, typography.labelLarge.fontFamily)
            assertEquals(original.titleMedium.fontFamily, typography.titleMedium.fontFamily)
            assertEquals(original.titleSmall.fontFamily, typography.titleSmall.fontFamily)
            assertEquals(original.titleMediumEmphasized.fontFamily, typography.titleMediumEmphasized.fontFamily)
            assertEquals(original.titleSmallEmphasized.fontFamily, typography.titleSmallEmphasized.fontFamily)
        }
    }
}
