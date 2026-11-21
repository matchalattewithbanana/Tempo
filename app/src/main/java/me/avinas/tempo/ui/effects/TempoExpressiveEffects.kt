package me.avinas.tempo.ui.effects

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.ui.composed
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
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
        this.drawWithCache {
            val radius = size.maxDimension * 0.37f
            val centerOfBrush = Offset(radius, radius)
            // Brushes are created only when size/colors/intensity change, not
            // every animation frame. The phase is read ONLY in onDrawBehind.
            val primaryBrush = Brush.radialGradient(
                listOf(colors.primary.copy(alpha = if (strong) 0.12f else 0.065f),
                    colors.primary.copy(alpha = 0f)),
                center = centerOfBrush, radius = radius,
            )
            val tertiaryBrush = Brush.radialGradient(
                listOf(colors.tertiary.copy(alpha = if (strong) 0.10f else 0.05f),
                    colors.tertiary.copy(alpha = 0f)),
                center = centerOfBrush, radius = radius,
            )
            onDrawBehind {
                val a = phase.value
                val center1 = Offset(
                    size.width * (0.21f + 0.12f * cos(a)),
                    size.height * (0.18f + 0.07f * sin(a)),
                )
                val center2 = Offset(
                    size.width * (0.78f + 0.10f * sin(a)),
                    size.height * (0.69f + 0.08f * cos(a)),
                )
                withTransform({
                    translate(center1.x - radius, center1.y - radius)
                }) {
                    drawCircle(
                        brush = primaryBrush, radius = radius, center = centerOfBrush,
                    )
                }
                withTransform({
                    translate(center2.x - radius, center2.y - radius)
                }) {
                    drawCircle(
                        brush = tertiaryBrush, radius = radius, center = centerOfBrush,
                    )
                }
            }
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
    if (!policy.allowListEntranceMotion) {
        this
    } else {
        val strong =
            LocalTempoAppearance.current.effectIntensity == AppearanceEffectIntensity.EXPRESSIVE
        val spatial = remember(index) { Animatable(0f) }
        val effects = remember(index) { Animatable(0f) }
        val spatialSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
        val effectsSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()

        LaunchedEffect(index, strong) {
            spatial.snapTo(0f)
            effects.snapTo(0f)
            // No arbitrary stagger: opacity and geometry settle independently
            // using the Material motion scheme and remain interruptible.
            kotlinx.coroutines.coroutineScope {
                launch { spatial.animateTo(1f, animationSpec = spatialSpec) }
                launch { effects.animateTo(1f, animationSpec = effectsSpec) }
            }
        }

        this.graphicsLayer {
            alpha = effects.value
            translationY =
                (1f - spatial.value) * (if (strong) 10.dp.toPx() else 6.dp.toPx())
            val scaleDelta = if (strong) 0.018f else 0.008f
            scaleX = 1f - (1f - spatial.value) * scaleDelta
            scaleY = scaleX
        }
    }
}
