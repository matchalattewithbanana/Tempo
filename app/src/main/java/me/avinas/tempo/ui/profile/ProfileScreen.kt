package me.avinas.tempo.ui.profile

/**
 * Profile screen displaying listener level, listening statistics,
 * daily challenges, and earned badges.
 */

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

// Widest the scrolling content ever gets — expanded widths (tablets, landscape,
// foldables) keep a readable measure instead of stretching cards edge to edge.
private val ProfileMaxContentWidth = 660.dp

// ── Level tier palette — connects visual radiance directly to listener rank ──
internal fun getLevelTierAccent(level: Int): Color =
    when {
        level >= 100 -> Color(0xFFEC4899)

        // Mythic Pink
        level >= 75 -> Color(0xFFF43F5E)

        // Rose
        level >= 50 -> TempoWarningBright

        // Gold (#FBBF24)
        level >= 35 -> Color(0xFFA855F7)

        // Purple
        level >= 20 -> TempoInfo

        // Electric Blue (#3B82F6)
        level >= 10 -> TempoAccent

        // Cyan (#5EEAD4)
        level >= 5 -> TempoSuccessBright

        // Emerald (#4ADE80)
        else -> TempoPrimary // Studio Teal (#2FDBB8)
    }

// ── Category & difficulty identity — data colormaps, not chrome tokens ──
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

// Challenge titles generated before the custom icon set landed carry a leading emoji.
// Persisted rows survive until the next daily refresh, so drop it at render time rather
// than showing the emoji next to its replacement icon for the rest of the day.
private val LeadingEmoji = Regex("^[^\\p{L}\\p{N}]+")

private fun String.withoutLeadingEmoji(): String = replace(LeadingEmoji, "")

/**
 * Custom icon for a challenge, keyed on [DailyChallenge.challengeId] rather than category.
 *
 * The five categories collapse eight distinct challenge types — VOLUME covers both
 * song-count and minute-count, DISCOVERY covers artists and genres — and each type has its
 * own icon. Ids are stable and persisted, so they survive daily title regeneration.
 */
private fun getChallengeIcon(challenge: DailyChallenge): Int {
    val id = challenge.challengeId
    return when {
        id.startsWith("volume_songs") -> {
            R.drawable.ic_challenge_songs
        }

        id.startsWith("volume_mins") -> {
            R.drawable.ic_challenge_minutes
        }

        id.startsWith("variety_artists") -> {
            R.drawable.ic_challenge_variety
        }

        id.startsWith("discovery_artists") -> {
            R.drawable.ic_challenge_discovery_artists
        }

        id.startsWith("discovery_genres") -> {
            R.drawable.ic_challenge_discovery_genres
        }

        id.startsWith("explore_artist") -> {
            R.drawable.ic_challenge_explore_artist
        }

        id.startsWith("explore_genre") -> {
            R.drawable.ic_challenge_explore_genre
        }

        id.startsWith("time_early_bird") -> {
            R.drawable.ic_challenge_early_bird
        }

        // Unrecognised id (e.g. a type added by a newer version) — fall back on category,
        // matching the previous category-keyed behaviour.
        else -> {
            when (challenge.category) {
                "TIME" -> R.drawable.ic_challenge_early_bird
                "VARIETY" -> R.drawable.ic_challenge_variety
                "DISCOVERY" -> R.drawable.ic_challenge_discovery_genres
                "EXPLORATION" -> R.drawable.ic_challenge_explore_artist
                else -> R.drawable.ic_challenge_songs
            }
        }
    }
}

private fun getDifficultyColor(difficulty: String): Color =
    when (difficulty) {
        "EASY" -> TempoSuccessDeep
        "MEDIUM" -> TempoWarning
        "HARD" -> TempoError
        else -> Color.Gray
    }

// ──────────────────────────────────────────────────────────────
// Editorial Section Catalog Kicker & Section Header
// ──────────────────────────────────────────────────────────────

@Composable
internal fun SectionCatalogKicker(
    number: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val appearancePalette = LocalTempoPalette.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = number,
            style = KickerSmall,
            fontWeight = FontWeight.Bold,
            color = appearancePalette.onSurface,
        )
        Box(
            modifier =
                Modifier
                    .width(10.dp)
                    .height(0.8.dp)
                    .background(appearancePalette.border.copy(alpha = 0.65f)),
        )
        Text(
            text = label.uppercase(Locale.getDefault()),
            style = KickerSmall,
            color = appearancePalette.mutedText,
            letterSpacing = 1.2.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EditorialSectionHeader(
    sectionNumber: String,
    title: String,
    subtitle: String,
    trailing: (@Composable () -> Unit)? = null,
) {
    val appearancePalette = LocalTempoPalette.current
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionCatalogKicker(number = sectionNumber, label = title)
            if (trailing != null) {
                trailing()
            }
        }
        Text(
            text = subtitle,
            style = CaptionSmall,
            color = appearancePalette.mutedText,
        )
    }
}

// ──────────────────────────────────────────────────────────────
// Masthead Hero Metric & Secondary Stat Blocks (Editorial System)
// ──────────────────────────────────────────────────────────────

