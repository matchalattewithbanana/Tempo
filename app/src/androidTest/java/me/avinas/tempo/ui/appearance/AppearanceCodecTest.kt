package me.avinas.tempo.ui.appearance

import androidx.test.ext.junit.runners.AndroidJUnit4
import me.avinas.tempo.ui.components.ShareTheme
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppearanceCodecTest {
    @Test fun roundTripPreservesCompleteDraft() {
        val settings = AppearanceSettings(
            preset = AppearancePreset.EMBER, tone = AppearanceTone.OLED,
            fontStyle = AppearanceFontStyle.ARCHIVE_SERIF, reduceMotion = true,
            accentSource = AppearanceAccentSource.CUSTOM, density = AppearanceDensity.COMPACT,
            shape = AppearanceShape.SHARP, boldText = true, surface = AppearanceSurface.SOLID,
            adaptiveEffects = false, nowListening = AppearanceNowListening.HIDDEN,
            hiddenHomeSections = setOf(AppearanceHomeSection.SPOTLIGHT),
            homeOrder = AppearanceHomeSection.entries.reversed(),
            statsTab = AppearanceStatsTab.ALBUMS, statsRange = AppearanceStatsRange.TODAY,
            statsSort = AppearanceStatsSort.TIME, showStatsSummary = false, highlightTopResult = false,
            profileTab = AppearanceProfileTab.BADGES, showProfileNowPlaying = false, showProfileProgress = false,
            defaultShareTheme = ShareTheme.DAYLIGHT,
            customAccent = "F19345", backdrop = AppearanceBackdrop.WASH,
            navigationStyle = AppearanceNavigationStyle.FLOATING,
            ambientAnimation = true, sensorTilt = true, listEntranceMotion = true,
            effectIntensity = AppearanceEffectIntensity.EXPRESSIVE,
            customSecondary = "D770C0", customTertiary = "80C0AA",
        )
        assertEquals(settings, AppearanceCodec.decode(AppearanceCodec.encode(settings)))
    }

    @Test fun oldAndUnknownFieldsFallBackWithoutDuplicatingCards() {
        val decoded = AppearanceCodec.decode("""{"version":1,"preset":"future","density":"future",
            "homeOrder":["INSIGHTS","INSIGHTS","unknown"],"hiddenHome":["unknown","TODAY"]}""")
        assertEquals(AppearancePreset.ABYSS, decoded.preset)
        assertEquals(AppearanceNavigationStyle.BAR, decoded.navigationStyle)
        assertFalse(decoded.ambientAnimation)
        assertFalse(decoded.sensorTilt)
        assertFalse(decoded.listEntranceMotion)
        assertEquals(AppearanceDensity.COMFORTABLE, decoded.density)
        assertEquals(AppearanceBackdrop.GRADIENT, decoded.backdrop)
        assertEquals(AppearanceStatsSort.COMBINED, decoded.statsSort)
        assertEquals(AppearanceProfileTab.CHALLENGES, decoded.profileTab)
        assertEquals(ShareTheme.MIDNIGHT, decoded.defaultShareTheme)
        assertTrue(decoded.highlightTopResult && decoded.showProfileProgress)
        assertNull(decoded.customAccent)
        assertEquals(AppearanceHomeSection.INSIGHTS, decoded.homeOrder.first())
        assertEquals(AppearanceHomeSection.entries.toSet(), decoded.homeOrder.toSet())
        assertEquals(AppearanceHomeSection.entries.size, decoded.homeOrder.size)
        assertEquals(setOf(AppearanceHomeSection.TODAY), decoded.hiddenHomeSections)
    }

    @Test fun futureOversizedAndMalformedFilesAreRejected() {
        listOf("""{"version":999}""", "x".repeat(AppearanceCodec.MAX_BYTES + 1), "[]", "{}").forEach {
            assertTrue(runCatching { AppearanceCodec.decode(it) }.isFailure)
        }
    }
}
