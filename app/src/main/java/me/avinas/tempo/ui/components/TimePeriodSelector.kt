package me.avinas.tempo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
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
 * Responsive connected M3 Expressive button groups.
 *
 * Five periods are a compact single row only on larger screens. On phones,
 * render independent connected groups of 2–3 items instead of squeezing
 * labels and creating inaccessible tap targets. The selected TimeRange remains
 * controlled by the existing ViewModel; there is no duplicate local state.
 */
@Composable
fun TimePeriodSelector(
    selectedRange: TimeRange,
    onRangeSelected: (TimeRange) -> Unit,
    availableRanges: List<TimeRange> = listOf(TimeRange.THIS_MONTH, TimeRange.THIS_YEAR, TimeRange.ALL_TIME),
    modifier: Modifier = Modifier,
) {
    if (availableRanges.isEmpty()) return

    val widthDp = LocalConfiguration.current.screenWidthDp
    val fontScale = LocalDensity.current.fontScale
    val haptic = LocalHapticFeedback.current
    val maxPerRow = connectedChoiceColumns(widthDp, fontScale, availableRanges.size)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        availableRanges.chunked(maxPerRow).forEach { rowRanges ->
            Row(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                rowRanges.forEachIndexed { index, range ->
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
                        shapes = when {
                            rowRanges.size == 1 -> ToggleButtonDefaults.shapes()
                            index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            index == rowRanges.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .semantics { role = Role.RadioButton },
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
