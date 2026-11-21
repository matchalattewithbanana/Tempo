package me.avinas.tempo.ui.settings

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.compose.foundation.layout.Column
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
                        title = if (spotifyConnected) {
                            "Spotify API · Connected"
                        } else {
                            "Spotify API · Connect account"
                        },
                        subtitle = if (spotifyConnected) {
                            "Spotify authorization active. Use the history sync option below to import recent plays."
                        } else {
                            "Sign into Spotify in your browser to allow Tempo to import listening history."
                        },
                        onClick = {
                            if (!spotifyConnected) {
                                scope.launch {
                                    val intent = try {
                                        viewModel.startSpotifyAuthorization(spotifyClientId)
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (_: Exception) {
                                        Toast.makeText(
                                            context,
                                            resources.getString(R.string.spotify_secure_storage_unavailable),
                                            Toast.LENGTH_LONG,
                                        ).show()
                                        return@launch
                                    }
                                    if (intent == null) {
                                        Toast.makeText(
                                            context,
                                            "Enter a valid Spotify Client ID first.",
                                            Toast.LENGTH_LONG,
                                        ).show()
                                    } else {
                                        try {
                                            context.startActivity(intent)
                                        } catch (_: ActivityNotFoundException) {
                                            Toast.makeText(
                                                context,
                                                "No browser available for Spotify login.",
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
                            label = { Text("Spotify Developer Client ID (32 characters)") },
                            supportingText = {
                                Text(
                                    "Create a Spotify Developer app and register " +
                                        "tempo://spotify-callback as its redirect URI.",
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        )
                    }

                    if (spotifyConnected) {
                        SettingsOption(
                            title = "Reconnect Spotify / update permissions",
                            subtitle = "Reconnect to grant current playback-state access for " +
                                "Spotify Connect verification. Existing listening history is retained.",
                            onClick = {
                                scope.launch {
                                    viewModel.disconnectSpotify()
                                    onSpotifyConnectedChange(false)
                                    Toast.makeText(
                                        context,
                                        "Spotify disconnected. Tap Connect account to authorize updated permissions.",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                }
                            },
                        )
                    }

                    if (spotifyConnected && uiState.spotifyApiOnlyMode) {
                        SettingsOption(
                            title = "Sync Spotify history now",
                            subtitle = "Fetch recently played tracks immediately; Spotify may not " +
                                "yet have reported the latest play.",
                            onClick = {
                                SpotifyPollingWorker.syncNow(context)
                                Toast.makeText(
                                    context,
                                    "Spotify sync requested",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                        )
                    }

                    SettingsSwitch(
                        title = "Spotify API history sync",
                        subtitle = "Automatic Spotify history reconciliation (about every 20 minutes) " +
                            "alongside local broadcasts and MediaSession. Requires Spotify login.",
                        checked = uiState.spotifyApiOnlyMode,
                        onCheckedChange = { enabled ->
                            if (enabled && !spotifyConnected) {
                                Toast.makeText(
                                    context,
                                    "Connect your Spotify account before using Spotify API sync.",
                                    Toast.LENGTH_LONG,
                                ).show()
                            } else {
                                viewModel.toggleSpotifyApiOnlyMode(enabled)
                            }
                        },
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    SettingsSwitch(
                        title = "Spotify Device Broadcast Status",
                        subtitle = "Real-time local Spotify playback updates. Turn on the matching " +
                            "switch in Spotify > Settings > Playback. Requires Tempo tracking service.",
                        checked = spotifyBroadcastEnabled,
                        onCheckedChange = { enabled ->
                            SpotifyBroadcastPresence.setEnabled(context, enabled)
                            onSpotifyBroadcastEnabledChange(enabled)
                        },
                    )
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
