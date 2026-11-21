package me.avinas.tempo.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.avinas.tempo.R
import me.avinas.tempo.service.TrackingServiceHeartbeat
import me.avinas.tempo.ui.components.SettingsOption
import me.avinas.tempo.ui.components.SettingsSectionHeader
import me.avinas.tempo.ui.components.SettingsSwitch
import me.avinas.tempo.ui.components.TempoSettingsGroup
import me.avinas.tempo.utils.OemBackgroundHelper

@Composable
internal fun TrackingSettingsSection(
    state: LazyListState,
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    notificationListenerGranted: Boolean,
    trackingHeartbeat: TrackingServiceHeartbeat.Snapshot?,
    trackingStatusCheckedAt: Long,
    batteryLevel: Int,
    isXiaomiDevice: Boolean,
    autostartState: OemBackgroundHelper.AutostartState,
    onNavigate: (SettingsDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    SettingsSectionList(state = state, modifier = modifier) {
        item(key = "tracking-source") {
            val trackingHealth = trackingHeartbeat?.let { heartbeat ->
                evaluateTrackingHealth(
                    permissionGranted = notificationListenerGranted,
                    listenerConnected = heartbeat.listenerConnected,
                    lastHeartbeatAtMs = heartbeat.lastServiceAliveAt,
                    lastNotificationCallbackAtMs = heartbeat.lastNotificationCallbackAt,
                    lastMediaSessionCallbackAtMs = heartbeat.lastMediaSessionCallbackAt,
                    nowMs = trackingStatusCheckedAt,
                )
            }
            val listenerStatus = stringResource(
                when (trackingHealth?.listenerState) {
                    null -> R.string.ui_loading
                    TrackingListenerState.PERMISSION_REQUIRED -> R.string.settings_health_permission_required
                    TrackingListenerState.WAITING -> R.string.settings_health_waiting
                    TrackingListenerState.STALE -> R.string.settings_health_stale
                    TrackingListenerState.CONNECTED -> R.string.settings_health_connected
                },
            )

            SettingsSectionHeader(stringResource(R.string.settings_tracking_access_status))
            TempoSettingsGroup {
                Column {
                    SettingsOption(
                        title = stringResource(R.string.settings_health_listener),
                        subtitle = listenerStatus,
                        showArrow = false,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsOption(
                        title = stringResource(R.string.settings_tracking_access_title),
                        subtitle = stringResource(
                            if (notificationListenerGranted) {
                                R.string.settings_tracking_access_granted
                            } else {
                                R.string.settings_tracking_access_missing
                            },
                        ),
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(intent)
                            } catch (_: ActivityNotFoundException) {
                                Toast.makeText(
                                    context,
                                    resources.getString(R.string.settings_notification_settings_error),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsOption(
                        title = stringResource(R.string.settings_manage_apps),
                        subtitle = stringResource(R.string.settings_manage_apps_desc),
                        onClick = { onNavigate(SettingsDestination.SUPPORTED_APPS) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsSwitch(
                        title = stringResource(R.string.settings_pause_tracking_low_battery),
                        subtitle = if (uiState.pauseTrackingOnLowBattery) {
                            stringResource(R.string.settings_pause_tracking_on_desc, batteryLevel)
                        } else {
                            stringResource(R.string.settings_pause_tracking_off_desc)
                        },
                        checked = uiState.pauseTrackingOnLowBattery,
                        onCheckedChange = viewModel::togglePauseTrackingOnLowBattery,
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            if (isXiaomiDevice) {
                SettingsSectionHeader(stringResource(R.string.settings_background_protection))
                TempoSettingsGroup {
                    SettingsOption(
                        title = if (autostartState == OemBackgroundHelper.AutostartState.DISABLED) {
                            stringResource(R.string.settings_configure_required)
                        } else {
                            stringResource(R.string.settings_xiaomi_settings)
                        },
                        subtitle = stringResource(R.string.settings_prevent_killed),
                        onClick = { onNavigate(SettingsDestination.BACKGROUND_PROTECTION) },
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        item(key = "tracking-filtering") {
            SettingsSectionHeader(stringResource(R.string.settings_content_filtering))
            TempoSettingsGroup {
                Column {
                    SettingsSwitch(
                        title = stringResource(R.string.settings_filter_podcasts),
                        subtitle = stringResource(R.string.settings_filter_podcasts_desc),
                        checked = uiState.filterPodcasts,
                        onCheckedChange = viewModel::toggleFilterPodcasts,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsSwitch(
                        title = stringResource(R.string.settings_filter_audiobooks),
                        subtitle = stringResource(R.string.settings_filter_audiobooks_desc),
                        checked = uiState.filterAudiobooks,
                        onCheckedChange = viewModel::toggleFilterAudiobooks,
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item(key = "tracking-statistics") {
            SettingsSectionHeader(stringResource(R.string.settings_advanced_stats))
            TempoSettingsGroup {
                SettingsSwitch(
                    title = stringResource(R.string.settings_extended_audio),
                    subtitle = stringResource(R.string.settings_extended_audio_desc),
                    checked = uiState.extendedAudioAnalysisEnabled,
                    onCheckedChange = viewModel::toggleExtendedAudioAnalysis,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
