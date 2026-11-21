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
internal fun TopTracksSection(
    tracks: List<TopTrack>,
    accent: Color,
    onTrackClick: (TopTrack) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        variant = GlassCardVariant.QuietGlass,
        accentColor = accent,
        borderColor = GlassBorderSoft,
        borderWidth = 0.8.dp,
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            ) {
                SectionCatalogKicker(
                    number = "02",
                    label = "TOP TRACKS"
                )

                Text(
                    text = "${tracks.size} ${if (tracks.size == 1) "track" else "tracks"}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = todayMutedText()
                )
            }

            tracks.forEachIndexed { index, track ->
                TrackRankRow(
                    rank = index + 1,
                    track = track,
                    accent = accent,
                    onClick = { onTrackClick(track) }
                )
                if (index < tracks.lastIndex) {
                    Box(
                        modifier = Modifier
                            .padding(start = 74.dp)
                            .fillMaxWidth()
                            .height(0.6.dp)
                            .background(GlassBorderSoft)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
internal fun TrackRankRow(
    rank: Int,
    track: TopTrack,
    accent: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .premiumClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Rank podium medal or clean numeral
        RankMedalOrNumeral(rank = rank, accent = accent)

        // Rounded artwork
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.10f))
        ) {
            CachedAsyncImage(
                imageUrl = track.albumArtUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                targetSizeDp = 100,
                placeholder = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(accent.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = accent.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
        }

        // Title and artist
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
                color = LocalTempoPalette.current.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                style = CaptionSmall,
                fontSize = 11.5.sp,
                color = todayMutedText(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Right-aligned play count & duration column
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = "${track.playCount}",
                style = MaterialTheme.typography.titleSmall.copy(fontFamily = DisplayFontFamily),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (rank == 1) accent else LocalTempoPalette.current.onSurface
            )
            Text(
                text = formatTrackDuration(track.totalTimeMs),
                style = CaptionSmall,
                fontSize = 10.sp,
                color = todayMutedText()
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
            contentDescription = null,
            tint = todayMutedText(),
            modifier = Modifier.size(11.dp)
        )
    }
}

// ──────────────────────────────────────────────────────────────
// 6. Section 03: Top Artists (Distinct Circular Avatar Styling)
// ──────────────────────────────────────────────────────────────

@Composable
internal fun TopArtistsSection(
    artists: List<TopArtist>,
    accent: Color,
    onArtistClick: (TopArtist) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        variant = GlassCardVariant.QuietGlass,
        accentColor = accent,
        borderColor = GlassBorderSoft,
        borderWidth = 0.8.dp,
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            ) {
                SectionCatalogKicker(
                    number = "03",
                    label = "TOP ARTISTS"
                )

                Text(
                    text = "${artists.size} ${if (artists.size == 1) "artist" else "artists"}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = todayMutedText()
                )
            }

            artists.forEachIndexed { index, artist ->
                ArtistRankRow(
                    rank = index + 1,
                    artist = artist,
                    accent = accent,
                    onClick = { onArtistClick(artist) }
                )
                if (index < artists.lastIndex) {
                    Box(
                        modifier = Modifier
                            .padding(start = 74.dp)
                            .fillMaxWidth()
                            .height(0.6.dp)
                            .background(GlassBorderSoft)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
internal fun ArtistRankRow(
    rank: Int,
    artist: TopArtist,
    accent: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .premiumClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Rank medal or quiet numeral
        RankMedalOrNumeral(rank = rank, accent = accent)

        // Circular artist portrait
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(TempoDarkSurfaceSunken)
                .border(1.dp, GlassBorderSoft, CircleShape)
        ) {
            CachedAsyncImage(
                imageUrl = artist.imageUrl,
                contentDescription = artist.artist,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                targetSizeDp = 100,
                placeholder = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = artist.artist.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = DisplayFontFamily),
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                    }
                }
            )
        }

        // Artist name and unique tracks count
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = artist.artist,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
                color = LocalTempoPalette.current.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${artist.uniqueTracks} ${if (artist.uniqueTracks == 1) "track" else "tracks"} today",
                style = CaptionSmall,
                fontSize = 11.5.sp,
                color = todayMutedText(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Right-aligned listening duration & play count
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = me.avinas.tempo.ui.stats.displayStatsDuration(artist.totalTimeMs),
                style = MaterialTheme.typography.titleSmall.copy(fontFamily = DisplayFontFamily),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (rank == 1) accent else LocalTempoPalette.current.onSurface
            )
            Text(
                text = "${artist.playCount} PLAYS",
                style = CaptionSmall,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp,
                color = todayMutedText()
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
            contentDescription = null,
            tint = todayMutedText(),
            modifier = Modifier.size(11.dp)
        )
    }
}

