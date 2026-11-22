package me.avinas.tempo.ui.spotlight

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridItemSpan

import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Balance
import kotlinx.coroutines.delay
import me.avinas.tempo.ui.components.CachedAsyncImage
import me.avinas.tempo.ui.components.LocalInCaptureContext
import androidx.compose.ui.platform.LocalContext
import me.avinas.tempo.data.stats.TimeRange
import me.avinas.tempo.ui.components.GlassCard
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import me.avinas.tempo.R
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.text.font.FontStyle

@Composable
fun ConclusionPage(page: SpotlightStoryPage.Conclusion) {
    val context = LocalContext.current
    val titleText = remember(page.timeRange) {
        SpotlightPoetry.getHeading(context, page.timeRange)
    }
    
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val dimens = rememberSpotlightDimens(maxHeight)
        
        // Warm gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF8B5CF6).copy(alpha = 0.08f),
                            Color.Transparent,
                            Color(0xFFEC4899).copy(alpha = 0.06f)
                        )
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = dimens.screenTopPadding,
                    start = dimens.horizontalPadding,
                    end = dimens.horizontalPadding,
                    bottom = dimens.screenBottomPadding
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header - Emotional callback
            EnterAnimation(delay = 0) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = (dimens.textHeadline.value * 1.1f).sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(dimens.spacerSmall))
                    
                    Text(
                        text = page.conversationalText,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = dimens.textBody,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        ),
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp * dimens.scale))
            
            // Total Listening - Hero stat
            EnterAnimation(delay = 200) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = String.format(java.util.Locale.US, "%,d", page.totalMinutes),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = (dimens.textDisplay.value * 0.8f).sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.spotlight_conclusion_min),
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = dimens.textLabel),
                        color = Color.White.copy(alpha = 0.7f),
                        letterSpacing = (1f * dimens.scale).sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp * dimens.scale))
            
            // Personality recap card
            EnterAnimation(delay = 400) {
                SolidCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(dimens.cardCornerRadius),
                    backgroundColor = Color(0xFF6D28D9),
                    borderColor = Color.Transparent,
                    contentPadding = PaddingValues(16.dp * dimens.scale)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val (pIcon, pColor) = getPersonalityAssets(page.personalityKey)
                        Icon(
                            imageVector = pIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp * dimens.scale)
                        )
                        Spacer(modifier = Modifier.width(12.dp * dimens.scale))
                        Column {
                            Text(
                                text = stringResource(R.string.spotlight_listening_personality),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = dimens.textLabel),
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = page.personalityType,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = (dimens.textBody.value * 1.1f).sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp * dimens.scale))
            
            // Top Artists & Songs - Two columns
            EnterAnimation(delay = 600) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp * dimens.scale)
                ) {
                    // Top Artists
                    SolidCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(dimens.cardCornerRadius),
                        backgroundColor = Color(0xFFEC4899).copy(alpha = 0.08f),
                        borderColor = Color(0xFFEC4899).copy(alpha = 0.2f),
                        contentPadding = PaddingValues(12.dp * dimens.scale)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(R.string.spotlight_conclusion_top_artists),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = dimens.textLabel),
                                color = Color(0xFFF9A8D4),
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp * dimens.scale))
                            
                            page.topArtists.take(5).forEach { artist ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 3.dp * dimens.scale)
                                ) {
                                    CachedAsyncImage(
                                        imageUrl = artist.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp * dimens.scale).clip(CircleShape),
                                        contentScale = ContentScale.Crop,
                                        allowHardware = false
                                    )
                                    Spacer(modifier = Modifier.width(6.dp * dimens.scale))
                                    Text(
                                        text = artist.name,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = (dimens.textLabel.value * 0.9f).sp),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Top Songs
                    SolidCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(dimens.cardCornerRadius),
                        backgroundColor = Color(0xFFF59E0B).copy(alpha = 0.08f),
                        borderColor = Color(0xFFF59E0B).copy(alpha = 0.2f),
                        contentPadding = PaddingValues(12.dp * dimens.scale)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(R.string.spotlight_conclusion_top_songs),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = dimens.textLabel),
                                color = Color(0xFFFCD34D),
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp * dimens.scale))
                            
                            page.topSongs.take(5).forEach { song ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 3.dp * dimens.scale)
                                ) {
                                    CachedAsyncImage(
                                        imageUrl = song.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp * dimens.scale).clip(RoundedCornerShape(3.dp * dimens.scale)),
                                        contentScale = ContentScale.Crop,
                                        allowHardware = false
                                    )
                                    Spacer(modifier = Modifier.width(6.dp * dimens.scale))
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = (dimens.textLabel.value * 0.9f).sp),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp * dimens.scale))
            
            // Closing message - Full circle moment
            EnterAnimation(delay = 800) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.3f)
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.3f))
                    )
                    Spacer(modifier = Modifier.height(16.dp * dimens.scale))
                    val closingResId = remember(page.timeRange) {
                        when (page.timeRange) {
                            me.avinas.tempo.data.stats.TimeRange.THIS_WEEK -> R.string.spotlight_conclusion_closing_week
                            me.avinas.tempo.data.stats.TimeRange.THIS_MONTH -> R.string.spotlight_conclusion_closing_month
                            me.avinas.tempo.data.stats.TimeRange.ALL_TIME -> R.string.spotlight_conclusion_closing_all_time
                            else -> R.string.spotlight_conclusion_closing
                        }
                    }
                    Text(
                        text = stringResource(closingResId),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = dimens.textBody,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            

        }
    }
}


// Maps stable English personality keys to icons and colors across locales
internal fun getPersonalityAssets(key: String): Pair<ImageVector, Color> {
    return when (key) {
        "Hip Hop Head"        -> Icons.Default.Mic to Color(0xFFF472B6)
        "Pop Icon"            -> Icons.Default.Star to Color(0xFFFBBF24)
        "Rock Star"           -> Icons.Default.Album to Color(0xFFEF4444)
        "Metalhead"           -> Icons.Default.FlashOn to Color(0xFFDC2626)
        "R&B Soul"            -> Icons.Default.Favorite to Color(0xFFEC4899)
        "Electronic Voyager"  -> Icons.Default.GraphicEq to Color(0xFF22D3EE)
        "Jazz Cat"            -> Icons.AutoMirrored.Filled.QueueMusic to Color(0xFFF59E0B)
        "Maestro"             -> Icons.Default.Piano to Color(0xFFA78BFA)
        "Indie Spirit"        -> Icons.Default.Explore to Color(0xFF34D399)
        "Party Starter"       -> Icons.Default.Celebration to Color(0xFFF472B6)
        "Intense Soul"        -> Icons.Default.Bolt to Color(0xFFEF4444)
        "Peaceful Optimist"   -> Icons.Default.Spa to Color(0xFF34D399)
        "Deep Thinker"        -> Icons.Default.SelfImprovement to Color(0xFF60A5FA)
        "Dance Floor Regular" -> Icons.Default.MusicNote to Color(0xFFF59E0B)
        "Balanced Enthusiast" -> Icons.Default.Balance to Color(0xFFA78BFA)
        "The Explorer"        -> Icons.Default.Explore to Color(0xFF22D3EE)
        "The Melophile"       -> Icons.Default.Psychology to Color(0xFFC4B5FD)
        else                  -> Icons.Default.Psychology to Color(0xFFC4B5FD)
    }
}

// ─────────────────────────────────────────────
// GAMIFICATION PAGES
// ─────────────────────────────────────────────

