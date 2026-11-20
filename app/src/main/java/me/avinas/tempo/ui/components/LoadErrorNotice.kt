package me.avinas.tempo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import me.avinas.tempo.R

/** Unified error state for Home, Stats and History. Focusable native action
 * and Material error-container contrast work in both Light and OLED themes. */
@Composable
internal fun LoadErrorNotice(message: String, onRetry: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.large,
        color = colors.errorContainer,
        contentColor = colors.onErrorContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TempoEmblem(
                icon = Icons.Outlined.ErrorOutline,
                size = 44.dp,
                style = TempoEmblemStyle.WARNING,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)) {
                Text(message, style = MaterialTheme.typography.bodyMedium)
                FilledTonalButton(
                    onClick = onRetry,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = colors.onErrorContainer,
                        contentColor = colors.errorContainer,
                    ),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                    Text(stringResource(R.string.common_retry))
                }
            }
        }
    }
}
