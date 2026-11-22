package me.avinas.tempo.ui.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupActionLayoutTest {
    @Test fun compactBackupButtonsStackWithoutCrushingSupportingText() {
        assertTrue(backupActionsStack(widthDp = 320f, fontScale = 1f))
        assertTrue(backupActionsStack(widthDp = 360f, fontScale = 1f))
        assertTrue(backupActionsStack(widthDp = 439f, fontScale = 1f))
    }

    @Test fun wideBackupActionsStayInOneRowAtNormalTextScale() {
        assertFalse(backupActionsStack(widthDp = 440f, fontScale = 1f))
        assertFalse(backupActionsStack(widthDp = 960f, fontScale = 1f))
    }

    @Test fun accessibilityTextAlwaysGetsFullWidthAction() {
        assertTrue(backupActionsStack(widthDp = 960f, fontScale = 1.3f))
        assertTrue(backupActionsStack(widthDp = 960f, fontScale = 2f))
    }
}
