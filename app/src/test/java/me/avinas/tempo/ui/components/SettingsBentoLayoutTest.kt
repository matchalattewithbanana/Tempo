package me.avinas.tempo.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure layout policy checks: no device or Compose rendering is needed. */
class SettingsBentoLayoutTest {
    @Test fun singleColumnOnCompactDevices() {
        assertEquals(1, settingsBentoColumns(320f, 1f))
        assertEquals(1, settingsBentoColumns(369.9f, 1f))
    }

    @Test fun twoColumnsAtReadableWindowWidth() {
        assertEquals(2, settingsBentoColumns(370f, 1f))
        assertEquals(2, settingsBentoColumns(600f, 1.2f))
    }

    @Test fun largeFontsOverridePhysicalScreenWidth() {
        assertEquals(1, settingsBentoColumns(900f, 1.3f))
        assertEquals(1, settingsBentoColumns(1200f, 1.7f))
        assertEquals(2, settingsBentoColumns(700f, 1.29f))
    }
}
