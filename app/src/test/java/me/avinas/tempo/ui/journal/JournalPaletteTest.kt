package me.avinas.tempo.ui.journal

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JournalPaletteTest {
    private fun contrast(a: Color, b: Color): Double {
        val first = a.luminance().toDouble()
        val second = b.luminance().toDouble()
        return (maxOf(first, second) + 0.05) / (minOf(first, second) + 0.05)
    }

    @Test fun `all journal tones keep text legible across surfaces`() {
        JournalTone.entries.forEach { tone ->
            val p = JournalPalettes.forTone(tone)
            assertTrue("$tone on canvas", contrast(p.onCanvas, p.canvas) >= 7.0)
            assertTrue("$tone on surface", contrast(p.onSurface, p.surface) >= 7.0)
            assertTrue("$tone on raised surface", contrast(p.onSurface, p.surfaceRaised) >= 7.0)
            assertTrue("$tone supporting text", contrast(p.supportingText, p.surface) >= 4.5)
            assertTrue("$tone faint text", contrast(p.faintText, p.surface) >= 4.5)
            assertTrue("$tone accent", contrast(p.accent, p.surface) >= 4.5)
            assertTrue("$tone button label", contrast(p.onAccent, p.accent) >= 4.5)
        }
    }

    @Test fun `paper ink and oled are materially distinct`() {
        assertNotEquals(JournalPalettes.ink.canvas, JournalPalettes.paper.canvas)
        assertNotEquals(JournalPalettes.ink.canvas, JournalPalettes.oled.canvas)
        assertNotEquals(JournalPalettes.paper.accent, JournalPalettes.ink.accent)
    }
}
