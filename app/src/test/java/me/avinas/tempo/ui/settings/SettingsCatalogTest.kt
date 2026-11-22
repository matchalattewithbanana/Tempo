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

    @Test fun searchablePreferencesIncludePreviouslyHiddenOptions() {
        val items = SettingsSearchCatalog.available(
            gamificationEnabled = true,
            analyticsConfigured = true,
            spotifyConnected = true,
            spotifyAccountLoaded = true,
        )
        val titles = items.map { it.titleRes }.toSet()
        assertTrue(me.avinas.tempo.R.string.settings_manage_permissions in titles)
        assertTrue(me.avinas.tempo.R.string.settings_health_last_signal in titles)
        assertTrue(me.avinas.tempo.R.string.settings_filter_podcasts in titles)
        assertTrue(me.avinas.tempo.R.string.settings_smart_merge in titles)
        assertTrue(me.avinas.tempo.R.string.settings_version in titles)
        assertEquals(items.size, items.map { it.section to it.titleRes }.toSet().size)
        assertEquals(SettingsSection.entries.toSet(), items.map { it.section }.toSet())
    }

    @Test fun conditionalSearchResultsDoNotAdvertiseUnavailableControls() {
        val limited = SettingsSearchCatalog.available(
            gamificationEnabled = false,
            analyticsConfigured = false,
            spotifyConnected = false,
            spotifyAccountLoaded = true,
        )
        val labels = limited.map { it.titleRes }.toSet()
        assertTrue(me.avinas.tempo.R.string.settings_achievements !in labels)
        assertTrue(me.avinas.tempo.R.string.settings_analytics_toggle !in labels)
        assertTrue(me.avinas.tempo.R.string.settings_spotify_device_broadcasts in labels)
        assertTrue(me.avinas.tempo.R.string.settings_spotify_history_verification !in labels)
        assertTrue(me.avinas.tempo.R.string.settings_spotify_client_id_label in labels)
        assertEquals(3, limited.single {
            it.titleRes == me.avinas.tempo.R.string.settings_clear_all
        }.group.scrollIndexOrNull(false))
        val configured = SettingsSearchCatalog.available(
            gamificationEnabled = false,
            analyticsConfigured = true,
            spotifyConnected = false,
            spotifyAccountLoaded = true,
        )
        assertEquals(4, configured.single {
            it.titleRes == me.avinas.tempo.R.string.settings_clear_all
        }.group.scrollIndexOrNull(true))
    }

    @Test fun deviceBroadcastsRemainDiscoverableWithoutSpotifyAccount() {
        for (loaded in listOf(false, true)) {
            val entries = SettingsSearchCatalog.available(
                gamificationEnabled = false,
                analyticsConfigured = false,
                spotifyConnected = false,
                spotifyAccountLoaded = loaded,
            )
            assertTrue(entries.any {
                it.titleRes == me.avinas.tempo.R.string.settings_spotify_device_broadcasts &&
                    it.group == SettingsGroup.CONNECTIONS_SERVICES
            })
            assertTrue(entries.none {
                it.titleRes == me.avinas.tempo.R.string.settings_spotify_history_verification
            })
        }
    }

    @Test fun settingsGroupsHaveUniqueStableKeysAndCorrectOptionalPositions() {
        val groups = SettingsGroup.entries
        assertEquals(groups.size, groups.map { it.itemKey }.toSet().size)
        assertEquals(SettingsSection.entries.toSet(), groups.map { it.section }.toSet())
        assertEquals(null, SettingsGroup.DATA_PRIVACY.scrollIndexOrNull(false))
        assertEquals(3, SettingsGroup.DATA_PRIVACY.scrollIndexOrNull(true))
        assertEquals(3, SettingsGroup.DATA_RESET.scrollIndexOrNull(false))
        assertEquals(4, SettingsGroup.DATA_RESET.scrollIndexOrNull(true))
        for (analyticsEnabled in listOf(false, true)) {
            for (section in SettingsSection.entries) {
                val visible = groups.filter {
                    it.section == section && (!it.requiresAnalytics || analyticsEnabled)
                }
                assertEquals(
                    visible.indices.toList(),
                    visible.map { it.scrollIndexOrNull(analyticsEnabled) },
                )
            }
        }
    }

    @Test fun destinationTitlesAreUnique() {
        assertEquals(
            SettingsDestination.entries.size,
            SettingsDestination.entries.map { it.titleRes }.toSet().size,
        )
    }
}
