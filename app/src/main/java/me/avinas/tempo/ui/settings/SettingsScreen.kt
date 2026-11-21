package me.avinas.tempo.ui.settings

import me.avinas.tempo.ui.effects.tempoAmbientBackdrop
import android.app.Activity
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.avinas.tempo.data.reset.AppResetState
import androidx.core.os.LocaleListCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import me.avinas.tempo.R
import me.avinas.tempo.service.SpotifyBroadcastPresence
import me.avinas.tempo.service.TrackingServiceHeartbeat
import me.avinas.tempo.data.importexport.ImportConflictStrategy
import me.avinas.tempo.data.importexport.ImportExportResult
import me.avinas.tempo.data.local.entities.AlbumArtSource
import me.avinas.tempo.data.drive.BackupStatus
import me.avinas.tempo.ui.components.TempoDialogBody
import me.avinas.tempo.ui.components.TempoDialogButtonRow
import me.avinas.tempo.ui.components.TempoDialogDangerButton
import me.avinas.tempo.ui.components.TempoDialogIcon
import me.avinas.tempo.ui.components.TempoDialogSecondaryButton
import me.avinas.tempo.ui.components.TempoDialogSurface
import me.avinas.tempo.ui.components.TempoDialogTextField
import me.avinas.tempo.ui.components.TempoDialogTitle
import me.avinas.tempo.ui.components.TempoIcons
import me.avinas.tempo.ui.components.TempoSnackbar
import me.avinas.tempo.ui.theme.*
import me.avinas.tempo.utils.BatteryUtils
import me.avinas.tempo.utils.OemBackgroundHelper
import me.avinas.tempo.utils.ReviewUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigate: (SettingsDestination) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val historyQuality by viewModel.historyQuality.collectAsStateWithLifecycle()
    val backupOverview by viewModel.backupOverview.collectAsStateWithLifecycle()
    var showHistoryAuditDialog by remember { mutableStateOf(false) }
    var historyBackupConfirmed by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val resources = LocalResources.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // Settings home is intentionally a small category navigator.
    var settingsSection by rememberSaveable { mutableStateOf<SettingsSection?>(null) }
    var settingsSearch by rememberSaveable { mutableStateOf("") }
    val settingsScrollState = key(settingsSection) { rememberLazyListState() }
    LaunchedEffect(settingsSearch) {
        // Search results and the unfiltered navigator both start at the top.
        settingsScrollState.scrollToItem(0)
    }
    val clearOrClose = {
        when {
            settingsSection != null -> settingsSection = null
            settingsSearch.isNotEmpty() -> settingsSearch = ""
            else -> onNavigateBack()
        }
    }
    BackHandler(enabled = settingsSection != null || settingsSearch.isNotEmpty(), onBack = clearOrClose)
    var notificationListenerGranted by remember { mutableStateOf(false) }
    var trackingHeartbeat by remember { mutableStateOf<TrackingServiceHeartbeat.Snapshot?>(null) }
    var trackingStatusCheckedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(settingsSection, lifecycleOwner) {
        if (settingsSection == SettingsSection.TRACKING) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    val (granted, heartbeat) = withContext(Dispatchers.IO) {
                        NotificationManagerCompat.getEnabledListenerPackages(context)
                            .contains(context.packageName) to TrackingServiceHeartbeat.snapshot(context)
                    }
                    notificationListenerGranted = granted
                    trackingHeartbeat = heartbeat
                    trackingStatusCheckedAt = System.currentTimeMillis()
                    delay(30_000L)
                }
            }
        }
    }
    var spotifyBroadcastEnabled by remember { mutableStateOf(SpotifyBroadcastPresence.enabled(context)) }
    var spotifyConnected by remember { mutableStateOf(false) }
    var spotifyClientId by remember { mutableStateOf("") }
    var spotifyAccountLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val (connected, id) = viewModel.spotifyAccountInfo()
            val newlyConnected = spotifyAccountLoaded && connected && !spotifyConnected
            if (!spotifyAccountLoaded || !connected) spotifyClientId = id
            spotifyConnected = connected
            spotifyAccountLoaded = true
            if (newlyConnected && uiState.spotifyApiOnlyMode) {
                me.avinas.tempo.worker.SpotifyPollingWorker.syncNow(context)
            }
        }
    }
    val versionName = me.avinas.tempo.BuildConfig.VERSION_NAME
    var showClearDataDialog by rememberSaveable { mutableStateOf(false) }
    val resetState by viewModel.resetState.collectAsStateWithLifecycle()
    val resetInProgress = resetState == AppResetState.Requesting || resetState == AppResetState.Requested
    val snackbarHostState = remember { SnackbarHostState() }

    // Import/Export states
    val importExportProgress by viewModel.importExportProgress.collectAsStateWithLifecycle()
    val importExportResult by viewModel.importExportResult.collectAsStateWithLifecycle()
    val conflictDialogUri by viewModel.showConflictDialog.collectAsStateWithLifecycle()
    val profileImageMessage by viewModel.profileImageMessage.collectAsStateWithLifecycle()
    val analyticsIdResetMessage = stringResource(R.string.settings_analytics_reset_id_done)

    // OEM detection for Background Protection
    val isXiaomiDevice = remember { OemBackgroundHelper.isXiaomiDevice() }
    var autostartState by remember { mutableStateOf(OemBackgroundHelper.AutostartState.UNKNOWN) }

    // Language selector state
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showCoverSourceDialog by remember { mutableStateOf(false) }
    val coverSourceOptions: List<Pair<AlbumArtSource?, String>> = listOf(
        null to stringResource(R.string.settings_cover_source_automatic),
        AlbumArtSource.MUSICBRAINZ to "MusicBrainz",
        AlbumArtSource.ITUNES to "Apple Music / iTunes",
        AlbumArtSource.DEEZER to "Deezer",
    )
    val selectedCoverSourceName =
        coverSourceOptions.firstOrNull { it.first == uiState.preferredCoverSource }?.second
            ?: stringResource(R.string.settings_cover_source_automatic)
    val currentLocale = AppCompatDelegate.getApplicationLocales().get(0)?.language ?: "en"
    val languages =
        listOf(
            "en" to stringResource(R.string.settings_language_english),
            "fr" to stringResource(R.string.settings_language_french),
            "de" to stringResource(R.string.settings_language_german),
            "hu" to stringResource(R.string.settings_language_hungarian),
            "pt" to stringResource(R.string.settings_language_portuguese),
            "ru" to stringResource(R.string.settings_language_russian),
        )
    val currentLanguageSubtitle =
        languages.firstOrNull { it.first == currentLocale }?.second
            ?: stringResource(R.string.settings_language_english)

    // These readings are only used by the tracking and connections categories.
    var batteryLevel by remember { mutableIntStateOf(100) }
    val isBatteryCritical = batteryLevel <= BatteryUtils.CRITICAL_BATTERY_LEVEL
    val isLowBattery = batteryLevel <= BatteryUtils.LOW_BATTERY_LEVEL
    LaunchedEffect(settingsSection, lifecycleOwner) {
        if (settingsSection == SettingsSection.TRACKING || settingsSection == SettingsSection.CONNECTIONS) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                if (settingsSection == SettingsSection.TRACKING && isXiaomiDevice) {
                    launch {
                        autostartState = withContext(Dispatchers.IO) {
                            OemBackgroundHelper.getAutostartState(context)
                        }
                    }
                }
                while (true) {
                    batteryLevel = withContext(Dispatchers.IO) { BatteryUtils.getBatteryLevel(context) }
                    delay(30_000L)
                }
            }
        }
    }

    // File picker for import (ZIP)
    val importLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri: Uri? ->
            uri?.let {
                viewModel.startImport(it)
            }
        }

    val profileImageLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            uri?.let(viewModel::updateProfileImage)
        }

    // File creator for export (ZIP)
    val exportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/zip"),
        ) { uri: Uri? ->
            uri?.let {
                viewModel.exportData(it)
            }
        }

    // Show result snackbar
    LaunchedEffect(importExportResult) {
        importExportResult?.let { result ->
            when (result) {
                is ImportExportResult.Success -> {
                    snackbarHostState.showSnackbar(
                        resources.getString(R.string.settings_import_success, result.totalRecords, result.imagesCount),
                    )
                }

                is ImportExportResult.Error -> {
                    snackbarHostState.showSnackbar(
                        result.message,
                    )
                }
            }
            viewModel.clearImportExportResult()
        }
    }

    LaunchedEffect(profileImageMessage) {
        profileImageMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearProfileImageMessage()
        }
    }

    // Resetting the analytics id is otherwise invisible — nothing on screen changes — so it
    // needs an acknowledgement or it reads as a tap that did nothing.
    LaunchedEffect(uiState.analyticsIdResetAt) {
        if (uiState.analyticsIdResetAt != null) {
            snackbarHostState.showSnackbar(analyticsIdResetMessage)
            viewModel.clearAnalyticsIdReset()
        }
    }

    LaunchedEffect(resetState) {
        if (resetState == AppResetState.Failed) {
            showClearDataDialog = false
            snackbarHostState.showSnackbar(resources.getString(R.string.settings_clear_data_failed))
        }
    }

    // Name Dialog State
    var showNameDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        text = settingsSection?.let { stringResource(it.titleRes) }
                            ?: stringResource(R.string.settings_title),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = clearOrClose) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) { TempoSnackbar(it) } },
    ) { padding ->
        Box(Modifier.fillMaxSize().tempoAmbientBackdrop()) {
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (settingsSection == null) {
                    SettingsOverview(
                        query = settingsSearch,
                        onQueryChange = { settingsSearch = it },
                        listState = settingsScrollState,
                        onSectionSelected = { settingsSection = it },
                        onNavigate = onNavigate,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    when (settingsSection) {
                        SettingsSection.GENERAL -> GeneralSettingsSection(
                            state = settingsScrollState,
                            uiState = uiState,
                            viewModel = viewModel,
                            currentLanguageSubtitle = currentLanguageSubtitle,
                            onEditName = {
                                tempName = uiState.userName
                                showNameDialog = true
                            },
                            onPickProfileImage = { profileImageLauncher.launch("image/*") },
                            onSelectLanguage = { showLanguageDialog = true },
                            modifier = Modifier.weight(1f),
                        )
                        SettingsSection.TRACKING -> TrackingSettingsSection(
                            state = settingsScrollState,
                            uiState = uiState,
                            viewModel = viewModel,
                            notificationListenerGranted = notificationListenerGranted,
                            trackingHeartbeat = trackingHeartbeat,
                            trackingStatusCheckedAt = trackingStatusCheckedAt,
                            batteryLevel = batteryLevel,
                            isXiaomiDevice = isXiaomiDevice,
                            autostartState = autostartState,
                            onNavigate = onNavigate,
                            modifier = Modifier.weight(1f),
                        )
                        SettingsSection.CONNECTIONS -> ConnectionsSettingsSection(
                            state = settingsScrollState,
                            uiState = uiState,
                            viewModel = viewModel,
                            spotifyConnected = spotifyConnected,
                            spotifyAccountLoaded = spotifyAccountLoaded,
                            spotifyClientId = spotifyClientId,
                            onSpotifyClientIdChange = { spotifyClientId = it },
                            onSpotifyConnectedChange = { spotifyConnected = it },
                            spotifyBroadcastEnabled = spotifyBroadcastEnabled,
                            onSpotifyBroadcastEnabledChange = { spotifyBroadcastEnabled = it },
                            batteryLevel = batteryLevel,
                            isBatteryCritical = isBatteryCritical,
                            isLowBattery = isLowBattery,
                            onNavigate = onNavigate,
                            modifier = Modifier.weight(1f),
                        )
                        SettingsSection.DATA -> DataSettingsSection(
                            state = settingsScrollState,
                            uiState = uiState,
                            viewModel = viewModel,
                            backupOverview = backupOverview,
                            selectedCoverSourceName = selectedCoverSourceName,
                            onNavigate = onNavigate,
                            onOpenHistoryAudit = {
                                historyBackupConfirmed = false
                                showHistoryAuditDialog = true
                                viewModel.scanHistoryQuality()
                            },
                            onSelectCoverSource = { showCoverSourceDialog = true },
                            onClearData = { showClearDataDialog = true },
                            modifier = Modifier.weight(1f),
                        )
                        SettingsSection.ABOUT -> AboutSettingsSection(
                            state = settingsScrollState,
                            versionName = versionName,
                            onNavigate = onNavigate,
                            modifier = Modifier.weight(1f),
                        )
                        null -> Unit
                    }
                }
            }
        }
    }

    // Keep recovery/repair dialogs mounted independent of lazy list visibility.
    if (showHistoryAuditDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryAuditDialog = false },
            title = { Text("Listening-history accuracy") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (historyQuality.isLoading) {
                        CircularProgressIndicator()
                        Text("Checking stored listening events…")
                    } else if (historyQuality.error != null) {
                        Text(historyQuality.error ?: "An unknown error occurred")
                    } else {
                        Text(
                            "Old Spotify API estimates: ${historyQuality.apiEstimatedCount} plays. " +
                                "Reconstructed history: ${historyQuality.reconstructedCount} plays."
                        )
                        Text(
                            "The Spotify recently-played API never supplied listening seconds. " +
                                "The optional repair preserves play records and removes only invented " +
                                "listening durations and completion for this exact API source."
                        )
                        Text(
                            "Exact duplicate records: ${historyQuality.exactDuplicateCount}. " +
                                "Only identical, fingerprinted events are eligible for safe removal; " +
                                "uncertain overlaps and older unmarked plays are not deleted."
                        )
                        historyQuality.repairedDuplicatesCount?.let { removed ->
                            Text("Removed $removed exact duplicate records. Listening statistics will refresh.")
                        }
                        if (historyQuality.reconstructedCount > 0) {
                            Text(
                                "Reconstructed plays are flagged for review, not automatically deleted."
                            )
                        }
                        historyQuality.repairedCount?.let { repaired ->
                            Text("Corrected $repaired API records. Reopen statistics to see refreshed totals.")
                        }
                        if ((historyQuality.apiEstimatedCount > 0 &&
                                historyQuality.repairedCount == null) ||
                            (historyQuality.exactDuplicateCount > 0 &&
                                historyQuality.repairedDuplicatesCount == null)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = historyBackupConfirmed,
                                    onCheckedChange = { historyBackupConfirmed = it },
                                )
                                Text(
                                    "I have exported a backup. I authorize correcting unverified " +
                                        "API durations or removing exact duplicate records only when I select an action."
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    if (historyQuality.apiEstimatedCount > 0 &&
                        historyQuality.repairedCount == null &&
                        historyQuality.error == null
                    ) {
                        TextButton(
                            enabled = historyBackupConfirmed && !historyQuality.isLoading,
                            onClick = {
                                historyBackupConfirmed = false
                                viewModel.repairHistoryAfterBackupConfirmation()
                            },
                        ) { Text("Correct API estimates") }
                    }
                    if (historyQuality.exactDuplicateCount > 0 &&
                        historyQuality.repairedDuplicatesCount == null &&
                        historyQuality.error == null
                    ) {
                        TextButton(
                            enabled = historyBackupConfirmed && !historyQuality.isLoading,
                            onClick = {
                                historyBackupConfirmed = false
                                viewModel.repairExactDuplicatesAfterBackupConfirmation()
                            },
                        ) { Text("Remove exact duplicates") }
                    }
                    if (historyQuality.apiEstimatedCount == 0 &&
                        historyQuality.exactDuplicateCount == 0
                    ) {
                        TextButton(onClick = { showHistoryAuditDialog = false }) {
                            Text("Close")
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showHistoryAuditDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Name Edit Dialog
    if (showNameDialog) {
        Dialog(onDismissRequest = { showNameDialog = false }) {
            TempoDialogSurface {
                TempoDialogIcon(
                    icon = TempoIcons.Edit,
                    tint = MaterialTheme.colorScheme.primary,
                    size = 48,
                )
                Spacer(modifier = Modifier.height(16.dp))
                TempoDialogTitle(text = stringResource(R.string.settings_update_name))
                Spacer(modifier = Modifier.height(6.dp))
                TempoDialogBody(text = stringResource(R.string.settings_name_dialog_body))
                Spacer(modifier = Modifier.height(20.dp))
                TempoDialogTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = stringResource(R.string.settings_display_name),
                )
                Spacer(modifier = Modifier.height(24.dp))
                TempoDialogButtonRow(
                    primaryText = stringResource(R.string.settings_save),
                    onPrimary = {
                        if (tempName.isNotBlank()) {
                            viewModel.updateUserName(tempName.trim())
                            showNameDialog = false
                        }
                    },
                    secondaryText = stringResource(R.string.settings_cancel),
                    onSecondary = { showNameDialog = false },
                    primaryEnabled = tempName.isNotBlank(),
                )
            }
        }
    }

    // Language Selector Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            containerColor = TempoSurfaceDialog,
            shape = RoundedCornerShape(24.dp),
            title = { Text(stringResource(R.string.settings_select_language), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    languages.forEach { (langTag, langName) ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        val localeList = LocaleListCompat.forLanguageTags(langTag)
                                        AppCompatDelegate.setApplicationLocales(localeList)
                                        showLanguageDialog = false
                                        (context as? Activity)?.recreate()
                                    }.padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = currentLocale == langTag,
                                onClick = {
                                    val localeList = LocaleListCompat.forLanguageTags(langTag)
                                    AppCompatDelegate.setApplicationLocales(localeList)
                                    showLanguageDialog = false
                                    (context as? Activity)?.recreate()
                                },
                                colors =
                                    RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                    ),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = langName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.settings_cancel), color = TextTertiary)
                }
            },
        )
    }

    // Preferred album artwork provider
    if (showCoverSourceDialog) {
        AlertDialog(
            onDismissRequest = { showCoverSourceDialog = false },
            containerColor = TempoSurfaceDialog,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    stringResource(R.string.settings_cover_source_title),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = {
                Column {
                    Text(
                        stringResource(R.string.settings_cover_source_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    coverSourceOptions.forEach { (source, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .selectable(
                                    selected = source == uiState.preferredCoverSource,
                                    onClick = {
                                        viewModel.setPreferredCoverSource(source)
                                        showCoverSourceDialog = false
                                    },
                                    role = Role.RadioButton,
                                )
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = source == uiState.preferredCoverSource,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCoverSourceDialog = false }) {
                    Text(stringResource(R.string.settings_cancel), color = TextTertiary)
                }
            },
        )
    }

    // Progress Dialog
    importExportProgress?.let { progress ->
        AlertDialog(
            onDismissRequest = { /* Cannot dismiss while in progress */ },
            containerColor = TempoSurfaceDialog,
            shape = RoundedCornerShape(24.dp),
            title = { Text(stringResource(R.string.settings_processing), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = progress.phase,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (progress.isIndeterminate) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                    } else {
                        LinearProgressIndicator(
                            progress = { progress.percentage.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = GlassFrostSoft,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${progress.current}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary,
                        )
                    }
                }
            },
            confirmButton = { },
        )
    }

    // Conflict Resolution Dialog
    conflictDialogUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelImport() },
            containerColor = TempoSurfaceDialog,
            shape = RoundedCornerShape(24.dp),
            title = { Text(stringResource(R.string.settings_import_options), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold) },
            text = {
                Text(stringResource(R.string.settings_import_conflict_msg), color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.importData(uri, ImportConflictStrategy.REPLACE) },
                ) {
                    Text(stringResource(R.string.settings_replace_existing), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { viewModel.cancelImport() }) {
                        Text(stringResource(R.string.settings_cancel), color = TextTertiary)
                    }
                    TextButton(
                        onClick = { viewModel.importData(uri, ImportConflictStrategy.SKIP) },
                    ) {
                        Text(stringResource(R.string.settings_skip_duplicates), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
        )
    }

    if (showClearDataDialog) {
        Dialog(
            onDismissRequest = { if (!resetInProgress) showClearDataDialog = false },
            properties = DialogProperties(
                dismissOnBackPress = !resetInProgress,
                dismissOnClickOutside = !resetInProgress
            )
        ) {
            TempoDialogSurface {
                TempoDialogIcon(
                    icon = TempoIcons.Trash,
                    tint = TempoError,
                    size = 48,
                )
                Spacer(modifier = Modifier.height(16.dp))
                TempoDialogTitle(text = stringResource(R.string.settings_clear_data_title))
                Spacer(modifier = Modifier.height(8.dp))
                TempoDialogBody(text = stringResource(R.string.settings_clear_data_msg))
                Spacer(modifier = Modifier.height(24.dp))
                TempoDialogDangerButton(
                    text = stringResource(
                        if (resetInProgress) R.string.settings_clear_data_requesting else R.string.settings_clear_everything
                    ),
                    enabled = !resetInProgress,
                    onClick = { viewModel.clearAllData() },
                    icon = TempoIcons.Trash,
                )
                Spacer(modifier = Modifier.height(6.dp))
                TempoDialogSecondaryButton(
                    text = stringResource(R.string.settings_cancel),
                    enabled = !resetInProgress,
                    onClick = { showClearDataDialog = false },
                )
            }
        }
    }
}
