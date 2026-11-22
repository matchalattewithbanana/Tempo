package me.avinas.tempo.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.espresso.Espresso
import me.avinas.tempo.ui.profile.ProfileModal
import org.junit.Rule
import org.junit.Test

class ProfileModalTest {
    @get:Rule val compose = createComposeRule()

    @Test fun backDismissesOnlyTheTopModal() {
        val detailOpen = mutableStateOf(true)
        val shareOpen = mutableStateOf(true)
        compose.setContent {
            Text("Profile content")
            if (detailOpen.value) {
                ProfileModal("Badge details", onDismiss = { detailOpen.value = false }) {
                    Text("Details content")
                }
            }
            if (shareOpen.value) {
                ProfileModal("Share preview", onDismiss = { shareOpen.value = false }) {
                    Text("Share content")
                }
            }
        }

        compose.onNodeWithText("Share content").assertExists()
        // Espresso targets the focused Android dialog window and synchronizes
        // with the main thread. A shell keyevent can race with window focus and
        // composition, even when its file descriptor has already been read.
        Espresso.pressBackUnconditionally()
        awaitDismissed("Share content")
        compose.onNodeWithText("Details content").assertExists()

        Espresso.pressBackUnconditionally()
        awaitDismissed("Details content")
        compose.onNodeWithText("Profile content").assertExists()
    }

    private fun awaitDismissed(text: String) {
        compose.waitUntil(timeoutMillis = 5_000L) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithText(text).assertDoesNotExist()
    }
}
