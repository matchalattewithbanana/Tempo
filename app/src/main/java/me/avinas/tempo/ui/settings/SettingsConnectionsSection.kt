package me.avinas.tempo.ui.settings

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import me.avinas.tempo.R
import me.avinas.tempo.service.SpotifyBroadcastPresence
import me.avinas.tempo.ui.components.SettingsOption
import me.avinas.tempo.ui.components.SettingsSectionHeader
import me.avinas.tempo.ui.components.SettingsSwitch
import me.avinas.tempo.ui.components.TempoSettingsGroup
import me.avinas.tempo.worker.SpotifyPollingWorker

@Composable
internal fun ConnectionsSettingsSection(
    state: LazyListState,
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    spotifyConnected: Boolean,
    spotifyAccountLoaded: Boolean,
    spotifyClientId: String,
    onSpotifyClientIdChange: (String) -> Unit,
    onSpotifyConnectedChange: (Boolean) -> Unit,
    spotifyBroadcastEnabled: Boolean,
    onSpotifyBroadcastEnabledChange: (Boolean) -> Unit,
    batteryLevel: Int,
    isBatteryCritical: Boolean,
    isLowBattery: Boolean,
    onNavigate: (SettingsDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()

    SettingsSectionList(
        state = state,
        tone = SettingsSection.CONNECTIONS.tone,
        modifier = modifier,
    ) {
        item(key = "connections-services") {
            SettingsSectionHeader(stringResource(R.string.settings_connected_accounts))
            TempoSettingsGroup {
                Column {
                    SettingsOption(
                        title = if (uiState.isLastFmConnected) {
                            stringResource(
                                R.string.settings_lastfm_connected,
                                uiState.lastFmUsername ?: "Connected",
                            )
                        } else {
                            stringResource(R.string.settings_import_lastfm)
                        },
                        subtitle = if (uiState.isLastFmConnected) {
                            when (uiState.lastFmSyncFrequency) {
                                "DAILY" -> stringResource(R.string.settings_lastfm_sync_daily)
                                "WEEKLY" -> stringResource(R.string.settings_lastfm_sync_weekly)
                                else -> stringResource(R.string.settings_lastfm_import_complete)
                            }
                        } else {
                            stringResource(R.string.settings_import_lastfm_desc)
                        },
                        onClick = { onNavigate(SettingsDestination.LASTFM_IMPORT) },
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    SettingsOption(
                        title = stringResource(R.string.settings_spotify_service_title),
                        subtitle = stringResource(
                            if (spotifyConnected) {
                                R.string.settings_spotify_service_connected
                            } else {
                                R.string.settings_spotify_service_disconnected
                            },
                        ),
                        onClick = if (spotifyConnected) {
                            null
                        } else {
                            {
                                scope.launch {
                                    val intent = try {
                                        viewModel.startSpotifyAuthorization(spotifyClientId)
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (_: Exception) {
                                        Toast.makeText(
                                            context,
                                            resources.getString(
                                                R.string.spotify_secure_storage_unavailable,
                                            ),
                                            Toast.LENGTH_LONG,
                                        ).show()
                                        return@launch
                                    }
                                    if (intent == null) {
                                        Toast.makeText(
                                            context,
                                            resources.getString(
                                                R.string.settings_spotify_client_id_required,
                                            ),
                                            Toast.LENGTH_LONG,
                                        ).show()
                                    } else {
                                        try {
                                            context.startActivity(intent)
                                        } catch (_: ActivityNotFoundException) {
                                            Toast.makeText(
                                                context,
                                                resources.getString(
                                                    R.string.settings_spotify_no_browser,
                                                ),
                                                Toast.LENGTH_LONG,
                                            ).show()
                                        }
                                    }
                                }
                            }
                        },
                    )

                    if (spotifyAccountLoaded && !spotifyConnected) {
                        OutlinedTextField(
                            value = spotifyClientId,
                            onValueChange = { onSpotifyClientIdChange(it.trim()) },
                            label = {
                                Text(stringResource(R.string.settings_spotify_client_id_label))
                            },
                            supportingText = {
                                Text(stringResource(R.string.settings_spotify_client_id_help))
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        )
                    }

                    // Local Spotify playback broadcasts do not require a Spotify API
                    // login, and must remain configurable even while disconnected.
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsSwitch(
                        title = stringResource(R.string.settings_spotify_device_broadcasts),
                        subtitle = stringResource(R.string.settings_spotify_device_broadcasts_desc),
                        checked = spotifyBroadcastEnabled,
                        onCheckedChange = { enabled ->
                            SpotifyBroadcastPresence.setEnabled(context, enabled)
                            onSpotifyBroadcastEnabledChange(enabled)
                        },
                    )

                    if (spotifyConnected) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsSwitch(
                            title = stringResource(
                                R.string.settings_spotify_history_verification,
                            ),
                            subtitle = stringResource(
                                R.string.settings_spotify_history_verification_desc,
                            ),
                            checked = uiState.spotifyApiOnlyMode,
                            onCheckedChange = viewModel::toggleSpotifyApiOnlyMode,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            if (uiState.spotifyApiOnlyMode) {
                                TextButton(
                                    onClick = {
                                        SpotifyPollingWorker.syncNow(context)
                                        Toast.makeText(
                                            context,
                                            resources.getString(
                                                R.string.settings_spotify_sync_requested,
                                            ),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    },
                                ) {
                                    Text(stringResource(R.string.settings_spotify_verify_now))
                                }
                            }
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        viewModel.disconnectSpotify()
                                        onSpotifyConnectedChange(false)
                                        Toast.makeText(
                                            context,
                                            resources.getString(
                                                R.string.settings_spotify_reconnect_done,
                                            ),
                                            Toast.LENGTH_LONG,
                                        ).show()
                                    }
                                },
                            ) {
                                Text(stringResource(R.string.settings_spotify_reconnect))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item(key = "connections-desktop") {
            SettingsSectionHeader(stringResource(R.string.desktop_link_section_header))
            TempoSettingsGroup {
                Column {
                    val scheme = MaterialTheme.colorScheme
                    val (statusContainer, statusForeground) = when {
                        isBatteryCritical -> scheme.errorContainer to scheme.onErrorContainer
                        isLowBattery -> scheme.tertiaryContainer to scheme.onTertiaryContainer
                        else -> scheme.secondaryContainer to scheme.onSecondaryContainer
                    }
                    val statusText = when {
                        isBatteryCritical -> R.string.settings_desktop_battery_critical
                        isLowBattery -> R.string.settings_desktop_battery_low
                        else -> R.string.settings_desktop_battery_healthy
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = statusContainer,
                        contentColor = statusForeground,
                    ) {
                        Text(
                            text = stringResource(statusText, batteryLevel),
                            style = MaterialTheme.typography.bodySmall,
                            color = statusForeground,
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                        )
                    }
                    HorizontalDivider(color = scheme.outlineVariant)
                    SettingsOption(
                        title = stringResource(R.string.desktop_link_settings_title),
                        subtitle = stringResource(R.string.desktop_link_settings_subtitle),
                        onClick = { onNavigate(SettingsDestination.DESKTOP) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
