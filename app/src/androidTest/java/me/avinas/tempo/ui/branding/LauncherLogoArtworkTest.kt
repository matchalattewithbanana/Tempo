package me.avinas.tempo.ui.branding

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import me.avinas.tempo.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/** Confirms that the shipped adaptive icons are real raster artwork, not placeholder vectors. */
@RunWith(AndroidJUnit4::class)
class LauncherLogoArtworkTest {
    @Test
    fun everyAlternateLauncherContainsTheRenderedBadgeAndMatchingThemedGlyph() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val assets = listOf(
            R.drawable.tempo_logo_pulse_art to R.drawable.tempo_logo_pulse_themed,
            R.drawable.tempo_logo_clock_art to R.drawable.tempo_logo_clock_themed,
            R.drawable.tempo_logo_trail_art to R.drawable.tempo_logo_trail_themed,
            R.drawable.tempo_logo_vinyl_art to R.drawable.tempo_logo_vinyl_themed,
        )
        for ((art, themed) in assets) {
            for (resource in listOf(art, themed)) {
                val bitmap = BitmapFactory.decodeResource(context.resources, resource)
                assertNotNull("Launcher resource $resource failed to decode", bitmap)
                assertEquals(432, bitmap.width)
                assertEquals(432, bitmap.height)
                assertTrue("Launcher artwork needs an alpha channel", bitmap.hasAlpha())
            }
        }
    }
}
