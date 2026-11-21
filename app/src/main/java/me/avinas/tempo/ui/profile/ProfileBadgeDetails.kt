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

// Delegated celebration composables for backwards compatibility
@Composable
fun LevelUpCelebration(
    level: Int,
    onDismiss: () -> Unit,
) {
    LevelUpCelebrationOverlay(
        level = level,
        onDismiss = onDismiss,
    )
}

@Composable
fun NewBadgeCelebrationOverlay(
    badges: List<Badge>,
    onDismiss: () -> Unit,
) {
    BadgeCelebrationOverlay(
        badges = badges,
        onDismiss = onDismiss,
    )
}

@Composable
fun ConfettiEffect() {
    CelebrationParticlesCanvas()
}

// Detail overlay for a selected badge, showing star tier progress and criteria.
@Composable
fun BadgeDetailsOverlay(
    badge: Badge,
    onDismiss: () -> Unit,
    onShareBadge: ((Badge) -> Unit)? = null,
) {
    val appearancePalette = LocalTempoPalette.current
    val intrinsicColor = getUniqueBadgeColor(badge.badgeId)
    val rarity = GamificationEngine.getRarity(badge.badgeId)
    val rarityColor = getRarityColor(rarity)
    val isBeginner = badge.badgeId in GamificationEngine.BEGINNER_BADGES
    val def =
        remember(badge.badgeId) {
            GamificationEngine.ALL_BADGE_DEFINITIONS.firstOrNull { it.badgeId == badge.badgeId }
        }
    val animatedProgress by animateFloatAsState(
        targetValue = badge.progressFraction,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "badgeDetailProgress",
    )
    val earnedDate =
        remember(badge.earnedAt) {
            if (badge.isEarned && badge.earnedAt > 0L) {
                SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    .format(Date(badge.earnedAt))
            } else {
                null
            }
        }
    val xpEarned = GamificationEngine.getBadgeXpContribution(badge.badgeId, badge.stars)

    val reducedMotion = rememberReducedMotion()
    val fastSpatialSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val defaultSpatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val fastEffectsSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current.density
    var isClosing by remember { mutableStateOf(false) }

    // Entrance animation state
    val emblemScale = remember { Animatable(if (reducedMotion) 0.85f else 0.35f) }
    val emblemAlpha = remember { Animatable(0f) }
    val glowScale = remember { Animatable(0.20f) }
    val contentAlpha = remember { Animatable(0f) }
    val contentOffsetY = remember { Animatable(if (reducedMotion) 0f else 48f) }

    // MotionScheme selects expressive springs normally and instant snap specs when
    // Android / Tempo disables animation. No fixed delays block dismissal.
    fun dismissSmoothly() {
        if (isClosing) return
        isClosing = true
        if (!reducedMotion) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        coroutineScope.launch {
            val jobs = listOf(
                launch { contentAlpha.animateTo(0f, fastEffectsSpec) },
                launch { contentOffsetY.animateTo(if (reducedMotion) 0f else 28f, fastSpatialSpec) },
                launch { emblemScale.animateTo(if (reducedMotion) 1f else 0.38f, fastSpatialSpec) },
                launch { emblemAlpha.animateTo(0f, fastEffectsSpec) },
                launch { glowScale.animateTo(0.20f, fastSpatialSpec) },
            )
            jobs.forEach { it.join() }
            onDismiss()
        }
    }

    LaunchedEffect(badge.badgeId, reducedMotion) {
        if (!reducedMotion) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        launch { emblemAlpha.animateTo(1f, fastEffectsSpec) }
        launch { glowScale.animateTo(1.18f, defaultSpatialSpec) }
        launch { emblemScale.animateTo(1f, defaultSpatialSpec) }
        launch { contentAlpha.animateTo(1f, fastEffectsSpec) }
        launch { contentOffsetY.animateTo(0f, defaultSpatialSpec) }
    }

    ProfileModal(
        title = badge.name,
        onDismiss = { dismissSmoothly() },
        modifier =
            Modifier
                .fillMaxSize()
                .background(appearancePalette.surface),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
        ) {
            // Badge emblem
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                // Ambient glow behind emblem
                Box(
                    modifier =
                        Modifier
                            .size(176.dp)
                            .graphicsLayer {
                                scaleX = glowScale.value
                                scaleY = glowScale.value
                                alpha = (0.50f * emblemAlpha.value).coerceIn(0f, 0.50f)
                            }.background(
                                brush =
                                    Brush.radialGradient(
                                        colors =
                                            listOf(
                                                intrinsicColor.copy(alpha = 0.45f),
                                                intrinsicColor.copy(alpha = 0.0f),
                                            ),
                                    ),
                                shape = CircleShape,
                            ),
                )

                // Scaled badge emblem
                Box(
                    modifier =
                        Modifier.graphicsLayer {
                            scaleX = emblemScale.value
                            scaleY = emblemScale.value
                            alpha = emblemAlpha.value
                            cameraDistance = 12f * density
                        },
                ) {
                    BadgeEmblem(
                        badge = badge,
                        intrinsicColor = intrinsicColor,
                        modifier = Modifier.size(124.dp),
                    )
                }
            }

            // Badge details and criteria
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = contentAlpha.value
                            translationY = contentOffsetY.value * density
                        },
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = badge.name,
                        style =
                            MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = DisplayFontFamily,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp,
                            ),
                        color = appearancePalette.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = badge.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = appearancePalette.mutedText,
                        textAlign = TextAlign.Center,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DetailsChip(text = rarity.label.uppercase(Locale.getDefault()), color = rarityColor)
                        DetailsChip(
                            text = getCategoryLabel(badge.category).uppercase(Locale.getDefault()),
                            color = getCategoryColor(badge.category),
                        )
                        if (badge.isMaxed) DetailsChip(text = "MAXED", color = TempoWarningBright)
                    }
                }

                GlassCard(
                    variant = GlassCardVariant.Obsidian,
                    shape = RoundedCornerShape(22.dp),
                    accentColor = intrinsicColor,
                    borderColor = intrinsicColor.copy(alpha = 0.32f),
                    borderWidth = 0.8.dp,
                    contentPadding = PaddingValues(20.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = "STAR TIERS",
                            style = KickerSmall,
                            fontWeight = FontWeight.Bold,
                            color = appearancePalette.mutedText,
                            letterSpacing = 1.4.sp,
                        )

                        if (isBeginner) {
                            if (badge.isEarned) {
                                Text(
                                    text = "UNLOCKED",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = profileTextColor(intrinsicColor),
                                    letterSpacing = 1.sp,
                                )
                                Text(
                                    text = "A participation badge — yours from the first moment.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = appearancePalette.mutedText,
                                )
                            } else {
                                Text(
                                    text = "${badge.progress} / ${badge.maxProgress}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = appearancePalette.onSurface,
                                )
                                ProgressTrack(
                                    progress = animatedProgress,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = intrinsicColor,
                                )
                            }
                        } else if (def != null) {
                            val unit = getBadgeUnit(badge.badgeId)
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                for (tier in 1..GamificationEngine.MAX_STARS) {
                                    val threshold = GamificationEngine.getStarThreshold(def, tier)
                                    val achieved = badge.stars >= tier
                                    val isNext = !achieved && badge.stars + 1 == tier
                                    val tierColor =
                                        when {
                                            achieved -> intrinsicColor
                                            isNext -> TempoWarningBright
                                            else -> appearancePalette.border
                                        }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = if (achieved) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                                            contentDescription = null,
                                            tint = profileIndicatorColor(tierColor, appearancePalette.surfaceSunken),
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Star $tier",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (achieved || isNext) appearancePalette.onSurface else appearancePalette.mutedText,
                                            )
                                            if (isNext) {
                                                val remaining = (threshold - badge.progress).coerceAtLeast(0)
                                                Text(
                                                    text = "%,d %s to go".format(remaining, unit),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = profileTextColor(TempoWarningBright),
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }
                                        }
                                        Text(
                                            text = "%,d".format(threshold),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (achieved) profileTextColor(intrinsicColor) else appearancePalette.mutedText,
                                        )
                                    }
                                    if (isNext && !badge.isMaxed) {
                                        ProgressTrack(
                                            progress = animatedProgress,
                                            modifier = Modifier.fillMaxWidth(),
                                            color = intrinsicColor,
                                            height = 4.dp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (earnedDate != null) DetailsChip(text = "EARNED · $earnedDate", color = intrinsicColor)
                    if (xpEarned > 0) DetailsChip(text = "%,d XP".format(xpEarned), color = TempoWarning)
                }
            }
        }

        // Frosted tactile action bar at bottom: Share + Close
        Row(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
                    .graphicsLayer { alpha = contentAlpha.value },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (badge.isEarned && onShareBadge != null) {
                Row(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(appearancePalette.elevatedSurface)
                            .border(0.8.dp, profileIndicatorColor(intrinsicColor), RoundedCornerShape(50))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onShareBadge(badge)
                            }.heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = profileIndicatorColor(intrinsicColor, appearancePalette.elevatedSurface),
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "Share Trophy",
                        style = CaptionSmall,
                        color = appearancePalette.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Row(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(appearancePalette.elevatedSurface)
                        .border(0.8.dp, profileIndicatorColor(appearancePalette.border), RoundedCornerShape(50))
                        .clickable { dismissSmoothly() }
                        .heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = appearancePalette.mutedText,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = stringResource(R.string.share_close),
                    style = CaptionSmall,
                    color = appearancePalette.mutedText,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun DetailsChip(
    text: String,
    color: Color,
) {
    val surface = color.copy(alpha = 0.14f).compositeOver(LocalTempoPalette.current.surface)
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = profileTextColor(color, surface),
        letterSpacing = 1.sp,
        modifier =
            Modifier
                .clip(RoundedCornerShape(50))
                .background(surface)
                .border(0.8.dp, color.copy(alpha = 0.35f), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

// Unit for star-tier thresholds, derived from the badge id's domain prefix.
private fun getBadgeUnit(badgeId: String): String =
    when {
        badgeId.startsWith("plays_") || badgeId == "first_play" -> "plays"
        badgeId.startsWith("time_") -> "hours"
        badgeId.startsWith("streak_") -> "days"
        badgeId.startsWith("artists_") -> "artists"
        badgeId.startsWith("genres_") -> "genres"
        badgeId == "night_owl" -> "late-night plays"
        badgeId == "early_bird" -> "early plays"
        badgeId == "marathon" -> "sessions"
        badgeId.startsWith("level_") -> "level"
        else -> ""
    }

