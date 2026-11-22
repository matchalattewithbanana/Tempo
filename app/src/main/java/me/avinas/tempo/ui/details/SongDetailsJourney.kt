@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.avinas.tempo.ui.details
import me.avinas.tempo.ui.util.observableLocale

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
fun ListenerAffinitySection(
    engagement: TrackEngagement,
) {
    val appearancePalette = LocalTempoPalette.current
    val score = engagement.engagementScore
    val tier = when {
        score >= 90 -> AffinityTierInfo(
            Icons.Rounded.AutoAwesome,
            stringResource(R.string.details_tier_top_status),
            stringResource(R.string.details_tier_top_name),
            stringResource(R.string.details_tier_top_desc),
        )
        score >= 80 -> AffinityTierInfo(
            Icons.Rounded.LocalFireDepartment,
            stringResource(R.string.details_tier_heavy_status),
            stringResource(R.string.details_tier_heavy_name),
            stringResource(R.string.details_tier_heavy_desc),
        )
        score >= 70 -> AffinityTierInfo(
            Icons.Rounded.Speed,
            stringResource(R.string.details_tier_regular_status),
            stringResource(R.string.details_tier_regular_name),
            stringResource(R.string.details_tier_regular_desc),
        )
        score >= 55 -> AffinityTierInfo(
            Icons.Rounded.MusicNote,
            stringResource(R.string.details_tier_moderate_status),
            stringResource(R.string.details_tier_moderate_name),
            stringResource(R.string.details_tier_moderate_desc),
        )
        score >= 40 -> AffinityTierInfo(
            Icons.Rounded.Headphones,
            stringResource(R.string.details_tier_occasional_status),
            stringResource(R.string.details_tier_occasional_name),
            stringResource(R.string.details_tier_occasional_desc),
        )
        score >= 25 -> AffinityTierInfo(
            Icons.Rounded.GraphicEq,
            stringResource(R.string.details_tier_background_status),
            stringResource(R.string.details_tier_background_name),
            stringResource(R.string.details_tier_background_desc),
        )
        else -> AffinityTierInfo(
            Icons.Rounded.Timeline,
            stringResource(R.string.details_tier_rare_status),
            stringResource(R.string.details_tier_rare_name),
            stringResource(R.string.details_tier_rare_desc),
        )
    }

    // Affinity tier badge and engagement score
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TempoSurfaceWhiteGray)
            .border(0.8.dp, appearancePalette.border.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(TempoSurfaceWhiteGrayChip),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = tier.icon,
                contentDescription = null,
                tint = appearancePalette.mutedText,
                modifier = Modifier.size(16.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tier.name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = DisplayFontFamily,
                ),
                fontWeight = FontWeight.Bold,
                color = appearancePalette.onSurface,
            )
            Text(
                text = tier.status.uppercase(observableLocale()),
                style = KickerSmall,
                color = appearancePalette.mutedText,
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = DisplayFontFamily,
                ),
                fontWeight = FontWeight.Bold,
                color = appearancePalette.onSurface,
            )
            Text(
                text = stringResource(R.string.details_engagement_score).uppercase(observableLocale()),
                style = KickerSmall,
                color = appearancePalette.mutedText,
            )
        }
    }
}

private data class AffinityTierInfo(
    val icon: ImageVector,
    val status: String,
    val name: String,
    val description: String,
)

// ──────────────────────────────────────────────────────────────
// 6. Listening Timeline & Milestones
// ──────────────────────────────────────────────────────────────

/**
 * Palette for flat timeline markers and labels, adjusted for contrast against cover art.
 */
internal data class FlatTimelineInk(
    val title: Color,
    val value: Color,
    val icon: Color,
    val divider: Color,
)

internal val DarkRoomInk = FlatTimelineInk(
    title = TextTertiary,
    value = TextPrimary,
    icon = TextSecondary,
    divider = GlassBorderSoft,
)

internal val BrightRoomInk = FlatTimelineInk(
    title = TextOnAccent.copy(alpha = 0.55f),
    value = TextOnAccent,
    icon = TextOnAccent.copy(alpha = 0.78f),
    divider = TextOnAccent.copy(alpha = 0.14f),
)

