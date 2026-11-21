@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.avinas.tempo.ui.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.palette.graphics.Palette
import coil3.BitmapImage
import coil3.imageLoader
import coil3.request.ImageRequest
import me.avinas.tempo.data.stats.HourlyDistribution
import me.avinas.tempo.data.stats.ListeningOverview
import me.avinas.tempo.data.stats.PeriodComparison
import me.avinas.tempo.data.stats.TimeRange
import me.avinas.tempo.data.stats.TopArtist
import me.avinas.tempo.data.stats.TopTrack
import me.avinas.tempo.ui.appearance.LocalAppearanceRuntimePolicy
import me.avinas.tempo.ui.components.ArtAtmosphereLayer
import me.avinas.tempo.ui.components.CachedAsyncImage
import me.avinas.tempo.ui.components.FrostedIconButton
import me.avinas.tempo.ui.components.GlassCard
import me.avinas.tempo.ui.components.GlassCardVariant
import me.avinas.tempo.ui.components.SectionCatalogKicker
import me.avinas.tempo.ui.details.conditionedAccent
import me.avinas.tempo.ui.profile.readableProfileColor
import me.avinas.tempo.ui.stats.StatsShareDialog
import me.avinas.tempo.ui.stats.StatsTab
import me.avinas.tempo.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun MastheadSecondaryStat(
    label: String,
    value: String,
    subtext: String,
    modifier: Modifier = Modifier,
    valueColor: Color = LocalTempoPalette.current.onSurface
) {
    Column(
        modifier = modifier.padding(horizontal = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = KickerSmall,
            color = todayMutedText(),
            maxLines = 2,
        )
        // The setting-controlled duration can be longer than a count. Keep the
        // full value readable instead of concatenating it with its caption
        // and ellipsizing the whole line on compact devices.
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = DisplayFontFamily),
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 3,
            softWrap = true,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtext,
            style = MaterialTheme.typography.labelSmall,
            color = todayMutedText(),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Comparison bar showing today's listening time relative to yesterday. */
@Composable
internal fun TodayVsYesterdayBar(
    periodComparison: PeriodComparison?,
    accent: Color
) {
    if (periodComparison == null) return
    val currentMs = periodComparison.currentPeriodTimeMs.coerceAtLeast(0L)
    val previousMs = periodComparison.previousPeriodTimeMs.coerceAtLeast(0L)
    val totalMs = currentMs + previousMs
    if (totalMs <= 0L) return

    val todayFraction = (currentMs.toFloat() / totalMs.toFloat()).coerceIn(0.04f, 0.96f)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(todayFraction)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(accent.copy(alpha = 0.75f), accent)
                        )
                    )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Text(
                    text = "TODAY · ${me.avinas.tempo.ui.stats.displayStatsDuration(currentMs)}",
                    style = KickerSmall,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.7.sp,
                    color = accent
                )
            }

            Text(
                text = "YESTERDAY · ${me.avinas.tempo.ui.stats.displayStatsDuration(previousMs)}",
                style = KickerSmall,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.7.sp,
                color = todayMutedText()
            )
        }
    }
}

@Composable
internal fun DayOverDayChip(
    periodComparison: PeriodComparison?,
    accent: Color
) {
    val change = periodComparison?.timeChangePercent ?: return
    when {
        change > 0.5 -> TrendChip(
            iconVector = Icons.AutoMirrored.Rounded.TrendingUp,
            text = "+${change.roundToInt()}% vs yesterday",
            color = accent
        )
        change < -0.5 -> TrendChip(
            iconVector = Icons.AutoMirrored.Rounded.TrendingDown,
            text = "-${abs(change.roundToInt())}% vs yesterday",
            color = todayMutedText()
        )
        else -> TrendChip(
            iconVector = Icons.AutoMirrored.Rounded.TrendingFlat,
            text = "Same as yesterday",
            color = todayMutedText()
        )
    }
}

@Composable
internal fun TrendChip(iconVector: ImageVector, text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(GlassFrostSoft)
            .border(0.5.dp, GlassBorderSoft, RoundedCornerShape(100.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = iconVector,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            maxLines = 1,
            softWrap = false
        )
    }
}

// ──────────────────────────────────────────────────────────────
// 4. Section 01: Listening Rhythm & Hourly Cadence
// ──────────────────────────────────────────────────────────────

