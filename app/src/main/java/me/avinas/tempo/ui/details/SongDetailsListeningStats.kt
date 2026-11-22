@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.avinas.tempo.ui.details

import me.avinas.tempo.ui.effects.tempoAmbientBackdrop
import me.avinas.tempo.ui.effects.tempoTiltSurface

/* Tempo · SongDetails · Audio Features & Listening History.
 * Human-centered copy standards (behuman):
 * - Clear, concrete facts over adjectives and puffery
 * - Direct, verb-first buttons and concise microcopy
 * - Human musical translations paired with precision data
 * - Zero emojis, 100% SVG vector icons and disciplined layout tokens
 */


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.window.Dialog
import me.avinas.tempo.ui.components.TempoDialogSurface
import me.avinas.tempo.ui.components.TempoDialogIcon
import me.avinas.tempo.ui.components.TempoDialogTitle
import me.avinas.tempo.ui.components.TempoDialogBody
import me.avinas.tempo.ui.components.TempoDialogButtonRow
import me.avinas.tempo.ui.components.TempoDialogDangerButton
import me.avinas.tempo.ui.components.TempoDialogSecondaryButton
import me.avinas.tempo.ui.components.TempoDialogTextField
import me.avinas.tempo.ui.components.TempoDropdownMenu
import me.avinas.tempo.ui.components.TempoDropdownMenuItem
import me.avinas.tempo.ui.components.TempoIcons
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight

