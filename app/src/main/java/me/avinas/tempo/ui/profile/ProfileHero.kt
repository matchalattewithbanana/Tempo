package me.avinas.tempo.ui.profile

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import me.avinas.tempo.R
import me.avinas.tempo.data.local.entities.UserLevel
import me.avinas.tempo.ui.components.CachedAsyncImage
import me.avinas.tempo.ui.theme.*
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
internal fun ProfileRetryMessage(onRetry: () -> Unit, retryEnabled: Boolean) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.profile_load_failed),
            color = LocalTempoPalette.current.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        TextButton(onClick = onRetry, enabled = retryEnabled) {
            Text(stringResource(R.string.common_retry))
        }
    }
}

// These are layout decisions for Tempo, not official M3 breakpoint tokens.
// Keep narrow and accessibility typography behavior directly testable.
internal fun shouldStackProfileIdentity(compact: Boolean, fontScale: Float): Boolean =
    (compact && fontScale >= 1.3f) || fontScale >= 1.8f

internal fun shouldStackProfileMetrics(widthDp: Float, fontScale: Float): Boolean =
    widthDp < 320f || fontScale >= 1.3f

// ──────────────────────────────────────────────────────────────
// Hero Profile Section
// ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun HeroProfileSection(
    userLevel: UserLevel,
    userTitle: String,
    userName: String,
    profileImagePath: String?,
    tierAccent: Color,
    compact: Boolean = false,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = userLevel.levelProgress.coerceIn(0f, 1f),
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>(),
        label = "levelProgress",
    )
    val swatch = me.avinas.tempo.ui.components.tempoTileSwatch(
        me.avinas.tempo.ui.components.TempoTileTone.AQUA
    )
    val stackIdentity = shouldStackProfileIdentity(compact, LocalDensity.current.fontScale)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraExtraLarge,
        color = swatch.container,
        contentColor = swatch.foreground,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // The profile identity is the only hero: avoid abstract background
            // shapes, repeated level labels and a second nested progress card.
            if (stackIdentity) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    HeroAvatar(
                        progress = animatedProgress,
                        level = userLevel.currentLevel,
                        userName = userName,
                        profileImagePath = profileImagePath,
                        tierAccent = tierAccent,
                        compact = true,
                    )
                    ProfileIdentityText(
                        name = userName,
                        title = userTitle,
                        level = userLevel.currentLevel,
                        foreground = swatch.foreground,
                        centered = true,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
                ) {
                    HeroAvatar(
                        progress = animatedProgress,
                        level = userLevel.currentLevel,
                        userName = userName,
                        profileImagePath = profileImagePath,
                        tierAccent = tierAccent,
                        compact = compact,
                    )
                    Box(Modifier.weight(1f)) {
                        ProfileIdentityText(
                            name = userName,
                            title = userTitle,
                            level = userLevel.currentLevel,
                            foreground = swatch.foreground,
                        )
                    }
                }
            }
            // Progress is secondary information inside the hero, not another
            // nested card competing for attention.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.profile_bento_total_xp),
                    style = MaterialTheme.typography.labelMedium,
                    color = swatch.foreground,
                )
                Text(
                    text = String.format(Locale.getDefault(), "%,d", userLevel.totalXp),
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    color = swatch.foreground,
                )
            }
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth(),
                color = swatch.foreground,
                trackColor = swatch.foreground.copy(alpha = 0.16f),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${(animatedProgress * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    color = swatch.foreground,
                )
                Text(
                    text = String.format(Locale.getDefault(), "%,d XP to next level", userLevel.xpRemaining),
                    style = MaterialTheme.typography.labelSmall,
                    color = swatch.foreground,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProfileIdentityText(
    name: String,
    title: String,
    level: Int,
    foreground: Color,
    centered: Boolean = false,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleLargeEmphasized,
            fontWeight = FontWeight.Bold,
            color = foreground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        )
        Text(
            text = if (title.isNotBlank()) "Level $level · $title" else "Level $level",
            style = MaterialTheme.typography.labelMedium,
            color = foreground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        )
    }
}

