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
fun BadgesEarnedPage(page: SpotlightStoryPage.BadgesEarned) {
    // Helper: map category to accent color
    fun categoryColor(cat: String): Color = when (cat) {
        "MILESTONE"  -> Color(0xFFF59E0B)
        "TIME"       -> Color(0xFF34D399)
        "STREAK"     -> Color(0xFFEF4444)
        "DISCOVERY"  -> Color(0xFF60A5FA)
        "ENGAGEMENT" -> Color(0xFFA78BFA)
        "LEVEL"      -> Color(0xFFFBBF24)
        else         -> Color(0xFFC4B5FD)
    }

    val infiniteTransition = me.avinas.tempo.ui.theme.rememberMotionAwareInfiniteTransition(label = "trophy")
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "shimmer"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val dimens = rememberSpotlightDimens(maxHeight)

        // Warm golden ambient glow
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Box(
                modifier = Modifier
                    .offset(y = (-40).dp)
                    .size(260.dp * dimens.scale)
                    .graphicsLayer { alpha = shimmer * 0.35f }
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFF59E0B), Color(0xFFF59E0B).copy(alpha = 0.3f), Color.Transparent)
                        ),
                        shape = CircleShape
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            EnterAnimation(delay = 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(32.dp * dimens.scale)
                    )
                    Spacer(modifier = Modifier.height(4.dp * dimens.scale))
                    Text(
                        text = stringResource(R.string.spotlight_badges_earned),
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = dimens.textTitle),
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            // Badges grid (up to 6, 2 cols)
            EnterAnimation(delay = 200) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp * dimens.scale),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    page.badges.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp * dimens.scale)
                        ) {
                            row.forEach { badge ->
                                val accent = categoryColor(badge.category)
                                SolidCard(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(dimens.cardCornerRadius),
                                    backgroundColor = accent.copy(alpha = 0.08f),
                                    borderColor = accent.copy(alpha = 0.25f),
                                    contentPadding = PaddingValues(10.dp * dimens.scale)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Stars
                                        Row(horizontalArrangement = Arrangement.Center) {
                                            repeat(badge.stars) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFBBF24),
                                                    modifier = Modifier.size(12.dp * dimens.scale)
                                                )
                                            }
                                            repeat((5 - badge.stars).coerceAtLeast(0)) {
                                                Icon(
                                                    imageVector = Icons.Default.StarBorder,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFBBF24).copy(alpha = 0.4f),
                                                    modifier = Modifier.size(12.dp * dimens.scale)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp * dimens.scale))
                                        Text(
                                            text = badge.name,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontSize = (dimens.textBody.value * 0.88f).sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = accent,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = badge.description,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = (dimens.textLabel.value * 0.8f).sp
                                            ),
                                            color = Color.White.copy(alpha = 0.6f),
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            // Fill space if odd number
                            if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Progress pill
            EnterAnimation(delay = 600) {
                        SolidCard(
                            modifier = Modifier.wrapContentSize(),
                            shape = RoundedCornerShape(50),
                            backgroundColor = Color(0xFFF59E0B).copy(alpha = 0.1f),
                            borderColor = Color(0xFFF59E0B).copy(alpha = 0.3f),
                            contentPadding = PaddingValues(horizontal = 20.dp * dimens.scale, vertical = 8.dp * dimens.scale)
                        ) {
                            Text(
                                text = stringResource(R.string.spotlight_badges_count, page.totalEarned, page.totalPossible),
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = (dimens.textBody.value * 0.85f).sp),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFBBF24)
                    )
                }
            }

            EnterAnimation(delay = 800) {
                Text(
                    text = page.conversationalText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = dimens.textBody),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun LevelUpPage(page: SpotlightStoryPage.LevelUp) {
    val skipEntryMotion = skipStoryEntryMotion()
    // Energy/power-up design — orange theme, horizontal glow streak, counter animation
    val levelAnim = remember { Animatable(0f) }
    val xpAnim   = remember { Animatable(0f) }
    val barAnim  = remember { Animatable(0f) }

    LaunchedEffect(page.currentLevel, skipEntryMotion) {
        if (skipEntryMotion) {
            levelAnim.snapTo(page.currentLevel.toFloat())
        } else {
            delay(300)
            levelAnim.animateTo(page.currentLevel.toFloat(), tween(1000, easing = FastOutSlowInEasing))
        }
    }
    LaunchedEffect(page.xpEarnedThisPeriod, skipEntryMotion) {
        if (skipEntryMotion) {
            xpAnim.snapTo(page.xpEarnedThisPeriod.toFloat())
        } else {
            delay(500)
            xpAnim.animateTo(page.xpEarnedThisPeriod.toFloat(), tween(1200, easing = FastOutSlowInEasing))
        }
    }
    LaunchedEffect(page.levelProgress, skipEntryMotion) {
        if (skipEntryMotion) {
            barAnim.snapTo(page.levelProgress.coerceIn(0f, 1f))
        } else {
            delay(800)
            barAnim.animateTo(page.levelProgress.coerceIn(0f, 1f), tween(1000, easing = FastOutSlowInEasing))
        }
    }

    val infiniteTransition = me.avinas.tempo.ui.theme.rememberMotionAwareInfiniteTransition(label = "lvl")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.12f, targetValue = 0.38f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pa"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val dimens = rememberSpotlightDimens(maxHeight)

        // Gradient built once with a fully opaque middle stop; the pulse is
        // applied as a layer alpha below, so nothing is allocated per frame.
        val streakBrush = remember {
            Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFFF59E0B),
                    Color.Transparent
                )
            )
        }

        // Horizontal streak across the center — distinctly different from radial glow
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp * dimens.scale)
                    .graphicsLayer { alpha = pulseAlpha }
                    .background(brush = streakBrush)
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Level up ribbon pill
            EnterAnimation(delay = 0) {
                SolidCard(
                    modifier = Modifier.wrapContentSize(),
                    shape = RoundedCornerShape(50),
                    backgroundColor = Color(0xFFFBBF24),
                    borderColor = Color.Transparent,
                    contentPadding = PaddingValues(horizontal = 16.dp * dimens.scale, vertical = 6.dp * dimens.scale)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = Color(0xFF220F08),
                            modifier = Modifier.size(18.dp * dimens.scale)
                        )
                        Spacer(Modifier.width(6.dp * dimens.scale))
                        Text(
                            text = stringResource(R.string.spotlight_level_up),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = dimens.textLabel,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (3f * dimens.scale).sp
                            ),
                            color = Color(0xFF220F08)
                        )
                    }
                }
            }

            // Giant level number — much bigger than TitleEarned's text
            EnterAnimation(delay = 150) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.spotlight_level_label),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = dimens.textLabel,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (4f * dimens.scale).sp
                        ),
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = levelAnim.value.toInt().toString(),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = (dimens.textDisplay.value * 1.7f).coerceAtMost(120f).sp,
                            fontWeight = FontWeight.Black,
                            shadow = Shadow(
                                color = Color(0xFFF59E0B),
                                offset = Offset(0f, 0f),
                                blurRadius = 32f
                            )
                        ),
                        color = Color(0xFFFBBF24)
                    )
                }
            }

            // XP Progress bar (tricolor)
            EnterAnimation(delay = 500) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.spotlight_lv_format, page.currentLevel),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = (dimens.textLabel.value * 0.85f).sp),
                            color = Color(0xFFFBBF24).copy(alpha = 0.7f)
                        )
                        Text(
                            text = stringResource(R.string.spotlight_lv_format, page.currentLevel + 1),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = (dimens.textLabel.value * 0.85f).sp),
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(Modifier.height(4.dp * dimens.scale))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp * dimens.scale)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(barAnim.value)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        listOf(Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFFFBBF24))
                                    )
                                )
                        )
                    }
                }
            }

            // XP gained stat row
            EnterAnimation(delay = 700) {
                        SolidCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(dimens.cardCornerRadius),
                            backgroundColor = Color(0xFFF59E0B).copy(alpha = 0.08f),
                            borderColor = Color(0xFFF59E0B).copy(alpha = 0.25f),
                            contentPadding = PaddingValues(16.dp * dimens.scale)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    val xpDisplayed = xpAnim.value.toLong()
                                    // String resource already includes the "+" prefix and "XP" suffix.
                                    val xpText = if (xpDisplayed >= 1000) stringResource(R.string.spotlight_xp_format_k, xpDisplayed / 1000f) else stringResource(R.string.spotlight_xp_format, xpDisplayed)
                                    Text(
                                        text = xpText,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontSize = dimens.textTitle,
                                            fontWeight = FontWeight.Black
                                        ),
                                        color = Color(0xFFFBBF24)
                                    )
                                    Text(
                                        text = stringResource(R.string.spotlight_earned_this_period),
                                        style = MaterialTheme.typography.labelMedium.copy(fontSize = dimens.textLabel),
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                                Text(
                                    text = page.currentTitle,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontSize = (dimens.textBody.value * 0.85f).sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFFFBBF24).copy(alpha = 0.8f)
                                )
                            }
                        }
            }

            EnterAnimation(delay = 900) {
                Text(
                    text = page.conversationalText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = (dimens.textBody.value * 0.9f).sp),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun TitleEarnedPage(page: SpotlightStoryPage.TitleEarned) {
    val skipEntryMotion = skipStoryEntryMotion()
    // Cross-fade to new title
    val oldTitleAlpha = remember { Animatable(0f) }
    val newTitleOffset = remember { Animatable(80f) }
    val newTitleAlpha  = remember { Animatable(0f) }
    val ringAlpha      = remember { Animatable(0f) }

    LaunchedEffect(skipEntryMotion) {
        if (skipEntryMotion) {
            oldTitleAlpha.snapTo(0f)
            newTitleOffset.snapTo(0f)
            newTitleAlpha.snapTo(1f)
        } else {
            delay(200)
            oldTitleAlpha.animateTo(1f, tween(400))
            delay(500)
            oldTitleAlpha.animateTo(0f, tween(350))
            delay(100)
            newTitleOffset.animateTo(0f, tween(600, easing = FastOutSlowInEasing))
            newTitleAlpha.animateTo(1f, tween(500))
        }
    }
    LaunchedEffect(skipEntryMotion) {
        if (skipEntryMotion) {
            ringAlpha.snapTo(1f)
        } else {
            delay(300)
            ringAlpha.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
        }
    }

    val titleColor = when (page.newTitle) {
        "Sound God"          -> Color(0xFFE879F9)
        "Audiophile"         -> Color(0xFFC084FC)
        "Music Legend"       -> Color(0xFF818CF8)
        "Music Connoisseur"  -> Color(0xFF60A5FA)
        "Dedicated Listener" -> Color(0xFF34D399)
        "Music Enthusiast"   -> Color(0xFFFBBF24)
        "Music Fan"          -> Color(0xFF94A3B8)
        else                 -> Color(0xFFC4B5FD)
    }

    val infiniteTransition = me.avinas.tempo.ui.theme.rememberMotionAwareInfiniteTransition(label = "titlering")
    val ringRotate by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(12000, easing = androidx.compose.animation.core.LinearEasing)),
        label = "rotate"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val dimens = rememberSpotlightDimens(maxHeight)

        // Decorative rotating halo ring drawn on Canvas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(ringAlpha.value),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.size(220.dp * dimens.scale)
            ) {
                val strokePx = 2.dp.toPx()
                val radius = size.minDimension / 2 - strokePx
                // Dashed arc effect: draw multiple short arcs
                val dashCount = 24
                val sweepEach = 360f / dashCount
                for (i in 0 until dashCount) {
                    val startAngle = ringRotate + i * sweepEach
                    val arcAlpha = if (i % 2 == 0) 0.6f else 0.15f
                    drawArc(
                        color = titleColor.copy(alpha = arcAlpha),
                        startAngle = startAngle,
                        sweepAngle = sweepEach * 0.65f,
                        useCenter = false,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                }
            }
            // Inner glow dot
            Box(
                modifier = Modifier
                    .size(16.dp * dimens.scale)
                    .background(
                        brush = Brush.radialGradient(listOf(titleColor, Color.Transparent)),
                        shape = CircleShape
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            EnterAnimation(delay = 0) {
                Text(
                    text = stringResource(R.string.spotlight_new_title),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = dimens.textLabel,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (4f * dimens.scale).sp
                    ),
                    color = titleColor.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            // Animated title swap area
            Box(
                modifier = Modifier
                    .height(100.dp * dimens.scale)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Old title — fades in then out
                if (page.previousTitle.isNotBlank()) {
                    Text(
                        text = page.previousTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = (dimens.textTitle.value * 0.9f).sp,
                            textDecoration = TextDecoration.LineThrough
                        ),
                        color = Color.White.copy(alpha = oldTitleAlpha.value * 0.4f),
                        textAlign = TextAlign.Center
                    )
                }
                // New title — rises from below
                Text(
                    text = page.newTitle,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = (dimens.textHeadline.value * 1.1f).sp,
                        fontWeight = FontWeight.Black,
                        shadow = Shadow(
                            color = titleColor.copy(alpha = 0.8f),
                            offset = Offset(0f, 6f),
                            blurRadius = 24f
                        )
                    ),
                    color = titleColor.copy(alpha = newTitleAlpha.value),
                    modifier = Modifier.offset(y = newTitleOffset.value.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Stats row
            EnterAnimation(delay = 1400) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp * dimens.scale)
                ) {
                    listOf(
                        stringResource(R.string.spotlight_lv_format, page.currentLevel) to stringResource(R.string.spotlight_level),
                        "${page.uniqueArtists}" to stringResource(R.string.spotlight_artists_discovered)
                    ).forEach { (value, label) ->
                        SolidCard(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(dimens.cardCornerRadius),
                            backgroundColor = titleColor.copy(alpha = 0.08f),
                            borderColor = titleColor.copy(alpha = 0.25f),
                            contentPadding = PaddingValues(14.dp * dimens.scale)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontSize = dimens.textTitle,
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = titleColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = (dimens.textLabel.value * 0.85f).sp),
                                    color = Color.White.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            EnterAnimation(delay = 1600) {
                Text(
                    text = page.conversationalText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = (dimens.textBody.value * 0.9f).sp),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = Color.White.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
