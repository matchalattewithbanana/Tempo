package me.avinas.tempo.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/** Compact navigation keeps all category labels legible; only wide layouts split. */
class SettingsBentoLayoutTest {
    @Test fun phonesAlwaysUseFullWidthCategoryRows() {
        assertEquals(1, settingsBentoColumns(320f, 1f))
        assertEquals(1, settingsBentoColumns(359.9f, 1f))
        assertEquals(1, settingsBentoColumns(393f, 1f))
        assertEquals(1, settingsBentoColumns(599f, 1f))
        assertEquals(1, settingsBentoColumns(393f, 1.3f))
    }

    @Test fun expandedScreensUseTwoColumns() {
        assertEquals(2, settingsBentoColumns(600f, 1.2f))
        assertEquals(2, settingsBentoColumns(920f, 1.2f))
        assertEquals(2, settingsBentoColumns(700f, 1.29f))
    }

    @Test fun largeAccessibilityTextGetsFullWidthRows() {
        assertEquals(1, settingsBentoColumns(900f, 1.3f))
        assertEquals(1, settingsBentoColumns(1200f, 1.7f))
    }
}
