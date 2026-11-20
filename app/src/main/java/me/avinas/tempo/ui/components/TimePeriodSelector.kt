package me.avinas.tempo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.avinas.tempo.R
import me.avinas.tempo.data.stats.TimeRange

/**
 * Connected listening-time choices use M3 Expressive ToggleButtons.
 * FlowRow prevents tiny unreadable labels at large accessibility font scales
 * while preserving all available periods and the caller's selected state.
 */
@Composable
fun TimePeriodSelector(
    selectedRange: TimeRange,
    onRangeSelected: (TimeRange) -> Unit,
    availableRanges: List<TimeRange> = listOf(TimeRange.THIS_MONTH, TimeRange.THIS_YEAR, TimeRange.ALL_TIME),
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        availableRanges.forEach { range ->
            val label = when (range) {
                TimeRange.TODAY -> stringResource(R.string.stats_share_period_today)
                TimeRange.THIS_WEEK -> stringResource(R.string.stats_range_week)
                TimeRange.THIS_MONTH -> stringResource(R.string.stats_range_month)
                TimeRange.THIS_YEAR -> stringResource(R.string.stats_range_year)
                TimeRange.ALL_TIME -> stringResource(R.string.stats_range_all_time)
            }
            ToggleButton(
                checked = range == selectedRange,
                onCheckedChange = {
                    if (range != selectedRange) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onRangeSelected(range)
                    }
                },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics { role = Role.RadioButton },
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
