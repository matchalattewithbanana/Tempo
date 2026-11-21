package me.avinas.tempo.ui.appearance

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * The user's three corner preferences remap the published M3 ten-step corner scale.
 * Values are Material tokens (0, 4, 8, 12, 16, 20, 28, 32, 48dp), rather than
 * near-matches like 18/24/30dp. The mappings themselves are Tempo design choices.
 * Dense controls still use the smaller component-specific slots.
 */
object AppearanceMaterialShapes {
    fun resolve(choice: AppearanceShape): Shapes = when (choice) {
        AppearanceShape.SOFT -> Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(20.dp),
            largeIncreased = RoundedCornerShape(28.dp),
            extraLarge = RoundedCornerShape(28.dp),
            extraLargeIncreased = RoundedCornerShape(32.dp),
            extraExtraLarge = RoundedCornerShape(32.dp),
        )
        AppearanceShape.MODERATE -> Shapes(
            extraSmall = RoundedCornerShape(4.dp),
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(12.dp),
            large = RoundedCornerShape(16.dp),
            largeIncreased = RoundedCornerShape(20.dp),
            extraLarge = RoundedCornerShape(28.dp),
            extraLargeIncreased = RoundedCornerShape(32.dp),
            extraExtraLarge = RoundedCornerShape(28.dp),
        )
        AppearanceShape.SHARP -> Shapes(
            extraSmall = RoundedCornerShape(0.dp),
            small = RoundedCornerShape(4.dp),
            medium = RoundedCornerShape(8.dp),
            large = RoundedCornerShape(12.dp),
            largeIncreased = RoundedCornerShape(16.dp),
            extraLarge = RoundedCornerShape(20.dp),
            extraLargeIncreased = RoundedCornerShape(28.dp),
            extraExtraLarge = RoundedCornerShape(20.dp),
        )
    }
}
