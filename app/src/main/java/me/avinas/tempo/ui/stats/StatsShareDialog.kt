package me.avinas.tempo.ui.stats

import me.avinas.tempo.ui.theme.LocalTempoPalette
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import me.avinas.tempo.R
import me.avinas.tempo.ui.components.ShareOutputButtons
import me.avinas.tempo.ui.components.rememberCardShareActions
import me.avinas.tempo.ui.appearance.AppearanceStatsDuration
import me.avinas.tempo.ui.appearance.AppearanceStatsSummary
import me.avinas.tempo.ui.theme.LocalTempoAppearance
import me.avinas.tempo.data.stats.CombinedBasicStats
import me.avinas.tempo.data.stats.TimeRange
import me.avinas.tempo.ui.components.CaptureWrapper
import me.avinas.tempo.ui.components.ShareTheme
import me.avinas.tempo.ui.components.ThemeSwatch
import me.avinas.tempo.ui.components.rememberCaptureController
import me.avinas.tempo.ui.onboarding.dataStore

private val CardDesignWidth = 360.dp
private val CardDesignHeight = 640.dp

@Composable
fun StatsShareDialog(
    tab: StatsTab,
    timeRange: TimeRange,
    items: List<Any>,
    totals: CombinedBasicStats?,
    onDismiss: () -> Unit,
    // Optional subtle share nudge context (shown quietly above the share button).
    nudgeCaption: String? = null,
    nudgeHint: String? = null,
    // Invoked instead of onDismiss after a successful share (lets the caller
    // distinguish "shared" from "closed without sharing").
    onShared: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val captureController = rememberCaptureController()
    val appearance = LocalTempoAppearance.current
    val defaultTheme = appearance.defaultShareTheme
    var config by remember(defaultTheme) { mutableStateOf(StatsShareConfig(theme = defaultTheme)) }
    val actions = rememberCardShareActions(captureController, fileName = "tempo-stats.png") {
        runCatching {
            context.dataStore.edit { prefs ->
                prefs[longPreferencesKey("share_last_success")] = System.currentTimeMillis()
            }
        }
        if (onShared != null) onShared() else onDismiss()
    }
    val text = statsShareText(context, tab, timeRange, items, totals, config, appearance.statsDuration, appearance.statsDurationPrecision, appearance.statsSummary)

    val contentToShare: @Composable () -> Unit = {
        CompositionLocalProvider(LocalTempoAppearance provides appearance) {
            StatsShareCard(
                tab = tab,
                timeRange = timeRange,
                items = items,
                totals = totals,
                config = config,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    Dialog(
        onDismissRequest = { if (!actions.busy) onDismiss() },
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // 1. Hidden capture source — rendered at the SAME fixed design size as the
            //    preview to guarantee WYSIWYG. The bitmap is captured at the device's
            //    pixel density (e.g. 1080x1920 at 3x), perfectly matching the preview.
            Box(
                modifier =
                    Modifier
                        .requiredSize(CardDesignWidth, CardDesignHeight)
                        .alpha(0f),
                contentAlignment = Alignment.Center,
            ) {
                CaptureWrapper(controller = captureController, modifier = Modifier.fillMaxSize()) {
                    contentToShare()
                }
            }

            // 2. Dark overlay
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)))

            // 3. Visible UI
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize().systemBarsPadding().padding(bottom = 24.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.stats_share_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    IconButton(
                        onClick = onDismiss,
                        enabled = !actions.busy,
                        modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape),
                    ) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.stats_share_close), tint = Color.White)
                    }
                }

                // Preview — laid out at the design size and scaled to fit
                BoxWithConstraints(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    val scale = minOf(maxWidth / CardDesignWidth, maxHeight / CardDesignHeight).coerceAtMost(1f)
                    val scaledWidth = CardDesignWidth * scale
                    val scaledHeight = CardDesignHeight * scale
                    Box(modifier = Modifier.size(scaledWidth, scaledHeight), contentAlignment = Alignment.Center) {
                        Box(
                            modifier =
                                Modifier
                                    .requiredSize(CardDesignWidth, CardDesignHeight)
                                    .graphicsLayer(scaleX = scale, scaleY = scale, transformOrigin = TransformOrigin(0.5f, 0.5f)),
                        ) {
                            contentToShare()
                        }
                    }
                }

                // Config controls
                ConfigPanel(
                    config = config,
                    onConfigChange = { if (!actions.busy) config = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                )

                // Subtle nudge caption — quiet, understated context line
                if (!nudgeCaption.isNullOrEmpty() || !nudgeHint.isNullOrEmpty()) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (!nudgeCaption.isNullOrEmpty()) {
                            Text(
                                text = nudgeCaption,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                            )
                        }
                        if (!nudgeHint.isNullOrEmpty()) {
                            Text(
                                text = nudgeHint,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.4f),
                                letterSpacing = 2.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                ShareOutputButtons(
                    actions = actions,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    text = text,
                )
            }
        }
    }
}

