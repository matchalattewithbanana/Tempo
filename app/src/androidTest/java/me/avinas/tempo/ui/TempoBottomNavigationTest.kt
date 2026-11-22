package me.avinas.tempo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.test.platform.app.InstrumentationRegistry
import me.avinas.tempo.R
import me.avinas.tempo.ui.appearance.AppearanceNavigationStyle
import me.avinas.tempo.ui.appearance.AppearanceSettings
import me.avinas.tempo.ui.appearance.AppearanceTone
import me.avinas.tempo.ui.navigation.Screen
import me.avinas.tempo.ui.navigation.TempoBottomNavigation
import me.avinas.tempo.ui.theme.TempoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TempoBottomNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun floatingNavigationExposesSelectionAndReselectActions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val home = context.getString(R.string.nav_home)
        val stats = context.getString(R.string.nav_stats)
        val selected = mutableStateOf(Screen.Home.route)
        var reselections = 0

        compose.setContent {
            TempoTheme(appearance = AppearanceSettings(
                tone = AppearanceTone.LIGHT,
                navigationStyle = AppearanceNavigationStyle.FLOATING,
            )) {
                Box(Modifier.width(320.dp)) {
                    TempoBottomNavigation(
                        currentDestination = NavDestination("test").apply { route = selected.value },
                        onNavigateToHome = { reselections++; selected.value = Screen.Home.route },
                        onNavigateToStats = { selected.value = Screen.Stats.route },
                        onNavigateToHistory = { selected.value = Screen.History.route },
                    )
                }
            }
        }

        compose.onNodeWithText(home).assertIsSelected().performClick()
        compose.runOnIdle { assertEquals(1, reselections) }
        compose.onNodeWithText(stats).assertIsNotSelected().performClick().assertIsSelected()
        compose.onNodeWithText(home).assertIsNotSelected()
    }

    @Test fun largeTextKeepsNavigationTouchTargetsBounded() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val home = context.getString(R.string.nav_home)
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f),
            ) {
                TempoTheme(appearance = AppearanceSettings(tone = AppearanceTone.LIGHT)) {
                    Box(Modifier.width(320.dp)) {
                        TempoBottomNavigation(
                            currentDestination = NavDestination("test").apply { route = Screen.Home.route },
                            onNavigateToHome = {},
                            onNavigateToStats = {},
                            onNavigateToHistory = {},
                        )
                    }
                }
            }
        }
        val bounds = compose.onNodeWithText(home).assertIsSelected().getUnclippedBoundsInRoot()
        assertTrue("Touch target height must be >= 48dp", bounds.height >= 48.dp)
        assertTrue("Touch target must not cover content above the bar", bounds.height < 150.dp)
    }
}
