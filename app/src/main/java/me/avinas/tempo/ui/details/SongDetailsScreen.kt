@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.avinas.tempo.ui.details

import me.avinas.tempo.ui.effects.tempoAmbientBackdrop
import me.avinas.tempo.ui.effects.tempoTiltSurface

/* Tempo · SongDetails · Audio Features & Listening History.
 * Human-centered copy standards (behuman):
 * - Clear, concrete facts over adjectives and puffery
 * - Direct, verb-first buttons and concise microcopy
 * - Human musical translations paired with precision data
 * - Zero emojis, 100% SVG vector icons and disciplined layout tokens
 */


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.window.Dialog
import me.avinas.tempo.ui.components.TempoDialogSurface
import me.avinas.tempo.ui.components.TempoDialogIcon
import me.avinas.tempo.ui.components.TempoDialogTitle
import me.avinas.tempo.ui.components.TempoDialogBody
import me.avinas.tempo.ui.components.TempoDialogButtonRow
import me.avinas.tempo.ui.components.TempoDialogDangerButton
import me.avinas.tempo.ui.components.TempoDialogSecondaryButton
import me.avinas.tempo.ui.components.TempoDialogTextField
import me.avinas.tempo.ui.components.TempoDropdownMenu
import me.avinas.tempo.ui.components.TempoDropdownMenuItem
import me.avinas.tempo.ui.components.TempoIcons
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight

import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.avinas.tempo.R
import me.avinas.tempo.data.enrichment.MusicBrainzEnrichmentService
import me.avinas.tempo.data.repository.TrackAudioFeatures
import me.avinas.tempo.data.stats.DailyListening
import me.avinas.tempo.data.stats.TagBasedMoodAnalyzer
import me.avinas.tempo.data.stats.TrackDetails
import me.avinas.tempo.data.stats.TrackEngagement
import me.avinas.tempo.ui.components.AlbumArtImage
import me.avinas.tempo.ui.components.ArtAtmosphereLayer
import me.avinas.tempo.ui.components.DeepOceanBackground
import me.avinas.tempo.ui.components.GlassCard
import me.avinas.tempo.ui.components.GlassCardVariant
import me.avinas.tempo.ui.components.FrostedIconButton
import me.avinas.tempo.ui.components.SharePreviewDialog
import me.avinas.tempo.ui.components.SongShareCard
import me.avinas.tempo.ui.spotify.SpotifyViewModel
import me.avinas.tempo.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun SongDetailsScreen(
    trackId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToAlbum: (String, String) -> Unit,
    viewModel: SongDetailsViewModel = hiltViewModel(),
    spotifyViewModel: SpotifyViewModel = hiltViewModel(),
) {
    val appearancePalette = LocalTempoPalette.current
    val uiState by viewModel.uiState.collectAsState()
    val isPlayingPreview by viewModel.isPlayingPreview.collectAsState()
    val previewProgress by viewModel.previewProgress.collectAsState()
    val previewPositionMs by viewModel.previewPositionMs.collectAsState()
    val trackDetails = uiState.trackDetails

    // Clean up preview on back navigation
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopAudioPreview()
        }
    }

    if (uiState.isLoading) {
        SongDetailsLoadingSkeleton()
    } else if (trackDetails != null) {
        SongDetailsContent(
            trackDetails = trackDetails,
            uiState = uiState,
            isPlayingPreview = isPlayingPreview,
            previewProgress = previewProgress,
            previewPositionMs = previewPositionMs,
            onTogglePreview = { viewModel.toggleAudioPreview() },
            onNavigateBack = onNavigateBack,
            onNavigateToArtist = onNavigateToArtist,
            onNavigateToAlbum = onNavigateToAlbum,
            viewModel = viewModel,
        )
    } else {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicOff,
                    contentDescription = null,
                    tint = TempoError.copy(alpha = 0.7f),
                    modifier = Modifier.size(36.dp),
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = uiState.error ?: stringResource(R.string.details_track_not_found),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedButton(
                    onClick = { viewModel.refresh() },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(0.8.dp, appearancePalette.border.copy(alpha = 0.65f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = appearancePalette.onSurface),
                ) {
                    Text(stringResource(R.string.common_retry).uppercase(Locale.getDefault()))
                }
            }
        }
    }

    // Auto-return only after successful delete/merge; a load error stays put so
    // the retry button above remains reachable.
    LaunchedEffect(uiState.showDeleteDialog, uiState.trackDetails, uiState.isLoading, uiState.error) {
        if (!uiState.isLoading && !uiState.showDeleteDialog && !uiState.isDeleting &&
            uiState.trackDetails == null && uiState.error == null
        ) {
            onNavigateBack()
        }
    }
}

