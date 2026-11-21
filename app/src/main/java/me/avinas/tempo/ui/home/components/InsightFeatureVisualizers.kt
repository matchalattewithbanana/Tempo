@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.avinas.tempo.ui.home.components

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
internal fun MoodVisualizer(valence: Float, energy: Float, color: Color) {
    val appearancePalette = LocalTempoPalette.current
    val safeEnergy = energy.coerceIn(0f, 1f)
    val safeValence = valence.coerceIn(0f, 1f)
    val energyPercent = (safeEnergy * 100).toInt()
    val vibePercent = (safeValence * 100).toInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 16 soundwave bars with amplitude and speed tied to energy level
        val barCount = 16
        // This visual describes a recorded mood, not live playback.
        // Data-shaped static heights avoid sixteen perpetual animations.
        val barHeights = remember(safeEnergy) {
            List(barCount) { index ->
                val factor = kotlin.math.sin(index * Math.PI / (barCount - 1))
                    .toFloat().coerceAtLeast(0.25f)
                ((0.35f + safeEnergy * 0.65f) * factor).coerceIn(0.1f, 1f)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(appearancePalette.surfaceSunken.copy(alpha = 0.5f))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            barHeights.forEach { height ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(height.coerceIn(0.1f, 1f))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(color, color.copy(alpha = 0.25f))
                            ),
                            shape = RoundedCornerShape(100.dp)
                        )
                )
            }
        }

        // Calibrated Dual-Metric Meters (Energy & Vibe Positivity) - Two responsive columns
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Energy Balance Cell
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .border(0.6.dp, appearancePalette.border.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ENERGY",
                        style = KickerSmall,
                        color = appearancePalette.mutedText
                    )
                    Text(
                        text = "$energyPercent%",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = color
                    )
                }
                // Calibration track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(safeEnergy)
                            .fillMaxHeight()
                            .background(color, RoundedCornerShape(100.dp))
                    )
                }
            }

            // Vibe Positivity Cell
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .border(0.6.dp, appearancePalette.border.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (safeValence >= 0.5f) "UPBEAT" else "MELLOW",
                        style = KickerSmall,
                        color = appearancePalette.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$vibePercent%",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = appearancePalette.onSurface
                    )
                }
                // Calibration track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(safeValence)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(listOf(color.copy(alpha = 0.6f), Color.White)),
                                RoundedCornerShape(100.dp)
                            )
                    )
                }
            }
        }
    }
}

/**
 * 24-hour listening curve area chart highlighting peak listening hour.
 * Generates a bell-curve falloff around the peak if distribution data is sparse.
 */
