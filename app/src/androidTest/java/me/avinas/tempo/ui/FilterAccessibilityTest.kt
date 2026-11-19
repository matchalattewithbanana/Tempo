package me.avinas.tempo.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import me.avinas.tempo.R
import me.avinas.tempo.data.stats.TimeRange
import me.avinas.tempo.ui.components.SettingsSwitch
import me.avinas.tempo.ui.components.TimePeriodSelector
import org.junit.Rule
import org.junit.Test

class FilterAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun periodChoicesExposeGroupAndUpdateSelection() {
        val range = mutableStateOf(TimeRange.TODAY)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val todayLabel = context.getString(R.string.stats_share_period_today)
        val weekLabel = context.getString(R.string.stats_range_week)
        compose.setContent {
            TimePeriodSelector(
                selectedRange = range.value,
                onRangeSelected = { range.value = it },
                availableRanges = listOf(TimeRange.TODAY, TimeRange.THIS_WEEK),
            )
        }
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup)).assertCountEquals(1)
        compose.onNodeWithText(todayLabel).assertIsSelected()
        compose.onNodeWithText(weekLabel).assertIsNotSelected().performClick().assertIsSelected()
        compose.onNodeWithText(todayLabel).assertIsNotSelected()
    }

    @Test fun settingsLabelAndSwitchShareOneToggleAction() {
        val enabled = mutableStateOf(false)
        compose.setContent {
            SettingsSwitch("Background tracking", "Detect plays while Tempo is closed", enabled.value) {
                enabled.value = it
            }
        }
        compose.onAllNodes(hasClickAction(), useUnmergedTree = true).assertCountEquals(1)
        compose.onNodeWithText("Background tracking").assertIsOff().performClick().assertIsOn()
    }
}
