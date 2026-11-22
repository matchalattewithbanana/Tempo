@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.avinas.tempo.ui.details

import me.avinas.tempo.ui.effects.tempoAmbientBackdrop
import me.avinas.tempo.ui.effects.tempoTiltSurface
import me.avinas.tempo.ui.effects.tempoListMotion

/* Tempo · ArtistDetails · Listening Profile.
 * Mirrors the SongDetails editorial system:
 * - Frosted header with collapsed title on scroll + gradient scrim
 * - Dominant-color accent extracted from the artist image
 * - ArtAtmosphere room layer, skeleton loading, hairline stat masthead
 * - Design tokens (TextPrimary/Secondary/Tertiary, KickerSmall, DisplayFontFamily)
 * - Zero emojis, 100% vector icons
 */

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.*
import me.avinas.tempo.ui.components.TempoDropdownMenu
import me.avinas.tempo.ui.components.TempoDropdownMenuItem
import me.avinas.tempo.ui.components.TempoIcons
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import me.avinas.tempo.R
import me.avinas.tempo.data.stats.ArtistDetails
import me.avinas.tempo.data.stats.TopAlbum
import me.avinas.tempo.data.stats.TopTrack
import me.avinas.tempo.ui.components.ArtAtmosphereLayer
import me.avinas.tempo.ui.components.CachedAsyncImage
import me.avinas.tempo.ui.components.DeepOceanBackground
import me.avinas.tempo.ui.components.GlassCard
import me.avinas.tempo.ui.components.GlassCardVariant
import me.avinas.tempo.ui.components.LocalHideStreaks
import me.avinas.tempo.ui.components.SharePreviewDialog
import me.avinas.tempo.ui.components.ArtistShareCard
import me.avinas.tempo.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ArtistDetailsScreen(
    artistId: Long? = null,
    artistName: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToSong: (Long) -> Unit,
    viewModel: ArtistDetailsViewModel = hiltViewModel()
) {
    val appearancePalette = LocalTempoPalette.current
    // Load artist by ID or name
    LaunchedEffect(artistId, artistName) {
        if (artistId != null && artistId > 0) {
            viewModel.loadArtistById(artistId)
        } else if (artistName != null) {
            viewModel.loadArtistByName(artistName)
        }
    }

    val uiState by viewModel.uiState.collectAsState()
    val artistDetails = uiState.artistDetails

    Box(Modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            ArtistDetailsLoadingSkeleton()
        } else if (artistDetails != null) {
            ArtistDetailsContent(
                artistDetails = artistDetails,
                uiState = uiState,
                onNavigateBack = onNavigateBack,
                onNavigateToSong = onNavigateToSong,
                onRefreshImage = { viewModel.refreshArtistImage() },
                onShowRenameDialog = { viewModel.showRenameDialog() },
                onShowSplitDialog = { viewModel.showSplitDialog() }
            )
        } else {
            // Error state with retry
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = TempoError.copy(alpha = 0.7f),
                        modifier = Modifier.size(36.dp),
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = uiState.error ?: stringResource(R.string.details_artist_not_found),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    OutlinedButton(
                        onClick = { viewModel.retry() },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(0.8.dp, appearancePalette.border.copy(alpha = 0.65f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = appearancePalette.onSurface),
                    ) {
                        Text(stringResource(R.string.common_retry).uppercase(Locale.getDefault()))
                    }
                }
            }
        }
    }

    // Rename dialog lives outside the loading/error branches
    val details = artistDetails
    if (details != null && uiState.showRenameDialog) {
        ArtistRenameDialog(
            currentName = details.artist.name,
            artistId = details.artist.id,
            splitArtists = uiState.detectedSplitArtists,
            isDetecting = uiState.isDetectingSplits,
            isRenaming = uiState.isRenaming,
            renameSuccess = uiState.renameSuccess,
            onDetectSplits = { newName -> viewModel.detectSplitsAndRename(newName) },
            onConfirmRenameAndMerge = { newName, mergeIds ->
                viewModel.confirmRenameAndMerge(newName, mergeIds)
            },
            onConfirmRenameOnly = { newName -> viewModel.confirmRenameOnly(newName) },
            onDismiss = { viewModel.dismissRenameDialog() }
        )
    }

    // Split-artist dialog (manual escape hatch for wrongly merged/collapsed artists)
    if (details != null && uiState.showSplitDialog) {
        val splitSourceId = details.artist.id
        ArtistSplitDialog(
            sourceArtistId = splitSourceId,
            sourceArtistName = details.artist.name,
            onDismiss = { viewModel.dismissSplitDialog() },
            onSplitComplete = { sourceDeleted ->
                viewModel.dismissSplitDialog()
                if (sourceDeleted) {
                    // The artist no longer exists — leave the details screen
                    onNavigateBack()
                } else {
                    // Reload to reflect the tracks that were moved out
                    viewModel.loadArtistById(splitSourceId)
                }
            }
        )
    }
}

