package me.avinas.tempo.ui.profile

/**
 * Profile screen displaying listener level, listening statistics,
 * daily challenges, and earned badges.
 */

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
internal fun EditorialSectionHeader(
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
    // Static focus mark: no perpetual animation or per-frame UI work.

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(6.dp)
.clip(CircleShape)
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
            fontSize = MaterialTheme.typography.labelSmall.fontSize,
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
internal fun ProgressTrack(
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
internal fun XpChip(xp: Int) {
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
internal fun StarsChip(
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
        enter = fadeIn(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()) +
            slideInVertically(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()) { -10 },
        exit = fadeOut(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()) +
            slideOutVertically(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()) { -10 },
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
    // An initial loading placeholder should not run an infinite animation loop.
    val currentAlpha = 0.65f

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

    Box(Modifier.fillMaxSize().tempoAmbientBackdrop()) {
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
                                            timeRemaining = streakTimeRemaining,
                                        )
                                    }

                                    if (appearance.showProfileProgress && uiState.levelLoaded && !hideStreaks) {
                                        MasterStatMasthead(
                                            userLevel = uiState.userLevel,
                                            currentStreak = streak.days,
                                            streakAtRisk = streakMinutes != null,
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
                            // Opaque scrim keeps the status/nav actions readable
                            // when the scrolling hero passes underneath them.
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
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

// These are layout decisions for Tempo, not official M3 breakpoint tokens.
// Keep narrow and accessibility typography behavior directly testable.
internal fun shouldStackProfileIdentity(compact: Boolean, fontScale: Float): Boolean =
    (compact && fontScale >= 1.3f) || fontScale >= 1.8f

internal fun shouldStackProfileMetrics(widthDp: Float, fontScale: Float): Boolean =
    widthDp < 320f || fontScale >= 1.3f

// ──────────────────────────────────────────────────────────────
// Hero Profile Section
// ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HeroProfileSection(
    userLevel: UserLevel,
    userTitle: String,
    userName: String,
    profileImagePath: String?,
    tierAccent: Color,
    compact: Boolean = false,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = userLevel.levelProgress.coerceIn(0f, 1f),
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>(),
        label = "levelProgress",
    )
    val swatch = me.avinas.tempo.ui.components.tempoTileSwatch(
        me.avinas.tempo.ui.components.TempoTileTone.AQUA
    )
    val stackIdentity = shouldStackProfileIdentity(compact, LocalDensity.current.fontScale)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraExtraLarge,
        color = swatch.container,
        contentColor = swatch.foreground,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // The profile identity is the only hero: avoid abstract background
            // shapes, repeated level labels and a second nested progress card.
            if (stackIdentity) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    HeroAvatar(
                        progress = animatedProgress,
                        level = userLevel.currentLevel,
                        userName = userName,
                        profileImagePath = profileImagePath,
                        tierAccent = tierAccent,
                        compact = true,
                    )
                    ProfileIdentityText(
                        name = userName,
                        title = userTitle,
                        level = userLevel.currentLevel,
                        foreground = swatch.foreground,
                        centered = true,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
                ) {
                    HeroAvatar(
                        progress = animatedProgress,
                        level = userLevel.currentLevel,
                        userName = userName,
                        profileImagePath = profileImagePath,
                        tierAccent = tierAccent,
                        compact = compact,
                    )
                    Box(Modifier.weight(1f)) {
                        ProfileIdentityText(
                            name = userName,
                            title = userTitle,
                            level = userLevel.currentLevel,
                            foreground = swatch.foreground,
                        )
                    }
                }
            }
            // Progress is secondary information inside the hero, not another
            // nested card competing for attention.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.profile_bento_total_xp),
                    style = MaterialTheme.typography.labelMedium,
                    color = swatch.foreground,
                )
                Text(
                    text = String.format(Locale.getDefault(), "%,d", userLevel.totalXp),
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    color = swatch.foreground,
                )
            }
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth(),
                color = swatch.foreground,
                trackColor = swatch.foreground.copy(alpha = 0.16f),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${(animatedProgress * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    color = swatch.foreground,
                )
                Text(
                    text = String.format(Locale.getDefault(), "%,d XP to next level", userLevel.xpRemaining),
                    style = MaterialTheme.typography.labelSmall,
                    color = swatch.foreground,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProfileIdentityText(
    name: String,
    title: String,
    level: Int,
    foreground: Color,
    centered: Boolean = false,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleLargeEmphasized,
            fontWeight = FontWeight.Bold,
            color = foreground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        )
        Text(
            text = if (title.isNotBlank()) "Level $level · $title" else "Level $level",
            style = MaterialTheme.typography.labelMedium,
            color = foreground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        )
    }
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
                fontSize = MaterialTheme.typography.labelSmall.fontSize,
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Summary Stats Block
// ──────────────────────────────────────────────────────────────

private data class ProfileMetricSpec(
    val value: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val supportingText: String? = null,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProfileProgressTile(spec: ProfileMetricSpec, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.heightIn(min = 104.dp),
        shape = MaterialTheme.shapes.large,
        color = scheme.surfaceContainerLow,
        contentColor = scheme.onSurface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = spec.icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = scheme.primary,
            )
            Text(
                text = spec.value,
                style = MaterialTheme.typography.titleLargeEmphasized,
                color = scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = spec.label,
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
                maxLines = 2,
            )
            spec.supportingText?.let { note ->
                Text(
                    text = note,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.error,
                    maxLines = 2,
                )
            }
        }
    }
}

