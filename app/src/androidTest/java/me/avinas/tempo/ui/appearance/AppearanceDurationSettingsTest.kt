package me.avinas.tempo.ui.appearance

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

/** Appearance-only backups retain duration style and remain backwards compatible. */
class AppearanceDurationSettingsTest {
    @Test
    fun precisionAndClockStyleSurviveExportImport() {
        val configured = AppearanceSettings(
            statsDuration = AppearanceStatsDuration.CLOCK_SECONDS,
            statsDurationPrecision = AppearanceDurationPrecision.THREE,
        )
        val restored = AppearanceCodec.decode(AppearanceCodec.encode(configured))
        assertEquals(AppearanceStatsDuration.CLOCK_SECONDS, restored.statsDuration)
        assertEquals(AppearanceDurationPrecision.THREE, restored.statsDurationPrecision)
    }

    @Test
    fun curatedPresetIdentifiersSurviveAppearanceExportAndImport() {
        val presets = listOf(
            AppearancePreset.MIDNIGHT_BLOOM, AppearancePreset.SAKURA,
            AppearancePreset.MATCHA, AppearancePreset.COBALT,
            AppearancePreset.PORCELAIN, AppearancePreset.TERRACOTTA,
            AppearancePreset.CITRINE, AppearancePreset.MOCHA,
            AppearancePreset.LAGOON, AppearancePreset.LILAC,
        )
        presets.forEach { preset ->
            val stored = AppearanceCodec.encode(AppearanceSettings(preset = preset))
            assertEquals(preset, AppearanceCodec.decode(stored).preset)
            assertEquals(preset, AppearancePreset.decode(preset.name))
        }
    }

    @Test
    fun oldAppearanceWithoutPrecisionUsesDefault() {
        val old = JSONObject(AppearanceCodec.encode(AppearanceSettings(
            statsDuration = AppearanceStatsDuration.HOURS,
        ))).apply {
            put("version", 2)
            remove("statsDurationPrecision")
        }
        val restored = AppearanceCodec.decode(old.toString())
        assertEquals(AppearanceStatsDuration.HOURS, restored.statsDuration)
        assertEquals(AppearanceDurationPrecision.TWO, restored.statsDurationPrecision)
    }
}
