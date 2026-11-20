package me.avinas.tempo.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/** Keep Settings readable on phone widths and at larger accessibility fonts. */
class SettingsBentoLayoutTest {
    @Test fun categoryRowsStaySingleColumnOnPhones() {
        assertEquals(1, settingsBentoColumns(320f, 1f))
        assertEquals(1, settingsBentoColumns(393f, 1f))
        assertEquals(1, settingsBentoColumns(600f, 1.2f))
        assertEquals(1, settingsBentoColumns(679.9f, 1f))
    }

    @Test fun wideLayoutsUseTwoColumns() {
        assertEquals(2, settingsBentoColumns(680f, 1f))
        assertEquals(2, settingsBentoColumns(920f, 1.2f))
    }

    @Test fun largeFontsPreserveFullWidthRows() {
        assertEquals(1, settingsBentoColumns(900f, 1.3f))
        assertEquals(1, settingsBentoColumns(1200f, 1.7f))
        assertEquals(2, settingsBentoColumns(700f, 1.29f))
    }
}
