package me.avinas.tempo.ui.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveNavigationPolicyTest {
    @Test fun railOnlyAppearsForPrimaryRoutesOnExpandedScreens() {
        assertFalse(shouldUseWideTempoRail(839.9f, true))
        assertTrue(shouldUseWideTempoRail(840f, true))
        assertTrue(shouldUseWideTempoRail(1200f, true))
        assertFalse(shouldUseWideTempoRail(1200f, false))
    }
}