@Composable
private fun ArtistDetailsLoadingSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SkeletonBlock(modifier = Modifier.size(232.dp), cornerRadius = 116.dp)
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

@Composable
fun ArtistDetailsContent(
    artistDetails: ArtistDetails,
    uiState: ArtistDetailsUiState,
    onNavigateBack: () -> Unit,
    onNavigateToSong: (Long) -> Unit,
    onRefreshImage: () -> Unit,
    onShowRenameDialog: () -> Unit = {},
    onShowSplitDialog: () -> Unit = {}
) {
    val appearancePalette = LocalTempoPalette.current
    var showMenu by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showMergeDialog by remember { mutableStateOf(false) }

    val accent = rememberArtistAccentColor(artistDetails.artist.imageUrl)
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Collapsed header title appears when the hero name scrolls under the
    // top bar — anchored to its live position, not a scroll-pixel guess.
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

    Box(modifier = Modifier.fillMaxSize().tempoAmbientBackdrop()) {
        // The room recolors per artist: blurred image wash behind all content.
        ArtAtmosphereLayer(
            artUrl = artistDetails.artist.imageUrl?.takeIf { it.isNotBlank() },
            tint = accent,
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
            // 1. Hero Stage
            item(key = "hero_section") {
                ArtistHeroSection(
                    artistDetails = artistDetails,
                    accent = accent,
                    onRefreshImage = onRefreshImage,
                    isRefreshingImage = uiState.isRefreshingImage,
                    onShowRenameDialog = onShowRenameDialog,
                    onNamePositioned = { top ->
                        val collapsed = top <= headerBottomPx
                        if (collapsed != showCollapsedTitle) {
                            showCollapsedTitle = collapsed
                        }
                    },
                )
            }

            // 2. Master Stats Masthead
            item(key = "stats_grid") {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    ArtistStatMasthead(artistDetails = artistDetails, accent = accent)
                }
            }

            // 3. Fan Standing
            item(key = "fan_status") {
                Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 6.dp)) {
                    FanStandingCard(
                        playCount = artistDetails.personalPlayCount,
                        percentile = uiState.artistPercentile,
                        accent = accent,
                    )
                }
            }

            // 4. Listening Journey & Milestones
            item(key = "listening_journey") {
                Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 26.dp)) {
                    ListeningJourneySection(artistDetails = artistDetails, tint = accent)
                }
            }

            // 5. Top Songs
            if (artistDetails.topSongs.isNotEmpty()) {
                item(key = "top_songs_section") {
                    Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 6.dp)) {
                        TopSongsPanel(
                            songs = artistDetails.topSongs.take(5),
                            onNavigateToSong = onNavigateToSong,
                            accent = accent,
                        )
                    }
                }
            }

            // 6. Top Albums
            if (artistDetails.topAlbums.isNotEmpty()) {
                item(key = "top_albums_header") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 14.dp)
                    ) {
                        SectionCatalogKicker(
                            number = "03",
                            label = stringResource(R.string.details_top_albums),
                        )
                    }
                }

                item(key = "top_albums_row") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        // Stable identity key: the old index-baked key renamed every
                        // row on insert/reorder, reusing slots mid-scroll ->
                        // subcompose IllegalArgumentException. Album+artist is the
                        // identity (matches TopAlbum keying in stats).
                        itemsIndexed(
                            items = artistDetails.topAlbums,
                            key = { _, album -> "album_${album.album}_${album.artist}" },
                            contentType = { _, _ -> "album" }
                        ) { index, album ->
                            Box(Modifier.tempoListMotion(index)) {
                                TopAlbumCard(album = album, accent = accent)
                            }
                        }
                    }
                }
            }

            // 7. Footer
            item(key = "footer") {
                Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 32.dp)) {
                    ArtistDetailsFooter()
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
        ArtistTopBarNavigation(
            showCollapsedTitle = showCollapsedTitle,
            title = artistDetails.artist.name,
            subtitle = artistDetails.topGenres.firstOrNull()
                ?: artistDetails.artist.genres.firstOrNull(),
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
                    title = stringResource(R.string.details_merge_with),
                    leadingIcon = TempoIcons.MergeStreams,
                    onClick = {
                        showMenu = false
                        showMergeDialog = true
                    }
                )
                TempoDropdownMenuItem(
                    title = stringResource(R.string.details_rename_artist),
                    leadingIcon = TempoIcons.Edit,
                    onClick = {
                        showMenu = false
                        onShowRenameDialog()
                    }
                )
                TempoDropdownMenuItem(
                    title = stringResource(R.string.details_split_artist),
                    leadingIcon = TempoIcons.SplitStreams,
                    onClick = {
                        showMenu = false
                        onShowSplitDialog()
                    }
                )
            }
        }

        if (showShareDialog) {
            val count = stringResource(R.string.stats_period_number, artistDetails.personalPlayCount)
            val plays = stringResource(R.string.share_text_metric, stringResource(R.string.stats_period_plays), count)
            SharePreviewDialog(
                onDismiss = { showShareDialog = false },
                text = (listOf(artistDetails.artist.name, plays) + artistDetails.topSongs.take(5).map { it.title })
                    .joinToString("\n"),
                contentToShare = { theme ->
                    ArtistShareCard(artistDetails = artistDetails, percentile = uiState.artistPercentile, theme = theme)
                }
            )
        }

        // Artist Merge Dialog
        if (showMergeDialog) {
            ArtistMergeSearchDialog(
                sourceArtistId = artistDetails.artist.id,
                sourceArtistName = artistDetails.artist.name,
                onDismiss = { showMergeDialog = false },
                onMergeComplete = {
                    onNavigateBack()
                }
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Top Bar Navigation with Collapsible Title
// ──────────────────────────────────────────────────────────────

@Composable
private fun ArtistTopBarNavigation(
    showCollapsedTitle: Boolean,
    title: String,
    subtitle: String?,
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
        FrostedHeaderButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.details_action_back),
            onClick = onNavigateBack,
        )

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
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = appearancePalette.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (!showCollapsedTitle) {
            Spacer(modifier = Modifier.weight(1f))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FrostedHeaderButton(
                icon = Icons.Rounded.Share,
                contentDescription = stringResource(R.string.share_content_description),
                onClick = onShare,
            )
            FrostedHeaderButton(
                icon = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.details_action_song_options),
                onClick = onMenuClick,
            )
        }
    }
}

@Composable
private fun FrostedHeaderButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val appearancePalette = LocalTempoPalette.current
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(GlassFrostMedium)
            .border(0.8.dp, appearancePalette.border.copy(alpha = 0.45f), CircleShape)
            .premiumClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = appearancePalette.onSurface,
            modifier = Modifier.size(18.dp),
        )
    }
}

// ──────────────────────────────────────────────────────────────
// 1. Hero Stage
// ──────────────────────────────────────────────────────────────

