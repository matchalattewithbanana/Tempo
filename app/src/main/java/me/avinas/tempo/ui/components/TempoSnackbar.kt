@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.avinas.tempo.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.res.stringResource
import me.avinas.tempo.R
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.avinas.tempo.ui.theme.TempoError
import me.avinas.tempo.ui.theme.TempoPrimary
import me.avinas.tempo.ui.theme.TempoSuccess
import me.avinas.tempo.ui.theme.TempoWarning

/**
 * Existing callers can still supply severity for analytics and classification.
 * Snackbar presentation is intentionally neutral: Material reserves strong
 * warning/error treatments for inline errors and interruptive dialogs.
 */
enum class TempoSnackbarVariant(val tint: Color) {
    Success(TempoSuccess),
    Error(TempoError),
    Warning(TempoWarning),
    Neutral(TempoPrimary);

    companion object {
        fun fromMessage(message: String): TempoSnackbarVariant {
            val m = message.lowercase()
            return when {
                m.startsWith("success") || "successfully" in m -> Success
                "fail" in m || "error" in m || "unable" in m || "cannot" in m ||
                    "not available" in m -> Error
                else -> Neutral
            }
        }
    }
}

/**
 * Official M3 snackbar hierarchy: opaque inverseSurface, inverseOnSurface
 * message, inversePrimary action. No decorative status icon or glass border:
 * snackbars communicate a short non-blocking update, not a second hero.
 */
@Composable
fun TempoSnackbar(
    data: SnackbarData,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER")
    variant: TempoSnackbarVariant = TempoSnackbarVariant.fromMessage(data.visuals.message),
) {
    val colors = MaterialTheme.colorScheme
    Snackbar(
        modifier = modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        containerColor = colors.inverseSurface,
        contentColor = colors.inverseOnSurface,
        action = data.visuals.actionLabel?.let { label ->
            {
                TextButton(
                    onClick = data::performAction,
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.inversePrimary),
                ) {
                    Text(label, style = MaterialTheme.typography.labelLargeEmphasized)
                }
            }
        },
        dismissAction = if (data.visuals.withDismissAction) {
            {
                IconButton(onClick = data::dismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.album_close_cd),
                        tint = colors.inverseOnSurface,
                    )
                }
            }
        } else null,
    ) {
        Text(data.visuals.message, style = MaterialTheme.typography.bodyMedium)
    }
}
