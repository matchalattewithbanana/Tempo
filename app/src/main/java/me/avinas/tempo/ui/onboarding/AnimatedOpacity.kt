package me.avinas.tempo.ui.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import me.avinas.tempo.ui.theme.rememberReducedMotion

/**
 * Single, interruptible onboarding entrance treatment.
 *
 * Legacy call sites may still supply a stagger delay, but we intentionally
 * ignore it: showing useful first-run content must not wait for a cascade.
 * Opacity uses an effects spring; gentle y-movement uses a spatial spring.
 */
@Composable
fun AnimatedOpacity(
    @Suppress("UNUSED_PARAMETER") delay: Int,
    visible: Boolean,
    content: @Composable () -> Unit,
) {
    val reduceMotion = rememberReducedMotion()
    val effects = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = if (reduceMotion) snap() else effects,
        label = "onboardingAlpha",
    )
    val arrival by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = if (reduceMotion) snap() else spatial,
        label = "onboardingArrival",
    )
    Box(
        modifier = Modifier
            .alpha(alpha)
            .graphicsLayer { translationY = if (reduceMotion) 0f else (1f - arrival) * 18f },
    ) {
        content()
    }
}
