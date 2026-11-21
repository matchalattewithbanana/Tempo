package me.avinas.tempo.ui.spotlight

import me.avinas.tempo.ui.theme.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlin.random.Random

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.Canvas
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import me.avinas.tempo.ui.components.CachedAsyncImage
import me.avinas.tempo.ui.components.GlassCard
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.rotate
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.abs
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.nativeCanvas
import kotlinx.coroutines.delay

@Composable
fun DashboardWeekendWarriorCard(
    data: SpotlightCardData.WeekendWarrior,
    showShareButton: Boolean = true,
    onShareClick: () -> Unit = {}
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        backgroundColor = TempoDarkBackground.copy(alpha = 0.95f), // Premium Dark
        contentPadding = PaddingValues(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            DashboardHeader(
                icon = Icons.Outlined.Architecture,
                title = "The Weekly Pulse",
                iconColor = TempoSecondary, // Teal
                showShareButton = showShareButton,
                onShareClick = onShareClick
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                // Blueprint Grid Background (Subtle)
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().alpha(0.1f)) {
                    val gridSize = 20.dp.toPx()
                    for (x in 0 until size.width.toInt() step gridSize.toInt()) {
                        drawLine(
                            color = TempoSecondary, // Teal Grid
                            start = Offset(x.toFloat(), 0f),
                            end = Offset(x.toFloat(), size.height),
                            strokeWidth = 1f
                        )
                    }
                    for (y in 0 until size.height.toInt() step gridSize.toInt()) {
                        drawLine(
                            color = TempoSecondary, // Teal Grid
                            start = Offset(0f, y.toFloat()),
                            end = Offset(size.width, y.toFloat()),
                            strokeWidth = 1f
                        )
                    }
                }

                // 3D Isometric Visualization
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                    val centerX = size.width / 2
                    val bottomY = size.height - 20.dp.toPx()
                    val maxBarHeight = size.height * 0.6f
                    val barWidth = 40.dp.toPx()
                    val depth = 20.dp.toPx() // Isometric depth
                    
                    val total = (data.weekdayAverage + data.weekendAverage).coerceAtLeast(1).toFloat()
                    val weekdayH = (data.weekdayAverage / total) * maxBarHeight
                    val weekendH = (data.weekendAverage / total) * maxBarHeight
                    
                    // Helper to draw isometric box
                    fun drawIsoBox(
                        x: Float, 
                        y: Float, 
                        w: Float, // width (front face)
                        h: Float, 
                        d: Float, // depth (side face width approx)
                        frontColor: Color,
                        topColor: Color,
                        sideColor: Color
                    ) {
                        // Math for "fake" isometric/perspective
                        // Front Face
                        drawRect(
                            color = frontColor,
                            topLeft = Offset(x, y - h),
                            size = Size(w, h)
                        )
                        
                        // Top Face (Rhombus-ish / slanted rect)
                        val topPath = Path().apply {
                            moveTo(x, y - h)
                            lineTo(x + d, y - h - (d * 0.5f))
                            lineTo(x + w + d, y - h - (d * 0.5f))
                            lineTo(x + w, y - h)
                            close()
                        }
                        drawPath(path = topPath, color = topColor)
                        
                        // Side Face
                        val sidePath = Path().apply {
                            moveTo(x + w, y - h)
                            lineTo(x + w + d, y - h - (d * 0.5f))
                            lineTo(x + w + d, y - (d * 0.5f)) 
                            lineTo(x + w, y)
                            close()
                        }
                         drawPath(path = sidePath, color = sideColor)
                    }

                    // 1. Weekday Structure (Left, Concrete/Teal)
                    drawIsoBox(
                        x = centerX - barWidth - 20.dp.toPx(),
                        y = bottomY,
                        w = barWidth,
                        h = weekdayH.coerceAtLeast(10f),
                        d = depth,
                        frontColor = Color(0xFF334155), // Slate 700
                        topColor = Color(0xFF475569), // Slate 600
                        sideColor = Color(0xFF1E293B) // Slate 800
                    )

                    // 2. Weekend Structure (Right, Vibrant/Rose)
                     drawIsoBox(
                        x = centerX + 20.dp.toPx(),
                        y = bottomY,
                        w = barWidth,
                        h = weekendH.coerceAtLeast(10f),
                        d = depth,
                        frontColor = TempoError, // Rose 500
                        topColor = Color(0xFFFB7185), // Rose 400
                        sideColor = Color(0xFFBE123C) // Rose 700
                    )
                }
                
                // Labels
                Row(
                    modifier = Modifier.fillMaxSize().padding(bottom = 0.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                     Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${data.weekdayAverage}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "WEEKDAY",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 2.sp
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                         Text(
                            text = "${data.weekendAverage}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "WEEKEND",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFDA4AF), // Rose 300
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
        }
    }
}

// 3. Kintsugi Style (Golden Repair)
@Composable
fun DashboardForgottenFavoriteCard(
    data: SpotlightCardData.ForgottenFavorite,
    showShareButton: Boolean = true,
    onShareClick: () -> Unit = {}
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        backgroundColor = TempoDarkSurface.copy(alpha = 0.8f), // Dark Stone
        contentPadding = PaddingValues(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            DashboardHeader(
                icon = Icons.Outlined.History,
                title = "Forgotten Favorite",
                iconColor = Color(0xFFD6D3D1),
                showShareButton = showShareButton,
                onShareClick = onShareClick
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Kintsugi Album Art (Broken Pottery)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(110.dp)
                ) {
                   if (data.albumArtUrl != null) {
                        // We use a custom shape to simulate fragments, but strictly clipping to a jagged path is complex in Compose without a custom Shape.
                        // Instead, we overlay the cracks firmly.
                        CachedAsyncImage(
                            imageUrl = data.albumArtUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(4.dp)) // Rough cut
                                .alpha(0.7f), // Faded memory
                            contentScale = ContentScale.Crop
                        )
                   }
                   
                   // Gold Cracks Overlay (Jagged / Randomness)
                   androidx.compose.foundation.Canvas(modifier = Modifier.size(110.dp)) {
                       val path = Path().apply {
                           // Jagged "Lightning" shape for realistic ceramic crack
                           moveTo(0f, size.height * 0.4f)
                           lineTo(size.width * 0.2f, size.height * 0.3f)
                           lineTo(size.width * 0.35f, size.height * 0.5f)
                           lineTo(size.width * 0.5f, size.height * 0.45f)
                           lineTo(size.width * 0.65f, size.height * 0.6f)
                           lineTo(size.width * 0.8f, size.height * 0.5f)
                           lineTo(size.width, size.height * 0.65f)
                       }
                       
                       // Glow (Luminescence)
                       drawPath(
                           path = path,
                           color = GoldenAmber.copy(alpha = 0.4f),
                           style = androidx.compose.ui.graphics.drawscope.Stroke(
                               width = 6.dp.toPx(),
                               cap = StrokeCap.Round,
                               join = StrokeJoin.Round
                           )
                       )
                       
                       // The Gold Fill
                       drawPath(
                           path = path,
                           brush = Brush.linearGradient(
                               colors = listOf(GoldenAmber, Color(0xFFB45309), GoldenAmber) // Shimmer
                           ),
                           style = androidx.compose.ui.graphics.drawscope.Stroke(
                               width = 2.dp.toPx(),
                               cap = StrokeCap.Butt,
                               join = StrokeJoin.Round
                           )
                       )
                       
                       // Secondary fine crack
                       val path2 = Path().apply {
                           moveTo(size.width * 0.35f, size.height * 0.5f)
                           lineTo(size.width * 0.4f, size.height)
                       }
                       drawPath(
                           path = path2,
                           color = GoldenAmber.copy(alpha = 0.8f),
                           style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                       )
                   }
                }
                
                Spacer(modifier = Modifier.width(20.dp))
                
                Column {
                    // "Days Lost" - Calligraphic Gold Centerpiece
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${data.daysSinceLastPlay}",
                            style = MaterialTheme.typography.displayMedium.copy(
                                brush = Brush.verticalGradient(
                                    colors = listOf(GoldenAmber, Color(0xFF92400E))
                                )
                            ),
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "days lost",
                            style = MaterialTheme.typography.labelMedium,
                            color = GoldenAmber.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = data.songTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "Restored from memory",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA8A29E)
                    )
                }
            }
        }
    }
}

