package me.avinas.tempo.ui.home.components

import me.avinas.tempo.ui.effects.tempoAmbientBackdrop
import me.avinas.tempo.ui.effects.tempoTiltSurface
import me.avinas.tempo.ui.effects.tempoListMotion
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.avinas.tempo.ui.components.CachedAsyncImage
import me.avinas.tempo.data.stats.TimeRange
import me.avinas.tempo.ui.components.GlassCard
import me.avinas.tempo.ui.components.GlassCardVariant
import me.avinas.tempo.ui.components.TelemetryMiniCell
import me.avinas.tempo.ui.components.TrendLine
import me.avinas.tempo.ui.theme.*
import androidx.compose.ui.res.stringResource
import me.avinas.tempo.R
import me.avinas.tempo.ui.theme.premiumClickable
import androidx.compose.foundation.border
import java.util.Locale

@Composable
fun HeroCard(
    userName: String,
    listeningTime: String,
    periodLabel: String,
    timeChangePercent: Double,
    trendData: List<Float>,
    selectedRange: TimeRange,
    dailyLabels: List<String> = emptyList(),
    dailyAvgMs: Long = 0L,
    activeDays: Int = 0,
    totalDays: Int = 0,
    peakDayMs: Long = 0L,
    modifier: Modifier = Modifier
) {
    val appearancePalette = LocalTempoPalette.current
    var scrubbingTimeMs by remember { mutableStateOf<Long?>(null) }
    var scrubbingLabel by remember { mutableStateOf<String?>(null) }

    // Reset scrubbing state when time range changes
    LaunchedEffect(selectedRange, dailyLabels) {
        scrubbingTimeMs = null
        scrubbingLabel = null
    }

    val greeting = remember(userName) {
        me.avinas.tempo.utils.TempoCopyEngine.getHeroGreeting(userName)
    }

    val dailyAvgFormatted = me.avinas.tempo.ui.stats.displayStatsDuration(dailyAvgMs)
    val peakFormatted = me.avinas.tempo.ui.stats.displayStatsDuration(peakDayMs)

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        variant = GlassCardVariant.Obsidian,
        shape = me.avinas.tempo.ui.components.appearanceCardShape(),
        borderColor = appearancePalette.border.copy(alpha = 0.45f),
        borderWidth = 0.8.dp,
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Masthead kicker row with accent dot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(appearancePalette.accent)
                )
                Text(
                    text = periodLabel.uppercase(Locale.getDefault()),
                    style = KickerSmall,
                    color = appearancePalette.mutedText,
                    letterSpacing = 1.0.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Large Hero Metric with DisplayFontFamily
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = scrubbingTimeMs?.let { me.avinas.tempo.ui.stats.displayStatsDuration(it) } ?: listeningTime,
                    style = MaterialTheme.typography.displayMediumEmphasized,
                    color = appearancePalette.onSurface
                )

                if (scrubbingLabel != null) {
                    Text(
                        text = "• $scrubbingLabel",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = appearancePalette.mutedText,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Greeting & Delta pill context
            val isPositive = timeChangePercent >= 0
            val percentString = "${if (isPositive) "+" else ""}${timeChangePercent.toInt()}%"
            val comparisonColor = if (isPositive) TempoSuccessBright else TempoWarningBright
            val arrowIcon = if (isPositive) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown
            val comparisonText = when (selectedRange) {
                TimeRange.TODAY -> "vs yesterday"
                TimeRange.THIS_WEEK -> "vs last week"
                TimeRange.THIS_MONTH -> "vs last month"
                TimeRange.THIS_YEAR -> "vs last year"
                TimeRange.ALL_TIME -> ""
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(comparisonColor.copy(alpha = 0.14f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = arrowIcon,
                            contentDescription = null,
                            tint = comparisonColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = percentString,
                            style = MaterialTheme.typography.labelSmall,
                            color = comparisonColor,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                    }
                }

                if (comparisonText.isNotBlank()) {
                    Text(
                        text = comparisonText,
                        style = MaterialTheme.typography.bodySmall,
                        color = appearancePalette.mutedText,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            // Secondary Telemetry Metrics Row
            if (trendData.isNotEmpty()) {
                // Hairline separator
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.8.dp)
                        .background(appearancePalette.border.copy(alpha = 0.45f))
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TelemetryMiniCell(
                        label = "DAILY AVG",
                        value = dailyAvgFormatted,
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(0.8.dp)
                            .background(appearancePalette.border.copy(alpha = 0.45f))
                    )

                    TelemetryMiniCell(
                        label = "ACTIVE DAYS",
                        value = "$activeDays / $totalDays",
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(0.8.dp)
                            .background(appearancePalette.border.copy(alpha = 0.45f))
                    )

                    TelemetryMiniCell(
                        label = "PEAK DAY",
                        value = peakFormatted,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Chart Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                ) {
                    if (dailyLabels.isNotEmpty() && trendData.size == dailyLabels.size) {
                        InteractiveTrendLine(
                            dataPoints = trendData,
                            labels = dailyLabels,
                            modifier = Modifier.fillMaxSize(),
                            lineColor = me.avinas.tempo.ui.theme.TempoSecondary,
                            fillColor = me.avinas.tempo.ui.theme.TempoSecondary.copy(alpha = 0.2f),
                            strokeWidth = 3.dp,
                            onValueSelected = { value, label ->
                                scrubbingTimeMs = (value * 60_000).toLong()
                                scrubbingLabel = label
                            },
                            onSelectionCleared = {
                                scrubbingTimeMs = null
                                scrubbingLabel = null
                            }
                        )
                    } else {
                        TrendLine(
                            dataPoints = trendData,
                            modifier = Modifier.fillMaxSize(),
                            lineColor = me.avinas.tempo.ui.theme.TempoSecondary,
                            fillColor = me.avinas.tempo.ui.theme.TempoSecondary.copy(alpha = 0.2f),
                            strokeWidth = 3.dp
                        )
                    }
                }
                if (dailyLabels.isNotEmpty() && trendData.size == dailyLabels.size) {
                    TrendValues(dailyLabels, trendData, selectedRange)
                }
            }
            if (selectedRange == TimeRange.TODAY || selectedRange == TimeRange.THIS_WEEK) {
                Text(
                    text = stringResource(R.string.home_session_start_attribution),
                    style = CaptionSmall,
                    color = appearancePalette.mutedText
                )
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrendValues(labels: List<String>, points: List<Float>, range: TimeRange) {
    var expanded by rememberSaveable(range) { mutableStateOf(false) }
    var selectedIndex by rememberSaveable(range, labels) { mutableStateOf(0) }

    TextButton(onClick = { expanded = !expanded }) {
        Text(stringResource(if (expanded) R.string.home_trend_hide_values else R.string.home_trend_show_values))
    }
    if (expanded) {
        val index = selectedIndex.coerceIn(labels.indices)
        Text(
            text = stringResource(R.string.home_trend_value, labels[index], points[index].toLong()),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodyLarge,
            color = LocalTempoPalette.current.onSurface
        )
        if (labels.size > 1) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { selectedIndex = index - 1 }, enabled = index > 0) {
                    Text(stringResource(R.string.home_trend_previous))
                }
                TextButton(onClick = { selectedIndex = index + 1 }, enabled = index < labels.lastIndex) {
                    Text(stringResource(R.string.how_it_works_next))
                }
            }
        }
    }
}

private val SpotlightStoryGradient = listOf(
    TempoPrimary,       // Electric Teal
    Color(0xFF06B6D4),  // Bright Cyan
    Color(0xFF8B5CF6),  // Vivid Violet
    Color(0xFFEC4899),  // Neon Rose
    Color(0xFFF59E0B),  // Warm Amber
    TempoPrimary        // Loop back to teal
)

@Composable
private fun SpotlightRing(
    albumArtUrl: String?,
    viewed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (viewed) {
        ViewedSpotlightRing(
            albumArtUrl = albumArtUrl,
            onClick = onClick,
            modifier = modifier
        )
    } else {
        ActiveSpotlightRing(
            albumArtUrl = albumArtUrl,
            onClick = onClick,
            modifier = modifier
        )
    }
}

@Composable
private fun ActiveSpotlightRing(
    albumArtUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appearancePalette = LocalTempoPalette.current
    val storyAccessibilityLabel = stringResource(R.string.spotlight_view_story)
    val infiniteTransition = me.avinas.tempo.ui.theme.rememberMotionAwareInfiniteTransition(label = "spotlightRingTransition")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.48f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringPulse"
    )

    val sweepGradientBrush = remember {
        Brush.sweepGradient(SpotlightStoryGradient)
    }

    Box(
        modifier = modifier
            .size(62.dp)
            .semantics {
                contentDescription = storyAccessibilityLabel
                role = Role.Button
            }
            .premiumClickable(onClick = onClick, pressedScale = 0.94f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val outerStroke = 2.2.dp.toPx()
            val ringRadius = (size.minDimension - outerStroke) / 2f

            // Ambient radial aura/glow behind the ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        appearancePalette.accent.copy(alpha = pulseAlpha * 0.36f),
                        Color(0xFF8B5CF6).copy(alpha = pulseAlpha * 0.16f),
                        Color.Transparent
                    ),
                    radius = size.minDimension / 2f
                ),
                radius = size.minDimension / 2f,
                center = center
            )

            // Rotating multi-stop gradient story ring
            rotate(rotationAngle) {
                drawCircle(
                    brush = sweepGradientBrush,
                    style = Stroke(width = outerStroke),
                    radius = ringRadius,
                    center = center
                )
            }
        }

        // Inner circular avatar with intentional dark separator border
        SpotlightAvatarContent(
            albumArtUrl = albumArtUrl,
            modifier = Modifier.size(50.dp)
        )

        // Interactive "Play Story" badge
        val badgeGradient = remember {
            Brush.linearGradient(
                colors = listOf(
                    appearancePalette.accent,
                    Color(0xFF8B5CF6)
                )
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 1.dp, y = 1.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(appearancePalette.elevatedSurface)
                .padding(1.8.dp)
                .clip(CircleShape)
                .background(badgeGradient),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(11.dp)
                    .offset(x = 0.5.dp) // Optical center compensation for play triangle
            )
        }
    }
}

