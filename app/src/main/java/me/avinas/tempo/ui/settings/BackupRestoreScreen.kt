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
fun BackupRestoreScreen(
    onNavigateBack: () -> Unit,
    viewModel: BackupRestoreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val importExportProgress by viewModel.importExportProgress.collectAsState()
    val importExportResult by viewModel.importExportResult.collectAsState()
    val conflictDialogUri by viewModel.showConflictDialog.collectAsState()
    
    val scope = rememberCoroutineScope()
    
    // Google Drive states
    val googleAccount by viewModel.googleAccount.collectAsState()
    val isSignedIn by viewModel.isSignedIn.collectAsState()
    val driveBackups by viewModel.driveBackups.collectAsState()
    val backupSettings by viewModel.backupSettings.collectAsState()
    val driveOperation by viewModel.driveOperation.collectAsState()
    val driveRestoreDialog by viewModel.showDriveRestoreDialog.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Get Activity context for Credential Manager
    val context = LocalContext.current
    val resources = LocalResources.current
    val activity = context as? Activity
    
    // Track if we've shown the activity error to avoid spamming
    var activityErrorShown by remember { mutableStateOf(false) }

    // A backup/restore runs on an application scope and survives this screen,
    // but blocking back while one is active prevents the user from accidentally
    // leaving in the middle of a long operation with no visible progress.
    val isOperationActive = driveOperation is DriveOperationState.Downloading ||
        driveOperation is DriveOperationState.Restoring ||
        driveOperation is DriveOperationState.Uploading ||
        importExportProgress != null
    BackHandler(enabled = isOperationActive) { }
    // Handle sign-in request from ViewModel
    val signInRequested by viewModel.signInRequested.collectAsState()
    val sessionRestoreRequested by viewModel.sessionRestoreRequested.collectAsState()
    val consentRequested by viewModel.consentRequested.collectAsState()
    
    // Consent Flow Launcher
    val consentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onConsentComplete(result.resultCode == Activity.RESULT_OK)
    }
    
    // Bridge Activity context for sign-in
    LaunchedEffect(signInRequested) {
        if (signInRequested) {
            if (activity != null) {
                viewModel.onSignInReady(activity)
            } else {
                viewModel.cancelSignIn()
                if (!activityErrorShown) {
                    snackbarHostState.showSnackbar("Cannot sign in: Activity not available")
                    activityErrorShown = true
                }
            }
        }
    }
    
    // Bridge Activity context for session restore on screen launch
    LaunchedEffect(sessionRestoreRequested) {
        if (sessionRestoreRequested && activity != null) {
            viewModel.onSessionRestoreReady(activity)
        }
    }
    
    // Handle Drive Consent Flow
    LaunchedEffect(consentRequested) {
        if (consentRequested) {
            val pendingIntent = viewModel.getDriveConsentPendingIntent()
            if (pendingIntent != null) {
                try {
                    val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                    consentLauncher.launch(intentSenderRequest)
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to open consent screen: ${e.message}")
                    viewModel.onConsentComplete(false)
                }
            } else {
                // No pending intent means consent isn't actually needed
                viewModel.onConsentComplete(true)
            }
        }
    }

    
    // File picker for import (ZIP)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.startImport(it) }
    }
    
    // File creator for export (ZIP)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportData(it) }
    }
    
    // Show result snackbar after successful import/restore
    LaunchedEffect(importExportResult) {
        importExportResult?.let { result ->
            when (result) {
                is ImportExportResult.Success -> {
                    val summary = resources.getString(R.string.settings_import_success, result.totalRecords, result.imagesCount)
                    val appearance = when (result.appearanceStatus) {
                        BackupAppearanceStatus.INCLUDED -> R.string.backup_appearance_included
                        BackupAppearanceStatus.RESTORED -> R.string.backup_appearance_restored
                        BackupAppearanceStatus.UNCHANGED -> R.string.backup_appearance_unchanged
                        null -> null
                    }
                    val reconciliation = if (result.eventsSkipped > 0 || result.eventsReplaced > 0) {
                        resources.getString(
                            R.string.backup_restore_reconciliation_result,
                            result.eventsCount, result.eventsReplaced, result.eventsSkipped,
                        )
                    } else null
                    snackbarHostState.showSnackbar(
                        listOfNotNull(summary, reconciliation,
                            appearance?.let { resources.getString(it) }).joinToString("\n")
                    )
                }
                is ImportExportResult.Error -> {
                    snackbarHostState.showSnackbar(result.message)
                }
            }
            viewModel.clearImportExportResult()
        }
    }
    
    // Show Drive operation results
    LaunchedEffect(driveOperation) {
        when (val op = driveOperation) {
            is DriveOperationState.Success -> {
                snackbarHostState.showSnackbar(op.message)
                viewModel.clearDriveOperation(op)
            }
            is DriveOperationState.Error -> {
                snackbarHostState.showSnackbar(op.message)
                viewModel.clearDriveOperation(op)
            }
            else -> {}
        }
    }
    
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_backup_restore),
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        enabled = !isOperationActive
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) { TempoSnackbar(it) } }
    ) { padding ->
        // The root navigation already draws the selected Appearance backdrop.
        // Avoid a second full-screen gradient when displaying this page.
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            // Subpages use a readable centered pane at expanded widths.
            // Compose only the visible backup sections instead of the entire page.
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .widthIn(max = 960.dp)
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                item(key = "overview") {
                    TempoSettingsCategoryHero(
                        title = stringResource(R.string.settings_backup_restore),
                        subtitle = stringResource(R.string.backup_restore_appearance_scope),
                        icon = Icons.Default.CloudDownload,
                        style = TempoEmblemStyle.MEMORY,
                    )
                    Spacer(Modifier.height(16.dp))
                    DataOverviewSection(uiState)
                    Spacer(Modifier.height(16.dp))
                }
                item(key = "drive") {
                    GoogleDriveSection(
                        isSignedIn = isSignedIn,
                        googleAccount = googleAccount,
                        driveBackups = driveBackups,
                        backupSettings = backupSettings,
                        driveOperation = driveOperation,
                        onSignIn = viewModel::requestSignIn,
                        onSignOut = viewModel::signOut,
                        onBackupNow = viewModel::backupToDrive,
                        onRestore = viewModel::startDriveRestore,
                        onDelete = viewModel::deleteDriveBackup,
                        onRefreshBackups = viewModel::loadDriveBackups,
                    )
                    Spacer(Modifier.height(24.dp))
                }
                item(key = "automatic") {
                    AutomaticBackupSection(
                        backupSettings = backupSettings,
                        onSetInterval = viewModel::setBackupInterval,
                        onSetWifiOnly = viewModel::setWifiOnly,
                    )
                    Spacer(Modifier.height(24.dp))
                }
                item(key = "local") {
                    LocalBackupSection(
                        uiState = uiState,
                        onToggleLocalImages = viewModel::toggleIncludeLocalImages,
                        onChooseAutoBackupFolder = viewModel::chooseLocalBackupFolder,
                        onExport = {
                            exportLauncher.launch("tempo_backup_${System.currentTimeMillis()}.zip")
                        },
                        onImport = {
                            importLauncher.launch(
                                arrayOf(
                                    "application/zip",
                                    "application/x-zip",
                                    "application/x-zip-compressed",
                                    "application/octet-stream",
                                )
                            )
                        },
                    )
                    Spacer(Modifier.height(24.dp))
                }
                item(key = "tips") {
                    BackupTipsCard()
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
    
    // Progress Dialog
    importExportProgress?.let { progress ->
        AlertDialog(
            onDismissRequest = { /* Cannot dismiss while in progress */ },
            containerColor = TempoSurfaceDialog,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    if (driveOperation == DriveOperationState.Restoring) "Restoring..."
                    else if (progress.phase.contains("Import", ignoreCase = true)) "Importing..."
                    else "Exporting...",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = progress.phase,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (progress.isIndeterminate || progress.total <= 0) {
                        TempoCircularLoadingIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                    } else {
                        LinearProgressIndicator(
                            progress = { (progress.current.toFloat() / progress.total.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = GlassFrostSoft
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${progress.current}/${progress.total}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = { }
        )
    }
    
    // Local Conflict Resolution Dialog
    conflictDialogUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelImport() },
            containerColor = TempoSurfaceDialog,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Import Options", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.settings_import_conflict_msg),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.backup_restore_duplicate_explanation),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.importData(uri, ImportConflictStrategy.REPLACE) }
                ) {
                    Text("Replace Existing", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { viewModel.cancelImport() }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(
                        onClick = { viewModel.importData(uri, ImportConflictStrategy.SKIP) }
                    ) {
                        Text("Skip Duplicates", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        )
    }
    
    // Drive Restore Confirmation Dialog
    driveRestoreDialog?.let { backup ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelDriveRestore() },
            containerColor = TempoSurfaceDialog,
            shape = RoundedCornerShape(24.dp),
            icon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Restore from Cloud", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    Text("Restore backup from ${formatDate(backup.createdAt)}?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Size: ${formatBytes(backup.sizeBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (backup.appVersion != null) {
                        Text(
                            text = "App version: ${backup.appVersion}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.backup_restore_duplicate_explanation),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.restoreFromDrive(backup, ImportConflictStrategy.REPLACE) }
                ) {
                    Text("Replace Existing", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { viewModel.cancelDriveRestore() }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(
                        onClick = { viewModel.restoreFromDrive(backup, ImportConflictStrategy.SKIP) }
                    ) {
                        Text("Skip Duplicates", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        )
    }
    
    // Drive Operation Progress Dialog
    when (val op = driveOperation) {
        is DriveOperationState.SigningIn,
        is DriveOperationState.Loading,
        is DriveOperationState.Restoring -> if (importExportProgress == null) {
            AlertDialog(
                onDismissRequest = { },
                containerColor = TempoSurfaceDialog,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Text(
                        when (op) {
                            DriveOperationState.SigningIn -> "Signing in..."
                            DriveOperationState.Loading -> "Loading..."
                            DriveOperationState.Restoring -> "Restoring..."
                            else -> ""
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                    }
                },
                confirmButton = { }
            )
        }
        is DriveOperationState.Uploading -> {
            AlertDialog(
                onDismissRequest = { },
                containerColor = TempoSurfaceDialog,
                shape = RoundedCornerShape(24.dp),
                title = { Text("Uploading to Google Drive...", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { op.progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = GlassFrostSoft
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${(op.progress * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                confirmButton = { }
            )
        }
        is DriveOperationState.Downloading -> {
            AlertDialog(
                onDismissRequest = { },
                containerColor = TempoSurfaceDialog,
                shape = RoundedCornerShape(24.dp),
                title = { Text("Downloading from Google Drive...", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { op.progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = GlassFrostSoft
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${(op.progress * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                confirmButton = { }
            )
        }
        else -> { }
    }
}

@Composable
private fun DataOverviewSection(uiState: BackupRestoreUiState) {
    SettingsSectionHeader(stringResource(R.string.backup_restore_data_header))
    
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        variant = GlassCardVariant.LowProminence
    ) {
        AnimatedVisibility(
            visible = !uiState.isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DataStatItem(Icons.Default.MusicNote, uiState.trackCount, stringResource(R.string.backup_restore_tracks_label))
                DataStatItem(Icons.Default.Person, uiState.artistCount, stringResource(R.string.backup_restore_artists_label))
                DataStatItem(Icons.Default.History, uiState.eventCount, stringResource(R.string.backup_restore_plays_label))
                DataStatItem(Icons.Default.Image, uiState.localImageCount, stringResource(R.string.backup_restore_images_label))
            }
        }
        
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            }
        }
    }
}

