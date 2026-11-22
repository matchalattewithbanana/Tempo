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
fun ArtistHeroSection(
    artistDetails: ArtistDetails,
    accent: Color = LocalTempoPalette.current.accent,
    onRefreshImage: () -> Unit,
    isRefreshingImage: Boolean = false,
    onShowRenameDialog: () -> Unit = {},
    onNamePositioned: (Float) -> Unit = {},
) {
    val appearancePalette = LocalTempoPalette.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Soft accent halo — ties the extracted tint to the hero and gives
        // the portrait depth beyond its border.
        val glowBrush = remember(accent) {
            Brush.radialGradient(
                colors = listOf(
                    accent.copy(alpha = 0.20f),
                    accent.copy(alpha = 0.0f),
                )
            )
        }

        Box(
            modifier = Modifier.size(248.dp).tempoTiltSurface(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = glowBrush, shape = CircleShape)
            )

            Box(
                modifier = Modifier.size(232.dp),
                contentAlignment = Alignment.Center,
            ) {
                val imageUrl = artistDetails.artist.imageUrl
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(
                            elevation = 22.dp,
                            shape = CircleShape,
                            ambientColor = appearancePalette.surfaceSunken.copy(alpha = 0.30f),
                            spotColor = accent.copy(alpha = 0.22f),
                        )
                        .clip(CircleShape)
                        .background(appearancePalette.surfaceSunken)
                        .border(1.dp, appearancePalette.border, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (imageUrl.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(accent.copy(alpha = 0.45f), accent.copy(alpha = 0.20f))
                                    )
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = artistDetails.artist.name.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontFamily = DisplayFontFamily,
                                ),
                                fontWeight = FontWeight.Bold,
                                color = appearancePalette.onSurface,
                            )
                        }
                    } else {
                        CachedAsyncImage(
                            imageUrl = imageUrl,
                            contentDescription = artistDetails.artist.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            targetSizeDp = 232,
                        )
                    }
                }

                // Refresh image control (bottom-right overlay on the image)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp)
                        .size(36.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = CircleShape,
                            ambientColor = Color.Black.copy(alpha = 0.45f),
                            spotColor = Color.Black.copy(alpha = 0.55f),
                        )
                        .clip(CircleShape)
                        .background(appearancePalette.elevatedSurface.copy(alpha = 0.90f))
                        .border(1.dp, appearancePalette.border, CircleShape)
                        .premiumClickable(onClick = onRefreshImage),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isRefreshingImage) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = accent,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.details_refresh_artist_image),
                            tint = appearancePalette.onSurface,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Name and Rename
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .onGloballyPositioned { coordinates ->
                    onNamePositioned(coordinates.boundsInWindow().top)
                },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = artistDetails.artist.name,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = DisplayFontFamily,
                    letterSpacing = (-0.5).sp,
                ),
                fontWeight = FontWeight.Bold,
                color = appearancePalette.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )

            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(GlassFrostSoft)
                    .border(0.6.dp, appearancePalette.border.copy(alpha = 0.45f), CircleShape)
                    .premiumClickable(onClick = onShowRenameDialog),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.details_rename_artist),
                    tint = appearancePalette.mutedText,
                    modifier = Modifier.size(15.dp),
                )
            }
        }

        // Release-style metadata: hairline divider over a kicker meta line.
        val country = artistDetails.country
        val genresList = artistDetails.topGenres.ifEmpty { artistDetails.artist.genres }
        val metaParts = listOfNotNull(
            country?.takeIf { it.isNotBlank() },
            genresList.take(2)
                .takeIf { it.isNotEmpty() }
                ?.joinToString(", ") { it.replaceFirstChar { c -> c.uppercase() } },
        )
        if (metaParts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(0.8.dp)
                        .background(appearancePalette.border.copy(alpha = 0.65f)),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = metaParts.joinToString("  ·  ").uppercase(Locale.getDefault()),
                    style = KickerSmall,
                    color = appearancePalette.mutedText,
                    letterSpacing = 1.4.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(0.8.dp)
                        .background(appearancePalette.border.copy(alpha = 0.65f)),
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
    }
}

// ──────────────────────────────────────────────────────────────
// 2. Master Stats Masthead
// ──────────────────────────────────────────────────────────────

@Composable
fun ArtistStatMasthead(artistDetails: ArtistDetails, accent: Color = LocalTempoPalette.current.accent) {
    val appearancePalette = LocalTempoPalette.current
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        variant = GlassCardVariant.Obsidian,
        borderColor = appearancePalette.border.copy(alpha = 0.45f),
        borderWidth = 0.8.dp,
        contentPadding = PaddingValues(0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 20.dp)) {
            // Total play count hero metric — same anatomy as the song masthead.
            MastheadHeroMetric(
                label = stringResource(R.string.details_total_plays),
                value = String.format(Locale.getDefault(), "%,d", artistDetails.personalPlayCount),
                suffix = stringResource(
                    R.string.details_together_suffix,
                    me.avinas.tempo.ui.stats.displayStatsDuration(artistDetails.personalTotalTimeMs),
                ),
                accentTint = accent,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Secondary metrics: unique songs and albums explored
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MastheadSecondaryStat(
                    label = stringResource(R.string.details_unique_tracks),
                    value = formatCount(artistDetails.uniqueTracksPlayed.toLong()),
                    subtext = stringResource(R.string.artist_explored_sub),
                    modifier = Modifier.weight(1f),
                )

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(0.8.dp)
                        .background(appearancePalette.border.copy(alpha = 0.45f)),
                )

                MastheadSecondaryStat(
                    label = stringResource(R.string.details_unique_albums),
                    value = formatCount(artistDetails.uniqueAlbumsPlayed.toLong()),
                    subtext = stringResource(R.string.artist_explored_sub),
                    modifier = Modifier.weight(1f).padding(start = 16.dp),
                )
            }
        }
    }
}


