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
internal fun EditTitleDialog(
    currentTitle: String,
    isSaving: Boolean,
    error: String?,
    onClearWarnings: () -> Unit,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val appearancePalette = LocalTempoPalette.current
    var titleText by remember { mutableStateOf(currentTitle) }
    val trimmed = titleText.trim()
    val isUnchanged = trimmed == currentTitle
    val canSave = trimmed.isNotEmpty() && !isUnchanged && !isSaving

    Dialog(onDismissRequest = { if (!isSaving) onDismiss() }) {
        TempoDialogSurface {
            TempoDialogIcon(
                icon = TempoIcons.Edit,
                tint = appearancePalette.accent,
                size = 48
            )
            Spacer(modifier = Modifier.height(16.dp))
            TempoDialogTitle(text = stringResource(R.string.details_edit_title_dialog_title))
            Spacer(modifier = Modifier.height(6.dp))
            TempoDialogBody(text = "Adjust the track title to fix scrobble typos or match your library.")
            Spacer(modifier = Modifier.height(20.dp))

            TempoDialogTextField(
                value = titleText,
                onValueChange = {
                    if (it.length <= 200) {
                        titleText = it
                        if (error != null) onClearWarnings()
                    }
                },
                label = stringResource(R.string.details_edit_title_label),
                isError = error != null || trimmed.isEmpty(),
                supportingText = when {
                    error != null -> error
                    trimmed.isEmpty() -> stringResource(R.string.details_edit_title_empty)
                    isUnchanged -> stringResource(R.string.details_edit_title_unchanged)
                    else -> "${titleText.length} / 200"
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isSaving) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = appearancePalette.accent)
                }
            } else {
                TempoDialogButtonRow(
                    primaryText = stringResource(R.string.common_save),
                    onPrimary = { onSave(trimmed) },
                    secondaryText = stringResource(R.string.common_cancel),
                    onSecondary = onDismiss,
                    primaryEnabled = canSave
                )
            }
        }
    }
}

@Composable
internal fun MergeConfirmDialog(
    target: me.avinas.tempo.data.local.entities.Track,
    isMerging: Boolean,
    error: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val appearancePalette = LocalTempoPalette.current
    Dialog(onDismissRequest = { if (!isMerging) onDismiss() }) {
        TempoDialogSurface {
            TempoDialogIcon(
                icon = TempoIcons.MergeStreams,
                tint = appearancePalette.accent,
                size = 48
            )
            Spacer(modifier = Modifier.height(16.dp))
            TempoDialogTitle(text = stringResource(R.string.details_edit_title_merge_title))
            Spacer(modifier = Modifier.height(8.dp))
            TempoDialogBody(
                text = stringResource(
                    R.string.details_edit_title_merge_message,
                    target.title,
                    target.artist
                )
            )

            // Failure feedback lives in the dialog that triggered it: dismissing the
            // dialog was previously the only outcome of a merge that did not happen.
            if (error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = TempoError,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isMerging) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = appearancePalette.accent)
                }
            } else {
                TempoDialogButtonRow(
                    primaryText = stringResource(R.string.details_edit_title_merge_button),
                    onPrimary = onConfirm,
                    secondaryText = stringResource(R.string.common_cancel),
                    onSecondary = onDismiss
                )
            }
        }
    }
}

@Composable
internal fun DeleteSongConfirmDialog(
    trackDetails: TrackDetails,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val appearancePalette = LocalTempoPalette.current
    Dialog(onDismissRequest = { if (!isDeleting) onDismiss() }) {
        TempoDialogSurface {
            TempoDialogIcon(
                icon = TempoIcons.Trash,
                tint = TempoError,
                size = 48
            )
            Spacer(modifier = Modifier.height(16.dp))
            TempoDialogTitle(text = stringResource(R.string.details_delete_song_title))
            Spacer(modifier = Modifier.height(6.dp))
            TempoDialogBody(text = "“${trackDetails.track.title}” by ${trackDetails.track.artist}")
            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(TempoError.copy(alpha = 0.08f))
                    .border(0.5.dp, TempoError.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.details_delete_song_warning),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TempoError
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• ${stringResource(R.string.details_delete_song_bullet_history, trackDetails.playCount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = appearancePalette.mutedText
                    )
                    Text(
                        text = "• ${stringResource(R.string.details_delete_song_undone)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = appearancePalette.mutedText
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isDeleting) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = TempoError)
                }
            } else {
                TempoDialogDangerButton(
                    text = stringResource(R.string.history_delete_button),
                    onClick = onConfirm,
                    icon = TempoIcons.Trash
                )
                Spacer(modifier = Modifier.height(6.dp))
                TempoDialogSecondaryButton(
                    text = stringResource(R.string.common_cancel),
                    onClick = onDismiss
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Footer
// ──────────────────────────────────────────────────────────────

@Composable
internal fun SongDetailsFooter() {
    val appearancePalette = LocalTempoPalette.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(1.dp)
                .background(appearancePalette.border.copy(alpha = 0.45f)),
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.details_footer).uppercase(Locale.getDefault()),
            style = KickerSmall,
            color = appearancePalette.mutedText,
            letterSpacing = 2.sp,
        )
    }
}

// ──────────────────────────────────────────────────────────────
// Helpers & Motion Primitives
// ──────────────────────────────────────────────────────────────

internal fun formatSongDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/** Formats timestamp as "MMM d, yyyy" (e.g., "Jun 12, 2023"). */
internal fun formatFlowDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

internal fun formatChartDate(isoDate: String): String {
    if (isoDate.isBlank()) return ""
    return try {
        java.time.LocalDate.parse(isoDate.take(10))
            .format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))
    } catch (_: Exception) {
        isoDate
    }
}

internal fun parseIsoToTimestamp(isoDate: String): Long {
    return try {
        val localDate = java.time.LocalDate.parse(isoDate.take(10))
        localDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    } catch (_: Exception) {
        System.currentTimeMillis()
    }
}

fun formatReleaseDate(dateString: String?): String? {
    if (dateString.isNullOrBlank()) return null
    return try {
        val date = java.time.LocalDate.parse(dateString.take(10))
        java.time.format.DateTimeFormatter
            .ofPattern("MMMM d, yyyy")
            .format(date)
    } catch (_: Exception) {
        null
    }
}

/**
 * Keeps artwork-derived accents usable on dark glass: tames neon saturation
 * and lifts near-black swatches into a readable accent band. Grays keep their
 * hue (coercing saturation on a gray would fabricate a red tint).
 */
internal fun conditionedAccent(color: Color): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    if (hsv[1] < 0.12f) {
        hsv[2] = hsv[2].coerceIn(0.55f, 0.80f)
    } else {
        hsv[1] = hsv[1].coerceIn(0.30f, 0.72f)
        hsv[2] = hsv[2].coerceIn(0.52f, 0.82f)
    }
    return Color(android.graphics.Color.HSVToColor(hsv))
}

@Composable
internal fun rememberDominantColor(trackDetails: TrackDetails): Color = rememberArtworkAccent(
    trackDetails.track.albumArtUrl,
    trackDetails.localBackupArtUrl,
)
