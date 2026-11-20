package me.avinas.tempo.ui.appearance

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceMaterialShapesTest {
    @Test fun cornerPreferenceMapsToSharedMaterialShapes() {
        val soft = AppearanceMaterialShapes.resolve(AppearanceShape.SOFT)
        val moderate = AppearanceMaterialShapes.resolve(AppearanceShape.MODERATE)
        val sharp = AppearanceMaterialShapes.resolve(AppearanceShape.SHARP)
        assertEquals(RoundedCornerShape(12.dp), soft.small)
        assertEquals(RoundedCornerShape(24.dp), soft.large)
        assertEquals(RoundedCornerShape(8.dp), moderate.small)
        assertEquals(RoundedCornerShape(18.dp), moderate.large)
        assertEquals(RoundedCornerShape(4.dp), sharp.small)
        assertEquals(RoundedCornerShape(12.dp), sharp.large)
    }
}
