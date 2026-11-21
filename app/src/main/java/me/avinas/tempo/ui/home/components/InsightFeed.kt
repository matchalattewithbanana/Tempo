@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.avinas.tempo.ui.home.components

import androidx.compose.animation.core.*
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextOverflow
import me.avinas.tempo.data.stats.InsightCardData
import me.avinas.tempo.data.stats.InsightType
import me.avinas.tempo.data.stats.InsightPayload
import me.avinas.tempo.data.stats.DiscoveryTrend
import me.avinas.tempo.data.stats.HourlyDistribution
import me.avinas.tempo.ui.components.GlassCard
import me.avinas.tempo.ui.components.GlassCardVariant
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.res.stringResource
import me.avinas.tempo.R
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.ContentScale
import me.avinas.tempo.ui.components.CachedAsyncImage
import me.avinas.tempo.ui.components.FrostedIconButton
import me.avinas.tempo.ui.theme.*


@Composable
fun ConstellationWeb(
    insights: List<InsightCardData>,
    selectedType: InsightType?,
    onTypeSelected: (InsightType?) -> Unit,
    tempoBpm: Float = 100f,
    modifier: Modifier = Modifier
) {
    val categories = remember(insights) {
        insights.map { it.type }.distinct().take(6)
    }
    
    if (categories.isEmpty()) return

    val orbitAngle = 0f
    val pulseAnim = 1f
    val packetFraction = 0f

    // Snap offsets for interactive drag snapping
    val nodeOffsets = remember(categories) {
        categories.associateWith { Animatable(Offset.Zero, Offset.VectorConverter) }
    }

    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        val angles = remember(categories.size) {
            categories.mapIndexed { index, _ ->
                (index * (2f * Math.PI) / categories.size)
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val w = maxWidth
            val h = maxHeight
            val cx = w / 2
            val cy = h / 2
            val radius = (minOf(w, h) / 2) * 0.65f
            val localDensity = androidx.compose.ui.platform.LocalDensity.current
            val nodeRadiusPx = remember(localDensity) { with(localDensity) { 24.dp.toPx() } }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val cwPx = size.width
                val chPx = size.height
                val cxPx = cwPx / 2
                val cyPx = chPx / 2
                val radiusPx = (minOf(cwPx, chPx) / 2) * 0.65f

                // Web lines
                for (r in listOf(0.4f, 0.75f, 1.0f)) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.02f),
                        radius = radiusPx * r,
                        center = Offset(cxPx, cyPx),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Draw central throb Vibe Core
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(InsightDanceability.copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(cxPx, cyPx),
                        radius = 32.dp.toPx() * pulseAnim
                    ),
                    center = Offset(cxPx, cyPx),
                    radius = 32.dp.toPx() * pulseAnim
                )
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = Offset(cxPx, cyPx)
                )
                drawCircle(
                    color = InsightDanceability,
                    radius = 5.dp.toPx(),
                    center = Offset(cxPx, cyPx)
                )

                // Elastic connectors
                val resolvedPositions = categories.mapIndexed { index, type ->
                    val angle = angles[index] + orbitAngle.toDouble()
                    val baseOffset = Offset(
                        x = (cxPx + radiusPx * kotlin.math.cos(angle)).toFloat(),
                        y = (cyPx + radiusPx * kotlin.math.sin(angle)).toFloat()
                    )
                    val dragOffsetPx = Offset(
                        x = nodeOffsets[type]?.value?.x?.dp?.toPx() ?: 0f,
                        y = nodeOffsets[type]?.value?.y?.dp?.toPx() ?: 0f
                    )
                    baseOffset + dragOffsetPx
                }

                for (i in resolvedPositions.indices) {
                    val p1 = resolvedPositions[i]
                    val p2 = resolvedPositions[(i + 1) % resolvedPositions.size]
                    
                    drawLine(
                        color = InsightDanceability.copy(alpha = 0.18f),
                        start = p1,
                        end = p2,
                        strokeWidth = 1.5.dp.toPx()
                    )

                    // Draw radial spokes from center Vibe Core to nodes
                    val isSelectedNode = selectedType == categories[i]
                    val connectorColor = if (isSelectedNode) {
                        val nodeColor = when(categories[i]) {
                            InsightType.MOOD -> me.avinas.tempo.ui.theme.InsightMood
                            InsightType.PEAK_TIME -> me.avinas.tempo.ui.theme.InsightPeakTime
                            InsightType.BINGE -> me.avinas.tempo.ui.theme.InsightBinge
                            InsightType.DISCOVERY -> me.avinas.tempo.ui.theme.InsightDiscovery
                            InsightType.ENERGY -> me.avinas.tempo.ui.theme.InsightEnergy
                            InsightType.DANCEABILITY -> me.avinas.tempo.ui.theme.InsightDanceability
                            InsightType.TEMPO -> me.avinas.tempo.ui.theme.InsightTempo
                            InsightType.ACOUSTICNESS -> me.avinas.tempo.ui.theme.InsightAcousticness
                            InsightType.STREAK -> me.avinas.tempo.ui.theme.InsightStreak
                            InsightType.GENRE -> me.avinas.tempo.ui.theme.InsightGenre
                            InsightType.ENGAGEMENT -> me.avinas.tempo.ui.theme.InsightEngagement
                            else -> Color.Gray
                        }
                        nodeColor.copy(alpha = 0.4f)
                    } else {
                        Color.White.copy(alpha = 0.04f)
                    }
                    val spokeWidth = if (isSelectedNode) 2.dp.toPx() else 1.dp.toPx()

                    drawLine(
                        color = connectorColor,
                        start = Offset(cxPx, cyPx),
                        end = p1,
                        strokeWidth = spokeWidth
                    )
                }

                // If a node is selected, render traveling packet
                selectedType?.let { type ->
                    val selIndex = categories.indexOf(type)
                    if (selIndex != -1 && selIndex < resolvedPositions.size) {
                        val targetPos = resolvedPositions[selIndex]
                        val startPos = Offset(cxPx, cyPx)
                        val packetX = startPos.x + (targetPos.x - startPos.x) * packetFraction
                        val packetY = startPos.y + (targetPos.y - startPos.y) * packetFraction
                        val packetPos = Offset(packetX, packetY)

                        val nodeColor = when(type) {
                            InsightType.MOOD -> me.avinas.tempo.ui.theme.InsightMood
                            InsightType.PEAK_TIME -> me.avinas.tempo.ui.theme.InsightPeakTime
                            InsightType.BINGE -> me.avinas.tempo.ui.theme.InsightBinge
                            InsightType.DISCOVERY -> me.avinas.tempo.ui.theme.InsightDiscovery
                            InsightType.ENERGY -> me.avinas.tempo.ui.theme.InsightEnergy
                            InsightType.DANCEABILITY -> me.avinas.tempo.ui.theme.InsightDanceability
                            InsightType.TEMPO -> me.avinas.tempo.ui.theme.InsightTempo
                            InsightType.ACOUSTICNESS -> me.avinas.tempo.ui.theme.InsightAcousticness
                            InsightType.STREAK -> me.avinas.tempo.ui.theme.InsightStreak
                            InsightType.GENRE -> me.avinas.tempo.ui.theme.InsightGenre
                            InsightType.ENGAGEMENT -> me.avinas.tempo.ui.theme.InsightEngagement
                            else -> Color.Gray
                        }

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(nodeColor.copy(alpha = 0.8f), Color.Transparent),
                                center = packetPos,
                                radius = 14.dp.toPx()
                            ),
                            center = packetPos,
                            radius = 14.dp.toPx()
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3.5.dp.toPx(),
                            center = packetPos
                        )
                    }
                }
            }

            // Interactive node targets placed at matching coordinates
            categories.forEachIndexed { index, type ->
                val angle = angles[index] + orbitAngle.toDouble()
                val nodeCenterX = cx + radius * kotlin.math.cos(angle).toFloat()
                val nodeCenterY = cy + radius * kotlin.math.sin(angle).toFloat()

                val dragOffset = nodeOffsets[type]?.value ?: Offset.Zero
                val isSelected = selectedType == type

                val nodeColor = when(type) {
                    InsightType.MOOD -> me.avinas.tempo.ui.theme.InsightMood
                    InsightType.PEAK_TIME -> me.avinas.tempo.ui.theme.InsightPeakTime
                    InsightType.BINGE -> me.avinas.tempo.ui.theme.InsightBinge
                    InsightType.DISCOVERY -> me.avinas.tempo.ui.theme.InsightDiscovery
                    InsightType.ENERGY -> me.avinas.tempo.ui.theme.InsightEnergy
                    InsightType.DANCEABILITY -> me.avinas.tempo.ui.theme.InsightDanceability
                    InsightType.TEMPO -> me.avinas.tempo.ui.theme.InsightTempo
                    InsightType.ACOUSTICNESS -> me.avinas.tempo.ui.theme.InsightAcousticness
                    InsightType.STREAK -> me.avinas.tempo.ui.theme.InsightStreak
                    InsightType.GENRE -> me.avinas.tempo.ui.theme.InsightGenre
                    InsightType.ENGAGEMENT -> me.avinas.tempo.ui.theme.InsightEngagement
                    else -> Color.Gray
                }

                Column(
                    modifier = Modifier
                        .offset(
                            x = nodeCenterX - 40.dp + dragOffset.x.dp,
                            y = nodeCenterY - 36.dp + dragOffset.y.dp
                        )
                        .width(80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = type.name.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(nodeColor.copy(alpha = if (isSelected) 0.4f else 0.15f), Color.Transparent),
                                    radius = nodeRadiusPx
                                )
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color.White else nodeColor.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                            .pointerInput(type) {
                                detectDragGestures(
                                    onDragStart = {
                                        onTypeSelected(type)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        coroutineScope.launch {
                                            val newX = dragOffset.x + dragAmount.x.toDp().value
                                            val newY = dragOffset.y + dragAmount.y.toDp().value
                                            nodeOffsets[type]?.snapTo(Offset(newX, newY))
                                        }
                                    },
                                    onDragEnd = {
                                        coroutineScope.launch {
                                            nodeOffsets[type]?.animateTo(
                                                targetValue = Offset.Zero,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            )
                                        }
                                    },
                                    onDragCancel = {
                                        coroutineScope.launch {
                                            nodeOffsets[type]?.animateTo(
                                                targetValue = Offset.Zero,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                )
                                            )
                                        }
                                    }
                                )
                            }
                            .clickable {
                                if (selectedType == type) onTypeSelected(null) else onTypeSelected(type)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when(type) {
                            InsightType.MOOD -> Icons.Filled.Face
                            InsightType.PEAK_TIME -> Icons.Filled.DateRange
                            InsightType.BINGE -> Icons.Filled.Bolt
                            InsightType.DISCOVERY -> Icons.Filled.Celebration
                            InsightType.ENERGY -> Icons.Filled.Bolt
                            InsightType.DANCEABILITY -> Icons.Filled.Celebration
                            InsightType.TEMPO -> Icons.Filled.Speed
                            InsightType.ACOUSTICNESS -> Icons.Filled.Piano
                            InsightType.STREAK -> Icons.Filled.LocalFireDepartment
                            InsightType.GENRE -> Icons.AutoMirrored.Filled.QueueMusic
                            InsightType.ENGAGEMENT -> Icons.Filled.Favorite
                            else -> Icons.Filled.Settings
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = type.name,
                            tint = if (isSelected) Color.White else nodeColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Text(
            text = if (selectedType == null) stringResource(R.string.constellation_hint) else stringResource(R.string.constellation_active, selectedType.name.replace("_", " ")),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
            letterSpacing = 1.2.sp
        )
    }
}



@Composable
fun InsightFeed(
    insights: List<InsightCardData>,
    onNavigateToTrack: (Long) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val nonGamification = remember(insights) {
        insights.filter { it.payload !is InsightPayload.GamificationProgress }
    }

    var selectedType by remember(nonGamification) {
        mutableStateOf<InsightType?>(nonGamification.firstOrNull()?.type)
    }

    val selectedInsight = remember(nonGamification, selectedType) {
        nonGamification.find { it.type == selectedType }
    }

    val avgTempo = remember(nonGamification) {
        val tempoInsight = nonGamification.find { it.type == InsightType.TEMPO }
        val payload = tempoInsight?.payload
        if (payload is InsightPayload.TempoValue) payload.bpm else 100f
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ConstellationWeb(
            insights = nonGamification,
            selectedType = selectedType,
            onTypeSelected = { selectedType = it },
            tempoBpm = avgTempo
        )

        Crossfade(
            targetState = selectedInsight,
            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>(),
            label = "deck_transition"
        ) { insight ->
            if (insight != null) {
                InsightCard(
                    insight = insight,
                    selectedType = selectedType,
                    onCloseClick = { selectedType = null },
                    onClick = {},
                    onNavigateToArtist = onNavigateToArtist,
                    onNavigateToTrack = onNavigateToTrack
                )
            }
        }
    }
}

@Composable
fun VibeHeader(
    energy: Float,
    valence: Float,
    userName: String,
    profileImagePath: String? = null,
    isNewUser: Boolean = false,
    userLevel: Int? = null,
    levelProgress: Float = 0f,
    levelTitle: String? = null,
    isGamificationEnabled: Boolean = true,
    onLevelClick: () -> Unit = {},
    onSettingsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp)
    ) {
        val avatarSize = 48.dp
        val rowPaddingH = 14.dp
        val rowPaddingV = 10.dp
        val itemSpacing = 12.dp
        val nameTextStyle = MaterialTheme.typography.titleMedium
        val levelNumStyle = MaterialTheme.typography.titleSmall
        val titleStyle = MaterialTheme.typography.labelMedium

        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Premium Opaque Card for Profile - Minimalist Pill Edition
            Surface(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clickable(
                        enabled = isGamificationEnabled,
                        onClick = onLevelClick
                    )
                    .defaultMinSize(minHeight = if (!levelTitle.isNullOrBlank()) 72.dp else 64.dp)
                    .border(
                        width = 1.dp,
                        color = colors.outlineVariant.copy(alpha = 0.65f),
                        shape = MaterialTheme.shapes.extraLarge,
                    ),
                color = colors.surfaceContainerHigh,
                contentColor = colors.onSurface,
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = rowPaddingH, vertical = rowPaddingV),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(itemSpacing)
                ) {
                    // Left: Adaptive Level Ring + Avatar
                    Box(
                        modifier = Modifier.size(avatarSize),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isGamificationEnabled && userLevel != null) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidth = 3.dp.toPx()
                                val radius = (size.minDimension - strokeWidth) / 2
                                val topLeft = Offset(
                                    (size.width - radius * 2) / 2,
                                    (size.height - radius * 2) / 2
                                )
                                val arcSize = Size(radius * 2, radius * 2)
                                
                                drawArc(
                                    color = colors.surfaceContainerLowest,
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    brush = Brush.sweepGradient(
                                        listOf(
                                            colors.primary,
                                            colors.tertiary,
                                            colors.secondary,
                                            colors.primary,
                                        )
                                    ),
                                    startAngle = -90f,
                                    sweepAngle = 360f * levelProgress,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }
                        }
                        
                        // Avatar image or text letter inside (with a small margin so it sits inside the ring)
                        val innerAvatarSize = if (isGamificationEnabled && userLevel != null) avatarSize - 8.dp else avatarSize
                        Box(
                            modifier = Modifier
                                .size(innerAvatarSize)
                                .clip(CircleShape)
                                .background(colors.surfaceContainerLow)
                                .border(1.dp, colors.outlineVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (profileImagePath.isNullOrBlank()) {
                                Text(
                                    text = userName.firstOrNull()?.toString()?.uppercase() ?: "U",
                                    style = levelNumStyle.copy(lineHeight = levelNumStyle.fontSize * 1.1),
                                    fontWeight = FontWeight.Black,
                                    color = colors.onSurface,
                                    maxLines = 1
                                )
                            } else {
                                CachedAsyncImage(
                                    imageUrl = profileImagePath,
                                    contentDescription = "Profile image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .widthIn(min = 100.dp, max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(if (levelTitle.isNullOrBlank()) 0.dp else 2.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ResponsiveText(
                                text = userName,
                                style = nameTextStyle.copy(lineHeight = nameTextStyle.fontSize * 1.1f),
                                modifier = Modifier,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isGamificationEnabled) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = colors.tertiaryContainer,
                                    tonalElevation = 0.dp,
                                    shadowElevation = 0.dp
                                ) {
                                    Text(
                                        text = "LVL ${userLevel ?: 0}",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Black,
                                        color = colors.onTertiaryContainer,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        if (isGamificationEnabled && !levelTitle.isNullOrBlank()) {
                            ResponsiveText(
                                text = levelTitle.uppercase(),
                                style = titleStyle.copy(letterSpacing = 0.9.sp, lineHeight = titleStyle.fontSize * 1.15f),
                                color = colors.tertiary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            if (onSettingsClick != null) {
                FrostedIconButton(
                    icon = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.home_settings),
                    onClick = onSettingsClick
                )
            }
        }
    }
}

/**
 * Visualizes audio valence and energy metrics with animated wave bars and level gauges.
 */
@Composable
fun InsightCard(
    insight: InsightCardData,
    onCloseClick: (() -> Unit)? = null,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    selectedType: InsightType? = null,
    onNavigateToArtist: ((String) -> Unit)? = null,
    onNavigateToTrack: ((Long) -> Unit)? = null
) {
    val appearancePalette = LocalTempoPalette.current
    val (icon, color) = when(insight.type) {
        InsightType.MOOD -> Icons.Filled.Face to me.avinas.tempo.ui.theme.InsightMood
        InsightType.PEAK_TIME -> Icons.Filled.DateRange to me.avinas.tempo.ui.theme.InsightPeakTime
        InsightType.BINGE -> Icons.Filled.Bolt to me.avinas.tempo.ui.theme.InsightBinge
        InsightType.DISCOVERY -> Icons.Filled.Celebration to me.avinas.tempo.ui.theme.InsightDiscovery
        InsightType.ENERGY -> Icons.Filled.Bolt to me.avinas.tempo.ui.theme.InsightEnergy
        InsightType.DANCEABILITY -> Icons.Filled.Celebration to me.avinas.tempo.ui.theme.InsightDanceability
        InsightType.TEMPO -> Icons.Filled.Speed to me.avinas.tempo.ui.theme.InsightTempo
        InsightType.ACOUSTICNESS -> Icons.Filled.Piano to me.avinas.tempo.ui.theme.InsightAcousticness
        InsightType.STREAK -> Icons.Filled.LocalFireDepartment to me.avinas.tempo.ui.theme.InsightStreak
        InsightType.GENRE -> Icons.AutoMirrored.Filled.QueueMusic to me.avinas.tempo.ui.theme.InsightGenre
        InsightType.ENGAGEMENT -> Icons.Filled.Favorite to me.avinas.tempo.ui.theme.InsightEngagement
        InsightType.RATE_APP -> Icons.Filled.Star to GoldPrimary
        else -> Icons.Filled.Settings to Color.Gray
    }

    val rawCategory = when(insight.type) {
        InsightType.MOOD -> stringResource(R.string.insight_category_mood)
        InsightType.PEAK_TIME -> stringResource(R.string.insight_category_peak_time)
        InsightType.BINGE -> stringResource(R.string.insight_category_binge)
        InsightType.DISCOVERY -> stringResource(R.string.insight_category_discovery)
        InsightType.ENERGY -> stringResource(R.string.insight_category_energy)
        InsightType.DANCEABILITY -> stringResource(R.string.insight_category_danceability)
        InsightType.TEMPO -> stringResource(R.string.insight_category_tempo)
        InsightType.ACOUSTICNESS -> stringResource(R.string.insight_category_acousticness)
        InsightType.STREAK -> stringResource(R.string.insight_category_streak)
        InsightType.GENRE -> stringResource(R.string.insight_category_genre)
        InsightType.ENGAGEMENT -> stringResource(R.string.insight_category_engagement)
        else -> stringResource(R.string.insight_category_default)
    }

    // Clean category prefix: extract specific label after "//" if present, so it never overflows
    val categoryPrefix = remember(rawCategory) {
        if (rawCategory.contains("//")) {
            rawCategory.substringAfter("//").trim().uppercase(java.util.Locale.getDefault())
        } else {
            rawCategory.uppercase(java.util.Locale.getDefault())
        }
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .premiumClickable(onClick = onClick, pressedScale = 0.985f),
        accentColor = color,
        accentStrength = 0.08f,
        variant = GlassCardVariant.Obsidian,
        shape = RoundedCornerShape(22.dp),
        borderColor = appearancePalette.border.copy(alpha = 0.45f),
        borderWidth = 0.8.dp,
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Masthead row: Icon Emblem + Kicker + Frosted close button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Circular icon emblem with ambient accent tint
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(appearancePalette.surfaceSunken)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(color.copy(alpha = 0.22f), Color.Transparent)
                            )
                        )
                        .border(1.dp, color.copy(alpha = 0.28f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Text(
                            text = categoryPrefix,
                            style = KickerSmall,
                            color = color,
                            letterSpacing = 1.0.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (onCloseClick != null) {
                    FrostedIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = stringResource(R.string.insight_close),
                        onClick = onCloseClick,
                        iconTint = appearancePalette.mutedText,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Editorial Serif Headline
            Text(
                text = insight.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp,
                    lineHeight = 26.sp
                ),
                color = appearancePalette.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                softWrap = true
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Context Narrative
            Text(
                text = insight.description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 20.sp
                ),
                color = appearancePalette.mutedText,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                softWrap = true
            )

            // Visualizer content
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.025f))
                    .border(0.6.dp, appearancePalette.border.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                when (val payload = insight.payload) {
                    is InsightPayload.MoodData -> {
                        MoodVisualizer(valence = payload.valence, energy = payload.energy, color = color)
                    }
                    is InsightPayload.PeakTimeData -> {
                        PeakTimeVisualizer(peakHour = payload.peakHour, hourlyDistribution = payload.hourlyDistribution, color = color)
                    }
                    is InsightPayload.BingeData -> {
                        BingeVisualizer(
                            artist = payload.artist,
                            playCount = payload.playCount,
                            durationMs = payload.durationMs,
                            color = color,
                            onArtistClick = onNavigateToArtist
                        )
                    }
                    is InsightPayload.DiscoveryData -> {
                        DiscoveryVisualizer(
                            newArtistsCount = payload.newArtistsCount,
                            trends = payload.trends,
                            color = color
                        )
                    }
                    is InsightPayload.StreakData -> {
                        StreakVisualizer(days = payload.days)
                    }
                    is InsightPayload.GenreData -> {
                        GenreVisualizer(genre = payload.genre, color = color)
                    }
                    is InsightPayload.FeatureValue -> {
                        if (payload.type == InsightType.ENGAGEMENT) {
                            EngagementVisualizer(value = payload.value, color = color)
                        } else {
                            val label = when(payload.type) {
                                InsightType.ENERGY -> "Energy"
                                InsightType.DANCEABILITY -> "Danceability"
                                InsightType.ACOUSTICNESS -> "Acousticness"
                                else -> "Feature Balance"
                            }
                            FeatureGaugeVisualizer(value = payload.value, color = color, label = label)
                        }
                    }
                    is InsightPayload.TempoValue -> {
                        TempoBPMVisualizer(bpm = payload.bpm, color = color)
                    }
                    else -> {
                        FallbackVisualizer(color = color)
                    }
                }
            }
        }
    }
}

@Composable
internal fun ResponsiveText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    maxLines: Int = 1,
    softWrap: Boolean = false,
    overflow: TextOverflow = TextOverflow.Clip
) {
    var fontSize by remember(text, style.fontSize, maxLines, softWrap) { mutableStateOf(style.fontSize) }
    var readyToDraw by remember(text, style.fontSize, maxLines, softWrap) { mutableStateOf(false) }

    Text(
        text = text,
        modifier = modifier.graphicsLayer { 
            alpha = if (readyToDraw) 1f else 0f 
        },
        style = style.copy(fontSize = fontSize),
        color = color,
        fontWeight = fontWeight,
        maxLines = maxLines,
        softWrap = softWrap,
        overflow = overflow,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.hasVisualOverflow && fontSize.value > 12f) {
                fontSize = (fontSize.value * 0.9f).sp
            } else {
                readyToDraw = true
            }
        }
    )
}

