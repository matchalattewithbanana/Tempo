package me.avinas.tempo.ui.settings

import me.avinas.tempo.ui.effects.tempoAmbientBackdrop
import me.avinas.tempo.ui.effects.tempoTiltSurface
import me.avinas.tempo.ui.effects.tempoListMotion
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Info
import me.avinas.tempo.ui.components.TempoEmblemStyle
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
import me.avinas.tempo.ui.components.LiveNowPlayingCard
import me.avinas.tempo.data.importexport.ImportConflictStrategy
import me.avinas.tempo.data.importexport.ImportExportResult
import me.avinas.tempo.data.local.entities.AlbumArtSource
import me.avinas.tempo.data.drive.BackupStatus
import me.avinas.tempo.ui.components.GlassCard
import me.avinas.tempo.ui.components.SettingsOption
import me.avinas.tempo.ui.components.SettingsSectionHeader
import me.avinas.tempo.ui.components.SettingsSwitch
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
    onNavigateToBackup: (() -> Unit)? = null,
    onNavigateToSupportedApps: (() -> Unit)? = null,
    onNavigateToBackgroundProtection: (() -> Unit)? = null,
    onNavigateToLastFmImport: (() -> Unit)? = null,
    onNavigateToSpotifyJsonImport: (() -> Unit)? = null,
    onNavigateToDeezerImport: (() -> Unit)? = null,
    onNavigateToAppleMusicImport: (() -> Unit)? = null,
    onNavigateToYouTubeMusicImport: (() -> Unit)? = null,
    onNavigateToMusicoletImport: (() -> Unit)? = null,
    onNavigateToExtensionImport: (() -> Unit)? = null,
    onNavigateToDesktop: () -> Unit = {},
    onNavigateToEnrichmentReport: (() -> Unit)? = null,
    onNavigateToYourData: (() -> Unit)? = null,
    onNavigateToAppearance: (() -> Unit)? = null,
    onNavigateToLogoPicker: (() -> Unit)? = null,
    onNavigateToUpdates: (() -> Unit)? = null,
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
    var settingsCategory by rememberSaveable { mutableStateOf("overview") }
    var settingsSearch by rememberSaveable { mutableStateOf("") }
    val settingsScrollState = key(settingsCategory) { rememberScrollState() }
    LaunchedEffect(settingsSearch) {
        // Search results and the unfiltered navigator both start at the top.
        settingsScrollState.scrollTo(0)
    }
    val clearOrClose = {
        when {
            settingsCategory != "overview" -> settingsCategory = "overview"
            settingsSearch.isNotEmpty() -> settingsSearch = ""
            else -> onNavigateBack()
        }
    }
    BackHandler(enabled = settingsCategory != "overview" || settingsSearch.isNotEmpty(), onBack = clearOrClose)
    val scope = rememberCoroutineScope()
    var notificationListenerGranted by remember { mutableStateOf(false) }
    var trackingHeartbeat by remember { mutableStateOf<TrackingServiceHeartbeat.Snapshot?>(null) }
    var trackingStatusCheckedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(settingsCategory, lifecycleOwner) {
        if (settingsCategory == "overview") {
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
    LaunchedEffect(settingsCategory, lifecycleOwner) {
        if (settingsCategory == "tracking" || settingsCategory == "connections") {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                if (settingsCategory == "tracking" && isXiaomiDevice) {
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
                        text = when (settingsCategory) {
                            "general" -> stringResource(R.string.settings_category_general)
                            "tracking" -> stringResource(R.string.settings_category_tracking)
                            "connections" -> stringResource(R.string.settings_category_connections)
                            "data" -> stringResource(R.string.settings_category_data)
                            "about" -> stringResource(R.string.settings_category_about)
                            else -> stringResource(R.string.settings_title)
                        },
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
                if (settingsCategory == "overview") {
                    OutlinedTextField(
                        value = settingsSearch,
                        onValueChange = { settingsSearch = it },
                        label = { Text(stringResource(R.string.settings_search_label)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = if (settingsSearch.isBlank()) null else {
                            {
                                IconButton(onClick = { settingsSearch = "" }) {
                                    Icon(Icons.Default.Close,
                                        contentDescription = stringResource(R.string.stats_search_clear))
                                }
                            }
                        },
                        shape = MaterialTheme.shapes.extraLarge,
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Search,
                        ),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(settingsScrollState)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                if (settingsCategory != "overview") {
                    val hero = when (settingsCategory) {
                        "general" -> Triple(
                            R.string.settings_category_general,
                            R.string.settings_category_general_desc,
                            Icons.Rounded.Palette,
                        )
                        "tracking" -> Triple(
                            R.string.settings_category_tracking,
                            R.string.settings_category_tracking_desc,
                            Icons.Rounded.GraphicEq,
                        )
                        "connections" -> Triple(
                            R.string.settings_category_connections,
                            R.string.settings_category_connections_desc,
                            Icons.Rounded.Link,
                        )
                        "data" -> Triple(
                            R.string.settings_category_data,
                            R.string.settings_category_data_desc,
                            Icons.Rounded.Storage,
                        )
                        else -> Triple(
                            R.string.settings_category_about,
                            R.string.settings_category_about_desc,
                            Icons.Rounded.Info,
                        )
                    }
                    me.avinas.tempo.ui.components.TempoSettingsCategoryHero(
                        title = stringResource(hero.first),
                        subtitle = stringResource(hero.second),
                        icon = hero.third,
                        style = when (settingsCategory) {
                            "general", "about" -> TempoEmblemStyle.DISCOVERY
                            "tracking" -> TempoEmblemStyle.PULSE
                            else -> TempoEmblemStyle.MEMORY
                        },
                    )
                    Spacer(Modifier.height(12.dp))
                }
                if (settingsCategory == "overview") {
                    if (settingsSearch.isNotBlank()) {
                        val tokens = settingsSearch.trim().split(Regex("\\s+"))
                        val directResults = listOf(
                            Triple(R.string.appearance_title, R.string.settings_search_general_terms, onNavigateToAppearance),
                            Triple(R.string.logo_picker_title, R.string.logo_picker_intro, onNavigateToLogoPicker),
                            Triple(R.string.update_center_title, R.string.update_center_desc, onNavigateToUpdates),
                            Triple(R.string.settings_manage_apps, R.string.settings_manage_apps_desc, onNavigateToSupportedApps),
                            Triple(R.string.settings_backup_restore, R.string.settings_backup_restore_desc, onNavigateToBackup),
                            Triple(R.string.settings_import_apple_music, R.string.settings_import_apple_music_desc, onNavigateToAppleMusicImport),
                            Triple(R.string.settings_import_spotify_json, R.string.settings_import_spotify_json_desc, onNavigateToSpotifyJsonImport),
                            Triple(R.string.settings_import_youtube_music, R.string.settings_import_youtube_music_desc, onNavigateToYouTubeMusicImport),
                            Triple(R.string.settings_import_deezer, R.string.settings_import_deezer_desc, onNavigateToDeezerImport),
                            Triple(R.string.settings_import_lastfm, R.string.settings_import_lastfm_desc, onNavigateToLastFmImport),
                            Triple(R.string.settings_import_musicolet, R.string.settings_import_musicolet_desc, onNavigateToMusicoletImport),
                            Triple(R.string.settings_background_protection, R.string.settings_search_tracking_terms, onNavigateToBackgroundProtection),
                            Triple(R.string.desktop_link_title, R.string.settings_search_connections_terms, onNavigateToDesktop),
                            Triple(R.string.extension_import_title, R.string.settings_search_data_terms, onNavigateToExtensionImport),
                            Triple(R.string.settings_your_data, R.string.settings_search_data_terms, onNavigateToYourData),
                            Triple(R.string.enrichment_report_settings_option,
                                R.string.enrichment_report_settings_option_desc, onNavigateToEnrichmentReport),
                        ).mapNotNull { (titleId, subtitleId, open) ->
                            if (open == null) null else Triple(stringResource(titleId), stringResource(subtitleId), open)
                        }.filter { (title, subtitle, _) ->
                            tokens.all { title.contains(it, true) || subtitle.contains(it, true) }
                        }
                        directResults.forEach { (title, subtitle, open) ->
                            SettingsOption(title = title, subtitle = subtitle, onClick = open)
                        }
                        val categories = listOf(
                            Triple("general", stringResource(R.string.settings_category_general), stringResource(R.string.settings_search_general_terms)),
                            Triple("tracking", stringResource(R.string.settings_category_tracking), stringResource(R.string.settings_search_tracking_terms)),
                            Triple("connections", stringResource(R.string.settings_category_connections), stringResource(R.string.settings_search_connections_terms)),
                            Triple("data", stringResource(R.string.settings_category_data), stringResource(R.string.settings_search_data_terms)),
                            Triple("about", stringResource(R.string.settings_category_about), stringResource(R.string.settings_search_about_terms)),
                        ).filter { (_, title, terms) ->
                            tokens.all { title.contains(it, true) || terms.contains(it, true) }
                        }
                        if (categories.isEmpty() && directResults.isEmpty()) Text(stringResource(R.string.settings_search_empty))
                        categories.forEach { (category, title, terms) ->
                            SettingsOption(title = title, subtitle = terms, onClick = { settingsCategory = category })
                        }
                    } else {
                    SettingsSectionHeader(stringResource(R.string.settings_category_browse))
                    me.avinas.tempo.ui.components.TempoSettingsBento(
                        destinations = listOfNotNull(
                            onNavigateToAppearance?.let { open ->
                                me.avinas.tempo.ui.components.TempoSettingsDestination(
                                title = stringResource(R.string.settings_category_tracking),
                                subtitle = stringResource(R.string.settings_category_tracking_desc),
                                icon = Icons.Rounded.GraphicEq,
                                style = TempoEmblemStyle.PULSE,
                                tone = me.avinas.tempo.ui.components.TempoTileTone.AQUA,
                                onClick = { settingsCategory = "tracking" },
                            ),
                            me.avinas.tempo.ui.components.TempoSettingsDestination(
                                    title = stringResource(R.string.appearance_title),
                                    subtitle = stringResource(R.string.appearance_settings_summary),
                                    icon = Icons.Rounded.Palette,
                                    style = TempoEmblemStyle.DISCOVERY,
                                    onClick = open,
                                    featured = true,
                                    tone = me.avinas.tempo.ui.components.TempoTileTone.ROSE,
                                )
                            },
                            me.avinas.tempo.ui.components.TempoSettingsDestination(
                                title = stringResource(R.string.settings_category_general),
                                subtitle = stringResource(R.string.settings_category_general_desc),
                                icon = Icons.Rounded.Settings,
                                style = TempoEmblemStyle.DISCOVERY,
                                tone = me.avinas.tempo.ui.components.TempoTileTone.LILAC,
                                onClick = { settingsCategory = "general" },
                            ),
                            me.avinas.tempo.ui.components.TempoSettingsDestination(
                                title = stringResource(R.string.settings_category_connections),
                                subtitle = stringResource(R.string.settings_category_connections_desc),
                                icon = Icons.Rounded.Link,
                                style = TempoEmblemStyle.MEMORY,
                                tone = me.avinas.tempo.ui.components.TempoTileTone.SKY,
                                onClick = { settingsCategory = "connections" },
                            ),
                            me.avinas.tempo.ui.components.TempoSettingsDestination(
                                title = stringResource(R.string.settings_category_data),
                                subtitle = stringResource(R.string.settings_category_data_desc),
                                icon = Icons.Rounded.Storage,
                                style = TempoEmblemStyle.MEMORY,
                                tone = me.avinas.tempo.ui.components.TempoTileTone.AMBER,
                                onClick = { settingsCategory = "data" },
                            ),
                            me.avinas.tempo.ui.components.TempoSettingsDestination(
                                title = stringResource(R.string.settings_category_about),
                                subtitle = stringResource(R.string.settings_category_about_desc),
                                icon = Icons.Rounded.Info,
                                style = TempoEmblemStyle.DISCOVERY,
                                tone = me.avinas.tempo.ui.components.TempoTileTone.ROSE,
                                onClick = { settingsCategory = "about" },
                            ),
                        ),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsSectionHeader(stringResource(R.string.settings_tracking_access_status))
                    val trackingHealth = trackingHeartbeat?.let { heartbeat ->
                        evaluateTrackingHealth(
                            permissionGranted = notificationListenerGranted,
                            listenerConnected = heartbeat.listenerConnected,
                            lastHeartbeatAtMs = heartbeat.lastServiceAliveAt,
                            lastNotificationCallbackAtMs = heartbeat.lastNotificationCallbackAt,
                            lastMediaSessionCallbackAtMs = heartbeat.lastMediaSessionCallbackAt,
                            nowMs = trackingStatusCheckedAt,
                        )
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column {
                            SettingsOption(
                                title = stringResource(R.string.settings_tracking_access_title),
                                subtitle = stringResource(
                                    when {
                                        trackingHealth == null -> R.string.ui_loading
                                        notificationListenerGranted -> R.string.settings_tracking_access_granted
                                        else -> R.string.settings_tracking_access_missing
                                    }
                                ),
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                    } catch (_: ActivityNotFoundException) {
                                        Toast.makeText(
                                            context,
                                            resources.getString(R.string.settings_notification_settings_error),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            SettingsOption(
                                title = stringResource(R.string.settings_health_listener),
                                subtitle = stringResource(
                                    when (trackingHealth?.listenerState) {
                                        null -> R.string.ui_loading
                                        TrackingListenerState.PERMISSION_REQUIRED ->
                                            R.string.settings_health_permission_required
                                        TrackingListenerState.WAITING ->
                                            R.string.settings_health_waiting
                                        TrackingListenerState.STALE ->
                                            R.string.settings_health_stale
                                        TrackingListenerState.CONNECTED ->
                                            R.string.settings_health_connected
                                    }
                                ),
                                onClick = { settingsCategory = "tracking" },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            SettingsOption(
                                title = stringResource(R.string.settings_health_last_signal),
                                subtitle = trackingHealth?.lastPlayerSignalAgeMs?.let {
                                    val minutes = it / 60_000L
                                    if (minutes < 1L) stringResource(R.string.settings_health_just_now)
                                    else stringResource(R.string.settings_health_minutes_ago, minutes)
                                } ?: stringResource(if (trackingHealth == null) R.string.ui_loading else R.string.settings_health_no_signal),
                                onClick = {
                                    if (onNavigateToYourData != null) onNavigateToYourData()
                                    else settingsCategory = "tracking"
                                },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    SettingsSectionHeader(stringResource(R.string.settings_backup_overview_header))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        // BackupSettingsManager is updated by the Drive worker.
                        // Local folder backups have an independent WorkManager job.
                        val latestStatus = if (!backupOverview.isGoogleDriveEnabled) {
                            R.string.settings_backup_overview_disabled
                        } else when (backupOverview.lastBackupStatus) {
                            BackupStatus.NEVER -> R.string.settings_backup_overview_never
                            BackupStatus.SUCCESS -> R.string.settings_backup_overview_success
                            BackupStatus.FAILED -> R.string.settings_backup_overview_failed
                            BackupStatus.IN_PROGRESS -> R.string.settings_backup_overview_running
                        }
                        val latestSuccess = backupOverview.lastBackupTime
                            ?.takeIf { it > 0L && it <= System.currentTimeMillis() }
                            ?.let { timestamp ->
                                val formatted = java.text.DateFormat.getDateTimeInstance(
                                    java.text.DateFormat.SHORT, java.text.DateFormat.SHORT,
                                ).format(java.util.Date(timestamp))
                                stringResource(R.string.settings_backup_overview_last_success, formatted)
                            }
                        SettingsOption(
                            title = stringResource(R.string.settings_backup_restore),
                            subtitle = listOfNotNull(stringResource(latestStatus), latestSuccess)
                                .joinToString(" · "),
                            onClick = { onNavigateToBackup?.invoke() },
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    SettingsSectionHeader(stringResource(R.string.update_center_title))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        SettingsOption(
                            title = stringResource(R.string.update_check_now),
                            subtitle = stringResource(R.string.update_center_desc),
                            onClick = { onNavigateToUpdates?.invoke() },
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    } // Unfiltered settings overview
                }

                if (settingsCategory == "general") {
                // Profile Section
                SettingsSectionHeader(stringResource(R.string.settings_profile))
                GlassCard(
                    modifier =
                        Modifier.fillMaxWidth().clickable {
                            tempName = uiState.userName
                            showNameDialog = true
                        },
                    contentPadding = PaddingValues(16.dp),
                    variant = me.avinas.tempo.ui.components.GlassCardVariant.LowProminence,
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(64.dp)
                                    .clickable {
                                        profileImageLauncher.launch("image/*")
                                    },
                            contentAlignment = Alignment.BottomEnd,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(
                                            brush =
                                                androidx.compose.ui.graphics.Brush.linearGradient(
                                                    colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer),
                                                ),
                                            shape = CircleShape,
                                        ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (uiState.profileImagePath.isNullOrBlank()) {
                                    Text(
                                        text =
                                            uiState.userName
                                                .firstOrNull()
                                                ?.toString()
                                                ?.uppercase() ?: "U",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                } else {
                                    me.avinas.tempo.ui.components.CachedAsyncImage(
                                        imageUrl = uiState.profileImagePath,
                                        contentDescription = stringResource(R.string.settings_profile_photo),
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    )
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                tonalElevation = 0.dp,
                                modifier = Modifier.size(22.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = stringResource(R.string.settings_change_photo),
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.userName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                            Text(
                                text = stringResource(R.string.settings_tap_to_edit),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = stringResource(R.string.settings_tap_photo_to_change),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (uiState.profileImagePath.isNullOrBlank()) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.settings_update_name),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                        }
                        if (!uiState.profileImagePath.isNullOrBlank()) {
                            TextButton(
                                onClick = viewModel::removeProfileImage,
                                modifier = Modifier.align(Alignment.End),
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_remove_photo),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Appearance is a focused gallery instead of another long list of toggles.
                SettingsSectionHeader(stringResource(R.string.appearance_title))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    // A Surface/GlassCard is a Box: rows must be stacked
                    // explicitly or they paint at exactly the same position.
                    Column {
                        SettingsOption(
                            title = stringResource(R.string.appearance_title),
                            subtitle = stringResource(R.string.appearance_settings_summary),
                            onClick = onNavigateToAppearance,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsOption(
                            title = stringResource(R.string.logo_picker_title),
                            subtitle = stringResource(R.string.logo_picker_settings_description),
                            onClick = onNavigateToLogoPicker,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                // Language Section
                SettingsSectionHeader(stringResource(R.string.settings_language))
                GlassCard(
                    contentPadding = PaddingValues(0.dp),
                    variant = me.avinas.tempo.ui.components.GlassCardVariant.LowProminence,
                ) {
                    SettingsOption(
                        title = stringResource(R.string.settings_language_title),
                        subtitle = currentLanguageSubtitle,
                        onClick = { showLanguageDialog = true },
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Notifications
                SettingsSectionHeader(stringResource(R.string.settings_notifications))
                GlassCard(
                    contentPadding = PaddingValues(0.dp),
                    variant = me.avinas.tempo.ui.components.GlassCardVariant.LowProminence,
                ) {
                    Column {
                        SettingsSwitch(
                            title = stringResource(R.string.settings_daily_summary),
                            subtitle = stringResource(R.string.settings_daily_summary_desc),
                            checked = uiState.dailySummaryEnabled,
                            onCheckedChange = viewModel::toggleDailySummary,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsSwitch(
                            title = stringResource(R.string.settings_weekly_recap),
                            subtitle = stringResource(R.string.settings_weekly_recap_desc),
                            checked = uiState.weeklyRecapEnabled,
                            onCheckedChange = viewModel::toggleWeeklyRecap,
                        )
                        if (uiState.isGamificationEnabled) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            SettingsSwitch(
                                title = stringResource(R.string.settings_daily_challenges),
                                subtitle = stringResource(R.string.settings_daily_challenges_desc),
                                checked = uiState.dailyChallengesEnabled,
                                onCheckedChange = viewModel::toggleDailyChallenges,
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            SettingsSwitch(
                                title = stringResource(R.string.settings_achievements),
                                subtitle = stringResource(R.string.settings_achievements_desc),
                                checked = uiState.achievementsEnabled,
                                onCheckedChange = viewModel::toggleAchievements,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                } // General
                if (settingsCategory == "tracking") {
                // Tracking
                SettingsSectionHeader(stringResource(R.string.settings_music_tracking))
                GlassCard(
                    contentPadding = PaddingValues(0.dp),
                    variant = me.avinas.tempo.ui.components.GlassCardVariant.LowProminence,
                ) {
                    Column {
                        SettingsOption(
                            title = stringResource(R.string.settings_manage_permissions),
                            subtitle = stringResource(R.string.settings_manage_permissions_desc),
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(intent)
                                } catch (_: ActivityNotFoundException) {
                                    Toast
                                        .makeText(
                                            context,
                                            resources.getString(R.string.settings_notification_settings_error),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                }
                            },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsOption(
                            title = stringResource(R.string.settings_manage_apps),
                            subtitle = stringResource(R.string.settings_manage_apps_desc),
                            onClick = { onNavigateToSupportedApps?.invoke() },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsSwitch(
                            title = stringResource(R.string.settings_pause_tracking_low_battery),
                            subtitle =
                                if (uiState.pauseTrackingOnLowBattery) {
                                    stringResource(R.string.settings_pause_tracking_on_desc, batteryLevel)
                                } else {
                                    stringResource(R.string.settings_pause_tracking_off_desc)
                                },
                            checked = uiState.pauseTrackingOnLowBattery,
                            onCheckedChange = viewModel::togglePauseTrackingOnLowBattery,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsOption(
                            title = stringResource(R.string.settings_cover_source_title),
                            subtitle = selectedCoverSourceName,
                            onClick = { showCoverSourceDialog = true },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Background Protection - Only for Xiaomi/MIUI devices
                if (isXiaomiDevice) {
                    SettingsSectionHeader(stringResource(R.string.settings_background_protection))
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(0.dp),
                        variant = me.avinas.tempo.ui.components.GlassCardVariant.LowProminence,
                    ) {
                        SettingsOption(
                            title =
                                if (autostartState == OemBackgroundHelper.AutostartState.DISABLED) {
                                    stringResource(R.string.settings_configure_required)
                                } else {
                                    stringResource(R.string.settings_xiaomi_settings)
                                },
                            subtitle = stringResource(R.string.settings_prevent_killed),
                            onClick = { onNavigateToBackgroundProtection?.invoke() },
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Content Filtering
                SettingsSectionHeader(stringResource(R.string.settings_content_filtering))
                GlassCard(
                    contentPadding = PaddingValues(0.dp),
                    variant = me.avinas.tempo.ui.components.GlassCardVariant.LowProminence,
                ) {
                    Column {
                        SettingsSwitch(
                            title = stringResource(R.string.settings_filter_podcasts),
                            subtitle = stringResource(R.string.settings_filter_podcasts_desc),
                            checked = uiState.filterPodcasts,
                            onCheckedChange = viewModel::toggleFilterPodcasts,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsSwitch(
                            title = stringResource(R.string.settings_filter_audiobooks),
                            subtitle = stringResource(R.string.settings_filter_audiobooks_desc),
                            checked = uiState.filterAudiobooks,
                            onCheckedChange = viewModel::toggleFilterAudiobooks,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Advanced Settings
                SettingsSectionHeader(stringResource(R.string.settings_advanced_stats))
                GlassCard(
                    contentPadding = PaddingValues(0.dp),
                    variant = me.avinas.tempo.ui.components.GlassCardVariant.LowProminence,
                ) {
                    Column {
                        SettingsSwitch(
                            title = stringResource(R.string.settings_extended_audio),
                            subtitle = stringResource(R.string.settings_extended_audio_desc),
                            checked = uiState.extendedAudioAnalysisEnabled,
                            onCheckedChange = viewModel::toggleExtendedAudioAnalysis,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsSwitch(
                            title = stringResource(R.string.settings_enable_gamification),
                            subtitle = stringResource(R.string.settings_enable_gamification_desc),
                            checked = uiState.isGamificationEnabled,
                            onCheckedChange = viewModel::toggleGamificationEnabled,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsSwitch(
                            title = stringResource(R.string.settings_hide_streaks),
                            subtitle = stringResource(R.string.settings_hide_streaks_desc),
                            checked = uiState.hideStreaks,
                            onCheckedChange = viewModel::toggleHideStreaks,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                } // Tracking
                if (settingsCategory == "connections") {
                // Last.fm Import
                SettingsSectionHeader(stringResource(R.string.settings_import_history))
                GlassCard(
                    contentPadding = PaddingValues(0.dp),
                    variant = me.avinas.tempo.ui.components.GlassCardVariant.LowProminence,
                ) {
                    Column {
                        if (uiState.isLastFmConnected) {
                            SettingsOption(
                                title = stringResource(R.string.settings_lastfm_connected, uiState.lastFmUsername ?: "Connected"),
                                subtitle =
                                    when (uiState.lastFmSyncFrequency) {
                                        "DAILY" -> stringResource(R.string.settings_lastfm_sync_daily)
                                        "WEEKLY" -> stringResource(R.string.settings_lastfm_sync_weekly)
                                        else -> stringResource(R.string.settings_lastfm_import_complete)
                                    },
                                onClick = { onNavigateToLastFmImport?.invoke() },
                            )
                        } else {
                            SettingsOption(
                                title = stringResource(R.string.settings_import_lastfm),
                                subtitle = stringResource(R.string.settings_import_lastfm_desc),
                                onClick = { onNavigateToLastFmImport?.invoke() },
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        SettingsOption(
                            title = if (spotifyConnected) "Spotify API · Connected" else "Spotify API · Connect account",
                            subtitle = if (spotifyConnected) {
                                "Spotify authorization active. Use the history sync option below to import recent plays."
                            } else {
                                "Sign into Spotify in your browser to allow Tempo to import listening history."
                            },
                            onClick = {
                                if (!spotifyConnected) scope.launch {
                                    val intent = try {
                                        viewModel.startSpotifyAuthorization(spotifyClientId)
                                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                                        throw cancelled
                                    } catch (failure: Exception) {
                                        Toast.makeText(context, resources.getString(R.string.spotify_secure_storage_unavailable), Toast.LENGTH_LONG).show()
                                        return@launch
                                    }
                                    if (intent == null) {
                                        Toast.makeText(context, "Enter a valid Spotify Client ID first.", Toast.LENGTH_LONG).show()
                                    } else {
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: ActivityNotFoundException) {
                                            Toast.makeText(context, "No browser available for Spotify login.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                        )
                        if (spotifyAccountLoaded && !spotifyConnected) {
                            OutlinedTextField(
                                value = spotifyClientId,
                                onValueChange = { spotifyClientId = it.trim() },
                                label = { Text("Spotify Developer Client ID (32 characters)") },
                                supportingText = { Text("Create a Spotify Developer app and register tempo://spotify-callback as its redirect URI.") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            )
                        }

                        if (spotifyConnected) {
                            SettingsOption(
                                title = "Reconnect Spotify / update permissions",
                                subtitle = "Reconnect to grant current playback-state access for Spotify Connect verification. Existing listening history is retained.",
                                onClick = {
                                    scope.launch {
                                        viewModel.disconnectSpotify()
                                        spotifyConnected = false
                                        Toast.makeText(
                                            context,
                                            "Spotify disconnected. Tap Connect account to authorize updated permissions.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                            )
                        }

                        if (spotifyConnected && uiState.spotifyApiOnlyMode) {
                            SettingsOption(
                                title = "Sync Spotify history now",
                                subtitle = "Fetch recently played tracks immediately; Spotify may not yet have reported the latest play.",
                                onClick = {
                                    me.avinas.tempo.worker.SpotifyPollingWorker.syncNow(context)
                                    Toast.makeText(context, "Spotify sync requested", Toast.LENGTH_SHORT).show()
                                },
                            )
                        }

                        SettingsSwitch(
                            title = "Spotify API history sync",
                            subtitle = "Automatic Spotify history reconciliation (about every 20 minutes) alongside local broadcasts and MediaSession. Requires Spotify login.",
                            checked = uiState.spotifyApiOnlyMode,
                            onCheckedChange = { enabled ->
                                if (enabled && !spotifyConnected) {
                                    Toast.makeText(
                                        context,
                                        "Connect your Spotify account before using Spotify API sync.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    viewModel.toggleSpotifyApiOnlyMode(enabled)
                                }
                            },
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        SettingsSwitch(
                            title = "Spotify Device Broadcast Status",
                            subtitle = "Real-time local Spotify playback updates. Turn on the matching switch in Spotify > Settings > Playback. Requires Tempo tracking service.",
                            checked = spotifyBroadcastEnabled,
                            onCheckedChange = { enabled ->
                                SpotifyBroadcastPresence.setEnabled(context, enabled)
                                spotifyBroadcastEnabled = enabled
                            },
                        )

                        // Uses either Spotify broadcast or the regular Android
                        // tracking fallback, even when broadcast mode is disabled.
                        LiveNowPlayingCard(modifier = Modifier.padding(horizontal = 16.dp))

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        SettingsOption(
                            title = stringResource(R.string.settings_import_spotify_json),
                            subtitle = stringResource(R.string.settings_import_spotify_json_desc),
                            onClick = { onNavigateToSpotifyJsonImport?.invoke() },
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        SettingsOption(
                            title = stringResource(R.string.settings_import_deezer),
                            subtitle = stringResource(R.string.settings_import_deezer_desc),
                            onClick = { onNavigateToDeezerImport?.invoke() },
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        SettingsOption(
                            title = stringResource(R.string.settings_import_apple_music),
                            subtitle = stringResource(R.string.settings_import_apple_music_desc),
                            onClick = { onNavigateToAppleMusicImport?.invoke() },
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        SettingsOption(
                            title = stringResource(R.string.settings_import_youtube_music),
                            subtitle = stringResource(R.string.settings_import_youtube_music_desc),
                            onClick = { onNavigateToYouTubeMusicImport?.invoke() },
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        SettingsOption(
                            title = stringResource(R.string.settings_import_musicolet),
                            subtitle = stringResource(R.string.settings_import_musicolet_desc),
                            onClick = { onNavigateToMusicoletImport?.invoke() },
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        SettingsOption(
                            title = stringResource(R.string.settings_import_extension),
                            subtitle = stringResource(R.string.settings_import_extension_desc),
                            onClick = { onNavigateToExtensionImport?.invoke() },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Desktop Sync
                SettingsSectionHeader(stringResource(R.string.desktop_link_section_header))
                GlassCard(
                    contentPadding = PaddingValues(0.dp),
                    variant = me.avinas.tempo.ui.components.GlassCardVariant.LowProminence,
                ) {
                    Column {
                        // Battery status warning if critical or low
                        if (isBatteryCritical) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .background(TempoErrorDeep.copy(alpha = 0.25f))
                                        .padding(12.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_desktop_battery_critical, batteryLevel),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TempoErrorSoft,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        } else if (isLowBattery) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .background(TempoWarningDeep.copy(alpha = 0.2f))
                                        .padding(12.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_desktop_battery_low, batteryLevel),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TempoWarningBright,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        } else {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .background(TempoSuccessDeep.copy(alpha = 0.15f))
                                        .padding(12.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_desktop_battery_healthy, batteryLevel),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TempoSuccessBright,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }

                        SettingsOption(
                            title = stringResource(R.string.desktop_link_settings_title),
                            subtitle = stringResource(R.string.desktop_link_settings_subtitle),
                            onClick = { onNavigateToDesktop() },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                } // Connections
                if (settingsCategory == "data") {
                // Data Management
                SettingsSectionHeader(stringResource(R.string.settings_your_data))
                GlassCard(
                    contentPadding = PaddingValues(0.dp),
                    variant = me.avinas.tempo.ui.components.GlassCardVariant.LowProminence,
                ) {
                    Column {
                        // The opt-out for anonymous app-health reporting lives here, at the
                        // top of "Your Data" next to backup/restore rather than buried in
                        // About. Hidden entirely in builds that cannot report (no analytics
                        // endpoint, or debug), since a switch for collection that cannot
                        // happen would be misleading.
                        if (uiState.analyticsConfigured) {
                            SettingsSwitch(
                                title = stringResource(R.string.settings_analytics_toggle),
                                subtitle = stringResource(R.string.settings_analytics_toggle_desc),
                                checked = uiState.analyticsEnabled,
                                onCheckedChange = viewModel::setAnalyticsEnabled,
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            // Offered even while reporting is off: someone who has just turned
                            // it off may still want the id they used beforehand replaced.
                            SettingsOption(
                                title = stringResource(R.string.settings_analytics_reset_id),
                                subtitle = stringResource(R.string.settings_analytics_reset_id_desc),
                                onClick = viewModel::resetAnalyticsId,
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        SettingsSwitch(
                            title = stringResource(R.string.settings_smart_merge),
                            subtitle = stringResource(R.string.settings_smart_merge_desc),
                            checked = uiState.mergeAlternateVersions,
                            onCheckedChange = viewModel::toggleMergeAlternateVersions,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsOption(
                            title = stringResource(R.string.settings_backup_restore),
                            subtitle = stringResource(R.string.settings_backup_restore_desc),
                            onClick = { onNavigateToBackup?.invoke() },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsOption(
                            title = "Check listening-history accuracy",
                            subtitle = "Find older Spotify API records with unverified listening minutes. No automatic changes.",
                            onClick = {
                                historyBackupConfirmed = false
                                showHistoryAuditDialog = true
                                viewModel.scanHistoryQuality()
                            },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsOption(
                            title = stringResource(R.string.enrichment_report_settings_option),
                            subtitle = stringResource(R.string.enrichment_report_settings_option_desc),
                            onClick = { onNavigateToEnrichmentReport?.invoke() },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsOption(
                            title = stringResource(R.string.settings_clear_all),
                            textColor = TempoError,
                            onClick = { showClearDataDialog = true },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

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
                                    if (historyQuality.reconstructedCount > 0) {
                                        Text(
                                            "Reconstructed plays are flagged for review, not automatically deleted."
                                        )
                                    }
                                    historyQuality.repairedCount?.let { repaired ->
                                        Text("Corrected $repaired API records. Reopen statistics to see refreshed totals.")
                                    }
                                    if (historyQuality.apiEstimatedCount > 0 &&
                                        historyQuality.repairedCount == null
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = historyBackupConfirmed,
                                                onCheckedChange = { historyBackupConfirmed = it },
                                            )
                                            Text(
                                                "I have exported a backup and accept that " +
                                                    "estimated minutes will be reset to unknown."
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
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
                            } else {
                                TextButton(onClick = { showHistoryAuditDialog = false }) {
                                    Text("Close")
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

                } // Library and data
                if (settingsCategory == "about") {
                TempoAboutCredits(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(24.dp))
                // Community
                SettingsSectionHeader(stringResource(R.string.settings_community))
                GlassCard(
                    contentPadding = PaddingValues(0.dp),
                    variant = me.avinas.tempo.ui.components.GlassCardVariant.LowProminence,
                ) {
                    Column {
                        SettingsOption(
                            title = stringResource(R.string.settings_reddit),
                            subtitle = stringResource(R.string.settings_reddit_sub),
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.reddit.com/r/TempoStats/"))
                                    context.startActivity(intent)
                                } catch (_: ActivityNotFoundException) {
                                    Toast.makeText(context, resources.getString(R.string.settings_no_browser), Toast.LENGTH_SHORT).show()
                                }
                            },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        SettingsOption(
                            title = stringResource(R.string.settings_github),
                            subtitle = stringResource(R.string.settings_github_desc),
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/matchalattewithbanana/Tempo"))
                                    context.startActivity(intent)
                                } catch (_: ActivityNotFoundException) {
                                    Toast.makeText(context, resources.getString(R.string.settings_no_browser), Toast.LENGTH_SHORT).show()
                                }
                            },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsOption(
                            title = stringResource(R.string.settings_contribute),
                            subtitle = stringResource(R.string.settings_contribute_desc),
                            onClick = {
                                try {
                                    val intent =
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://github.com/matchalattewithbanana/Tempo/blob/main/CONTRIBUTING.md"),
                                        )
                                    context.startActivity(intent)
                                } catch (_: ActivityNotFoundException) {
                                    Toast.makeText(context, resources.getString(R.string.settings_no_browser), Toast.LENGTH_SHORT).show()
                                }
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Privacy & About
                SettingsSectionHeader(stringResource(R.string.settings_about))
                GlassCard(
                    contentPadding = PaddingValues(0.dp),
                    variant = me.avinas.tempo.ui.components.GlassCardVariant.LowProminence,
                ) {
                    Column {
                        SettingsOption(
                            title = stringResource(
                                if (me.avinas.tempo.BuildConfig.PLAY_STORE_REVIEWS) R.string.settings_rate_play_store else R.string.settings_report_issue,
                            ),
                            onClick = {
                                if (me.avinas.tempo.BuildConfig.PLAY_STORE_REVIEWS) {
                                    ReviewUtils.openPlayStoreListing(context)
                                } else {
                                    try {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/matchalattewithbanana/Tempo/issues")),
                                        )
                                    } catch (_: ActivityNotFoundException) {
                                        Toast.makeText(context, resources.getString(R.string.settings_no_browser), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsOption(
                            title = stringResource(R.string.settings_privacy_policy),
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/matchalattewithbanana/Tempo/blob/main/PRIVACY.md"))
                                    context.startActivity(intent)
                                } catch (_: ActivityNotFoundException) {
                                    Toast.makeText(context, resources.getString(R.string.settings_no_browser), Toast.LENGTH_SHORT).show()
                                }
                            },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsOption(
                            title = stringResource(R.string.settings_data_diagnostics),
                            subtitle = stringResource(R.string.settings_data_diagnostics_desc),
                            onClick = { onNavigateToYourData?.invoke() },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsOption(
                            title = stringResource(R.string.settings_version),
                            subtitle = versionName,
                            onClick = { onNavigateToUpdates?.invoke() },
                        )
                    }
                }

                } // Community and About
                Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
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
