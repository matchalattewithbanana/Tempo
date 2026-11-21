package me.avinas.tempo.ui.settings

import android.graphics.drawable.Drawable
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import me.avinas.tempo.ui.components.TempoCircularLoadingIndicator
import me.avinas.tempo.ui.components.TempoLinearLoadingIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import me.avinas.tempo.R
import me.avinas.tempo.data.InstalledApp
import me.avinas.tempo.data.local.entities.AppPreference
import me.avinas.tempo.data.local.entities.ManualContentMark
import me.avinas.tempo.data.preferences.TrackingRulesPreferences
import me.avinas.tempo.data.preferences.TrackingRulesPreferences.ContentOverrideType
import me.avinas.tempo.data.preferences.TrackingRulesPreferences.DurationMode
import me.avinas.tempo.ui.components.DeepOceanBackground
import me.avinas.tempo.ui.components.GlassCard
import me.avinas.tempo.ui.components.GlassCardVariant
import me.avinas.tempo.ui.components.SettingsSectionHeader
import me.avinas.tempo.ui.components.TempoDialogShape
import me.avinas.tempo.ui.theme.GlassBorderSoft
import me.avinas.tempo.ui.theme.GlassFrostSoft
import me.avinas.tempo.ui.theme.TempoDarkSurfaceElevated
import me.avinas.tempo.ui.theme.TempoPrimary
import me.avinas.tempo.ui.theme.TempoSuccessBright
import me.avinas.tempo.ui.theme.TempoSurfaceDialog
import me.avinas.tempo.ui.theme.TextOnAccent
import me.avinas.tempo.ui.theme.TextPrimary
import me.avinas.tempo.ui.theme.TextQuaternary
import me.avinas.tempo.ui.theme.TextSecondary
import me.avinas.tempo.ui.theme.TextTertiary

/**
 * Semaphore that limits the number of concurrent PackageManager icon loads.
 * PackageManager.getApplicationIcon() internally acquires locks on AssetManager/ResourcesImpl.
 * Without this limit, loading many icons in parallel causes severe lock contention which
 * can starve the main thread and trigger an ANR (Input dispatching timed out).
 */
