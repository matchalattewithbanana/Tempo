package me.avinas.tempo.ui.settings

import me.avinas.tempo.ui.effects.tempoAmbientBackdrop
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import me.avinas.tempo.data.reset.AppResetState
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
import me.avinas.tempo.data.importexport.ImportExportResult
import me.avinas.tempo.data.local.entities.AlbumArtSource
import me.avinas.tempo.ui.components.TempoSnackbar
import me.avinas.tempo.ui.theme.*
import me.avinas.tempo.utils.BatteryUtils
import me.avinas.tempo.utils.OemBackgroundHelper

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
    var activeDialog by rememberSaveable { mutableStateOf<SettingsDialog?>(null) }
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

    val profileImageLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            uri?.let(viewModel::updateProfileImage)
        }

    // Show result snackbar
    LaunchedEffect(importExportResult) {
        importExportResult?.let { result ->
            when (result) {
                is ImportExportResult.Success -> {
                    val successMessage =
                        buildString {
                            append(resources.getString(R.string.settings_import_success, result.totalRecords, result.imagesCount))
                            if (result.warnings.isNotEmpty()) {
                                append("\n")
                                append(result.warnings.joinToString(separator = "\n"))
                            }
                        }
                    snackbarHostState.showSnackbar(successMessage)
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
            if (activeDialog == SettingsDialog.CLEAR_DATA) activeDialog = null
            snackbarHostState.showSnackbar(resources.getString(R.string.settings_clear_data_failed))
        }
    }

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
                            onEditName = { activeDialog = SettingsDialog.EDIT_NAME },
                            onPickProfileImage = { profileImageLauncher.launch("image/*") },
                            onSelectLanguage = { activeDialog = SettingsDialog.LANGUAGE },
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
                                activeDialog = SettingsDialog.HISTORY_AUDIT
                                viewModel.scanHistoryQuality()
                            },
                            onSelectCoverSource = { activeDialog = SettingsDialog.COVER_SOURCE },
                            onClearData = { activeDialog = SettingsDialog.CLEAR_DATA },
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

    SettingsDialogs(
        activeDialog = activeDialog,
        onDismissLocalDialog = { activeDialog = null },
        uiState = uiState,
        historyQuality = historyQuality,
        languages = languages,
        currentLocale = currentLocale,
        coverSourceOptions = coverSourceOptions,
        importExportProgress = importExportProgress,
        conflictDialogUri = conflictDialogUri,
        resetInProgress = resetInProgress,
        viewModel = viewModel,
    )

}
