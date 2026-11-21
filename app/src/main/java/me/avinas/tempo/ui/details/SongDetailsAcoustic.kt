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
fun AcousticDNASpecSheet(
    audioFeatures: TrackAudioFeatures?,
    moodSummary: TagBasedMoodAnalyzer.MoodSummary?,
    tint: Color = LocalTempoPalette.current.accent,
    sectionNumber: String = "03",
) {
    val appearancePalette = LocalTempoPalette.current
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        variant = GlassCardVariant.QuietGlass,
        accentColor = tint,
        contentPadding = PaddingValues(20.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionCatalogKicker(
                    number = sectionNumber,
                    label = stringResource(R.string.details_audio_features_title),
                )
                Text(
                    text = when {
                        audioFeatures != null -> stringResource(R.string.details_af_verified)
                        moodSummary != null -> stringResource(R.string.details_af_tag_estimate)
                        else -> stringResource(R.string.details_af_awaiting)
                    },
                    style = CaptionSmall,
                    color = appearancePalette.mutedText,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (audioFeatures != null || moodSummary != null) {
                val bpm = audioFeatures?.tempo?.roundToInt()?.takeIf { it > 0 }
                val hasHarmonicKey = audioFeatures != null && audioFeatures.hasValidKey

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Tempo / BPM readout
                    if (bpm != null) {
                        val tempoDesc = when {
                            bpm > 140 -> stringResource(R.string.details_af_fast)
                            bpm > 120 -> stringResource(R.string.details_af_upbeat)
                            bpm > 95 -> stringResource(R.string.details_af_moderate)
                            else -> stringResource(R.string.details_af_laidback)
                        }
                        AcousticHeaderMetric(
                            icon = Icons.Rounded.Speed,
                            label = stringResource(R.string.details_af_tempo),
                            primaryValue = "$bpm BPM",
                            sublabel = tempoDesc,
                            tint = tint,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    if (bpm != null && hasHarmonicKey) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(0.8.dp)
                                .background(appearancePalette.border.copy(alpha = 0.45f))
                        )
                    }

                    // Musical key readout
                    val keyName = audioFeatures?.takeIf { it.hasValidKey }?.musicalKey
                    if (keyName != null) {
                        val keyDesc = audioFeatures?.moodDescription
                            ?: moodSummary?.energyName
                            ?: stringResource(R.string.details_af_awaiting_profile)

                        AcousticHeaderMetric(
                            icon = Icons.Rounded.GraphicEq,
                            label = stringResource(R.string.details_af_key),
                            primaryValue = keyName,
                            sublabel = keyDesc,
                            tint = tint,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                if (audioFeatures != null) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // Spectrum Meters (Real values from audio features)
                    val energyDesc = when {
                        audioFeatures.energy >= 0.8f -> stringResource(R.string.details_energy_high)
                        audioFeatures.energy >= 0.5f -> stringResource(R.string.details_energy_mid)
                        else -> stringResource(R.string.details_energy_low)
                    }

                    val valenceDesc = when {
                        audioFeatures.valence >= 0.7f -> stringResource(R.string.details_valence_high)
                        audioFeatures.valence >= 0.45f -> stringResource(R.string.details_valence_mid)
                        else -> stringResource(R.string.details_valence_low)
                    }

                    val danceDesc = when {
                        audioFeatures.danceability >= 0.75f -> stringResource(R.string.details_dance_high)
                        audioFeatures.danceability >= 0.5f -> stringResource(R.string.details_dance_mid)
                        else -> stringResource(R.string.details_dance_low)
                    }

                    val acousticDesc = when {
                        audioFeatures.acousticness >= 0.6f -> stringResource(R.string.details_acoustic_high)
                        audioFeatures.acousticness >= 0.3f -> stringResource(R.string.details_acoustic_mid)
                        else -> stringResource(R.string.details_acoustic_low)
                    }

                    // One accent (the artwork tint), staggered reveal
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        AcousticSpecMeter(label = stringResource(R.string.details_energy_label), humanDesc = energyDesc, fraction = audioFeatures.energy, tint = tint, delayMs = 0)
                        AcousticSpecMeter(label = stringResource(R.string.details_mood_label), humanDesc = valenceDesc, fraction = audioFeatures.valence, tint = tint, delayMs = 70)
                        AcousticSpecMeter(label = stringResource(R.string.details_danceability_label), humanDesc = danceDesc, fraction = audioFeatures.danceability, tint = tint, delayMs = 140)
                        AcousticSpecMeter(label = stringResource(R.string.details_acoustic_label), humanDesc = acousticDesc, fraction = audioFeatures.acousticness, tint = tint, delayMs = 210)
                    }
                }

                // Audio feature badges
                val dynamicBadges = mutableListOf<String>()
                if (audioFeatures != null) {
                    if (audioFeatures.energy >= 0.75f) dynamicBadges.add(stringResource(R.string.details_badge_intensity))
                    if (audioFeatures.danceability >= 0.75f) dynamicBadges.add(stringResource(R.string.details_badge_club))
                    if (audioFeatures.valence <= 0.35f) dynamicBadges.add(stringResource(R.string.details_badge_nocturnal))
                    if (audioFeatures.acousticness >= 0.60f) dynamicBadges.add(stringResource(R.string.details_badge_organic))
                    if (audioFeatures.tempo >= 130) dynamicBadges.add(stringResource(R.string.details_badge_fast))
                }

                val moodTags = moodSummary?.moodTags.orEmpty()
                val combinedBadges = (dynamicBadges + moodTags).distinct().take(5)

                if (combinedBadges.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(18.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        combinedBadges.forEach { tag ->
                            MoodTagPill(tag = tag)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AcousticHeaderMetric(
    icon: ImageVector,
    label: String,
    primaryValue: String,
    sublabel: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val appearancePalette = LocalTempoPalette.current
    Column(
        modifier = modifier.padding(horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label.uppercase(Locale.getDefault()),
                style = KickerSmall,
                color = appearancePalette.mutedText,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = primaryValue,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.Bold,
            ),
            color = appearancePalette.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = sublabel,
            style = CaptionSmall,
            color = appearancePalette.mutedText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AcousticSpecMeter(
    label: String,
    humanDesc: String,
    fraction: Float,
    tint: Color,
    delayMs: Int = 0,
) {
    val appearancePalette = LocalTempoPalette.current
    val percent = (fraction * 100).roundToInt().coerceIn(0, 100)
    // Numeric listening metrics must never show an interpolated, inaccurate value.
    val animatedPercent = percent
    val animatedFraction = fraction.coerceIn(0f, 1f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = label,
                    style = Kicker,
                    color = appearancePalette.onSurface,
                )
                Text(
                    text = "· $humanDesc",
                    style = CaptionSmall,
                    color = appearancePalette.mutedText,
                )
            }
            Text(
                text = "$animatedPercent%",
                style = Kicker,
                color = tint,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
        ) {
            // Track
            drawLine(
                color = appearancePalette.border.copy(alpha = 0.45f),
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = size.height,
                cap = StrokeCap.Round,
            )
            // Filled bar
            if (animatedFraction > 0f) {
                drawLine(
                    color = tint,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width * animatedFraction.coerceIn(0f, 1f), size.height / 2f),
                    strokeWidth = size.height,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun MoodTagPill(tag: String) {
    val appearancePalette = LocalTempoPalette.current
    Box(
        modifier = Modifier
            .background(GlassFrostMedium, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = tag,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = appearancePalette.mutedText,
        )
    }
}

// ──────────────────────────────────────────────────────────────
// 5. Listening Standing
// ──────────────────────────────────────────────────────────────

