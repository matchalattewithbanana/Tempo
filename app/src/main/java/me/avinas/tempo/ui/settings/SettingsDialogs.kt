package me.avinas.tempo.ui.settings

import android.app.Activity
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.os.LocaleListCompat
import me.avinas.tempo.R
import me.avinas.tempo.data.importexport.ImportConflictStrategy
import me.avinas.tempo.data.importexport.ImportExportProgress
import me.avinas.tempo.data.local.entities.AlbumArtSource
import me.avinas.tempo.ui.components.TempoDialogBody
import me.avinas.tempo.ui.components.TempoDialogButtonRow
import me.avinas.tempo.ui.components.TempoDialogDangerButton
import me.avinas.tempo.ui.components.TempoDialogIcon
import me.avinas.tempo.ui.components.TempoDialogSecondaryButton
import me.avinas.tempo.ui.components.TempoDialogSurface
import me.avinas.tempo.ui.components.TempoDialogTextField
import me.avinas.tempo.ui.components.TempoDialogTitle
import me.avinas.tempo.ui.components.TempoIcons
import me.avinas.tempo.ui.theme.GlassFrostSoft
import me.avinas.tempo.ui.theme.TempoError
import me.avinas.tempo.ui.theme.TempoSurfaceDialog
import me.avinas.tempo.ui.theme.TextTertiary

internal enum class SettingsDialog {
    HISTORY_AUDIT,
    EDIT_NAME,
    LANGUAGE,
    COVER_SOURCE,
    CLEAR_DATA,
}