@Composable
internal fun PeakTimeVisualizer(peakHour: Int, hourlyDistribution: List<HourlyDistribution>, color: Color) {
    val appearancePalette = LocalTempoPalette.current
    val safePeak = peakHour.coerceIn(0, 23)
    val formattedPeak = remember(safePeak) {
        when {
            safePeak == 0 -> "12 AM"
            safePeak < 12 -> "$safePeak AM"
            safePeak == 12 -> "12 PM"
            else -> "${safePeak - 12} PM"
        }
    }

    // Populate a 24-point array (hours 0..23) so the curve spans the entire day.
    // If sparse (<2 non-zero points), generates a natural Gaussian falloff around safePeak
    val hourlyPlays = remember(hourlyDistribution, safePeak) {
        val array = FloatArray(24) { 0f }
        hourlyDistribution.forEach { item ->
            if (item.hour in 0..23) {
                array[item.hour] = item.playCount.toFloat()
            }
        }
        val nonZero = array.count { it > 0f }
        if (nonZero < 2) {
            for (h in 0..23) {
                val diff = kotlin.math.min(kotlin.math.abs(h - safePeak), 24 - kotlin.math.abs(h - safePeak))
                val bell = kotlin.math.exp(-(diff * diff) / 20.0).toFloat()
                array[h] = 0.08f + 0.92f * bell
            }
        }
        array
    }
    val maxHourlyPlay = remember(hourlyPlays) {
        hourlyPlays.maxOrNull()?.coerceAtLeast(0.01f) ?: 1f
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Smooth Cubic Bézier Curve with Gradient Fill & Glowing Peak Point
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val pointsCount = 24
                val stepX = w / (pointsCount - 1)

                val path = Path()
                val fillPath = Path()

                for (i in 0 until pointsCount) {
                    val x = i * stepX
                    val ratio = (hourlyPlays[i] / maxHourlyPlay).coerceIn(0f, 1f)
                    val y = h - (ratio * h * 0.72f) - (h * 0.08f)

                    if (i == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, h)
                        fillPath.lineTo(x, y)
                    } else {
                        val prevX = (i - 1) * stepX
                        val prevRatio = (hourlyPlays[i - 1] / maxHourlyPlay).coerceIn(0f, 1f)
                        val prevY = h - (prevRatio * h * 0.72f) - (h * 0.08f)

                        val controlX1 = prevX + (stepX / 2f)
                        val controlY1 = prevY
                        val controlX2 = prevX + (stepX / 2f)
                        val controlY2 = y

                        path.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                        fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                    }

                    if (i == pointsCount - 1) {
                        fillPath.lineTo(x, h)
                        fillPath.close()
                    }
                }

                // Area gradient fill below the curve
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(color.copy(alpha = 0.35f), Color.Transparent),
                        startY = 0f,
                        endY = h
                    )
                )

                // Smooth curve stroke
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                // Vertical dashed guide line from x-axis to the peak point
                val peakX = safePeak * stepX
                val peakRatio = (hourlyPlays[safePeak] / maxHourlyPlay).coerceIn(0f, 1f)
                val peakY = h - (peakRatio * h * 0.72f) - (h * 0.08f)

                drawLine(
                    color = color.copy(alpha = 0.5f),
                    start = Offset(peakX, h),
                    end = Offset(peakX, peakY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )

                // Glowing concentric peak coordinate point
                drawCircle(
                    color = color.copy(alpha = 0.30f),
                    radius = 11.dp.toPx(),
                    center = Offset(peakX, peakY)
                )
                drawCircle(
                    color = color,
                    radius = 5.dp.toPx(),
                    center = Offset(peakX, peakY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.5.dp.toPx(),
                    center = Offset(peakX, peakY)
                )
            }
        }

        // Timeline markers & Peak Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("12 AM", style = KickerSmall, color = appearancePalette.mutedText)
            Text("6 AM", style = KickerSmall, color = appearancePalette.mutedText)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(color.copy(alpha = 0.12f))
                    .border(0.6.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 2.5.dp)
            ) {
                Box(modifier = Modifier.size(4.dp).background(color, CircleShape))
                Text("PEAK // $formattedPeak", style = KickerSmall, color = color)
            }
            Text("6 PM", style = KickerSmall, color = appearancePalette.mutedText)
            Text("11 PM", style = KickerSmall, color = appearancePalette.mutedText)
        }
    }
}

/**
 * Visualizes continuous playback sessions with a single artist, showing play count and session duration.
 */
