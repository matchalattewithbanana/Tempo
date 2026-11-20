package me.avinas.tempo.ui.appearance

import android.animation.ValueAnimator
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Pure policy, independently testable without a renderer or Android device. */
data class AppearanceRuntimePolicy(
    val reduceMotion: Boolean = false,
    val solidSurfaces: Boolean = false,
    val allowArtworkAccent: Boolean = true,
    val allowArtworkBackdrop: Boolean = true,
    val allowAmbientAnimation: Boolean = false,
    val allowSensorTilt: Boolean = false,
    val allowListEntranceMotion: Boolean = false,
) {
    companion object {
        fun resolve(
            settings: AppearanceSettings,
            systemAnimationsEnabled: Boolean,
            powerSave: Boolean,
            lowRam: Boolean,
            thermalLimited: Boolean,
            foreground: Boolean,
        ): AppearanceRuntimePolicy {
            val constrained = powerSave || thermalLimited || !foreground || (settings.adaptiveEffects && lowRam)
            // User-facing "adaptive" can relax static visual compromises,
            // never the hard safety gate for continuous/sensor/list effects.
            val extrasSafe = systemAnimationsEnabled && !settings.reduceMotion &&
                !powerSave && !thermalLimited && !lowRam && foreground
            return AppearanceRuntimePolicy(
                reduceMotion = settings.reduceMotion || !systemAnimationsEnabled || constrained,
                solidSurfaces = settings.surface == AppearanceSurface.SOLID || constrained,
                allowArtworkAccent = !constrained,
                allowArtworkBackdrop = !constrained && settings.surface != AppearanceSurface.SOLID,
                allowAmbientAnimation = settings.ambientAnimation && extrasSafe,
                allowSensorTilt = settings.sensorTilt && extrasSafe,
                allowListEntranceMotion = settings.listEntranceMotion && extrasSafe,
            )
        }
    }
}
val LocalAppearanceRuntimePolicy = staticCompositionLocalOf { AppearanceRuntimePolicy() }

@Composable
fun rememberAppearanceRuntimePolicy(settings: AppearanceSettings): AppearanceRuntimePolicy {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val power = remember(context) { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
    val lowRam = remember(context) {
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).isLowRamDevice
    }
    var powerSave by remember { mutableStateOf(power.isPowerSaveMode) }
    var animations by remember { mutableStateOf(ValueAnimator.areAnimatorsEnabled()) }
    var thermal by remember { mutableStateOf(false) }
    var foreground by remember(owner) { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    DisposableEffect(context, owner, power) {
        fun refresh() {
            powerSave = power.isPowerSaveMode
            animations = ValueAnimator.areAnimatorsEnabled()
            foreground = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            if (Build.VERSION.SDK_INT >= 29) thermal = power.currentThermalStatus >= PowerManager.THERMAL_STATUS_MODERATE
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) = refresh()
        }
        ContextCompat.registerReceiver(context, receiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
        val observer = LifecycleEventObserver { _, _ -> refresh() }
        owner.lifecycle.addObserver(observer)
        refresh()
        onDispose {
            owner.lifecycle.removeObserver(observer)
            context.unregisterReceiver(receiver)
        }
    }
    if (Build.VERSION.SDK_INT >= 29) {
        DisposableEffect(power) {
            val listener = PowerManager.OnThermalStatusChangedListener { status ->
                thermal = status >= PowerManager.THERMAL_STATUS_MODERATE
            }
            power.addThermalStatusListener(context.mainExecutor, listener)
            onDispose { power.removeThermalStatusListener(listener) }
        }
    }
    return AppearanceRuntimePolicy.resolve(settings, animations, powerSave, lowRam, thermal, foreground)
}
