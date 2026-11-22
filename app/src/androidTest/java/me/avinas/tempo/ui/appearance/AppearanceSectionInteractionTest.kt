package me.avinas.tempo.ui.appearance

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.test.platform.app.InstrumentationRegistry
import me.avinas.tempo.R
import me.avinas.tempo.ui.theme.TempoTheme
import org.junit.Rule
import org.junit.Test

/** Verifies expandable settings behave as one accessible, animated action. */
class AppearanceSectionInteractionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun accordionAnnouncesAndRestoresExpandedState() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val title = context.getString(R.string.appearance_effects_heading)
        val collapsed = context.getString(R.string.appearance_section_collapsed)
        val expanded = context.getString(R.string.appearance_section_expanded)
        val body = "Motion settings example"

        compose.setContent {
            TempoTheme(appearance = AppearanceSettings()) {
                AppearanceSection(R.string.appearance_effects_heading) {
                    Text(body)
                }
            }
        }

        val header = compose.onNode(hasText(title) and hasClickAction())
        compose.onNode(
            hasText(title) and hasClickAction() and
                SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, collapsed),
        ).assertExists()
        compose.onNodeWithText(body).assertDoesNotExist()

        header.performClick()
        compose.onNode(
            hasText(title) and hasClickAction() and
                SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, expanded),
        ).assertExists()
        compose.onNodeWithText(body).assertIsDisplayed()

        header.performClick()
        compose.onNode(
            hasText(title) and hasClickAction() and
                SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, collapsed),
        ).assertExists()
        compose.waitForIdle()
        compose.onNodeWithText(body).assertDoesNotExist()
    }
}
