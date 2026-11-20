package me.avinas.tempo.ui.appearance

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** A shared Material 3 shape scale based on the existing corner preference. */
object AppearanceMaterialShapes {
    fun resolve(choice: AppearanceShape): Shapes = when (choice) {
        AppearanceShape.SOFT -> Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(30.dp),
            largeIncreased = RoundedCornerShape(30.dp),
            extraLargeIncreased = RoundedCornerShape(34.dp),
            extraExtraLarge = RoundedCornerShape(40.dp),
        )
        AppearanceShape.MODERATE -> Shapes(
            extraSmall = RoundedCornerShape(4.dp),
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(12.dp),
            large = RoundedCornerShape(18.dp),
            extraLarge = RoundedCornerShape(24.dp),
            largeIncreased = RoundedCornerShape(24.dp),
            extraLargeIncreased = RoundedCornerShape(28.dp),
            extraExtraLarge = RoundedCornerShape(34.dp),
        )
        AppearanceShape.SHARP -> Shapes(
            extraSmall = RoundedCornerShape(2.dp),
            small = RoundedCornerShape(4.dp),
            medium = RoundedCornerShape(8.dp),
            large = RoundedCornerShape(12.dp),
            extraLarge = RoundedCornerShape(16.dp),
            largeIncreased = RoundedCornerShape(16.dp),
            extraLargeIncreased = RoundedCornerShape(20.dp),
            extraExtraLarge = RoundedCornerShape(26.dp),
        )
    }
}
