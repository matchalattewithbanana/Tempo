@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.avinas.tempo.ui.details

import me.avinas.tempo.ui.effects.tempoAmbientBackdrop
import me.avinas.tempo.ui.effects.tempoTiltSurface
import me.avinas.tempo.ui.effects.tempoListMotion

/* Tempo · AlbumDetails · Editorial album listening profile.
 * Shares the SongDetails design language: art-as-atmosphere background,
 * collapsing header title, frosted top-bar actions, a conditioned
 * dominant-color accent and the Obsidian stat masthead — extended with
 * the album's track sheet and inline edit mode.
 */

import me.avinas.tempo.ui.components.ShareSnapshot
import me.avinas.tempo.ui.components.SnapshotShareDialog
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import me.avinas.tempo.R
import me.avinas.tempo.data.enrichment.MusicBrainzEnrichmentService
import me.avinas.tempo.data.local.entities.Track
import me.avinas.tempo.data.stats.AlbumDetails
import me.avinas.tempo.data.stats.TrackWithStats
import me.avinas.tempo.ui.components.AlbumArtImage
import me.avinas.tempo.ui.components.ArtAtmosphereLayer
import me.avinas.tempo.ui.components.DeepOceanBackground
import me.avinas.tempo.ui.components.GlassCard
import me.avinas.tempo.ui.components.GlassCardVariant
import me.avinas.tempo.ui.components.TempoDialogShape
import me.avinas.tempo.ui.theme.*
import me.avinas.tempo.ui.components.TempoDropdownMenu
import me.avinas.tempo.ui.components.TempoDropdownMenuItem
import me.avinas.tempo.ui.components.TempoDialogSurface
import me.avinas.tempo.ui.components.TempoDialogIcon
import me.avinas.tempo.ui.components.TempoDialogTitle
import me.avinas.tempo.ui.components.TempoDialogBody
import me.avinas.tempo.ui.components.TempoDialogDangerButton
import me.avinas.tempo.ui.components.TempoDialogSecondaryButton
import me.avinas.tempo.ui.components.TempoIcons
import java.util.Locale

@Composable
fun AlbumDetailsScreen(
    albumId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToSong: (Long) -> Unit,
    onNavigateToArtist: (Long) -> Unit = {},
    onNavigateToAlbum: (Long) -> Unit = {},
    viewModel: AlbumDetailsViewModel = hiltViewModel(),
) {
    val appearancePalette = LocalTempoPalette.current
    val uiState by viewModel.uiState.collectAsState()
    val albumDetails = uiState.albumDetails

    when {
        uiState.isLoading -> AlbumDetailsLoadingSkeleton()

        albumDetails != null -> AlbumDetailsContent(
            albumDetails = albumDetails,
            isEditMode = uiState.isEditMode,
            onNavigateBack = onNavigateBack,
            onNavigateToSong = onNavigateToSong,
            onNavigateToArtist = onNavigateToArtist,
            onNavigateToAlbum = onNavigateToAlbum,
            onToggleEdit = viewModel::toggleEditMode,
            onAddClick = viewModel::openAddDialog,
            onRemoveTrack = viewModel::requestRemove,
        )

        else -> Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Album,
                    contentDescription = null,
                    tint = TempoError.copy(alpha = 0.7f),
                    modifier = Modifier.size(36.dp),
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = uiState.error ?: stringResource(R.string.album_not_found),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedButton(
                    onClick = viewModel::refresh,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(0.8.dp, appearancePalette.border.copy(alpha = 0.65f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = appearancePalette.onSurface),
                ) {
                    Text(stringResource(R.string.common_retry).uppercase(Locale.getDefault()))
                }
            }
        }
    }

    if (uiState.addDialogVisible) {
        AddTrackToAlbumDialog(
            query = uiState.addQuery,
            results = uiState.addResults,
            isSearching = uiState.isSearching,
            artistName = albumDetails?.artistName.orEmpty(),
            onQueryChange = viewModel::onAddQueryChange,
            onTrackSelected = viewModel::addTrackToAlbum,
            onDismiss = viewModel::closeAddDialog,
        )
    }

    uiState.pendingRemove?.let { track ->
        RemoveFromAlbumDialog(
            trackTitle = track.track.title,
            onConfirm = viewModel::confirmRemove,
            onDismiss = viewModel::cancelRemove,
        )
    }
}

