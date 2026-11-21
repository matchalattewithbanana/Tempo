package me.avinas.tempo.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TempoExpressiveTilesTest {
    private fun contrast(a: Color, b: Color): Float {
        val max = maxOf(a.luminance(), b.luminance())
        val min = minOf(a.luminance(), b.luminance())
        return (max + 0.05f) / (min + 0.05f)
    }

    @Test fun `keep semantic text when both tonal surfaces are readable`() {
        val foreground = Color(0xFFF7F7F7)
        assertEquals(
            foreground,
            tileForegroundFor(
                Color(0xFF181E27), Color(0xFF273543), foreground,
            ),
        )
    }

    @Test fun `recover readable text across bright and dark custom tiles`() {
        val cases = listOf(
            Triple(Color(0xFFF9F3D8), Color(0xFFEACF99), Color(0xFFEFEFEF)),
            Triple(Color(0xFF121F24), Color(0xFF274047), Color(0xFF222222)),
            Triple(Color(0xFFE0F2FA), Color(0xFFB3DCEC), Color(0xFFEFEFEF)),
        )
        cases.forEach { (surface, icon, preferred) ->
            val chosen = tileForegroundFor(surface, icon, preferred)
            assertTrue(contrast(chosen, surface) >= 4.5f)
            assertTrue(contrast(chosen, icon) >= 4.5f)
        }
    }
}
