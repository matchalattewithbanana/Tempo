@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.avinas.tempo.ui.onboarding

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import me.avinas.tempo.R
import me.avinas.tempo.data.drive.DriveBackupInfo
import me.avinas.tempo.data.drive.DriveRestoreResult
import me.avinas.tempo.data.importexport.ImportConflictStrategy
import me.avinas.tempo.data.importexport.ImportExportResult
import me.avinas.tempo.data.lastfm.LastFmImportService
import me.avinas.tempo.data.spotify.SpotifyJsonImportService
import me.avinas.tempo.data.youtube.YouTubeMusicImportService
import me.avinas.tempo.ui.components.GlassCard
import me.avinas.tempo.ui.components.GlassCardVariant
import me.avinas.tempo.ui.components.TempoSnackbar
import me.avinas.tempo.ui.lastfm.LastFmUiState
import me.avinas.tempo.ui.lastfm.LastFmViewModel
import me.avinas.tempo.ui.onboarding.restore.ExpandableRestoreCard
import me.avinas.tempo.ui.onboarding.restore.RestoreHeader
import me.avinas.tempo.ui.onboarding.restore.RestoreSectionTitle
import me.avinas.tempo.ui.onboarding.restore.RestoreTopBar
import me.avinas.tempo.ui.settings.BackupRestoreViewModel
import me.avinas.tempo.ui.settings.DriveOperationState
import me.avinas.tempo.ui.spotify.SpotifyJsonImportUiState
import me.avinas.tempo.ui.spotify.SpotifyJsonImportViewModel
import me.avinas.tempo.ui.theme.*
import me.avinas.tempo.ui.theme.TempoDarkBackground
import me.avinas.tempo.ui.utils.adaptiveSizeByCategory
import me.avinas.tempo.ui.utils.adaptiveTextUnitByCategory
import me.avinas.tempo.ui.utils.rememberClampedHeightPercentage
import me.avinas.tempo.ui.utils.rememberScreenHeightPercentage
import me.avinas.tempo.ui.utils.scaledSize
import me.avinas.tempo.ui.youtube.TakeoutGuideDialog
import me.avinas.tempo.ui.youtube.YouTubeMusicImportUiState
import me.avinas.tempo.ui.youtube.YouTubeMusicImportViewModel
import me.avinas.tempo.utils.FormatUtils.formatBytes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreScreen(
    onFinish: () -> Unit,
    onBack: () -> Unit,
    viewModel: BackupRestoreViewModel = hiltViewModel(),
    lastFmViewModel: LastFmViewModel = hiltViewModel(),
    spotifyJsonImportViewModel: SpotifyJsonImportViewModel = hiltViewModel(),
    youTubeMusicImportViewModel: YouTubeMusicImportViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Track if we've shown the activity error to avoid spamming
    var activityErrorShown by remember { mutableStateOf(false) }

    // Guard against onFinish being called multiple times from different LaunchedEffect blocks.
    // Multiple paths (import success, Spotify success, Last.fm success, Start Fresh button) can
    // call onFinish concurrently; calling it more than once causes NavController to try to
    // transition a back-stack entry that no longer exists → IllegalStateException.
    var hasFinished by remember { mutableStateOf(false) }
    val safeOnFinish: () -> Unit = {
        if (!hasFinished) {
            hasFinished = true
            onFinish()
        }
    }

    // Single-expanded accordion — only one restore option open at a time,
    // keeps the step to ~1.5 screens of scroll instead of 4+.
    var expandedId by remember { mutableStateOf<String?>(null) }

    fun toggleExpanded(id: String) {
        expandedId = if (expandedId == id) null else id
    }

    // Handle system back press - block during active operations
    val driveOperation by viewModel.driveOperation.collectAsState()
    val importExportProgress by viewModel.importExportProgress.collectAsState()

    // Last.fm State
    val lastFmUiState by lastFmViewModel.uiState.collectAsState()
    val lastFmImportProgress by lastFmViewModel.importProgress.collectAsState()

    // Spotify JSON Import State
    val spotifyJsonImportUiState by spotifyJsonImportViewModel.uiState.collectAsState()
    val spotifyJsonImportState by spotifyJsonImportViewModel.importState.collectAsState()

    // YouTube Music Import State
    val youTubeMusicImportUiState by youTubeMusicImportViewModel.uiState.collectAsState()
    val youTubeMusicImportState by youTubeMusicImportViewModel.importState.collectAsState()

    val isOperationActive =
        driveOperation is DriveOperationState.Downloading ||
            driveOperation is DriveOperationState.Restoring ||
            driveOperation is DriveOperationState.Uploading ||
            importExportProgress != null ||
            lastFmUiState.isImporting ||
            lastFmUiState.isLoading ||
            spotifyJsonImportUiState is SpotifyJsonImportUiState.Importing ||
            youTubeMusicImportUiState is YouTubeMusicImportUiState.Importing

    // Keep ownership of system Back while the toolbar actions are disabled.
    androidx.activity.compose.BackHandler {
        if (!isOperationActive) onBack()
    }

    val isSignedIn by viewModel.isSignedIn.collectAsState()
    val driveBackups by viewModel.driveBackups.collectAsState()
    val importExportResult by viewModel.importExportResult.collectAsState()

    // Dialog States
    val conflictDialogUri by viewModel.showConflictDialog.collectAsState()
    val driveRestoreDialog by viewModel.showDriveRestoreDialog.collectAsState()

    // Error Handling State
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle Sign-In/Restore/Consent callbacks
    val signInRequested by viewModel.signInRequested.collectAsState()
    val sessionRestoreRequested by viewModel.sessionRestoreRequested.collectAsState()
    val consentRequested by viewModel.consentRequested.collectAsState()

    // Consent Flow Launcher
    val consentLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult(),
        ) { result ->
            viewModel.onConsentComplete(result.resultCode == Activity.RESULT_OK)
        }

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

    // Success Handling - Finish onboarding on successful restore
    LaunchedEffect(importExportResult) {
        when (val result = importExportResult) {
            is ImportExportResult.Success -> {
                safeOnFinish() // Auto-finish on success
                viewModel.clearImportExportResult()
            }

            is ImportExportResult.Error -> {
                snackbarHostState.showSnackbar("Restore failed: ${result.message}")
                viewModel.clearImportExportResult()
            }

            else -> {}
        }
    }

    LaunchedEffect(driveOperation) {
        when (val op = driveOperation) {
            is DriveOperationState.Success -> {
                viewModel.clearDriveOperation(op)
            }

            is DriveOperationState.Error -> {
                snackbarHostState.showSnackbar("Drive error: ${op.message}")
                viewModel.clearDriveOperation(op)
            }

            else -> {}
        }
    }

    // Local Backup Picker
    val importLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri: Uri? ->
            uri?.let { viewModel.startImport(it) }
        }

    // ponytail: no Scaffold — its content padding double-applies the system-bar
    // insets under edge-to-edge (every other onboarding screen skips Scaffold).
    me.avinas.tempo.ui.components.DeepOceanBackground(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Fixed top bar — never scrolls away
            RestoreTopBar(
                onBack = onBack,
                onSkip = safeOnFinish,
                skipEnabled = !isOperationActive,
                backEnabled = !isOperationActive,
            )

            // Scrollable content
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                RestoreHeader(
                    title = "Bring your history",
                    subtitle = "Import from another service or restore a Tempo backup.",
                )

                Spacer(modifier = Modifier.height(rememberScreenHeightPercentage(0.03f)))

                RestoreSectionTitle(text = "Import from services")

                // Last.fm Import Card
                LastFmImportCard(
                    uiState = lastFmUiState,
                    importProgress = lastFmImportProgress,
                    onUsernameSubmit = lastFmViewModel::discoverUser,
                    onSelectTier = lastFmViewModel::startImportDirect,
                    onCancel = lastFmViewModel::reset,
                    onCancelImport = lastFmViewModel::cancelImport,
                    onClearError = lastFmViewModel::clearError,
                    onFinishImport = {
                        lastFmViewModel.reset()
                        safeOnFinish()
                    },
                    expanded = expandedId == "lastfm",
                    onExpandedChange = { open -> expandedId = if (open) "lastfm" else null },
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Spotify JSON Import Card
                SpotifyJsonImportCard(
                    uiState = spotifyJsonImportUiState,
                    importState = spotifyJsonImportState,
                    onImport = { uris ->
                        spotifyJsonImportViewModel.importFiles(context, uris)
                    },
                    onReset = {
                        spotifyJsonImportViewModel.resetState()
                    },
                    onFinish = {
                        spotifyJsonImportViewModel.resetState()
                        safeOnFinish()
                    },
                    expanded = expandedId == "spotify",
                    onExpandedChange = { open -> expandedId = if (open) "spotify" else null },
                )

                Spacer(modifier = Modifier.height(12.dp))

                // YouTube Music Import Card
                YouTubeMusicImportCard(
                    uiState = youTubeMusicImportUiState,
                    importState = youTubeMusicImportState,
                    onImport = { uris ->
                        youTubeMusicImportViewModel.importFiles(context, uris)
                    },
                    onReset = {
                        youTubeMusicImportViewModel.resetState()
                    },
                    onFinish = {
                        youTubeMusicImportViewModel.resetState()
                        safeOnFinish()
                    },
                    expanded = expandedId == "youtube",
                    onExpandedChange = { open -> expandedId = if (open) "youtube" else null },
                )

                Spacer(modifier = Modifier.height(24.dp))

                RestoreSectionTitle(text = "Restore backup")

                // Option 1: Google Drive (same header pattern as import cards)
                ExpandableRestoreCard(
                    title = "Google Drive",
                    subtitle =
                        if (isSignedIn && driveBackups.isNotEmpty()) {
                            "${driveBackups.size} backup${if (driveBackups.size == 1) "" else "s"} found"
                        } else {
                            "Restore from the cloud"
                        },
                    accent = Color(0xFF4285F4),
                    icon = Icons.Default.Cloud,
                    expanded = expandedId == "drive",
                    onToggle = { toggleExpanded("drive") },
                    interactionEnabled = !isOperationActive,
                ) {
                    if (!isSignedIn) {
                        Text(
                            text = "Sign in to list backups from your Google Drive.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.requestSignIn() },
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("Connect Google Account", fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        if (driveOperation is DriveOperationState.Loading || driveOperation is DriveOperationState.SigningIn) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        } else if (driveBackups.isNotEmpty()) {
                            Column {
                                driveBackups.forEachIndexed { index, backup ->
                                    Row(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .clickable { viewModel.startDriveRestore(backup) }
                                                .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text =
                                                    SimpleDateFormat(
                                                        "MMM dd, yyyy",
                                                        Locale.getDefault(),
                                                    ).format(Date(backup.createdAt)),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontSize = adaptiveTextUnitByCategory(16.sp, 15.sp, 14.sp),
                                            )
                                            Text(
                                                text = formatBytes(backup.sizeBytes) + (backup.deviceName?.let { " • $it" } ?: ""),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = adaptiveTextUnitByCategory(14.sp, 13.sp, 12.sp),
                                            )
                                        }
                                        Icon(
                                            Icons.Default.CloudDownload,
                                            contentDescription = "Restore",
                                            tint = TempoPrimary,
                                        )
                                    }
                                    if (index < driveBackups.lastIndex) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "No backups found on Drive.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Option 2: Local Backup (same header pattern, explicit button)
                ExpandableRestoreCard(
                    title = "Restore from File",
                    subtitle = "Select a .zip backup file",
                    accent = MaterialTheme.colorScheme.primary,
                    icon = Icons.Default.FolderOpen,
                    expanded = expandedId == "local",
                    onToggle = { toggleExpanded("local") },
                    interactionEnabled = !isOperationActive,
                ) {
                    Text(
                        text = "Pick a .zip backup file previously exported from Tempo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            importLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select backup file", fontWeight = FontWeight.SemiBold)
                    }
                }

                // Slim dismissible note — imports/restore stay available in Settings.
                var showLaterNote by remember { mutableStateOf(true) }
                if (showLaterNote) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        variant = GlassCardVariant.LowProminence,
                        contentPadding = PaddingValues(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "You can do this later from Settings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = { showLaterNote = false },
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.height(rememberScreenHeightPercentage(0.04f)))
            }

            // Pinned CTA — same rect as every other step.
            OnboardingFooter(
                text = "Start Tracking",
                onClick = safeOnFinish,
                enabled = !isOperationActive,
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp),
            snackbar = { TempoSnackbar(it) },
        )
    }

    // Dialogs

    // Progress Dialog
    importExportProgress?.let { progress ->
        AlertDialog(
            onDismissRequest = { },
            containerColor = TempoSurfaceDialog,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    if (progress.phase.contains("Import")) "Restoring..." else "Processing...",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(progress.phase, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (progress.isIndeterminate || progress.total <= 0) {
                        CircularProgressIndicator(color = TempoPrimary, strokeWidth = 2.dp)
                    } else {
                        LinearProgressIndicator(
                            progress = { (progress.current.toFloat() / progress.total.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                            color = TempoPrimary,
                            trackColor = GlassFrostSoft,
                        )
                    }
                }
            },
            confirmButton = {},
        )
    }

    // Drive Downloading Dialog
    if (driveOperation is DriveOperationState.Downloading ||
        (driveOperation is DriveOperationState.Restoring && importExportProgress == null)
    ) {
        AlertDialog(
            onDismissRequest = { },
            containerColor = TempoSurfaceDialog,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Restoring from Cloud...", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val downloadingState = driveOperation as? DriveOperationState.Downloading
                    if (downloadingState != null) {
                        val currentProgress = downloadingState.progress
                        LinearProgressIndicator(
                            progress = { currentProgress },
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                            color = TempoPrimary,
                            trackColor = GlassFrostSoft,
                        )
                    } else {
                        CircularProgressIndicator(color = TempoPrimary, strokeWidth = 2.dp)
                    }
                }
            },
            confirmButton = {},
        )
    }

    // Local Conflict Resolution
    conflictDialogUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelImport() },
            containerColor = TempoSurfaceDialog,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Restore Options", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            text = { Text("How should we handle data conflicts?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { viewModel.importData(uri, ImportConflictStrategy.REPLACE) }) {
                    Text("Replace Everything", color = TempoPrimary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { viewModel.cancelImport() }) {
                        Text("Cancel", color = TextTertiary)
                    }
                    TextButton(onClick = { viewModel.importData(uri, ImportConflictStrategy.SKIP) }) {
                        Text("Skip Duplicates", color = TextSecondary)
                    }
                }
            },
        )
    }

    // Drive Restore Confirmation
    driveRestoreDialog?.let { backup ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelDriveRestore() },
            containerColor = TempoSurfaceDialog,
            shape = RoundedCornerShape(24.dp),
            icon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = TempoPrimary) },
            title = { Text("Restore this backup?", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    Text(
                        "Date: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(backup.createdAt))}",
                        color = TextSecondary,
                    )
                    Text("Size: ${formatBytes(backup.sizeBytes)}", color = TextTertiary)
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.restoreFromDrive(backup, ImportConflictStrategy.REPLACE) }) {
                    Text("Restore", color = TempoPrimary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDriveRestore() }) {
                    Text("Cancel", color = TextTertiary)
                }
            },
        )
    }
}

/**
 * Last.fm Import Card for onboarding
 * Compact card that handles the full Last.fm import flow inline
 */
