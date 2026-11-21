package me.avinas.tempo.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.avinas.tempo.R
import me.avinas.tempo.data.local.entities.Badge
import me.avinas.tempo.data.stats.ArtistDetails
import me.avinas.tempo.data.stats.GamificationEngine
import me.avinas.tempo.data.stats.TrackDetails
import me.avinas.tempo.ui.profile.BadgeEmblem
import me.avinas.tempo.ui.profile.getCategoryLabel
import me.avinas.tempo.ui.profile.getLevelTierAccent
import me.avinas.tempo.ui.profile.getRarityColor
import me.avinas.tempo.ui.profile.getUniqueBadgeColor
import me.avinas.tempo.ui.theme.DisplayFontFamily
import me.avinas.tempo.ui.theme.TempoDarkBackground
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Common background for share cards. Themed via [ShareThemePalette] so every
 * share surface (stats, song, artist) offers the same theme set and keeps
 * text readable on both dark and light backdrops.
 */
@Composable
fun SongShareCard(
    trackDetails: TrackDetails,
    theme: ShareTheme = ShareTheme.MIDNIGHT,
    modifier: Modifier = Modifier,
    genre: String? = null,
    mood: String? = null,
    habitualHour: String? = null,
    peakBingeDay: Pair<String, Int>? = null,
    completionRate: Float? = null,
    skipRate: Float? = null,
    releaseYear: Int? = null,
) {
    val palette = theme.palette
    ShareCardBackground(
        imageUrl = trackDetails.track.albumArtUrl,
        palette = palette,
        modifier = modifier.aspectRatio(9f / 16f),
    ) {
        FitToHeight(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 26.dp, bottom = 64.dp, start = 24.dp, end = 24.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Standing / Flex Status Kicker
                val standingKicker =
                    remember(trackDetails.peakRank, trackDetails.playCount, completionRate, skipRate) {
                        resolveSongStandingKicker(
                            peakRank = trackDetails.peakRank,
                            playCount = trackDetails.playCount,
                            completionRate = completionRate,
                            skipRate = skipRate,
                        )
                    }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(palette.surface)
                            .border(1.dp, palette.divider, RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = standingKicker,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (palette.isDark) palette.accent else palette.textStrong,
                        letterSpacing = 1.5.sp,
                        fontSize = 10.sp,
                    )
                }

                // Hero Album Art with Glow & Favorite Badge
                Box(
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(190.dp)
                                .background(
                                    brush =
                                        Brush.radialGradient(
                                            colors =
                                                listOf(
                                                    palette.heroGlow.copy(alpha = 0.85f),
                                                    Color.Transparent,
                                                ),
                                        ),
                                    shape = CircleShape,
                                ),
                    )

                    GlassCard(
                        modifier = Modifier.size(192.dp),
                        shape = palette.cardShape,
                    ) {
                        CachedAsyncImage(
                            imageUrl = trackDetails.track.albumArtUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            allowHardware = false,
                            placeholder = {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(palette.cellPlaceholder),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = palette.textSecondary)
                                }
                            },
                        )
                    }

                    if (trackDetails.isFavorite) {
                        Box(
                            modifier =
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 6.dp, y = (-6).dp)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE11D48))
                                    .border(1.5.dp, Color.White.copy(alpha = 0.85f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }

                // Track Info (Title, Artist, Album/Year)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = trackDetails.track.title,
                        style =
                            MaterialTheme.typography.titleLarge.copy(
                                fontWeight = palette.headlineWeight ?: FontWeight.Black,
                                fontSize = 22.sp,
                                lineHeight = 26.sp,
                            ),
                        color = palette.textPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )

                    Text(
                        text = trackDetails.track.artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (palette.isDark) Color(0xFFE9D5FF) else palette.accent,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                    )

                    val albumYearText =
                        remember(trackDetails.track.album, releaseYear) {
                            val album = trackDetails.track.album?.takeIf { it.isNotBlank() }
                            when {
                                album != null && releaseYear != null -> "$album • $releaseYear"
                                album != null -> album
                                releaseYear != null -> "Released $releaseYear"
                                else -> null
                            }
                        }
                    if (albumYearText != null) {
                        Text(
                            text = albumYearText,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }

                // Col 3 flex stat resolved before card
                val flexStat =
                    remember(
                        trackDetails.firstPlayed,
                        trackDetails.peakRank,
                        completionRate,
                        trackDetails.isFavorite,
                    ) {
                        resolveSongFlexStat(
                            firstPlayed = trackDetails.firstPlayed,
                            peakRank = trackDetails.peakRank,
                            completionRate = completionRate,
                            isFavorite = trackDetails.isFavorite,
                        )
                    }

                // 3 Core Flex Stats Grid: Plays | Time | Standout Flex
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = palette.surface,
                    shape = palette.cardShape,
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Col 1: PLAYS
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = if (palette.isDark) Color(0xFFF472B6) else palette.accent,
                                    modifier = Modifier.size(12.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.share_plays_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary,
                                    letterSpacing = palette.labelTracking ?: 1.5.sp,
                                    fontSize = 9.sp,
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = trackDetails.playCount.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = palette.headlineWeight ?: FontWeight.Black,
                                color = palette.textPrimary,
                                fontSize = 20.sp,
                            )
                        }

                        // Vertical Divider with generous breathing room
                        Box(
                            modifier =
                                Modifier
                                    .padding(horizontal = 8.dp)
                                    .height(26.dp)
                                    .width(1.dp)
                                    .background(palette.divider),
                        )

                        // Col 2: TIME
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = if (palette.isDark) Color(0xFFC084FC) else palette.accent,
                                    modifier = Modifier.size(12.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "TIME",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary,
                                    letterSpacing = palette.labelTracking ?: 1.5.sp,
                                    fontSize = 9.sp,
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            val formattedTime = me.avinas.tempo.ui.stats.displayStatsDuration(trackDetails.totalTimeMs)
                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = palette.headlineWeight ?: FontWeight.Black,
                                color = palette.textPrimary,
                                fontSize = 20.sp,
                            )
                        }

                        // Vertical Divider with generous breathing room
                        Box(
                            modifier =
                                Modifier
                                    .padding(horizontal = 8.dp)
                                    .height(26.dp)
                                    .width(1.dp)
                                    .background(palette.divider),
                        )

                        // Col 3: THE FLEX (Discovery Date / Peak Rank / Finish Rate)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = flexStat.icon,
                                    contentDescription = null,
                                    tint = if (palette.isDark) Color(0xFF38BDF8) else palette.accent,
                                    modifier = Modifier.size(12.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = flexStat.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary,
                                    letterSpacing = palette.labelTracking ?: 1.5.sp,
                                    fontSize = 9.sp,
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = flexStat.value,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = palette.headlineWeight ?: FontWeight.Black,
                                color = palette.textPrimary,
                                fontSize = 20.sp,
                            )
                        }
                    }
                }

                // Standout Flex Highlight Pill (Cherry-picked editorial flex)
                val flexHighlight =
                    remember(peakBingeDay, habitualHour, skipRate, mood, genre) {
                        resolveSongFlexHighlight(
                            peakBingeDay = peakBingeDay,
                            habitualHour = habitualHour,
                            skipRate = skipRate,
                            playCount = trackDetails.playCount,
                            mood = mood,
                            genre = genre,
                        )
                    }

                if (flexHighlight != null) {
                    GlassCard(
                        shape = RoundedCornerShape(50),
                        backgroundColor = palette.surfaceStrong,
                        borderColor = palette.divider,
                        borderWidth = 1.dp,
                        fillMaxWidth = false,
                    ) {
                        Text(
                            text = flexHighlight,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textPrimary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                }

                // Mini Equalizer Waveform Bars (Tactile music player aesthetic)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    val barHeights = listOf(6.dp, 14.dp, 10.dp, 18.dp, 12.dp, 16.dp, 8.dp, 12.dp, 6.dp)
                    barHeights.forEach { h ->
                        Box(
                            modifier =
                                Modifier
                                    .width(3.dp)
                                    .height(h)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (palette.isDark) {
                                            palette.accent.copy(alpha = 0.5f)
                                        } else {
                                            palette.textSecondary.copy(alpha = 0.4f)
                                        },
                                    ),
                        )
                    }
                }
            }
        }
    }
}

