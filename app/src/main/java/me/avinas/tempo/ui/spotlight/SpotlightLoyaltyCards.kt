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
fun DashboardArtistLoyaltyCard(
    data: SpotlightCardData.ArtistLoyalty,
    showShareButton: Boolean = true,
    onShareClick: () -> Unit = {}
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp), 
        backgroundColor = Color(0xFF1E1E1E), 
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        ) {
             // 1. Warm "Listening Room" Background
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                 drawRect(
                     brush = Brush.verticalGradient(
                         colors = listOf(Color(0xFF2C1E18), Color(0xFF0F0A08)) // Dark Amber/Wood tone
                     )
                 )
                 // "Dust" particles
                 val random = Random(123)
                 for(i in 0..50) {
                     drawCircle(
                         color = Color(0xFFFCD34D).copy(alpha = 0.1f),
                         radius = random.nextFloat() * 2.dp.toPx(),
                         center = Offset(random.nextFloat()*size.width, random.nextFloat()*size.height)
                     )
                 }
            }

            // 2. Vinyl Stack Visualization
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, bottom = 24.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                 // Stack of records
                 // We draw multiple "spines" of records
                 val stackCount = (data.uniqueTrackCount / 2).coerceIn(3, 15)
                 
                 Column(verticalArrangement = Arrangement.spacedBy((-12).dp)) {
                     repeat(stackCount) { index ->
                         val isTop = index == stackCount - 1
                         val width = 180.dp + (index * 2).dp // Slight variation
                         val color = if (isTop) Color(0xFF1DB954) else Color.White.copy(alpha = 0.1f + (index * 0.05f))
                         
                         Box(
                             modifier = Modifier
                                 .width(width)
                                 .height(24.dp)
                                 // Rotated slightly for chaotic stack look
                                 .rotate(if (index % 2 == 0) -1f else 1f)
                                 .clip(RoundedCornerShape(2.dp))
                                 .background(
                                     brush = Brush.horizontalGradient(
                                         0.0f to Color(0xFF121212),
                                         0.1f to color.copy(alpha=0.5f), // Spine highlight
                                         0.9f to Color(0xFF121212)
                                     )
                                 )
                                 .border(0.5.dp, Color.White.copy(alpha=0.2f), RoundedCornerShape(2.dp))
                         )
                     }
                 }
            }
            
            // 3. Info Content (Right Side)
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(180.dp)
                    .padding(top = 24.dp, end = 20.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.End
            ) {
                DashboardHeader(
                    title = "THE DEEP CUT",
                    icon = Icons.Filled.Album,
                    iconColor = Color(0xFFF59E0B), // Amber 500
                    showShareButton = showShareButton,
                    onShareClick = onShareClick
                )
                
                Spacer(modifier = Modifier.weight(1f))
            
                Text(
                    text = "${data.uniqueTrackCount}",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF59E0B)
                )
                Text(
                    text = "UNIQUE TRACKS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFCD34D).copy(alpha=0.7f),
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "You don't just play the hits. You explored ${data.artistName}'s entire catalog.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha=0.8f),
                    textAlign = TextAlign.End,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// 9. Discovery -> "The Sonic Horizon" (Event Horizon)
@Composable
fun DashboardDiscoveryCard(
    data: SpotlightCardData.Discovery,
    showShareButton: Boolean = true,
    onShareClick: () -> Unit = {}
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        backgroundColor = Color.Black,
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        ) {
            // 1. Event Horizon Visualization
             androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                 val centerX = size.width * 0.5f
                 val centerY = size.height * 0.6f
                 val maxRadius = size.width * 0.8f
                 
                 // Core accent mapped to discovery type
                 val coreColor = when(data.discoveryType) {
                     "The Explorer" -> Color(0xFF8B5CF6) // Violet (New)
                     "The Time Traveler" -> Color(0xFFF59E0B) // Amber (Old/Gold)
                     else -> Color(0xFF06B6D4) // Cyan (Balanced)
                 }

                 // A. Accretion Disk (Swirling gradients)
                 withTransform({
                     translate(centerX, centerY)
                     scale(1f, 0.4f) // Flatten perspective
                     rotate(degrees = 20f, pivot = Offset.Zero) // Tilt
                 }) {
                     // Outer Glow
                     drawCircle(
                         brush = Brush.radialGradient(
                             colors = listOf(coreColor.copy(alpha=0.6f), Color.Transparent),
                             radius = maxRadius
                         ),
                         radius = maxRadius
                     )
                     
                     // The Ring System (Representation of Repeats vs New)
                     val ringWidth = 40.dp.toPx()
                     drawCircle(
                         brush = Brush.sweepGradient(
                             colors = listOf(
                                 coreColor,
                                 Color.White,
                                 coreColor.copy(alpha=0.5f),
                                 coreColor
                             )
                         ),
                         radius = maxRadius * 0.6f,
                         style = Stroke(width = ringWidth)
                     )
                     
                     // Gaps/Debris in ring based on Variety score
                     if (data.varietyScore > 50) {
                         drawCircle(
                            color = Color.Black.copy(alpha=0.3f),
                            radius = maxRadius * 0.6f,
                            style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 20f), 0f))
                         )
                     }
                 }
                 
                 // B. The Singularity (Black Hole Center) - Pure Black
                 drawCircle(
                     color = Color.Black,
                     radius = maxRadius * 0.25f,
                     center = Offset(centerX, centerY)
                 )
                 
                 // C. Photon Ring (Thin bright border around black hole)
                 drawCircle(
                     color = Color.White.copy(alpha=0.8f),
                     radius = maxRadius * 0.255f,
                     center = Offset(centerX, centerY),
                     style = Stroke(width = 2.dp.toPx())
                 )
            }
            
            // 2. Info Overlay
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                 DashboardHeader(
                    title = "THE HORIZON",
                    icon = Icons.Filled.AllInclusive, // Infinity symbol
                    iconColor = Color.White,
                    showShareButton = showShareButton,
                    onShareClick = onShareClick
                )
                
                Spacer(modifier = Modifier.weight(1f))
            
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                         Text(
                            text = "${data.newContentPercentage}%",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "NEW DISCOVERIES",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha=0.7f),
                            letterSpacing = 1.sp
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = data.discoveryType.uppercase(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = when(data.discoveryType) {
                                 "The Explorer" -> Color(0xFFA78BFA)
                                 "The Time Traveler" -> Color(0xFFFBD38D)
                                 else -> Color(0xFF67E8F9)
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if(data.topNewArtist != null) "Top find: ${data.topNewArtist}" else "Expanding horizons",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha=0.5f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