// ──────────────────────────────────────────────────────────────
// 3. Fan Standing
// ──────────────────────────────────────────────────────────────

@Composable
fun FanStandingCard(
    playCount: Int,
    percentile: Double? = null,
    accent: Color = LocalTempoPalette.current.accent,
) {
    val tier = if (percentile != null) {
        when {
            percentile <= 1.0 -> FanStandingTierStyle(
                status = stringResource(R.string.fan_status_top_1),
                description = stringResource(R.string.fan_status_top_1_desc),
                icon = Icons.Rounded.AutoAwesome,
                tint = Color(0xFFB45309),
            )
            percentile <= 5.0 -> FanStandingTierStyle(
                status = stringResource(R.string.fan_status_top_5),
                description = stringResource(R.string.fan_status_top_5_desc),
                icon = Icons.Rounded.AutoAwesome,
                tint = Color(0xFFEA580C),
            )
            percentile <= 10.0 -> FanStandingTierStyle(
                status = stringResource(R.string.fan_status_top_10),
                description = stringResource(R.string.fan_status_top_10_desc),
                icon = Icons.Rounded.LocalFireDepartment,
                tint = Color(0xFFE11D48),
            )
            percentile <= 25.0 -> FanStandingTierStyle(
                status = stringResource(R.string.fan_status_top_25),
                description = stringResource(R.string.fan_status_top_25_desc),
                icon = Icons.Rounded.Headphones,
                tint = Color(0xFF0284C7),
            )
            percentile <= 50.0 -> FanStandingTierStyle(
                status = stringResource(R.string.fan_status_top_50),
                description = stringResource(R.string.fan_status_top_50_desc),
                icon = Icons.Rounded.MusicNote,
                tint = Color(0xFF0D9488),
            )
            else -> FanStandingTierStyle(
                status = stringResource(R.string.fan_status_listener),
                description = stringResource(R.string.fan_status_listener_desc),
                icon = Icons.Rounded.MusicNote,
                tint = Color(0xFF475569),
            )
        }
    } else {
        when {
            playCount > 1000 -> FanStandingTierStyle(
                status = stringResource(R.string.fan_status_ultimate),
                description = stringResource(R.string.fan_status_ultimate_desc),
                icon = Icons.Rounded.AutoAwesome,
                tint = Color(0xFFB45309),
            )
            playCount > 500 -> FanStandingTierStyle(
                status = stringResource(R.string.fan_status_super),
                description = stringResource(R.string.fan_status_super_desc),
                icon = Icons.Rounded.AutoAwesome,
                tint = Color(0xFFEA580C),
            )
            playCount > 200 -> FanStandingTierStyle(
                status = stringResource(R.string.fan_status_big),
                description = stringResource(R.string.fan_status_big_desc),
                icon = Icons.Rounded.LocalFireDepartment,
                tint = Color(0xFFE11D48),
            )
            playCount > 50 -> FanStandingTierStyle(
                status = stringResource(R.string.fan_status_regular),
                description = stringResource(R.string.fan_status_regular_desc),
                icon = Icons.Rounded.Headphones,
                tint = Color(0xFF0284C7),
            )
            else -> FanStandingTierStyle(
                status = stringResource(R.string.fan_status_listener),
                description = stringResource(R.string.fan_status_listener_desc),
                icon = Icons.Rounded.MusicNote,
                tint = Color(0xFF475569),
            )
        }
    }

    val scheme = MaterialTheme.colorScheme
    val palette = LocalTempoPalette.current
    val container = scheme.surfaceContainerLow
    // Tier hues keep their meaning, but every text color and icon adapts
    // to the active Light/Dark/OLED/custom palette.
    val tierAccent = me.avinas.tempo.ui.appearance.AppearancePaletteResolver.readableAccent(
        palette, tier.tint, container,
    )
    val iconContainer = lerp(container, tierAccent, 0.13f)
    val iconForeground = me.avinas.tempo.ui.appearance.AppearancePaletteResolver.readableAccent(
        palette, tierAccent, iconContainer,
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = container,
        contentColor = scheme.onSurface,
        border = BorderStroke(1.dp, scheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconContainer)
                    .border(0.8.dp, scheme.outlineVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = tier.icon,
                    contentDescription = null,
                    tint = iconForeground,
                    modifier = Modifier.size(20.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tier.status.uppercase(Locale.getDefault()),
                    style = KickerSmall,
                    color = tierAccent,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tier.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

private data class FanStandingTierStyle(
    val status: String,
    val description: String,
    val icon: ImageVector,
    val tint: Color,
)

// ──────────────────────────────────────────────────────────────
// 4. Listening Journey & Milestones
// ──────────────────────────────────────────────────────────────

