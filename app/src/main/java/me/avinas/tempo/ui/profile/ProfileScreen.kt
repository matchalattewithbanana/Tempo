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
