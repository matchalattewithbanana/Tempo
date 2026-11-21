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
fun SongHeroEditorialStage(
    trackDetails: TrackDetails,
    dominantColor: Color,
    previewUrl: String?,
    isPlayingPreview: Boolean,
    previewProgress: Float,
    previewPositionMs: Long,
    onTogglePreview: () -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToAlbum: (String, String) -> Unit,
    onEditTitle: () -> Unit,
    onEditCover: () -> Unit,
    onHotlinkSuccess: () -> Unit = {},
    onTitlePositioned: (Float) -> Unit = {},
    genre: String? = null,
    releaseDate: String? = null,
    releaseYear: Int? = null,
    recordLabel: String? = null,
) {
    val appearancePalette = LocalTempoPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Soft dominant-color halo — ties the extracted tint to the hero
        // and gives the art depth beyond its shadow.
        val glowBrush = remember(dominantColor) {
            Brush.radialGradient(
                colors = listOf(
                    dominantColor.copy(alpha = 0.20f),
                    dominantColor.copy(alpha = 0.0f),
                )
            )
        }
        // Artwork Box
        Box(
            modifier = Modifier.size(240.dp).tempoTiltSurface(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = glowBrush, shape = CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(232.dp)
                    .shadow(
                        elevation = 22.dp,
                        shape = RoundedCornerShape(22.dp),
                        ambientColor = appearancePalette.surfaceSunken.copy(alpha = 0.30f),
                        spotColor = dominantColor.copy(alpha = 0.22f),
                    )
                    .clip(RoundedCornerShape(22.dp))
                    .background(appearancePalette.surfaceSunken)
                    .border(1.dp, appearancePalette.border, RoundedCornerShape(22.dp)),
            ) {
                AlbumArtImage(
                    albumArtUrl = trackDetails.track.albumArtUrl,
                    localArtUrl = trackDetails.localBackupArtUrl,
                    contentDescription = stringResource(
                        R.string.details_cover_artwork_cd, trackDetails.track.title
                    ),
                    modifier = Modifier.fillMaxSize(),
                    onHotlinkSuccess = { onHotlinkSuccess() },
                )

                // Favorite Badge
                if (trackDetails.isFavorite) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(appearancePalette.surface.copy(alpha = 0.85f))
                            .border(0.6.dp, appearancePalette.border.copy(alpha = 0.45f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = stringResource(R.string.details_favorite_track_cd),
                            tint = TempoError,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Explicit affordance: artwork is editable without relying on a hidden gesture.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(appearancePalette.surface.copy(alpha = 0.90f))
                        .border(0.8.dp, appearancePalette.border.copy(alpha = 0.65f), CircleShape)
                        .premiumClickable(onClick = onEditCover),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.details_change_cover),
                        tint = appearancePalette.onSurface,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        // 30s Audio Preview Pill
        if (!previewUrl.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            AudioPreviewPill(
                isPlaying = isPlayingPreview,
                progress = previewProgress,
                positionMs = previewPositionMs,
                onToggle = onTogglePreview,
                accentColor = dominantColor,
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Title and Edit
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    onTitlePositioned(coordinates.boundsInWindow().top)
                },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = trackDetails.track.title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = DisplayFontFamily,
                    letterSpacing = (-0.5).sp,
                ),
                fontWeight = FontWeight.Bold,
                color = appearancePalette.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .premiumClickable(onClick = onEditTitle),
            )

            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(GlassFrostSoft)
                    .border(0.6.dp, appearancePalette.border.copy(alpha = 0.45f), CircleShape)
                    .premiumClickable(onClick = onEditTitle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = stringResource(R.string.details_edit_title),
                    tint = appearancePalette.mutedText,
                    modifier = Modifier.size(15.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Artist link
        val artistIdentifier = if (trackDetails.track.primaryArtistId != null && trackDetails.track.primaryArtistId > 0) {
            "id:${trackDetails.track.primaryArtistId}"
        } else {
            trackDetails.track.artist
        }
        ClickableEntityRow(
            text = trackDetails.track.artist,
            style = MaterialTheme.typography.titleMedium,
            tint = appearancePalette.onSurface,
            onClick = { onNavigateToArtist(artistIdentifier) },
        )

        // Album link
        if (!trackDetails.track.album.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(3.dp))
            ClickableEntityRow(
                text = trackDetails.track.album!!,
                style = MaterialTheme.typography.bodyMedium,
                tint = appearancePalette.mutedText,
                onClick = { onNavigateToAlbum(trackDetails.track.album!!, trackDetails.track.artist) },
            )
        }

        // Release date and album metadata
        val metaParts = listOfNotNull(
            releaseDate?.let { formatReleaseDate(it) } ?: releaseYear?.toString(),
            genre?.takeIf { it.isNotBlank() },
            recordLabel?.takeIf { it.isNotBlank() },
        )
        if (metaParts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(0.8.dp)
                        .background(appearancePalette.border.copy(alpha = 0.65f)),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = metaParts.joinToString("  ·  ").uppercase(Locale.getDefault()),
                    style = KickerSmall,
                    color = appearancePalette.mutedText,
                    letterSpacing = 1.4.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(0.8.dp)
                        .background(appearancePalette.border.copy(alpha = 0.65f)),
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun AudioPreviewPill(
    isPlaying: Boolean,
    progress: Float,
    positionMs: Long,
    onToggle: () -> Unit,
    accentColor: Color,
) {
    val secondsElapsed = (positionMs / 1000).toInt().coerceIn(0, 30)
    val timecode = stringResource(R.string.details_preview_timecode, secondsElapsed)
    // Source progress arrives frequently; use the theme's fast spatial spring,
    // which snaps immediately under reduced motion.
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "previewProgressFill",
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(PillSurface)
            .border(1.dp, PillBorder, RoundedCornerShape(14.dp))
            .premiumClickable(onClick = onToggle, pressedScale = 0.96f),
    ) {
        Row(
            modifier = Modifier.padding(
                start = 14.dp,
                end = 14.dp,
                top = 7.dp,
                bottom = if (isPlaying || progress > 0f) 9.dp else 7.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = stringResource(
                    if (isPlaying) R.string.details_preview_pause_cd else R.string.details_preview_play_cd
                ),
                tint = PillTextPrimary,
                modifier = Modifier.size(15.dp),
            )

            Text(
                text = if (isPlaying) timecode else stringResource(R.string.details_preview_label),
                style = CaptionSmall,
                fontWeight = FontWeight.Bold,
                color = PillTextPrimary,
            )

            if (isPlaying) {
                MiniEqualizer(color = PillTextPrimary)
            }
        }

        // Hairline position indicator — appears only once listening has
        // started; never on the untouched idle chip.
        if (isPlaying || progress > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(PillTextPrimary.copy(alpha = 0.14f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                        .background(PillTextPrimary)
                )
            }
        }
    }
}

@Composable
private fun MiniEqualizer(
    color: Color,
    modifier: Modifier = Modifier,
) {
    // A stable playing indicator is legible without perpetual drawing work.
    Canvas(modifier = modifier.size(width = 12.dp, height = 12.dp)) {
        val barWidth = 2.5.dp.toPx()
        val gap = ((size.width - barWidth * 3f) / 2f).coerceAtLeast(0f)
        repeat(3) { index ->
            val level = listOf(0.40f, 0.85f, 0.60f)[index]
            val barHeight = size.height * level
            drawRoundRect(
                color = color,
                topLeft = Offset(index * (barWidth + gap), (size.height - barHeight) / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}

@Composable
internal fun ClickableEntityRow(
    text: String,
    style: TextStyle,
    tint: Color,
    onClick: () -> Unit,
) {
    val appearancePalette = LocalTempoPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .premiumClickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = style,
            fontWeight = FontWeight.SemiBold,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = appearancePalette.mutedText,
            modifier = Modifier.size(15.dp),
        )
    }
}

// ──────────────────────────────────────────────────────────────
// 2. High-Contrast Master Stats
// ──────────────────────────────────────────────────────────────

