package me.avinas.tempo.ui.profile

import me.avinas.tempo.ui.effects.tempoAmbientBackdrop
import me.avinas.tempo.ui.effects.tempoTiltSurface
import me.avinas.tempo.ui.effects.tempoListMotion
import androidx.compose.runtime.saveable.rememberSaveable
import me.avinas.tempo.ui.appearance.AppearanceProfileTab
import me.avinas.tempo.ui.theme.LocalTempoAppearance
import me.avinas.tempo.ui.components.ShareSnapshot
import me.avinas.tempo.ui.components.SnapshotShareDialog
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import me.avinas.tempo.data.repository.observeClockChanges
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import me.avinas.tempo.R
import me.avinas.tempo.data.local.entities.Badge
import me.avinas.tempo.data.local.entities.DailyChallenge
import me.avinas.tempo.data.local.entities.UserLevel
import me.avinas.tempo.data.stats.GamificationEngine
import me.avinas.tempo.ui.components.ArtAtmosphereLayer
import me.avinas.tempo.ui.components.BadgeShareCard
import me.avinas.tempo.ui.components.CachedAsyncImage
import me.avinas.tempo.ui.components.CelebrationParticlesCanvas
import me.avinas.tempo.ui.components.LiveNowPlayingCard
import me.avinas.tempo.ui.components.TempoEmblem
import me.avinas.tempo.ui.components.TempoEmblemStyle
import me.avinas.tempo.ui.components.FrostedIconButton
import me.avinas.tempo.ui.components.GlassCard
import me.avinas.tempo.ui.components.GlassCardVariant
import me.avinas.tempo.ui.components.MilestoneShareCard
import me.avinas.tempo.ui.components.SharePreviewDialog
import me.avinas.tempo.ui.theme.*
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal fun getCategoryColor(category: String): Color =
    when (category) {
        "MILESTONE" -> TempoWarning
        "TIME" -> TempoInfo
        "STREAK" -> TempoError
        "DISCOVERY" -> TempoSuccessDeep
        "ENGAGEMENT" -> InsightDanceability
        "LEVEL" -> InsightBinge
        else -> Color.Gray
    }

internal fun getCategoryLabel(category: String): String =
    when (category) {
        "MILESTONE" -> "Milestones"
        "TIME" -> "Time"
        "STREAK" -> "Streaks"
        "DISCOVERY" -> "Discovery"
        "ENGAGEMENT" -> "Engagement"
        "LEVEL" -> "Levels"
        else -> category
    }


// ──────────────────────────────────────────────────────────────
// Trophy Room (Badges) Section
// ──────────────────────────────────────────────────────────────

