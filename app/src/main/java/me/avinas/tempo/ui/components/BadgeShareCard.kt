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
fun BadgeShareCard(
    badge: Badge,
    userName: String,
    profileImagePath: String? = null,
    theme: ShareTheme = ShareTheme.MIDNIGHT,
    modifier: Modifier = Modifier,
) {
    val palette = theme.palette
    val intrinsicColor = getUniqueBadgeColor(badge.badgeId)
    val rarity = GamificationEngine.getRarity(badge.badgeId)
    val rarityColor = getRarityColor(rarity)
    val categoryLabel =
        remember(badge.category) {
            getCategoryLabel(badge.category).uppercase(Locale.getDefault())
        }
    val xpEarned = GamificationEngine.getBadgeXpContribution(badge.badgeId, badge.stars)
    val earnedDate =
        remember(badge.earnedAt) {
            if (badge.isEarned && badge.earnedAt > 0L) {
                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(badge.earnedAt))
            } else {
                "Recent"
            }
        }

    val backdropBitmap =
        remember(badge.badgeId) {
            createBadgeBackdropBitmap(badge = badge)
        }

    ShareCardBackground(
        imageUrl = null,
        backdropBitmap = backdropBitmap,
        customBackdrop = {
            BadgeCustomCanvas(badge = badge)
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Top Identity Pill: User profile stamp
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
                                .background(intrinsicColor.copy(alpha = 0.25f)),
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
                        text = if (badge.isMaxed) "TROPHY MASTERED" else "TROPHY UNLOCKED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (palette.isDark) intrinsicColor else palette.textStrong,
                        letterSpacing = 1.sp,
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Hero Stage: Grand Silhouette Emblem with Layered Blooming Halo
                Box(contentAlignment = Alignment.Center) {
                    // Outer diffuse glow
                    Box(
                        modifier =
                            Modifier
                                .size(176.dp)
                                .background(
                                    brush =
                                        Brush.radialGradient(
                                            listOf(
                                                intrinsicColor.copy(alpha = if (palette.isDark) 0.42f else 0.20f),
                                                Color.Transparent,
                                            ),
                                        ),
                                    shape = CircleShape,
                                ),
                    )

                    // Concentric accent ring
                    Box(
                        modifier =
                            Modifier
                                .size(152.dp)
                                .border(
                                    width = 1.dp,
                                    color = rarityColor.copy(alpha = if (palette.isDark) 0.35f else 0.18f),
                                    shape = CircleShape,
                                ),
                    )

                    BadgeEmblem(
                        badge = badge,
                        intrinsicColor = intrinsicColor,
                        modifier = Modifier.size(136.dp),
                    )
                }

                // Category Kicker, Title & Description
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "✦ $categoryLabel DISCIPLINE",
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                            ),
                        color = if (palette.isDark) intrinsicColor else palette.textStrong,
                    )
                    Text(
                        text = badge.name,
                        style =
                            MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = DisplayFontFamily,
                                fontWeight = palette.headlineWeight ?: FontWeight.Black,
                                fontSize = 26.sp,
                                letterSpacing = (-0.5).sp,
                            ),
                        color = palette.textPrimary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 30.sp,
                        maxLines = 2,
                    )
                    Text(
                        text = badge.description,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = palette.textSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp,
                        maxLines = 2,
                    )
                }

                // Prestige Rarity Badge & 5-Star Constellation
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(palette.surfaceStrong)
                            .border(1.dp, rarityColor.copy(alpha = if (palette.isDark) 0.40f else 0.20f), RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(rarityColor.copy(alpha = if (palette.isDark) 0.22f else 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = rarity.label.uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Black,
                            color = if (palette.isDark) rarityColor else palette.textStrong,
                            letterSpacing = 1.sp,
                        )
                    }
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (i in 1..5) {
                            val active = i <= badge.stars
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint =
                                    if (active) {
                                        (if (badge.isMaxed) intrinsicColor else Color(0xFFFBBF24))
                                    } else {
                                        palette.textSecondary.copy(alpha = 0.22f)
                                    },
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }

                // Micro Progress Bar if in-progress
                if (!badge.isMaxed && badge.maxProgress > 1) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "TIER PROGRESS",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Bold,
                                color = palette.textSecondary,
                                letterSpacing = 1.2.sp,
                            )
                            Text(
                                text = "${badge.progress} / ${badge.maxProgress}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary,
                            )
                        }
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(palette.divider),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(badge.progressFraction)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            brush =
                                                Brush.horizontalGradient(
                                                    listOf(intrinsicColor, rarityColor),
                                                ),
                                        ),
                            )
                        }
                    }
                }

                // Structured Telemetry Dossier Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = palette.surface,
                    shape = palette.cardShape,
                    borderColor = palette.divider,
                    borderWidth = 1.dp,
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = "RANK TIER",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = palette.textSecondary,
                                letterSpacing = palette.labelTracking ?: 1.2.sp,
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (badge.isMaxed) "Master" else "Tier ${badge.stars}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = palette.headlineWeight ?: FontWeight.Bold,
                                color = if (badge.isMaxed && palette.isDark) intrinsicColor else palette.textPrimary,
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = if (badge.isMaxed) "5 of 5 Stars" else "${badge.stars} of 5 Stars",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = palette.textSecondary,
                            )
                        }

                        Box(
                            modifier =
                                Modifier
                                    .height(32.dp)
                                    .width(1.dp)
                                    .background(palette.divider),
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = "XP VALUE",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = palette.textSecondary,
                                letterSpacing = palette.labelTracking ?: 1.2.sp,
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (xpEarned > 0) "+$xpEarned XP" else "+${rarity.xpPerStar * badge.stars.coerceAtLeast(1)} XP",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = palette.headlineWeight ?: FontWeight.Bold,
                                color = if (palette.isDark) Color(0xFFFBBF24) else palette.textPrimary,
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "${rarity.label} Tier",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = palette.textSecondary,
                            )
                        }

                        Box(
                            modifier =
                                Modifier
                                    .height(32.dp)
                                    .width(1.dp)
                                    .background(palette.divider),
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = "ACHIEVED",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = palette.textSecondary,
                                letterSpacing = palette.labelTracking ?: 1.2.sp,
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = earnedDate,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = palette.headlineWeight ?: FontWeight.Bold,
                                color = palette.textPrimary,
                                maxLines = 1,
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "Archive Record",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = palette.textSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 9:16 Optimized Share Card for Level Up Milestones
 */
