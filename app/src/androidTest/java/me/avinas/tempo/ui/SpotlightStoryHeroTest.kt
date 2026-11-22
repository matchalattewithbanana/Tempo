package me.avinas.tempo.ui

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import me.avinas.tempo.R
import me.avinas.tempo.ui.appearance.AppearanceSettings
import me.avinas.tempo.ui.appearance.AppearanceTone
import me.avinas.tempo.ui.spotlight.SpotlightStoryHeroCard
import me.avinas.tempo.ui.theme.TempoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SpotlightStoryHeroTest {
    @get:Rule val compose = createComposeRule()

    @Test fun unlockedHeroRespondsToNativeSurfaceTap() {
        var launched = 0
        compose.setContent {
            TempoTheme(appearance = AppearanceSettings(tone = AppearanceTone.LIGHT)) {
                SpotlightStoryHeroCard(
                    onClick = { launched++ },
                    buttonText = "Play story",
                    isLocked = false,
                    lockMessage = "",
                    leadArtUrl = null,
                    storyLoading = false,
                )
            }
        }
        compose.onNodeWithText("Play story").performClick()
        compose.runOnIdle { assertEquals(1, launched) }
    }

    @Test fun lockedHeroAnnouncesReasonAndCannotBeActivated() {
        val title = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(R.string.spotlight_story_locked_title)
        compose.setContent {
            TempoTheme(appearance = AppearanceSettings(tone = AppearanceTone.LIGHT)) {
                SpotlightStoryHeroCard(
                    onClick = { error("Locked story must not launch") },
                    buttonText = "Unavailable",
                    isLocked = true,
                    lockMessage = "Not yet available",
                    leadArtUrl = null,
                    storyLoading = false,
                )
            }
        }
        compose.onNodeWithText(title).assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithText("Not yet available").assertExists()
    }
}