import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.avinas.tempo.R
import me.avinas.tempo.data.enrichment.MusicBrainzEnrichmentService
import me.avinas.tempo.data.repository.TrackAudioFeatures
import me.avinas.tempo.data.stats.DailyListening
import me.avinas.tempo.data.stats.TagBasedMoodAnalyzer
import me.avinas.tempo.data.stats.TrackDetails
import me.avinas.tempo.data.stats.TrackEngagement
import me.avinas.tempo.ui.components.AlbumArtImage
import me.avinas.tempo.ui.components.ArtAtmosphereLayer
import me.avinas.tempo.ui.components.DeepOceanBackground
import me.avinas.tempo.ui.components.GlassCard
import me.avinas.tempo.ui.components.GlassCardVariant
import me.avinas.tempo.ui.components.FrostedIconButton
import me.avinas.tempo.ui.components.SharePreviewDialog
import me.avinas.tempo.ui.components.SongShareCard
import me.avinas.tempo.ui.spotify.SpotifyViewModel
import me.avinas.tempo.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun MasterStatMasthead(
    trackDetails: TrackDetails,
    engagement: TrackEngagement?,
    dominantColor: Color,
) {
    val appearancePalette = LocalTempoPalette.current
    val formattedTime = me.avinas.tempo.ui.stats.displayStatsDuration(trackDetails.totalTimeMs)

    val rankText = trackDetails.peakRank?.let { "#$it" } ?: "—"
    val rankSubtext = when (trackDetails.peakRank) {
        1 -> stringResource(R.string.details_rank_most_played)
        in 2..5 -> stringResource(R.string.details_rank_top_5)
        in 6..20 -> stringResource(R.string.details_rank_top_20)
        else -> stringResource(R.string.details_rank_library)
    }

    val hasEngagement = engagement != null && engagement.playCount > 0
    val loyaltyRate = if (hasEngagement) engagement.averageCompletionPercent.roundToInt() else null
    val loyaltyText = loyaltyRate?.let { "$it%" } ?: "—"
    val loyaltySubtext = when {
        loyaltyRate == null -> stringResource(R.string.details_awaiting_playback)
        loyaltyRate >= 90 -> stringResource(R.string.details_completion_to_end)
        loyaltyRate >= 70 -> stringResource(R.string.details_completion_high)
        loyaltyRate >= 50 -> stringResource(R.string.details_completion_moderate)
        else -> stringResource(R.string.details_completion_skipped)
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        variant = GlassCardVariant.Obsidian,
        borderColor = appearancePalette.border.copy(alpha = 0.45f),
        borderWidth = 0.8.dp,
        contentPadding = PaddingValues(0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 20.dp)) {
                MastheadHeroMetric(
                    label = stringResource(R.string.details_total_plays),
                    value = String.format(Locale.getDefault(), "%,d", trackDetails.playCount),
                    suffix = stringResource(R.string.details_together_suffix, formattedTime),
                    accentTint = dominantColor,
                )

                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.8.dp)
                        .background(appearancePalette.border.copy(alpha = 0.45f))
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Secondary metrics
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MastheadSecondaryStat(
                        label = stringResource(R.string.details_peak_position),
                        value = rankText,
                        subtext = rankSubtext,
                        modifier = Modifier.weight(1f),
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(0.8.dp)
                            .background(appearancePalette.border.copy(alpha = 0.45f)),
                    )

                    MastheadSecondaryStat(
                        label = stringResource(R.string.details_completion_rate),
                        value = loyaltyText,
                        subtext = loyaltySubtext,
                        valueColor = when {
                            loyaltyRate == null -> appearancePalette.onSurface
                            loyaltyRate >= 70 -> TempoSuccess
                            else -> TempoWarning
                        },
                        modifier = Modifier.weight(1f).padding(start = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun EditorialStatBlock(
    label: String,
    value: String,
    subtext: String,
    accentTint: Color,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
) {
    val appearancePalette = LocalTempoPalette.current
    Column(
        modifier = modifier.padding(
            horizontal = 18.dp,
            vertical = if (isCompact) 14.dp else 18.dp
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(accentTint)
            )
            Text(
                text = label.uppercase(Locale.getDefault()),
                style = KickerSmall,
                color = appearancePalette.mutedText,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = DisplayFontFamily,
                fontSize = if (isCompact) 22.sp else 28.sp,
                letterSpacing = (-0.5).sp,
            ),
            fontWeight = FontWeight.Bold,
            color = appearancePalette.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtext,
            style = CaptionSmall,
            color = appearancePalette.mutedText,
        )
    }
}

/**
 * Hero metric header for editorial stat mastheads: accent-dot kicker over a
 * display-size value with a trailing suffix line. Shared by song and artist.
 */
@Composable
internal fun MastheadHeroMetric(
    label: String,
    value: String,
    suffix: String,
    accentTint: Color,
) {
    val appearancePalette = LocalTempoPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(accentTint)
        )
        Text(
            text = label.uppercase(Locale.getDefault()),
            style = KickerSmall,
            color = appearancePalette.mutedText,
        )
    }

    Spacer(modifier = Modifier.height(6.dp))

    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = value,
            style = MaterialTheme.typography.displaySmall.copy(
                fontFamily = DisplayFontFamily,
                letterSpacing = (-1).sp,
            ),
            fontWeight = FontWeight.Bold,
            color = appearancePalette.onSurface,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = suffix,
            style = MaterialTheme.typography.bodySmall,
            color = appearancePalette.mutedText,
            modifier = Modifier.padding(bottom = 6.dp),
        )
    }
}

/**
 * Secondary masthead stat: kicker label over value with inline 11sp subtext.
 * Shared by song and artist mastheads.
 */
@Composable
internal fun MastheadSecondaryStat(
    label: String,
    value: String,
    subtext: String,
    modifier: Modifier = Modifier,
    valueColor: Color = LocalTempoPalette.current.onSurface,
) {
    val appearancePalette = LocalTempoPalette.current
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(Locale.getDefault()),
            style = KickerSmall,
            color = appearancePalette.mutedText,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = buildAnnotatedString {
                append(value)
                append("  ")
                withStyle(
                    SpanStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = appearancePalette.mutedText,
                    )
                ) {
                    append(subtext)
                }
            },
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = DisplayFontFamily),
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ──────────────────────────────────────────────────────────────
// 3. Listening Activity Over Time
// ──────────────────────────────────────────────────────────────

