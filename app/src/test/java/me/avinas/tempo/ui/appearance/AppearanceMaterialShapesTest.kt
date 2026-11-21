package me.avinas.tempo.ui.appearance

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceMaterialShapesTest {
    @Test fun cornerPreferencesUseOnlyPublishedMaterialSteps() {
        val allowed = setOf(
            RoundedCornerShape(0.dp),
            RoundedCornerShape(4.dp),
            RoundedCornerShape(8.dp),
            RoundedCornerShape(12.dp),
            RoundedCornerShape(16.dp),
            RoundedCornerShape(20.dp),
            RoundedCornerShape(28.dp),
            RoundedCornerShape(32.dp),
            RoundedCornerShape(48.dp),
        )
        AppearanceShape.entries.forEach { preference ->
            val s = AppearanceMaterialShapes.resolve(preference)
            listOf(
                s.extraSmall, s.small, s.medium, s.large, s.largeIncreased,
                s.extraLarge, s.extraLargeIncreased, s.extraExtraLarge,
            ).forEach { shape ->
                org.junit.Assert.assertTrue("Unknown shape for $preference: $shape", shape in allowed)
            }
        }
    }

    @Test fun cornerPreferenceKeepsDistinctDensityHierarchy() {
        val soft = AppearanceMaterialShapes.resolve(AppearanceShape.SOFT)
        val moderate = AppearanceMaterialShapes.resolve(AppearanceShape.MODERATE)
        val sharp = AppearanceMaterialShapes.resolve(AppearanceShape.SHARP)
        assertEquals(RoundedCornerShape(12.dp), soft.small)
        assertEquals(RoundedCornerShape(20.dp), soft.large)
        assertEquals(RoundedCornerShape(8.dp), moderate.small)
        assertEquals(RoundedCornerShape(16.dp), moderate.large)
        assertEquals(RoundedCornerShape(4.dp), sharp.small)
        assertEquals(RoundedCornerShape(12.dp), sharp.large)
        assertEquals(RoundedCornerShape(28.dp), soft.largeIncreased)
        assertEquals(RoundedCornerShape(32.dp), soft.extraLargeIncreased)
        assertEquals(RoundedCornerShape(32.dp), soft.extraExtraLarge)
        assertEquals(RoundedCornerShape(20.dp), moderate.largeIncreased)
        assertEquals(RoundedCornerShape(32.dp), moderate.extraLargeIncreased)
        assertEquals(RoundedCornerShape(28.dp), moderate.extraExtraLarge)
        assertEquals(RoundedCornerShape(16.dp), sharp.largeIncreased)
        assertEquals(RoundedCornerShape(28.dp), sharp.extraLargeIncreased)
        assertEquals(RoundedCornerShape(20.dp), sharp.extraExtraLarge)
    }
}
