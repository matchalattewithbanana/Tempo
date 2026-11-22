@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

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

/** A narrow phone or large accessibility text gets a full-width action button. */
internal fun backupActionsStack(widthDp: Float, fontScale: Float): Boolean =
    widthDp < 440f || fontScale >= 1.3f

/** Device backup actions remain separate from Google's authentication state. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LocalBackupSection(
    uiState: BackupRestoreUiState,
    onToggleLocalImages: (Boolean) -> Unit,
    onChooseAutoBackupFolder: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    SettingsSectionHeader(stringResource(R.string.backup_restore_local_backup_header))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column {
            LocalBackupActionRow(
                title = stringResource(R.string.backup_restore_folder_title),
                supporting = stringResource(R.string.backup_restore_folder_desc),
                buttonLabel = stringResource(R.string.backup_restore_choose_folder),
                icon = Icons.Default.FolderOpen,
                primary = false,
                onClick = onChooseAutoBackupFolder,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingsSwitch(
                title = stringResource(R.string.backup_restore_include_local_images),
                subtitle = if (uiState.localImageCount > 0) {
                    stringResource(
                        R.string.backup_restore_local_images_desc,
                        uiState.localImageCount, uiState.localImageSizeFormatted,
                    )
                } else {
                    stringResource(R.string.backup_restore_no_local_images)
                },
                checked = uiState.includeLocalImages,
                onCheckedChange = onToggleLocalImages,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            LocalBackupActionRow(
                title = stringResource(R.string.backup_restore_export_device),
                supporting = stringResource(R.string.backup_restore_estimated_size,
                    uiState.estimatedExportSizeFormatted),
                buttonLabel = stringResource(R.string.backup_restore_export_button),
                icon = Icons.Default.Save,
                primary = true,
                onClick = onExport,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            LocalBackupActionRow(
                title = stringResource(R.string.backup_restore_import_device),
                supporting = stringResource(R.string.backup_restore_import_file_desc),
                buttonLabel = stringResource(R.string.backup_restore_import_button),
                icon = Icons.Default.FolderOpen,
                primary = false,
                onClick = onImport,
            )
        }
    }
}

/**
 * One responsive action pattern for choose-folder, export and import.
 * Buttons never steal the width needed for the supporting description.
 */
@Composable
private fun LocalBackupActionRow(
    title: String,
    supporting: String,
    buttonLabel: String,
    icon: ImageVector,
    primary: Boolean,
    onClick: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = backupActionsStack(maxWidth.value, androidx.compose.ui.platform.LocalDensity.current.fontScale)
        if (stacked) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                LocalBackupActionDescription(title, supporting)
                LocalBackupActionButton(buttonLabel, icon, primary, true, onClick)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    LocalBackupActionDescription(title, supporting)
                }
                LocalBackupActionButton(buttonLabel, icon, primary, false, onClick)
            }
        }
    }
}

@Composable
private fun LocalBackupActionDescription(title: String, supporting: String) {
    Text(title, style = MaterialTheme.typography.titleMediumEmphasized)
    Spacer(Modifier.height(4.dp))
    Text(
        supporting,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun LocalBackupActionButton(
    label: String,
    icon: ImageVector,
    primary: Boolean,
    fullWidth: Boolean,
    onClick: () -> Unit,
) {
    val modifier = Modifier.heightIn(min = 48.dp)
        .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
    if (primary) {
        Button(onClick = onClick, modifier = modifier, shapes = ButtonDefaults.shapes()) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier, shapes = ButtonDefaults.shapes()) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label)
        }
    }
}

@Composable
internal fun BackupTipsCard() {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = scheme.secondaryContainer,
        contentColor = scheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = MaterialShapes.Cookie6Sided.toShape(),
                    color = scheme.secondary,
                    contentColor = scheme.onSecondary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null,
                            modifier = Modifier.size(22.dp))
                    }
                }
                Text(
                    stringResource(R.string.backup_restore_tips_title),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                )
            }
            Text(
                "• Google Drive backups are stored in your Drive account\n" +
                    "• Only the 5 most recent backups are kept\n" +
                    "• Album art from streaming services will be re-downloaded\n" +
                    "• Local album art is only included if toggled on",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSecondaryContainer,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.3,
            )
        }
    }
}

@Composable
internal fun DataStatItem(
    icon: ImageVector,
    count: Int,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = formatCount(count),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1000 -> String.format(java.util.Locale.US, "%.1fK", count / 1000.0)
        else -> count.toString()
    }
}

internal fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

internal fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000} min ago"
        diff < 86_400_000 -> "${diff / 3_600_000} hours ago"
        diff < 604_800_000 -> "${diff / 86_400_000} days ago"
        else -> formatDate(timestamp)
    }
}
