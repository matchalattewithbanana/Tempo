package me.avinas.tempo.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.animation.core.snap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.ui.composed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Honors in-app reduction and the OS accessibility setting, including changes on resume. */
@Composable
fun rememberReducedMotion(): Boolean {
    val view = LocalView.current
    val owner = LocalLifecycleOwner.current
    val userReduced = LocalTempoAppearance.current.reduceMotion
    fun systemReduced(): Boolean =
        android.provider.Settings.Global.getFloat(
            view.context.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    var systemReducedMotion by remember(view) { mutableStateOf(systemReduced()) }
    DisposableEffect(owner, view) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) systemReducedMotion = systemReduced()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return userReduced || systemReducedMotion || me.avinas.tempo.ui.appearance.LocalAppearanceRuntimePolicy.current.reduceMotion
}

fun Modifier.innerShadow(
    color: Color = Color.Black,
    cornersRadius: Dp = 0.dp,
    spread: Dp = 0.dp,
    blur: Dp = 0.dp,
    offsetY: Dp = 0.dp,
    offsetX: Dp = 0.dp
): Modifier = drawWithCache {
    val shadowColor = color.toArgb()
    val transparentColor = color.copy(alpha = 0f).toArgb()
    val paint = Paint()
    val frameworkPaint = paint.asFrameworkPaint()
    frameworkPaint.color = transparentColor
    frameworkPaint.setShadowLayer(
        blur.toPx(),
        offsetX.toPx(),
        offsetY.toPx(),
        shadowColor
    )
    val radiusPx = cornersRadius.toPx()

    onDrawBehind {
        drawIntoCanvas { canvas ->
            canvas.drawRoundRect(
                left = 0f,
                top = 0f,
                right = size.width,
                bottom = size.height,
                radiusX = radiusPx,
                radiusY = radiusPx,
                paint = paint
            )
        }
    }
}

fun Modifier.premiumClickable(
    onClick: () -> Unit,
    pressedScale: Float = 0.98f
): Modifier = composed {
    val interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val reducedMotion = rememberReducedMotion()
    val spatial = MaterialTheme.motionScheme.fastSpatialSpec<Float>()

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (reducedMotion) 1f else when {
            isPressed -> pressedScale
            isFocused -> 1.015f
            else -> 1f
        },
        animationSpec = if (reducedMotion) snap() else spatial,
        label = "premium_click_scale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(),
            onClick = onClick
        )
}

fun Modifier.glassBlur(
    radius: Dp = 16.dp,
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
): Modifier = composed {
    val policy = me.avinas.tempo.ui.appearance.LocalAppearanceRuntimePolicy.current
    this.clip(shape)
    .then(
        if (!policy.solidSurfaces && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            Modifier.graphicsLayer {
                val blurRadius = radius.toPx()
                if (blurRadius > 0) {
                    renderEffect = android.graphics.RenderEffect
                        .createBlurEffect(blurRadius, blurRadius, android.graphics.Shader.TileMode.CLAMP)
                        .asComposeRenderEffect()
                }
            }
        } else {
            Modifier
        }
    )

}
