@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.avinas.tempo.ui.details

import me.avinas.tempo.ui.effects.tempoAmbientBackdrop
import me.avinas.tempo.ui.effects.tempoTiltSurface
import me.avinas.tempo.ui.effects.tempoListMotion

/* Tempo · AlbumDetails · Editorial album listening profile.
 * Shares the SongDetails design language: art-as-atmosphere background,
 * collapsing header title, frosted top-bar actions, a conditioned
 * dominant-color accent and the Obsidian stat masthead — extended with
 * the album's track sheet and inline edit mode.
 */

import me.avinas.tempo.ui.components.ShareSnapshot
import me.avinas.tempo.ui.components.SnapshotShareDialog
import androidx.compose.material.icons.filled.Share
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import me.avinas.tempo.R
import me.avinas.tempo.data.enrichment.MusicBrainzEnrichmentService
import me.avinas.tempo.data.local.entities.Track
import me.avinas.tempo.data.stats.AlbumDetails
import me.avinas.tempo.data.stats.TrackWithStats
import me.avinas.tempo.ui.components.AlbumArtImage
import me.avinas.tempo.ui.components.ArtAtmosphereLayer
import me.avinas.tempo.ui.components.DeepOceanBackground
import me.avinas.tempo.ui.components.GlassCard
import me.avinas.tempo.ui.components.GlassCardVariant
import me.avinas.tempo.ui.components.TempoDialogShape
import me.avinas.tempo.ui.theme.*
import me.avinas.tempo.ui.components.TempoDropdownMenu
import me.avinas.tempo.ui.components.TempoDropdownMenuItem
import me.avinas.tempo.ui.components.TempoDialogSurface
import me.avinas.tempo.ui.components.TempoDialogIcon
import me.avinas.tempo.ui.components.TempoDialogTitle
import me.avinas.tempo.ui.components.TempoDialogBody
import me.avinas.tempo.ui.components.TempoDialogDangerButton
import me.avinas.tempo.ui.components.TempoDialogSecondaryButton
import me.avinas.tempo.ui.components.TempoIcons
import java.util.Locale

