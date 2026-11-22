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
fun DiscoveryCountPage(page: SpotlightStoryPage.DiscoveryCount) {
    val skipEntryMotion = skipStoryEntryMotion()
    val artistsAnim = remember { Animatable(0f) }
    val tracksAnim  = remember { Animatable(0f) }

    LaunchedEffect(page.uniqueArtists, skipEntryMotion) {
        if (skipEntryMotion) {
            artistsAnim.snapTo(page.uniqueArtists.toFloat())
        } else {
            delay(500)
            artistsAnim.animateTo(page.uniqueArtists.toFloat(), tween(1400, easing = FastOutSlowInEasing))
        }
    }
    LaunchedEffect(page.uniqueTracks, skipEntryMotion) {
        if (skipEntryMotion) {
            tracksAnim.snapTo(page.uniqueTracks.toFloat())
        } else {
            delay(650)
            tracksAnim.animateTo(page.uniqueTracks.toFloat(), tween(1600, easing = FastOutSlowInEasing))
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val dimens = rememberSpotlightDimens(maxHeight)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = dimens.screenTopPadding,
                    start = dimens.horizontalPadding,
                    end = dimens.horizontalPadding,
                    bottom = dimens.screenBottomPadding
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            EnterAnimation(delay = 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null,
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(20.dp * dimens.scale)
                    )
                    Spacer(modifier = Modifier.width(6.dp * dimens.scale))
                    Text(
                        text = stringResource(R.string.spotlight_music_universe),
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = dimens.textTitle),
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            // Main counters — two large cards
            EnterAnimation(delay = 300) {
                val targetArtistsText = String.format(java.util.Locale.US, "%,d", page.uniqueArtists)
                val targetTracksText = String.format(java.util.Locale.US, "%,d", page.uniqueTracks)
                val maxLen = maxOf(targetArtistsText.length, targetTracksText.length)
                val sharedFontSize = when {
                    maxLen > 5 -> (dimens.textDisplay.value * 0.42f).sp
                    maxLen > 4 -> (dimens.textDisplay.value * 0.48f).sp
                    maxLen > 3 -> (dimens.textDisplay.value * 0.55f).sp
                    else -> (dimens.textDisplay.value * 0.6f).sp
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp * dimens.scale)
                ) {
                    // Artists
                    SolidCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(dimens.cardCornerRadius),
                        backgroundColor = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                        borderColor = Color(0xFF8B5CF6).copy(alpha = 0.3f),
                        contentPadding = PaddingValues(vertical = 20.dp * dimens.scale, horizontal = 8.dp * dimens.scale)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val artistsText = String.format(java.util.Locale.US, "%,d", artistsAnim.value.toInt())
                            Text(
                                text = artistsText,
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontSize = sharedFontSize,
                                    fontWeight = FontWeight.Black
                                ),
                                color = Color(0xFFC4B5FD),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp * dimens.scale))
                            Text(
                                text = stringResource(R.string.spotlight_artists),
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = (dimens.textBody.value * 0.85f).sp),
                                color = Color.White.copy(alpha = 0.75f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Tracks
                    SolidCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(dimens.cardCornerRadius),
                        backgroundColor = Color(0xFF06B6D4).copy(alpha = 0.12f),
                        borderColor = Color(0xFF06B6D4).copy(alpha = 0.3f),
                        contentPadding = PaddingValues(vertical = 20.dp * dimens.scale, horizontal = 8.dp * dimens.scale)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val tracksText = String.format(java.util.Locale.US, "%,d", tracksAnim.value.toInt())
                            Text(
                                text = tracksText,
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontSize = sharedFontSize,
                                    fontWeight = FontWeight.Black
                                ),
                                color = Color(0xFF67E8F9),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp * dimens.scale))
                            Text(
                                text = stringResource(R.string.spotlight_unique_songs),
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = (dimens.textBody.value * 0.85f).sp),
                                color = Color.White.copy(alpha = 0.75f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Conversational tagline
            EnterAnimation(delay = 600) {
                Text(
                    text = page.conversationalText,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = dimens.textBody),
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // New discoveries chip (bottom)
            EnterAnimation(delay = 900) {
                if (page.newArtistsThisPeriod > 0) {
                    GlassCard(
                        modifier = Modifier.wrapContentSize(),
                        shape = RoundedCornerShape(50),
                        backgroundColor = Color(0xFF34D399).copy(alpha = 0.15f),
                        contentPadding = PaddingValues(horizontal = 18.dp * dimens.scale, vertical = 8.dp * dimens.scale)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(14.dp * dimens.scale)
                            )
                            Spacer(modifier = Modifier.width(6.dp * dimens.scale))
                            Text(
                                text = stringResource(R.string.spotlight_new_artists_chip, page.newArtistsThisPeriod, page.timeRangeLabel),
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = (dimens.textBody.value * 0.85f).sp),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34D399)
                            )
                        }
                    }
                } else {
                    // Spacer placeholder so layout stays consistent
                    Spacer(modifier = Modifier.height(36.dp * dimens.scale))
                }
            }
            
            // Comparative surprise callout
            if (page.comparativeText != null) {
                EnterAnimation(delay = 1100) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0xFF06B6D4).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(50)
                            )
                            .border(1.dp * dimens.scale, Color(0xFF22D3EE).copy(alpha = 0.4f), RoundedCornerShape(50))
                            .padding(horizontal = 20.dp * dimens.scale, vertical = 10.dp * dimens.scale)
                    ) {
                        Text(
                            text = page.comparativeText,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = (dimens.textBody.value * 0.9f).sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF67E8F9),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeekdayVsWeekendPage(page: SpotlightStoryPage.WeekdayVsWeekend) {
    val skipEntryMotion = skipStoryEntryMotion()
    val barAnims = remember { List(7) { Animatable(0f) } }
    LaunchedEffect(skipEntryMotion) {
        if (skipEntryMotion) {
            barAnims.forEachIndexed { index, anim ->
                anim.snapTo((page.dailyIntensity.getOrNull(index) ?: 0) / 100f)
            }
        } else {
            delay(400)
            barAnims.forEachIndexed { index, anim ->
                val delayMs = 400L + (index * 80).toLong()
                anim.snapTo(0f)
                delay(delayMs - 400L)
                anim.animateTo(
                    targetValue = (page.dailyIntensity.getOrNull(index) ?: 0) / 100f,
                    animationSpec = tween(700, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    val isWeekendDominant = page.dominantSide == "weekend"
    val accentColor = if (isWeekendDominant) Color(0xFFF59E0B) else Color(0xFF60A5FA)
    val dayNamesRes = listOf(
        R.string.spotlight_day_mon, R.string.spotlight_day_tue, R.string.spotlight_day_wed,
        R.string.spotlight_day_thu, R.string.spotlight_day_fri, R.string.spotlight_day_sat,
        R.string.spotlight_day_sun
    )
    val isWeekend = listOf(false, false, false, false, false, true, true)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val dimens = rememberSpotlightDimens(maxHeight)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = dimens.screenTopPadding,
                    start = dimens.horizontalPadding,
                    end = dimens.horizontalPadding,
                    bottom = dimens.screenBottomPadding
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            EnterAnimation(delay = 0) {
                Text(
                    text = stringResource(R.string.spotlight_week_vs_weekend),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = dimens.textTitle),
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            // VS comparison cards
            EnterAnimation(delay = 150) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp * dimens.scale)
                ) {
                    // Weekday card
                    SolidCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(dimens.cardCornerRadius),
                        backgroundColor = if (!isWeekendDominant) Color(0xFF60A5FA).copy(alpha = 0.12f)
                                         else Color.White.copy(alpha = 0.04f),
                        borderColor = if (!isWeekendDominant) Color(0xFF60A5FA).copy(alpha = 0.35f)
                                     else Color.White.copy(alpha = 0.12f),
                        contentPadding = PaddingValues(12.dp * dimens.scale)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${page.weekdayAvgMinutes}",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontSize = dimens.textHeadline,
                                    fontWeight = FontWeight.Black
                                ),
                                color = if (!isWeekendDominant) Color(0xFF93C5FD) else Color.White.copy(alpha = 0.65f),
                                maxLines = 1
                            )
                            Text(
                                text = stringResource(R.string.spotlight_min_per_day),
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = dimens.textLabel),
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(4.dp * dimens.scale))
                            Text(
                                text = stringResource(R.string.spotlight_weekdays),
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = (dimens.textBody.value * 0.85f).sp),
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp * dimens.scale))
                            Text(
                                text = page.weekdayLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = dimens.textLabel),
                                color = if (!isWeekendDominant) Color(0xFF93C5FD) else Color.Transparent,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Weekend card
                    SolidCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(dimens.cardCornerRadius),
                        backgroundColor = if (isWeekendDominant) Color(0xFFF59E0B).copy(alpha = 0.12f)
                                         else Color.White.copy(alpha = 0.04f),
                        borderColor = if (isWeekendDominant) Color(0xFFF59E0B).copy(alpha = 0.35f)
                                     else Color.White.copy(alpha = 0.12f),
                        contentPadding = PaddingValues(12.dp * dimens.scale)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${page.weekendAvgMinutes}",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontSize = dimens.textHeadline,
                                    fontWeight = FontWeight.Black
                                ),
                                color = if (isWeekendDominant) Color(0xFFFCD34D) else Color.White.copy(alpha = 0.65f),
                                maxLines = 1
                            )
                            Text(
                                text = stringResource(R.string.spotlight_min_per_day),
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = dimens.textLabel),
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(4.dp * dimens.scale))
                            Text(
                                text = stringResource(R.string.spotlight_weekends),
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = (dimens.textBody.value * 0.85f).sp),
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp * dimens.scale))
                            Text(
                                text = page.weekendLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = dimens.textLabel),
                                color = if (isWeekendDominant) Color(0xFFFCD34D) else Color.Transparent,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // 7-bar chart on a flat white card
            val barMaxHeight = remember(dimens.scale) { 100.dp * dimens.scale }
            EnterAnimation(delay = 400) {
                SolidCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(dimens.cardCornerRadius),
                    backgroundColor = Color.White,
                    borderColor = Color.Transparent,
                    contentPadding = PaddingValues(
                        horizontal = 12.dp * dimens.scale,
                        vertical = 14.dp * dimens.scale
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        dayNamesRes.forEachIndexed { i, dayRes ->
                            val isWknd = isWeekend[i]
                            val barColor = when {
                                isWknd && isWeekendDominant -> Color(0xFFF59E0B)
                                !isWknd && !isWeekendDominant -> Color(0xFF3B82F6)
                                isWknd -> Color(0xFFF59E0B).copy(alpha = 0.45f)
                                else -> Color(0xFF3B82F6).copy(alpha = 0.45f)
                            }
                            val intensity = barAnims[i].value.coerceAtLeast(0.04f)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.7f)
                                        .height(barMaxHeight * intensity)
                                        .clip(RoundedCornerShape(topStart = 6.dp * dimens.scale, topEnd = 6.dp * dimens.scale))
                                        .background(barColor)
                                )
                                Spacer(modifier = Modifier.height(6.dp * dimens.scale))
                                Text(
                                    text = stringResource(dayRes),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = dimens.textLabel),
                                    fontWeight = FontWeight.Medium,
                                    color = if (isWknd) Color(0xFFB45309)
                                            else Color(0xFF64748B),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Tagline
            EnterAnimation(delay = 700) {
                Text(
                    text = page.conversationalText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = dimens.textBody),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

