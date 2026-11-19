package me.avinas.tempo.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.avinas.tempo.R
import me.avinas.tempo.data.analytics.TempoFeature
import me.avinas.tempo.utils.ShareUtils

private sealed interface ImageDestination {
    data object Share : ImageDestination
    data class File(val uri: Uri) : ImageDestination
}

internal class CardShareActions(
    val busy: Boolean,
    val share: () -> Unit,
    val save: () -> Unit,
)

@Composable
internal fun rememberCardShareActions(
    controller: CaptureController,
    fileName: String = "tempo-card.png",
    feature: TempoFeature = TempoFeature.SHARE_CARD,
    onShared: suspend () -> Unit = {},
): CardShareActions {
    val context = LocalContext.current
    val latestOnShared by rememberUpdatedState(onShared)
    var destination by remember(controller) { mutableStateOf<ImageDestination?>(null) }

    val savePicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        if (uri != null && destination == null) {
            destination = ImageDestination.File(uri)
            controller.capture()
        }
    }

    LaunchedEffect(controller, context) {
        controller.capturedBitmap.collect { bitmap ->
            val request = destination
            val success = try {
                when (request) {
                    ImageDestination.Share -> ShareUtils.shareBitmap(context, bitmap, feature)
                    is ImageDestination.File -> ShareUtils.saveBitmap(context, bitmap, request.uri)
                    null -> false
                }
            } finally {
                bitmap.recycle()
                destination = null
            }
            when {
                request == null -> Unit
                !success -> Toast.makeText(context, R.string.share_export_failed, Toast.LENGTH_SHORT).show()
                request is ImageDestination.File -> Toast.makeText(context, R.string.share_image_saved, Toast.LENGTH_SHORT).show()
                else -> latestOnShared()
            }
        }
    }
    LaunchedEffect(controller, context) {
        controller.captureFailed.collect {
            if (destination != null) {
                destination = null
                Toast.makeText(context, R.string.share_export_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    return CardShareActions(
        busy = destination != null,
        share = {
            if (destination == null) {
                destination = ImageDestination.Share
                controller.capture()
            }
        },
        save = {
            if (destination == null) {
                try {
                    savePicker.launch(fileName)
                } catch (_: android.content.ActivityNotFoundException) {
                    Toast.makeText(context, R.string.share_export_failed, Toast.LENGTH_SHORT).show()
                }
            }
        },
    )
}

@Composable
internal fun ShareOutputButtons(
    actions: CardShareActions,
    modifier: Modifier = Modifier,
    text: String? = null,
    links: List<Pair<String, String>> = emptyList(),
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    val available = enabled && !actions.busy
    fun copy(value: String) {
        val copied = ShareUtils.copyText(context, value)
        if (!copied || android.os.Build.VERSION.SDK_INT < 33) {
            Toast.makeText(context, if (copied) R.string.share_copied else R.string.share_export_failed, Toast.LENGTH_SHORT).show()
        }
    }

    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = actions.share, enabled = available, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
            if (actions.busy) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text(stringResource(R.string.share_image))
        }
        OutlinedButton(onClick = actions.save, enabled = available, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.share_save_png))
        }
        if (!text.isNullOrBlank() || links.isNotEmpty()) {
            androidx.compose.foundation.layout.Box {
                TextButton(enabled = available, onClick = { menuOpen = true }) {
                    Text(stringResource(R.string.share_more))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (!text.isNullOrBlank()) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.share_as_text)) }, onClick = {
                            menuOpen = false
                            if (!ShareUtils.shareText(context, text)) {
                                Toast.makeText(context, R.string.share_export_failed, Toast.LENGTH_SHORT).show()
                            }
                        })
                        DropdownMenuItem(text = { Text(stringResource(R.string.share_copy_text)) }, onClick = {
                            menuOpen = false
                            copy(text)
                        })
                    }
                    links.forEach { (label, url) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = {
                            menuOpen = false
                            copy(url)
                        })
                    }
                }
            }
        }
    }
}

internal fun shareThemeLabel(theme: ShareTheme): Int = when (theme) {
    ShareTheme.MIDNIGHT -> R.string.stats_share_theme_midnight
    ShareTheme.GLASS -> R.string.share_theme_glass
    ShareTheme.ASCII -> R.string.share_theme_ascii
    ShareTheme.MINIMUM -> R.string.share_theme_minimum
    ShareTheme.DAYLIGHT -> R.string.stats_share_theme_daylight
    ShareTheme.GRAIN -> R.string.share_theme_grain
}
