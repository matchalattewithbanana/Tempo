package me.avinas.tempo.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsCatalogTest {
    @Test fun browseOrderContainsEverySectionExactlyOnce() {
        assertEquals(SettingsSection.entries.size, SettingsSection.browseOrder.size)
        assertEquals(SettingsSection.entries.toSet(), SettingsSection.browseOrder.toSet())
    }

    @Test fun everyDestinationHasAValidSingleOwner() {
        val validOwners = SettingsSection.entries.toSet()
        assertTrue(SettingsDestination.entries.all { it.owner in validOwners })
    }

    @Test fun destinationTitlesAreUnique() {
        assertEquals(
            SettingsDestination.entries.size,
            SettingsDestination.entries.map { it.titleRes }.toSet().size,
        )
    }
}