@Composable
private fun SongDetailsLoadingSkeleton() {
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SkeletonBlock(modifier = Modifier.size(232.dp), cornerRadius = 22.dp)
            Spacer(modifier = Modifier.height(24.dp))
            SkeletonBlock(modifier = Modifier.size(160.dp, 22.dp), cornerRadius = 8.dp)
            Spacer(modifier = Modifier.height(12.dp))
            SkeletonBlock(modifier = Modifier.size(100.dp, 14.dp), cornerRadius = 7.dp)
            Spacer(modifier = Modifier.height(30.dp))
            SkeletonBlock(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(152.dp),
                cornerRadius = 22.dp,
            )
        }
    }
}

// Shared with AlbumDetailsScreen (same package).
@Composable
internal fun SkeletonBlock(modifier: Modifier, cornerRadius: Dp) {
    val appearancePalette = LocalTempoPalette.current
    // Static skeletons avoid an infinite transition per track/album placeholder.
    Box(
        modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(GlassFrostMedium)
            .border(0.8.dp, appearancePalette.border.copy(alpha = 0.45f), RoundedCornerShape(cornerRadius))
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDetailsContent(
    trackDetails: TrackDetails,
    uiState: SongDetailsUiState,
    isPlayingPreview: Boolean,
    previewProgress: Float,
    previewPositionMs: Long,
    onTogglePreview: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToAlbum: (String, String) -> Unit,
    viewModel: SongDetailsViewModel = hiltViewModel(),
) {
    val appearancePalette = LocalTempoPalette.current
    var showMenu by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showMergeDialog by remember { mutableStateOf(false) }

    val dominantColor = rememberDominantColor(trackDetails)
    val listState = rememberLazyListState()

    // Collapsed header title appears when the hero title itself scrolls under
    // the top bar — anchored to its live position, not a scroll-pixel guess.
    // The callback compares against the threshold and only writes on a flip,
    // so scrolling never churns recomposition.
    val density = LocalDensity.current
    val headerBottomPx = with(density) {
        (WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 56.dp).toPx()
    }
    var showCollapsedTitle by remember { mutableStateOf(false) }
    val headerScrimAlpha by animateFloatAsState(
        targetValue = if (showCollapsedTitle) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>(),
        label = "headerScrim",
    )

    // Sections below need real playback data, not just an empty engagement row.
    val engagement = uiState.engagement
    val hasPlaybackData = engagement != null && engagement.playCount > 0
    val hasJourney = trackDetails.firstPlayed != null ||
        trackDetails.lastPlayed != null ||
        uiState.engagement?.firstPlayedTimestamp != null ||
        uiState.habitualHour != null
    val hasListeningHistory = uiState.listeningHistory.isNotEmpty()
    val hasAcousticData = uiState.audioFeatures != null

    // Assign sequential numbering for currently visible sections
    val sectionNumbers = remember(hasPlaybackData, hasJourney, hasListeningHistory, hasAcousticData) {
        var next = 0
        mapOf(
            "odyssey" to hasJourney,
            "trends" to hasListeningHistory,
            "acoustic" to hasAcousticData,
            "playback" to hasPlaybackData,
        ).mapNotNull { (id, visible) ->
            if (visible) id to "%02d".format(++next) else null
        }.toMap()
    }

    Box(Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().tempoAmbientBackdrop()) {
            ArtAtmosphereLayer(
                artUrl = trackDetails.track.albumArtUrl
                    ?.takeIf { it.isNotBlank() }
                    ?.let { MusicBrainzEnrichmentService.fixHttpUrl(it) }
                    ?: trackDetails.localBackupArtUrl,
                tint = dominantColor,
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 60.dp,
                    bottom = 64.dp
                ),
            ) {
                // Hero stage (artwork, preview controls, metadata)
                item(key = "hero_section") {
                    SongHeroEditorialStage(
                        trackDetails = trackDetails,
                        dominantColor = dominantColor,
                        previewUrl = uiState.previewUrl,
                        isPlayingPreview = isPlayingPreview,
                        previewProgress = previewProgress,
                        previewPositionMs = previewPositionMs,
                        onTogglePreview = onTogglePreview,
                        onNavigateToArtist = onNavigateToArtist,
                        onNavigateToAlbum = onNavigateToAlbum,
                        onEditTitle = { viewModel.showEditTitleDialog() },
                        onEditCover = { viewModel.showCoverPicker() },
                        onHotlinkSuccess = {
                            viewModel.consumeLocalArtworkBackup(trackDetails.localBackupArtUrl)
                        },
                        onTitlePositioned = { top ->
                            val collapsed = top <= headerBottomPx
                            if (collapsed != showCollapsedTitle) {
                                showCollapsedTitle = collapsed
                            }
                        },
                        genre = uiState.genre,
                        releaseDate = uiState.releaseDate,
                        releaseYear = uiState.releaseYear,
                        recordLabel = uiState.recordLabel,
                    )
                }

                // Summary statistics
                item(key = "master_stats_masthead") {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        MasterStatMasthead(
                            trackDetails = trackDetails,
                            engagement = uiState.engagement,
                            dominantColor = dominantColor,
                        )
                    }
                }

                // Affinity and library ranking
                if (hasPlaybackData) {
                    item(key = "listener_affinity") {
                        Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 6.dp)) {
                            ListenerAffinitySection(
                                engagement = engagement,
                            )
                        }
                    }
                }

                // Listening milestones
                if (hasJourney) {
                    item(key = "chronological_odyssey") {
                        Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 26.dp)) {
                            ListeningOdysseySection(
                                trackDetails = trackDetails,
                                engagement = uiState.engagement,
                                habitualHour = uiState.habitualHour,
                                habitualHourOfDay = uiState.habitualHourOfDay,
                                tint = dominantColor,
                                sectionNumber = sectionNumbers["odyssey"] ?: "01",
                            )
                        }
                    }
                }

                // Listening history trends
                if (hasListeningHistory) {
                    item(key = "temporal_rhythm") {
                        Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)) {
                            TemporalRhythmSection(
                                history = uiState.listeningHistory,
                                peakBinge = uiState.peakBingeDay,
                                tint = dominantColor,
                                sectionNumber = sectionNumbers["trends"] ?: "02",
                            )
                        }
                    }
                }

                // Audio features spec sheet
                if (hasAcousticData) {
                    item(key = "acoustic_dna") {
                        Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 6.dp)) {
                            AcousticDNASpecSheet(
                                audioFeatures = uiState.audioFeatures,
                                moodSummary = uiState.moodSummary,
                                tint = dominantColor,
                                sectionNumber = sectionNumbers["acoustic"] ?: "03",
                            )
                        }
                    }
                }

                // Playback breakdown
                if (hasPlaybackData) {
                    item(key = "playback_telemetry") {
                        Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 6.dp)) {
                            PlaybackFidelitySection(
                                engagement = engagement,
                                tint = dominantColor,
                                sectionNumber = sectionNumbers["playback"] ?: "04",
                            )
                        }
                    }
                }

                // Share action
                item(key = "streaming_hub") {
                    Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp)) {
                        StreamingHubSection(
                            onShare = { showShareDialog = true },
                        )
                    }
                }

                // 9. Footer
                item(key = "footer") {
                    Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 32.dp)) {
                        SongDetailsFooter()
                    }
                }
            }

            // Gradient scrim so scrolled content never collides with the
            // collapsed title; fades in with the title itself.
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(136.dp)
                    .graphicsLayer { alpha = headerScrimAlpha }
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                appearancePalette.background.copy(alpha = 0.94f),
                                appearancePalette.background.copy(alpha = 0.60f),
                                Color.Transparent,
                            )
                        )
                    )
            )

            // Fixed Top Bar with Collapsible Title
            TopBarNavigation(
                showCollapsedTitle = showCollapsedTitle,
                title = trackDetails.track.title,
                artist = trackDetails.track.artist,
                onNavigateBack = onNavigateBack,
                onShare = { showShareDialog = true },
                onMenuClick = { showMenu = true },
            )

            // Overflow Menu
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 52.dp, end = 16.dp)
            ) {
            TempoDropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                TempoDropdownMenuItem(
                    title = stringResource(R.string.details_merge_duplicate),
                    leadingIcon = TempoIcons.MergeStreams,
                    onClick = {
                        showMenu = false
                        showMergeDialog = true
                    }
                )
                TempoDropdownMenuItem(
                    title = stringResource(R.string.details_delete_song),
                    leadingIcon = TempoIcons.Trash,
                    isDestructive = true,
                    onClick = {
                        showMenu = false
                        viewModel.showDeleteDialog()
                    }
                )
            }
        }
    }
    }

    if (showShareDialog) {
        val spotifyLabel = stringResource(R.string.share_copy_spotify_link)
        val appleLabel = stringResource(R.string.share_copy_apple_link)
        SharePreviewDialog(
            onDismiss = { showShareDialog = false },
            text = listOf(trackDetails.track.title, trackDetails.track.artist, trackDetails.track.album.orEmpty())
                .filter(String::isNotBlank).joinToString("\n"),
            links = listOfNotNull(
                trackDetails.spotifyUrl?.takeIf(String::isNotBlank)?.let { spotifyLabel to it },
                trackDetails.appleMusicUrl?.takeIf(String::isNotBlank)?.let { appleLabel to it },
            ),
            contentToShare = { theme ->
                val engagement = uiState.engagement
                val completionFraction = engagement?.let { it.averageCompletionPercent / 100f }
                val skipFraction = engagement?.let {
                    if (it.playCount > 0) it.skipsCount.toFloat() / it.playCount else null
                }
                SongShareCard(
                    trackDetails = trackDetails,
                    theme = theme,
                    genre = uiState.genre,
                    mood = uiState.moodSummary?.moodName,
                    habitualHour = uiState.habitualHour,
                    peakBingeDay = uiState.peakBingeDay,
                    completionRate = completionFraction,
                    skipRate = skipFraction,
                    releaseYear = uiState.releaseYear,
                )
            },
        )
    }

    if (showMergeDialog) {
        MergeSearchDialog(
            sourceTrackId = trackDetails.track.id,
            onDismiss = { showMergeDialog = false },
            onTrackSelected = { /* Handled in ViewModel */ },
        )
    }

    if (uiState.showCoverPicker) {
        CoverArtPickerSheet(
            state = uiState,
            onSelect = viewModel::selectCover,
            onSelectProvider = viewModel::setSelectedCoverProvider,
            onGallerySelected = viewModel::selectCoverFromGallery,
            onResetAutomatic = viewModel::resetCoverToAutomatic,
            onRetry = { viewModel.retryCoverCandidates() },
            onRetryProvider = viewModel::retryCoverCandidates,
            onDismiss = viewModel::dismissCoverPicker,
        )
    }

    if (uiState.showDeleteDialog) {
        DeleteSongConfirmDialog(
            trackDetails = trackDetails,
            isDeleting = uiState.isDeleting,
            onConfirm = viewModel::deleteTrack,
            onDismiss = viewModel::dismissDeleteDialog,
        )
    }

    val mergeTarget = uiState.mergeTargetTrack
    if (uiState.showEditTitleDialog && mergeTarget == null) {
        EditTitleDialog(
            currentTitle = trackDetails.track.title,
            isSaving = uiState.isSavingTitle,
            error = uiState.editTitleError,
            onClearWarnings = viewModel::clearEditTitleWarnings,
            onSave = { newTitle -> viewModel.updateTrackTitle(newTitle) },
            onDismiss = viewModel::dismissEditTitleDialog,
        )
    }

    if (mergeTarget != null) {
        MergeConfirmDialog(
            target = mergeTarget,
            isMerging = uiState.isSavingTitle,
            error = uiState.editTitleError,
            onConfirm = viewModel::confirmEditTitleMerge,
            onDismiss = viewModel::cancelEditTitleMerge,
        )
    }
}

