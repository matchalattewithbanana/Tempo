package me.avinas.tempo.ui.profile

import me.avinas.tempo.ui.effects.tempoAmbientBackdrop
import me.avinas.tempo.ui.effects.tempoTiltSurface
import me.avinas.tempo.ui.effects.tempoListMotion
import androidx.compose.runtime.saveable.rememberSaveable
import me.avinas.tempo.ui.appearance.AppearanceProfileTab
import me.avinas.tempo.ui.theme.LocalTempoAppearance
import me.avinas.tempo.ui.components.ShareSnapshot
import me.avinas.tempo.ui.components.SnapshotShareDialog
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import me.avinas.tempo.data.repository.observeClockChanges
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import me.avinas.tempo.R
import me.avinas.tempo.data.local.entities.Badge
import me.avinas.tempo.data.local.entities.DailyChallenge
import me.avinas.tempo.data.local.entities.UserLevel
import me.avinas.tempo.data.stats.GamificationEngine
import me.avinas.tempo.ui.components.ArtAtmosphereLayer
import me.avinas.tempo.ui.components.BadgeShareCard
import me.avinas.tempo.ui.components.CachedAsyncImage
import me.avinas.tempo.ui.components.CelebrationParticlesCanvas
import me.avinas.tempo.ui.components.LiveNowPlayingCard
import me.avinas.tempo.ui.components.TempoEmblem
import me.avinas.tempo.ui.components.TempoEmblemStyle
import me.avinas.tempo.ui.components.FrostedIconButton
import me.avinas.tempo.ui.components.GlassCard
import me.avinas.tempo.ui.components.GlassCardVariant
import me.avinas.tempo.ui.components.MilestoneShareCard
import me.avinas.tempo.ui.components.SharePreviewDialog
import me.avinas.tempo.ui.theme.*
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// Challenge titles generated before the custom icon set landed carry a leading emoji.
// Persisted rows survive until the next daily refresh, so drop it at render time rather
// than showing the emoji next to its replacement icon for the rest of the day.
private val LeadingEmoji = Regex("^[^\\p{L}\\p{N}]+")

internal fun String.withoutLeadingEmoji(): String = replace(LeadingEmoji, "")

/**
 * Custom icon for a challenge, keyed on [DailyChallenge.challengeId] rather than category.
 *
 * The five categories collapse eight distinct challenge types — VOLUME covers both
 * song-count and minute-count, DISCOVERY covers artists and genres — and each type has its
 * own icon. Ids are stable and persisted, so they survive daily title regeneration.
 */
private fun getChallengeIcon(challenge: DailyChallenge): Int {
    val id = challenge.challengeId
    return when {
        id.startsWith("volume_songs") -> {
            R.drawable.ic_challenge_songs
        }

        id.startsWith("volume_mins") -> {
            R.drawable.ic_challenge_minutes
        }

        id.startsWith("variety_artists") -> {
            R.drawable.ic_challenge_variety
        }

        id.startsWith("discovery_artists") -> {
            R.drawable.ic_challenge_discovery_artists
        }

        id.startsWith("discovery_genres") -> {
            R.drawable.ic_challenge_discovery_genres
        }

        id.startsWith("explore_artist") -> {
            R.drawable.ic_challenge_explore_artist
        }

        id.startsWith("explore_genre") -> {
            R.drawable.ic_challenge_explore_genre
        }

        id.startsWith("time_early_bird") -> {
            R.drawable.ic_challenge_early_bird
        }

        // Unrecognised id (e.g. a type added by a newer version) — fall back on category,
        // matching the previous category-keyed behaviour.
        else -> {
            when (challenge.category) {
                "TIME" -> R.drawable.ic_challenge_early_bird
                "VARIETY" -> R.drawable.ic_challenge_variety
                "DISCOVERY" -> R.drawable.ic_challenge_discovery_genres
                "EXPLORATION" -> R.drawable.ic_challenge_explore_artist
                else -> R.drawable.ic_challenge_songs
            }
        }
    }
}

private fun getDifficultyColor(difficulty: String): Color =
    when (difficulty) {
        "EASY" -> TempoSuccessDeep
        "MEDIUM" -> TempoWarning
        "HARD" -> TempoError
        else -> Color.Gray
    }


// ──────────────────────────────────────────────────────────────
// Daily Challenges (Quests) Section
// ──────────────────────────────────────────────────────────────

