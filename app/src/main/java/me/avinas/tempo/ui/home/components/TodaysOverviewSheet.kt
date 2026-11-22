@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.avinas.tempo.ui.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.palette.graphics.Palette
import coil3.BitmapImage
import coil3.imageLoader
import coil3.request.ImageRequest
import me.avinas.tempo.data.stats.HourlyDistribution
import me.avinas.tempo.data.stats.ListeningOverview
import me.avinas.tempo.data.stats.PeriodComparison
import me.avinas.tempo.data.stats.TimeRange
import me.avinas.tempo.data.stats.TopArtist
import me.avinas.tempo.data.stats.TopTrack
import me.avinas.tempo.ui.appearance.LocalAppearanceRuntimePolicy
import me.avinas.tempo.ui.components.ArtAtmosphereLayer
import me.avinas.tempo.ui.components.CachedAsyncImage
import me.avinas.tempo.ui.components.FrostedIconButton
import me.avinas.tempo.ui.components.GlassCard
import me.avinas.tempo.ui.components.GlassCardVariant
import me.avinas.tempo.ui.components.SectionCatalogKicker
import me.avinas.tempo.ui.details.conditionedAccent
import me.avinas.tempo.ui.profile.readableProfileColor
import me.avinas.tempo.ui.stats.StatsShareDialog
import me.avinas.tempo.ui.stats.StatsTab
import me.avinas.tempo.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun todayMutedText(): Color {
    val palette = LocalTempoPalette.current
    val surface = if (palette.isDark) palette.elevatedSurface else palette.surfaceSunken
    return readableProfileColor(palette.mutedText, surface, 4.5f)
}

/* Rank medal accents — shared by podium rows on the overview. */
internal val RankGold = Color(0xFFFFD479)
internal val RankSilver = Color(0xFFC7CCD4)
internal val RankBronze = Color(0xFFCD8B5A)

/**
 * Staggered content entrance: each section fades and lifts in smoothly once
 * the sheet has settled.
 */
@Composable
internal fun EntranceReveal(@Suppress("UNUSED_PARAMETER") index: Int, content: @Composable () -> Unit) {
    // Avoid holding back real content for hundreds of milliseconds by default.
    // Optional list entrances are already gated for motion, battery and thermal state.
    if (!LocalAppearanceRuntimePolicy.current.allowListEntranceMotion) {
        content()
        return
    }
    val appear = remember { Animatable(0f) }
    val effectSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    LaunchedEffect(Unit) { appear.animateTo(1f, effectSpec) }
    Box(modifier = Modifier.graphicsLayer { alpha = appear.value }) { content() }
}

/**
 * Fullscreen daily listening summary overlay.
 * Displays today's top track, listening time comparison, hourly rhythm chart,
 * and rankings for top tracks and artists.
 */
