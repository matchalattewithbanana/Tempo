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
fun ArtistShareCard(
    artistDetails: ArtistDetails,
    percentile: Double? = null,
    theme: ShareTheme = ShareTheme.MIDNIGHT,
    modifier: Modifier = Modifier,
) {
    val palette = theme.palette
    ShareCardBackground(
        imageUrl = artistDetails.artist.imageUrl,
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
                // Standing / Fan Status Kicker
                val standingKicker =
                    remember(percentile, artistDetails.personalPlayCount) {
                        resolveArtistStandingKicker(percentile, artistDetails.personalPlayCount)
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

                // Artist Image with Glow
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier =
                            Modifier
                                .size(144.dp)
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

                    Box(
                        modifier =
                            Modifier
                                .size(136.dp)
                                .clip(CircleShape)
                                .border(3.dp, palette.divider, CircleShape),
                    ) {
                        CachedAsyncImage(
                            imageUrl = artistDetails.artist.imageUrl,
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
                }

                // Artist Name
                Text(
                    text = artistDetails.artist.name,
                    style =
                        MaterialTheme.typography.titleLarge.copy(
                            fontWeight = palette.headlineWeight ?: FontWeight.Black,
                            fontSize = 24.sp,
                            lineHeight = 28.sp,
                        ),
                    color = palette.textPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )

                // Badges Row (Country + Top 1% Fan)
                if (artistDetails.country != null || artistDetails.personalPlayCount > 50) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (artistDetails.country != null) {
                            GlassCard(
                                shape = RoundedCornerShape(50),
                                backgroundColor = palette.surface,
                                fillMaxWidth = false,
                            ) {
                                Text(
                                    text = stringResource(R.string.share_country_format, artistDetails.country),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.textPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                        }

                        if (artistDetails.country != null && artistDetails.personalPlayCount > 50) {
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        if (artistDetails.personalPlayCount > 50) {
                            val (badgeText, badgeEmoji, badgeColor) =
                                if (percentile != null) {
                                    when {
                                        percentile == 0.0 -> Triple("#1 Artist", "👑", Color(0xFFFFD700))
                                        percentile <= 1.0 -> Triple(stringResource(R.string.fan_status_top_1), "👑", Color(0xFFFFD700))
                                        percentile <= 5.0 -> Triple(stringResource(R.string.fan_status_top_5), "🌟", Color(0xFFF59E0B))
                                        percentile <= 10.0 -> Triple(stringResource(R.string.fan_status_top_10), "🔥", Color(0xFFEF4444))
                                        percentile <= 25.0 -> Triple(stringResource(R.string.fan_status_top_25), "🎧", Color(0xFF3B82F6))
                                        percentile <= 50.0 -> Triple(stringResource(R.string.fan_status_top_50), "🎵", Color(0xFF8B5CF6))
                                        else -> Triple(stringResource(R.string.fan_status_listener), "🎵", Color(0xFF94A3B8))
                                    }
                                } else {
                                    when {
                                        artistDetails.personalPlayCount > 1000 -> {
                                            Triple(
                                                stringResource(R.string.fan_status_ultimate),
                                                "👑",
                                                Color(0xFFFFD700),
                                            )
                                        }

                                        artistDetails.personalPlayCount > 500 -> {
                                            Triple(
                                                stringResource(R.string.fan_status_super),
                                                "🌟",
                                                Color(0xFFF59E0B),
                                            )
                                        }

                                        artistDetails.personalPlayCount > 200 -> {
                                            Triple(
                                                stringResource(R.string.fan_status_big),
                                                "🔥",
                                                Color(0xFFEF4444),
                                            )
                                        }

                                        else -> {
                                            Triple(stringResource(R.string.fan_status_regular), "🎧", Color(0xFF3B82F6))
                                        }
                                    }
                                }
                            GlassCard(
                                shape = RoundedCornerShape(50),
                                backgroundColor = badgeColor.copy(alpha = 0.15f),
                                borderColor = badgeColor.copy(alpha = 0.4f),
                                borderWidth = 1.dp,
                                fillMaxWidth = false,
                            ) {
                                Text(
                                    text = "$badgeEmoji $badgeText",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (palette.isDark) badgeColor else palette.textStrong,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }

                // Optional Artist Flex Highlight Pill (streak, discovery date, or genre).
                // A hidden streak is passed through as 0 so the pill falls back to the
                // discovery date the card already knows rather than dropping the pill.
                val hideStreaks = LocalHideStreaks.current
                val artistFlexPill =
                    remember(
                        artistDetails.listeningStreakDays,
                        artistDetails.firstListenedDate,
                        artistDetails.firstDiscovery,
                        artistDetails.topGenres,
                        hideStreaks,
                    ) {
                        resolveArtistFlexPill(
                            listeningStreakDays = if (hideStreaks) 0 else artistDetails.listeningStreakDays,
                            firstListenedDate = artistDetails.firstListenedDate,
                            firstDiscoveryTimestamp = artistDetails.firstDiscovery?.firstListenTimestamp,
                            topGenre = artistDetails.topGenres.firstOrNull(),
                        )
                    }
                if (artistFlexPill != null) {
                    GlassCard(
                        shape = RoundedCornerShape(50),
                        backgroundColor = palette.surfaceStrong,
                        borderColor = palette.divider,
                        borderWidth = 1.dp,
                        fillMaxWidth = false,
                    ) {
                        Text(
                            text = artistFlexPill,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textPrimary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                }

                // Stats Grid (GlassCard) - 3 Columns: Plays, Time, Unique Songs
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
                                    text = "PLAYS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary,
                                    letterSpacing = palette.labelTracking ?: 1.5.sp,
                                    fontSize = 9.sp,
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = artistDetails.personalPlayCount.toString(),
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
                            val formattedTime = me.avinas.tempo.ui.stats.displayStatsDuration(artistDetails.personalTotalTimeMs)
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

                        // Col 3: SONGS
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = if (palette.isDark) Color(0xFF38BDF8) else palette.accent,
                                    modifier = Modifier.size(12.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "SONGS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary,
                                    letterSpacing = palette.labelTracking ?: 1.5.sp,
                                    fontSize = 9.sp,
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = artistDetails.uniqueTracksPlayed.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = palette.headlineWeight ?: FontWeight.Black,
                                color = palette.textPrimary,
                                fontSize = 20.sp,
                            )
                        }
                    }
                }

                // Top Songs Section
                if (artistDetails.topSongs.isNotEmpty()) {
                    GlassCard(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .border(1.dp, palette.divider, palette.cardShape),
                        backgroundColor = palette.surfaceStrong,
                        shape = palette.cardShape,
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.share_top_songs),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textPrimary,
                                fontWeight = palette.labelWeight ?: FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp),
                            )

                            artistDetails.topSongs.take(3).forEachIndexed { index, song ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 3.dp),
                                        color = palette.divider,
                                        thickness = 0.5.dp,
                                    )
                                }
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    val badgeColors =
                                        when (index) {
                                            0 -> listOf(Color(0xFFFBBF24), Color(0xFFF59E0B))
                                            1 -> listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8))
                                            else -> listOf(Color(0xFFCD7F32), Color(0xFFB45309))
                                        }
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(20.dp)
                                                .background(
                                                    brush = Brush.linearGradient(colors = badgeColors),
                                                    shape = palette.badgeShape,
                                                ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.contrastingText(badgeColors.first()),
                                            fontSize = 10.sp,
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        text = stringResource(R.string.share_plays_format, song.playCount),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.textSecondary,
                                    )
                                }
                            }
                        }
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

/**
 * 9:16 Optimized Share Card for Songs
 */
