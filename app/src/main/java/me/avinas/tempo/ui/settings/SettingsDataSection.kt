package me.avinas.tempo.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date
import me.avinas.tempo.R
import me.avinas.tempo.data.drive.BackupSettings
import me.avinas.tempo.data.drive.BackupStatus
import me.avinas.tempo.ui.components.SettingsOption
import me.avinas.tempo.ui.components.SettingsSectionHeader
import me.avinas.tempo.ui.components.SettingsSwitch
import me.avinas.tempo.ui.components.TempoSettingsGroup

@Composable
internal fun DataSettingsSection(
    state: LazyListState,
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    backupOverview: BackupSettings,
    selectedCoverSourceName: String,
    onNavigate: (SettingsDestination) -> Unit,
    onOpenHistoryAudit: () -> Unit,
    onSelectCoverSource: () -> Unit,
    onClearData: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSectionList(
        state = state,
        tone = SettingsSection.DATA.tone,
        modifier = modifier,
    ) {
        item(key = "data-backups") {
            SettingsSectionHeader(stringResource(R.string.settings_data_backup_section))
            val backupStatus = if (!backupOverview.isGoogleDriveEnabled) {
                R.string.settings_backup_overview_disabled
            } else {
                when (backupOverview.lastBackupStatus) {
                    BackupStatus.NEVER -> R.string.settings_backup_overview_never
                    BackupStatus.SUCCESS -> R.string.settings_backup_overview_success
                    BackupStatus.FAILED -> R.string.settings_backup_overview_failed
                    BackupStatus.IN_PROGRESS -> R.string.settings_backup_overview_running
                }
            }
            val backupSubtitle = backupOverview.lastBackupTime
                ?.takeIf { it > 0L && it <= System.currentTimeMillis() }
                ?.let { timestamp ->
                    val formatted = DateFormat.getDateTimeInstance(
                        DateFormat.SHORT,
                        DateFormat.SHORT,
                    ).format(Date(timestamp))
                    stringResource(R.string.settings_backup_overview_last_success, formatted)
                } ?: stringResource(backupStatus)
            TempoSettingsGroup {
                SettingsOption(
                    title = stringResource(R.string.settings_backup_restore),
                    subtitle = backupSubtitle,
                    onClick = { onNavigate(SettingsDestination.BACKUP) },
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        item(key = "data-imports") {
            SettingsSectionHeader(stringResource(R.string.settings_import_history))
            TempoSettingsGroup {
                Column {
                    SettingsSwitch(
                        title = stringResource(R.string.settings_smart_merge),
                        subtitle = stringResource(R.string.settings_smart_merge_desc),
                        checked = uiState.mergeAlternateVersions,
                        onCheckedChange = viewModel::toggleMergeAlternateVersions,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    val imports = listOf(
                        SettingsDestination.SPOTIFY_JSON_IMPORT,
                        SettingsDestination.APPLE_MUSIC_IMPORT,
                        SettingsDestination.DEEZER_IMPORT,
                        SettingsDestination.YOUTUBE_MUSIC_IMPORT,
                        SettingsDestination.MUSICOLET_IMPORT,
                        SettingsDestination.EXTENSION_IMPORT,
                    )
                    imports.forEachIndexed { index, destination ->
                        SettingsOption(
                            title = stringResource(destination.titleRes),
                            subtitle = stringResource(destination.subtitleRes),
                            onClick = { onNavigate(destination) },
                        )
                        if (index != imports.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        item(key = "data-accuracy") {
            SettingsSectionHeader(stringResource(R.string.settings_history_tools))
            TempoSettingsGroup {
                Column {
                    SettingsOption(
                        title = "Check listening-history accuracy",
                        subtitle = "Check duplicates and inaccurate estimates before repairing your backed-up history.",
                        onClick = onOpenHistoryAudit,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsOption(
                        title = stringResource(SettingsDestination.YOUR_DATA.titleRes),
                        subtitle = stringResource(SettingsDestination.YOUR_DATA.subtitleRes),
                        onClick = { onNavigate(SettingsDestination.YOUR_DATA) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsOption(
                        title = stringResource(SettingsDestination.ENRICHMENT_REPORT.titleRes),
                        subtitle = stringResource(SettingsDestination.ENRICHMENT_REPORT.subtitleRes),
                        onClick = { onNavigate(SettingsDestination.ENRICHMENT_REPORT) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsOption(
                        title = stringResource(R.string.settings_cover_source_title),
                        subtitle = selectedCoverSourceName,
                        onClick = onSelectCoverSource,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (uiState.analyticsConfigured) {
            item(key = "data-privacy") {
                SettingsSectionHeader(stringResource(R.string.settings_privacy_controls))
                TempoSettingsGroup {
                    Column {
                        SettingsSwitch(
                            title = stringResource(R.string.settings_analytics_toggle),
                            subtitle = stringResource(R.string.settings_analytics_toggle_desc),
                            checked = uiState.analyticsEnabled,
                            onCheckedChange = viewModel::setAnalyticsEnabled,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsOption(
                            title = stringResource(R.string.settings_analytics_reset_id),
                            subtitle = stringResource(R.string.settings_analytics_reset_id_desc),
                            onClick = viewModel::resetAnalyticsId,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        item(key = "data-reset") {
            SettingsSectionHeader(stringResource(R.string.settings_data_reset_section))
            TempoSettingsGroup {
                SettingsOption(
                    title = stringResource(R.string.settings_clear_all),
                    textColor = MaterialTheme.colorScheme.error,
                    onClick = onClearData,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