private fun statsShareText(
    context: android.content.Context,
    tab: StatsTab,
    period: TimeRange,
    items: List<Any>,
    totals: CombinedBasicStats?,
    config: StatsShareConfig,
    durationUnit: AppearanceStatsDuration,
    durationPrecision: me.avinas.tempo.ui.appearance.AppearanceDurationPrecision,
    summaryMetric: AppearanceStatsSummary,
): String = buildString {
    val title = when (tab) {
        StatsTab.TOP_SONGS -> R.string.stats_share_top_songs
        StatsTab.TOP_ARTISTS -> R.string.stats_share_top_artists
        StatsTab.TOP_ALBUMS -> R.string.stats_share_top_albums
    }
    val range = when (period) {
        TimeRange.TODAY -> R.string.stats_share_period_today
        TimeRange.THIS_WEEK -> R.string.stats_share_period_this_week
        TimeRange.THIS_MONTH -> R.string.stats_share_period_this_month
        TimeRange.THIS_YEAR -> R.string.stats_share_period_this_year
        TimeRange.ALL_TIME -> R.string.stats_share_period_all_time
    }
    appendLine(context.getString(title))
    appendLine(context.getString(range))
    if (config.showSummary && totals != null) {
        val duration = formatStatsDuration(context, totals.totalTimeMs, durationUnit, durationPrecision)
        appendLine(context.getString(R.string.share_text_metric, context.getString(R.string.stats_period_listening_time), duration))
        appendLine(context.getString(R.string.share_text_metric, context.getString(R.string.stats_period_plays), context.getString(R.string.stats_period_number, totals.playCount)))
        appendLine(context.getString(R.string.share_text_metric, context.getString(statsSummaryLabel(summaryMetric)),
            context.getString(R.string.stats_period_number, totals.summaryCount(summaryMetric))))
    }
    items.asSequence().mapNotNull(::statsItemInfo).take(config.count.count).forEachIndexed { index, item ->
        val name = listOf(item.title, item.subtitle).filter(String::isNotBlank).joinToString(" — ")
        appendLine(context.getString(R.string.share_ranked_text, index + 1, name))
    }
}.trim()

