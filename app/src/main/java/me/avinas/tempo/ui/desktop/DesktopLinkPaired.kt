package me.avinas.tempo.ui.desktop
import me.avinas.tempo.ui.util.observableLocale

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import me.avinas.tempo.R
import me.avinas.tempo.ui.components.GlassCard
import me.avinas.tempo.ui.components.GlassCardVariant
import me.avinas.tempo.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PairedContent(
    uiState: DesktopLinkUiState,
    context: Context,
    onDisconnect: () -> Unit
) {
    val session = uiState.session ?: return

    // Connected card
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        variant = GlassCardVariant.LowProminence
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = if (uiState.isServerRunning) TempoSuccessBright else TempoWarningBright,
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (uiState.isServerRunning)
                        stringResource(R.string.desktop_receiver_running, uiState.phonePort)
                    else
                        stringResource(R.string.desktop_receiver_stopped),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            InfoRow(
                label = stringResource(R.string.desktop_connected_to),
                value = session.desktopIp?.let { "${it}:${session.desktopPort ?: "?"}" }
                    ?: stringResource(R.string.desktop_manual_entry)
            )
            Spacer(modifier = Modifier.height(6.dp))
            InfoRow(
                label = stringResource(R.string.desktop_phone_address),
                value = "${uiState.phoneIp}:${uiState.phonePort}",
                monospace = true
            )
            if (session.deviceName.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                InfoRow(
                    label = stringResource(R.string.desktop_device_name),
                    value = session.deviceName
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Token card (copy-able, desktop needs this to authenticate)
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        variant = GlassCardVariant.LowProminence
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.desktop_auth_token),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                StatusPill(
                    text = if (uiState.isServerRunning) "Listening" else "Paused",
                    color = if (uiState.isServerRunning) TempoSuccessBright else TempoWarningBright
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = session.authToken,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Tempo Token", session.authToken))
                },
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.desktop_copy_code),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }

    // Last sync card
    session.lastSeenMs?.let { lastSeen ->
        if (lastSeen > 0L) {
            Spacer(modifier = Modifier.height(16.dp))
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                variant = GlassCardVariant.LowProminence
            ) {
                InfoRow(
                    label = stringResource(R.string.desktop_last_sync),
                    value = SimpleDateFormat("MMM d, HH:mm", observableLocale()).format(Date(lastSeen))
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Disconnect button
    OutlinedButton(
        onClick = onDisconnect,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TempoError),
        border = BorderStroke(1.dp, TempoError.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Filled.LinkOff,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(stringResource(R.string.desktop_disconnect))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Desktop Stats section (shown when paired and has data)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun DesktopStatsSection(stats: DesktopStats) {
    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = stringResource(R.string.desktop_stats_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Stats overview row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DesktopStatCard(
            label = stringResource(R.string.desktop_stats_total_scrobbles),
            value = stats.totalPlays.toString(),
            icon = Icons.Filled.MusicNote,
            modifier = Modifier.weight(1f)
        )
        DesktopStatCard(
            label = stringResource(R.string.desktop_stats_listening_time),
            value = formatListeningTime(stats.totalListeningTimeMs),
            icon = Icons.Filled.Schedule,
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Last 7 days row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DesktopStatCard(
            label = stringResource(R.string.desktop_stats_7day_scrobbles),
            value = stats.last7DaysPlays.toString(),
            icon = Icons.Filled.DateRange,
            modifier = Modifier.weight(1f)
        )
        DesktopStatCard(
            label = stringResource(R.string.desktop_stats_7day_time),
            value = formatListeningTime(stats.last7DaysListeningTimeMs),
            icon = Icons.Filled.Timer,
            modifier = Modifier.weight(1f)
        )
    }

    // Top artist / track
    if (stats.topArtist != null || stats.topTrack != null) {
        Spacer(modifier = Modifier.height(12.dp))

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            variant = GlassCardVariant.LowProminence
        ) {
            Column {
                Text(
                    text = stringResource(R.string.desktop_stats_top_listened),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                stats.topArtist?.let { artist ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.desktop_stats_top_artist),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = artist,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                stats.topTrack?.let { track ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.desktop_stats_top_track),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (stats.topTrackArtist != null) "$track — ${stats.topTrackArtist}" else track,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    // Source breakdown
    if (stats.sourceBreakdown.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            variant = GlassCardVariant.LowProminence
        ) {
            Column {
                Text(
                    text = stringResource(R.string.desktop_stats_sources),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                stats.sourceBreakdown.forEach { source ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = source.source.removePrefix("desktop:"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${source.cnt}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopStatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(14.dp),
        variant = GlassCardVariant.LowProminence
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

private fun formatListeningTime(ms: Long): String {
    if (ms <= 0) return "0m"
    val totalMinutes = ms / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

// ─────────────────────────────────────────────────────────────────────────────
// Download Desktop App button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun DesktopAppDownloadButton(context: Context) {
    OutlinedButton(
        onClick = {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://tempo.avinas.me/desktop"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
            }
        },
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Filled.Download,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = stringResource(R.string.desktop_download_app),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.desktop_download_app_desc),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared helper composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InfoRow(
    label: String,
    value: String,
    monospace: Boolean = false,
    truncate: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            maxLines = if (truncate) 1 else Int.MAX_VALUE,
            overflow = if (truncate) TextOverflow.Ellipsis else TextOverflow.Clip,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
internal fun DesktopTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    monospace: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = MaterialTheme.typography.bodySmall.copy(
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = GlassBorderMedium,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    )
}