// ──────────────────────────────────────────────────────────────
// Top Bar Navigation with Collapsible Title
// ──────────────────────────────────────────────────────────────

@Composable
private fun TopBarNavigation(
    showCollapsedTitle: Boolean,
    title: String,
    artist: String,
    onNavigateBack: () -> Unit,
    onShare: () -> Unit,
    onMenuClick: () -> Unit,
) {
    val appearancePalette = LocalTempoPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FrostedIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.details_action_back),
            onClick = onNavigateBack,
        )

        // Collapsed Header Title (smooth fade in)
        AnimatedVisibility(
            visible = showCollapsedTitle,
            enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec<Float>()) +
                    slideInVertically(MaterialTheme.motionScheme.fastSpatialSpec<androidx.compose.ui.unit.IntOffset>()) { -10 },
            exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec<Float>()) +
                    slideOutVertically(MaterialTheme.motionScheme.fastSpatialSpec<androidx.compose.ui.unit.IntOffset>()) { -10 },
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = appearancePalette.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = artist,
                    style = MaterialTheme.typography.labelSmall,
                    color = appearancePalette.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (!showCollapsedTitle) {
            Spacer(modifier = Modifier.weight(1f))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FrostedIconButton(
                icon = Icons.Rounded.Share,
                contentDescription = stringResource(R.string.details_action_share_card),
                onClick = onShare,
            )

            FrostedIconButton(
                icon = Icons.Rounded.MoreVert,
                contentDescription = stringResource(R.string.details_action_song_options),
                onClick = onMenuClick,
            )
        }
    }
}


// ──────────────────────────────────────────────────────────────
// 1. Song Hero Stage
// ──────────────────────────────────────────────────────────────

