package me.avinas.tempo.ui.components
import me.avinas.tempo.ui.util.observableLocale

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
fun MilestoneShareCard(
    level: Int,
    title: String,
    userName: String,
    profileImagePath: String? = null,
    totalXp: Long = 0,
    streak: Int = 0,
    theme: ShareTheme = ShareTheme.MIDNIGHT,
    modifier: Modifier = Modifier,
) {
    val palette = theme.palette
    val tierAccent = getLevelTierAccent(level)

    val backdropBitmap =
        remember(level) {
            createMilestoneBackdropBitmap(level = level)
        }

    ShareCardBackground(
        imageUrl = null,
        backdropBitmap = backdropBitmap,
        customBackdrop = {
            MilestoneCustomCanvas(level = level)
        },
        palette = palette,
        modifier = modifier.aspectRatio(9f / 16f),
    ) {
        FitToHeight(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 36.dp, bottom = 68.dp, start = 24.dp, end = 24.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Top Identity Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(palette.surface)
                            .border(1.dp, palette.divider, RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(tierAccent.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!profileImagePath.isNullOrBlank()) {
                            CachedAsyncImage(
                                imageUrl = profileImagePath,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                allowHardware = false,
                            )
                        } else {
                            Text(
                                text = userName.firstOrNull()?.toString()?.uppercase() ?: "U",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary,
                            )
                        }
                    }
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                    )
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                    )
                    Text(
                        text = "LEVEL PROMOTION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (palette.isDark) tierAccent else palette.textStrong,
                        letterSpacing = 1.2.sp,
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Hero Level Crest
                Box(contentAlignment = Alignment.Center) {
                    // Outer radial aura
                    Box(
                        modifier =
                            Modifier
                                .size(180.dp)
                                .background(
                                    brush =
                                        Brush.radialGradient(
                                            listOf(
                                                tierAccent.copy(alpha = if (palette.isDark) 0.38f else 0.20f),
                                                Color.Transparent,
                                            ),
                                        ),
                                    shape = CircleShape,
                                ),
                    )

                    // Concentric Ring
                    Box(
                        modifier =
                            Modifier
                                .size(136.dp)
                                .clip(CircleShape)
                                .background(palette.surfaceStrong)
                                .border(3.dp, tierAccent.copy(alpha = 0.85f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "LEVEL",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = tierAccent,
                                letterSpacing = 2.sp,
                            )
                            Text(
                                text = "$level",
                                style =
                                    MaterialTheme.typography.displayLarge.copy(
                                        fontFamily = DisplayFontFamily,
                                        fontSize = 52.sp,
                                        letterSpacing = (-1.5).sp,
                                    ),
                                fontWeight = FontWeight.Black,
                                color = palette.textPrimary,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Listener Title & Description
                Text(
                    text = title.ifBlank { "Dedicated Listener" },
                    style =
                        MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = DisplayFontFamily,
                            fontWeight = palette.headlineWeight ?: FontWeight.Black,
                        ),
                    color = palette.textPrimary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 32.sp,
                )

                Text(
                    text = "A milestone reached on the listening rhythm.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )

                // Stats Grid
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = palette.surface,
                    shape = palette.cardShape,
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "TOTAL XP",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary,
                                letterSpacing = palette.labelTracking ?: 1.5.sp,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(observableLocale(), "%,d", totalXp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = palette.headlineWeight ?: FontWeight.Bold,
                                color = palette.textPrimary,
                            )
                        }

                        if (!LocalHideStreaks.current) {
                            Box(
                                modifier =
                                    Modifier
                                        .height(30.dp)
                                        .width(1.dp)
                                        .background(palette.divider),
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "STREAK",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary,
                                    letterSpacing = palette.labelTracking ?: 1.5.sp,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (streak > 0) "$streak days" else "Active",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = palette.headlineWeight ?: FontWeight.Bold,
                                    color = palette.textPrimary,
                                )
                            }
                        }

                        Box(
                            modifier =
                                Modifier
                                    .height(30.dp)
                                    .width(1.dp)
                                    .background(palette.divider),
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "RANK TIER",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary,
                                letterSpacing = palette.labelTracking ?: 1.5.sp,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text =
                                    when {
                                        level >= 100 -> "Mythic"
                                        level >= 75 -> "Rose"
                                        level >= 50 -> "Gold"
                                        level >= 35 -> "Purple"
                                        level >= 20 -> "Electric"
                                        level >= 10 -> "Cyan"
                                        level >= 5 -> "Emerald"
                                        else -> "Studio"
                                    },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = palette.headlineWeight ?: FontWeight.Bold,
                                color = if (palette.isDark) tierAccent else palette.textPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}
