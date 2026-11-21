package me.avinas.tempo.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/** The Orion-inspired staggered layout must still respect narrow screens and scale. */
class SettingsBentoLayoutTest {
    @Test fun smallestPhonesUseFullWidthCards() {
        assertEquals(1, settingsBentoColumns(320f, 1f))
        assertEquals(1, settingsBentoColumns(359.9f, 1f))
        assertEquals(1, settingsBentoColumns(393f, 1.3f))
    }

    @Test fun normalPhoneAndTabletWidthsShowTwoColumnBento() {
        assertEquals(2, settingsBentoColumns(360f, 1f))
        assertEquals(2, settingsBentoColumns(393f, 1f))
        assertEquals(2, settingsBentoColumns(600f, 1.2f))
        assertEquals(2, settingsBentoColumns(920f, 1.2f))
    }

    @Test fun accessibilityFontScaleAvoidsNarrowHalfCards() {
        assertEquals(1, settingsBentoColumns(900f, 1.3f))
        assertEquals(1, settingsBentoColumns(1200f, 1.7f))
        assertEquals(2, settingsBentoColumns(700f, 1.29f))
    }
}