@Composable
internal fun BingeVisualizer(
    artist: String,
    playCount: Int,
    durationMs: Long,
    color: Color,
    onArtistClick: ((String) -> Unit)? = null
) {
    val appearancePalette = LocalTempoPalette.current
    val safeArtist = artist.ifBlank { "Top Artist" }
    val safeCount = playCount.coerceAtLeast(1)
    val timeFormatted = if (durationMs <= 0L) "Active Session"
        else me.avinas.tempo.ui.stats.displayStatsDuration(durationMs)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Artist Identity Header Row with responsive text truncation
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.18f))
                        .border(1.dp, color.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Face,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = safeArtist,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = appearancePalette.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (onArtistClick != null && artist.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(color.copy(alpha = 0.12f))
                        .border(0.6.dp, color.copy(alpha = 0.3f), RoundedCornerShape(100.dp))
                        .clickable { onArtistClick(artist) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "EXPLORE",
                        style = KickerSmall,
                        color = color
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(8.dp)
                    )
                }
            }
        }

        // Live Rhythmic Playback Wave
        val barCount = 18
        // Decorative history illustration: stable rhythm instead of
        // one live animation state per bar.
        val barHeights = remember {
            List(barCount) { index ->
                (0.16f + ((index * 43 + 11) % 76) / 100f).coerceIn(0.12f, 0.95f)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(appearancePalette.surfaceSunken.copy(alpha = 0.4f))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.5.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            barHeights.forEach { height ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(height)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(color, color.copy(alpha = 0.25f))
                            ),
                            shape = RoundedCornerShape(100.dp)
                        )
                )
            }
        }

        // Dual Telemetry Cards Row (Plays in a row + Session length)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .border(0.6.dp, appearancePalette.border.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "STREAK PLAYS",
                    style = KickerSmall,
                    color = appearancePalette.mutedText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$safeCount IN A ROW",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .border(0.6.dp, appearancePalette.border.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "SESSION TIME",
                    style = KickerSmall,
                    color = appearancePalette.mutedText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = appearancePalette.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Visualizes artist discoveries, rendering a trajectory curve for historical trends
 * or a radar view when trend points are limited.
 */
@Composable
internal fun DiscoveryVisualizer(newArtistsCount: Int, trends: List<DiscoveryTrend>, color: Color) {
    val appearancePalette = LocalTempoPalette.current
    val safeCount = newArtistsCount.coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Discovery Telemetry Headline
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "+$safeCount",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp
                    ),
                    color = color
                )
                Text(
                    text = "NEW ARTISTS",
                    style = KickerSmall,
                    color = appearancePalette.onSurface
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(color.copy(alpha = 0.12f))
                    .border(0.6.dp, color.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Box(modifier = Modifier.size(4.dp).background(color, CircleShape))
                Text(
                    text = "EXPANDING CATALOG",
                    style = KickerSmall,
                    color = color
                )
            }
        }

        if (trends.size >= 2) {
            // Trend curve when historical data exists
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val pointsCount = trends.size
                    val maxDiscoveries = trends.maxOfOrNull { it.new_artists_count }?.coerceAtLeast(1) ?: 1
                    val stepX = w / (pointsCount - 1)

                    val path = Path()
                    val fillPath = Path()

                    trends.forEachIndexed { index, trend ->
                        val x = index * stepX
                        val ratio = trend.new_artists_count.toFloat() / maxDiscoveries
                        val y = h - (ratio * h * 0.7f) - (h * 0.08f)

                        if (index == 0) {
                            path.moveTo(x, y)
                            fillPath.moveTo(x, h)
                            fillPath.lineTo(x, y)
                        } else {
                            val prevX = (index - 1) * stepX
                            val prevRatio = trends[index - 1].new_artists_count.toFloat() / maxDiscoveries
                            val prevY = h - (prevRatio * h * 0.7f) - (h * 0.08f)

                            val controlX1 = prevX + (stepX / 2f)
                            val controlY1 = prevY
                            val controlX2 = prevX + (stepX / 2f)
                            val controlY2 = y

                            path.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                            fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                        }

                        if (index == pointsCount - 1) {
                            fillPath.lineTo(x, h)
                            fillPath.close()
                        }
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(color.copy(alpha = 0.28f), Color.Transparent),
                            startY = 0f,
                            endY = h
                        )
                    )

                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )

                    trends.forEachIndexed { index, trend ->
                        val x = index * stepX
                        val ratio = trend.new_artists_count.toFloat() / maxDiscoveries
                        val y = h - (ratio * h * 0.7f) - (h * 0.08f)

                        drawCircle(
                            color = Color.White,
                            radius = 2.5.dp.toPx(),
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = color,
                            radius = 4.dp.toPx(),
                            center = Offset(x, y),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }
            }

            // Recent months labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val displayTrends = if (trends.size > 5) trends.takeLast(5) else trends
                displayTrends.forEach { trend ->
                    val displayMonth = try {
                        val parts = trend.month.split("-")
                        val monthInt = parts[1].toInt()
                        java.time.Month.of(monthInt).getDisplayName(
                            java.time.format.TextStyle.SHORT,
                            java.util.Locale.ENGLISH
                        ).uppercase()
                    } catch (e: Exception) {
                        trend.month.takeLast(3).uppercase()
                    }
                    Text(
                        text = displayMonth,
                        style = KickerSmall,
                        color = appearancePalette.mutedText
                    )
                }
            }
        } else {
            // Discovery Intake Constellation Fallback (Guaranteed never blank for 0 or 1 data point)
            // A static constellation means "little data so far"; it does not
            // falsely suggest an active radar scan.
            val orbitAngle = 0f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(appearancePalette.surfaceSunken.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2
                    val cy = size.height / 2
                    val maxR = size.height * 0.42f

                    // Concentric radar orbits
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        radius = maxR * 0.5f,
                        center = Offset(cx, cy),
                        style = Stroke(1.dp.toPx())
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.08f),
                        radius = maxR,
                        center = Offset(cx, cy),
                        style = Stroke(1.dp.toPx())
                    )

                    // Orbiting discovery spark nodes
                    val nodeCount = 4
                    for (i in 0 until nodeCount) {
                        val angleRad = Math.toRadians((orbitAngle + i * (360.0 / nodeCount)).toDouble())
                        val r = maxR * (0.6f + (i % 2) * 0.35f)
                        val nx = (cx + r * kotlin.math.cos(angleRad)).toFloat()
                        val ny = (cy + r * kotlin.math.sin(angleRad)).toFloat()

                        drawCircle(
                            color = color.copy(alpha = 0.35f),
                            radius = 6.dp.toPx(),
                            center = Offset(nx, ny)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.5.dp.toPx(),
                            center = Offset(nx, ny)
                        )
                    }

                    // Center radar core
                    drawCircle(
                        color = color,
                        radius = 4.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                }

                Text(
                    text = "Actively mapping new sounds to your library",
                    style = KickerSmall,
                    color = appearancePalette.mutedText,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
                )
            }
        }
    }
}

