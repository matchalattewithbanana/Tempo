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
fun DashboardNewObsessionCard(
    data: SpotlightCardData.NewObsession,
    showShareButton: Boolean = true,
    onShareClick: () -> Unit = {}
) {
    // Newsprint color palette
    val paperColor = Color(0xFFF5F0E6) // Aged paper
    val inkColor = Color(0xFF1A1A1A) // Deep ink black
    val accentRed = Color(0xFFB91C1C) // Classic newspaper red
    
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        backgroundColor = paperColor,
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row with masthead styling
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(inkColor)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "THE DAILY ROTATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = paperColor,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Est. ${data.daysKnown} days ago",
                        style = MaterialTheme.typography.labelSmall,
                        color = paperColor.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                }
                
                if (showShareButton) {
                    IconButton(
                        onClick = onShareClick,
                        modifier = Modifier
                            .size(32.dp)
                            .background(paperColor.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = paperColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            // Breaking News Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(accentRed)
                    .padding(vertical = 6.dp)
            ) {
                Text(
                    text = "★ BREAKING: NEW OBSESSION DETECTED ★",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Main Content Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Artist Image with halftone effect (SINGLE IMAGE LOAD)
                Box(
                    modifier = Modifier
                        .weight(0.45f)
                        .aspectRatio(0.85f)
                        .clip(RoundedCornerShape(4.dp))
                        .border(2.dp, inkColor, RoundedCornerShape(4.dp))
                ) {
                    if (data.artistImageUrl != null) {
                        // Single image with newsprint desaturation
                        CachedAsyncImage(
                            imageUrl = data.artistImageUrl,
                            contentDescription = data.artistName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            colorFilter = ColorFilter.colorMatrix(
                                ColorMatrix().apply { 
                                    setToSaturation(0.15f) // Low saturation for vintage look
                                }
                            )
                        )
                    }
                    
                    // Halftone Dot Overlay (newsprint texture)
                    Canvas(modifier = Modifier.fillMaxSize().alpha(0.15f)) {
                        val dotSpacing = 4.dp.toPx()
                        val maxDotSize = 2.dp.toPx()
                        
                        for (x in 0 until size.width.toInt() step dotSpacing.toInt()) {
                            for (y in 0 until size.height.toInt() step dotSpacing.toInt()) {
                                // Vary dot size slightly for authentic halftone
                                val variation = ((x + y) % 3) * 0.3f
                                drawCircle(
                                    color = inkColor,
                                    radius = maxDotSize * (0.5f + variation),
                                    center = Offset(x.toFloat(), y.toFloat())
                                )
                            }
                        }
                    }
                    
                    // Photo credit line
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(inkColor.copy(alpha = 0.7f))
                            .padding(4.dp)
                    ) {
                        Text(
                            text = "Photo: Archive",
                            style = MaterialTheme.typography.labelSmall,
                            color = paperColor,
                            fontSize = 8.sp
                        )
                    }
                }
                
                // Article Content
                Column(
                    modifier = Modifier.weight(0.55f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Artist Name as Headline
                    Text(
                        text = data.artistName.uppercase(),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                            fontWeight = FontWeight.Black,
                            lineHeight = 28.sp
                        ),
                        color = inkColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Divider line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(inkColor)
                    )
                    
                    // Stats as article excerpt
                    Text(
                        text = "Sources confirm this artist has captured ${data.percentOfListening}% of total listening time, marking an unprecedented rise in the charts.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                            lineHeight = 16.sp
                        ),
                        color = inkColor.copy(alpha = 0.8f)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Play count as bold stat
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(inkColor)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PLAYS",
                            style = MaterialTheme.typography.labelSmall,
                            color = paperColor,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${data.playCount}",
                            style = MaterialTheme.typography.titleLarge,
                            color = paperColor,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
            
            // Footer with torn edge effect
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            ) {
                // Torn paper edge (jagged line)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = Path().apply {
                        moveTo(0f, size.height)
                        var x = 0f
                        while (x < size.width) {
                            val peakHeight = (5..15).random().toFloat()
                            lineTo(x + 8f, size.height - peakHeight)
                            lineTo(x + 16f, size.height)
                            x += 16f
                        }
                        lineTo(size.width, 0f)
                        lineTo(0f, 0f)
                        close()
                    }
                    drawPath(path, color = paperColor)
                }
                
                // Page number / edition
                Text(
                    text = "Page A1 • Music Section",
                    style = MaterialTheme.typography.labelSmall,
                    color = inkColor.copy(alpha = 0.5f),
                    fontSize = 9.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 4.dp)
                )
            }
        }
    }
}

