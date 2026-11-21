package me.avinas.tempo.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TempoDialogLayoutPolicyTest {
    @Test fun compactPhonesStackActions() {
        assertTrue(stackTempoDialogActions(320f, 1f))
        assertTrue(stackTempoDialogActions(359.9f, 1f))
    }

    @Test fun ordinaryWidthsKeepActionOrderSideBySide() {
        assertFalse(stackTempoDialogActions(360f, 1f))
        assertFalse(stackTempoDialogActions(412f, 1.2f))
        assertFalse(stackTempoDialogActions(840f, 1f))
    }

    @Test fun accessibilityScalingForcesFullWidthActions() {
        assertTrue(stackTempoDialogActions(412f, 1.3f))
        assertTrue(stackTempoDialogActions(920f, 1.5f))
    }
}
