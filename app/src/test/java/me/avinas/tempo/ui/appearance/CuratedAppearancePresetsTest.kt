package me.avinas.tempo.ui.appearance

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CuratedAppearancePresetsTest {
    private val editions = listOf(
        AppearancePreset.MIDNIGHT_BLOOM, AppearancePreset.SAKURA,
        AppearancePreset.MATCHA, AppearancePreset.COBALT,
        AppearancePreset.PORCELAIN, AppearancePreset.TERRACOTTA,
        AppearancePreset.CITRINE, AppearancePreset.MOCHA,
        AppearancePreset.LAGOON, AppearancePreset.LILAC,
    )

    private fun contrast(a: Color, b: Color): Float {
        val light = maxOf(a.luminance(), b.luminance())
        val dark = minOf(a.luminance(), b.luminance())
        return (light + 0.05f) / (dark + 0.05f)
    }

    @Test fun newLightAndDarkEditionsKeepTextAndActionsLegible() {
        val combinations = listOf(
            AppearanceTone.DARK to true,
            AppearanceTone.LIGHT to false,
            AppearanceTone.OLED to true,
            AppearanceTone.SYSTEM to false,
            AppearanceTone.SYSTEM to true,
        )
        for (preset in editions) {
            for ((tone, systemDark) in combinations) {
                val palette = AppearancePaletteResolver.resolve(
                    AppearanceSettings(preset = preset, tone = tone), systemDark,
                )
                val contrastPairs = listOf(
                    palette.onSurface to palette.surface,
                    palette.onSurface to palette.elevatedSurface,
                    palette.mutedText to palette.surface,
                    palette.mutedText to palette.elevatedSurface,
                    palette.accent to palette.surface,
                    palette.accent to palette.elevatedSurface,
                    palette.onAccent to palette.accent,
                )
                for ((foreground, background) in contrastPairs) {
                    assertTrue("Unreadable $preset $tone (${contrast(foreground, background)})",
                        contrast(foreground, background) >= 4.5f)
                }
            }
        }
    }

    @Test fun curatedPresetsHaveDistinctSurfacesInBothTones() {
        for (tone in listOf(AppearanceTone.LIGHT, AppearanceTone.DARK)) {
            val palettes = editions.map {
                AppearancePaletteResolver.resolve(AppearanceSettings(preset = it, tone = tone))
            }
            assertEquals(editions.size, palettes.map { it.background }.toSet().size)
            assertEquals(editions.size, palettes.map { it.elevatedSurface }.toSet().size)
            assertEquals(editions.size, palettes.map { it.accent }.toSet().size)
        }
    }

    @Test fun presetSelectionRestoresCuratedAccentWithoutDiscardingCustomColor() {
        val original = AppearanceSettings(
            preset = AppearancePreset.ABYSS,
            accentSource = AppearanceAccentSource.CUSTOM,
            customAccent = "12AB34",
            fontStyle = AppearanceFontStyle.STUDIO_MONO,
            backdrop = AppearanceBackdrop.FLAT,
            shape = AppearanceShape.SHARP,
        )
        val curated = original.withCuratedPreset(AppearancePreset.SAKURA, true)
        assertEquals(AppearancePreset.SAKURA, curated.preset)
        assertEquals(AppearanceAccentSource.PRESET, curated.accentSource)
        assertEquals("12AB34", curated.customAccent)
        assertEquals(AppearanceFontStyle.ARCHIVE_SERIF, curated.fontStyle)
        assertEquals(AppearanceBackdrop.WASH, curated.backdrop)

        val colorsOnly = original.withCuratedPreset(AppearancePreset.COBALT, false)
        assertEquals(AppearanceAccentSource.PRESET, colorsOnly.accentSource)
        assertEquals(original.fontStyle, colorsOnly.fontStyle)
        assertEquals(original.shape, colorsOnly.shape)
        assertEquals(original.backdrop, colorsOnly.backdrop)

        val signature = original.withCuratedPreset(AppearancePreset.GLACIER, true)
        assertEquals(original.fontStyle, signature.fontStyle)
        assertEquals(original.shape, signature.shape)
    }
    @Test fun selectingAnyPresetWithoutSuggestedDetailsPreservesLayoutAndMotion() {
        val original = AppearanceSettings(
            preset = AppearancePreset.ABYSS,
            accentSource = AppearanceAccentSource.CUSTOM,
            customAccent = "19AACC",
            fontStyle = AppearanceFontStyle.STUDIO_MONO,
            backdrop = AppearanceBackdrop.FLAT,
            shape = AppearanceShape.SHARP,
            density = AppearanceDensity.COMPACT,
            reduceMotion = true,
            showNavigationLabels = false,
            instantNavigation = true,
        )
        for (preset in AppearancePreset.entries) {
            val selected = original.withCuratedPreset(preset, applySuggestedDetails = false)
            assertEquals(
                "Selecting $preset must not unexpectedly change type, shape, density or motion",
                original.copy(preset = preset, accentSource = AppearanceAccentSource.PRESET),
                selected,
            )
        }
    }

}
