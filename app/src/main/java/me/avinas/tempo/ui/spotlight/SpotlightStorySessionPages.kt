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
fun BingeSessionPage(page: SpotlightStoryPage.BingeSession) {
    val skipEntryMotion = skipStoryEntryMotion()
    val countAnim = remember { Animatable(0f) }
    val timeAnim  = remember { Animatable(0f) }

    LaunchedEffect(page.bingeCount, skipEntryMotion) {
        if (skipEntryMotion) {
            countAnim.snapTo(page.bingeCount.toFloat())
        } else {
            delay(600)
            countAnim.animateTo(page.bingeCount.toFloat(), tween(1200, easing = FastOutSlowInEasing))
        }
    }
    LaunchedEffect(page.totalBingeMinutes, skipEntryMotion) {
        if (skipEntryMotion) {
            timeAnim.snapTo(page.totalBingeMinutes.toFloat())
        } else {
            delay(800)
            timeAnim.animateTo(page.totalBingeMinutes.toFloat(), tween(1400, easing = FastOutSlowInEasing))
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
            EnterAnimation(delay = 0) {
                Text(
                    text = stringResource(R.string.spotlight_longest_binge),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = dimens.textTitle),
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            // Artist name — big reveal
            EnterAnimation(delay = 200) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = page.artistName,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = dimens.textHeadline,
                            fontWeight = FontWeight.Black,
                            shadow = Shadow(
                                color = Color(0xFFEC4899).copy(alpha = 0.6f),
                                offset = Offset(0f, 4f),
                                blurRadius = 20f
                            )
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp * dimens.scale))
                    SolidCard(
                        modifier = Modifier.wrapContentSize(),
                        shape = RoundedCornerShape(50),
                        backgroundColor = Color(0xFFEC4899).copy(alpha = 0.12f),
                        borderColor = Color(0xFFEC4899).copy(alpha = 0.3f),
                        contentPadding = PaddingValues(horizontal = 16.dp * dimens.scale, vertical = 6.dp * dimens.scale)
                    ) {
                        Text(
                            text = stringResource(R.string.spotlight_marathon_session),
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = dimens.textLabel),
                            color = Color(0xFFF9A8D4),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Stats row
            EnterAnimation(delay = 500) {
                val targetCountText = page.bingeCount.toString()
                val targetMins = page.totalBingeMinutes
                val targetH = targetMins / 60
                val targetM = targetMins % 60
                val worstTimeText = stringResource(R.string.spotlight_time_hours, targetH, 59)
                val targetTimeText = stringResource(R.string.spotlight_time_hours, targetH, targetM)
                val maxLen = maxOf(targetCountText.length, worstTimeText.length, targetTimeText.length)
                val statFontSize = when {
                    maxLen >= 8 -> (dimens.textDisplay.value * 0.38f).sp
                    maxLen >= 6 -> (dimens.textDisplay.value * 0.42f).sp
                    else -> (dimens.textDisplay.value * 0.5f).sp
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp * dimens.scale)
                ) {
                    SolidCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(dimens.cardCornerRadius),
                        backgroundColor = Color(0xFFEC4899).copy(alpha = 0.1f),
                        borderColor = Color(0xFFEC4899).copy(alpha = 0.3f),
                        contentPadding = PaddingValues(16.dp * dimens.scale)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = countAnim.value.toInt().toString(),
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontSize = statFontSize,
                                    fontWeight = FontWeight.Black
                                ),
                                color = Color(0xFFF9A8D4),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.spotlight_tracks_in_a_row),
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = dimens.textLabel),
                                color = Color.White.copy(alpha = 0.65f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    SolidCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(dimens.cardCornerRadius),
                        backgroundColor = Color(0xFF8B5CF6).copy(alpha = 0.1f),
                        borderColor = Color(0xFF8B5CF6).copy(alpha = 0.3f),
                        contentPadding = PaddingValues(16.dp * dimens.scale)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val timeText = me.avinas.tempo.ui.stats.displayStatsDuration(
                                timeAnim.value.toLong().coerceAtLeast(0L) * 60_000L
                            )
                            
                            Text(
                                text = timeText,
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontSize = statFontSize,
                                    fontWeight = FontWeight.Black
                                ),
                                color = Color(0xFFC4B5FD),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.spotlight_straight),
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = dimens.textLabel),
                                color = Color.White.copy(alpha = 0.65f)
                            )
                        }
                    }
                }
            }

            EnterAnimation(delay = 800) {
                Text(
                    text = page.conversationalText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = dimens.textBody),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TimeOfDayVibesPage(page: SpotlightStoryPage.TimeOfDayVibes) {
    // Vector icons instead of emoji: emoji render per-OEM and rasterize soft
    // in the shared image; vectors match the icon language used on every other page.
    data class Period(val labelRes: Int, val icon: ImageVector, val percent: Int, val color: Color, val bg: Color)
    val periods = listOf(
        Period(R.string.spotlight_period_morning,   Icons.Default.WbSunny,   page.morningPercent,   Color(0xFFFBBF24), Color(0xFFF59E0B)),
        Period(R.string.spotlight_period_afternoon, Icons.Default.LightMode, page.afternoonPercent, Color(0xFF34D399), Color(0xFF10B981)),
        Period(R.string.spotlight_period_evening,   Icons.Default.WbTwilight, page.eveningPercent,   Color(0xFF60A5FA), Color(0xFF3B82F6)),
        Period(R.string.spotlight_period_night,     Icons.Default.DarkMode,  page.nightPercent,     Color(0xFFA78BFA), Color(0xFF8B5CF6))
    )

    val totalPct = periods.sumOf { it.percent }.coerceAtLeast(1)

    val skipEntryMotion = skipStoryEntryMotion()
    // Each bar animates independently
    val anims = remember { periods.map { Animatable(0f) } }
    LaunchedEffect(skipEntryMotion) {
        if (skipEntryMotion) {
            anims.forEachIndexed { index, anim ->
                anim.snapTo(periods[index].percent / totalPct.toFloat())
            }
        } else {
            delay(400)
            anims.forEachIndexed { index, anim ->
                val delayMs = (index * 120).toLong()
                delay(if (index == 0) delayMs else 120L)
                anim.animateTo(
                    targetValue = periods[index].percent / totalPct.toFloat(),
                    animationSpec = tween(900, easing = FastOutSlowInEasing)
                )
            }
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
            EnterAnimation(delay = 0) {
                Text(
                    text = stringResource(R.string.spotlight_when_you_listen),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = dimens.textTitle),
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            // Dominant period big label
            EnterAnimation(delay = 150) {
                // dominantPeriod is a stable English key from the generator — match by
                // index, never against localized labels (broke in non-English locales).
                val dominantIndex = when (page.dominantPeriod.lowercase()) {
                    "morning" -> 0
                    "afternoon" -> 1
                    "evening" -> 2
                    "night" -> 3
                    else -> -1
                }
                val dominant = if (dominantIndex >= 0) periods[dominantIndex]
                    else periods.maxByOrNull { it.percent }!!
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = dominant.icon,
                        contentDescription = null,
                        tint = dominant.color,
                        modifier = Modifier.size((dimens.textDisplay.value * 0.42f).dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp * dimens.scale))
                    Text(
                        text = stringResource(dominant.labelRes),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = dimens.textHeadline,
                            fontWeight = FontWeight.Black,
                            shadow = Shadow(
                                color = dominant.bg.copy(alpha = 0.5f),
                                offset = Offset(0f, 3f),
                                blurRadius = 12f
                            )
                        ),
                        color = dominant.color,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.spotlight_listener),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = dimens.textBody),
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Horizontal share bars on a flat white card
            EnterAnimation(delay = 400) {
                SolidCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(dimens.cardCornerRadius),
                    backgroundColor = Color.White,
                    borderColor = Color.Transparent,
                    contentPadding = PaddingValues(
                        horizontal = 14.dp * dimens.scale,
                        vertical = 16.dp * dimens.scale
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp * dimens.scale)
                    ) {
                        periods.forEachIndexed { i, period ->
                            val targetFraction = anims[i].value
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = period.icon,
                                    contentDescription = null,
                                    tint = period.bg,
                                    modifier = Modifier.size((dimens.textBody.value * 1.15f).dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(10.dp * dimens.scale)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(0xFFE2E8F0))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(targetFraction)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(50))
                                            .background(period.bg)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "${period.percent}%",
                                    style = MaterialTheme.typography.labelLarge.copy(fontSize = dimens.textLabel),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.width(36.dp * dimens.scale),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }
            }

            EnterAnimation(delay = 800) {
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

@Composable
fun AudioMoodPage(page: SpotlightStoryPage.AudioMood) {
    val skipEntryMotion = skipStoryEntryMotion()
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
            verticalArrangement = Arrangement.Center
        ) {
            EnterAnimation(delay = 0) {
                Text(
                    text = stringResource(R.string.spotlight_sound_profile),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = dimens.textTitle),
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Dominant mood badge
            EnterAnimation(delay = 150) {
                Text(
                    text = page.dominantMood,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = dimens.textHeadline,
                        fontWeight = FontWeight.Black,
                        shadow = Shadow(
                            color = Color(0xFF8B5CF6).copy(alpha = 0.5f),
                            offset = Offset(0f, 4f),
                            blurRadius = 16f
                        )
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(dimens.spacerSmall))

            EnterAnimation(delay = 250) {
                Text(
                    text = page.conversationalText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = dimens.textBody),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(dimens.spacerLarge))

            // Mood bars
            val moodBars = listOf(
                Triple(R.string.spotlight_mood_energy, page.energyPercent, Color(0xFFEF4444)),
                Triple(R.string.spotlight_mood_positivity, page.valencePercent, Color(0xFFF59E0B)),
                Triple(R.string.spotlight_mood_danceability, page.danceabilityPercent, Color(0xFF10B981)),
                Triple(R.string.spotlight_mood_acoustic, page.acousticnessPercent, Color(0xFF60A5FA))
            )

            moodBars.forEachIndexed { index, (label, percent, color) ->
                val animatedWidth = remember { Animatable(0f) }
                LaunchedEffect(percent, skipEntryMotion) {
                    if (skipEntryMotion) {
                        animatedWidth.snapTo(percent / 100f)
                    } else {
                        delay((300 + index * 150).toLong())
                        animatedWidth.animateTo(
                            targetValue = percent / 100f,
                            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
                        )
                    }
                }

                EnterAnimation(delay = 300 + index * 150) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(label),
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = dimens.textBody),
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = "${percent}%",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = dimens.textBody),
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp * dimens.scale))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp * dimens.scale)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedWidth.value)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                color.copy(alpha = 0.8f),
                                                color
                                            )
                                        )
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.height(dimens.spacerMedium))
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

