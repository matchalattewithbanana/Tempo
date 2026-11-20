package me.avinas.tempo.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import me.avinas.tempo.ui.appearance.AppearanceSettings
import me.avinas.tempo.ui.appearance.AppearancePalette
import me.avinas.tempo.ui.appearance.AppearancePaletteResolver
import me.avinas.tempo.ui.appearance.AppearancePreset
import me.avinas.tempo.ui.appearance.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color


// Dark color scheme mapping
private val DarkColorScheme = darkColorScheme(
    primary = TempoPrimary,
    onPrimary = TextOnAccent,
    primaryContainer = TempoPrimaryDeep,
    onPrimaryContainer = TempoAccent,

    secondary = TempoSecondary,
    onSecondary = TextOnAccent,
    secondaryContainer = Color(0xFF123B32),
    onSecondaryContainer = TempoAccentBright,

    tertiary = TempoPop,
    onTertiary = Color(0xFF28081A),
    tertiaryContainer = TempoPopContainer,
    onTertiaryContainer = OnTempoPopContainer,

    error = TempoError,
    onError = TextOnAccent,
    errorContainer = TempoErrorDeep,
    onErrorContainer = TempoErrorSoft,

    background = TempoDarkBackground,
    onBackground = TextPrimary,
    surface = TempoDarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = TempoDarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = TempoDarkSurfaceSunken,
    surfaceContainerLow = TempoSurfaceChip,
    surfaceContainer = TempoSurfaceCard,
    surfaceContainerHigh = TempoDarkSurfaceElevated,
    surfaceContainerHighest = TempoSurfaceRaised,
    surfaceDim = TempoDarkSurfaceSunken,
    surfaceBright = TempoDarkSurfaceElevated,
    inverseSurface = TextPrimary,
    inverseOnSurface = TempoDarkBackground,
    inversePrimary = TempoPrimaryDeep,
    surfaceTint = TempoPrimary,

    outline = GlassBorderMedium,
    outlineVariant = GlassBorderSoft
)

/** Optional, explicit tokens used by migrated surfaces. Legacy constants remain stable. */
val LocalTempoPalette = staticCompositionLocalOf {
    AppearancePaletteResolver.forPreset(AppearancePreset.ABYSS)
}
val LocalTempoAppearance = staticCompositionLocalOf { AppearanceSettings() }

@Composable
fun TempoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Retained for binary/source compatibility
    appearance: AppearanceSettings = AppearanceSettings(),
    preview: Boolean = false,
    content: @Composable () -> Unit
) {
    val effective = if (preview) appearance else appearance.forLiveApp()
    val context = LocalContext.current
    val basePalette = androidx.compose.runtime.remember(effective.preset, effective.tone, effective.accentSource, effective.customAccent, darkTheme) {
        AppearancePaletteResolver.resolve(effective, darkTheme)
    }
    val palette = if (effective.accentSource == AppearanceAccentSource.WALLPAPER && Build.VERSION.SDK_INT >= 31) {
        val wallpaper = if (basePalette.isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        AppearancePaletteResolver.withAccent(basePalette, wallpaper.primary)
    } else basePalette
    val policy = rememberAppearanceRuntimePolicy(effective)
    val motionScheme = androidx.compose.runtime.remember(policy.reduceMotion) {
        if (policy.reduceMotion) MotionScheme.standard() else MotionScheme.expressive()
    }
    val colorScheme = if (effective.preset == AppearancePreset.ABYSS && effective.tone == AppearanceTone.DARK &&
        (effective.accentSource == AppearanceAccentSource.PRESET || effective.accentSource == AppearanceAccentSource.ARTWORK)) DarkColorScheme else palette.materialColorScheme()

    val view = LocalView.current
    if (!view.isInEditMode && !preview) {
        SideEffect {
            val activity = generateSequence(view.context) { (it as? android.content.ContextWrapper)?.baseContext }
                .filterIsInstance<Activity>().firstOrNull()
            val window = activity?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !palette.isDark
            insetsController.isAppearanceLightNavigationBars = !palette.isDark
        }
    }

    CompositionLocalProvider(
        LocalTempoPalette provides palette,
        LocalTempoAppearance provides effective,
        LocalAppearanceRuntimePolicy provides policy,
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = motionScheme,
            typography = androidx.compose.runtime.remember(effective.fontStyle, effective.boldText) {
                AppearanceTypography.resolve(effective.fontStyle, effective.boldText)
            },
            shapes = androidx.compose.runtime.remember(effective.shape) {
                AppearanceMaterialShapes.resolve(effective.shape)
            },
            content = content,
        )
    }
}