@Composable
private fun ViewedSpotlightRing(
    albumArtUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val storyAccessibilityLabel = stringResource(R.string.spotlight_view_story)
    Box(
        modifier = modifier
            .size(62.dp)
            .semantics {
                contentDescription = storyAccessibilityLabel
                role = Role.Button
            }
            .premiumClickable(onClick = onClick, pressedScale = 0.94f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val outerStroke = 1.2.dp.toPx()
            val ringRadius = (size.minDimension - outerStroke) / 2f

            // Calm, elegant, thin muted ring
            drawCircle(
                color = Color.White.copy(alpha = 0.18f),
                style = Stroke(width = outerStroke),
                radius = ringRadius,
                center = center
            )
        }

        // Inner circular avatar with intentional dark separator border
        SpotlightAvatarContent(
            albumArtUrl = albumArtUrl,
            modifier = Modifier.size(50.dp)
        )
    }
}

@Composable
private fun SpotlightAvatarContent(
    albumArtUrl: String?,
    modifier: Modifier = Modifier
) {
    val appearancePalette = LocalTempoPalette.current
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(appearancePalette.elevatedSurface)
            .border(2.dp, appearancePalette.elevatedSurface, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!albumArtUrl.isNullOrBlank()) {
            CachedAsyncImage(
                imageUrl = albumArtUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                targetSizeDp = 100
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                appearancePalette.accent.copy(alpha = 0.20f),
                                appearancePalette.elevatedSurface
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = appearancePalette.accent,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun SpotlightStoryCard(
    onClick: () -> Unit,
    onRingClick: () -> Unit,
    albumArtUrl: String? = null,
    storyAvailable: Boolean = true,
    storyViewed: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = me.avinas.tempo.ui.components.appearanceCardShape()
    // One tonal overlay follows every palette, including dynamic wallpaper
    // and Light / OLED modes. Key the brush to colors so live preview works.
    val tint = remember(colors.primary, colors.secondary, colors.tertiary) {
        Brush.linearGradient(
            listOf(
                colors.primary.copy(alpha = 0.15f),
                colors.secondary.copy(alpha = 0.07f),
                colors.tertiary.copy(alpha = 0.10f),
            ),
        )
    }
    Box(
        modifier = modifier.tempoTiltSurface().fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceContainerHigh)
            .background(tint)
            .border(1.dp, colors.outlineVariant.copy(alpha = 0.6f), shape)
            .premiumClickable(onClick = onClick, pressedScale = 0.98f)
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (storyAvailable) {
                SpotlightRing(
                    albumArtUrl = albumArtUrl,
                    viewed = storyViewed,
                    onClick = onRingClick,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_spotlight_title),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = colors.onSurface,
                    maxLines = 2,
                )
                Text(
                    text = stringResource(R.string.home_spotlight_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            // A static Material polygon is distinctive without a looping
            // animation or GPU blur pass on the Home scroll surface.
            Surface(
                modifier = Modifier.size(42.dp),
                shape = MaterialShapes.Cookie9Sided.toShape(),
                color = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

/**
 * An adaptive, tonal four-part listening digest. Content stays readable in
 * Light, Dark and OLED: no hardcoded white-on-translucent text.
 */
@Composable
fun WeekInReviewGrid(
    topArtistName: String?,
    topArtistImage: String?,
    topTrackName: String?,
    topTrackImage: String?,
    totalHours: String,
    newDiscoveries: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.home_week_review),
            style = MaterialTheme.typography.titleLargeEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val twoColumns = weekDigestColumns(maxWidth.value, androidx.compose.ui.platform.LocalDensity.current.fontScale) == 2
            val tiles: List<@Composable (Modifier) -> Unit> = listOf(
                { tile ->
                    WeekDigestTile(
                        label = stringResource(R.string.home_top_artist),
                        value = topArtistName ?: "—",
                        modifier = tile,
                        accent = MaterialTheme.colorScheme.tertiaryContainer,
                        accentContent = MaterialTheme.colorScheme.onTertiaryContainer,
                        imageUrl = topArtistImage,
                        icon = Icons.Rounded.Person,
                    )
                },
                { tile ->
                    WeekDigestTile(
                        label = stringResource(R.string.home_listen_time),
                        value = totalHours,
                        modifier = tile,
                        accent = MaterialTheme.colorScheme.primaryContainer,
                        accentContent = MaterialTheme.colorScheme.onPrimaryContainer,
                        icon = Icons.Default.Timer,
                    )
                },
                { tile ->
                    WeekDigestTile(
                        label = stringResource(R.string.desktop_stats_top_track),
                        value = topTrackName ?: "—",
                        modifier = tile,
                        accent = MaterialTheme.colorScheme.secondaryContainer,
                        accentContent = MaterialTheme.colorScheme.onSecondaryContainer,
                        imageUrl = topTrackImage,
                        icon = Icons.Rounded.MusicNote,
                    )
                },
                { tile ->
                    WeekDigestTile(
                        label = stringResource(R.string.home_new_finds),
                        value = newDiscoveries.toString(),
                        modifier = tile,
                        accent = MaterialTheme.colorScheme.tertiaryContainer,
                        accentContent = MaterialTheme.colorScheme.onTertiaryContainer,
                        icon = Icons.Default.Explore,
                    )
                },
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (twoColumns) {
                    tiles.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            pair.forEach { tile -> tile(Modifier.weight(1f)) }
                        }
                    }
                } else {
                    tiles.forEach { tile -> tile(Modifier.fillMaxWidth()) }
                }
            }
        }
    }
}

@Composable
private fun WeekDigestTile(
    label: String,
    value: String,
    modifier: Modifier,
    accent: Color,
    accentContent: Color,
    icon: ImageVector,
    imageUrl: String? = null,
) {
    Surface(
        modifier = modifier.heightIn(min = 142.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                shape = MaterialShapes.Cookie6Sided.toShape(),
                color = accent,
                contentColor = accentContent,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (!imageUrl.isNullOrBlank()) {
                        CachedAsyncImage(
                            imageUrl = imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(MaterialShapes.Cookie6Sided.toShape()),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
                    }
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun DiscoverySection(
    discoveryStats: me.avinas.tempo.data.stats.DiscoveryStats?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.home_ready_discover),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
        )
        
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val newArtists = discoveryStats?.newArtistsCount ?: 0
            val newTracks = discoveryStats?.newTracksCount ?: 0
            val varietyScore = ((discoveryStats?.varietyScore ?: 0.0) * 10).toInt()
            
            DiscoveryCard(
                title = stringResource(R.string.home_found_artists, newArtists),
                subtitle = stringResource(R.string.home_expand_horizon),
                icon = Icons.Default.Explore,
                color = TempoSky,
                backgroundColor = TempoInfo.copy(alpha = 0.2f)
            )
            
            DiscoveryCard(
                title = stringResource(R.string.home_discovered_tracks, newTracks),
                subtitle = stringResource(R.string.home_fresh_beats),
                icon = Icons.Default.History,
                color = TempoError,
                backgroundColor = TempoErrorDeep.copy(alpha = 0.2f)
            )
            
            DiscoveryCard(
                title = stringResource(R.string.home_variety_score, varietyScore),
                subtitle = stringResource(R.string.home_how_unique),
                icon = Icons.Default.Fingerprint,
                color = InsightMood,
                backgroundColor = InsightMood.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
fun DiscoveryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    backgroundColor: Color
) {
    Surface(
        modifier = Modifier.width(208.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(color.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HabitInsights(
    insights: List<HabitInsightData>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.home_habits_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
        )
        
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            insights.forEach { insight ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(insight.iconBgColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = insight.icon,
                                contentDescription = null,
                                tint = insight.iconColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = insight.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = insight.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

data class HabitInsightData(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconColor: Color,
    val iconBgColor: Color,
    val gradient: List<Color>
)