@Composable
internal fun MastheadHeroMetric(
    label: String,
    value: String,
    suffix: String,
    accentTint: Color,
) {
    val appearancePalette = LocalTempoPalette.current
    val reducedMotion = rememberReducedMotion()
    val pulseScale by if (reducedMotion) {
        remember { mutableFloatStateOf(1f) }
    } else {
        val transition = me.avinas.tempo.ui.theme.rememberMotionAwareInfiniteTransition(label = "heroMetricPulse")
        transition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.35f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "heroMetricPulseScale",
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(6.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }.clip(CircleShape)
                        .background(accentTint),
            )
            Text(
                text = label.uppercase(Locale.getDefault()),
                style = KickerSmall,
                color = appearancePalette.mutedText,
                letterSpacing = 1.4.sp,
            )
        }
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = value,
                style =
                    MaterialTheme.typography.displayMedium.copy(
                        fontFamily = DisplayFontFamily,
                        letterSpacing = (-1.5).sp,
                    ),
                fontWeight = FontWeight.Bold,
                color = appearancePalette.onSurface,
                lineHeight = 44.sp,
            )
            if (suffix.isNotBlank()) {
                Text(
                    text = suffix,
                    style = MaterialTheme.typography.titleSmall,
                    color = appearancePalette.mutedText,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
        }
    }
}

@Composable
internal fun MastheadSecondaryStat(
    label: String,
    value: String,
    subtext: String,
    modifier: Modifier = Modifier,
    valueColor: Color = LocalTempoPalette.current.onSurface,
) {
    val appearancePalette = LocalTempoPalette.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label.uppercase(Locale.getDefault()),
            style = KickerSmall,
            color = appearancePalette.mutedText,
            letterSpacing = 1.2.sp,
            fontSize = 10.sp,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = profileTextColor(valueColor),
        )
        if (subtext.isNotBlank()) {
            Text(
                text = subtext,
                style = CaptionSmall,
                color = appearancePalette.mutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Hairline Progress Track & Chips
// ──────────────────────────────────────────────────────────────

@Composable
private fun ProgressTrack(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = LocalTempoPalette.current.accent,
    height: Dp = 6.dp,
) {
    val track = profileIndicatorColor(LocalTempoPalette.current.border)
    val fill = profileIndicatorColor(color)
    Box(
        modifier = modifier
            .height(height)
            .clip(CircleShape)
            .border(1.dp, track, CircleShape)
            .drawBehind {
                val p = progress.coerceIn(0f, 1f)
                if (p > 0f) {
                    val cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
                    drawRoundRect(color = fill, size = Size(size.width * p, size.height), cornerRadius = cornerRadius)
                }
            },
    )
}

@Composable
private fun XpChip(xp: Int) {
    val chipSurface = TempoWarning.copy(alpha = 0.12f).compositeOver(LocalTempoPalette.current.surface)
    val chipText = profileTextColor(TempoWarning, chipSurface)
    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(chipSurface)
                .border(0.8.dp, TempoWarning.copy(alpha = 0.32f), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = chipText,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = "+$xp XP",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = chipText,
            letterSpacing = 0.4.sp,
        )
    }
}

@Composable
private fun StarsChip(
    total: Int,
    max: Int,
) {
    val chipSurface = TempoWarning.copy(alpha = 0.12f).compositeOver(LocalTempoPalette.current.surface)
    val chipText = profileTextColor(TempoWarning, chipSurface)
    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(chipSurface)
                .border(0.8.dp, TempoWarning.copy(alpha = 0.32f), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Star,
            contentDescription = null,
            tint = chipText,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = "$total / $max",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = chipText,
            letterSpacing = 0.4.sp,
        )
    }
}

/** Compact identity shown in the top bar once the hero identity scrolls out of view. */
@Composable
private fun CollapsingProfileTitle(
    visible: Boolean,
    userName: String,
    level: Int,
    title: String,
) {
    val appearancePalette = LocalTempoPalette.current
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { -10 },
        exit = fadeOut(tween(140)) + slideOutVertically(tween(140)) { -10 },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = userName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = appearancePalette.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Level $level · $title",
                style = MaterialTheme.typography.labelSmall,
                color = getLevelTierAccent(level),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Loading Skeleton State
// ──────────────────────────────────────────────────────────────

@Composable
internal fun SkeletonBlock(
    modifier: Modifier,
    cornerRadius: Dp,
    alpha: Float = 0.65f,
) {
    val appearancePalette = LocalTempoPalette.current
    Box(
        modifier
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(cornerRadius))
            .background(GlassFrostMedium)
            .border(0.8.dp, appearancePalette.border.copy(alpha = 0.45f), RoundedCornerShape(cornerRadius)),
    )
}

@Composable
private fun ProfileLoadingSkeleton(
    compact: Boolean,
    contentModifier: Modifier,
    heroTopClearance: Dp,
) {
    val reducedMotion = rememberReducedMotion()
    val pulse =
        me.avinas.tempo.ui.theme.rememberMotionAwareInfiniteTransition(label = "profileSkeleton").animateFloat(
            initialValue = 0.40f,
            targetValue = 0.85f,
            animationSpec =
                infiniteRepeatable(
                    tween(900, easing = FastOutSlowInEasing),
                    RepeatMode.Reverse,
                ),
            label = "profileSkeletonAlpha",
        )
    val currentAlpha = if (reducedMotion) 0.65f else pulse.value

    Column(
        modifier = contentModifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Spacer(modifier = Modifier.height(heroTopClearance))
        val avatarSize = if (compact) 104.dp else 116.dp
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 18.dp),
        ) {
            SkeletonBlock(modifier = Modifier.size(avatarSize), cornerRadius = avatarSize / 2, alpha = currentAlpha)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SkeletonBlock(modifier = Modifier.size(160.dp, 24.dp), cornerRadius = 8.dp, alpha = currentAlpha)
                SkeletonBlock(modifier = Modifier.size(120.dp, 14.dp), cornerRadius = 7.dp, alpha = currentAlpha)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        SkeletonBlock(modifier = Modifier.fillMaxWidth().height(86.dp), cornerRadius = 20.dp, alpha = currentAlpha)
        SkeletonBlock(modifier = Modifier.fillMaxWidth().height(150.dp), cornerRadius = 22.dp, alpha = currentAlpha)
        SkeletonBlock(modifier = Modifier.fillMaxWidth().height(48.dp), cornerRadius = 16.dp, alpha = currentAlpha)
        SkeletonBlock(modifier = Modifier.fillMaxWidth().height(120.dp), cornerRadius = 20.dp, alpha = currentAlpha)
        SkeletonBlock(modifier = Modifier.fillMaxWidth().height(120.dp), cornerRadius = 20.dp, alpha = currentAlpha)
    }
}

