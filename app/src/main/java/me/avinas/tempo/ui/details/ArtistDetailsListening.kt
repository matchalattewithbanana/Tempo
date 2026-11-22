@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.avinas.tempo.ui.details
import me.avinas.tempo.ui.util.observableLocale

import me.avinas.tempo.ui.effects.tempoAmbientBackdrop
import me.avinas.tempo.ui.effects.tempoTiltSurface
import me.avinas.tempo.ui.effects.tempoListMotion

/* Tempo · ArtistDetails · Listening Profile.
 * Mirrors the SongDetails editorial system:
 * - Frosted header with collapsed title on scroll + gradient scrim
 * - Dominant-color accent extracted from the artist image
 * - ArtAtmosphere room layer, skeleton loading, hairline stat masthead
 * - Design tokens (TextPrimary/Secondary/Tertiary, KickerSmall, DisplayFontFamily)
 * - Zero emojis, 100% vector icons
 */

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.*
import me.avinas.tempo.ui.components.TempoDropdownMenu
import me.avinas.tempo.ui.components.TempoDropdownMenuItem
import me.avinas.tempo.ui.components.TempoIcons
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import me.avinas.tempo.R
import me.avinas.tempo.data.stats.ArtistDetails
import me.avinas.tempo.data.stats.TopAlbum
import me.avinas.tempo.data.stats.TopTrack
import me.avinas.tempo.ui.components.ArtAtmosphereLayer
import me.avinas.tempo.ui.components.CachedAsyncImage
import me.avinas.tempo.ui.components.DeepOceanBackground
import me.avinas.tempo.ui.components.GlassCard
import me.avinas.tempo.ui.components.GlassCardVariant
import me.avinas.tempo.ui.components.LocalHideStreaks
import me.avinas.tempo.ui.components.SharePreviewDialog
import me.avinas.tempo.ui.components.ArtistShareCard
import me.avinas.tempo.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ListeningJourneySection(artistDetails: ArtistDetails, tint: Color = LocalTempoPalette.current.accent) {
    val appearancePalette = LocalTempoPalette.current
    val firstListen = artistDetails.firstListenedDate ?: artistDetails.firstDiscovery?.firstListenTimestamp
    val lastListen = artistDetails.lastListenedDate
    // "N days in a row with this artist" is still a streak, so it goes with the rest of them.
    val hideStreaks = LocalHideStreaks.current

    val milestones = listOfNotNull(
        firstListen?.let {
            OdysseyMilestone(
                icon = Icons.Rounded.AutoAwesome,
                title = stringResource(R.string.details_first_listen),
                value = formatRibbonDate(firstListen),
            )
        },
        artistDetails.listeningStreakDays.takeIf { it > 1 && !hideStreaks }?.let {
            OdysseyMilestone(
                icon = Icons.Rounded.LocalFireDepartment,
                title = stringResource(R.string.artist_streak_short),
                value = stringResource(R.string.details_trends_days, artistDetails.listeningStreakDays),
            )
        },
        artistDetails.peakListeningHour?.let {
            val isDay = artistDetails.peakListeningHour in 6..17
            OdysseyMilestone(
                icon = if (isDay) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay,
                title = stringResource(R.string.details_peak_hour),
                value = artistDetails.peakHourFormatted,
            )
        },
        lastListen?.let {
            OdysseyMilestone(
                icon = Icons.Rounded.Headphones,
                title = stringResource(R.string.details_last_listen),
                value = formatRibbonDate(lastListen),
            )
        },
    )
    if (milestones.isEmpty()) return

    // Same ink treatment as the song timeline: contrast flips when the
    // accent-derived backdrop gets bright.
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
            number = "01",
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

// ──────────────────────────────────────────────────────────────
// 5. Top Songs
// ──────────────────────────────────────────────────────────────

@Composable
fun TopSongsPanel(
    songs: List<TopTrack>,
    onNavigateToSong: (Long) -> Unit,
    accent: Color = LocalTempoPalette.current.accent,
) {
    val appearancePalette = LocalTempoPalette.current
    if (songs.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionCatalogKicker(
            number = "02",
            label = stringResource(R.string.details_top_songs),
        )

        Spacer(modifier = Modifier.height(12.dp))

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            variant = GlassCardVariant.QuietGlass,
            accentColor = accent,
            contentPadding = PaddingValues(0.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                songs.forEachIndexed { index, song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .premiumClickable(onClick = { onNavigateToSong(song.trackId) })
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = DisplayFontFamily,
                            ),
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            modifier = Modifier.width(30.dp),
                            textAlign = TextAlign.Start
                        )

                        CachedAsyncImage(
                            imageUrl = song.albumArtUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = appearancePalette.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = song.album ?: stringResource(R.string.details_unknown_album),
                                style = CaptionSmall,
                                color = appearancePalette.mutedText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${song.playCount}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = DisplayFontFamily,
                                ),
                                fontWeight = FontWeight.Bold,
                                color = appearancePalette.onSurface
                            )
                            Text(
                                text = stringResource(R.string.details_plays).uppercase(observableLocale()),
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

                    if (index < songs.lastIndex) {
                        Box(
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .fillMaxWidth()
                                .height(0.6.dp)
                                .background(appearancePalette.border.copy(alpha = 0.45f))
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// 6. Top Albums
// ──────────────────────────────────────────────────────────────

@Composable
fun TopAlbumCard(album: TopAlbum, accent: Color = LocalTempoPalette.current.accent) {
    val appearancePalette = LocalTempoPalette.current
    Column(
        modifier = Modifier
            .width(140.dp)
            .padding(bottom = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(14.dp),
                    ambientColor = appearancePalette.surfaceSunken.copy(alpha = 0.30f),
                    spotColor = accent.copy(alpha = 0.18f),
                )
                .clip(RoundedCornerShape(14.dp))
                .background(appearancePalette.surfaceSunken)
                .border(0.8.dp, appearancePalette.border.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (album.albumArtUrl.isNullOrBlank()) {
                Icon(
                    imageVector = Icons.Rounded.Album,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = appearancePalette.mutedText
                )
            } else {
                CachedAsyncImage(
                    imageUrl = album.albumArtUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = album.album,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = appearancePalette.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.details_plays_count, album.playCount),
            style = CaptionSmall,
            color = appearancePalette.mutedText,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

// ──────────────────────────────────────────────────────────────
// Accent color extraction
// ──────────────────────────────────────────────────────────────

@Composable
internal fun rememberArtistAccentColor(imageUrl: String?): Color = rememberArtworkAccent(imageUrl)

// ──────────────────────────────────────────────────────────────
// Formatting helpers (formatListeningTime is used by other screens)
// ──────────────────────────────────────────────────────────────

fun formatCount(count: Long): String {
    return when {
        count >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format(java.util.Locale.US, "%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

fun formatListeningTime(millis: Long): String {
    val totalMinutes = millis / 1000 / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "<1m"
    }
}

// ──────────────────────────────────────────────────────────────
// Footer
// ──────────────────────────────────────────────────────────────

@Composable
internal fun ArtistDetailsFooter() {
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
            text = stringResource(R.string.details_footer).uppercase(observableLocale()),
            style = KickerSmall,
            color = appearancePalette.mutedText,
            letterSpacing = 2.sp,
        )
    }
}

/** Formats timestamp as "MMM d, yyyy" — ribbon-safe short date. */
private fun formatRibbonDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

