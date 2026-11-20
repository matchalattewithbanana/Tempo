package me.avinas.tempo.ui.effects

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.composed
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import me.avinas.tempo.ui.appearance.AppearanceEffectIntensity
import me.avinas.tempo.ui.appearance.LocalAppearanceRuntimePolicy
import me.avinas.tempo.ui.theme.LocalTempoAppearance
import androidx.compose.material3.MaterialTheme
import kotlin.math.cos
import kotlin.math.sin

/** A single sensor state is provided by TempoTheme, not by individual cards. */
val LocalTempoTilt = staticCompositionLocalOf<State<Offset>> { mutableStateOf(Offset.Zero) }

/**
 * Registers a gravity sensor only while the feature is enabled AND this app
 * window is resumed. SENSOR_DELAY_UI, an additional 50ms throttle and a
 * low-pass filter avoid excessive main-thread state updates.
 *
 * There is one listener per active TempoTheme. No sensor permissions, services,
 * network calls, or background sensor sampling.
 */
@Composable
fun rememberTempoTilt(enabled: Boolean): State<Offset> {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val position = remember { mutableStateOf(Offset.Zero) }
    DisposableEffect(context, owner, enabled) {
        if (!enabled) {
            position.value = Offset.Zero
            onDispose { }
        } else {
            val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            val sensor = manager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            var registered = false
            var lastEvent = 0L
            val listener = object : SensorEventListener {
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.values.size < 2 || event.timestamp - lastEvent < 50_000_000L) return
                    lastEvent = event.timestamp
                    val x = (-event.values[0] / 9.81f).coerceIn(-1f, 1f)
                    val y = (event.values[1] / 9.81f).coerceIn(-1f, 1f)
                    val previous = position.value
                    position.value = Offset(
                        previous.x + (x - previous.x) * 0.18f,
                        previous.y + (y - previous.y) * 0.18f,
                    )
                }
            }
            fun update() {
                val active = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                if (active && !registered && manager != null && sensor != null) {
                    registered = manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
                } else if (!active && registered) {
                    manager?.unregisterListener(listener)
                    registered = false
                    position.value = Offset.Zero
                }
            }
            val observer = LifecycleEventObserver { _, _ -> update() }
            owner.lifecycle.addObserver(observer)
            update()
            onDispose {
                owner.lifecycle.removeObserver(observer)
                if (registered) manager?.unregisterListener(listener)
                position.value = Offset.Zero
            }
        }
    }
    return position
}

/** Translation/rotation of featured cards only, not independently tracked rows. */
fun Modifier.tempoTiltSurface(): Modifier = composed {
    val policy = LocalAppearanceRuntimePolicy.current
    if (!policy.allowSensorTilt) this
    else {
        val tilt = LocalTempoTilt.current
        val strong = LocalTempoAppearance.current.effectIntensity == AppearanceEffectIntensity.EXPRESSIVE
        this.graphicsLayer {
            val position = tilt.value
            val amplitude = if (strong) 8.dp.toPx() else 4.dp.toPx()
            translationX = position.x * amplitude
            translationY = position.y * amplitude
            rotationY = position.x * if (strong) 2.2f else 1.1f
            rotationX = -position.y * if (strong) 2.2f else 1.1f
        }
    }
}

/**
 * One quiet backdrop animation per visible main screen. Reads its changing
 * phase in drawBehind so lazy lists are not recomposed at animation frame rate.
 * When policy disables the effect this modifier starts no transition at all.
 */
fun Modifier.tempoAmbientBackdrop(): Modifier = composed {
    val policy = LocalAppearanceRuntimePolicy.current
    if (!policy.allowAmbientAnimation) this
    else {
        val strong = LocalTempoAppearance.current.effectIntensity == AppearanceEffectIntensity.EXPRESSIVE
        val colors = MaterialTheme.colorScheme
        val phase = rememberInfiniteTransition(label = "tempo_ambient")
            .animateFloat(
                initialValue = 0f,
                targetValue = 6.2831855f,
                animationSpec = infiniteRepeatable(
                    tween(if (strong) 13_000 else 24_000, easing = LinearEasing),
                    RepeatMode.Restart,
                ),
                label = "ambient_phase",
            )
        this.drawBehind {
            val a = phase.value
            val radius = size.maxDimension * 0.37f
            val center1 = Offset(
                size.width * (0.21f + 0.12f * cos(a)),
                size.height * (0.18f + 0.07f * sin(a)),
            )
            val center2 = Offset(
                size.width * (0.78f + 0.10f * sin(a)),
                size.height * (0.69f + 0.08f * cos(a)),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(colors.primary.copy(alpha = if (strong) 0.12f else 0.065f),
                        colors.primary.copy(alpha = 0f)),
                    center = center1, radius = radius,
                ),
                radius = radius, center = center1,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(colors.tertiary.copy(alpha = if (strong) 0.10f else 0.05f),
                        colors.tertiary.copy(alpha = 0f)),
                    center = center2, radius = radius,
                ),
                radius = radius, center = center2,
            )
        }
    }
}

/**
 * Optional staggered entrance for actual LazyColumn items. Animations are
 * capped and never infinite. Reading Animatable.value in the render layer avoids
 * frame-by-frame recomposition of album text or image loading.
 */
fun Modifier.tempoListMotion(index: Int): Modifier = composed {
    val policy = LocalAppearanceRuntimePolicy.current
    if (!policy.allowListEntranceMotion) this
    else {
        val strong = LocalTempoAppearance.current.effectIntensity == AppearanceEffectIntensity.EXPRESSIVE
        val animated = remember { Animatable(0f) }
        LaunchedEffect(index, strong) {
            animated.snapTo(0f)
            delay((index.coerceAtLeast(0) % 5) * (if (strong) 37L else 24L))
            animated.animateTo(
                1f,
                animationSpec = tween(
                    durationMillis = if (strong) 390 else 230,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
        this.graphicsLayer {
            alpha = animated.value
            translationY = (1f - animated.value) * (if (strong) 18.dp.toPx() else 9.dp.toPx())
            scaleX = 1f - (1f - animated.value) * (if (strong) 0.035f else 0.012f)
            scaleY = scaleX
        }
    }
}