/** A compact listening streak overview; XP and levels live in the identity hero. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MasterStatMasthead(
    userLevel: UserLevel,
    currentStreak: Int,
    streakAtRisk: Boolean,
    hideStreaks: Boolean,
) {
    if (hideStreaks) return
    val streakValue = androidx.compose.ui.res.pluralStringResource(
        R.plurals.profile_bento_streak_day,
        currentStreak.coerceAtLeast(0),
        currentStreak.coerceAtLeast(0),
    )
    val bestStreakValue = androidx.compose.ui.res.pluralStringResource(
        R.plurals.profile_bento_streak_day,
        userLevel.longestStreak.coerceAtLeast(0),
        userLevel.longestStreak.coerceAtLeast(0),
    )
    val metrics = listOf(
        ProfileMetricSpec(
            value = streakValue,
            label = stringResource(R.string.profile_bento_current_streak),
            icon = Icons.Rounded.LocalFireDepartment,
            supportingText = if (streakAtRisk) stringResource(R.string.profile_bento_streak_warning) else null,
        ),
        ProfileMetricSpec(
            value = bestStreakValue,
            label = stringResource(R.string.profile_bento_best_streak),
            icon = Icons.Rounded.Whatshot,
        ),
    )
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val singleColumn = shouldStackProfileMetrics(maxWidth.value, LocalDensity.current.fontScale)
        if (singleColumn) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                metrics.forEach { metric ->
                    ProfileProgressTile(metric, Modifier.fillMaxWidth())
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                metrics.forEach { metric ->
                    ProfileProgressTile(metric, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StreakRiskBanner(
    timeRemaining: String,
) {
    // A status warning needs a legible paired color, not an always-dark glass
    // card with accent-colored text that can disappear in light or OLED themes.
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = scheme.errorContainer,
        contentColor = scheme.onErrorContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.LocalFireDepartment,
                contentDescription = null,
                tint = scheme.onErrorContainer,
                modifier = Modifier.size(24.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Streak at risk",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onErrorContainer,
                )
                Text(
                    text = "Play a track in the next $timeRemaining to keep your streak alive.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onErrorContainer,
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Segmented Tab Switcher
// ──────────────────────────────────────────────────────────────

/**
 * Native M3 Expressive tabs keep profile achievements and badges discoverable
 * and remove the old offset-indicator animation and extra compositing layers.
 */
@Composable
private fun TabSwitcher(
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        androidx.compose.material3.PrimaryTabRow(
            selectedTabIndex = selectedTab.coerceIn(0, (tabs.size - 1).coerceAtLeast(0)),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            tabs.forEachIndexed { index, title ->
                androidx.compose.material3.Tab(
                    selected = selectedTab == index,
                    onClick = { if (selectedTab != index) onTabSelected(index) },
                    text = {
                        Text(title, maxLines = 1,
                            style = MaterialTheme.typography.labelLarge,
                            overflow = TextOverflow.Ellipsis)
                    },
                )
            }
        }
    }
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