// 6. Listening Peak (Harmonic Resonance / Generative Line Art)
@Composable
fun DashboardListeningPeakCard(
    data: SpotlightCardData.ListeningPeak,
    showShareButton: Boolean = true,
    onShareClick: () -> Unit = {}
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        backgroundColor = TempoDarkBackground, // Deep Void
        contentPadding = PaddingValues(24.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
            // 1. Background - Pure Void (removed grid for cleaner look)

            // 2. Harmonic Waves (High Fidelity)
            // Increased density and smoother curves for "Polished" look
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                val numLines = 45 // Increased density
                val centerY = size.height * 0.55f // Slightly higher to account for displacement
                val spacing = 3.5.dp.toPx() // Tighter spacing
                
                val peakIntensity = (data.totalMinutes / 120f).coerceIn(0.8f, 1.3f) 
                
                for (i in 0 until numLines) {
                    val lineY = centerY - (numLines / 2f * spacing) + (i * spacing)
                    val progress = i / numLines.toFloat()
                    
                    // Calculate verticalFactor (Center lines are the "peak" subject)
                    // Ease the factor for smoother drop-off
                    val verticalFactor = 1f - abs(progress - 0.5f) * 2f 
                    val smoothFactor = verticalFactor.pow(1.5f)

                    val path = Path()
                    path.moveTo(0f, lineY)
                    
                    val steps = 180 // Smoother curves
                    val stepX = size.width / steps
                    
                    for (j in 0..steps) {
                        val x = j * stepX
                        val normX = j / steps.toFloat()
                        
                        // Main Peak (Gaussian)
                        val centerMean = 0.5f
                        val stdDev = 0.12f // Tighter peak
                        val gaussian = exp(-0.5 * ((normX - centerMean) / stdDev).pow(2)).toFloat()
                        
                        // Harmonics (Secondary waves) for "Resonance" feel
                        val harmonic1 = sin(normX * PI.toFloat() * 4f + (i * 0.2f)) * 0.1f // Slow wave
                        val harmonic2 = sin(normX * 50f + i) * 0.02f // Fine noise
                        
                        // Combined displacement
                        val displacement = -1 * (gaussian + harmonic1 + harmonic2) * 70.dp.toPx() * peakIntensity * smoothFactor
                        
                        path.lineTo(x, lineY + displacement)
                    }
                    
                    // Polished Gradient Stroke
                    // Center lines are brighter/blue, edges darker/purple
                    val alpha = (0.3f + (smoothFactor * 0.7f)).coerceIn(0f, 1f)
                    
                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF0F172A).copy(alpha = 0f), // Transparent edges
                                Color(0xFF38BDF8), // Sky Blue
                                Color(0xFF818CF8), // Indigo
                                Color(0xFFC084FC), // Purple
                                Color(0xFF0F172A).copy(alpha = 0f)
                            )
                        ),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = (1.2f + (smoothFactor * 0.5f)).dp.toPx(), // Varies slightly with depth
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        ),
                        alpha = alpha
                    )
                }
            }

            // 3. Foreground Content - Refined Typography
            Column(modifier = Modifier.fillMaxWidth()) {
                DashboardHeader(
                    icon = Icons.Outlined.GraphicEq,
                    title = "Harmonic Resonance",
                    iconColor = Color(0xFF38BDF8),
                    showShareButton = showShareButton,
                    onShareClick = onShareClick
                )
                
                Spacer(modifier = Modifier.weight(1f))
            
                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    // Refined Peak Time
                    Row(verticalAlignment = Alignment.Bottom) {
                         Text(
                            text = data.peakTimeRange,
                            style = MaterialTheme.typography.displaySmall.copy( // Smaller but refined
                                fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                                fontWeight = FontWeight.Light,
                                letterSpacing = (-0.5).sp
                            ),
                            color = Color.White
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Minimalist Intensity Display
                    Row(
                        modifier = Modifier.fillMaxWidth(), 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "INTENSITY",
                            style = MaterialTheme.typography.labelSmall,
                            color = GlassWhite.copy(alpha=0.5f),
                            letterSpacing = 3.sp,
                            fontSize = 10.sp
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))

                        // Styled Value
                        Text(
                            text = "${data.totalMinutes} MIN",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// 7. Early Adopter -> "Interstellar Passport" (Sci-Fi / Space ID)

// 7. Early Adopter -> "Interstellar Passport" (Sci-Fi / Space ID)
@Composable
fun DashboardEarlyAdopterCard(
    data: SpotlightCardData.EarlyAdopter,
    showShareButton: Boolean = true,
    onShareClick: () -> Unit = {}
) {
    var showQrDialog by remember { mutableStateOf(false) }

    if (showQrDialog) {
        // Safe encoding for generic search
        val encodedArtist = try {
            java.net.URLEncoder.encode(data.artistName, "UTF-8")
        } catch(e: Exception) { data.artistName }
        
        val qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=500x500&color=38BDF8&bgcolor=020617&data=https%3A%2F%2Fopen.spotify.com%2Fsearch%2F$encodedArtist"
        
        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            containerColor = Color(0xFF020617),
            textContentColor = Color(0xFFE2E8F0),
            titleContentColor = Color(0xFFE2E8F0),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.border(1.dp, Color(0xFF38BDF8).copy(alpha=0.3f), RoundedCornerShape(24.dp)),
            title = {
                Text(
                    "ACCESS VERIFICATION", 
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                         modifier = Modifier
                             .size(240.dp)
                             .clip(RoundedCornerShape(12.dp))
                             .border(1.dp, Color(0xFF38BDF8).copy(alpha=0.5f), RoundedCornerShape(12.dp))
                             .background(Color.Black), // Contrast for QR
                         contentAlignment = Alignment.Center
                    ) {
                        // Scanline effect background
                         androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().alpha(0.1f)) {
                            val lineH = 2.dp.toPx()
                            val gap = 2.dp.toPx()
                            for(i in 0 until (size.height / (lineH+gap)).toInt()) {
                                drawRect(Color(0xFF38BDF8), topLeft = Offset(0f, i*(lineH+gap)), size = Size(size.width, lineH))
                            }
                         }
                         
                        CachedAsyncImage(
                            imageUrl = qrUrl,
                            contentDescription = "QR Code",
                            modifier = Modifier.size(220.dp).padding(10.dp),
                            contentScale = ContentScale.Fit
                        )
                        
                        // Corner brackets
                        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                             // Top Left
                             Box(modifier = Modifier.align(Alignment.TopStart).size(20.dp, 2.dp).background(Color(0xFF38BDF8)))
                             Box(modifier = Modifier.align(Alignment.TopStart).size(2.dp, 20.dp).background(Color(0xFF38BDF8)))
                             // Bottom Right
                             Box(modifier = Modifier.align(Alignment.BottomEnd).size(20.dp, 2.dp).background(Color(0xFF38BDF8)))
                             Box(modifier = Modifier.align(Alignment.BottomEnd).size(2.dp, 20.dp).background(Color(0xFF38BDF8)))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "SCAN FOR ACCESS",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 2.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showQrDialog = false }) {
                    Text("CLOSE", color = Color(0xFF38BDF8))
                }
            }
        )
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp), // Smooth curve like a modern ID
        backgroundColor = Color(0xFF020617), // Deep Space Blue/Black
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        ) {
            // 1. Background Atmosphere (Nebula)
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width * 0.8f, size.height * 0.2f)
                // Nebula Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF6366F1).copy(alpha = 0.3f), // Indigo
                            Color(0xFF8B5CF6).copy(alpha = 0.1f), // Violet
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.width * 0.6f
                    ),
                    center = center,
                    radius = size.width * 0.6f
                )
                
                // Starfield
                val randomSeed = Random(data.artistName.hashCode())
                for (i in 0..30) {
                    val x = randomSeed.nextFloat() * size.width
                    val y = randomSeed.nextFloat() * size.height
                    val starSize = randomSeed.nextFloat() * 2.dp.toPx()
                    val opacity = randomSeed.nextFloat() * 0.8f
                    drawCircle(
                        color = Color.White.copy(alpha=opacity),
                        radius = starSize / 2,
                        center = Offset(x, y)
                    )
                }
                
                // Tech Grid / Overlay Data
                val gridColor = Color(0xFF94A3B8).copy(alpha = 0.1f)
                val lineH = size.height * 0.75f
                drawLine(gridColor, Offset(0f, lineH), Offset(size.width, lineH), strokeWidth = 1.dp.toPx())
                drawLine(gridColor, Offset(size.width * 0.35f, 0f), Offset(size.width * 0.35f, size.height), strokeWidth = 1.dp.toPx())
            }

            // 2. Content Layout
            Row(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                // Left Column: Photo & ID
                Column(
                    modifier = Modifier.weight(0.35f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Passport Photo Frame
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.8f) // 4:5 Photo ratio
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(4.dp)) // Silver Border
                            .background(Color(0xFF1E293B))
                    ) {
                         if (data.artistImageUrl != null) {
                             CachedAsyncImage(
                                 imageUrl = data.artistImageUrl,
                                 contentDescription = null,
                                 modifier = Modifier.fillMaxSize(),
                                 contentScale = ContentScale.Crop,
                                 colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.2f) }) // Desaturated Sci-Fi look
                             )
                         }
                         // "Holo" Overlay on photo
                         Box(modifier = Modifier.fillMaxSize().background(
                             brush = Brush.linearGradient(
                                 colors = listOf(
                                     Color.Transparent,
                                     Color(0xFF38BDF8).copy(alpha=0.2f),
                                     Color.Transparent
                                 ),
                                 start = Offset(0f, 0f),
                                 end = Offset(100f, 100f) // Approximate diagonal
                             )
                         ))
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // ID Barcode (Fake visual) - CLICKABLE TO SHOW QR
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .clickable { showQrDialog = true }, // Interaction added
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        repeat(10) {
                             Box(modifier = Modifier.width(Random.nextInt(2, 6).dp).fillMaxHeight().background(Color(0xFF475569)))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(20.dp))
                
                // Right Column: Data Fields
                Column(
                    modifier = Modifier.weight(0.65f).fillMaxHeight()
                ) {
                    // Header
                    Text(
                        text = "TEMPO VANGUARD",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8), // Slate 400
                        letterSpacing = 2.sp,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "ACCESS PASS",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFF8FAFC), // Slate 50
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Field: SUBJECT
                    Text("SUBJECT", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B), fontSize = 8.sp)
                    Text(
                        text = data.artistName.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFE2E8F0),
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        maxLines = 1
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Field: CLASS
                    Text("CLASS", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B), fontSize = 8.sp)
                    Text(
                        text = "EARLY ADOPTER (FOUNDER)", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = Color(0xFFA7F3D0), // Soft Green/Teal
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Field: DISCOVERY / ISSUED
                    Text("FIRST CONTACT", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B), fontSize = 8.sp)
                    Text(
                        text = data.discoveryDate, 
                        style = MaterialTheme.typography.bodySmall, 
                        color = Color(0xFFE2E8F0),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
            
            // 3. Stamped Verification Seal (Overlaid)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
                    .rotate(-15f)
            ) {
                // We draw a custom "Rubber Stamp" effect
                 androidx.compose.foundation.Canvas(modifier = Modifier.size(100.dp)) {
                     val color = Color(0xB3F43F5E) // Rose 500, 70% opacity
                     
                     // Outer Ring
                     drawCircle(color = color, style = Stroke(width = 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f), 0f)))
                     // Inner Ring
                     drawCircle(color = color, radius = size.minDimension/2 * 0.9f, style = Stroke(width = 1.dp.toPx()))
                     
                     // Text
                     drawContext.canvas.nativeCanvas.apply {
                         val paint = android.graphics.Paint().apply {
                             this.color = android.graphics.Color.parseColor("#F43F5E")
                             textSize = 12.dp.toPx()
                             textAlign = android.graphics.Paint.Align.CENTER
                             typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                             alpha = 180
                         }
                         drawText("APPROVED",center.x, center.y + 4.dp.toPx(), paint)
                     }
                 }
            }
            
            // Share Button (Top Right) -> Now acts as PRIMARY QR Trigger too if desired, or keep as Share
            // User asked: "when clicked on that icon" -> Top Right Icon is Icons.Outlined.QrCode2
            IconButton(
                 onClick = { showQrDialog = true }, // Trigger QR Dialog
                 modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
             ) {
                 Icon(
                     imageVector = Icons.Outlined.QrCode2, // Techy icon
                     contentDescription = "Show QR Code",
                     tint = Color.White.copy(alpha=0.5f),
                     modifier = Modifier.size(20.dp)
                 )
             }
        }
    }
}

// 8. Artist Loyalty -> "The Deep Cut" (Vinyl Stack)
