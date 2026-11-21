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

    @Test fun largeSingleChoiceSetsUseChipsInsteadOfCrowdedButtonGroups() {
        org.junit.Assert.assertFalse(shouldUseFilterChips(0))
        org.junit.Assert.assertFalse(shouldUseFilterChips(2))
        org.junit.Assert.assertFalse(shouldUseFilterChips(5))
        org.junit.Assert.assertTrue(shouldUseFilterChips(6))
        org.junit.Assert.assertTrue(shouldUseFilterChips(9))
    }

    @Test fun choicesNeverExceedActualOptions() {
        assertEquals(1, connectedChoiceColumns(390, 1f, 1))
        assertEquals(1, connectedChoiceColumns(390, 1f, 0))
        assertEquals(3, connectedChoiceColumns(390, 1f, 3))
    }
    @Test fun materialBreakpointsResolveToOneCanonicalPolicy() {
        assertEquals(TempoWidthClass.COMPACT, tempoAdaptiveLayout(599.9f).widthClass)
        assertEquals(TempoWidthClass.MEDIUM, tempoAdaptiveLayout(600f).widthClass)
        assertEquals(TempoWidthClass.EXPANDED, tempoAdaptiveLayout(840f).widthClass)
        assertEquals(TempoWidthClass.LARGE, tempoAdaptiveLayout(1200f).widthClass)
        assertEquals(TempoWidthClass.EXTRA_LARGE, tempoAdaptiveLayout(1600f).widthClass)
    }

    @Test fun expandedLayoutsExposeStructuralCapabilities() {
        assertEquals(TempoPrimaryNavigation.BOTTOM_BAR, tempoAdaptiveLayout(393f).primaryNavigation)
        assertEquals(TempoPrimaryNavigation.COLLAPSED_RAIL, tempoAdaptiveLayout(700f).primaryNavigation)
        assertEquals(TempoPrimaryNavigation.EXPANDED_RAIL, tempoAdaptiveLayout(900f).primaryNavigation)
        org.junit.Assert.assertFalse(tempoAdaptiveLayout(839.9f).supportsTwoPane)
        org.junit.Assert.assertTrue(tempoAdaptiveLayout(840f).supportsTwoPane)
        assertEquals(16f, tempoAdaptiveLayout(393f).contentMarginDp)
        assertEquals(24f, tempoAdaptiveLayout(840f).contentMarginDp)
    }
}
