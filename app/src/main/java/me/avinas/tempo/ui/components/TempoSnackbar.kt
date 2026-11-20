package me.avinas.tempo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.avinas.tempo.ui.theme.*

/**
 * Severity of an in-app notification. Drives the leading icon badge and
 * action accent. [fromMessage] is a best-effort classifier for the app's
 * English feedback strings; pass an explicit variant when semantics are known.
 */
enum class TempoSnackbarVariant(
    val tint: Color
) {
    Success(TempoSuccess),
    Error(TempoError),
    Warning(TempoWarning),
    Neutral(TempoPrimary);

    companion object {
        fun fromMessage(message: String): TempoSnackbarVariant {
            val m = message.lowercase()
            return when {
                m.startsWith("success") || "successfully" in m -> Success
                "fail" in m || "error" in m || "unable" in m || "cannot" in m || "not available" in m -> Error
                else -> Neutral
            }
        }
    }
}

/**
 * Native M3 snackbar host presentation. Severity remains visible in the icon,
 * while standard Material actions provide ripple, keyboard and TalkBack support.
 * No layered gradient or shadow passes are needed.
 */
@Composable
fun TempoSnackbar(
    data: SnackbarData,
    modifier: Modifier = Modifier,
    variant: TempoSnackbarVariant = TempoSnackbarVariant.fromMessage(data.visuals.message),
) {
    val scheme = MaterialTheme.colorScheme
    val notificationTint = when (variant) {
        TempoSnackbarVariant.Success -> LocalTempoPalette.current.accent
        TempoSnackbarVariant.Error -> scheme.error
        TempoSnackbarVariant.Warning -> scheme.tertiary
        TempoSnackbarVariant.Neutral -> scheme.primary
    }
    val actionLabel = data.visuals.actionLabel
    Snackbar(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = MaterialTheme.shapes.large,
        containerColor = scheme.surfaceContainerHigh,
        contentColor = scheme.onSurface,
        action = if (actionLabel == null) null else {
            {
                TextButton(onClick = { data.performAction() }) {
                    Text(actionLabel, color = scheme.primary,
                        style = MaterialTheme.typography.labelLarge)
                }
            }
        },
        dismissAction = if (data.visuals.withDismissAction) {
            {
                IconButton(onClick = { data.dismiss() }) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss message",
                        tint = scheme.onSurfaceVariant)
                }
            }
        } else null,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = when (variant) {
                    TempoSnackbarVariant.Success -> TempoIcons.CheckCircle
                    TempoSnackbarVariant.Error -> TempoIcons.ErrorCircle
                    TempoSnackbarVariant.Warning -> TempoIcons.AlertCircle
                    TempoSnackbarVariant.Neutral -> TempoIcons.InfoCircle
                },
                contentDescription = null,
                tint = notificationTint,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = data.visuals.message,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurface,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}
