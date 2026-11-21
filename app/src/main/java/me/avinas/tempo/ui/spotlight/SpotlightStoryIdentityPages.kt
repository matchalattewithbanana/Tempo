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

internal val LocalStoryReducedMotion = compositionLocalOf { false }

@Composable
fun PersonalityPage(page: SpotlightStoryPage.Personality) {
    val (icon, color) = getPersonalityAssets(page.personalityKey)
    
    val infiniteTransition = me.avinas.tempo.ui.theme.rememberMotionAwareInfiniteTransition(label = "personality")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val dimens = rememberSpotlightDimens(maxHeight)
        val isShortScreen = maxHeight < 650.dp
        
        // Full-screen color wash
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = 0.4f),
                            color.copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        center = Offset(0.5f, 0.3f)
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
            EnterAnimation(delay = 0) {
                Text(
                    text = stringResource(R.string.spotlight_listening_personality),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = dimens.textTitle),
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // CINEMATIC ICON - Huge, rotating, dramatic
            EnterAnimation(delay = 300) {
                // Reduce ring size on short screens to avoid clipping
                val ringSize = if (isShortScreen) {
                    (220.dp * dimens.scale).coerceIn(170.dp, 240.dp)
                } else {
                    (300.dp * dimens.scale).coerceIn(240.dp, 360.dp)
                }
                Box(
                    modifier = Modifier.size(ringSize),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer rotating ring
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationZ = rotationAngle }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.sweepGradient(
                                        colors = listOf(
                                            color.copy(alpha = 0.6f),
                                            Color.Transparent,
                                            color.copy(alpha = 0.3f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = CircleShape
                                )
                        )
                    }
                    
                    // Personality icon badge
                    Box(
                        modifier = Modifier
                            .size(140.dp * dimens.scale)
                            .background(color, CircleShape)
                            .border(3.dp * dimens.scale, Color.White.copy(alpha = 0.9f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color(0xFF1B120E),
                            modifier = Modifier.size(80.dp * dimens.scale)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // REVEAL - Personality type with dramatic typography
            // Auto-fitting title for smaller screens
            EnterAnimation(delay = 600) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AutoFitText(
                        text = page.personalityType,
                        maxFontSize = (dimens.textDisplay.value * 1.2f).coerceAtMost(56f).sp,
                        baseStyle = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (2f * dimens.scale).sp
                        ),
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        minFontSize = dimens.textTitle,
                        modifier = Modifier.fillMaxWidth(),
                        lineHeightFactor = 1.05f
                    )

                    Spacer(modifier = Modifier.height(if (isShortScreen) dimens.spacerMedium else dimens.spacerLarge))

                    // Description - intimate, left-aligned
                    Text(
                        text = page.description,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = dimens.textBody,
                            lineHeight = (dimens.textBody.value * (if (isShortScreen) 1.45f else 1.6f)).sp
                        ),
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Start,
                        maxLines = if (isShortScreen) 3 else 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(dimens.spacerMedium))

                    // Conversational - whispered, italic
                    Text(
                        text = page.conversationalText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = (dimens.textBody.value * 0.9f).sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        ),
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        maxLines = if (isShortScreen) 2 else 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// NEW SPOTLIGHT STORY PAGES

@Composable
fun ListeningStreakPage(page: SpotlightStoryPage.ListeningStreak) {
    val skipEntryMotion = skipStoryEntryMotion()
    val animatedStreak = remember { Animatable(0f) }
    LaunchedEffect(page.currentStreakDays, skipEntryMotion) {
        if (skipEntryMotion) {
            animatedStreak.snapTo(page.currentStreakDays.toFloat())
        } else {
            delay(400)
            animatedStreak.animateTo(
                targetValue = page.currentStreakDays.toFloat(),
                animationSpec = tween(durationMillis = 1800, easing = FastOutSlowInEasing)
            )
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
            verticalArrangement = Arrangement.Center
        ) {
            EnterAnimation(delay = 0) {
                Text(
                    text = stringResource(R.string.spotlight_listening_streak_label),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = dimens.textTitle),
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Streak icon badge
            EnterAnimation(delay = 200) {
                Box(
                    modifier = Modifier
                        .size(96.dp * dimens.scale)
                        .background(Color(0xFFFF9900), CircleShape)
                        .border(2.dp * dimens.scale, Color.White.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFF220F08),
                        modifier = Modifier.size(52.dp * dimens.scale)
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimens.spacerSmall))

            // Animated streak number
            EnterAnimation(delay = 300) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = animatedStreak.value.toInt().toString(),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = dimens.textDisplay,
                            fontWeight = FontWeight.Black
                        ),
                        color = Color(0xFFFFD060),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (page.currentStreakDays == 1) stringResource(R.string.spotlight_day_in_a_row) else stringResource(R.string.spotlight_days_in_a_row),
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = dimens.textHeadline),
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f),
                        letterSpacing = (1f * dimens.scale).sp
                    )
                    Spacer(modifier = Modifier.height(dimens.spacerSmall))
                    Text(
                        text = page.conversationalText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = dimens.textBody),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = Color.White.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Stats row: longest + total active days
            EnterAnimation(delay = 600) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp * dimens.scale)
                ) {
                    // Best streak highlight card
                    SolidCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(dimens.cardCornerRadius),
                        backgroundColor = Color(0xFF7C3A00),
                        borderColor = Color.Transparent,
                        contentPadding = PaddingValues(dimens.spacerSmall)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = page.longestStreakDays.toString(),
                                style = MaterialTheme.typography.headlineMedium.copy(fontSize = dimens.textTitle),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD060),
                                maxLines = 1
                            )
                            Text(
                                text = stringResource(R.string.spotlight_best_streak),
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = dimens.textLabel),
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1
                            )
                        }
                    }
                    SolidCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(dimens.cardCornerRadius),
                        backgroundColor = Color.White.copy(alpha = 0.06f),
                        borderColor = Color.White.copy(alpha = 0.15f),
                        contentPadding = PaddingValues(dimens.spacerSmall)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = page.totalActiveDays.toString(),
                                style = MaterialTheme.typography.headlineMedium.copy(fontSize = dimens.textTitle),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                            Text(
                                text = stringResource(R.string.spotlight_active_days),
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = dimens.textLabel),
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
            
            // Comparative surprise callout
            if (page.comparativeText != null) {
                EnterAnimation(delay = 800) {
                    Spacer(modifier = Modifier.height(dimens.spacerMedium))
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0xFFFF6B00).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(50)
                            )
                            .border(1.dp * dimens.scale, Color(0xFFFF9900).copy(alpha = 0.4f), RoundedCornerShape(50))
                            .padding(horizontal = 20.dp * dimens.scale, vertical = 10.dp * dimens.scale)
                    ) {
                        Text(
                            text = page.comparativeText,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = (dimens.textBody.value * 0.9f).sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFFFFD060),
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
fun ListeningClockPage(page: SpotlightStoryPage.ListeningClock) {
    val skipEntryMotion = skipStoryEntryMotion()
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(skipEntryMotion) {
        if (skipEntryMotion) {
            animatedProgress.snapTo(1f)
        } else {
            delay(300)
            animatedProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1600, easing = FastOutSlowInEasing)
            )
        }
    }

    val isNightOwl = page.peakHour >= 20 || page.peakHour < 6
    val accentColor = if (isNightOwl) Color(0xFF7C3AED) else Color(0xFFF59E0B)
    val secondaryColor = if (isNightOwl) Color(0xFF60A5FA) else Color(0xFFFBBF24)

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
                    text = stringResource(R.string.spotlight_listening_clock_label),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = dimens.textTitle),
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Radial Clock Canvas
            EnterAnimation(delay = 200) {
                val clockSize = (260.dp * dimens.scale).coerceIn(220.dp, 320.dp)
                Box(
                    modifier = Modifier.size(clockSize),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val outerRadius = size.minDimension / 2f
                        val innerRadius = outerRadius * 0.35f
                        val barMaxHeight = outerRadius - innerRadius

                        // Draw background track ring
                        drawCircle(
                            color = Color.White.copy(alpha = 0.1f),
                            radius = (innerRadius + barMaxHeight / 2f),
                            style = Stroke(width = barMaxHeight)
                        )

                        val numBars = 24
                        for (i in 0 until numBars) {
                            val level = (page.hourlyLevels.getOrNull(i) ?: 0) / 100f
                            val animLevel = level * animatedProgress.value
                            val angleDeg = (i.toFloat() / numBars) * 360f - 90f
                            val angleRad = (angleDeg * PI / 180f).toFloat()

                            val barHeight = (barMaxHeight * animLevel.coerceAtLeast(0.05f))
                            val startR = innerRadius
                            val endR = startR + barHeight

                            val isPeak = i == page.peakHour
                            val barColor = when {
                                isPeak -> accentColor
                                level > 0.6f -> secondaryColor.copy(alpha = 0.9f)
                                level > 0.3f -> Color.White.copy(alpha = 0.65f)
                                else -> Color.White.copy(alpha = 0.25f)
                            }

                            val startX = cx + cos(angleRad) * startR
                            val startY = cy + sin(angleRad) * startR
                            val endX = cx + cos(angleRad) * endR
                            val endY = cy + sin(angleRad) * endR

                            drawLine(
                                color = barColor,
                                start = androidx.compose.ui.geometry.Offset(startX, startY),
                                end = androidx.compose.ui.geometry.Offset(endX, endY),
                                strokeWidth = if (isPeak) 8f else 5f,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    // Center label
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (isNightOwl) Icons.Default.Nightlight else Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(28.dp * dimens.scale)
                        )
                        Text(
                            text = page.peakHourLabel,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = dimens.textBody),
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.spotlight_peak),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = dimens.textLabel),
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(dimens.spacerMedium))

            // Listener type badge
            EnterAnimation(delay = 500) {
                    SolidCard(
                        modifier = Modifier.wrapContentSize(),
                        shape = RoundedCornerShape(50),
                        backgroundColor = accentColor.copy(alpha = 0.12f),
                        borderColor = accentColor.copy(alpha = 0.35f),
                        contentPadding = PaddingValues(horizontal = 24.dp * dimens.scale, vertical = 10.dp * dimens.scale)
                    ) {
                        Text(
                            text = page.listenerType,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = dimens.textBody),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimens.spacerSmall))

            EnterAnimation(delay = 700) {
                Text(
                    text = page.conversationalText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = dimens.textBody),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Hour labels
            EnterAnimation(delay = 800) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(
                        R.string.spotlight_clock_12am,
                        R.string.spotlight_clock_6am,
                        R.string.spotlight_clock_12pm,
                        R.string.spotlight_clock_6pm
                    ).forEach { labelRes ->
                        SolidCard(
                            modifier = Modifier.wrapContentSize(),
                            shape = RoundedCornerShape(50),
                            backgroundColor = Color.White.copy(alpha = 0.05f),
                            borderColor = Color.White.copy(alpha = 0.12f),
                            borderWidth = 0.5.dp,
                            contentPadding = PaddingValues(horizontal = 8.dp * dimens.scale, vertical = 4.dp * dimens.scale)
                        ) {
                            Text(
                                text = stringResource(labelRes),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = dimens.textLabel),
                                color = Color.White.copy(alpha = 0.65f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopAlbumPage(page: SpotlightStoryPage.TopAlbum) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val dimens = rememberSpotlightDimens(maxHeight)

        // Blurred background art
        if (page.albumArtUrl != null) {
            CachedAsyncImage(
                imageUrl = page.albumArtUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(40.dp * dimens.scale)
                    .scale(1.3f),
                contentScale = ContentScale.Crop,
                allowHardware = false
            )
            // Darken overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.55f),
                                Color.Black.copy(alpha = 0.75f)
                            )
                        )
                    )
            )
        }

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
            EnterAnimation(delay = 0) {
                Text(
                    text = stringResource(R.string.spotlight_top_album_label),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = dimens.textTitle),
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Album art - large reveal
            EnterAnimation(delay = 300) {
                val artSize = dimens.imageMain * 1.1f
                Box(contentAlignment = Alignment.Center) {
                    // Glow behind art
                    Box(
                        modifier = Modifier
                            .size(artSize + 50.dp * dimens.scale)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                ),
                                shape = RoundedCornerShape(28.dp * dimens.scale)
                            )
                    )
                    CachedAsyncImage(
                        imageUrl = page.albumArtUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(artSize)
                            .clip(RoundedCornerShape(20.dp * dimens.scale))
                            .border(2.dp * dimens.scale, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp * dimens.scale)),
                        contentScale = ContentScale.Crop,
                        allowHardware = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(dimens.spacerMedium))

            // Album name and artist
            EnterAnimation(delay = 500) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = page.albumName,
                        style = MaterialTheme.typography.displaySmall.copy(fontSize = dimens.textHeadline),
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp * dimens.scale))
                    Text(
                        text = page.artistName,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = dimens.textTitle),
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(dimens.spacerSmall))
                    Text(
                        text = page.conversationalText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = dimens.textBody),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Stats row
            EnterAnimation(delay = 700) {
                val timeText = me.avinas.tempo.ui.stats.displayStatsDuration(page.totalTimeMs)
                val values = listOf(page.playCount.toString(), timeText, page.uniqueTracksPlayed.toString())
                val maxLen = values.maxOf { it.length }
                val statFontSize = when {
                    maxLen >= 8 -> (dimens.textTitle.value * 0.7f).sp
                    maxLen >= 6 -> (dimens.textTitle.value * 0.8f).sp
                    else -> dimens.textTitle
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp * dimens.scale)
                ) {
                    listOf(
                        Pair(values[0], stringResource(R.string.spotlight_plays)),
                        Pair(values[1], stringResource(R.string.spotlight_time_spent)),
                        Pair(values[2], stringResource(R.string.spotlight_tracks))
                    ).forEach { (value, label) ->
                        SolidCard(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(dimens.cardCornerRadius),
                            backgroundColor = Color.White.copy(alpha = 0.06f),
                            borderColor = Color.White.copy(alpha = 0.15f),
                            contentPadding = PaddingValues(vertical = 10.dp * dimens.scale, horizontal = 4.dp * dimens.scale)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.titleLarge.copy(fontSize = statFontSize),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = dimens.textLabel),
                                    color = Color.White.copy(alpha = 0.65f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

