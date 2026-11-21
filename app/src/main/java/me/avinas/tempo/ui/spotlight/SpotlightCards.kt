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
fun DashboardHeader(
    icon: ImageVector,
    title: String,
    iconColor: Color,
    showShareButton: Boolean = true,
    onShareClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(
                    color = iconColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = iconColor,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
        
        if (showShareButton) {
            IconButton(
                onClick = onShareClick,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// 1. Vedic Astronomy (Samrat Yantra) Functional Clock
@Composable
fun DashboardCosmicClockCard(
    data: SpotlightCardData.CosmicClock,
    showShareButton: Boolean = true,
    onShareClick: () -> Unit = {}
) {
    // Data-driven function: Point to Peak Activity Time
    val peakHour = remember(data.hourlyLevels) {
        data.hourlyLevels.indices.maxByOrNull { data.hourlyLevels[it] } ?: 12 // Default to noon if empty
    }
    
    // Animation state
    val animatedRotation = remember { Animatable(-180f) }
    val targetRotation = (peakHour - 18) * 15f
    
    LaunchedEffect(targetRotation) {
        delay(300) 
        animatedRotation.animateTo(
            targetValue = targetRotation,
            animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
        )
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        backgroundColor = TempoDarkSurface.copy(alpha = 0.9f),
        contentPadding = PaddingValues(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            DashboardHeader(
                icon = Icons.Outlined.Schedule,
                title = "Circadian Rhythm",
                iconColor = GoldenAmber,
                showShareButton = showShareButton,
                onShareClick = onShareClick
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                // Main Instrument Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = center
                    val radius = size.minDimension / 2
                    
                    // 1. The Sky Cycle (Background Ring)
                    val skyGradient = Brush.sweepGradient(
                        0.0f to Color(0xFF0F172A), // Midnight (Top/00:00) - Slate 900
                        0.25f to Color(0xFFFB923C), // Sunrise (06:00) - Orange 400
                        0.5f to Color(0xFFFEF08A), // Noon (12:00) - Yellow 200
                        0.75f to Color(0xFFC084FC), // Sunset (18:00) - Purple 400
                        1.0f to Color(0xFF0F172A), // Midnight
                        center = center
                    )
                    
                    // Draw the Sky Track
                    drawCircle(
                        brush = skyGradient,
                        radius = radius,
                        style = Stroke(width = 4.dp.toPx())
                    )
                    
                    // Fill subtle darkness in the center
                    drawCircle(
                        brush = Brush.radialGradient(
                           colors = listOf(Color(0xFF1E293B), Color(0xFF020617)),
                           center = center,
                           radius = radius
                        ),
                        radius = radius - 4.dp.toPx()
                    )

                    // 2. The Vedic Grid (Muhurtas & Hours)
                    for (i in 0 until 24) {
                        // 00:00 is at Top (-90 deg), but Index 0 in loop.
                        // Formula: (i * 15) - 90
                        val angleDeg = (i * 15f) - 90f
                        val angleRad = angleDeg * (PI / 180f).toFloat()
                        val isMajor = i % 6 == 0 
                        
                        val startR = if (isMajor) radius * 0.88f else radius * 0.92f
                        val endR = radius * 0.98f
                        
                        drawLine(
                            color = if (isMajor) GoldenAmber else Color.White.copy(alpha = 0.2f),
                            start = Offset(center.x + startR * cos(angleRad), center.y + startR * sin(angleRad)),
                            end = Offset(center.x + endR * cos(angleRad), center.y + endR * sin(angleRad)),
                            strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                        )
                        
                        // Numeric Labels for Major Hours
                        if (isMajor) {
                            val text = when(i) {
                                0 -> "0"
                                6 -> "6"
                                12 -> "12"
                                18 -> "18"
                                else -> ""
                            }
                            drawContext.canvas.nativeCanvas.apply {
                                val textPaint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.parseColor("#94A3B8") // Slate 400
                                    textSize = 12.dp.toPx()
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                                }
                                val labelR = radius * 0.82f
                                drawText(
                                    text, 
                                    center.x + labelR * cos(angleRad), 
                                    center.y + labelR * sin(angleRad) + (textPaint.textSize / 3), 
                                    textPaint
                                )
                            }
                        }
                    }
                    
                    // 30 Muhurta Ticks (Inner Ring)
                    for (j in 0 until 30) {
                         val angleDeg = (j * 12f) - 90f
                         val angleRad = angleDeg * (PI / 180f).toFloat()
                         val isMajor = j % 5 == 0 // Every 5 muhurtas (every 4 hours)
                         
                         // Visual: Small ticks floating inside
                         val markerStartR = radius * 0.65f
                         val markerEndR = radius * 0.70f 
                         
                         drawLine(
                            color = GoldenAmber.copy(alpha = if (isMajor) 0.6f else 0.3f),
                            start = Offset(center.x + markerStartR * cos(angleRad), center.y + markerStartR * sin(angleRad)),
                            end = Offset(center.x + markerEndR * cos(angleRad), center.y + markerEndR * sin(angleRad)),
                            strokeWidth = 1.dp.toPx()
                         )
                    }

                    // 3. The Galactic Rhythm (Data Waveform)
                    val path = Path()
                    val dataRadiusBase = radius * 0.45f // Base of the chart 
                    val dataRadiusMax = radius * 0.75f  // Top of the highest peak
                    
                    val points = mutableListOf<Offset>()
                    
                    data.hourlyLevels.forEachIndexed { index, level ->
                        val angleDeg = (index * 15f) - 90f
                        val angleRad = angleDeg * (PI / 180f).toFloat()
                        
                        // Smooth data a bit? No, raw fidelity is better for "truth".
                        // Normalize 0-100 to 0.0-1.0
                        val normalized = level / 100f
                        val r = dataRadiusBase + (normalized * (dataRadiusMax - dataRadiusBase))
                        
                        val x = center.x + r * cos(angleRad)
                        val y = center.y + r * sin(angleRad)
                        points.add(Offset(x, y))
                    }
                    
                    // Build smooth path (Catmull-Rom or simple LineTo)
                    // Straight-line segments to close waveform loop
                    if (points.isNotEmpty()) {
                        path.moveTo(points[0].x, points[0].y)
                        for (i in 1 until points.size) {
                            path.lineTo(points[i].x, points[i].y)
                        }
                        path.close() // Close the loop
                        
                        // Fill Area
                        drawPath(
                            path = path,
                            brush = Brush.radialGradient(
                                colors = listOf(GoldenAmber.copy(alpha = 0.5f), Color.Transparent),
                                center = center,
                                radius = radius * 0.8f
                            )
                        )
                        // Stroke Line
                        drawPath(
                            path = path,
                            color = GoldenAmber,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
                
                // 4. The Samrat Yantra Gnomon (Physical Structure)
                Canvas(modifier = Modifier.size(80.dp)) {
                    val gnomonPath = Path().apply {
                        // Top-down view of a triangular ramp/gnomon
                        // Center is (40, 40)
                        val cx = size.width / 2
                        val cy = size.height / 2
                        
                        moveTo(cx, cy - 30.dp.toPx()) // Tip pointing Top
                        lineTo(cx + 10.dp.toPx(), cy + 20.dp.toPx()) // Base Right
                        lineTo(cx, cy + 10.dp.toPx()) // Inner notch
                        lineTo(cx - 10.dp.toPx(), cy + 20.dp.toPx()) // Base Left
                        close()
                    }
                    
                    drawPath(
                        path = gnomonPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF475569), Color(0xFF1E293B))
                        )
                    )
                    drawPath(
                        path = gnomonPath,
                        color = Color.White.copy(alpha=0.1f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Draw shadow indicator pointing to peak listening hour
                Canvas(modifier = Modifier.fillMaxSize()) {
                     val currentRotation = animatedRotation.value
                     val angleRad = (currentRotation + 180f) * (PI / 180f).toFloat()
                     val shadowLen = size.minDimension / 2 * 0.85f
                     val endX = center.x + shadowLen * cos(angleRad)
                     val endY = center.y + shadowLen * sin(angleRad)
                     
                     drawLine(
                         brush = Brush.linearGradient(
                             colors = listOf(GoldenAmber.copy(alpha=0f), GoldenAmber),
                             start = center,
                             end = Offset(endX, endY)
                         ),
                         start = center,
                         end = Offset(endX, endY),
                         strokeWidth = 3.dp.toPx(),
                         cap = StrokeCap.Round
                     )
                     
                     // Tip
                     drawCircle(
                         color = GoldenAmber,
                         radius = 3.dp.toPx(),
                         center = Offset(endX, endY)
                     )
                }

                // Center Information Display
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(60.dp)) // Push down below Gnomon
                    
                    // Archetype
                    Text(
                        text = data.sunListenerType.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFBAE6FD), 
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Peak Time Display
                    val amPm = if (peakHour < 12) "AM" else "PM"
                    val hour12 = if (peakHour % 12 == 0) 12 else peakHour % 12
                    
                    Text(
                        text = "$hour12 $amPm", 
                        style = MaterialTheme.typography.displaySmall.copy(
                            shadow = Shadow(color = GoldenAmber, blurRadius = 24f)
                        ),
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Insight Summary
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                 // Day
                 Row(verticalAlignment = Alignment.CenterVertically) {
                     Box(modifier = Modifier.size(8.dp).background(GoldenAmber, CircleShape))
                     Spacer(modifier = Modifier.width(8.dp))
                     Column {
                         Text("DAY (${data.dayPercentage}%)", style = MaterialTheme.typography.labelSmall, color = Color.White)
                         Text("Solar Power", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                     }
                 }
                 
                 // Night
                 Row(verticalAlignment = Alignment.CenterVertically) {
                     Box(modifier = Modifier.size(8.dp).background(Color(0xFFC084FC), CircleShape))
                     Spacer(modifier = Modifier.width(8.dp))
                     Column(horizontalAlignment = Alignment.End) {
                         Text("NIGHT (${data.nightPercentage}%)", style = MaterialTheme.typography.labelSmall, color = Color.White)
                         Text("Lunar Calm", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                     }
                 }
            }
        }
    }
}




// 2. Weekend Warrior (3D Isometric Constructivism)
