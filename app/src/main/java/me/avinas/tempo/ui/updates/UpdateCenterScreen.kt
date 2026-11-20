package me.avinas.tempo.ui.updates

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.*
import me.avinas.tempo.ui.components.TempoCircularLoadingIndicator
import me.avinas.tempo.ui.components.TempoLinearLoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.avinas.tempo.R
import me.avinas.tempo.data.update.UpdateChannel
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateCenterScreen(
    onNavigateBack: () -> Unit,
    viewModel: UpdateCenterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.update_center_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "summary") {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Outlined.SystemUpdate, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(32.dp))
                        Text(stringResource(R.string.update_current_version, state.installedVersion),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            when {
                                state.checking -> stringResource(R.string.update_checking)
                                state.newerAvailable -> stringResource(R.string.update_available_status)
                                state.release == null && state.error == null -> stringResource(R.string.update_no_release)
                                state.error != null -> stringResource(R.string.update_check_failed)
                                else -> stringResource(R.string.update_up_to_date)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        if (state.checking) TempoLinearLoadingIndicator(modifier = Modifier.fillMaxWidth())
                        OutlinedButton(onClick = viewModel::refresh, enabled = !state.checking,
                            modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.update_check_now))
                        }
                    }
                }
            }

            item(key = "preferences") {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.update_channel_title),
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            UpdateChannel.entries.forEach { channel ->
                                FilterChip(
                                    selected = state.channel == channel,
                                    onClick = { viewModel.selectChannel(channel) },
                                    label = { Text(stringResource(if (channel == UpdateChannel.STABLE)
                                        R.string.update_stable else R.string.update_preview)) },
                                )
                            }
                        }
                        Text(stringResource(if (state.channel == UpdateChannel.STABLE)
                            R.string.update_stable_desc else R.string.update_preview_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.update_auto_check),
                                    style = MaterialTheme.typography.bodyLarge)
                                Text(stringResource(R.string.update_auto_check_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = state.autoCheck,
                                onCheckedChange = viewModel::setAutomaticChecks)
                        }
                        if (state.lastCheckedAt > 0L) {
                            Text(stringResource(R.string.update_last_checked,
                                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                    .format(Date(state.lastCheckedAt))),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            state.release?.let { release ->
                item(key = "release") {
                    Surface(shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        Column(Modifier.fillMaxWidth().padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(R.string.update_latest_release),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(release.name.ifBlank { release.tag },
                                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(release.tag + if (release.prerelease) " · " +
                                stringResource(R.string.update_preview) else "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary)
                            if (release.publishedAt.length >= 10) Text(release.publishedAt.take(10),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(stringResource(R.string.update_release_notes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)
                            Text(release.notes.ifBlank { stringResource(R.string.update_no_notes) }.take(6000),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (state.newerAvailable) {
                                if (state.downloading) {
                                    TempoLinearLoadingIndicator(progress = { state.percent / 100f },
                                        modifier = Modifier.fillMaxWidth())
                                    Text(stringResource(R.string.update_download_progress, state.percent),
                                        style = MaterialTheme.typography.labelMedium)
                                    TextButton(onClick = viewModel::cancelDownload) {
                                        Text(stringResource(R.string.settings_cancel))
                                    }
                                } else if (state.downloaded) {
                                    Button(onClick = viewModel::install, enabled = !state.installing,
                                        modifier = Modifier.fillMaxWidth()) {
                                        Text(stringResource(if (state.installing)
                                            R.string.update_verifying else R.string.update_install))
                                    }
                                } else {
                                    Button(onClick = viewModel::startDownload,
                                        modifier = Modifier.fillMaxWidth()) {
                                        Text(stringResource(R.string.update_download,
                                            (release.apkBytes + 1_048_575L) / 1_048_576L))
                                    }
                                }
                                if (state.ignored) {
                                    Text(stringResource(R.string.update_ignored),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall)
                                } else if (!state.downloading) {
                                    TextButton(onClick = viewModel::ignoreThisRelease) {
                                        Text(stringResource(R.string.update_skip_release))
                                    }
                                }
                            }
                            Text(stringResource(R.string.update_install_warning),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedButton(
                                onClick = {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release.pageUrl)))
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text(stringResource(R.string.update_view_on_github)) }
                        }
                    }
                }
            }
            state.error?.let { message ->
                item(key = "error") {
                    Surface(shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.errorContainer) {
                        Text(message, modifier = Modifier.fillMaxWidth().padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }
    }
}
