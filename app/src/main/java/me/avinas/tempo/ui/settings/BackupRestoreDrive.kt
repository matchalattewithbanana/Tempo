package me.avinas.tempo.ui.settings

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import me.avinas.tempo.ui.components.TempoSettingsCategoryHero
import me.avinas.tempo.ui.components.TempoEmblemStyle
import me.avinas.tempo.ui.components.TempoCircularLoadingIndicator
import me.avinas.tempo.ui.components.TempoLinearLoadingIndicator
import me.avinas.tempo.ui.components.CachedAsyncImage
import me.avinas.tempo.data.drive.*
import me.avinas.tempo.data.importexport.ImportConflictStrategy
import me.avinas.tempo.data.importexport.ImportExportResult
import me.avinas.tempo.data.importexport.BackupAppearanceStatus
import me.avinas.tempo.ui.components.DeepOceanBackground
import me.avinas.tempo.ui.components.GlassCard
import me.avinas.tempo.ui.components.GlassCardVariant
import me.avinas.tempo.ui.components.SettingsSwitch
import me.avinas.tempo.ui.components.SettingsSectionHeader
import me.avinas.tempo.ui.components.TempoSnackbar
import me.avinas.tempo.ui.theme.*
import me.avinas.tempo.ui.components.TempoDropdownMenu
import me.avinas.tempo.ui.components.TempoDropdownMenuItem
import me.avinas.tempo.ui.components.TempoIcons
import me.avinas.tempo.utils.FormatUtils.formatBytes
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.res.stringResource
import me.avinas.tempo.R

/**
 * Dedicated Backup & Restore screen with Google Drive integration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GoogleDriveSection(
    isSignedIn: Boolean,
    googleAccount: GoogleAccount?,
    driveBackups: List<DriveBackupInfo>,
    backupSettings: BackupSettings,
    driveOperation: DriveOperationState,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onBackupNow: () -> Unit,
    onRestore: (DriveBackupInfo) -> Unit,
    onDelete: (DriveBackupInfo) -> Unit,
    onRefreshBackups: () -> Unit
) {
    SettingsSectionHeader("Google Drive")
    
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
        variant = GlassCardVariant.LowProminence
    ) {
        Column {
            if (!isSignedIn) {
                // Sign in prompt
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSignIn() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = TempoInfo.copy(alpha = 0.2f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Cloud,
                            contentDescription = null,
                            tint = TempoInfo,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Connect Google Account",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Back up your data to Google Drive",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Signed in account info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile picture or placeholder
                    if (googleAccount?.photoUrl != null) {
                        CachedAsyncImage(
                            imageUrl = googleAccount.photoUrl.toString(),
                            contentDescription = "Profile",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(TempoInfo, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = googleAccount?.email?.first()?.uppercase() ?: "G",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = googleAccount?.displayName ?: "Google Account",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = googleAccount?.email ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    TextButton(onClick = onSignOut) {
                        Text("Sign Out", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                HorizontalDivider(color = GlassBorderSoft)
                
                // The primary action belongs beneath its status text. Keeping
                // them in one narrow Row squeezed the backup description down
                // to a few characters per line on compact phones.
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Backup to Drive",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (backupSettings.lastBackupTime != null) {
                        Text(
                            text = stringResource(
                                R.string.settings_backup_overview_last_success,
                                formatRelativeTime(backupSettings.lastBackupTime),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = stringResource(
                            when (backupSettings.lastBackupStatus) {
                                BackupStatus.NEVER -> R.string.settings_backup_overview_never
                                BackupStatus.SUCCESS -> R.string.settings_backup_overview_success
                                BackupStatus.FAILED -> R.string.settings_backup_overview_failed
                                BackupStatus.IN_PROGRESS -> R.string.settings_backup_overview_running
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (backupSettings.lastBackupStatus == BackupStatus.FAILED) {
                            MaterialTheme.colorScheme.error
                        } else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.settings_backup_status_shared_destination),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        onClick = onBackupNow,
                        enabled = driveOperation == DriveOperationState.Idle &&
                            backupSettings.isGoogleDriveEnabled,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Backup Now")
                    }
                }

                // Backup history
                if (driveBackups.isNotEmpty()) {
                    HorizontalDivider(color = GlassBorderSoft)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Backup History (${driveBackups.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        IconButton(onClick = onRefreshBackups, modifier = Modifier.size(24.dp)) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    
                    driveBackups.forEach { backup ->
                        DriveBackupItem(
                            backup = backup,
                            onRestore = { onRestore(backup) },
                            onDelete = { onDelete(backup) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AutomaticBackupSection(
    backupSettings: BackupSettings,
    onSetInterval: (BackupInterval) -> Unit,
    onSetWifiOnly: (Boolean) -> Unit
) {
    SettingsSectionHeader("Automatic Backups")

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
        variant = GlassCardVariant.LowProminence
    ) {
        Column {
            var intervalExpanded by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { intervalExpanded = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-backup",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = backupSettings.backupInterval.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TempoDropdownMenu(
                        expanded = intervalExpanded,
                        onDismissRequest = { intervalExpanded = false }
                    ) {
                        BackupInterval.entries.forEach { interval ->
                            TempoDropdownMenuItem(
                                title = interval.displayName,
                                onClick = {
                                    onSetInterval(interval)
                                    intervalExpanded = false
                                },
                                isSelected = backupSettings.backupInterval == interval
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = GlassBorderSoft)

            SettingsSwitch(
                title = "Drive on Wi-Fi only",
                subtitle = "Device backups still run offline; only the Google Drive upload waits for Wi-Fi",
                checked = backupSettings.wifiOnly,
                onCheckedChange = onSetWifiOnly
            )
        }
    }
}

@Composable
private fun DriveBackupItem(
    backup: DriveBackupInfo,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatDate(backup.createdAt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row {
                Text(
                    text = formatBytes(backup.sizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (backup.deviceName != null) {
                    Text(
                        text = " • ${backup.deviceName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        
        Row {
            IconButton(onClick = onRestore) {
                Icon(
                    Icons.Default.CloudDownload,
                    contentDescription = "Restore",
                    tint = TempoInfo
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