@Composable
fun ListeningOdysseySection(
    trackDetails: TrackDetails,
    engagement: TrackEngagement?,
    habitualHour: String?,
    habitualHourOfDay: Int? = null,
    tint: Color = LocalTempoPalette.current.accent,
    sectionNumber: String = "01",
) {
    val appearancePalette = LocalTempoPalette.current
    val firstListen = trackDetails.firstPlayed ?: engagement?.firstPlayedTimestamp
    val lastListen = trackDetails.lastPlayed ?: engagement?.lastPlayedTimestamp

    // Construct milestones for first played, peak listening hour, and latest played
    val milestones = listOfNotNull(
        firstListen?.let {
            OdysseyMilestone(
                icon = Icons.Rounded.AutoAwesome,
                title = stringResource(R.string.details_stat_first_played),
                value = formatFlowDate(it),
            )
        },
        habitualHour?.let {
            OdysseyMilestone(
                icon = habitualHourIcon(habitualHourOfDay),
                title = stringResource(R.string.details_most_active),
                value = it,
            )
        },
        lastListen?.let {
            OdysseyMilestone(
                icon = Icons.Rounded.Headphones,
                title = stringResource(R.string.details_timeline_latest),
                value = formatFlowDate(it),
            )
        },
    )
    if (milestones.isEmpty()) return

    // Pick high-contrast text color based on background luminance
    val ink = remember(tint, appearancePalette) {
        val backdrop = lerp(tint, appearancePalette.background, 0.65f)
        if (backdrop.luminance() > 0.25f) BrightRoomInk else FlatTimelineInk(
            title = appearancePalette.mutedText,
            value = appearancePalette.onSurface,
            icon = appearancePalette.mutedText,
            divider = appearancePalette.border.copy(alpha = 0.45f),
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionCatalogKicker(
            number = sectionNumber,
            label = stringResource(R.string.details_timeline_title),
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Horizontal connecting milestone line
        TimeFlowRibbon(
            stationCount = milestones.size,
            ink = ink,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            milestones.forEach { milestone ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                ) {
                    FlatMilestoneRow(milestone = milestone, ink = ink)
                }
            }
        }
    }
}

/**
 * Horizontal connecting line with milestone markers and end chevron.
 */
@Composable
internal fun TimeFlowRibbon(
    stationCount: Int,
    ink: FlatTimelineInk,
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp),
    ) {
        val cy = size.height / 2f
        val arrowLen = 5.dp.toPx()
        val endX = size.width - arrowLen
        val step = if (stationCount > 0) size.width / stationCount else size.width

        // Horizontal connecting line
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(ink.divider, ink.icon.copy(alpha = 0.55f)),
                startX = 0f,
                endX = endX,
            ),
            start = Offset(0f, cy),
            end = Offset(endX, cy),
            strokeWidth = 1.2.dp.toPx(),
            cap = StrokeCap.Round,
        )

        // End chevron
        val chevronBase = endX + 1.2.dp.toPx()
        drawLine(
            color = ink.icon,
            start = Offset(chevronBase - 2.6.dp.toPx(), cy - 3.dp.toPx()),
            end = Offset(chevronBase, cy),
            strokeWidth = 1.2.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = ink.icon,
            start = Offset(chevronBase - 2.6.dp.toPx(), cy + 3.dp.toPx()),
            end = Offset(chevronBase, cy),
            strokeWidth = 1.2.dp.toPx(),
            cap = StrokeCap.Round,
        )

        // Milestone dots
        repeat(stationCount) { index ->
            drawCircle(
                color = if (index == stationCount - 1) ink.value else ink.icon,
                radius = 2.8.dp.toPx(),
                center = Offset(index * step, cy),
            )
            if (index == 0 && stationCount > 1) {
                // Outer ring for first milestone
                drawCircle(
                    color = ink.icon.copy(alpha = 0.45f),
                    radius = 5.2.dp.toPx(),
                    center = Offset(0f, cy),
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
        }
    }
}

internal data class OdysseyMilestone(
    val icon: ImageVector,
    val title: String,
    val value: String,
)

/**
 * Returns an icon representing the time of day for the given hour (0..23).
 */
private fun habitualHourIcon(hourOfDay: Int?): ImageVector = when (hourOfDay) {
    in 5..7 -> Icons.Rounded.WbTwilight       // Dawn (5-7 AM)
    in 8..11 -> Icons.Rounded.LightMode       // Morning (8-11 AM)
    in 12..16 -> Icons.Rounded.WbSunny        // Afternoon (12-4 PM)
    in 17..21 -> Icons.Rounded.WbTwilight     // Evening (5-9 PM)
    in 22..23, in 0..4 -> Icons.Rounded.DarkMode // Night (10 PM - 4 AM)
    else -> Icons.Rounded.DarkMode
}

/**
 * Milestone column displaying an icon, label, and formatted date or hour.
 */
