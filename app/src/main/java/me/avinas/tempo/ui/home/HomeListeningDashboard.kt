package me.avinas.tempo.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import me.avinas.tempo.R
import me.avinas.tempo.data.stats.ListeningOverview
import me.avinas.tempo.data.stats.DailyListening
import me.avinas.tempo.ui.components.LiveNowPlayingCard
import me.avinas.tempo.ui.theme.LocalTempoPalette
import me.avinas.tempo.ui.theme.LocalTempoAppearance
import me.avinas.tempo.ui.appearance.AppearanceNowListening
import me.avinas.tempo.ui.components.appearanceCardShape
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Home's default, single-surface listening dashboard.
 *
 * All numbers originate from the same Room-backed stats that power the older
 * Home widgets. Playback is observed locally through the existing now-playing
 * component; there are no network requests or synthetic chart values here.
 */
@Composable
internal fun HomeListeningDashboard(
    todayOverview: ListeningOverview?,
    weeklyListening: List<DailyListening>,
    expanded: Boolean,
    isLoading: Boolean,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onOpenToday: () -> Unit,
    onNavigateToStats: () -> Unit,
    onToggleInsights: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalTempoPalette.current
    val appearance = LocalTempoAppearance.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = appearanceCardShape(),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, palette.border.copy(alpha = 0.72f)),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.dashboard_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = palette.onSurface,
                    // Keep the full title readable on 320dp phones and at
                    // larger accessibility font scales next to the actions.
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onNavigateToHistory) {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = stringResource(R.string.dashboard_browse_history),
                        tint = palette.onSurface,
                    )
                }
                IconButton(onClick = onNavigateToProfile) {
                    Icon(
                        Icons.Outlined.AccountCircle,
                        contentDescription = stringResource(R.string.dashboard_profile),
                        tint = palette.onSurface,
                    )
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.home_settings),
                        tint = palette.onSurface,
                    )
                }
            }

            // COMPACT has its own cover-art strip above bottom navigation; HIDDEN
            // suppresses playback on Home altogether. Do not show two live cards.
            if (appearance.nowListening == AppearanceNowListening.INLINE) {
                LiveNowPlayingCard(embedded = true)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                DashboardMetric(
                    label = stringResource(R.string.dashboard_listening_today),
                    value = todayOverview?.let { formatDashboardDuration(it.totalListeningTimeMs) } ?: "—",
                    modifier = Modifier.weight(1f),
                    enabled = todayOverview != null && todayOverview.totalPlayCount > 0,
                    onClick = onOpenToday,
                )
                DashboardMetric(
                    label = stringResource(R.string.dashboard_tracks_played),
                    // "Tracks played" counts distinct songs, not repeated
                    // play events. Today's full play count remains in the
                    // expanded Today overview.
                    value = todayOverview?.uniqueTracksCount?.toString() ?: "—",
                    modifier = Modifier.weight(1f),
                    enabled = true,
                    onClick = onNavigateToHistory,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_trends),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(R.string.dashboard_this_week),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.mutedText,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_activity),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.mutedText,
                    )
                    Icon(
                        Icons.Outlined.BarChart,
                        contentDescription = null,
                        tint = palette.mutedText,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Query is for THIS_WEEK independent of the expanded detailed
                // period selector, so the seven columns remain Monday–Sunday.
                val today = LocalDate.now()
                val week = remember(today, weeklyListening) {
                    buildWeeklyListeningDays(today, weeklyListening)
                }
                DashboardWeekChart(week, today)

                if (!isLoading && week.all { it.listeningMs == 0L }) {
                    Text(
                        text = stringResource(R.string.dashboard_no_activity),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.mutedText,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onToggleInsights, modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            if (expanded) R.string.dashboard_less_insights
                            else R.string.dashboard_more_insights
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.width(5.dp))
                    Icon(
                        if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
                TextButton(onClick = onNavigateToStats) {
                    Text(stringResource(R.string.dashboard_view_all))
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardMetric(
    label: String,
    value: String,
    modifier: Modifier,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalTempoPalette.current
    Surface(
        modifier = modifier.then(
            if (enabled) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
        ),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, palette.border.copy(alpha = 0.55f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = palette.mutedText,
                maxLines = 2,
                minLines = 2,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = palette.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DashboardWeekChart(week: List<WeeklyListeningDay>, today: LocalDate) {
    val palette = LocalTempoPalette.current
    val maximum = week.maxOfOrNull { it.listeningMs }?.coerceAtLeast(1L) ?: 1L
    val locale = Locale.getDefault()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        week.forEach { day ->
            val fullDayName = day.date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
            val shortDayName = day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale)
            val description = stringResource(
                R.string.dashboard_bar_accessibility,
                fullDayName,
                formatDashboardDuration(day.listeningMs),
                day.playCount,
            )
            val fraction = (day.listeningMs.toDouble() / maximum.toDouble())
                .coerceIn(0.0, 1.0)
            val height = if (day.listeningMs == 0L) 3.dp
                else (120.0 * fraction).coerceAtLeast(6.0).dp
            val color = if (day.date == today) palette.accent
                else palette.mutedText.copy(alpha = 0.58f)

            Column(
                modifier = Modifier.weight(1f)
                    .clearAndSetSemantics { contentDescription = description },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        modifier = Modifier.fillMaxWidth(0.70f)
                            .height(height)
                            .clip(RoundedCornerShape(7.dp))
                            .background(color),
                    )
                }
                Text(
                    text = shortDayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (day.date == today) palette.onSurface else palette.mutedText,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Display measured listening time without rounding an unfinished minute up. */
internal fun formatDashboardDuration(milliseconds: Long): String {
    val minutes = milliseconds.coerceAtLeast(0L) / 60_000L
    val hours = minutes / 60L
    val remaining = minutes % 60L
    return buildString {
        if (hours > 0L) {
            append(hours)
            append("h")
            if (remaining > 0L) append(" ")
        }
        if (remaining > 0L || hours == 0L) {
            append(remaining)
            append("m")
        }
    }
}
