package me.avinas.tempo.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.avinas.tempo.R
import me.avinas.tempo.ui.components.DeepOceanBackground
import me.avinas.tempo.ui.theme.TempoPrimary
import me.avinas.tempo.ui.theme.TextOnAccent
import me.avinas.tempo.ui.theme.rememberReducedMotion
import me.avinas.tempo.ui.utils.adaptiveSizeByCategory
import me.avinas.tempo.ui.utils.adaptiveTextUnitByCategory
import me.avinas.tempo.ui.utils.rememberScreenHeightPercentage
import me.avinas.tempo.ui.utils.scaledSize

/**
 * Welcome onboarding screen in three layers:
 *   back · native revolving notes around the head,
 *   mid  · welcome.png fullscreen art,
 *   top  · headline + CTA overlaid on the image.
 * Entry plays once as one foreground hero transition. Decorative artwork
 * remains stable, and reduced-motion settings skip the transition entirely.
 */
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onSkip: () -> Unit,
) {
    val reducedMotion = rememberReducedMotion()
    val haptic = LocalHapticFeedback.current

    // One onboarding hero transition: content moves with the spatial spring
    // while opacity uses the effects spring. No hand-authored stagger or tween.
    val contentSpatial = remember { Animatable(0f) }
    val contentEffects = remember { Animatable(0f) }
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            contentSpatial.snapTo(1f)
            contentEffects.snapTo(1f)
        } else {
            kotlinx.coroutines.coroutineScope {
                launch { contentSpatial.animateTo(1f, animationSpec = spatialSpec) }
                launch { contentEffects.animateTo(1f, animationSpec = effectsSpec) }
            }
        }
    }

    // Full-bleed: the art owns the whole screen, copy sits on top.
    DeepOceanBackground(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Backdrop remains stable; only the foreground content performs the
        // first-run hero transition.
        WelcomeBackdrop(modifier = Modifier.fillMaxSize())

        // ── TOP — legibility scrim + headline and call to action ──
        // Gentle bottom blend so the copy melts into the artwork.
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        brush =
                            Brush.verticalGradient(
                                0.55f to Color.Transparent,
                                1f to Color(0xFF0A0E0E).copy(alpha = 0.72f),
                            ),
                    ),
        )
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = adaptiveSizeByCategory(24.dp, 22.dp, 20.dp))
                    .padding(bottom = rememberScreenHeightPercentage(0.03f)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val headlineSize = adaptiveTextUnitByCategory(32.sp, 29.sp, 26.sp)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = contentEffects.value
                    translationY = (1f - contentSpatial.value) * 18f
                }
            ) {
                Text(
                    text = stringResource(R.string.welcome_headline_1),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = headlineSize,
                    textAlign = TextAlign.Center,
                    lineHeight = adaptiveTextUnitByCategory(38.sp, 35.sp, 32.sp),
                )
                Text(
                    text = stringResource(R.string.welcome_headline_2),
                    style =
                        MaterialTheme.typography.headlineMedium.copy(
                            brush = Brush.linearGradient(listOf(Color.White, TempoPrimary)),
                        ),
                    fontWeight = FontWeight.Bold,
                    fontSize = headlineSize,
                    textAlign = TextAlign.Center,
                    lineHeight = adaptiveTextUnitByCategory(38.sp, 35.sp, 32.sp),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onGetStarted()
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = contentEffects.value
                            translationY = (1f - contentSpatial.value) * 18f
                        }
                        .height(scaledSize(54.dp, 0.85f, 1.1f)),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = TempoPrimary,
                        contentColor = TextOnAccent,
                    ),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Text(
                    text = stringResource(R.string.welcome_get_started),
                    fontSize = adaptiveTextUnitByCategory(18.sp, 17.sp, 16.sp),
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Skip — rendered LAST to sit on top (z-ordering in Box)
        TextButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSkip()
            },
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(adaptiveSizeByCategory(16.dp, 14.dp, 12.dp)),
        ) {
            Text(
                text = stringResource(R.string.welcome_skip),
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
