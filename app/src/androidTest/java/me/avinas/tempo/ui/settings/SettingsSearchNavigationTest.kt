package me.avinas.tempo.ui.settings

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import me.avinas.tempo.R
import me.avinas.tempo.ui.appearance.AppearanceSettings
import me.avinas.tempo.ui.theme.TempoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Prevent search navigation from regressing to implicit LazyColumn positions. */
class SettingsSearchNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun podcastSearchTargetsFilteringGroup() {
        verifyRoute("podcasts", R.string.settings_filter_podcasts, SettingsGroup.TRACKING_FILTERING)
    }

    @Test fun broadcastSearchTargetsConnectionsEvenWithoutSpotifyApi() {
        verifyRoute("broadcasts", R.string.settings_spotify_device_broadcasts, SettingsGroup.CONNECTIONS_SERVICES)
    }

    private fun verifyRoute(query: String, labelRes: Int, expected: SettingsGroup) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var selected: SettingsGroup? = null
        compose.setContent {
            TempoTheme(appearance = AppearanceSettings()) {
                SettingsOverview(
                    query = query,
                    onQueryChange = {},
                    listState = rememberLazyListState(),
                    onSectionSelected = {},
                    onSettingSelected = { selected = it },
                    onNavigate = {},
                    gamificationEnabled = false,
                    analyticsConfigured = false,
                    spotifyConnected = false,
                    spotifyAccountLoaded = false,
                )
            }
        }
        compose.onNodeWithText(context.getString(labelRes)).performClick()
        compose.runOnIdle { assertEquals(expected, selected) }
    }
}
