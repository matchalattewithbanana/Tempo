package me.avinas.tempo.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.avinas.tempo.ui.appearance.LocalAppearanceRuntimePolicy

/**
 * A single loading language across Home, Stats, History, Settings, and updates.
 * Wavy indicators are reserved for active loading; reduced motion and adaptive
 * power policy always fall back to standard Material 3 rendering.
 */
@Composable
fun TempoCircularLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 4.dp,
) {
    if (LocalAppearanceRuntimePolicy.current.reduceMotion) {
        CircularProgressIndicator(modifier = modifier, color = color, strokeWidth = strokeWidth)
    } else {
        CircularWavyProgressIndicator(modifier = modifier, color = color)
    }
}

@Composable
fun TempoLinearLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    progress: (() -> Float)? = null,
) {
    val reduced = LocalAppearanceRuntimePolicy.current.reduceMotion
    if (progress == null) {
        if (reduced) LinearProgressIndicator(modifier = modifier, color = color)
        else LinearWavyProgressIndicator(modifier = modifier, color = color)
    } else {
        if (reduced) LinearProgressIndicator(
            progress = { progress().coerceIn(0f, 1f) }, modifier = modifier, color = color,
        ) else LinearWavyProgressIndicator(
            progress = { progress().coerceIn(0f, 1f) }, modifier = modifier, color = color,
        )
    }
}
