package me.avinas.tempo.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsSectionTest {
    @Test
    fun browseOrderContainsEverySectionExactlyOnce() {
        val order = SettingsSection.browseOrder
        assertEquals(SettingsSection.entries.size, order.size)
        assertEquals(SettingsSection.entries.toSet(), order.toSet())
    }
}