/** Medal badge for ranks 1 to 3, or a plain rank number for 4 and above. */
@Composable
internal fun RankMedalOrNumeral(rank: Int, accent: Color) {
    if (rank <= 3) {
        val medalColor = when (rank) {
            1 -> RankGold
            2 -> RankSilver
            else -> RankBronze
        }
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(medalColor.copy(alpha = 0.16f), CircleShape)
                .border(1.dp, medalColor.copy(alpha = 0.65f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$rank",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = medalColor
            )
        }
    } else {
        Text(
            text = "$rank",
            style = MaterialTheme.typography.titleSmall.copy(fontFamily = DisplayFontFamily),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = todayMutedText(),
            textAlign = TextAlign.Center,
            modifier = Modifier.width(22.dp)
        )
    }
}

// ──────────────────────────────────────────────────────────────
// 7. Footer Actions & Editorial Signature
// ──────────────────────────────────────────────────────────────

@Composable
internal fun FooterActions(
    accent: Color,
    onClose: () -> Unit,
    onViewAllStats: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .weight(1.6f)
                .height(50.dp)
                .shadow(10.dp, RoundedCornerShape(25.dp), ambientColor = Color.Black.copy(alpha = 0.35f))
                .clip(RoundedCornerShape(25.dp))
                .background(accent)
                .premiumClickable(onClick = onViewAllStats)
        ) {
            Text(
                text = "Full stats hub",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = readableProfileColor(LocalTempoPalette.current.onAccent, accent, 4.5f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
                tint = readableProfileColor(LocalTempoPalette.current.onAccent, accent, 4.5f),
                modifier = Modifier.size(11.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .clip(RoundedCornerShape(25.dp))
                .background(GlassFrostSoft)
                .border(0.8.dp, GlassBorderSoft, RoundedCornerShape(25.dp))
                .premiumClickable(onClick = onClose)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = todayMutedText(),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "Close",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = todayMutedText()
            )
        }
    }
}

@Composable
internal fun TodaysOverviewFooter() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(0.8.dp)
                .background(GlassBorderSoft)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "TEMPO · TODAY'S OVERVIEW",
            style = KickerSmall,
            color = todayMutedText(),
            letterSpacing = 2.sp
        )
    }
}

// ──────────────────────────────────────────────────────────────
// Accent Extraction via Coil & Android Palette
// ──────────────────────────────────────────────────────────────

@Composable
internal fun rememberTodayAccentColor(imageUrl: String?): Color {
    var accent by remember { mutableStateOf(TempoPrimary) }
    val context = LocalContext.current

    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrBlank()) return@LaunchedEffect
        val result = context.imageLoader.execute(
            ImageRequest.Builder(context)
                .data(imageUrl)
                .size(64, 64)
                .build()
        )
        var bitmap = (result.image as? BitmapImage)?.bitmap ?: return@LaunchedEffect
        if (bitmap.config == android.graphics.Bitmap.Config.HARDWARE) {
            bitmap = bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
        }
        Palette.from(bitmap).generate { palette ->
            val color = palette?.let {
                it.vibrantSwatch?.rgb
                    ?: it.mutedSwatch?.rgb
                    ?: it.dominantSwatch?.rgb
            }?.let { conditionedAccent(Color(it)) } ?: TempoPrimary
            accent = color
        }
    }
    return accent
}

// ──────────────────────────────────────────────────────────────
// Formatters
// ──────────────────────────────────────────────────────────────

internal fun formatTrackDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return when {
        minutes == 0L -> "${seconds}s"
        else -> String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

internal fun formatHourLabel(hour: Int): String {
    val h = when (val mod = hour % 12) {
        0 -> 12
        else -> mod
    }
    return "$h ${if (hour >= 12) "PM" else "AM"}"
}
