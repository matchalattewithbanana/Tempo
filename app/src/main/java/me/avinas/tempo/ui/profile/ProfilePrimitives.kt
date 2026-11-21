package me.avinas.tempo.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.semantics
import me.avinas.tempo.ui.theme.*
import java.util.Locale

// ── Level tier palette — connects visual radiance directly to listener rank ──
internal fun getLevelTierAccent(level: Int): Color =
    when {
        level >= 100 -> Color(0xFFEC4899)

        // Mythic Pink
        level >= 75 -> Color(0xFFF43F5E)

        // Rose
        level >= 50 -> TempoWarningBright

        // Gold (#FBBF24)
        level >= 35 -> Color(0xFFA855F7)

        // Purple
        level >= 20 -> TempoInfo

        // Electric Blue (#3B82F6)
        level >= 10 -> TempoAccent

        // Cyan (#5EEAD4)
        level >= 5 -> TempoSuccessBright

        // Emerald (#4ADE80)
        else -> TempoPrimary // Studio Teal (#2FDBB8)
    }

// ── Category & difficulty identity — data colormaps, not chrome tokens ──
// ──────────────────────────────────────────────────────────────
// Editorial Section Catalog Kicker & Section Header
// ──────────────────────────────────────────────────────────────

@Composable
internal fun SectionCatalogKicker(
    number: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val appearancePalette = LocalTempoPalette.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = number,
            style = KickerSmall,
            fontWeight = FontWeight.Bold,
            color = appearancePalette.onSurface,
        )
        Box(
            modifier =
                Modifier
                    .width(10.dp)
                    .height(0.8.dp)
                    .background(appearancePalette.border.copy(alpha = 0.65f)),
        )
        Text(
            text = label.uppercase(Locale.getDefault()),
            style = KickerSmall,
            color = appearancePalette.mutedText,
            letterSpacing = 1.2.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun EditorialSectionHeader(
    sectionNumber: String,
    title: String,
    subtitle: String,
    trailing: (@Composable () -> Unit)? = null,
) {
    val appearancePalette = LocalTempoPalette.current
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionCatalogKicker(number = sectionNumber, label = title)
            if (trailing != null) {
                trailing()
            }
        }
        Text(
            text = subtitle,
            style = CaptionSmall,
            color = appearancePalette.mutedText,
        )
    }
}

// ──────────────────────────────────────────────────────────────
// Masthead Hero Metric & Secondary Stat Blocks (Editorial System)
// ──────────────────────────────────────────────────────────────

@Composable
internal fun MastheadHeroMetric(
    label: String,
    value: String,
    suffix: String,
    accentTint: Color,
) {
    val appearancePalette = LocalTempoPalette.current
    // Static focus mark: no perpetual animation or per-frame UI work.

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(6.dp)
.clip(CircleShape)
                        .background(accentTint),
            )
            Text(
                text = label.uppercase(Locale.getDefault()),
                style = KickerSmall,
                color = appearancePalette.mutedText,
                letterSpacing = 1.4.sp,
            )
        }
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = value,
                style =
                    MaterialTheme.typography.displayMedium.copy(
                        fontFamily = DisplayFontFamily,
                        letterSpacing = (-1.5).sp,
                    ),
                fontWeight = FontWeight.Bold,
                color = appearancePalette.onSurface,
                lineHeight = 44.sp,
            )
            if (suffix.isNotBlank()) {
                Text(
                    text = suffix,
                    style = MaterialTheme.typography.titleSmall,
                    color = appearancePalette.mutedText,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
        }
    }
}