@Composable
internal fun FlatMilestoneRow(
    milestone: OdysseyMilestone,
    ink: FlatTimelineInk,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = milestone.icon,
                contentDescription = null,
                tint = ink.icon,
                modifier = Modifier.size(11.dp),
            )
            Text(
                text = milestone.title.uppercase(observableLocale()),
                style = KickerSmall,
                color = ink.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = milestone.value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = ink.value,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ──────────────────────────────────────────────────────────────
// 7. Playback Breakdown
// ──────────────────────────────────────────────────────────────

@Composable
fun PlaybackFidelitySection(
    engagement: TrackEngagement,
    tint: Color = LocalTempoPalette.current.accent,
    sectionNumber: String = "04",
) {
    val appearancePalette = LocalTempoPalette.current
    val total = engagement.playCount.coerceAtLeast(1)
    val fullPlaysRatio = (engagement.fullPlaysCount.toFloat() / total).coerceIn(0f, 1f)
    val partialPlaysRatio = (engagement.partialPlaysCount.toFloat() / total).coerceIn(0f, 1f)
    val skipsRatio = (engagement.skipsCount.toFloat() / total).coerceIn(0f, 1f)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        variant = GlassCardVariant.QuietGlass,
        accentColor = tint,
        contentPadding = PaddingValues(20.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SectionCatalogKicker(
                number = sectionNumber,
                label = stringResource(R.string.details_playback_title),
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Segmented bar: one clipped rounded track with plain segment
            // rects inside — no per-segment corner seams between neighbors.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(appearancePalette.border.copy(alpha = 0.45f))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    if (fullPlaysRatio > 0f) {
                        drawRect(
                            color = TempoSuccess,
                            topLeft = Offset(0f, 0f),
                            size = Size(w * fullPlaysRatio, size.height),
                        )
                    }
                    if (partialPlaysRatio > 0f) {
                        drawRect(
                            color = TempoWarning,
                            topLeft = Offset(w * fullPlaysRatio, 0f),
                            size = Size(w * partialPlaysRatio, size.height),
                        )
                    }
                    if (skipsRatio > 0f) {
                        drawRect(
                            color = TempoError,
                            topLeft = Offset(w * (fullPlaysRatio + partialPlaysRatio), 0f),
                            size = Size(w * skipsRatio, size.height),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Legend with Typographical Ranges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                FidelityLegendItem(
                    color = TempoSuccess,
                    label = stringResource(R.string.details_playback_full_label),
                )
                FidelityLegendItem(
                    color = TempoWarning,
                    label = stringResource(R.string.details_playback_partial_label),
                )
                FidelityLegendItem(
                    color = TempoError,
                    label = stringResource(R.string.details_playback_skipped_label),
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Telemetry Data Points
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
            ) {
                TelemetryMiniCell(
                    label = stringResource(R.string.details_stat_replays),
                    value = "${engagement.replayCount}",
                    modifier = Modifier.weight(1f),
                )

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(0.8.dp)
                        .background(appearancePalette.border.copy(alpha = 0.45f))
                )

                TelemetryMiniCell(
                    label = stringResource(R.string.details_telemetry_pauses),
                    value = String.format(observableLocale(), "%.1f", engagement.averagePauseCount),
                    modifier = Modifier.weight(1f),
                )

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(0.8.dp)
                        .background(appearancePalette.border.copy(alpha = 0.45f))
                )

                TelemetryMiniCell(
                    label = stringResource(R.string.details_telemetry_sessions),
                    value = "${engagement.uniqueSessionsCount}",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun FidelityLegendItem(
    color: Color,
    label: String,
) {
    val appearancePalette = LocalTempoPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = CaptionSmall,
            color = appearancePalette.mutedText,
        )
    }
}

@Composable
internal fun TelemetryMiniCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val appearancePalette = LocalTempoPalette.current
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label.uppercase(observableLocale()),
            style = KickerSmall,
            color = appearancePalette.mutedText,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.Bold,
            ),
            color = appearancePalette.onSurface,
        )
    }
}

// ──────────────────────────────────────────────────────────────
// 8. Streaming & Actions
// ──────────────────────────────────────────────────────────────

@Composable
fun StreamingHubSection(
    onShare: () -> Unit,
) {
    // Share card button
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PillSurface)
            .premiumClickable(onClick = onShare, pressedScale = 0.98f)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = null,
                    tint = PillTextPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.details_action_share_card),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = PillTextPrimary,
                    letterSpacing = 0.3.sp,
                )
            }
        }
}

// ──────────────────────────────────────────────────────────────
// Section Headers
// ──────────────────────────────────────────────────────────────

@Composable
internal fun SectionCatalogKicker(
    number: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val appearancePalette = LocalTempoPalette.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = number,
            style = KickerSmall,
            fontWeight = FontWeight.Bold,
            color = appearancePalette.onSurface,
        )
        Box(
            modifier = Modifier
                .width(10.dp)
                .height(0.8.dp)
                .background(appearancePalette.border.copy(alpha = 0.65f)),
        )
        Text(
            text = label.uppercase(observableLocale()),
            style = KickerSmall,
            color = appearancePalette.mutedText,
            letterSpacing = 1.2.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ──────────────────────────────────────────────────────────────
// Dialogs: Edit Title & Delete Song
// ──────────────────────────────────────────────────────────────

