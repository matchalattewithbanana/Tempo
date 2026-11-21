package me.avinas.tempo.ui.appearance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import org.junit.Test

class AppearancePaletteTest {
    @Test fun `unrecognized saved style resets to Abyss`() {
        assertEquals(AppearancePreset.ABYSS, AppearancePreset.decode("removed_preset"))
        assertEquals(AppearancePreset.ABYSS, AppearancePreset.decode(null))
    }

    @Test fun `unknown saved font falls back to bundled Tempo typography`() {
        assertEquals(AppearanceFontStyle.TEMPO, AppearanceFontStyle.decode(null))
        assertEquals(AppearanceFontStyle.TEMPO, AppearanceFontStyle.decode("missing_style"))
    }

    @Test fun `unrecognized tone retains dark backward compatibility`() {
        assertEquals(AppearanceTone.DARK, decodeAppearanceTone(null))
        assertEquals(AppearanceTone.DARK, decodeAppearanceTone("future_mode"))
    }

    @Test fun `each initial visual style has independent foreground background and accent`() {
        val palettes = AppearancePreset.entries.map(AppearancePaletteResolver::forPreset)
        assertEquals(AppearancePreset.entries.size, palettes.map { it.background }.distinct().size)
        assertEquals(AppearancePreset.entries.size, palettes.map { it.accent }.distinct().size)
        palettes.forEach {
            assertNotEquals(it.onSurface, it.background)
            assertNotEquals(it.accent, it.background)
            assertNotEquals(it.onAccent, it.accent)
        }
    }