// 4. Op Art Style (Optical Illusion / Tunnel)
@Composable
fun DashboardDeepDiveCard(
    data: SpotlightCardData.DeepDive,
    showShareButton: Boolean = true,
    onShareClick: () -> Unit = {}
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        backgroundColor = TempoDarkBackground, // Deep Void
        contentPadding = PaddingValues(24.dp)
    ) {
        Column {
            DashboardHeader(
                icon = Icons.Outlined.PlayArrow,
                title = "Sonic Immersion", 
                iconColor = ElectricBlue,
                showShareButton = showShareButton,
                onShareClick = onShareClick
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Op Art Tunnel Background (Animated)
                val infiniteTransition = me.avinas.tempo.ui.theme.rememberMotionAwareInfiniteTransition(label = "tunnel")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                        animation = androidx.compose.animation.core.tween(20000, easing = androidx.compose.animation.core.LinearEasing)
                    ),
                    label = "rotation"
                )
                
                val pulse by infiniteTransition.animateFloat(
                    initialValue = 0.95f,
                    targetValue = 1.05f,
                    animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                        animation = androidx.compose.animation.core.tween(2000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                        repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                    ),
                    label = "pulse"
                )

                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { 
                            rotationZ = rotation 
                            scaleX = pulse
                            scaleY = pulse
                        }
                ) {
                     val centerX = size.width / 2
                     val centerY = size.height / 2
                     val maxRadius = size.maxDimension / 2
                     
                     // Warped Concentric Rings (Bridget Riley style)
                     for (i in 0 until 12) {
                         val progress = i / 12f
                         val radius = maxRadius * (1f - progress)
                         
                         // Sine wave distortion
                         val path = Path()
                         val steps = 60
                         for (j in 0..steps) {
                             val theta = (j.toFloat() / steps) * 2 * PI
                             val distortion = sin(theta * 6) * (10.dp.toPx() * progress) // Wavy effect increases towards center? Or edge?
                             val r = radius + distortion.toFloat()
                             
                             val x = centerX + r * cos(theta).toFloat()
                             val y = centerY + r * sin(theta).toFloat()
                             
                             if (j == 0) path.moveTo(x, y) else path.lineTo(x, y)
                         }
                         path.close()
                         
                         drawPath(
                             path = path,
                             color = if (i % 2 == 0) GlassWhite else ElectricBlue.copy(alpha=0.2f),
                             style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()) // Thin precise lines
                         )
                     }
                     
                     // Radial Vignette/Depth
                     drawCircle(
                         brush = Brush.radialGradient(
                             colors = listOf(
                                 Color.Transparent,
                                 Color.Black.copy(alpha = 0.9f)
                             ),
                             center = Offset(centerX, centerY),
                             radius = maxRadius
                         ),
                         radius = maxRadius,
                         center = Offset(centerX, centerY)
                     )
                }
                
                // Singularity Data (The Light at the End)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF38BDF8).copy(alpha=0.3f), Color.Transparent)
                            )
                        )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${data.durationMinutes}",
                            style = MaterialTheme.typography.displayMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                         Text(
                            text = "MINUTES",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF38BDF8),
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
             
             // Time Context (Curved or Bottom)
             Text(
                text = "${data.timeOfDay} • ${data.date}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFBAE6FD),
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp)
            )
        }
    }
}

// 5. Newspaper Clipping Style (Editorial / Vintage Print)
