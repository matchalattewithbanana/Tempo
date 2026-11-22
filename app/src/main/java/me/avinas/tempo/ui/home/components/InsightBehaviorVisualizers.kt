@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.avinas.tempo.ui.home.components
import me.avinas.tempo.ui.util.observableLocale

import androidx.compose.animation.core.*
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextOverflow
import me.avinas.tempo.data.stats.InsightCardData
import me.avinas.tempo.data.stats.InsightType
import me.avinas.tempo.data.stats.InsightPayload
import me.avinas.tempo.data.stats.DiscoveryTrend
import me.avinas.tempo.data.stats.HourlyDistribution
import me.avinas.tempo.ui.components.GlassCard
import me.avinas.tempo.ui.components.GlassCardVariant
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.res.stringResource
import me.avinas.tempo.R
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.ContentScale
import me.avinas.tempo.ui.components.CachedAsyncImage
import me.avinas.tempo.ui.components.FrostedIconButton
import me.avinas.tempo.ui.theme.*


@Composable
internal fun StreakVisualizer(days: Int) {
    val appearancePalette = LocalTempoPalette.current
    val safeDays = days.coerceAtLeast(1)
        val fireColor1 = InsightStreak
    val fireColor2 = InsightEnergy
    val fireColor3 = TempoWarningBright

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Canvas(
            modifier = Modifier.size(46.dp)
        ) {
            val w = size.width
            val h = size.height

            val path = Path().apply {
                moveTo(w * 0.5f, h * 0.05f)
                cubicTo(w * 0.72f, h * 0.28f, w * 0.95f, h * 0.55f, w * 0.8f, h * 0.8f)
                cubicTo(w * 0.7f, h * 0.95f, w * 0.3f, h * 0.95f, w * 0.2f, h * 0.8f)
                cubicTo(w * 0.05f, h * 0.55f, w * 0.28f, h * 0.28f, w * 0.5f, h * 0.05f)
                close()
            }

            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(fireColor3, fireColor1, fireColor2),
                    startY = 0f,
                    endY = h
                )
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$safeDays DAYS IN A ROW",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = fireColor1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 7-Dot Habit Momentum Matrix (Past 7 days)
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val filledDots = safeDays.coerceIn(1, 7)
                for (i in 1..7) {
                    val isFilled = i <= filledDots
                    Box(
                        modifier = Modifier
                            .size(width = 12.dp, height = 5.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(
                                if (isFilled) fireColor1 else Color.White.copy(alpha = 0.12f)
                            )
                    )
                }
            }

            Text(
                text = stringResource(R.string.streak_hint),
                style = KickerSmall,
                color = appearancePalette.mutedText,
                maxLines = 2,
                softWrap = true,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Renders the top music genre alongside a simulated audio frequency spectrum.
 */
@Composable
internal fun GenreVisualizer(genre: String, color: Color) {
    val appearancePalette = LocalTempoPalette.current
    val safeGenre = genre.ifBlank { "Eclectic Sound" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PRIMARY SOUND PROFILE",
                style = KickerSmall,
                color = appearancePalette.mutedText
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(modifier = Modifier.size(4.dp).background(color, CircleShape))
                Text(
                    text = "HEAVY ROTATION",
                    style = KickerSmall,
                    color = color
                )
            }
        }

        // Genre Headline with clean ellipsis protection
        Text(
            text = safeGenre.uppercase(observableLocale()),
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = 1.0.sp
            ),
            color = appearancePalette.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Frequency Spectrum Wave
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(appearancePalette.surfaceSunken.copy(alpha = 0.4f))
                .padding(horizontal = 6.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.5.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            val bars = 20
            for (i in 0 until bars) {
                val heightFraction = (0.25f + 0.75f * kotlin.math.sin(i * Math.PI / (bars - 1)).toFloat()).coerceIn(0.2f, 1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(heightFraction)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(color, color.copy(alpha = 0.25f))
                            ),
                            shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                        )
                )
            }
        }
    }
}

/**
 * Horizontal gauge displaying track audio attributes such as energy or danceability.
 */
