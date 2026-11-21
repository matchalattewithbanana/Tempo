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
internal fun skipStoryEntryMotion(): Boolean =
    LocalInCaptureContext.current || LocalStoryReducedMotion.current || isStoryPageSeen()

// The pager marks viewed pages so they can skip entry effects on revisit.
val LocalStorySeenPages = compositionLocalOf<SnapshotStateMap<String, Boolean>?> { null }
val LocalStoryPageId = compositionLocalOf<String?> { null }

@Composable
fun isStoryPageSeen(): Boolean {
    val seenMap = LocalStorySeenPages.current ?: return false
    val id = LocalStoryPageId.current ?: return false
    return seenMap[id] == true
}

@Composable
internal fun SolidCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    backgroundColor: Color = Color.White.copy(alpha = 0.08f),
    borderColor: Color = Color.White.copy(alpha = 0.12f),
    borderWidth: Dp = 1.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(backgroundColor, shape)
            .border(borderWidth, borderColor, shape)
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

data class SpotlightDimens(
    val scale: Float,
    // Spacing
    val screenTopPadding: Dp,
    val screenBottomPadding: Dp,
    val horizontalPadding: Dp,
    val spacerSmall: Dp,
    val spacerMedium: Dp,
    val spacerLarge: Dp,
    
    // Text Sizes
    val textDisplay: TextUnit,     // very large numbers
    val textHeadline: TextUnit,    // main titles
    val textTitle: TextUnit,       // section titles
    val textBody: TextUnit,        // normal text
    val textLabel: TextUnit,       // small details
    
    // Image Sizes
    val imageMain: Dp,
    val imageList: Dp,
    val imageGrid: Dp,
    val bubbleMain: Dp,
    
    // Layout
    val cardCornerRadius: Dp,
    val gridSpacing: Dp
)

@Composable
fun rememberSpotlightDimens(maxHeight: Dp): SpotlightDimens {
    val density = LocalDensity.current
    // Reference height: 800dp (approx generic phone height)
    // We clamp scale between 0.75 (small phones) and 1.2 (large/tall phones)
    // so UI doesn't look too tiny or too blown up.
    val scale = (maxHeight.value / 800f).coerceIn(0.75f, 1.2f)
    
    return remember(scale, maxHeight) {
        SpotlightDimens(
            scale = scale,

            screenTopPadding = (maxHeight * 0.08f).coerceAtLeast(32.dp),
            screenBottomPadding = (maxHeight * 0.15f).coerceAtLeast(130.dp), // Increased to clear 'Share Your Story' button
            horizontalPadding = 24.dp, 
            spacerSmall = (maxHeight * 0.015f).coerceAtLeast(8.dp),  // 1.5% height
            spacerMedium = (maxHeight * 0.025f).coerceAtLeast(16.dp), // 2.5% height
            spacerLarge = (maxHeight * 0.05f).coerceAtLeast(32.dp),   // 5% height
            
            textDisplay = (88.sp.value * scale).sp,
            textHeadline = (32.sp.value * scale).sp,
            textTitle = (24.sp.value * scale).sp,
            textBody = (16.sp.value * scale).sp,
            textLabel = (12.sp.value * scale).sp,
            
            imageMain = (maxHeight * 0.25f).coerceIn(160.dp, 260.dp), // 25% of screen height
            imageList = (maxHeight * 0.06f).coerceIn(40.dp, 56.dp),   // 6% of screen height
            imageGrid = (maxHeight * 0.035f).coerceIn(24.dp, 36.dp),
            bubbleMain = (maxHeight * 0.3f).coerceIn(200.dp, 300.dp),
            
            cardCornerRadius = 20.dp * scale,
            gridSpacing = 8.dp
        )
    }
}

@Composable
fun EnterAnimation(
    delay: Int = 0,
    content: @Composable () -> Unit
) {
    if (skipStoryEntryMotion()) {
        content()
    } else {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay((delay + 50).toLong())
            visible = true
        }
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(450)) +
                    slideInVertically(
                        animationSpec = tween(450, easing = FastOutSlowInEasing),
                        initialOffsetY = { 40 }
                    )
        ) {
            content()
        }
    }
}

/**
 * Text that steps its font size down (10% at a time) until it fits the given
 * constraints, so long values / translations never wrap or clip. Falls back to
 * ellipsis only below [minFontSize].
 */
@Composable
internal fun AutoFitText(
    text: String,
    maxFontSize: TextUnit,
    modifier: Modifier = Modifier,
    baseStyle: TextStyle = MaterialTheme.typography.displayLarge,
    color: Color = Color.White,
    fontWeight: FontWeight = FontWeight.Bold,
    fontStyle: FontStyle? = null,
    shadow: Shadow? = null,
    textAlign: TextAlign = TextAlign.Center,
    lineHeightFactor: Float = 1.15f,
    minFontSize: TextUnit = 14.sp,
    maxLines: Int = 3
) {
    var fontSize by remember(text) { mutableStateOf(maxFontSize) }
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        textAlign = textAlign,
        lineHeight = (fontSize.value * lineHeightFactor).sp,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = baseStyle.copy(
            shadow = shadow ?: Shadow(Color.Transparent, Offset.Zero, 0f)
        ),
        onTextLayout = { result ->
            if (result.hasVisualOverflow && fontSize > minFontSize) {
                fontSize = (fontSize.value * 0.9f).sp
            }
        }
    )
}

    