@Composable
private fun InnerAvatarCircle(
    innerSize: Dp,
    tierAccent: Color,
    userName: String,
    profileImagePath: String?,
) {
    val appearancePalette = LocalTempoPalette.current
    Box(
        modifier =
            Modifier
                .size(innerSize)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = appearancePalette.surfaceSunken.copy(alpha = 0.30f),
                    spotColor = tierAccent.copy(alpha = 0.25f),
                ).clip(CircleShape)
                .background(appearancePalette.surfaceSunken)
                .border(1.dp, appearancePalette.border, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (profileImagePath.isNullOrBlank()) {
            Text(
                text = userName.firstOrNull()?.toString()?.uppercase() ?: "U",
                style =
                    MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                    ),
                color = appearancePalette.onSurface,
            )
        } else {
            CachedAsyncImage(
                imageUrl = profileImagePath,
                contentDescription = "Avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun HeroAvatar(
    progress: Float,
    level: Int,
    userName: String,
    profileImagePath: String?,
    tierAccent: Color,
    compact: Boolean = false,
) {
    val appearancePalette = LocalTempoPalette.current
    val ringSize = if (compact) 84.dp else 96.dp
    val innerSize = if (compact) 66.dp else 76.dp
    val haloSize = ringSize + 20.dp

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(haloSize)) {
        // Soft atmospheric radial glow halo — inspired by SongDetails' artwork halo
        Box(
            modifier =
                Modifier
                    .size(haloSize)
                    .background(
                        brush =
                            Brush.radialGradient(
                                colors =
                                    listOf(
                                        tierAccent.copy(alpha = 0.22f),
                                        tierAccent.copy(alpha = 0.0f),
                                    ),
                            ),
                        shape = CircleShape,
                    ),
        )

        // Progress arc ring
        Canvas(modifier = Modifier.size(ringSize)) {
            val strokeWidth = 5.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2f, size.height / 2f)
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2, radius * 2)

            drawArc(
                color = Color.White.copy(alpha = 0.08f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            val sweep = 360f * progress.coerceIn(0f, 1f)
            if (sweep > 0.5f) {
                drawArc(
                    brush =
                        Brush.sweepGradient(
                            listOf(LevelRingSweepStart, LevelRingSweepMid, LevelRingSweepEnd),
                            center,
                        ),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
            val angle = Math.toRadians((-90f + sweep).toDouble())
            val dotCenter =
                Offset(
                    center.x + radius * cos(angle).toFloat(),
                    center.y + radius * sin(angle).toFloat(),
                )
            drawCircle(color = LevelRingSweepEnd, radius = 5.5.dp.toPx(), center = dotCenter)
            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = dotCenter)
        }

        InnerAvatarCircle(
            innerSize = innerSize,
            tierAccent = tierAccent,
            userName = userName,
            profileImagePath = profileImagePath,
        )
        // Anchored level pill badge
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-2).dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(appearancePalette.surface)
                    .border(0.8.dp, tierAccent.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 9.dp, vertical = 3.dp),
        ) {
            Text(
                text = "LVL $level",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = profileTextColor(tierAccent),
                letterSpacing = 0.8.sp,
                fontSize = MaterialTheme.typography.labelSmall.fontSize,
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Summary Stats Block
// ──────────────────────────────────────────────────────────────

private data class ProfileMetricSpec(
    val value: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val supportingText: String? = null,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProfileProgressTile(spec: ProfileMetricSpec, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.heightIn(min = 104.dp),
        shape = MaterialTheme.shapes.large,
        color = scheme.surfaceContainerLow,
        contentColor = scheme.onSurface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = spec.icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = scheme.primary,
            )
            Text(
                text = spec.value,
                style = MaterialTheme.typography.titleLargeEmphasized,
                color = scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = spec.label,
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
                maxLines = 2,
            )
            spec.supportingText?.let { note ->
                Text(
                    text = note,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.error,
                    maxLines = 2,
                )
            }
        }
    }
}

/** A compact listening streak overview; XP and levels live in the identity hero. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MasterStatMasthead(
    userLevel: UserLevel,
    currentStreak: Int,
    streakAtRisk: Boolean,
    hideStreaks: Boolean,
) {
    if (hideStreaks) return
    val streakValue = androidx.compose.ui.res.pluralStringResource(
        R.plurals.profile_bento_streak_day,
        currentStreak.coerceAtLeast(0),
        currentStreak.coerceAtLeast(0),
    )
    val bestStreakValue = androidx.compose.ui.res.pluralStringResource(
        R.plurals.profile_bento_streak_day,
        userLevel.longestStreak.coerceAtLeast(0),
        userLevel.longestStreak.coerceAtLeast(0),
    )
    val metrics = listOf(
        ProfileMetricSpec(
            value = streakValue,
            label = stringResource(R.string.profile_bento_current_streak),
            icon = Icons.Rounded.LocalFireDepartment,
            supportingText = if (streakAtRisk) stringResource(R.string.profile_bento_streak_warning) else null,
        ),
        ProfileMetricSpec(
            value = bestStreakValue,
            label = stringResource(R.string.profile_bento_best_streak),
            icon = Icons.Rounded.Whatshot,
        ),
    )
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val singleColumn = shouldStackProfileMetrics(maxWidth.value, LocalDensity.current.fontScale)
        if (singleColumn) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                metrics.forEach { metric ->
                    ProfileProgressTile(metric, Modifier.fillMaxWidth())
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                metrics.forEach { metric ->
                    ProfileProgressTile(metric, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
internal fun StreakRiskBanner(
    timeRemaining: String,
) {
    // A status warning needs a legible paired color, not an always-dark glass
    // card with accent-colored text that can disappear in light or OLED themes.
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = scheme.errorContainer,
        contentColor = scheme.onErrorContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.LocalFireDepartment,
                contentDescription = null,
                tint = scheme.onErrorContainer,
                modifier = Modifier.size(24.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Streak at risk",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onErrorContainer,
                )
                Text(
                    text = "Play a track in the next $timeRemaining to keep your streak alive.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onErrorContainer,
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Segmented Tab Switcher
// ──────────────────────────────────────────────────────────────

/**
 * Native M3 Expressive tabs keep profile achievements and badges discoverable
 * and remove the old offset-indicator animation and extra compositing layers.
 */
@Composable
internal fun TabSwitcher(
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        androidx.compose.material3.PrimaryTabRow(
            selectedTabIndex = selectedTab.coerceIn(0, (tabs.size - 1).coerceAtLeast(0)),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            tabs.forEachIndexed { index, title ->
                androidx.compose.material3.Tab(
                    selected = selectedTab == index,
                    onClick = { if (selectedTab != index) onTabSelected(index) },
                    text = {
                        Text(title, maxLines = 1,
                            style = MaterialTheme.typography.labelLarge,
                            overflow = TextOverflow.Ellipsis)
                    },
                )
            }
        }
    }
}

// Level ring — shared with HomeScreen's compact header ring
@Composable
fun CompactLevelRing(
    progress: Float,
    level: Int,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    val appearancePalette = LocalTempoPalette.current
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 4.dp.toPx()
            val radius = (this.size.minDimension - strokeWidth) / 2f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2f, radius * 2f)

            drawArc(
                color = Color.White.copy(alpha = 0.10f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            val sweep = 360f * progress.coerceIn(0f, 1f)
            if (sweep > 0.5f) {
                drawArc(
                    brush =
                        Brush.sweepGradient(
                            listOf(LevelRingSweepStart, LevelRingSweepMid, LevelRingSweepEnd),
                            center,
                        ),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        }
        Text(
            text = "$level",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = appearancePalette.onSurface,
        )
    }
}
