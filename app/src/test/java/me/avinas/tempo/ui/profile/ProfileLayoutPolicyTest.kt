package me.avinas.tempo.ui.profile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileLayoutPolicyTest {
    @Test fun normalPhoneKeepsCompactTwoMetricRow() {
        assertFalse(shouldStackProfileMetrics(320f, 1f))
        assertFalse(shouldStackProfileMetrics(393f, 1f))
        assertFalse(shouldStackProfileMetrics(840f, 1f))
    }

    @Test fun narrowPhonesAndLargeTextShowFullWidthMetricCards() {
        assertTrue(shouldStackProfileMetrics(280f, 1f))
        assertTrue(shouldStackProfileMetrics(393f, 1.3f))
        assertTrue(shouldStackProfileMetrics(840f, 2f))
    }

    @Test fun identityStacksOnlyWhenTextNeedsMoreRoom() {
        assertFalse(shouldStackProfileIdentity(compact = true, fontScale = 1f))
        assertFalse(shouldStackProfileIdentity(compact = false, fontScale = 1.3f))
        assertTrue(shouldStackProfileIdentity(compact = true, fontScale = 1.5f))
        assertTrue(shouldStackProfileIdentity(compact = false, fontScale = 2f))
    }
}