// ──────────────────────────────────────────────────────────────
// Main Profile Screen
// ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val applicationContext = LocalContext.current.applicationContext
    LaunchedEffect(lifecycle, viewModel, applicationContext) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            kotlinx.coroutines.coroutineScope {
                launch { viewModel.observeWhileVisible() }
                launch { viewModel.observeChallenges(applicationContext) }
            }
        }
    }
    val scope = rememberCoroutineScope()
    val appearance = LocalTempoAppearance.current
    var tabOverride by rememberSaveable { mutableStateOf<Int?>(null) }
    val selectedTab = tabOverride ?: if (appearance.profileTab == AppearanceProfileTab.BADGES) 1 else 0
    var showProfileShare by rememberSaveable { mutableStateOf(false) }
    var includeIdentity by rememberSaveable { mutableStateOf(false) }
    var shareChallenge by remember { mutableStateOf<DailyChallenge?>(null) }
    var selectedBadge by remember { mutableStateOf<Badge?>(null) }
    var shareBadgeTarget by remember { mutableStateOf<Badge?>(null) }
    var showMilestoneShare by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val heroScrolledPast by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }
    val hideStreaks = me.avinas.tempo.ui.components.LocalHideStreaks.current
    var streakNow by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(lifecycle, applicationContext, hideStreaks) {
        if (!hideStreaks) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                observeClockChanges(applicationContext, everyMinute = true).collect {
                    streakNow = Instant.now()
                }
            }
        }
    }
    val streak = if (hideStreaks) ProfileStreakStatus() else ProfileStreakStatus.at(
        uiState.userLevel, streakNow, uiState.streakAnchorZone ?: ZoneId.systemDefault()
    )
    val streakMinutes = streak.minutesUntilExpiry
    val streakTimeRemaining = streakMinutes?.let { "${it / 60}h ${it % 60}m" }.orEmpty()


    val tierAccent =
        remember(uiState.userLevel.currentLevel) {
            getLevelTierAccent(uiState.userLevel.currentLevel)
        }

    Box(Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compact = maxWidth < 380.dp
            val expanded = maxWidth >= 600.dp
            val sidePadding =
                if (compact) {
                    16.dp
                } else if (expanded) {
                    24.dp
                } else {
                    20.dp
                }
            val contentModifier =
                Modifier
                    .fillMaxWidth()
                    .then(if (expanded) Modifier.widthIn(max = ProfileMaxContentWidth) else Modifier)
                    .padding(horizontal = sidePadding)
            val badgeColumns =
                when {
                    maxWidth >= 840.dp -> 4
                    expanded -> 3
                    else -> 2
                }
            val tabs = if (compact) listOf("Quests", "Badges") else listOf("Challenges", "Badges")
            val heroTopClearance = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp

            Box(modifier = Modifier.fillMaxSize()) {
                // Ambient atmosphere layer linked to avatar image if present
                if (!uiState.profileImagePath.isNullOrBlank()) {
                    ArtAtmosphereLayer(
                        artUrl = uiState.profileImagePath,
                        tint = tierAccent,
                    )
                }

                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { scope.launch { viewModel.refresh() } },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (uiState.isLoading) {
                        ProfileLoadingSkeleton(
                            compact = compact,
                            contentModifier = contentModifier,
                            heroTopClearance = heroTopClearance,
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 132.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            item(key = "hero") {
                                Column(modifier = contentModifier, horizontalAlignment = Alignment.CenterHorizontally) {
                                    Spacer(modifier = Modifier.height(heroTopClearance))
                                    if (uiState.problems.any { it != ProfileProblem.ACKNOWLEDGEMENT }) {
                                        ProfileRetryMessage(
                                            onRetry = { scope.launch { viewModel.refresh() } },
                                            retryEnabled = !uiState.isRefreshing,
                                        )
                                    }
                                    if (uiState.levelLoaded) {
                                        HeroProfileSection(
                                            userLevel = uiState.userLevel,
                                            userTitle = uiState.userTitle,
                                            userName = uiState.userName,
                                            profileImagePath = uiState.profileImagePath,
                                            tierAccent = tierAccent,
                                            compact = compact,
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(20.dp))
                                }
                            }

                            if (appearance.showProfileNowPlaying) item(key = "now_playing") {
                                Box(modifier = contentModifier) {
                                    LiveNowPlayingCard()
                                }
                                Spacer(modifier = Modifier.height(18.dp))
                            }

                            item(key = "stats_tabs") {
                                Column(
                                    modifier = contentModifier,
                                    verticalArrangement = Arrangement.spacedBy(22.dp),
                                ) {
                                    if (appearance.showProfileStreakWarning && streakMinutes != null) {
                                        StreakRiskBanner(
                                            streakDurationMinutes = streakMinutes,
                                            timeRemaining = streakTimeRemaining,
                                        )
                                    }

                                    if (appearance.showProfileProgress && uiState.levelLoaded && uiState.badgesLoaded) {
                                        MasterStatMasthead(
                                            userLevel = uiState.userLevel,
                                            earnedCount = uiState.earnedCount,
                                            totalCount = uiState.totalCount,
                                            totalStars = uiState.totalStars,
                                            currentStreak = streak.days,
                                            streakAtRisk = streakMinutes != null,
                                            timeRemaining = streakTimeRemaining,
                                            tierAccent = tierAccent,
                                            compact = compact,
                                            hideStreaks = hideStreaks,
                                        )
                                    }

                                    TabSwitcher(
                                        tabs = tabs,
                                        selectedTab = selectedTab,
                                        onTabSelected = { tabOverride = it },
                                    )
                                }
                            }

                            if (selectedTab == 0) {
                                if (uiState.challenges.isNotEmpty()) {
                                    challengesSection(
                                        challenges = uiState.challenges,
                                        totalXpAvailable = uiState.challengeXpTotal,
                                        onShareChallenge = { shareChallenge = it },
                                        contentModifier = contentModifier,
                                    )
                                } else if (uiState.challengesLoaded &&
                                    ProfileProblem.CHALLENGE_REFRESH !in uiState.problems
                                ) {
                                    item(key = "empty_challenges") {
                                        Column(modifier = contentModifier) {
                                            Spacer(modifier = Modifier.height(24.dp))
                                            EmptyChallengesState()
                                        }
                                    }
                                }
                            } else if (uiState.badgesLoaded) {
                                badgeSection(
                                    filteredBadges = uiState.filteredBadges,
                                    categories = uiState.categories,
                                    selectedCategory = uiState.selectedCategory,
                                    onCategorySelected = viewModel::onCategorySelected,
                                    onBadgeClick = { badge ->
                                        selectedBadge = badge
                                    },
                                    contentModifier = contentModifier,
                                    badgeColumns = badgeColumns,
                                )
                            }
                        }
                    }
                }

                // Top Bar Navigation (matching SongDetails frosted action spec)
                Row(
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .then(if (expanded) Modifier.widthIn(max = ProfileMaxContentWidth) else Modifier)
                            .statusBarsPadding()
                            .padding(horizontal = sidePadding, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FrostedIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.settings_back),
                        onClick = onBack,
                    )
                    Box(modifier = Modifier.weight(1f).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                        CollapsingProfileTitle(
                            visible = heroScrolledPast,
                            userName = uiState.userName,
                            level = uiState.userLevel.currentLevel,
                            title = uiState.userTitle,
                        )
                    }
                    if (uiState.levelLoaded) {
                        FrostedIconButton(
                            icon = Icons.Default.Share,
                            contentDescription = stringResource(R.string.share_profile),
                            onClick = {
                                includeIdentity = false
                                showProfileShare = true
                            },
                        )
                    }
                    FrostedIconButton(
                        icon = Icons.Rounded.Settings,
                        contentDescription = stringResource(R.string.settings_title),
                        onClick = onNavigateToSettings,
                    )
                }
            }

            if (!showProfileShare && shareChallenge == null && selectedBadge == null &&
                shareBadgeTarget == null && uiState.showLevelUpCelebration
            ) {
                LevelUpCelebrationOverlay(
                    level = uiState.userLevel.currentLevel,
                    title = uiState.userTitle,
                    userName = uiState.userName,
                    profileImagePath = uiState.profileImagePath,
                    totalXp = uiState.userLevel.totalXp,
                    currentStreak = uiState.userLevel.currentStreak,
                    onDismiss = viewModel::dismissLevelUpCelebration,
                    onShareMilestone = { showMilestoneShare = true },
                )
            }

            if (!showProfileShare && shareChallenge == null && selectedBadge == null && uiState.unacknowledgedBadges.isNotEmpty() &&
                (!uiState.showLevelUpCelebration || shareBadgeTarget != null)
            ) {
                BadgeCelebrationOverlay(
                    badges = uiState.unacknowledgedBadges,
                    userName = uiState.userName,
                    profileImagePath = uiState.profileImagePath,
                    onDismiss = { viewModel.acknowledgeBadges(uiState.unacknowledgedBadges.map { it.badgeId }) },
                    onShareBadge = { badgeToShare -> shareBadgeTarget = badgeToShare },
                    acknowledging = uiState.acknowledgingBadges,
                    acknowledgementFailed = ProfileProblem.ACKNOWLEDGEMENT in uiState.problems,
                )
            }

            if (selectedBadge != null) {
                val badge = selectedBadge!!
                val liveBadge = uiState.allBadges.firstOrNull { it.badgeId == badge.badgeId } ?: badge
                BadgeDetailsOverlay(
                    badge = liveBadge,
                    onDismiss = {
                        selectedBadge = null
                    },
                    onShareBadge = { badgeToShare -> shareBadgeTarget = badgeToShare },
                )
            }

            if (showProfileShare && uiState.levelLoaded) {
                val level = stringResource(R.string.stats_period_number, uiState.userLevel.currentLevel)
                SnapshotShareDialog(
                    snapshot = ShareSnapshot(
                        title = if (includeIdentity) uiState.userName
                            else stringResource(R.string.share_profile_title),
                        subtitle = uiState.userTitle,
                        artUrl = uiState.profileImagePath.takeIf { includeIdentity },
                        metrics = buildList {
                            add(stringResource(R.string.share_profile_level_label) to level)
                            if (uiState.badgesLoaded) {
                                add(stringResource(R.string.profile_badges) to stringResource(R.string.stats_period_number, uiState.earnedCount))
                            }
                        },
                    ),
                    onDismiss = { showProfileShare = false },
                    settings = {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                            Checkbox(checked = includeIdentity, onCheckedChange = { includeIdentity = it })
                            Text(stringResource(R.string.share_profile_include_identity))
                        }
                    },
                )
            }

            shareChallenge?.let { challenge ->
                SnapshotShareDialog(
                    snapshot = ShareSnapshot(
                        title = challenge.title.withoutLeadingEmoji(),
                        subtitle = stringResource(R.string.share_challenge_completed),
                        metrics = listOf(stringResource(R.string.share_challenge_reward) to stringResource(R.string.share_challenge_xp, challenge.xpReward)),
                    ),
                    onDismiss = { shareChallenge = null },
                )
            }

            val badgeToShare = shareBadgeTarget
            if (badgeToShare != null) {
                SharePreviewDialog(
                    onDismiss = { shareBadgeTarget = null },
                    text = listOf(badgeToShare.name, badgeToShare.description, badgeToShare.starLabel).joinToString("\n"),
                ) { theme ->
                    BadgeShareCard(
                        badge = badgeToShare,
                        userName = uiState.userName,
                        profileImagePath = uiState.profileImagePath,
                        theme = theme,
                    )
                }
            } else if (showMilestoneShare) {
                SharePreviewDialog(
                    onDismiss = { showMilestoneShare = false },
                    text = listOf(uiState.userTitle, stringResource(R.string.share_profile_level, uiState.userLevel.currentLevel))
                        .joinToString("\n"),
                ) { theme ->
                    MilestoneShareCard(
                        level = uiState.userLevel.currentLevel,
                        title = uiState.userTitle,
                        userName = uiState.userName,
                        profileImagePath = uiState.profileImagePath,
                        totalXp = uiState.userLevel.totalXp,
                        streak = if (hideStreaks) 0 else uiState.userLevel.currentStreak,
                        theme = theme,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileRetryMessage(onRetry: () -> Unit, retryEnabled: Boolean) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.profile_load_failed),
            color = LocalTempoPalette.current.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        TextButton(onClick = onRetry, enabled = retryEnabled) {
            Text(stringResource(R.string.common_retry))
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Hero Profile Section
// ──────────────────────────────────────────────────────────────

@Composable
private fun HeroProfileSection(
    userLevel: UserLevel,
    userTitle: String,
    userName: String,
    profileImagePath: String?,
    tierAccent: Color,
    compact: Boolean = false,
) {
    val appearancePalette = LocalTempoPalette.current
    val animatedProgress by animateFloatAsState(
        targetValue = userLevel.levelProgress,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "levelProgress",
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 18.dp),
        ) {
            HeroAvatar(
                progress = animatedProgress,
                level = userLevel.currentLevel,
                userName = userName,
                profileImagePath = profileImagePath,
                tierAccent = tierAccent,
                compact = compact,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // User Name in Display Font
                Text(
                    text = userName,
                    style =
                        (if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium).copy(
                            fontFamily = DisplayFontFamily,
                            letterSpacing = (-0.5).sp,
                        ),
                    fontWeight = FontWeight.Bold,
                    color = appearancePalette.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // Editorial Subtitle Kicker
                val levelKicker =
                    if (userTitle.isNotBlank()) {
                        "LEVEL ${userLevel.currentLevel}  ·  ${userTitle.uppercase(Locale.getDefault())}"
                    } else {
                        "LEVEL ${userLevel.currentLevel}"
                    }
                Text(
                    text = levelKicker,
                    style = KickerSmall,
                    color = profileTextColor(tierAccent),
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Level Progress Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            variant = GlassCardVariant.Obsidian,
            borderColor = appearancePalette.border.copy(alpha = 0.45f),
            borderWidth = 0.8.dp,
            contentPadding = PaddingValues(16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "LEVEL ${userLevel.currentLevel} PROGRESS",
                        style = KickerSmall,
                        color = appearancePalette.mutedText,
                        letterSpacing = 1.1.sp,
                    )
                    ProgressPercentLabel(progress = animatedProgress, tierAccent = tierAccent)
                }

                ProgressTrack(
                    progress = animatedProgress,
                    modifier = Modifier.fillMaxWidth(),
                    color = tierAccent,
                    height = 6.dp,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "%,d XP total", userLevel.totalXp),
                        style = CaptionSmall,
                        color = appearancePalette.mutedText,
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%,d XP to next level", userLevel.xpRemaining),
                        style = CaptionSmall,
                        color = appearancePalette.mutedText,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressPercentLabel(
    progress: Float,
    tierAccent: Color,
) {
    val percent = (progress * 100).roundToInt()
    Text(
        text = "$percent%",
        style = KickerSmall,
        color = profileTextColor(tierAccent),
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun InnerAvatarCircle(
    innerSize: Dp,
    tierAccent: Color,
    userName: String,
    profileImagePath: String?,
) {
    val appearancePalette = LocalTempoPalette.current
    Box(
        modifier =
            Modifier
                .size(innerSize)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = appearancePalette.surfaceSunken.copy(alpha = 0.30f),
                    spotColor = tierAccent.copy(alpha = 0.25f),
                ).clip(CircleShape)
                .background(appearancePalette.surfaceSunken)
                .border(1.dp, appearancePalette.border, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (profileImagePath.isNullOrBlank()) {
            Text(
                text = userName.firstOrNull()?.toString()?.uppercase() ?: "U",
                style =
                    MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                    ),
                color = appearancePalette.onSurface,
            )
        } else {
            CachedAsyncImage(
                imageUrl = profileImagePath,
                contentDescription = "Avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun HeroAvatar(
    progress: Float,
    level: Int,
    userName: String,
    profileImagePath: String?,
    tierAccent: Color,
    compact: Boolean = false,
) {
    val appearancePalette = LocalTempoPalette.current
    val ringSize = if (compact) 84.dp else 96.dp
    val innerSize = if (compact) 66.dp else 76.dp
    val haloSize = ringSize + 20.dp

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(haloSize)) {
        // Soft atmospheric radial glow halo — inspired by SongDetails' artwork halo
        Box(
            modifier =
                Modifier
                    .size(haloSize)
                    .background(
                        brush =
                            Brush.radialGradient(
                                colors =
                                    listOf(
                                        tierAccent.copy(alpha = 0.22f),
                                        tierAccent.copy(alpha = 0.0f),
                                    ),
                            ),
                        shape = CircleShape,
                    ),
        )

        // Progress arc ring
        Canvas(modifier = Modifier.size(ringSize)) {
            val strokeWidth = 5.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2f, size.height / 2f)
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2, radius * 2)

            drawArc(
                color = Color.White.copy(alpha = 0.08f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            val sweep = 360f * progress.coerceIn(0f, 1f)
            if (sweep > 0.5f) {
                drawArc(
                    brush =
                        Brush.sweepGradient(
                            listOf(LevelRingSweepStart, LevelRingSweepMid, LevelRingSweepEnd),
                            center,
                        ),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
            val angle = Math.toRadians((-90f + sweep).toDouble())
            val dotCenter =
                Offset(
                    center.x + radius * cos(angle).toFloat(),
                    center.y + radius * sin(angle).toFloat(),
                )
            drawCircle(color = LevelRingSweepEnd, radius = 5.5.dp.toPx(), center = dotCenter)
            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = dotCenter)
        }

        InnerAvatarCircle(
            innerSize = innerSize,
            tierAccent = tierAccent,
            userName = userName,
            profileImagePath = profileImagePath,
        )
        // Anchored level pill badge
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-2).dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(appearancePalette.surface)
                    .border(0.8.dp, tierAccent.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 9.dp, vertical = 3.dp),
        ) {
            Text(
                text = "LVL $level",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = profileTextColor(tierAccent),
                letterSpacing = 0.8.sp,
                fontSize = 10.sp,
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Summary Stats Block
// ──────────────────────────────────────────────────────────────

@Composable
private fun MasterStatMasthead(
    userLevel: UserLevel,
    earnedCount: Int,
    totalCount: Int,
    totalStars: Int,
    currentStreak: Int,
    streakAtRisk: Boolean,
    timeRemaining: String,
    tierAccent: Color,
    compact: Boolean,
    hideStreaks: Boolean = false,
) {
    val appearancePalette = LocalTempoPalette.current

    val suffix =
        when {
            streakAtRisk -> if (currentStreak == 1) "day  ·  ends in $timeRemaining" else "days  ·  ends in $timeRemaining"
            currentStreak == 1 -> "day in motion"
            else -> "days in motion"
        }
    val accentTint = if (streakAtRisk) TempoErrorSoft else tierAccent

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        variant = GlassCardVariant.Obsidian,
        borderColor = if (streakAtRisk) TempoErrorSoft.copy(alpha = 0.35f) else appearancePalette.border.copy(alpha = 0.45f),
        borderWidth = 0.8.dp,
        contentPadding = PaddingValues(0.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (compact) 16.dp else 20.dp, vertical = 20.dp),
        ) {
            // Hero metric. The streak is the only thing this card wants to lead with, so when
            // streaks are hidden the card opens straight on XP and badges instead of a placeholder.
            if (!hideStreaks) {
                MastheadHeroMetric(
                    label = "Listening rhythm",
                    value = "$currentStreak",
                    suffix = suffix,
                    accentTint = accentTint,
                )

                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(0.8.dp)
                            .background(appearancePalette.border.copy(alpha = 0.45f)),
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Secondary metrics row with vertical hairline dividers
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!hideStreaks) {
                    MastheadSecondaryStat(
                        label = "Best streak",
                        value = "${userLevel.longestStreak}d",
                        subtext = "Personal record",
                        modifier = Modifier.weight(1f),
                    )

                    Box(
                        modifier =
                            Modifier
                                .fillMaxHeight()
                                .width(0.8.dp)
                                .background(appearancePalette.border.copy(alpha = 0.45f)),
                    )
                }

                MastheadSecondaryStat(
                    label = "Total XP",
                    value = String.format(Locale.getDefault(), "%,d", userLevel.totalXp),
                    subtext = "Rank: Level ${userLevel.currentLevel}",
                    modifier =
                        Modifier
                            .weight(1f)
                            // The inset separates this stat from the divider before it; with no
                            // streak column ahead of it there is nothing to separate from.
                            .padding(start = if (hideStreaks) 0.dp else if (compact) 10.dp else 16.dp),
                )

                Box(
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .width(0.8.dp)
                            .background(appearancePalette.border.copy(alpha = 0.45f)),
                )

                MastheadSecondaryStat(
                    label = "Badges",
                    value = "$earnedCount / $totalCount",
                    subtext = if (totalStars > 0) "$totalStars stars" else "Unlocked",
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(start = if (compact) 10.dp else 16.dp),
                    valueColor = if (earnedCount > 0) TempoWarning else appearancePalette.onSurface,
                )
            }
        }
    }
}

@Composable
private fun StreakRiskBanner(
    streakDurationMinutes: Long,
    timeRemaining: String,
) {
    val appearancePalette = LocalTempoPalette.current
    val riskColor =
        when {
            streakDurationMinutes > 360 -> TempoErrorSoft
            streakDurationMinutes > 180 -> TempoError
            else -> TempoErrorDeep
        }
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        variant = GlassCardVariant.Obsidian,
        borderColor = riskColor.copy(alpha = 0.40f),
        borderWidth = 0.8.dp,
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(riskColor.copy(alpha = 0.14f))
                        .border(0.8.dp, riskColor.copy(alpha = 0.32f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocalFireDepartment,
                    contentDescription = null,
                    tint = riskColor,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Streak at risk",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = appearancePalette.onSurface,
                )
                Text(
                    text = "Play a track in the next $timeRemaining to keep your streak alive.",
                    style = MaterialTheme.typography.bodySmall,
                    color = appearancePalette.mutedText,
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Segmented Tab Switcher
// ──────────────────────────────────────────────────────────────

@Composable
private fun TabSwitcher(
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    val appearancePalette = LocalTempoPalette.current
    val reducedMotion = rememberReducedMotion()
    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(appearancePalette.surfaceSunken)
                .border(0.8.dp, appearancePalette.border.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                .padding(4.dp),
    ) {
        val tabWidth = maxWidth / tabs.size
        val indicatorPosition by animateFloatAsState(
            targetValue = selectedTab.toFloat(),
            animationSpec = tween(durationMillis = if (reducedMotion) 0 else 160, easing = FastOutSlowInEasing),
            label = "tabIndicator",
        )
        Box(modifier = Modifier.matchParentSize()) {
            Box(
                modifier =
                    Modifier
                        .offset {
                            IntOffset(
                                x = (tabWidth.toPx() * indicatorPosition).roundToInt(),
                                y = 0,
                            )
                        }.width(tabWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(appearancePalette.accent.copy(alpha = 0.16f))
                        .border(0.8.dp, appearancePalette.accent.copy(alpha = 0.42f), RoundedCornerShape(12.dp)),
            )
        }
        Row(modifier = Modifier.fillMaxWidth().selectableGroup()) {
            tabs.forEachIndexed { index, title ->
                val selected = selectedTab == index
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) appearancePalette.onSurface else appearancePalette.mutedText,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .selectable(selected = selected, role = Role.Tab) { onTabSelected(index) }
                            .padding(vertical = 11.dp, horizontal = 8.dp),
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Daily Challenges (Quests) Section
// ──────────────────────────────────────────────────────────────

private fun LazyListScope.challengesSection(
    challenges: List<DailyChallenge>,
    totalXpAvailable: Int,
    contentModifier: Modifier,
    onShareChallenge: (DailyChallenge) -> Unit,
) {
    val completedCount = challenges.count { it.isCompleted }
    item(key = "challenges_header") {
        val resetLabel =
            remember {
                val midnight =
                    LocalDate
                        .now()
                        .plusDays(1)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                val diffMs = midnight - System.currentTimeMillis()
                val h = (diffMs / (1000 * 60 * 60)).toInt()
                val m = ((diffMs % (1000 * 60 * 60)) / (1000 * 60)).toInt()
                if (h > 0) "resets in ${h}h ${m}m" else "resets in ${m}m"
            }
        Column(modifier = contentModifier) {
            Spacer(modifier = Modifier.height(24.dp))
            EditorialSectionHeader(
                sectionNumber = "01",
                title = "Daily quests",
                subtitle = "$completedCount of ${challenges.size} complete · $resetLabel",
                trailing = { XpChip(xp = totalXpAvailable) },
            )
            Spacer(modifier = Modifier.height(14.dp))
            ProgressTrack(
                progress = if (challenges.isEmpty()) 0f else completedCount.toFloat() / challenges.size,
                modifier = Modifier.fillMaxWidth(),
                color = LocalTempoPalette.current.accent,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    itemsIndexed(challenges, key = { _, c -> "challenge_${c.id}" }) { _, challenge ->
        Column(modifier = contentModifier) {
            ChallengeCard(challenge = challenge)
            if (challenge.isCompleted) {
                TextButton(onClick = { onShareChallenge(challenge) }, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.share_challenge))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun EmptyChallengesState(modifier: Modifier = Modifier) {
    val appearancePalette = LocalTempoPalette.current
    GlassCard(
        modifier = modifier,
        variant = GlassCardVariant.Obsidian,
        shape = RoundedCornerShape(20.dp),
        borderColor = appearancePalette.border.copy(alpha = 0.45f),
        borderWidth = 0.8.dp,
        contentPadding = PaddingValues(28.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(appearancePalette.surfaceSunken)
                        .border(0.8.dp, appearancePalette.border.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = appearancePalette.mutedText,
                    modifier = Modifier.size(26.dp),
                )
            }
            Text(
                text = "Nothing queued yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = appearancePalette.mutedText,
            )
            Text(
                text = "Pull to refresh or continue listening — new daily quests will appear here.",
                style = MaterialTheme.typography.bodySmall,
                color = appearancePalette.mutedText,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ChallengePercentText(
    progress: Float,
    accent: Color,
) {
    Text(
        text = "${(progress * 100).toInt()}%",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = profileTextColor(accent),
    )
}

@Composable
private fun ChallengeCard(challenge: DailyChallenge) {
    val appearancePalette = LocalTempoPalette.current
    val isCompleted = challenge.isCompleted
    val accent = if (isCompleted) TempoSuccessDeep else getDifficultyColor(challenge.difficulty)
    val difficultySurface = accent.copy(alpha = 0.12f).compositeOver(appearancePalette.surfaceSunken)
    val rewardSurface = (if (isCompleted) TempoSuccessDeep.copy(alpha = 0.12f) else TempoWarning.copy(alpha = 0.10f))
        .compositeOver(appearancePalette.surfaceSunken)
    val animatedProgress by animateFloatAsState(
        targetValue = challenge.progressFraction,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "challengeProgress",
    )

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        variant = GlassCardVariant.Obsidian,
        borderColor = if (isCompleted) TempoSuccessDeep.copy(alpha = 0.25f) else appearancePalette.border.copy(alpha = 0.45f),
        borderWidth = 0.8.dp,
        contentPadding = PaddingValues(18.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Top metadata row: Difficulty tag, category label, and XP reward pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(difficultySurface)
                                .border(0.8.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = challenge.difficulty.uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = profileTextColor(accent, difficultySurface),
                            letterSpacing = 0.8.sp,
                            fontSize = 10.sp,
                        )
                    }
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.labelSmall,
                        color = appearancePalette.mutedText,
                    )
                    Text(
                        text = challenge.category.replace("_", " ").uppercase(Locale.getDefault()),
                        style = KickerSmall,
                        fontWeight = FontWeight.Medium,
                        color = appearancePalette.mutedText,
                        letterSpacing = 0.8.sp,
                        fontSize = 10.sp,
                    )
                }

                Row(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(rewardSurface)
                            .border(
                                0.8.dp,
                                if (isCompleted) TempoSuccessDeep.copy(alpha = 0.30f) else TempoWarning.copy(alpha = 0.25f),
                                RoundedCornerShape(8.dp),
                            ).padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = profileIndicatorColor(if (isCompleted) TempoSuccessDeep else TempoWarning, rewardSurface),
                        modifier = Modifier.size(11.dp),
                    )
                    Text(
                        text = "+${challenge.xpReward} XP",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = profileTextColor(if (isCompleted) TempoSuccessDeep else TempoWarning, rewardSurface),
                        letterSpacing = 0.4.sp,
                    )
                }
            }

            // Core content row: Sunken Icon + Title & Description
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(appearancePalette.surfaceSunken)
                            .border(0.8.dp, accent.copy(alpha = if (isCompleted) 0.40f else 0.20f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(getChallengeIcon(challenge)),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = challenge.title.withoutLeadingEmoji(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = appearancePalette.onSurface,
                    )
                    Text(
                        text = challenge.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = appearancePalette.mutedText,
                        lineHeight = 18.sp,
                    )
                }
            }

            // Hairline Progress bar
            ProgressTrack(
                progress = animatedProgress,
                modifier = Modifier.fillMaxWidth(),
                color = accent,
                height = 5.dp,
            )

            // Bottom row: numeric progress & status/action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${challenge.currentProgress} / ${challenge.targetValue}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCompleted) profileTextColor(TempoSuccessDeep) else appearancePalette.mutedText,
                )

                if (isCompleted) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(TempoSuccessDeep.copy(alpha = 0.12f))
                                .border(0.8.dp, TempoSuccessDeep.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = TempoSuccessDeep,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = "Completed",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = profileTextColor(TempoSuccessDeep),
                        )
                    }
                } else {
                    ChallengePercentText(progress = animatedProgress, accent = accent)
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Trophy Room (Badges) Section
// ──────────────────────────────────────────────────────────────

private fun LazyListScope.badgeSection(
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
            Text(
                stringResource(R.string.profile_no_badges),
                modifier = contentModifier,
                color = LocalTempoPalette.current.mutedText,
            )
        }
    }
    itemsIndexed(
        filteredBadges.chunked(badgeColumns),
        key = { _, rowBadges -> rowBadges.joinToString("-") { it.badgeId } },
    ) { _, rowBadges ->
        Column(modifier = contentModifier) {
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
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
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
                        fontSize = 10.sp,
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
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
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
                fontSize = 10.sp,
            )
            if (isEarned) {
                if (isBeginner) {
                    Text(
                        text = "UNLOCKED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = profileTextColor(intrinsicColor),
                        letterSpacing = 1.sp,
                        fontSize = 10.sp,
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
                            fontSize = 10.sp,
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
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
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

    // Dismiss animation handler
    fun dismissSmoothly() {
        if (isClosing) return
        isClosing = true
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        coroutineScope.launch {
            launch {
                contentAlpha.animateTo(0f, tween(160, easing = FastOutLinearInEasing))
                contentOffsetY.animateTo(28f, tween(160, easing = FastOutLinearInEasing))
            }
            launch {
                emblemScale.animateTo(if (reducedMotion) 0.80f else 0.38f, tween(190, easing = FastOutLinearInEasing))
                emblemAlpha.animateTo(0f, tween(180, easing = FastOutLinearInEasing))
                glowScale.animateTo(0.20f, tween(190, easing = FastOutLinearInEasing))
            }
            delay(220)
            onDismiss()
        }
    }

    // Trigger entrance animations
    LaunchedEffect(badge.badgeId) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        launch {
            emblemAlpha.animateTo(1f, tween(120, easing = LinearEasing))
        }
        launch {
            glowScale.animateTo(
                targetValue = 1.18f,
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
            )
        }
        launch {
            emblemScale.animateTo(
                targetValue = 1f,
                animationSpec =
                    spring(
                        dampingRatio = if (reducedMotion) 1f else 0.62f,
                        stiffness = if (reducedMotion) 400f else 340f,
                    ),
            )
        }
        launch {
            if (!reducedMotion) delay(70)
            launch {
                contentAlpha.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
            }
            launch {
                contentOffsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
                )
            }
        }
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

// Level ring — shared with HomeScreen's compact header ring
@Composable
fun CompactLevelRing(
    progress: Float,
    level: Int,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    val appearancePalette = LocalTempoPalette.current
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 4.dp.toPx()
            val radius = (this.size.minDimension - strokeWidth) / 2f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2f, radius * 2f)

            drawArc(
                color = Color.White.copy(alpha = 0.10f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            val sweep = 360f * progress.coerceIn(0f, 1f)
            if (sweep > 0.5f) {
                drawArc(
                    brush =
                        Brush.sweepGradient(
                            listOf(LevelRingSweepStart, LevelRingSweepMid, LevelRingSweepEnd),
                            center,
                        ),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        }
        Text(
            text = "$level",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = appearancePalette.onSurface,
        )
    }
}