private val iconLoadingSemaphore = Semaphore(permits = 4)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SupportedAppsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AppPreferenceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val trackingRules = remember(context) { TrackingRulesPreferences(context.applicationContext) }

    var showAddAppDialog by remember { mutableStateOf(false) }
    var showMinPlayDialog by remember { mutableStateOf(false) }
    var showDefaultMaxDialog by remember { mutableStateOf(false) }
    var showContentOverridesDialog by remember { mutableStateOf(false) }
    var durationApp by remember { mutableStateOf<AppPreference?>(null) }
    var rulesRevision by remember { mutableIntStateOf(0) }

    // Reading the duration SharedPreferences is cheap; revision makes summaries refresh
    // immediately after a duration dialog saves a new rule. Content overrides are observed
    // directly from Room through the ViewModel.
    val minimumPlayDurationMs = remember(rulesRevision) { trackingRules.minimumPlayDurationMs }
    val defaultMaxMusicDurationMs = remember(rulesRevision) { trackingRules.defaultMaxMusicDurationMs }
    val trackUnrecognizedApps = remember(rulesRevision) { trackingRules.trackUnrecognizedApps }
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(uiState.searchQuery) { listState.scrollToItem(0) }
    val overrideCount = uiState.contentOverrides.size

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_manage_apps),
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back), tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    IconButton(onClick = { showAddAppDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add App", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        // AppNavigation supplies the Appearance backdrop; avoid duplicate overdraw.
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Persistent, native Material search avoids rebuilding a nested
                // glass card every keystroke. Search results update off-main.
                Surface(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = viewModel::setSearchQuery,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        placeholder = {
                            Text("Search apps on this device…",
                                style = MaterialTheme.typography.bodyLarge)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge,
                    )
                }
                // Only a narrow indicator changes while text is being filtered;
                // never replace the entire list with a spinner on each keystroke.
                Box(Modifier.fillMaxWidth().height(3.dp)) {
                    if (uiState.searchQuery != searchQuery.trim() && !uiState.isLoading) {
                        TempoLinearLoadingIndicator(Modifier.fillMaxSize())
                    }
                }
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        TempoCircularLoadingIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (uiState.searchQuery.isBlank()) {
                            item(key = "header_tracking_rules") {
                                SettingsSectionHeader(stringResource(R.string.tracking_rules_section))
                            }
                            item(key = "tracking_rules") {
                                TrackingRulesCard(
                                    minimumPlayDurationMs = minimumPlayDurationMs,
                                    defaultMaxMusicDurationMs = defaultMaxMusicDurationMs,
                                    overrideCount = overrideCount,
                                    trackUnrecognizedApps = trackUnrecognizedApps,
                                    onMinimumPlayClick = { showMinPlayDialog = true },
                                    onDefaultMaxClick = { showDefaultMaxDialog = true },
                                    onOverridesClick = { showContentOverridesDialog = true },
                                    onTrackUnrecognizedChange = { enabled ->
                                        trackingRules.trackUnrecognizedApps = enabled
                                        rulesRevision++
                                    }
                                )
                            }

                        }

                        // Apps Tempo heard but refused to track. This is what makes the
                        // allowlist liveable: an unlisted player shows up here with a one-tap
                        // Allow instead of silently going untracked.
                        if (uiState.detectedApps.isNotEmpty()) {
                            item(key = "header_detected_apps") {
                                SettingsSectionHeader("Detected — not tracked")
                            }
                            item(key = "detected_apps_help") {
                                Text(
                                    text = "Tempo only tracks apps it knows are music players. " +
                                        "These apps played something Tempo did not recognize. " +
                                        "Allow one if it is a music source you want in your " +
                                        "journal, or block it so Tempo stops offering it.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                            item(key = "list_detected_apps") {
                                GlassCard(
                                    contentPadding = PaddingValues(0.dp),
                                    variant = GlassCardVariant.LowProminence
                                ) {
                                    Column(modifier = Modifier.animateContentSize()) {
                                        uiState.detectedApps.forEachIndexed { index, app ->
                                            DetectedAppItem(
                                                app = app,
                                                onAllow = { viewModel.allowDetectedApp(app.packageName) },
                                                onBlock = { viewModel.blockApp(app.packageName) },
                                                showDivider = index < uiState.detectedApps.size - 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (uiState.preinstalledApps.isNotEmpty()) {
                            item(key = "header_music_apps") {
                                SettingsSectionHeader("Music Apps")
                            }
                            item(key = "list_music_apps") {
                                GlassCard(
                                    contentPadding = PaddingValues(0.dp),
                                    variant = GlassCardVariant.LowProminence
                                ) {
                                    Column(modifier = Modifier.animateContentSize()) {
                                        uiState.preinstalledApps.forEachIndexed { index, app ->
                                            AppPreferenceItem(
                                                app = app,
                                                onToggle = { enabled ->
                                                    viewModel.toggleAppEnabled(app.packageName, enabled)
                                                },
                                                onBlock = { viewModel.blockApp(app.packageName) },
                                                onDurationClick = { durationApp = app },
                                                durationSummary = appDurationSummary(trackingRules, app.packageName),
                                                showDivider = index < uiState.preinstalledApps.size - 1,
                                                isInstalled = app.packageName in uiState.installedPackageNames
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (uiState.userAddedApps.isNotEmpty()) {
                            item(key = "header_user_apps") {
                                SettingsSectionHeader("Your Apps")
                            }
                            item(key = "list_user_apps") {
                                GlassCard(
                                    contentPadding = PaddingValues(0.dp),
                                    variant = GlassCardVariant.LowProminence
                                ) {
                                    Column(modifier = Modifier.animateContentSize()) {
                                        uiState.userAddedApps.forEachIndexed { index, app ->
                                            AppPreferenceItem(
                                                app = app,
                                                onToggle = { enabled ->
                                                    viewModel.toggleAppEnabled(app.packageName, enabled)
                                                },
                                                onRemove = { viewModel.removeApp(app.packageName) },
                                                onDurationClick = { durationApp = app },
                                                durationSummary = appDurationSummary(trackingRules, app.packageName),
                                                showDivider = index < uiState.userAddedApps.size - 1,
                                                isUserAdded = true,
                                                isInstalled = app.packageName in uiState.installedPackageNames
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Search now reaches every installed app, including ones Tempo has no
                        // record of. Previously the query only filtered the curated lists, so
                        // searching for the app you actually needed to act on returned nothing.
                        if (uiState.searchQuery.isNotBlank() && uiState.searchResults.isNotEmpty()) {
                            item(key = "header_search_results") {
                                SettingsSectionHeader("Other installed apps")
                            }
                            item(key = "search_results_help") {
                                Text(
                                    text = "Not tracked. Allow an app to start journaling its " +
                                        "playback, or block it to keep it out of the way.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                            items(uiState.searchResults, key = { "search_" + it.packageName }) { app ->
                                GlassCard(contentPadding = PaddingValues(0.dp), variant = GlassCardVariant.LowProminence) {
                                    AllowableAppItem(
                                        app = app,
                                        onAllow = { viewModel.allowApp(app.packageName) },
                                        onBlock = { viewModel.blockApp(app.packageName) },
                                        showDivider = false,
                                    )
                                }
                            }
                        }

                        if (uiState.blockedApps.isNotEmpty()) {
                            item(key = "header_blocked_apps") {
                                SettingsSectionHeader("Blocked Apps")
                            }
                            item(key = "list_blocked_apps") {
                                GlassCard(
                                    contentPadding = PaddingValues(0.dp),
                                    variant = GlassCardVariant.LowProminence
                                ) {
                                    Column(modifier = Modifier.animateContentSize()) {
                                        uiState.blockedApps.forEachIndexed { index, app ->
                                            BlockedAppItem(
                                                app = app,
                                                onUnblock = { viewModel.unblockApp(app.packageName) },
                                                showDivider = index < uiState.blockedApps.size - 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Voice notes, dialers and recorders. These are rejected in code and
                        // deliberately have no toggle: offering "Block" would imply tracking is
                        // an option, and the reason users could not find Viber or Signal here at
                        // all was that code-only lists never rendered.
                        if (uiState.neverTrackedApps.isNotEmpty()) {
                            item(key = "header_never_tracked") {
                                SettingsSectionHeader("Not supported")
                            }
                            item(key = "never_tracked_help") {
                                Text(
                                    text = "These apps play voice notes, calls or recordings — " +
                                        "never music — so Tempo does not track them and cannot " +
                                        "be asked to.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                            item(key = "list_never_tracked") {
                                GlassCard(
                                    contentPadding = PaddingValues(0.dp),
                                    variant = GlassCardVariant.LowProminence
                                ) {
                                    Column(modifier = Modifier.animateContentSize()) {
                                        uiState.neverTrackedApps.forEachIndexed { index, app ->
                                            NeverTrackedAppItem(
                                                app = app,
                                                showDivider = index < uiState.neverTrackedApps.size - 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (
                            uiState.preinstalledApps.isEmpty() &&
                            uiState.userAddedApps.isEmpty() &&
                            uiState.blockedApps.isEmpty() &&
                            uiState.detectedApps.isEmpty() &&
                            uiState.neverTrackedApps.isEmpty() &&
                            uiState.searchResults.isEmpty()
                        ) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (uiState.isScanningInstalledApps) stringResource(R.string.ui_loading) else "No apps found",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        item(key = "help_text") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.tracking_rules_help),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddAppDialog) {
        val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
        AddAppDialog(
            installedApps = installedApps,
            onDismiss = { showAddAppDialog = false },
            onAddApp = { packageName, displayName ->
                viewModel.addCustomApp(packageName, displayName)
                showAddAppDialog = false
            }
        )
    }

    if (showMinPlayDialog) {
        MinimumPlayDurationDialog(
            initialMs = trackingRules.minimumPlayDurationMs,
            onDismiss = { showMinPlayDialog = false },
            onSave = { value ->
                trackingRules.minimumPlayDurationMs = value
                rulesRevision++
                showMinPlayDialog = false
            }
        )
    }

    if (showDefaultMaxDialog) {
        DefaultMaxDurationDialog(
            initialMs = trackingRules.defaultMaxMusicDurationMs,
            onDismiss = { showDefaultMaxDialog = false },
            onSave = { value ->
                trackingRules.defaultMaxMusicDurationMs = value
                rulesRevision++
                showDefaultMaxDialog = false
            }
        )
    }

    durationApp?.let { app ->
        AppDurationDialog(
            app = app,
            trackingRules = trackingRules,
            onDismiss = { durationApp = null },
            onSaved = {
                rulesRevision++
                durationApp = null
            }
        )
    }

    if (showContentOverridesDialog) {
        ContentOverridesDialog(
            rules = uiState.contentOverrides,
            onAdd = viewModel::addContentOverride,
            onDelete = viewModel::removeContentOverride,
            onDismiss = { showContentOverridesDialog = false }
        )
    }
}