internal fun LazyListScope.challengesSection(
    challenges: List<DailyChallenge>,
    totalXpAvailable: Int,
    contentModifier: Modifier,
    onShareChallenge: (DailyChallenge) -> Unit,
) {
    val completedCount = challenges.count { it.isCompleted }
    item(key = "challenges_header") {
        val resetLabel =
            remember {
                val midnight =
                    LocalDate
                        .now()
                        .plusDays(1)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                val diffMs = midnight - System.currentTimeMillis()
                val h = (diffMs / (1000 * 60 * 60)).toInt()
                val m = ((diffMs % (1000 * 60 * 60)) / (1000 * 60)).toInt()
                if (h > 0) "resets in ${h}h ${m}m" else "resets in ${m}m"
            }
        Column(modifier = contentModifier) {
            Spacer(modifier = Modifier.height(24.dp))
            EditorialSectionHeader(
                sectionNumber = "01",
                title = "Daily quests",
                subtitle = "$completedCount of ${challenges.size} complete · $resetLabel",
                trailing = { XpChip(xp = totalXpAvailable) },
            )
            Spacer(modifier = Modifier.height(14.dp))
            ProgressTrack(
                progress = if (challenges.isEmpty()) 0f else completedCount.toFloat() / challenges.size,
                modifier = Modifier.fillMaxWidth(),
                color = LocalTempoPalette.current.accent,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    itemsIndexed(challenges, key = { _, c -> "challenge_${c.id}" }) { index, challenge ->
        Column(modifier = contentModifier.tempoListMotion(index)) {
            ChallengeCard(challenge = challenge)
            if (challenge.isCompleted) {
                TextButton(onClick = { onShareChallenge(challenge) }, modifier = Modifier.align(Alignment.End)) {
                    Text(stringResource(R.string.share_challenge))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
internal fun EmptyChallengesState(modifier: Modifier = Modifier) {
    val appearancePalette = LocalTempoPalette.current
    GlassCard(
        modifier = modifier,
        variant = GlassCardVariant.Obsidian,
        shape = me.avinas.tempo.ui.components.appearanceCardShape(),
        borderColor = appearancePalette.border.copy(alpha = 0.45f),
        borderWidth = 0.8.dp,
        contentPadding = PaddingValues(28.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TempoEmblem(
                icon = Icons.Rounded.Schedule,
                size = 76.dp,
                style = TempoEmblemStyle.MEMORY,
            )
            Text(
                text = "Nothing queued yet",
                style = MaterialTheme.typography.titleLargeEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Pull to refresh or continue listening — new daily quests will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ChallengePercentText(
    progress: Float,
    accent: Color,
) {
    Text(
        text = "${(progress * 100).toInt()}%",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = profileTextColor(accent),
    )
}

@Composable
private fun ChallengeCard(challenge: DailyChallenge) {
    val appearancePalette = LocalTempoPalette.current
    val isCompleted = challenge.isCompleted
    val accent = if (isCompleted) TempoSuccessDeep else getDifficultyColor(challenge.difficulty)
    val difficultySurface = accent.copy(alpha = 0.12f).compositeOver(appearancePalette.surfaceSunken)
    val rewardSurface = (if (isCompleted) TempoSuccessDeep.copy(alpha = 0.12f) else TempoWarning.copy(alpha = 0.10f))
        .compositeOver(appearancePalette.surfaceSunken)
    val animatedProgress by animateFloatAsState(
        targetValue = challenge.progressFraction,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "challengeProgress",
    )

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        variant = GlassCardVariant.Obsidian,
        borderColor = if (isCompleted) TempoSuccessDeep.copy(alpha = 0.25f) else appearancePalette.border.copy(alpha = 0.45f),
        borderWidth = 0.8.dp,
        contentPadding = PaddingValues(18.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Top metadata row: Difficulty tag, category label, and XP reward pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(difficultySurface)
                                .border(0.8.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = challenge.difficulty.uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = profileTextColor(accent, difficultySurface),
                            letterSpacing = 0.8.sp,
                            fontSize = MaterialTheme.typography.labelSmall.fontSize,
                        )
                    }
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.labelSmall,
                        color = appearancePalette.mutedText,
                    )
                    Text(
                        text = challenge.category.replace("_", " ").uppercase(Locale.getDefault()),
                        style = KickerSmall,
                        fontWeight = FontWeight.Medium,
                        color = appearancePalette.mutedText,
                        letterSpacing = 0.8.sp,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                    )
                }

                Row(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(rewardSurface)
                            .border(
                                0.8.dp,
                                if (isCompleted) TempoSuccessDeep.copy(alpha = 0.30f) else TempoWarning.copy(alpha = 0.25f),
                                RoundedCornerShape(8.dp),
                            ).padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = profileIndicatorColor(if (isCompleted) TempoSuccessDeep else TempoWarning, rewardSurface),
                        modifier = Modifier.size(11.dp),
                    )
                    Text(
                        text = "+${challenge.xpReward} XP",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = profileTextColor(if (isCompleted) TempoSuccessDeep else TempoWarning, rewardSurface),
                        letterSpacing = 0.4.sp,
                    )
                }
            }

            // Core content row: Sunken Icon + Title & Description
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(appearancePalette.surfaceSunken)
                            .border(0.8.dp, accent.copy(alpha = if (isCompleted) 0.40f else 0.20f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(getChallengeIcon(challenge)),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = challenge.title.withoutLeadingEmoji(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = appearancePalette.onSurface,
                    )
                    Text(
                        text = challenge.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = appearancePalette.mutedText,
                        lineHeight = 18.sp,
                    )
                }
            }

            // Hairline Progress bar
            ProgressTrack(
                progress = animatedProgress,
                modifier = Modifier.fillMaxWidth(),
                color = accent,
                height = 5.dp,
            )

            // Bottom row: numeric progress & status/action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${challenge.currentProgress} / ${challenge.targetValue}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCompleted) profileTextColor(TempoSuccessDeep) else appearancePalette.mutedText,
                )

                if (isCompleted) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(TempoSuccessDeep.copy(alpha = 0.12f))
                                .border(0.8.dp, TempoSuccessDeep.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = TempoSuccessDeep,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = "Completed",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = profileTextColor(TempoSuccessDeep),
                        )
                    }
                } else {
                    ChallengePercentText(progress = animatedProgress, accent = accent)
                }
            }
        }
    }
}