internal fun LazyListScope.badgeSection(
    filteredBadges: List<Badge>,
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    onBadgeClick: (Badge) -> Unit,
    contentModifier: Modifier,
    badgeColumns: Int = 2,
) {
    item(key = "badges_header") {
        val appearancePalette = LocalTempoPalette.current
        val earnedCount = remember(filteredBadges) { filteredBadges.count { it.isEarned } }
        val totalCount = filteredBadges.size
        val totalStars = remember(filteredBadges) { ProfileUiState.computeTotalStars(filteredBadges) }
        val maxPossibleStars = remember(filteredBadges) { ProfileUiState.computeMaxPossibleStars(filteredBadges) }
        val spotlightBadge = remember(filteredBadges) { selectFeaturedBadge(filteredBadges) }
        val collectionProgress = if (totalCount == 0) 0f else earnedCount.toFloat() / totalCount
        val count = "$earnedCount of $totalCount badges earned."
        val subtitle = selectedCategory?.let { "${getCategoryLabel(it)} · $count" } ?: count

        Column(modifier = contentModifier) {
            Spacer(modifier = Modifier.height(24.dp))
            EditorialSectionHeader(
                sectionNumber = "02",
                title = "Trophy room",
                subtitle = subtitle,
                trailing = if (totalStars > 0) ({ StarsChip(total = totalStars, max = maxPossibleStars) }) else null,
            )
            Spacer(modifier = Modifier.height(14.dp))
            ProgressTrack(progress = collectionProgress, modifier = Modifier.fillMaxWidth(), color = TempoWarning)
            Spacer(modifier = Modifier.height(16.dp))

            // Category Filter Chips
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategorySelected(null) },
                    label = { Text("All") },
                    colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = appearancePalette.accent.copy(alpha = 0.14f),
                            containerColor = Color.Transparent,
                            labelColor = appearancePalette.mutedText,
                            selectedLabelColor = appearancePalette.onSurface,
                        ),
                    border =
                        FilterChipDefaults.filterChipBorder(
                            borderColor = appearancePalette.border.copy(alpha = 0.65f),
                            selectedBorderColor = appearancePalette.accent.copy(alpha = 0.40f),
                            enabled = true,
                            selected = selectedCategory == null,
                        ),
                )
                categories.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { onCategorySelected(category) },
                        label = { Text(getCategoryLabel(category)) },
                        colors =
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = appearancePalette.accent.copy(alpha = 0.14f),
                                containerColor = Color.Transparent,
                                labelColor = appearancePalette.mutedText,
                                selectedLabelColor = appearancePalette.onSurface,
                            ),
                        border =
                            FilterChipDefaults.filterChipBorder(
                                borderColor = appearancePalette.border.copy(alpha = 0.65f),
                                selectedBorderColor = appearancePalette.accent.copy(alpha = 0.40f),
                                enabled = true,
                                selected = selectedCategory == category,
                            ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Featured Spotlight Achievement Card (inspired by SongDetails affinity spotlight)
            if (spotlightBadge != null) {
                SpotlightBadgeCard(
                    badge = spotlightBadge,
                    isAlmostThere = !spotlightBadge.isEarned,
                    onClick = { onBadgeClick(spotlightBadge) },
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    if (filteredBadges.isEmpty()) {
        item(key = "badges_empty") {
            Column(
                modifier = contentModifier.padding(top = 20.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TempoEmblem(
                    icon = Icons.Rounded.EmojiEvents,
                    size = 84.dp,
                    style = TempoEmblemStyle.DISCOVERY,
                )
                Text(
                    text = stringResource(R.string.profile_no_badges),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
    itemsIndexed(
        filteredBadges.chunked(badgeColumns),
        key = { _, rowBadges -> rowBadges.joinToString("-") { it.badgeId } },
    ) { index, rowBadges ->
        Column(modifier = contentModifier.tempoListMotion(index)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowBadges.forEach { badge ->
                    BadgeCard(badge = badge, onClick = { onBadgeClick(badge) }, modifier = Modifier.weight(1f))
                }
                repeat(badgeColumns - rowBadges.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SpotlightBadgeCard(
    badge: Badge,
    isAlmostThere: Boolean,
    onClick: () -> Unit,
) {
    val appearancePalette = LocalTempoPalette.current
    val accent = if (isAlmostThere) appearancePalette.accent else TempoWarning
    val remaining = (badge.maxProgress - badge.progress).coerceAtLeast(0)
    val remainingLabel = "$remaining"
    val progressLabel = "${badge.progress} / ${badge.maxProgress}"
    val animatedProgress by animateFloatAsState(
        targetValue = badge.progressFraction,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "spotlightProgress",
    )

    val haptic = LocalHapticFeedback.current
    GlassCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .premiumClickable(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    },
                    pressedScale = 0.96f,
                ),
        shape = RoundedCornerShape(20.dp),
        variant = GlassCardVariant.Obsidian,
        borderColor = accent.copy(alpha = 0.42f),
        borderWidth = 0.8.dp,
        contentPadding = PaddingValues(18.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(accent),
                    )
                    Text(
                        text = if (isAlmostThere) "FEATURED · ALMOST UNLOCKED" else "FEATURED · NEXT TIER",
                        style = KickerSmall,
                        fontWeight = FontWeight.Bold,
                        color = profileTextColor(accent),
                        letterSpacing = 1.2.sp,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                    )
                }
                Text(
                    text = progressLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = appearancePalette.mutedText,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BadgeEmblem(
                    badge = badge,
                    intrinsicColor = getUniqueBadgeColor(badge.badgeId),
                    modifier = Modifier.size(56.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = badge.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = appearancePalette.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = badge.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = appearancePalette.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text =
                            if (isAlmostThere) {
                                "$remainingLabel to go — keep the rhythm going."
                            } else {
                                "Tier achieved — push for the next star."
                            },
                        style = CaptionSmall,
                        color = appearancePalette.mutedText,
                    )
                }
            }

            ProgressTrack(
                progress = animatedProgress,
                modifier = Modifier.fillMaxWidth(),
                color = accent,
                height = 5.dp,
            )
        }
    }
}

@Composable
private fun BadgeCard(
    badge: Badge,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appearancePalette = LocalTempoPalette.current
    val isEarned = badge.isEarned
    val intrinsicColor = getUniqueBadgeColor(badge.badgeId)
    val rarity = GamificationEngine.getRarity(badge.badgeId)
    val rarityColor = getRarityColor(rarity)
    val isBeginner = badge.badgeId in GamificationEngine.BEGINNER_BADGES
    val animatedProgress by animateFloatAsState(
        targetValue = badge.progressFraction,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "badgeProgress",
    )
    val earnedDate =
        remember(badge.earnedAt) {
            if (badge.isEarned && badge.earnedAt > 0L) {
                SimpleDateFormat("MMM yyyy", Locale.getDefault())
                    .format(Date(badge.earnedAt))
            } else {
                null
            }
        }

    val haptic = LocalHapticFeedback.current
    GlassCard(
        modifier =
            modifier.premiumClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                pressedScale = 0.95f,
            ),
        shape = RoundedCornerShape(18.dp),
        variant = if (isEarned) GlassCardVariant.TintedSolid else GlassCardVariant.Obsidian,
        accentColor = if (isEarned) intrinsicColor else null,
        borderColor = if (isEarned) intrinsicColor.copy(alpha = 0.32f) else appearancePalette.border.copy(alpha = 0.45f),
        borderWidth = 0.8.dp,
        contentPadding = PaddingValues(14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BadgeEmblem(badge = badge, intrinsicColor = intrinsicColor, modifier = Modifier.size(64.dp))
            Text(
                text = badge.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isEarned) appearancePalette.onSurface else appearancePalette.mutedText,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.heightIn(min = 36.dp),
            )
            Text(
                text = badge.description,
                style = MaterialTheme.typography.bodySmall,
                color = appearancePalette.mutedText,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.heightIn(min = 32.dp),
            )
            Text(
                text = rarity.label.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = profileTextColor(if (isEarned) rarityColor else appearancePalette.mutedText),
                letterSpacing = 1.sp,
                fontSize = MaterialTheme.typography.labelSmall.fontSize,
            )
            if (isEarned) {
                if (isBeginner) {
                    Text(
                        text = "UNLOCKED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = profileTextColor(intrinsicColor),
                        letterSpacing = 1.sp,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (i in 1..5) {
                            val starColor =
                                if (i <= badge.stars) {
                                    if (badge.isMaxed) intrinsicColor else TempoWarningBright
                                } else {
                                    appearancePalette.border
                                }
                            Icon(
                                imageVector = if (i <= badge.stars) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                                contentDescription = null,
                                tint = profileIndicatorColor(starColor),
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                    if (badge.isMaxed) {
                        Text(
                            text = "MAXED",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = profileTextColor(TempoWarningBright),
                            letterSpacing = 1.sp,
                            fontSize = MaterialTheme.typography.labelSmall.fontSize,
                        )
                    } else {
                        ProgressTrack(
                            progress = animatedProgress,
                            modifier = Modifier.fillMaxWidth(),
                            color = intrinsicColor,
                            height = 4.dp,
                        )
                        Text(
                            text = "${badge.progress} / ${badge.maxProgress}  →  ★${badge.stars + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = appearancePalette.mutedText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 9.sp,
                        )
                    }
                }
                if (earnedDate != null) {
                    Text(
                        text = "EARNED · $earnedDate",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp,
                        letterSpacing = 1.5.sp,
                        color = profileTextColor(intrinsicColor.copy(alpha = 0.85f)),
                    )
                }
            } else {
                ProgressTrack(
                    progress = animatedProgress,
                    modifier = Modifier.fillMaxWidth(),
                    color = intrinsicColor.copy(alpha = 0.55f),
                    height = 4.dp,
                )
                Text(
                    text = "${badge.progress} / ${badge.maxProgress}",
                    style = MaterialTheme.typography.labelSmall,
                    color = appearancePalette.mutedText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                )
            }
        }
    }
}

// Custom silhouette badge emblem with rarity metallic rim and enamel gradient face.
@Composable
internal fun BadgeEmblem(
    badge: Badge,
    intrinsicColor: Color,
    modifier: Modifier = Modifier,
) {
    val isEarned = badge.isEarned
    val rarity = remember(badge.badgeId) { GamificationEngine.getRarity(badge.badgeId) }
    val rarityColor = remember(rarity) { getRarityColor(rarity) }
    val metal = remember(rarity) { getRarityMetal(rarity) }
    val glowAlpha = remember(isEarned, rarity) { if (isEarned) getRarityGlowAlpha(rarity) else 0f }
    val art = remember(badge.badgeId) { BadgeArt.artFor(badge.badgeId) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f

            val outerPath = BadgeSilhouettes.getPath(badge.badgeId, size, insetScale = 0.96f)
            val facePath = BadgeSilhouettes.getPath(badge.badgeId, size, insetScale = 0.81f)
            val hairlinePath = BadgeSilhouettes.getPath(badge.badgeId, size, insetScale = 0.74f)

            // Soft glow for EPIC+ rarity badges
            if (glowAlpha > 0f) {
                drawPath(
                    path = outerPath,
                    brush =
                        Brush.radialGradient(
                            colors = listOf(rarityColor.copy(alpha = glowAlpha), Color.Transparent),
                            center = center,
                            radius = radius * 1.15f,
                        ),
                )
            }

            // Metallic rim
            val rimBrush =
                if (rarity == GamificationEngine.BadgeRarity.MYTHIC && isEarned) {
                    Brush.sweepGradient(metal, center)
                } else {
                    Brush.linearGradient(
                        colors = metal,
                        start = Offset(size.width * 0.15f, 0f),
                        end = Offset(size.width * 0.85f, size.height),
                    )
                }
            drawPath(path = outerPath, brush = rimBrush)

            // Badge face gradient fill
            val faceBrush =
                if (isEarned) {
                    Brush.radialGradient(
                        colors =
                            listOf(
                                lerp(intrinsicColor, Color.White, 0.38f),
                                intrinsicColor,
                                lerp(intrinsicColor, Color.Black, 0.42f),
                            ),
                        center = Offset(center.x - radius * 0.3f, center.y - radius * 0.38f),
                        radius = radius * 1.35f,
                    )
                } else {
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF2B2B31), Color(0xFF131317)),
                        center = Offset(center.x - radius * 0.25f, center.y - radius * 0.3f),
                        radius = radius * 1.2f,
                    )
                }
            drawPath(path = facePath, brush = faceBrush)

            // Inner hairline border
            drawPath(
                path = hairlinePath,
                color = Color.White.copy(alpha = if (isEarned) 0.25f else 0.08f),
                style = Stroke(width = 1.dp.toPx()),
            )

            // Static diagonal sheen highlight
            if (isEarned) {
                clipPath(facePath) {
                    rotate(degrees = -24f, pivot = center) {
                        drawRect(
                            brush =
                                Brush.horizontalGradient(
                                    colors =
                                        listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.16f),
                                            Color.Transparent,
                                        ),
                                ),
                            topLeft = Offset(center.x - radius * 0.8f, center.y - radius * 1.6f),
                            size = Size(radius * 0.65f, radius * 3.2f),
                        )
                    }
                }
            }
        }

        Icon(
            imageVector = art,
            contentDescription = badge.name,
            tint = if (isEarned) Color.White.copy(alpha = 0.96f) else Color.White.copy(alpha = 0.22f),
            modifier = Modifier.fillMaxSize(0.40f),
        )
    }
}

// Maps badge IDs to their base theme colors
internal fun getUniqueBadgeColor(badgeId: String): Color =
    when (badgeId) {
        "first_play" -> Color(0xFF10B981)
        "plays_100" -> Color(0xFF3B82F6)
        "plays_500" -> Color(0xFF8B5CF6)
        "plays_1000" -> Color(0xFFEC4899)
        "plays_5000" -> Color(0xFFF43F5E)
        "plays_10000" -> Color(0xFFEAB308)
        "time_1h" -> Color(0xFF06B6D4)
        "time_24h" -> Color(0xFF0EA5E9)
        "time_100h" -> Color(0xFF6366F1)
        "time_500h" -> Color(0xFFD946EF)
        "streak_7" -> Color(0xFFF97316)
        "streak_30" -> Color(0xFFEF4444)
        "streak_100" -> Color(0xFFDC2626)
        "streak_365" -> Color(0xFF991B1B)
        "artists_10" -> Color(0xFF14B8A6)
        "artists_50" -> Color(0xFF22C55E)
        "artists_100" -> Color(0xFF84CC16)
        "genres_10" -> Color(0xFFF59E0B)
        "genres_25" -> Color(0xFFD97706)
        "night_owl" -> Color(0xFF312E81)
        "early_bird" -> Color(0xFFFBBF24)
        "marathon" -> Color(0xFF4F46E5)
        "level_5" -> Color(0xFF6EE7B7)
        "level_10" -> Color(0xFF34D399)
        "level_25" -> Color(0xFF10B981)
        "level_50" -> Color(0xFF059669)
        "level_75" -> Color(0xFF047857)
        "level_100" -> Color(0xFF064E3B)
        else -> Color(0xFFA855F7)
    }

internal fun getRarityColor(rarity: GamificationEngine.BadgeRarity): Color =
    when (rarity) {
        GamificationEngine.BadgeRarity.COMMON -> Color(0xFF9CA3AF)
        GamificationEngine.BadgeRarity.RARE -> Color(0xFF3B82F6)
        GamificationEngine.BadgeRarity.EPIC -> Color(0xFFA855F7)
        GamificationEngine.BadgeRarity.LEGENDARY -> Color(0xFFF59E0B)
        GamificationEngine.BadgeRarity.MYTHIC -> Color(0xFFEC4899)
    }

/** The metal gradient of the coin rim, light struck from the upper left. */
internal fun getRarityMetal(rarity: GamificationEngine.BadgeRarity): List<Color> =
    when (rarity) {
        GamificationEngine.BadgeRarity.COMMON -> {
            listOf(Color(0xFFDCDFE4), Color(0xFF9CA3AB), Color(0xFF5F646B), Color(0xFFB9BDC3))
        }

        GamificationEngine.BadgeRarity.RARE -> {
            listOf(Color(0xFFF4F8FF), Color(0xFFC3D5EE), Color(0xFF8199BE), Color(0xFFE1EBF8))
        }

        GamificationEngine.BadgeRarity.EPIC -> {
            listOf(Color(0xFFFFF6D9), Color(0xFFF4CF6D), Color(0xFFBA8C20), Color(0xFFF1DE9E))
        }

        GamificationEngine.BadgeRarity.LEGENDARY -> {
            listOf(Color(0xFFFFEFEE), Color(0xFFF8C3CC), Color(0xFFC57486), Color(0xFFFFDCE1))
        }

        GamificationEngine.BadgeRarity.MYTHIC -> {
            listOf(Color(0xFFE4D4FF), Color(0xFFAEE9F7), Color(0xFFFBD3E9), Color(0xFFD8F5E3), Color(0xFFFDE9C8))
        }
    }

internal fun getRarityGlowAlpha(rarity: GamificationEngine.BadgeRarity): Float =
    when (rarity) {
        GamificationEngine.BadgeRarity.COMMON -> 0f
        GamificationEngine.BadgeRarity.RARE -> 0f
        GamificationEngine.BadgeRarity.EPIC -> 0.20f
        GamificationEngine.BadgeRarity.LEGENDARY -> 0.30f
        GamificationEngine.BadgeRarity.MYTHIC -> 0.42f
    }

