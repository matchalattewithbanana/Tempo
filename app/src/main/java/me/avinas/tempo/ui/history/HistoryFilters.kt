package me.avinas.tempo.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import me.avinas.tempo.R
import me.avinas.tempo.ui.components.*
import me.avinas.tempo.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryFilterSheetContent(
    currentShowSkips: Boolean,
    currentStartDate: Long?,
    currentEndDate: Long?,
    onApply: (Long?, Long?, Boolean) -> Unit,
    onReset: () -> Unit
) {
    var showSkips by remember { mutableStateOf(currentShowSkips) }
    var selectedRange by remember { mutableStateOf(getRangeLabel(currentStartDate)) }
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 28.dp)
            .navigationBarsPadding()
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.history_filter_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.history_reset),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (selectedRange != "All Time" || !showSkips) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedRange = "All Time"
                        showSkips = true
                        onReset()
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Time Range Kicker
        Text(
            text = stringResource(R.string.history_time_range).uppercase(Locale.getDefault()),
            style = KickerSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Expressive segmented choices retain the History filter keys but
        // use native focus, selection and touch-target behavior.
        val ranges = listOf(
            "All Time" to stringResource(R.string.history_all_time),
            "Last 7 Days" to stringResource(R.string.history_7_days_abbr),
            "Last 30 Days" to stringResource(R.string.history_30_days_abbr),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(androidx.compose.material3.ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            ranges.forEachIndexed { index, (key, label) ->
                androidx.compose.material3.ToggleButton(
                    checked = selectedRange == key,
                    shapes = when (index) {
                        0 -> androidx.compose.material3.ButtonGroupDefaults.connectedLeadingButtonShapes()
                        ranges.lastIndex -> androidx.compose.material3.ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> androidx.compose.material3.ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                    onCheckedChange = {
                        if (selectedRange != key) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedRange = key
                        }
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        .semantics { role = Role.RadioButton },
                ) {
                    Text(label, style = MaterialTheme.typography.labelLarge,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.history_playback),
            style = MaterialTheme.typography.titleSmallEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                showSkips = !showSkips
            },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.history_show_skipped),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "Include songs skipped during playback",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = showSkips,
                    onCheckedChange = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showSkips = it
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                val (start, end) = getRangeBounds(selectedRange)
                onApply(start, end, showSkips)
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text(
                text = stringResource(R.string.history_apply_filters),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

fun getRangeLabel(start: Long?): String {
    if (start == null) return "All Time"
    val now = System.currentTimeMillis()
    val diff = now - start
    return when {
        diff <= 8 * 24 * 60 * 60 * 1000L -> "Last 7 Days"
        diff <= 31 * 24 * 60 * 60 * 1000L -> "Last 30 Days"
        else -> "All Time"
    }
}

fun getRangeBounds(label: String): Pair<Long?, Long?> {
    val now = System.currentTimeMillis()
    return when (label) {
        "Last 7 Days" -> Pair(now - 7 * 24 * 60 * 60 * 1000L, now)
        "Last 30 Days" -> Pair(now - 30 * 24 * 60 * 60 * 1000L, now)
        else -> Pair(null, null)
    }
}