@Composable
internal fun AlbumHeroEditorialStage(
    albumDetails: AlbumDetails,
    dominantColor: Color,
    onNavigateToArtist: (Long) -> Unit,
    onTitlePositioned: (Float) -> Unit,
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
                    albumArtUrl = albumDetails.album.artworkUrl,
                    contentDescription = stringResource(
                        R.string.details_cover_artwork_cd, albumDetails.album.title
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

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
                text = albumDetails.album.title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = DisplayFontFamily,
                    letterSpacing = (-0.5).sp,
                ),
                fontWeight = FontWeight.Bold,
                color = appearancePalette.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }


        // Release-style metadata: hairline divider over a kicker meta line,
        // matching the artist hero treatment.
        val metaParts = listOfNotNull(
            albumDetails.album.releaseYear?.toString(),
            albumDetails.album.releaseType
                ?.takeIf { it.isNotBlank() }
                ?.replaceFirstChar { it.uppercase(Locale.getDefault()) },
        )
        if (metaParts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
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
                    modifier = Modifier.padding(horizontal = 24.dp),
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

// ──────────────────────────────────────────────────────────────
// 2. Master Stats Masthead
// ──────────────────────────────────────────────────────────────

@Composable
internal fun AlbumStatMasthead(
    albumDetails: AlbumDetails,
    dominantColor: Color,
) {
    val appearancePalette = LocalTempoPalette.current
    val hasPlays = albumDetails.totalPlayCount > 0
    val completion = albumDetails.completionRate
    val completionText = if (hasPlays) {
        String.format(Locale.getDefault(), "%.0f%%", completion)
    } else {
        "—"
    }
    val completionSubtext = when {
        !hasPlays -> stringResource(R.string.details_awaiting_playback)
        completion >= 90 -> stringResource(R.string.details_completion_to_end)
        completion >= 70 -> stringResource(R.string.details_completion_high)
        completion >= 50 -> stringResource(R.string.details_completion_moderate)
        else -> stringResource(R.string.details_completion_skipped)
    }
    val runtimeMs = remember(albumDetails) {
        albumDetails.tracks.sumOf { it.track.duration ?: 0L }
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        variant = GlassCardVariant.Obsidian,
        borderColor = appearancePalette.border.copy(alpha = 0.45f),
        borderWidth = 0.8.dp,
        contentPadding = PaddingValues(0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 20.dp)) {
            // Hero metric — same anatomy as the song and artist mastheads:
            // display-size play count with the listening time as its suffix.
            MastheadHeroMetric(
                label = stringResource(R.string.details_total_plays),
                value = String.format(Locale.getDefault(), "%,d", albumDetails.totalPlayCount),
                suffix = stringResource(
                    R.string.details_together_suffix,
                    me.avinas.tempo.ui.stats.displayStatsDuration(albumDetails.totalTimeMs),
                ),
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

            // Secondary row: track count with runtime, and the completion
            // rate — the one tint with meaning.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MastheadSecondaryStat(
                    label = stringResource(R.string.details_tracks),
                    value = "${albumDetails.tracks.size}",
                    subtext = if (runtimeMs > 0) {
                        formatListeningTime(runtimeMs)
                    } else {
                        stringResource(R.string.details_stat_recorded_library)
                    },
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
                    value = completionText,
                    subtext = completionSubtext,
                    valueColor = when {
                        !hasPlays -> appearancePalette.onSurface
                        completion >= 70 -> TempoSuccess
                        else -> TempoWarning
                    },
                    modifier = Modifier.weight(1f).padding(start = 16.dp),
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// 3. Track Sheet
// ──────────────────────────────────────────────────────────────

/**
 * Kicker + track count + edit-mode add affordance.
 *
 * Its own LazyColumn item directly above the track rows (see [AlbumTrackRow]),
 * so the header never forces those rows to compose.
 */
@Composable
internal fun AlbumTrackSheetHeader(
    trackCount: Int,
    isEditMode: Boolean,
    onAddClick: () -> Unit,
) {
    val appearancePalette = LocalTempoPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionCatalogKicker(
            number = "01",
            label = stringResource(R.string.details_tracks),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(
                    if (trackCount == 1) R.string.album_one_song else R.string.album_songs_count,
                    trackCount,
                ),
                style = CaptionSmall,
                color = appearancePalette.mutedText,
                fontWeight = FontWeight.Medium,
            )
            if (isEditMode) {
                AddSongPill(onClick = onAddClick)
            }
        }
    }
}

@Composable
internal fun AlbumEmptyTracksCard(tint: Color) {
    val appearancePalette = LocalTempoPalette.current
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        variant = GlassCardVariant.QuietGlass,
        accentColor = tint,
        contentPadding = PaddingValues(24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                contentDescription = null,
                tint = appearancePalette.mutedText,
                modifier = Modifier.size(30.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.album_no_tracks),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = appearancePalette.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.album_no_tracks_hint),
                style = MaterialTheme.typography.bodySmall,
                color = appearancePalette.mutedText,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * One lazily-composed track row of the track sheet.
 *
 * Replaces the eager `tracks.forEachIndexed` Column that used to sit inside a
 * single wraparound [GlassCard]: the card chrome (glass gradient, accent border,
 * hairline divider) is now applied per row, so the outer 20dp corners live on
 * the first/last row and the hairline sits inside each row (skipped on the last)
 * to stay clipped by those corners. Row height is unchanged — content padding
 * (11.2dp top / bottom) + the original 0.8dp [GlassBorderSoft] hairline == the
 * old 12dp + hairline.
 */
@Composable
internal fun AlbumTrackRow(
    index: Int,
    track: TrackWithStats,
    tint: Color,
    isEditMode: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onNavigateToSong: (Long) -> Unit,
) {
    val appearancePalette = LocalTempoPalette.current
    val rowShape = RoundedCornerShape(
        topStart = if (isFirst) 20.dp else 0.dp,
        topEnd = if (isFirst) 20.dp else 0.dp,
        bottomEnd = if (isLast) 20.dp else 0.dp,
        bottomStart = if (isLast) 20.dp else 0.dp,
    )

    Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp)) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = rowShape,
            variant = GlassCardVariant.QuietGlass,
            accentColor = tint,
            contentPadding = PaddingValues(0.dp),
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isEditMode) Modifier
                            else Modifier.premiumClickable(onClick = { onNavigateToSong(track.track.id) })
                        )
                        .padding(horizontal = 16.dp, vertical = 11.2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = DisplayFontFamily,
                        ),
                        fontWeight = FontWeight.Bold,
                        color = if (track.playCount > 0) tint else TextQuaternary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(28.dp),
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.track.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = appearancePalette.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        val duration = track.track.duration
                        val durationLabel = if (duration != null && duration > 0) {
                            formatDuration(duration)
                        } else {
                            null
                        }
                        if (durationLabel != null) {
                            Text(
                                text = durationLabel,
                                style = CaptionSmall,
                                color = appearancePalette.mutedText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${track.playCount}",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = DisplayFontFamily,
                            ),
                            fontWeight = FontWeight.Bold,
                            color = appearancePalette.onSurface
                        )
                        Text(
                            text = stringResource(R.string.details_plays).uppercase(Locale.getDefault()),
                            style = CaptionSmall,
                            color = appearancePalette.mutedText,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                        contentDescription = null,
                        tint = appearancePalette.mutedText,
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .size(11.dp)
                    )
                }

                if (!isLast) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.8.dp)
                            .background(appearancePalette.border.copy(alpha = 0.45f))
                    )
                }
            }
        }
    }
}

@Composable
private fun AddSongPill(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(PillSurface)
            .premiumClickable(onClick = onClick, pressedScale = 0.96f)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            Icons.Rounded.Add,
            contentDescription = null,
            tint = PillTextPrimary,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = stringResource(R.string.album_add_song),
            color = PillTextPrimary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ──────────────────────────────────────────────────────────────
// 4. Footer
// ──────────────────────────────────────────────────────────────

@Composable
internal fun AlbumDetailsFooter() {
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
            text = stringResource(R.string.album_footer).uppercase(Locale.getDefault()),
            style = KickerSmall,
            color = appearancePalette.mutedText,
            letterSpacing = 2.sp,
        )
    }
}

// ──────────────────────────────────────────────────────────────
// Dialogs
// ──────────────────────────────────────────────────────────────