@Composable
private fun AlbumDetailsLoadingSkeleton() {
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
            SkeletonBlock(modifier = Modifier.size(180.dp, 22.dp), cornerRadius = 8.dp)
            Spacer(modifier = Modifier.height(12.dp))
            SkeletonBlock(modifier = Modifier.size(110.dp, 14.dp), cornerRadius = 7.dp)
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

@Composable
private fun AlbumDetailsContent(
    albumDetails: AlbumDetails,
    isEditMode: Boolean,
    onNavigateBack: () -> Unit,
    onNavigateToSong: (Long) -> Unit,
    onNavigateToArtist: (Long) -> Unit,
    onNavigateToAlbum: (Long) -> Unit,
    onToggleEdit: () -> Unit,
    onAddClick: () -> Unit,
    onRemoveTrack: (TrackWithStats) -> Unit,
) {
    val appearancePalette = LocalTempoPalette.current
    val dominantColor = rememberArtworkAccent(albumDetails.album.artworkUrl)
    var showMenu by remember { mutableStateOf(false) }
    var showMergeDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }

    // Collapsed header title appears when the hero title itself scrolls under
    // the top bar — anchored to its live position, not a scroll-pixel guess.
    val density = LocalDensity.current
    val headerBottomPx = with(density) {
        (WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 56.dp).toPx()
    }
    var showCollapsedTitle by remember { mutableStateOf(false) }
    val headerScrimAlpha by animateFloatAsState(
        targetValue = if (showCollapsedTitle) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>(),
        label = "albumHeaderScrim",
    )

    // The room recolors per album: blurred cover-art wash behind all content.
    // Falls through to the plain DeepOcean base when no art exists.
    val atmosphereArtUrl = albumDetails.album.artworkUrl
        ?.takeIf { it.isNotBlank() }
        ?.let { MusicBrainzEnrichmentService.fixHttpUrl(it) }

    Box(Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().tempoAmbientBackdrop()) {
            ArtAtmosphereLayer(
                artUrl = atmosphereArtUrl,
                tint = dominantColor,
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 60.dp,
                    bottom = 64.dp,
                ),
            ) {
                // 1. Hero Stage
                item(key = "hero_section") {
                    AlbumHeroEditorialStage(
                        albumDetails = albumDetails,
                        dominantColor = dominantColor,
                        onNavigateToArtist = onNavigateToArtist,
                        onTitlePositioned = { top ->
                            val collapsed = top <= headerBottomPx
                            if (collapsed != showCollapsedTitle) {
                                showCollapsedTitle = collapsed
                            }
                        },
                    )
                }

                // 2. High-Contrast Master Stats
                item(key = "master_stats_masthead") {
                    Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 6.dp)) {
                        AlbumStatMasthead(
                            albumDetails = albumDetails,
                            dominantColor = dominantColor,
                        )
                    }
                }

                // 3. Track Sheet — header, then one lazy item PER track row.
                //
                // The rows used to sit inside a single item(key = "track_sheet")
                // as an eager Column, which bypassed LazyColumn virtualization:
                // every row of a 50–200 track album/compilation composed and
                // measured on first layout and stayed composed while scrolled far
                // off-screen. Now only the visible window is materialised. Items
                // are keyed off the track id (never the index): index-baked keys
                // resolve to a different id when the list is mutated mid-scroll,
                // reuse a slot twice and throw the subcompose
                // IllegalArgumentException — see the same discipline in
                // HistoryScreen/StatsScreen.
                item(key = "track_sheet_header") {
                    Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp)) {
                        AlbumTrackSheetHeader(
                            trackCount = albumDetails.tracks.size,
                            isEditMode = isEditMode,
                            onAddClick = onAddClick,
                        )
                    }
                }

                if (albumDetails.tracks.isEmpty()) {
                    item(key = "track_sheet_empty") {
                        Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 6.dp)) {
                            AlbumEmptyTracksCard(tint = dominantColor)
                        }
                    }
                } else {
                    itemsIndexed(
                        items = albumDetails.tracks,
                        key = { _, item -> "track_${item.track.id}" },
                        contentType = { _, _ -> "album_track_row" },
                    ) { index, track ->
                        Box(Modifier.tempoListMotion(index)) {
                            AlbumTrackRow(
                                index = index,
                                track = track,
                                tint = dominantColor,
                                isEditMode = isEditMode,
                                isFirst = index == 0,
                                isLast = index == albumDetails.tracks.lastIndex,
                                onNavigateToSong = onNavigateToSong,
                            )
                        }
                    }
                }

                // 4. Footer
                item(key = "footer") {
                    Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 32.dp)) {
                        AlbumDetailsFooter()
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

            AlbumTopBar(
                showCollapsedTitle = showCollapsedTitle,
                title = albumDetails.album.title,
                artist = albumDetails.artistName,
                isEditMode = isEditMode,
                onNavigateBack = onNavigateBack,
                onToggleEdit = onToggleEdit,
                onAddClick = onAddClick,
                onMenuClick = { showMenu = true },
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 16.dp, top = 48.dp)
            ) {
                TempoDropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    TempoDropdownMenuItem(
                        title = stringResource(R.string.share_album),
                        leadingIcon = Icons.Filled.Share,
                        onClick = {
                            showMenu = false
                            showShareDialog = true
                        },
                    )
                    TempoDropdownMenuItem(
                        title = stringResource(R.string.details_merge_album),
                        leadingIcon = TempoIcons.MergeStreams,
                        onClick = {
                            showMenu = false
                            showMergeDialog = true
                        }
                    )
                }
            }

            if (showShareDialog) {
                val duration = if (albumDetails.totalTimeMs > 0) me.avinas.tempo.ui.stats.displayStatsDuration(albumDetails.totalTimeMs)
                    else stringResource(R.string.stats_period_zero_time)
                SnapshotShareDialog(
                    snapshot = ShareSnapshot(
                        title = albumDetails.album.title,
                        subtitle = albumDetails.artistName,
                        artUrl = atmosphereArtUrl,
                        metrics = listOf(
                            stringResource(R.string.stats_period_plays) to stringResource(R.string.stats_period_number, albumDetails.totalPlayCount),
                            stringResource(R.string.stats_period_listening_time) to duration,
                        ),
                        rows = albumDetails.tracks.sortedByDescending { it.playCount }.take(5).map { track ->
                            track.track.title to track.track.artist
                        },
                    ),
                    onDismiss = { showShareDialog = false },
                )
            }

            if (showMergeDialog) {
                AlbumMergeSearchDialog(
                    sourceAlbumId = albumDetails.album.id,
                    sourceAlbumTitle = albumDetails.album.title,
                    sourceArtistId = albumDetails.album.artistId,
                    sourceArtistName = albumDetails.artistName,
                    onDismiss = { showMergeDialog = false },
                    onMergeComplete = { targetAlbumId ->
                        showMergeDialog = false
                        onNavigateToAlbum(targetAlbumId)
                    }
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Top Bar with Collapsible Title & Edit Toggle
// ──────────────────────────────────────────────────────────────

@Composable
private fun AlbumTopBar(
    showCollapsedTitle: Boolean,
    title: String,
    artist: String,
    isEditMode: Boolean,
    onNavigateBack: () -> Unit,
    onToggleEdit: () -> Unit,
    onAddClick: () -> Unit,
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
        AlbumTopBarAction(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.details_action_back),
            onClick = onNavigateBack,
        )

        AnimatedVisibility(
            visible = showCollapsedTitle,
            enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec<Float>()) +
                    slideInVertically(MaterialTheme.motionScheme.fastSpatialSpec<androidx.compose.ui.unit.IntOffset>()) { -10 },
            exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec<Float>()) +
                    slideOutVertically(MaterialTheme.motionScheme.fastSpatialSpec<androidx.compose.ui.unit.IntOffset>()) { -10 },
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = appearancePalette.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = artist,
                    style = MaterialTheme.typography.labelSmall,
                    color = appearancePalette.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
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
            // ponytail: direct one-tap entry to Add Song; edit mode still gates
            // the inline pill, this just skips the two-step dance.
            AlbumTopBarAction(
                icon = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.album_add_song),
                onClick = onAddClick,
            )
            AlbumTopBarAction(
                icon = if (isEditMode) Icons.Rounded.Check else Icons.Rounded.Edit,
                contentDescription = stringResource(
                    if (isEditMode) R.string.album_action_done_cd else R.string.album_action_edit_cd
                ),
                onClick = onToggleEdit,
                iconTint = if (isEditMode) appearancePalette.accent else appearancePalette.onSurface,
                containerColor = if (isEditMode) appearancePalette.accent.copy(alpha = 0.16f) else GlassFrostMedium,
                borderColor = if (isEditMode) appearancePalette.accent.copy(alpha = 0.40f) else appearancePalette.border.copy(alpha = 0.45f),
            )
            AlbumTopBarAction(
                icon = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.details_action_song_options),
                onClick = onMenuClick,
            )
        }
    }
}

@Composable
private fun AlbumTopBarAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    iconTint: Color = LocalTempoPalette.current.onSurface,
    containerColor: Color = GlassFrostMedium,
    borderColor: Color = LocalTempoPalette.current.border.copy(alpha = 0.45f),
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(containerColor)
            .border(0.8.dp, borderColor, CircleShape)
            .premiumClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(18.dp),
        )
    }
}

// ──────────────────────────────────────────────────────────────
// 1. Hero Stage
// ──────────────────────────────────────────────────────────────