@Composable
internal fun PeakHoursCard(
    hourlyDistribution: List<HourlyDistribution>,
    accent: Color
) {
    val peak = remember(hourlyDistribution) {
        hourlyDistribution.filter { it.playCount > 0 }.maxByOrNull { it.playCount }
    }
    val isDay = peak?.let { it.hour in 6..17 } ?: true

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                SectionCatalogKicker(
                    number = "01",
                    label = "LISTENING RHYTHM"
                )

                if (peak != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(accent.copy(alpha = 0.12f))
                            .border(0.5.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = if (isDay) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "${formatHourLabel(peak.hour)} · ${peak.playCount} ${if (peak.playCount == 1) "play" else "plays"}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accent
                        )
                    }
                }
            }

            TodaysHoursChart(
                hourlyDistribution = hourlyDistribution,
                accent = accent,
                peakHour = peak?.hour
            )

            // Day Quadrant Breakdown (Night, Morning, Afternoon, Evening)
            DayQuadrantRow(
                hourlyDistribution = hourlyDistribution,
                accent = accent
            )
        }
    }
}

/** 24-hour play volume distribution chart with current hour highlight & peak elevation. */
@Composable
internal fun TodaysHoursChart(
    hourlyDistribution: List<HourlyDistribution>,
    accent: Color,
    peakHour: Int?
) {
    val currentHour = remember { LocalTime.now().hour }
    val maxPlays = remember(hourlyDistribution) {
        hourlyDistribution.maxOfOrNull { it.playCount }?.coerceAtLeast(1) ?: 1
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            (0..23).forEach { hour ->
                val playCount = hourlyDistribution.find { it.hour == hour }?.playCount ?: 0
                val heightPercent = (playCount.toFloat() / maxPlays).coerceIn(0.06f, 1f)
                val isCurrentHour = hour == currentHour
                val isPeak = peakHour != null && hour == peakHour && playCount > 0

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (playCount > 0) Modifier.fillMaxHeight(heightPercent)
                            else Modifier.height(2.5.dp)
                        )
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(
                            when {
                                isPeak -> accent
                                isCurrentHour && playCount > 0 -> TempoAccentBright
                                isCurrentHour -> accent.copy(alpha = 0.45f)
                                playCount > 0 -> {
                                    val alpha = 0.45f + (0.50f * (playCount.toFloat() / maxPlays))
                                    accent.copy(alpha = alpha)
                                }
                                else -> Color.White.copy(alpha = 0.07f)
                            }
                        )
                )
            }
        }

        // 6-hour axis ticks
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            listOf("12 AM", "6 AM", "12 PM", "6 PM").forEachIndexed { index, label ->
                Text(
                    text = label,
                    style = KickerSmall,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = todayMutedText(),
                    letterSpacing = 0.4.sp,
                    textAlign = if (index == 0) TextAlign.Start else TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Compact 4-cell row showing listening volume distributed across the 4 day quadrants. */
@Composable
internal fun DayQuadrantRow(
    hourlyDistribution: List<HourlyDistribution>,
    accent: Color
) {
    val nightPlays = remember(hourlyDistribution) {
        hourlyDistribution.filter { it.hour in 0..5 }.sumOf { it.playCount }
    }
    val morningPlays = remember(hourlyDistribution) {
        hourlyDistribution.filter { it.hour in 6..11 }.sumOf { it.playCount }
    }
    val afternoonPlays = remember(hourlyDistribution) {
        hourlyDistribution.filter { it.hour in 12..17 }.sumOf { it.playCount }
    }
    val eveningPlays = remember(hourlyDistribution) {
        hourlyDistribution.filter { it.hour in 18..23 }.sumOf { it.playCount }
    }

    val quadrants = listOf(
        QuadrantData("NIGHT", "12A-6A", nightPlays),
        QuadrantData("MORNING", "6A-12P", morningPlays),
        QuadrantData("AFTERNOON", "12P-6P", afternoonPlays),
        QuadrantData("EVENING", "6P-12A", eveningPlays)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GlassFrostSoft)
            .border(0.5.dp, GlassBorderSoft, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        quadrants.forEach { q ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = q.label,
                    style = KickerSmall,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = todayMutedText()
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${q.plays}",
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = DisplayFontFamily),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (q.plays > 0) LocalTempoPalette.current.onSurface else todayMutedText()
                )
            }
        }
    }
}

private data class QuadrantData(val label: String, val window: String, val plays: Int)

// ──────────────────────────────────────────────────────────────
// 5. Section 02: Top Tracks (TopSongsPanel Layout)
// ──────────────────────────────────────────────────────────────

