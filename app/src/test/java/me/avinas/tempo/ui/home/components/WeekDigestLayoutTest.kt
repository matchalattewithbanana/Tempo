package me.avinas.tempo.ui.home.components

import org.junit.Assert.assertEquals
import org.junit.Test

class WeekDigestLayoutTest {
    @Test fun narrowPhonesStaySingleColumn() {
        assertEquals(1, weekDigestColumns(320f, 1f))
        assertEquals(1, weekDigestColumns(359.9f, 1f))
    }

    @Test fun regularPhoneSupportsTwoColumns() {
        assertEquals(2, weekDigestColumns(360f, 1f))
        assertEquals(2, weekDigestColumns(412f, 1.2f))
        assertEquals(2, weekDigestColumns(840f, 1f))
    }

    @Test fun largeFontsPreventOvercrowding() {
        assertEquals(1, weekDigestColumns(412f, 1.3f))
        assertEquals(1, weekDigestColumns(900f, 1.5f))
    }
}