    @Test fun `Abyss retains existing default background and accent`() {
        val abyss = AppearancePaletteResolver.forPreset(AppearancePreset.ABYSS)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF0A0E0E), abyss.background)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF2FDBB8), abyss.accent)
    }
    @Test fun `all dark presets use high contrast text and buttons`() {
        AppearancePreset.entries.forEach { preset ->
            val palette = AppearancePaletteResolver.forPreset(preset)
            assertTrue("$preset primary text", contrast(palette.onSurface, palette.background) >= 7.0)
            assertTrue("$preset muted text", contrast(palette.mutedText, palette.surface) >= 4.5)
            assertTrue("$preset button label", contrast(palette.onAccent, palette.accent) >= 4.5)
        }
    }

    @Test fun `light previews and OLED preserve readable labels`() {
        AppearancePreset.entries.forEach { preset ->
            listOf(AppearanceTone.LIGHT, AppearanceTone.OLED).forEach { tone ->
                val palette = AppearancePaletteResolver.resolve(AppearanceSettings(preset = preset, tone = tone))
                assertTrue("$preset $tone text", contrast(palette.onSurface, palette.background) >= 7.0)
                assertTrue("$preset $tone muted", contrast(palette.mutedText, palette.surface) >= 4.5)
                assertTrue("$preset $tone button", contrast(palette.onAccent, palette.accent) >= 4.5)
            }
        }
    }

    @Test fun `system tone observes dark and light OS preference`() {
        AppearancePreset.entries.forEach { preset ->
            val settings = AppearanceSettings(preset = preset, tone = AppearanceTone.SYSTEM)
            val light = AppearancePaletteResolver.resolve(settings, systemDark = false)
            val dark = AppearancePaletteResolver.resolve(settings, systemDark = true)
            assertTrue("$preset should be light", !light.isDark)
            assertTrue("$preset should be dark", dark.isDark)
            assertNotEquals(light.background, dark.background)
        }
    }

    @Test fun `opt in beta tones survive live settings`() {
        assertEquals(AppearanceTone.LIGHT, AppearanceSettings(tone = AppearanceTone.LIGHT).forLiveApp().tone)
        assertEquals(AppearanceTone.SYSTEM, AppearanceSettings(tone = AppearanceTone.SYSTEM).forLiveApp().tone)
        assertEquals(AppearanceTone.OLED, AppearanceSettings(tone = AppearanceTone.OLED).forLiveApp().tone)
    }

    @Test fun `light presets have distinct surfaces and readable muted text`() {
        val palettes = AppearancePreset.entries.map {
            AppearancePaletteResolver.resolve(AppearanceSettings(preset = it, tone = AppearanceTone.LIGHT))
        }
        assertEquals(palettes.size, palettes.map { it.background }.distinct().size)
        assertEquals(palettes.size, palettes.map { it.surface }.distinct().size)
        palettes.forEach { palette ->
            for (surface in listOf(palette.surface, palette.elevatedSurface, palette.surfaceSunken)) {
                assertTrue("${palette.preset} muted text", contrast(palette.mutedText, surface) >= 4.5)
                assertTrue("${palette.preset} accent text", contrast(palette.accent, surface) >= 4.5)
            }
        }
    }

    @Test fun `custom accents adapt to each tone without replacing preset surfaces`() {
        val settings = AppearanceSettings(preset = AppearancePreset.AURORA, accentSource = AppearanceAccentSource.CUSTOM)
        for (systemDark in listOf(false, true)) {
            for (tone in AppearanceTone.entries) {
                val base = AppearancePaletteResolver.resolve(settings.copy(tone = tone), systemDark)
                val custom = AppearancePaletteResolver.resolve(
                    settings.copy(tone = tone, customAccent = if (base.isDark) "000000" else "FFFFFF"),
                    systemDark,
                )
                assertEquals(base.background, custom.background)
                assertEquals(base.surface, custom.surface)
                for (surface in listOf(custom.surface, custom.elevatedSurface, custom.surfaceSunken)) {
                    assertTrue("$tone custom text", contrast(custom.accent, surface) >= 4.5)
                }
                assertTrue("$tone custom button", contrast(custom.onAccent, custom.accent) >= 4.5)
                assertEquals(base, AppearancePaletteResolver.resolve(settings.copy(tone = tone, customAccent = "invalid"), systemDark))
            }
        }
        assertEquals("ABCDEF", normalizeAccentHex(" #abcdef "))
        assertEquals(null, normalizeAccentHex("#FFFFFF7F"))
    }

    @Test fun `optional palette seeds do not alter listening surfaces and maintain contrast`() {
        for (tone in listOf(AppearanceTone.DARK, AppearanceTone.LIGHT, AppearanceTone.OLED)) {
            val baseSettings = AppearanceSettings(
                preset = AppearancePreset.LAGOON, tone = tone,
                accentSource = AppearanceAccentSource.CUSTOM,
                customAccent = "537CF2",
            )
            val base = AppearancePaletteResolver.resolve(baseSettings)
            val custom = AppearancePaletteResolver.resolve(
                baseSettings.copy(customSecondary = "FF0000", customTertiary = "003900"),
            )
            assertEquals(base.background, custom.background)
            assertEquals(base.surface, custom.surface)
            assertTrue(custom.secondaryAccent != null && custom.tertiaryAccent != null)
            val scheme = custom.materialColorScheme()
            assertTrue(contrast(scheme.onSecondary, scheme.secondary) >= 4.5)
            assertTrue(contrast(scheme.onTertiary, scheme.tertiary) >= 4.5)
            listOf(base.surface, base.elevatedSurface, base.surfaceSunken).forEach { surface ->
                assertTrue(contrast(custom.secondaryAccent!!, surface) >= 4.5)
                assertTrue(contrast(custom.tertiaryAccent!!, surface) >= 4.5)
            }
        }
        val invalid = AppearancePaletteResolver.resolve(
            AppearanceSettings(
                accentSource = AppearanceAccentSource.CUSTOM,
                customSecondary = "not-hex", customTertiary = "#FFFFAAAA",
            ),
        )
        assertEquals(null, invalid.secondaryAccent)
        assertEquals(null, invalid.tertiaryAccent)
    }

    @Test fun `Material inverse actions and container roles stay paired in every tone`() {
        AppearancePreset.entries.forEach { preset ->
            AppearanceTone.entries.forEach { tone ->
                val scheme = AppearancePaletteResolver.resolve(
                    AppearanceSettings(preset = preset, tone = tone),
                    systemDark = false,
                ).materialColorScheme()
                assertTrue("$preset $tone inverse action",
                    contrast(scheme.inversePrimary, scheme.inverseSurface) >= 4.5)
                assertTrue("$preset $tone inverse text",
                    contrast(scheme.inverseOnSurface, scheme.inverseSurface) >= 4.5)
                assertTrue("$preset $tone tertiary container",
                    contrast(scheme.onTertiaryContainer, scheme.tertiaryContainer) >= 4.5)
            }
        }
    }

        private fun contrast(first: Color, second: Color): Double {
        fun luma(color: Color): Double {
            fun channel(value: Float): Double {
                val v = value.toDouble()
                return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
            }
            return channel(color.red) * 0.2126 +
                channel(color.green) * 0.7152 + channel(color.blue) * 0.0722
        }
        val a = luma(first)
        val b = luma(second)
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }

}