/**
 * Visualizes rhythm and beats per minute with an animated pulse wave and tempo label.
 */
@Composable
internal fun TempoBPMVisualizer(bpm: Float, color: Color) {
    val appearancePalette = LocalTempoPalette.current
    val safeBpm = bpm.coerceIn(40f, 240f)
    // BPM is a recorded attribute, not a running timer. A static metronome
    // mark avoids two continuous animation timelines in the insight feed.
    val tempoTag = remember(safeBpm) {
        when {
            safeBpm >= 140 -> "FAST // PRESTO"
            safeBpm >= 120 -> "UPBEAT // ALLEGRO"
            safeBpm >= 100 -> "GROOVE // MODERATO"
            safeBpm >= 80 -> "WALKING // ANDANTE"
            else -> "RELAXED // ADAGIO"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(52.dp),
            contentAlignment = Alignment.Center
        ) {
            // Fixed outer ring. Only actual live playback should need periodic updates.
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(color.copy(alpha = 0.18f), CircleShape)
            )
            // Center vinyl pip
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(appearancePalette.surfaceSunken)
                    .border(2.dp, color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(MaterialTheme.colorScheme.onSurface, CircleShape)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${safeBpm.toInt()}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = appearancePalette.onSurface
                )
                Text(
                    text = "BPM",
                    style = KickerSmall,
                    color = color,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Box(
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = tempoTag,
                        style = TextStyle(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = MaterialTheme.typography.labelSmall.fontSize,
                            letterSpacing = 0.5.sp
                        ),
                        color = color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = stringResource(R.string.tempo_bpm_hint),
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
 * 6. Streak Visualizer:
 * Visualizes daily listening consistency with flame animation and 7-dot weekly habit matrix.
 */
