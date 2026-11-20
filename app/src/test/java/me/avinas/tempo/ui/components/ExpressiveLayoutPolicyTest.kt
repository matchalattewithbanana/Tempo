package me.avinas.tempo.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class ExpressiveLayoutPolicyTest {
    @Test fun phoneAndAccessibilityControlsHaveReadableGroupSizes() {
        assertEquals(2, connectedChoiceColumns(320, 1f, 5))
        assertEquals(2, connectedChoiceColumns(390, 1.4f, 5))
        assertEquals(3, connectedChoiceColumns(390, 1f, 5))
    }

    @Test fun fourOptionRowsStayBalancedOnNormalPhones() {
        assertEquals(2, connectedChoiceColumns(390, 1f, 4))
        assertEquals(4, connectedChoiceColumns(700, 1f, 4))
    }

    @Test fun choicesNeverExceedActualOptions() {
        assertEquals(1, connectedChoiceColumns(390, 1f, 1))
        assertEquals(1, connectedChoiceColumns(390, 1f, 0))
        assertEquals(3, connectedChoiceColumns(390, 1f, 3))
    }
}