@Composable
internal fun SettingsDialogs(
    activeDialog: SettingsDialog?,
    onDismissLocalDialog: () -> Unit,
    uiState: SettingsUiState,
    historyQuality: HistoryQualityUiState,
    languages: List<Pair<String, String>>,
    currentLocale: String,
    coverSourceOptions: List<Pair<AlbumArtSource?, String>>,
    importExportProgress: ImportExportProgress?,
    conflictDialogUri: Uri?,
    resetInProgress: Boolean,
    viewModel: SettingsViewModel,
) {
    val context = LocalContext.current
    var historyBackupConfirmed by rememberSaveable(activeDialog) { mutableStateOf(false) }
    var tempName by rememberSaveable(activeDialog, uiState.userName) {
        mutableStateOf(uiState.userName)
    }

    when (activeDialog) {
        SettingsDialog.HISTORY_AUDIT -> {
            AlertDialog(
                onDismissRequest = onDismissLocalDialog,
                title = { Text("Listening-history accuracy") },
                text = {
                    Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                        if (historyQuality.isLoading) {
                            CircularProgressIndicator()
                            Text("Checking stored listening events…")
                        } else if (historyQuality.error != null) {
                            Text(historyQuality.error ?: "An unknown error occurred")
                        } else {
                            Text(
                                "Old Spotify API estimates: ${historyQuality.apiEstimatedCount} plays. " +
                                    "Reconstructed history: ${historyQuality.reconstructedCount} plays.",
                            )
                            Text(
                                "The Spotify recently-played API never supplied listening seconds. " +
                                    "The optional repair preserves play records and removes only invented " +
                                    "listening durations and completion for this exact API source.",
                            )
                            Text(
                                "Exact duplicate records: ${historyQuality.exactDuplicateCount}. " +
                                    "Only identical, fingerprinted events are eligible for safe removal; " +
                                    "uncertain overlaps and older unmarked plays are not deleted.",
                            )
                            historyQuality.repairedDuplicatesCount?.let { removed ->
                                Text("Removed $removed exact duplicate records. Listening statistics will refresh.")
                            }
                            if (historyQuality.reconstructedCount > 0) {
                                Text("Reconstructed plays are flagged for review, not automatically deleted.")
                            }
                            historyQuality.repairedCount?.let { repaired ->
                                Text("Corrected $repaired API records. Reopen statistics to see refreshed totals.")
                            }
                            val repairAvailable =
                                (historyQuality.apiEstimatedCount > 0 &&
                                    historyQuality.repairedCount == null) ||
                                    (historyQuality.exactDuplicateCount > 0 &&
                                        historyQuality.repairedDuplicatesCount == null)
                            if (repairAvailable) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = historyBackupConfirmed,
                                        onCheckedChange = { historyBackupConfirmed = it },
                                    )
                                    Text(
                                        "I have exported a backup. I authorize correcting unverified " +
                                            "API durations or removing exact duplicate records only when I select an action.",
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Column(horizontalAlignment = Alignment.End) {
                        if (
                            historyQuality.apiEstimatedCount > 0 &&
                            historyQuality.repairedCount == null &&
                            historyQuality.error == null
                        ) {
                            TextButton(
                                enabled = historyBackupConfirmed && !historyQuality.isLoading,
                                onClick = {
                                    historyBackupConfirmed = false
                                    viewModel.repairHistoryAfterBackupConfirmation()
                                },
                            ) { Text("Correct API estimates") }
                        }
                        if (
                            historyQuality.exactDuplicateCount > 0 &&
                            historyQuality.repairedDuplicatesCount == null &&
                            historyQuality.error == null
                        ) {
                            TextButton(
                                enabled = historyBackupConfirmed && !historyQuality.isLoading,
                                onClick = {
                                    historyBackupConfirmed = false
                                    viewModel.repairExactDuplicatesAfterBackupConfirmation()
                                },
                            ) { Text("Remove exact duplicates") }
                        }
                        if (
                            historyQuality.apiEstimatedCount == 0 &&
                            historyQuality.exactDuplicateCount == 0
                        ) {
                            TextButton(onClick = onDismissLocalDialog) { Text("Close") }
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissLocalDialog) { Text("Cancel") }
                },
            )
        }

        SettingsDialog.EDIT_NAME -> {
            Dialog(onDismissRequest = onDismissLocalDialog) {
                TempoDialogSurface {
                    TempoDialogIcon(
                        icon = TempoIcons.Edit,
                        tint = MaterialTheme.colorScheme.primary,
                        size = 48,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TempoDialogTitle(text = stringResource(R.string.settings_update_name))
                    Spacer(modifier = Modifier.height(6.dp))
                    TempoDialogBody(text = stringResource(R.string.settings_name_dialog_body))
                    Spacer(modifier = Modifier.height(20.dp))
                    TempoDialogTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = stringResource(R.string.settings_display_name),
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    TempoDialogButtonRow(
                        primaryText = stringResource(R.string.settings_save),
                        onPrimary = {
                            if (tempName.isNotBlank()) {
                                viewModel.updateUserName(tempName.trim())
                                onDismissLocalDialog()
                            }
                        },
                        secondaryText = stringResource(R.string.settings_cancel),
                        onSecondary = onDismissLocalDialog,
                        primaryEnabled = tempName.isNotBlank(),
                    )
                }
            }
        }

        SettingsDialog.LANGUAGE -> {
            AlertDialog(
                onDismissRequest = onDismissLocalDialog,
                containerColor = TempoSurfaceDialog,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Text(
                        stringResource(R.string.settings_select_language),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                text = {
                    Column {
                        languages.forEach { (langTag, langName) ->
                            val selectLanguage = {
                                val localeList = LocaleListCompat.forLanguageTags(langTag)
                                AppCompatDelegate.setApplicationLocales(localeList)
                                onDismissLocalDialog()
                                (context as? Activity)?.recreate()
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(onClick = selectLanguage)
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = currentLocale == langTag,
                                    onClick = selectLanguage,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = langName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = onDismissLocalDialog) {
                        Text(stringResource(R.string.settings_cancel), color = TextTertiary)
                    }
                },
            )
        }

        SettingsDialog.COVER_SOURCE -> {
            AlertDialog(
                onDismissRequest = onDismissLocalDialog,
                containerColor = TempoSurfaceDialog,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Text(
                        stringResource(R.string.settings_cover_source_title),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                text = {
                    Column {
                        Text(
                            stringResource(R.string.settings_cover_source_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        coverSourceOptions.forEach { (source, name) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .selectable(
                                        selected = source == uiState.preferredCoverSource,
                                        onClick = {
                                            viewModel.setPreferredCoverSource(source)
                                            onDismissLocalDialog()
                                        },
                                        role = Role.RadioButton,
                                    )
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = source == uiState.preferredCoverSource,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = onDismissLocalDialog) {
                        Text(stringResource(R.string.settings_cancel), color = TextTertiary)
                    }
                },
            )
        }

        SettingsDialog.CLEAR_DATA -> {
            Dialog(
                onDismissRequest = { if (!resetInProgress) onDismissLocalDialog() },
                properties = DialogProperties(
                    dismissOnBackPress = !resetInProgress,
                    dismissOnClickOutside = !resetInProgress,
                ),
            ) {
                TempoDialogSurface {
                    TempoDialogIcon(
                        icon = TempoIcons.Trash,
                        tint = TempoError,
                        size = 48,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TempoDialogTitle(text = stringResource(R.string.settings_clear_data_title))
                    Spacer(modifier = Modifier.height(8.dp))
                    TempoDialogBody(text = stringResource(R.string.settings_clear_data_msg))
                    Spacer(modifier = Modifier.height(24.dp))
                    TempoDialogDangerButton(
                        text = stringResource(
                            if (resetInProgress) {
                                R.string.settings_clear_data_requesting
                            } else {
                                R.string.settings_clear_everything
                            },
                        ),
                        enabled = !resetInProgress,
                        onClick = viewModel::clearAllData,
                        icon = TempoIcons.Trash,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    TempoDialogSecondaryButton(
                        text = stringResource(R.string.settings_cancel),
                        enabled = !resetInProgress,
                        onClick = onDismissLocalDialog,
                    )
                }
            }
        }

        null -> Unit
    }

    importExportProgress?.let { progress ->
        AlertDialog(
            onDismissRequest = {},
            containerColor = TempoSurfaceDialog,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    stringResource(R.string.settings_processing),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = progress.phase,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (progress.isIndeterminate) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        LinearProgressIndicator(
                            progress = { progress.percentage.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = GlassFrostSoft,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${progress.current}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary,
                        )
                    }
                }
            },
            confirmButton = {},
        )
    }

    conflictDialogUri?.let { uri ->
        AlertDialog(
            onDismissRequest = viewModel::cancelImport,
            containerColor = TempoSurfaceDialog,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    stringResource(R.string.settings_import_options),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = {
                Text(
                    stringResource(R.string.settings_import_conflict_msg),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.importData(uri, ImportConflictStrategy.REPLACE) },
                ) {
                    Text(
                        stringResource(R.string.settings_replace_existing),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = viewModel::cancelImport) {
                        Text(stringResource(R.string.settings_cancel), color = TextTertiary)
                    }
                    TextButton(
                        onClick = { viewModel.importData(uri, ImportConflictStrategy.SKIP) },
                    ) {
                        Text(
                            stringResource(R.string.settings_skip_duplicates),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
        )
    }
}