@Composable
fun TodaysOverviewOverlay(
    overview: ListeningOverview?,
    topTracks: List<TopTrack>,
    topArtists: List<TopArtist>,
    hourlyDistribution: List<HourlyDistribution>,
    periodComparison: PeriodComparison?,
    onDismiss: () -> Unit,
    onViewAllStats: () -> Unit,
    onNavigateToTrack: (Long) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    val opacity = remember { Animatable(0f) }
    val reduceMotion = LocalAppearanceRuntimePolicy.current.reduceMotion
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val effectsSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    var isClosing by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }

    // Accent color extracted from top track album art or top artist image
    val topArtUrl = remember(topTracks, topArtists) {
        topTracks.firstOrNull()?.albumArtUrl?.takeIf { it.isNotBlank() }
            ?: topArtists.firstOrNull()?.imageUrl?.takeIf { it.isNotBlank() }
    }
    val palette = LocalTempoPalette.current
    val artworkAccent = rememberTodayAccentColor(topArtUrl)
    val contrastSurface = if (palette.isDark) palette.elevatedSurface else palette.surfaceSunken
    val accent = readableProfileColor(artworkAccent, contrastSurface, 4.5f)
    val scrollState = rememberScrollState()

    val dateLabel = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d", Locale.US))
    }
    val shortDateLabel = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d", Locale.US)).uppercase()
    }

    // Scroll-linked collapsing top bar header.
    // derivedStateOf keeps the per-pixel scroll read out of composition: the
    // boolean only invalidates this scope when it actually flips past 120.
    val showCollapsedTitle by remember {
        derivedStateOf { scrollState.value > 120 }
    }
    val headerScrimAlpha by animateFloatAsState(
        targetValue = if (showCollapsedTitle) 1f else 0f,
        animationSpec = if (reduceMotion) snap() else effectsSpec,
        label = "overviewHeaderScrim"
    )

    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            progress.snapTo(1f)
            opacity.snapTo(1f)
        } else {
            coroutineScope {
                launch { progress.animateTo(1f, spatialSpec) }
                launch { opacity.animateTo(1f, effectsSpec) }
            }
        }
    }

    LaunchedEffect(isClosing, reduceMotion) {
        if (isClosing) {
            if (reduceMotion) {
                progress.snapTo(0f)
                opacity.snapTo(0f)
            } else {
                coroutineScope {
                    launch { progress.animateTo(0f, spatialSpec) }
                    launch { opacity.animateTo(0f, effectsSpec) }
                }
            }
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = { if (!isClosing) isClosing = true },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            // Backdrop scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = opacity.value }
                    .background(LocalTempoPalette.current.background.copy(alpha = 0.94f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { if (!isClosing) isClosing = true }
            )

            // Sheet content container with smooth slide-up
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = (1f - progress.value) * size.height
                        alpha = opacity.value
                    }
                    .background(LocalTempoPalette.current.background)
            ) {
                // Ambient album art background layer tinted with extracted accent
                ArtAtmosphereLayer(
                    artUrl = topArtUrl,
                    tint = accent
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.background.copy(alpha = 0.90f))
                )

                // Top gradient scrim for scroll readability
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(130.dp)
                        .graphicsLayer { alpha = headerScrimAlpha }
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    LocalTempoPalette.current.background.copy(alpha = 0.95f),
                                    LocalTempoPalette.current.background.copy(alpha = 0.65f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Bottom gradient scrim
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.20f)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, LocalTempoPalette.current.background.copy(alpha = 0.88f))
                            )
                        )
                )

                // Main scrollable content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp)
                        .navigationBarsPadding()
                        .padding(
                            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 58.dp,
                            bottom = 32.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    var sectionIndex = 0

                    // 1. Daily Dispatch Header
                    EntranceReveal(sectionIndex++) {
                        TodaysEditorialHeader(
                            shortDateLabel = shortDateLabel,
                            accent = accent
                        )
                    }

                    // 2. Today's Anthem / Daily Spotlight Hero
                    val topTrack = topTracks.firstOrNull()
                    if (topTrack != null) {
                        EntranceReveal(sectionIndex++) {
                            TodaysAnthemHeroCard(
                                track = topTrack,
                                accent = accent,
                                onTrackClick = { onNavigateToTrack(topTrack.trackId) }
                            )
                        }
                    }

                    // 3. Master Stats Masthead
                    if (overview != null) {
                        EntranceReveal(sectionIndex++) {
                            HeroListeningCard(
                                overview = overview,
                                periodComparison = periodComparison,
                                accent = accent
                            )
                        }
                    }

                    // 4. Section 01: Listening Rhythm & Hourly Cadence
                    if (hourlyDistribution.isNotEmpty()) {
                        EntranceReveal(sectionIndex++) {
                            PeakHoursCard(
                                hourlyDistribution = hourlyDistribution,
                                accent = accent
                            )
                        }
                    }

                    // 5. Section 02: Top Tracks
                    if (topTracks.isNotEmpty()) {
                        EntranceReveal(sectionIndex++) {
                            TopTracksSection(
                                tracks = topTracks,
                                accent = accent,
                                onTrackClick = { track -> onNavigateToTrack(track.trackId) }
                            )
                        }
                    }

                    // 6. Section 03: Top Artists
                    if (topArtists.isNotEmpty()) {
                        EntranceReveal(sectionIndex++) {
                            TopArtistsSection(
                                artists = topArtists,
                                accent = accent,
                                onArtistClick = { artist ->
                                    val id = artist.artistId
                                    if (id != null && id > 0) {
                                        onNavigateToArtist("id:$id")
                                    } else {
                                        onNavigateToArtist(artist.artist)
                                    }
                                }
                            )
                        }
                    }

                    // 7. Footer Actions & Editorial Signature
                    EntranceReveal(sectionIndex++) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            FooterActions(
                                accent = accent,
                                onClose = { if (!isClosing) isClosing = true },
                                onViewAllStats = { if (!isClosing) onViewAllStats() }
                            )
                            TodaysOverviewFooter()
                        }
                    }
                }

                // Fixed Frosted Top Bar Navigation with Collapsing Title
                TodaysTopBarNavigation(
                    showCollapsedTitle = showCollapsedTitle,
                    dateLabel = dateLabel,
                    accent = accent,
                    onClose = { if (!isClosing) isClosing = true },
                    onShare = { showShareDialog = true }
                )
            }
        }

        // Stats Share Dialog for Today's Overview
        if (showShareDialog) {
            StatsShareDialog(
                tab = if (topTracks.isNotEmpty()) StatsTab.TOP_SONGS else StatsTab.TOP_ARTISTS,
                timeRange = TimeRange.TODAY,
                items = if (topTracks.isNotEmpty()) topTracks else topArtists,
                totals = overview?.totals,
                onDismiss = { showShareDialog = false }
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Top Bar Navigation with Collapsing Title & Frosted Controls
// ──────────────────────────────────────────────────────────────

@Composable
private fun TodaysTopBarNavigation(
    showCollapsedTitle: Boolean,
    dateLabel: String,
    accent: Color,
    onClose: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FrostedIconButton(
            icon = Icons.Default.Close,
            contentDescription = "Close today's overview",
            onClick = onClose,
            iconTint = LocalTempoPalette.current.onSurface
        )

        AnimatedVisibility(
            visible = showCollapsedTitle,
            enter = fadeIn(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()) + slideInVertically(
                animationSpec = MaterialTheme.motionScheme.fastSpatialSpec<androidx.compose.ui.unit.IntOffset>()
            ) { -10 },
            exit = fadeOut(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()) + slideOutVertically(
                animationSpec = MaterialTheme.motionScheme.fastSpatialSpec<androidx.compose.ui.unit.IntOffset>()
            ) { -10 },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "TODAY · $dateLabel".uppercase(Locale.getDefault()),
                    style = KickerSmall,
                    fontSize = 9.5.sp,
                    letterSpacing = 1.2.sp,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Daily Listening",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = LocalTempoPalette.current.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }

        FrostedIconButton(
            icon = Icons.Default.Share,
            contentDescription = "Share today's listening",
            onClick = onShare,
            iconTint = LocalTempoPalette.current.onSurface
        )
    }
}

// ──────────────────────────────────────────────────────────────
// 1. Editorial Header (In-Scroll Hero Dispatch)
// ──────────────────────────────────────────────────────────────

@Composable
private fun TodaysEditorialHeader(
    shortDateLabel: String,
    accent: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Text(
                text = "TODAY · $shortDateLabel",
                style = KickerSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                color = accent
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Daily Listening",
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 36.sp,
            letterSpacing = (-0.5).sp,
            color = LocalTempoPalette.current.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(0.8.dp)
                .background(GlassBorderMedium)
        )
    }
}

// ──────────────────────────────────────────────────────────────
// 2. Today's Anthem / Daily Spotlight Hero Stage
// ──────────────────────────────────────────────────────────────

@Composable
private fun TodaysAnthemHeroCard(
    track: TopTrack,
    accent: Color,
    onTrackClick: () -> Unit
) {
    val glowBrush = remember(accent) {
        Brush.radialGradient(
            colors = listOf(
                accent.copy(alpha = 0.20f),
                Color.Transparent
            )
        )
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .premiumClickable(onClick = onTrackClick),
        shape = RoundedCornerShape(22.dp),
        variant = GlassCardVariant.QuietGlass,
        accentColor = accent,
        borderColor = GlassBorderSoft,
        borderWidth = 0.8.dp,
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Artwork with radial glow halo & multi-layer shadow
            Box(
                modifier = Modifier.size(86.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(brush = glowBrush, shape = RoundedCornerShape(20.dp))
                )

                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .shadow(
                            elevation = 14.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = GlassShadowTeal,
                            spotColor = accent.copy(alpha = 0.25f)
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(TempoDarkSurfaceSunken)
                        .border(1.dp, GlassBorderStrong, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    CachedAsyncImage(
                        imageUrl = track.albumArtUrl,
                        contentDescription = track.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        targetSizeDp = 150,
                        placeholder = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(accent.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = accent.copy(alpha = 0.7f),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    )
                }
            }

            // Info column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                    Text(
                        text = "TODAY'S ANTHEM",
                        style = KickerSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                        color = accent
                    )
                }

                Text(
                    text = track.title,
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    lineHeight = 21.sp,
                    color = LocalTempoPalette.current.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.5.sp,
                    color = todayMutedText(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(0.8.dp)
                        .background(GlassBorderMedium)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${track.playCount} ${if (track.playCount == 1) "play" else "plays"} today · ${formatTrackDuration(track.totalTimeMs)}",
                    style = KickerSmall,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.6.sp,
                    color = todayMutedText(),
                    maxLines = 1
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
                tint = todayMutedText(),
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────
// 3. Master Stats Masthead
// ──────────────────────────────────────────────────────────────

@Composable
private fun HeroListeningCard(
    overview: ListeningOverview,
    periodComparison: PeriodComparison?,
    accent: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Hero block: accent dot kicker over display-size numeral
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(accent)
                        )
                        Text(
                            text = "TOTAL LISTENING TIME",
                            style = KickerSmall,
                            color = todayMutedText()
                        )
                    }

                    DayOverDayChip(periodComparison = periodComparison, accent = accent)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = me.avinas.tempo.ui.stats.displayStatsDuration(overview.totalListeningTimeMs),
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 44.sp,
                    lineHeight = 48.sp,
                    letterSpacing = (-1).sp,
                    color = LocalTempoPalette.current.onSurface,
                    maxLines = 1,
                    softWrap = false
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${overview.totalPlayCount} ${if (overview.totalPlayCount == 1) "play" else "plays"} recorded across today",
                    style = MaterialTheme.typography.bodySmall,
                    color = todayMutedText(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Hairline divider separating hero from secondary metrics
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.8.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // Secondary metrics — stat columns split by vertical hairlines
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MastheadSecondaryStat(
                    label = "TRACKS",
                    value = "${overview.uniqueTracksCount}",
                    subtext = "${overview.uniqueAlbumsCount} ${if (overview.uniqueAlbumsCount == 1) "album" else "albums"}",
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(0.8.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                MastheadSecondaryStat(
                    label = "ARTISTS",
                    value = "${overview.uniqueArtistsCount}",
                    subtext = "unique",
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(0.8.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                MastheadSecondaryStat(
                    label = "AVG SESSION",
                    value = me.avinas.tempo.ui.stats.displayStatsDuration(overview.averageSessionDurationMs),
                    subtext = "per session",
                    modifier = Modifier.weight(1.35f),
                    valueColor = accent
                )
            }

            if (periodComparison != null) {
                // Hairline divider separating metrics from the day-over-day bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.8.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    TodayVsYesterdayBar(
                        periodComparison = periodComparison,
                        accent = accent
                    )
                }
            }
        }
    }
}