@Composable
fun TemporalRhythmSection(
    history: List<DailyListening>,
    peakBinge: Pair<String, Int>?,
    tint: Color = LocalTempoPalette.current.accent,
    sectionNumber: String = "02",
) {
    val appearancePalette = LocalTempoPalette.current
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        variant = GlassCardVariant.QuietGlass,
        accentColor = tint,
        contentPadding = PaddingValues(18.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionCatalogKicker(
                    number = sectionNumber,
                    label = stringResource(R.string.details_listening_trends),
                )
                if (peakBinge != null && peakBinge.second > 1) {
                    Text(
                        text = stringResource(
                            R.string.details_trends_peak,
                            peakBinge.second,
                            formatChartDate(peakBinge.first),
                        ),
                        style = CaptionSmall,
                        color = appearancePalette.mutedText,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            ScrubbableRhythmChart(
                history = history,
                peakBinge = peakBinge,
                tint = tint,
            )
        }
    }
}

@Composable
private fun ScrubbableRhythmChart(
    history: List<DailyListening>,
    peakBinge: Pair<String, Int>?,
    tint: Color,
) {
    val appearancePalette = LocalTempoPalette.current
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    var clearJob by remember { mutableStateOf<Job?>(null) }
    val haptic = LocalHapticFeedback.current

    val maxPlays = remember(history) {
        history.maxOfOrNull { it.playCount }?.coerceAtLeast(1) ?: 1
    }
    val activeDays = remember(history) { history.count { it.playCount > 0 } }
    val totalPlays = remember(history) { history.sumOf { it.playCount } }
    val avgPerActiveDay = if (activeDays > 0) {
        String.format(Locale.getDefault(), "%.1f", totalPlays.toFloat() / activeDays)
    } else {
        "1.0"
    }

    // Render historical data at its final values immediately; loading and
    // navigation already provide coherent feedback without a 750ms chart delay.

    // Screen-reader summary of the currently inspected day
    val inspectedItem = selectedIndex?.takeIf { it in history.indices }?.let { history[it] }
    val chartStateDescription = if (inspectedItem != null) {
        val timeStr = me.avinas.tempo.ui.stats.displayStatsDuration(inspectedItem.totalTimeMs)
        stringResource(
            R.string.details_trends_day_summary,
            formatSongDate(parseIsoToTimestamp(inspectedItem.date)),
            inspectedItem.playCount,
            timeStr,
        )
    } else {
        stringResource(R.string.details_trends_scrub_hint)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Summary metrics (active days, daily average, peak day)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            TelemetryMiniCell(
                label = stringResource(R.string.details_trends_active_days),
                value = activeDays.toString(),
                modifier = Modifier.weight(1f),
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(0.8.dp)
                    .background(appearancePalette.border.copy(alpha = 0.45f))
            )

            TelemetryMiniCell(
                label = stringResource(R.string.details_trends_daily_avg),
                value = stringResource(R.string.details_trends_per_day, avgPerActiveDay),
                modifier = Modifier.weight(1f),
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(0.8.dp)
                    .background(appearancePalette.border.copy(alpha = 0.45f))
            )

            TelemetryMiniCell(
                label = stringResource(R.string.details_trends_peak_day),
                value = (peakBinge?.second ?: maxPlays).toString(),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // History bar chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .semantics { stateDescription = chartStateDescription }
                .pointerInput(history) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            clearJob?.cancel()
                            val idx = ((offset.x / size.width) * history.size).toInt().coerceIn(0, history.size - 1)
                            if (selectedIndex != idx) {
                                selectedIndex = idx
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val idx = ((change.position.x / size.width) * history.size).toInt().coerceIn(0, history.size - 1)
                            if (selectedIndex != idx) {
                                selectedIndex = idx
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        onDragEnd = {
                            clearJob = scope.launch {
                                delay(1800)
                                selectedIndex = null
                            }
                        },
                        onDragCancel = {
                            selectedIndex = null
                        }
                    )
                }
                .pointerInput(history) {
                    detectTapGestures { offset ->
                        clearJob?.cancel()
                        val idx = ((offset.x / size.width) * history.size).toInt().coerceIn(0, history.size - 1)
                        selectedIndex = idx
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        clearJob = scope.launch {
                            delay(2200)
                            selectedIndex = null
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barCount = history.size
                val totalWidth = size.width
                val totalHeight = size.height
                val barSpacing = 2.dp.toPx()
                val availableWidth = totalWidth - (barSpacing * (barCount - 1).coerceAtLeast(0))
                val barWidth = (availableWidth / barCount.coerceAtLeast(1)).coerceIn(2.5.dp.toPx(), 14.dp.toPx())

                // Baseline
                drawLine(
                    color = appearancePalette.border.copy(alpha = 0.45f),
                    start = Offset(0f, totalHeight),
                    end = Offset(totalWidth, totalHeight),
                    strokeWidth = 1.dp.toPx(),
                )

                history.forEachIndexed { index, item ->
                    val x = index * (totalWidth / barCount.coerceAtLeast(1))
                    val isSelected = selectedIndex == index
                    val isPeak = item.playCount == maxPlays && maxPlays > 1

                    if (item.playCount == 0) {
                        // Empty-day indicator dot on baseline
                        drawCircle(
                            color = if (isSelected) appearancePalette.onSurface else appearancePalette.border.copy(alpha = 0.65f),
                            radius = 1.5.dp.toPx(),
                            center = Offset(x + barWidth / 2f, totalHeight - 1.5.dp.toPx()),
                        )
                        return@forEachIndexed
                    }

                    val fraction = (item.playCount.toFloat() / maxPlays).coerceIn(0.06f, 1f)
                    val stagger = 1f
                    val barHeight = totalHeight * fraction * stagger
                    val barTop = totalHeight - barHeight

                    val barColor = when {
                        isSelected -> appearancePalette.onSurface
                        isPeak -> tint
                        else -> tint.copy(alpha = 0.55f)
                    }

                    // Highlight halo for selected and peak bars
                    if (isSelected || isPeak) {
                        drawRoundRect(
                            color = barColor.copy(alpha = 0.20f),
                            topLeft = Offset(x - 1.5.dp.toPx(), barTop - 2.dp.toPx()),
                            size = Size(barWidth + 3.dp.toPx(), barHeight + 2.dp.toPx()),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                        )
                    }

                    // Bar vertical gradient fill
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                barColor,
                                barColor.copy(alpha = 0.45f),
                            ),
                            startY = barTop,
                            endY = totalHeight,
                        ),
                        topLeft = Offset(x, barTop),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx()),
                    )

                    // Cap dot indicator above selected bar
                    if (isSelected) {
                        drawCircle(
                            color = appearancePalette.onSurface,
                            radius = 2.dp.toPx(),
                            center = Offset(x + barWidth / 2f, barTop - 5.dp.toPx()),
                        )
                    }
                }
            }

            // Scrubbing inspection HUD overlay
            inspectedItem?.let { item ->
                val timeStr = me.avinas.tempo.ui.stats.displayStatsDuration(item.totalTimeMs)
                val playsLabel = stringResource(
                    if (item.playCount == 1) R.string.details_trends_hud_one else R.string.details_trends_hud_many,
                    item.playCount,
                    timeStr,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(appearancePalette.elevatedSurface)
                        .border(0.8.dp, appearancePalette.border.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = formatSongDate(parseIsoToTimestamp(item.date)),
                        style = KickerSmall,
                        color = appearancePalette.onSurface,
                    )
                    Text(
                        text = "·",
                        style = CaptionSmall,
                        color = appearancePalette.mutedText,
                    )
                    Text(
                        text = playsLabel,
                        style = KickerSmall,
                        color = tint,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Date axis labels with optional midpoint for wide ranges
        val midIndex = history.size / 2
        val midDate = if (history.size >= 45 && midIndex in history.indices) {
            formatChartDate(history[midIndex].date)
        } else {
            null
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatChartDate(history.first().date),
                style = CaptionSmall,
                color = appearancePalette.mutedText,
            )
            if (midDate != null) {
                Text(
                    text = midDate,
                    style = CaptionSmall,
                    color = appearancePalette.mutedText,
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                text = formatChartDate(history.last().date),
                style = CaptionSmall,
                color = appearancePalette.mutedText,
            )
        }
    }
}


