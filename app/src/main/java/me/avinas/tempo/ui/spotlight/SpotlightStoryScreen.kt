@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.avinas.tempo.ui.spotlight

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import me.avinas.tempo.ui.components.CaptureController
import me.avinas.tempo.ui.components.CaptureWrapper
import me.avinas.tempo.ui.components.rememberCaptureController
import me.avinas.tempo.utils.ShareUtils
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.filled.Pause
import me.avinas.tempo.R
import me.avinas.tempo.ui.theme.rememberReducedMotion

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpotlightStoryScreen(
    storyPages: List<SpotlightStoryPage>,
    onClose: () -> Unit
) {
    // Freeze the page list for the whole playback session. The ViewModel regenerates
    // story pages reactively on every new listening event; if the live list were used,
    // a scrobble landing mid-story could change the page count (optional slides vary),
    // clamping the pager's current page and restarting the timer mid-playback.
    val storyPages = remember { storyPages }
    val pagerState = rememberPagerState(pageCount = { storyPages.size })
    val coroutineScope = rememberCoroutineScope()
    val reducedMotion = rememberReducedMotion()
    val currentProgress = remember { Animatable(0f) }
    var userPaused by rememberSaveable { mutableStateOf(false) }
    var holding by remember { mutableStateOf(false) }
    var sharing by rememberSaveable { mutableStateOf(false) }
    val captureRequested = remember { mutableStateOf(false) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState == Lifecycle.State.RESUMED) }
    fun playbackAllowed() = resumed && !userPaused && !holding && !sharing && !captureRequested.value
    val canPlay = playbackAllowed()
    
    fun moveStoryPage(forward: Boolean) {
        if (sharing || captureRequested.value) return
        val target = pagerState.currentPage + (if (forward) 1 else -1)
        if (target in storyPages.indices) {
            coroutineScope.launch {
                if (reducedMotion) pagerState.scrollToPage(target)
                else pagerState.animateScrollToPage(target)
            }
        } else if (forward) {
            onClose()
        }
    }

    // Audio Controller
    val context = LocalContext.current
    val audioController = remember { SpotlightAudioController(context = context, scope = coroutineScope) }
    val isMuted by audioController.isMuted.collectAsState()
    
    DisposableEffect(audioController) {
        onDispose { audioController.release() }
    }
    DisposableEffect(lifecycle, audioController) {
        val observer = LifecycleEventObserver { _, _ ->
            resumed = lifecycle.currentState == Lifecycle.State.RESUMED
            if (!resumed) audioController.setPaused(true)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    var previewPage by remember { mutableIntStateOf(-1) }
    LaunchedEffect(pagerState.currentPage, canPlay) {
        if (!playbackAllowed() || lifecycle.currentState != Lifecycle.State.RESUMED) {
            audioController.setPaused(true)
            return@LaunchedEffect
        }
        if (previewPage != pagerState.currentPage) {
            previewPage = pagerState.currentPage
            val page = storyPages.getOrNull(previewPage)
            val url = page?.previewUrl
            when {
                url == null -> audioController.fadeOutAndStop()
                page is SpotlightStoryPage.TopTrackSetup -> audioController.playSetup(url)
                else -> audioController.playHighlight(url)
            }
        }
        audioController.setPaused(false)
    }

    // Determine colors based on current page - Narrative Arc Color Story
    // Act 1 (Setup): Cool blues/purples - curiosity, introspection
    // Act 2 (Discoveries): Warm ambers/pinks - excitement, passion  
    // Act 3 (Climax): Bold reds/golds - revelation, celebration
    // Act 4 (Resolution): Serene teals/whites - reflection, peace
    val currentPage = storyPages.getOrNull(pagerState.currentPage)
    val (primaryColor, secondaryColor, tertiaryColor) = when (currentPage) {
        // ACT 1: SETUP - Cool, introspective blues/purples
        is SpotlightStoryPage.ListeningMinutes -> Triple(
            Color(0xFF6366F1), // Indigo - curiosity
            Color(0xFF818CF8), // Light Indigo
            Color(0xFF4F46E5)  // Deep Indigo
        )
        is SpotlightStoryPage.ListeningStreak -> Triple(
            Color(0xFF7C3AED), // Violet - building momentum
            Color(0xFF8B5CF6), // Light Violet
            Color(0xFF6D28D9)  // Deep Violet
        )
        is SpotlightStoryPage.ListeningClock -> Triple(
            Color(0xFF1E1B4B), // Deep Indigo (night)
            Color(0xFF3730A3), // Indigo
            Color(0xFF4338CA)  // Bright Indigo
        )
        
        // ACT 2: DISCOVERIES - Warm, exciting ambers/pinks
        is SpotlightStoryPage.TopArtist -> Triple(
            Color(0xFFEC4899), // Pink - passion
            Color(0xFFF472B6), // Light Pink
            Color(0xFFBE185D)  // Dark Pink
        )
        is SpotlightStoryPage.TopAlbum -> Triple(
            Color(0xFFD97706), // Amber - warmth
            Color(0xFFF59E0B), // Light Amber
            Color(0xFFB45309)  // Dark Amber
        )
        is SpotlightStoryPage.TopSongs -> Triple(
            Color(0xFFF59E0B), // Amber - energy
            Color(0xFFFBBF24), // Light Amber
            Color(0xFFD97706)  // Dark Amber
        )
        is SpotlightStoryPage.TopTrackSetup -> Triple(
            Color(0xFFFB923C), // Orange - anticipation
            Color(0xFFFDBA74), // Light Orange
            Color(0xFFEA580C)  // Dark Orange
        )
        is SpotlightStoryPage.TopGenres -> Triple(
            Color(0xFF10B981), // Emerald - exploration
            Color(0xFF34D399), // Light Emerald
            Color(0xFF059669)  // Dark Emerald
        )
        is SpotlightStoryPage.DiscoveryCount -> Triple(
            Color(0xFF06B6D4), // Cyan - discovery
            Color(0xFF22D3EE), // Light Cyan
            Color(0xFF0891B2)  // Dark Cyan
        )
        is SpotlightStoryPage.WeekdayVsWeekend -> Triple(
            Color(0xFF0284C7), // Sky blue - patterns
            Color(0xFF38BDF8), // Light Sky
            Color(0xFF0369A1)  // Dark Sky
        )
        is SpotlightStoryPage.BingeSession -> Triple(
            Color(0xFFE11D48), // Rose - intensity
            Color(0xFFFB7185), // Light Rose
            Color(0xFFBE123C)  // Dark Rose
        )
        is SpotlightStoryPage.TimeOfDayVibes -> Triple(
            Color(0xFF8B5CF6), // Violet - moods
            Color(0xFFA78BFA), // Light Violet
            Color(0xFF7C3AED)  // Dark Violet
        )
        
        // ACT 3: CLIMAX - Bold, celebratory reds/golds
        is SpotlightStoryPage.AudioMood -> Triple(
            Color(0xFFDC2626), // Red - emotional peak
            Color(0xFFEF4444), // Light Red
            Color(0xFFB91C1C)  // Dark Red
        )
        is SpotlightStoryPage.Personality -> Triple(
            Color(0xFFEAB308), // Gold - REVELATION
            Color(0xFFFDE047), // Light Gold
            Color(0xFFCA8A04)  // Dark Gold
        )
        is SpotlightStoryPage.BadgesEarned -> Triple(
            Color(0xFFD97706), // Amber - achievement
            Color(0xFFF59E0B), // Light Amber
            Color(0xFFB45309)  // Dark Amber
        )
        
        // ACT 4: RESOLUTION - Serene, reflective teals/whites
        is SpotlightStoryPage.LevelUp -> Triple(
            Color(0xFF14B8A6), // Teal - growth
            Color(0xFF2DD4BF), // Light Teal
            Color(0xFF0F766E)  // Dark Teal
        )
        is SpotlightStoryPage.TitleEarned -> Triple(
            Color(0xFF0D9488), // Dark Teal - identity
            Color(0xFF5EEAD4), // Light Teal
            Color(0xFF115E59)  // Deep Teal
        )
        is SpotlightStoryPage.Conclusion -> Triple(
            Color(0xFF64748B), // Slate - reflection
            Color(0xFF94A3B8), // Light Slate
            Color(0xFF475569)  // Dark Slate
        )
        null -> Triple(Color(0xFF6366F1), Color(0xFF818CF8), Color(0xFF4F46E5))
    }

    val captureController = rememberCaptureController()
    val shareLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        sharing = false
    }
    val shareFailed = stringResource(R.string.spotlight_share_failed)
    LaunchedEffect(captureRequested.value) {
        if (!captureRequested.value) return@LaunchedEffect
        val bitmap = withTimeoutOrNull(5_000) {
            val captured = async(start = CoroutineStart.UNDISPATCHED) {
                captureController.capturedBitmap.first()
            }
            withFrameNanos { }
            withFrameNanos { }
            captureController.capture()
            captured.await()
        }
        val success = bitmap != null && ShareUtils.shareBitmap(context, bitmap, launchChooser = { chooser ->
            sharing = true
            shareLauncher.launch(chooser)
        })
        if (!success) {
            sharing = false
            android.widget.Toast.makeText(context, shareFailed, android.widget.Toast.LENGTH_SHORT).show()
        }
        captureRequested.value = false
    }

    // Tracks which page the current progress bar value belongs to. Progress must be
    // reset inside the auto-advance coroutine itself, not in a separate LaunchedEffect:
    // a sibling reset effect races this one over the Animatable's mutex, and if the
    // timer reads the previous page's near-1f progress before the snap lands, the
    // remaining duration collapses to ~0ms and the story skips pages instantly.
    val timerPageIndex = remember { mutableIntStateOf(-1) }

    // Tracks viewed pages so re-visits skip entry animations
    val seenPages = remember { SnapshotStateMap<String, Boolean>() }
    var lastViewedIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != lastViewedIndex) {
            storyPages.getOrNull(lastViewedIndex)?.let { seenPages[it.id] = true }
            lastViewedIndex = pagerState.currentPage
        }
    }

    LaunchedEffect(pagerState.currentPage, canPlay, reducedMotion) {
        val page = pagerState.currentPage
        if (timerPageIndex.value != page) {
            currentProgress.snapTo(0f)
            timerPageIndex.value = page
        }
        if (canPlay && !reducedMotion && storyPages.isNotEmpty()) {
            val remainingMs = ((1f - currentProgress.value) * 15_000).toInt()
            currentProgress.animateTo(1f, tween(durationMillis = remainingMs, easing = LinearEasing))
            if (pagerState.currentPage == page && playbackAllowed() &&
                lifecycle.currentState == Lifecycle.State.RESUMED
            ) {
                moveStoryPage(forward = true)
            }
        }
    }

    // Main Content Box
    Box(modifier = Modifier.fillMaxSize()) {
        
        // 0. Shadow Renderer (For Sharing) - Hidden offscreen
        // Only composed when a capture is requested — avoids doubling render cost on every frame.
        // Renders the CURRENT page at exactly 1080x1920 px for consistent sharing.
        val shadowPage = storyPages.getOrNull(pagerState.currentPage)
        if (captureRequested.value && shadowPage != null) {
            ShadowStoryRenderer(
                page = shadowPage,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                tertiaryColor = tertiaryColor,
                captureController = captureController
            )
        }
        
        // 1. Visible Content (Background + Pager)
        // No CaptureWrapper here anymore
        SpotlightStoryBackground(
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            tertiaryColor = tertiaryColor
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = false // Disable swipe to rely on taps
                ) { pageIndex ->
                    val page = storyPages[pageIndex]
                    Box(modifier = Modifier.fillMaxSize()) {
                        CompositionLocalProvider(
                            LocalStoryReducedMotion provides reducedMotion,
                            LocalStorySeenPages provides seenPages,
                            LocalStoryPageId provides page.id
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                when (page) {
                                    is SpotlightStoryPage.ListeningMinutes -> ListeningMinutesPage(page)
                                    is SpotlightStoryPage.ListeningStreak -> ListeningStreakPage(page)
                                    is SpotlightStoryPage.ListeningClock -> ListeningClockPage(page)
                                    is SpotlightStoryPage.TopArtist -> TopArtistPage(page)
                                    is SpotlightStoryPage.TopAlbum -> TopAlbumPage(page)
                                    is SpotlightStoryPage.TopTrackSetup -> TopTrackSetupPage(page)
                                    is SpotlightStoryPage.TopSongs -> TopSongsPage(page)
                                    is SpotlightStoryPage.DiscoveryCount -> DiscoveryCountPage(page)
                                    is SpotlightStoryPage.AudioMood -> AudioMoodPage(page)
                                    is SpotlightStoryPage.WeekdayVsWeekend -> WeekdayVsWeekendPage(page)
                                    is SpotlightStoryPage.BingeSession -> BingeSessionPage(page)
                                    is SpotlightStoryPage.TimeOfDayVibes -> TimeOfDayVibesPage(page)
                                    is SpotlightStoryPage.BadgesEarned -> BadgesEarnedPage(page)
                                    is SpotlightStoryPage.LevelUp -> LevelUpPage(page)
                                    is SpotlightStoryPage.TitleEarned -> TitleEarnedPage(page)
                                    is SpotlightStoryPage.TopGenres -> TopGenresPage(page)
                                    is SpotlightStoryPage.Personality -> PersonalityPage(page)
                                    is SpotlightStoryPage.Conclusion -> ConclusionPage(page)
                                }
                            }
                        }
                    }
                }
                
                // Watermark (Visible on screen too, optional but keeps consistency)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                ) {
                    Text(
                        text = "Tempo Spotlight",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.5f),
                                offset = androidx.compose.ui.geometry.Offset(0f, 2f),
                                blurRadius = 4f
                            )
                        ),
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // 2. Gesture Overlay (Interactive layer, NOT captured)
        // Placed ABOVE Content to intercept touches throughout the screen.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(reducedMotion) {
                    detectTapGestures(
                        onPress = {
                            holding = true
                            try {
                                tryAwaitRelease()
                            } finally {
                                holding = false
                            }
                        },
                        onTap = { offset ->
                            moveStoryPage(forward = offset.x >= size.width * 0.3f)
                        }
                    )
                }
        )

        // 3. UI Overlays (Top Bar, Share Button, etc.)

        // Visual indicator shown during hold-to-pause
        AnimatedVisibility(
            visible = holding && !reducedMotion,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.Center)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.spotlight_paused),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
        
        // Top Bar (Progress & Close)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                .align(Alignment.TopCenter)
        ) {
            val position = if (storyPages.isEmpty()) "" else stringResource(
                R.string.spotlight_story_position, pagerState.currentPage + 1, storyPages.size
            )
            Row(
                modifier = Modifier.fillMaxWidth().semantics {
                    if (position.isNotEmpty()) {
                        contentDescription = position
                        liveRegion = LiveRegionMode.Polite
                    }
                },
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                storyPages.forEachIndexed { index, _ ->
                    val progress = when {
                        index < pagerState.currentPage -> 1f
                        index == pagerState.currentPage -> if (reducedMotion) 1f else currentProgress.value
                        else -> 0f
                    }
                    // Dim upcoming segments and brighten watched ones
                    val trackAlpha = when {
                        index < pagerState.currentPage -> 0.5f
                        index == pagerState.currentPage -> 0.3f
                        else -> 0.16f
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = trackAlpha))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val controlsEnabled = resumed && !sharing && !captureRequested.value
                StoryControl(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    label = stringResource(R.string.home_trend_previous),
                    enabled = controlsEnabled && pagerState.currentPage > 0,
                    onClick = { moveStoryPage(forward = false) }
                )
                StoryControl(
                    icon = if (userPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    label = stringResource(if (userPaused) R.string.spotlight_resume else R.string.spotlight_pause),
                    enabled = controlsEnabled,
                    onClick = { userPaused = !userPaused }
                )
                val lastPage = pagerState.currentPage >= storyPages.lastIndex
                StoryControl(
                    icon = if (lastPage) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowForward,
                    label = stringResource(if (lastPage) R.string.common_done else R.string.how_it_works_next),
                    enabled = controlsEnabled,
                    onClick = { moveStoryPage(forward = true) }
                )
                StoryControl(
                    icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    label = stringResource(if (isMuted) R.string.spotlight_unmute else R.string.spotlight_mute),
                    enabled = controlsEnabled,
                    onClick = { audioController.toggleMute() }
                )
                StoryControl(
                    icon = Icons.Default.Close,
                    label = stringResource(R.string.share_close),
                    enabled = controlsEnabled,
                    onClick = onClose
                )
            }
        }

        // Share Button (Bottom)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                .padding(bottom = 48.dp)
        ) {
             Button(
                onClick = { captureRequested.value = true },
                enabled = resumed && !sharing && !captureRequested.value,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.2f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(50),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    stringResource(R.string.spotlight_share_your_story),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Loading overlay while preparing share image
        if (captureRequested.value) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Text(
                        text = stringResource(R.string.spotlight_preparing),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun StoryControl(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    // These controls sit above artwork and generated story colors: one
    // translucent neutral Material surface retains contrast across pages.
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(48.dp),
        shapes = IconButtonDefaults.shapes(),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = Color.Black.copy(alpha = 0.72f),
            contentColor = Color.White,
            disabledContainerColor = Color.Black.copy(alpha = 0.36f),
            disabledContentColor = Color.White.copy(alpha = 0.46f),
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
fun ShadowStoryRenderer(
    page: SpotlightStoryPage,
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color,
    captureController: CaptureController
) {
    // 1. Force Density for consistent scale (Pixel 2 XL density approx)
    val targetDensity = androidx.compose.ui.unit.Density(2.75f)
    
    // 2. Hide offscreen (safe culling avoidance)
    Box(
        modifier = Modifier
            .offset(x = 10000.dp)
            .wrapContentSize(unbounded = true)
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalDensity provides targetDensity
        ) {
            // 3. Custom Layout to force 1080x1920 measurement
            androidx.compose.ui.layout.Layout(
                content = {
                    CaptureWrapper(
                        controller = captureController,
                        modifier = Modifier.size(392.dp, 698.dp) // 1080px/2.75, 1920px/2.75 (Must match desired pixel / density)
                    ) {
                         SpotlightStoryBackground(
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            tertiaryColor = tertiaryColor
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                when (page) {
                                    is SpotlightStoryPage.ListeningMinutes -> ListeningMinutesPage(page)
                                    is SpotlightStoryPage.ListeningStreak -> ListeningStreakPage(page)
                                    is SpotlightStoryPage.ListeningClock -> ListeningClockPage(page)
                                    is SpotlightStoryPage.TopArtist -> TopArtistPage(page)
                                    is SpotlightStoryPage.TopAlbum -> TopAlbumPage(page)
                                    is SpotlightStoryPage.TopTrackSetup -> TopTrackSetupPage(page)
                                    is SpotlightStoryPage.TopSongs -> TopSongsPage(page)
                                    is SpotlightStoryPage.DiscoveryCount -> DiscoveryCountPage(page)
                                    is SpotlightStoryPage.AudioMood -> AudioMoodPage(page)
                                    is SpotlightStoryPage.WeekdayVsWeekend -> WeekdayVsWeekendPage(page)
                                    is SpotlightStoryPage.BingeSession -> BingeSessionPage(page)
                                    is SpotlightStoryPage.TimeOfDayVibes -> TimeOfDayVibesPage(page)
                                    is SpotlightStoryPage.BadgesEarned -> BadgesEarnedPage(page)
                                    is SpotlightStoryPage.LevelUp -> LevelUpPage(page)
                                    is SpotlightStoryPage.TitleEarned -> TitleEarnedPage(page)
                                    is SpotlightStoryPage.TopGenres -> TopGenresPage(page)
                                    is SpotlightStoryPage.Personality -> PersonalityPage(page)
                                    is SpotlightStoryPage.Conclusion -> ConclusionPage(page)
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 24.dp)
                                ) {
                                    Text(
                                        text = "Tempo Spotlight",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            shadow = androidx.compose.ui.graphics.Shadow(
                                                color = Color.Black.copy(alpha = 0.5f),
                                                offset = androidx.compose.ui.geometry.Offset(0f, 2f),
                                                blurRadius = 4f
                                            )
                                        ),
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            ) { measurables, _ ->
                // Force measure to exactly 1080x1920 pixels
                // We derived the Dp size above (392x698) based on density 2.75
                // 392 * 2.75 = 1078. 698 * 2.75 = 1919.5. Close enough.
                // Better: rely on fixed Pixel size if possible? 
                // CaptureWrapper captures the View. The View size is determined by the Compose layout.
                // If we specify Modifier.size(392.72.dp, 698.18.dp), we get 1080x1920.
                
                val widthPx = 1080
                val heightPx = 1920
                
                val placeable = measurables[0].measure(
                     androidx.compose.ui.unit.Constraints.fixed(widthPx, heightPx)
                )
                
                layout(widthPx, heightPx) {
                    placeable.place(0, 0)
                }
            }
        }
    }
}