@Composable
private fun ConfigPanel(
    config: StatsShareConfig,
    onConfigChange: (StatsShareConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appearancePalette = LocalTempoPalette.current
    // Floating control sheet from the sharecontrol mockup (drag handle,
    // grouped segmented controls, theme strip + summary pill) with Tempo
    // tokens for the sheet and selection — dark sheet, teal pill, gradient
    // theme swatches — plus the mockup's intended off-white segmented track.
    val sheetShape = RoundedCornerShape(24.dp)
    Column(
        modifier =
            modifier
                .clip(sheetShape)
                .background(appearancePalette.elevatedSurface)
                .border(1.dp, appearancePalette.border.copy(alpha = 0.65f), sheetShape)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Drag handle — visual affordance that this is a bottom sheet.
        Box(
            modifier =
                Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .align(Alignment.CenterHorizontally),
        )

        // Row 1: Layout + Items as true segmented controls (single track with
        // a selected pill) rather than three detached boxes.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ConfigLabel(text = stringResource(R.string.stats_share_layout_label))
                SegmentedTrack {
                    SegmentedIconCell(
                        icon = Icons.AutoMirrored.Filled.ViewList,
                        contentDescription = stringResource(R.string.stats_share_layout_list),
                        selected = config.layout == StatsShareLayout.LIST,
                        onClick = { onConfigChange(config.copy(layout = StatsShareLayout.LIST)) },
                    )
                    SegmentedIconCell(
                        icon = Icons.Default.EmojiEvents,
                        contentDescription = stringResource(R.string.stats_share_layout_podium),
                        selected = config.layout == StatsShareLayout.PODIUM,
                        onClick = { onConfigChange(config.copy(layout = StatsShareLayout.PODIUM)) },
                    )
                    SegmentedIconCell(
                        icon = Icons.Default.GridView,
                        contentDescription = stringResource(R.string.stats_share_layout_grid),
                        selected = config.layout == StatsShareLayout.GRID,
                        onClick = { onConfigChange(config.copy(layout = StatsShareLayout.GRID)) },
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ConfigLabel(text = stringResource(R.string.stats_share_count_label))
                SegmentedTrack {
                    StatsShareCount.entries.forEach { c ->
                        SegmentedTextCell(
                            text = c.count.toString(),
                            selected = config.count == c,
                            onClick = { onConfigChange(config.copy(count = c)) },
                        )
                    }
                }
            }
        }

        // Hairline separator between the segmented group and the theme group.
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(Color.White.copy(alpha = 0.08f)),
        )

        // Row 2: Theme strip + Summary pill.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ConfigLabel(text = stringResource(R.string.stats_share_theme_label))
                // 32dp touch wrappers around the 26dp gradient swatches;
                // selected swatch keeps its white ring (see ThemeSwatch).
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ShareTheme.entries.forEach { t ->
                        Box(
                            modifier = Modifier.size(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            ThemeSwatch(
                                theme = t,
                                selected = config.theme == t,
                                onClick = { onConfigChange(config.copy(theme = t)) },
                            )
                        }
                    }
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End,
            ) {
                ConfigLabel(text = stringResource(R.string.stats_share_summary))
                SummaryToggle(
                    enabled = config.showSummary,
                    onToggle = { onConfigChange(config.copy(showSummary = it)) },
                )
            }
        }
    }
}

@Composable
private fun ConfigLabel(text: String) {
    val appearancePalette = LocalTempoPalette.current
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = appearancePalette.mutedText,
        letterSpacing = 1.2.sp,
    )
}

/** Off-white segmented track shared by the layout and items selectors. */
@Composable
private fun SegmentedTrack(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(14.dp))
                .background(LocalTempoPalette.current.surfaceSunken)
                .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun RowScope.SegmentedIconCell(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val appearancePalette = LocalTempoPalette.current
    Box(
        modifier =
            Modifier
                .weight(1f)
                .height(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) appearancePalette.accent else Color.Transparent)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) appearancePalette.onAccent else appearancePalette.mutedText,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun RowScope.SegmentedTextCell(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val appearancePalette = LocalTempoPalette.current
    Box(
        modifier =
            Modifier
                .weight(1f)
                .height(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) appearancePalette.accent else Color.Transparent)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (selected) appearancePalette.onAccent else appearancePalette.mutedText,
        )
    }
}

@Composable
private fun SummaryToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val appearancePalette = LocalTempoPalette.current
    Row(
        modifier =
            Modifier
                .height(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (enabled) appearancePalette.accent else appearancePalette.elevatedSurface)
                .clickable { onToggle(!enabled) }
                .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = if (enabled) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (enabled) appearancePalette.onAccent else appearancePalette.mutedText,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(if (enabled) R.string.stats_share_on else R.string.stats_share_off),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (enabled) appearancePalette.onAccent else appearancePalette.mutedText,
        )
    }
}