@Composable
internal fun MastheadSecondaryStat(
    label: String,
    value: String,
    subtext: String,
    modifier: Modifier = Modifier,
    valueColor: Color = LocalTempoPalette.current.onSurface,
) {
    val appearancePalette = LocalTempoPalette.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label.uppercase(Locale.getDefault()),
            style = KickerSmall,
            color = appearancePalette.mutedText,
            letterSpacing = 1.2.sp,
            fontSize = MaterialTheme.typography.labelSmall.fontSize,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = profileTextColor(valueColor),
        )
        if (subtext.isNotBlank()) {
            Text(
                text = subtext,
                style = CaptionSmall,
                color = appearancePalette.mutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Hairline Progress Track & Chips
// ──────────────────────────────────────────────────────────────

@Composable
internal fun ProgressTrack(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = LocalTempoPalette.current.accent,
    height: Dp = 6.dp,
) {
    val track = profileIndicatorColor(LocalTempoPalette.current.border)
    val fill = profileIndicatorColor(color)
    Box(
        modifier = modifier
            .height(height)
            .clip(CircleShape)
            .border(1.dp, track, CircleShape)
            .drawBehind {
                val p = progress.coerceIn(0f, 1f)
                if (p > 0f) {
                    val cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
                    drawRoundRect(color = fill, size = Size(size.width * p, size.height), cornerRadius = cornerRadius)
                }
            },
    )
}

@Composable
internal fun XpChip(xp: Int) {
    val chipSurface = TempoWarning.copy(alpha = 0.12f).compositeOver(LocalTempoPalette.current.surface)
    val chipText = profileTextColor(TempoWarning, chipSurface)
    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(chipSurface)
                .border(0.8.dp, TempoWarning.copy(alpha = 0.32f), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = chipText,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = "+$xp XP",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = chipText,
            letterSpacing = 0.4.sp,
        )
    }
}

@Composable
internal fun StarsChip(
    total: Int,
    max: Int,
) {
    val chipSurface = TempoWarning.copy(alpha = 0.12f).compositeOver(LocalTempoPalette.current.surface)
    val chipText = profileTextColor(TempoWarning, chipSurface)
    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(chipSurface)
                .border(0.8.dp, TempoWarning.copy(alpha = 0.32f), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Star,
            contentDescription = null,
            tint = chipText,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = "$total / $max",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = chipText,
            letterSpacing = 0.4.sp,
        )
    }
}

/** Compact identity shown in the top bar once the hero identity scrolls out of view. */
@Composable
internal fun CollapsingProfileTitle(
    visible: Boolean,
    userName: String,
    level: Int,
    title: String,
) {
    val appearancePalette = LocalTempoPalette.current
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()) +
            slideInVertically(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()) { -10 },
        exit = fadeOut(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()) +
            slideOutVertically(animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()) { -10 },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = userName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = appearancePalette.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Level $level · $title",
                style = MaterialTheme.typography.labelSmall,
                color = getLevelTierAccent(level),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Loading Skeleton State
// ──────────────────────────────────────────────────────────────

@Composable
internal fun SkeletonBlock(
    modifier: Modifier,
    cornerRadius: Dp,
    alpha: Float = 0.65f,
) {
    val appearancePalette = LocalTempoPalette.current
    Box(
        modifier
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(cornerRadius))
            .background(GlassFrostMedium)
            .border(0.8.dp, appearancePalette.border.copy(alpha = 0.45f), RoundedCornerShape(cornerRadius)),
    )
}

@Composable
internal fun ProfileLoadingSkeleton(
    compact: Boolean,
    contentModifier: Modifier,
    heroTopClearance: Dp,
) {
    // An initial loading placeholder should not run an infinite animation loop.
    val currentAlpha = 0.65f

    Column(
        modifier = contentModifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Spacer(modifier = Modifier.height(heroTopClearance))
        val avatarSize = if (compact) 104.dp else 116.dp
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 18.dp),
        ) {
            SkeletonBlock(modifier = Modifier.size(avatarSize), cornerRadius = avatarSize / 2, alpha = currentAlpha)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SkeletonBlock(modifier = Modifier.size(160.dp, 24.dp), cornerRadius = 8.dp, alpha = currentAlpha)
                SkeletonBlock(modifier = Modifier.size(120.dp, 14.dp), cornerRadius = 7.dp, alpha = currentAlpha)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        SkeletonBlock(modifier = Modifier.fillMaxWidth().height(86.dp), cornerRadius = 20.dp, alpha = currentAlpha)
        SkeletonBlock(modifier = Modifier.fillMaxWidth().height(150.dp), cornerRadius = 22.dp, alpha = currentAlpha)
        SkeletonBlock(modifier = Modifier.fillMaxWidth().height(48.dp), cornerRadius = 16.dp, alpha = currentAlpha)
        SkeletonBlock(modifier = Modifier.fillMaxWidth().height(120.dp), cornerRadius = 20.dp, alpha = currentAlpha)
        SkeletonBlock(modifier = Modifier.fillMaxWidth().height(120.dp), cornerRadius = 20.dp, alpha = currentAlpha)
    }
}