@Composable
internal fun FeatureGaugeVisualizer(value: Float, color: Color, label: String) {
    val appearancePalette = LocalTempoPalette.current
    val safeValue = value.coerceIn(0f, 1f)
    // Present accurate stat levels on the first frame; no artificial reveal delay.
    val displayedValue = safeValue

    val percentage = (safeValue * 100).toInt()
    val levelDescription = when {
        safeValue >= 0.75f -> "HIGH INTENSITY"
        safeValue >= 0.45f -> "MODERATE"
        else -> "SUBTLE"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label.uppercase(observableLocale()),
                style = KickerSmall,
                color = appearancePalette.mutedText
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = levelDescription,
                    style = KickerSmall,
                    color = color
                )
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = appearancePalette.onSurface
                )
            }
        }

        // Calibrated horizontal track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(displayedValue)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(color.copy(alpha = 0.5f), color)
                        ),
                        RoundedCornerShape(100.dp)
                    )
            )
        }

        // Calibration scale ticks
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0% LOW", style = MaterialTheme.typography.labelSmall, color = appearancePalette.mutedText.copy(alpha = 0.6f))
            Text("50% MODERATE", style = MaterialTheme.typography.labelSmall, color = appearancePalette.mutedText.copy(alpha = 0.6f))
            Text("100% HIGH", style = MaterialTheme.typography.labelSmall, color = appearancePalette.mutedText.copy(alpha = 0.6f))
        }
    }
}

/**
 * Circular progress ring displaying listener engagement or completion rates.
 */
@Composable
internal fun EngagementVisualizer(value: Float, color: Color) {
    val appearancePalette = LocalTempoPalette.current
    val isSkipRate = value < 0f
    val absValue = kotlin.math.abs(value).coerceIn(0f, 1f)
    val percentage = (absValue * 100).toInt()
    val displayedValue = absValue

    val archetypeTitle = if (isSkipRate) "CURATOR MODE" else "COMPLETIONIST"
    val archetypeSub = if (isSkipRate) "Skip Rate Focus" else "Track Completion"
    val descriptor = if (isSkipRate) {
        "You skip $percentage% of songs to find the exact vibe you want."
    } else {
        "You finish $percentage% of the songs you start. A true album listener."
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Circular ring progress gauge
        Box(
            modifier = Modifier.size(86.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val strokeWidthPx = 6.5.dp.toPx()
                val radius = (w - strokeWidthPx) / 2f
                val cx = w / 2f
                val cy = h / 2f

                // Track ring
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = radius,
                    style = Stroke(width = strokeWidthPx)
                )

                // Animated Sweep Arc
                val sweep = displayedValue * 360f
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(color.copy(alpha = 0.45f), color, color)
                    ),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2),
                    size = Size(w - strokeWidthPx, h - strokeWidthPx),
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )

                // Glowing Tip Pip at the end of the arc
                if (displayedValue > 0.02f) {
                    val endAngleDeg = -90f + sweep
                    val endAngleRad = Math.toRadians(endAngleDeg.toDouble())
                    val tipX = (cx + radius * kotlin.math.cos(endAngleRad)).toFloat()
                    val tipY = (cy + radius * kotlin.math.sin(endAngleRad)).toFloat()

                    drawCircle(
                        color = color.copy(alpha = 0.35f),
                        radius = 6.dp.toPx(),
                        center = Offset(tipX, tipY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx(),
                        center = Offset(tipX, tipY)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = appearancePalette.onSurface
                )
            }
        }

        // Contextual Archetype Narrative
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(color))
                Text(
                    text = archetypeTitle,
                    style = KickerSmall,
                    color = color
                )
            }
            Text(
                text = archetypeSub,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = appearancePalette.onSurface
            )
            Text(
                text = descriptor,
                style = KickerSmall,
                color = appearancePalette.mutedText,
                maxLines = 2,
                softWrap = true,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Animated waveform fallback for cards without a specific visualizer.
 */
@Composable
internal fun FallbackVisualizer(color: Color) {
    val appearancePalette = LocalTempoPalette.current
    val barCount = 14
    val barHeights = remember {
        List(barCount) { index ->
            (0.2f + ((index * 29 + 17) % 63) / 100f).coerceIn(0.2f, 0.85f)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(appearancePalette.surfaceSunken.copy(alpha = 0.4f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        barHeights.forEach { height ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(height)
                    .background(
                        brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.6f), color.copy(alpha = 0.15f))),
                        shape = RoundedCornerShape(100.dp)
                    )
            )
        }
    }
}