internal data class SongFlexStat(
    val label: String,
    val value: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

internal fun resolveSongStandingKicker(
    peakRank: Int?,
    playCount: Int,
    completionRate: Float?,
    skipRate: Float?,
): String =
    when {
        peakRank == 1 -> "👑 #1 ALL-TIME TRACK"
        peakRank != null && peakRank <= 5 -> "🌟 TOP $peakRank IN LIBRARY"
        peakRank != null && peakRank <= 20 -> "🔥 TOP $peakRank ROTATION"
        playCount >= 100 -> "⚡ CENTURY CLUB • 100+ SPINS"
        playCount >= 50 -> "⚡ HEAVY ROTATION"
        skipRate != null && skipRate < 0.05f && playCount >= 15 -> "🎧 ZERO SKIPS • DEEP ROTATION"
        else -> "TEMPO • TRACK HIGHLIGHT"
    }

internal fun resolveSongFlexStat(
    firstPlayed: Long?,
    peakRank: Int?,
    completionRate: Float?,
    isFavorite: Boolean,
): SongFlexStat =
    when {
        firstPlayed != null && firstPlayed > 0L -> {
            val sdf = SimpleDateFormat("MMM ''yy", Locale.getDefault())
            SongFlexStat("SINCE", sdf.format(Date(firstPlayed)), Icons.Default.History)
        }

        peakRank != null -> {
            SongFlexStat("PEAK", "#$peakRank", Icons.Default.EmojiEvents)
        }

        completionRate != null -> {
            SongFlexStat("FINISH", "${(completionRate * 100).toInt()}%", Icons.Default.CheckCircle)
        }

        else -> {
            SongFlexStat("STATUS", if (isFavorite) "Fav" else "Spun", Icons.Default.Star)
        }
    }

internal fun resolveSongFlexHighlight(
    peakBingeDay: Pair<String, Int>?,
    habitualHour: String?,
    skipRate: Float?,
    playCount: Int,
    mood: String?,
    genre: String?,
): String? =
    when {
        peakBingeDay != null && peakBingeDay.second >= 4 -> {
            "🔥 Binge Record: ${peakBingeDay.second} plays in a day"
        }

        habitualHour != null -> {
            "🌙 Signature ${habitualHour.lowercase()} soundtrack"
        }

        skipRate != null && skipRate < 0.08f && playCount >= 10 -> {
            "✨ 100% Loyalty • Never Skipped"
        }

        mood != null -> {
            "✨ Vibe: $mood"
        }

        genre != null -> {
            "🎧 $genre"
        }

        else -> {
            null
        }
    }

internal fun resolveArtistStandingKicker(
    percentile: Double?,
    playCount: Int,
): String =
    when {
        percentile != null && percentile == 0.0 && playCount >= 10 -> "👑 #1 ALL-TIME ARTIST"
        percentile != null && percentile <= 1.0 && playCount >= 25 -> "👑 TOP 1% IN YOUR LIBRARY"
        percentile != null && percentile <= 5.0 && playCount >= 20 -> "🌟 TOP 5% IN YOUR LIBRARY"
        percentile != null && percentile <= 10.0 && playCount >= 15 -> "🔥 TOP 10% IN YOUR LIBRARY"
        playCount >= 500 -> "⚡ CENTURY CLUB • 500+ SPINS"
        playCount >= 200 -> "⚡ HEAVY ROTATION • 200+ SPINS"
        playCount >= 50 -> "🎧 ROTATION REGULAR"
        else -> "TEMPO • ARTIST SPOTLIGHT"
    }

internal fun resolveArtistFlexPill(
    listeningStreakDays: Int,
    firstListenedDate: Long?,
    firstDiscoveryTimestamp: Long?,
    topGenre: String? = null,
): String? =
    when {
        listeningStreakDays >= 3 -> {
            "🔥 $listeningStreakDays-day listening streak"
        }

        firstListenedDate != null && firstListenedDate > 0L -> {
            val sdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
            "🎧 Listening since ${sdf.format(Date(firstListenedDate))}"
        }

        firstDiscoveryTimestamp != null && firstDiscoveryTimestamp > 0L -> {
            val sdf = SimpleDateFormat("yyyy", Locale.getDefault())
            "🎧 Discovered in ${sdf.format(Date(firstDiscoveryTimestamp))}"
        }

        !topGenre.isNullOrBlank() -> {
            "🎵 $topGenre"
        }

        else -> {
            null
        }
    }

/**
 * 9:16 Optimized Share Card for Badges / Trophy Achievements
 */
