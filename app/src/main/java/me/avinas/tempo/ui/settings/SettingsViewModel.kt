package me.avinas.tempo.ui.settings

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dagger.Lazy
import me.avinas.tempo.data.remote.spotify.SpotifyAuthManager
import me.avinas.tempo.data.remote.spotify.SpotifyTokenStorage
import android.net.Uri
import androidx.core.app.NotificationManagerCompat
import androidx.compose.runtime.Immutable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import me.avinas.tempo.data.drive.BackupSettings
import me.avinas.tempo.data.drive.BackupSettingsManager
import kotlinx.coroutines.launch
import me.avinas.tempo.BuildConfig
import me.avinas.tempo.data.analytics.AnalyticsConsent
import me.avinas.tempo.data.analytics.AnalyticsDefaults
import me.avinas.tempo.data.analytics.AnalyticsGate
import me.avinas.tempo.data.importexport.ImportConflictStrategy
import me.avinas.tempo.data.importexport.ImportExportManager
import me.avinas.tempo.data.importexport.ImportExportProgress
import me.avinas.tempo.data.importexport.ImportExportResult
import me.avinas.tempo.data.local.AppDatabase
import me.avinas.tempo.data.local.entities.AlbumArtSource
import me.avinas.tempo.data.local.entities.EnrichmentStatus
import me.avinas.tempo.data.enrichment.CoverArtSourcePreference
import me.avinas.tempo.data.reset.AppDataResetter
import me.avinas.tempo.data.profile.ProfileIdentityManager
import me.avinas.tempo.ui.onboarding.dataStore
import me.avinas.tempo.worker.ChallengeWorker
import me.avinas.tempo.worker.EnrichmentWorker
import me.avinas.tempo.worker.GamificationWorker
import me.avinas.tempo.worker.NotificationWorker
import me.avinas.tempo.worker.SpotifyPollingWorker
import me.avinas.tempo.service.SpotifyBroadcastPresence
import me.avinas.tempo.service.TrackingServiceHeartbeat
import me.avinas.tempo.utils.BatteryUtils
import me.avinas.tempo.utils.OemBackgroundHelper
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val database: AppDatabase,
        private val statsRepository: me.avinas.tempo.data.repository.StatsRepository,
        private val refreshCoordinator: me.avinas.tempo.data.repository.RefreshCoordinator,
        private val importExportManager: ImportExportManager,
        private val profileIdentityManager: ProfileIdentityManager,
        private val userPreferencesDao: me.avinas.tempo.data.local.dao.UserPreferencesDao,
        private val analyticsConsent: AnalyticsConsent,
        private val analyticsInstallId: me.avinas.tempo.data.analytics.AnalyticsInstallId,
        private val analyticsDailyLedger: me.avinas.tempo.data.analytics.AnalyticsDailyLedger,
        private val analyticsFeatureLedger: me.avinas.tempo.data.analytics.AnalyticsFeatureLedger,
        private val coverArtSourcePreference: CoverArtSourcePreference,
        private val backupSettingsManager: BackupSettingsManager,
        private val spotifyAuthManager: Lazy<SpotifyAuthManager>,
        private val spotifyTokenStorage: Lazy<SpotifyTokenStorage>,
        private val appDataResetter: AppDataResetter,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SettingsUiState())
        val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

        /** Snapshot of the existing backup DataStore; does not contact Drive. */
        val backupOverview: StateFlow<BackupSettings> = backupSettingsManager.settings
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), BackupSettings())

        // Import/Export states
        val importExportProgress: StateFlow<ImportExportProgress?> = importExportManager.progress

        private val _importExportResult = MutableStateFlow<ImportExportResult?>(null)
        val importExportResult: StateFlow<ImportExportResult?> = _importExportResult.asStateFlow()

        private val _showConflictDialog = MutableStateFlow<Uri?>(null)
        val showConflictDialog: StateFlow<Uri?> = _showConflictDialog.asStateFlow()

        private val _profileImageMessage = MutableStateFlow<String?>(null)
        val profileImageMessage: StateFlow<String?> = _profileImageMessage.asStateFlow()

        private val _historyQuality = MutableStateFlow(HistoryQualityUiState())
        val historyQuality: StateFlow<HistoryQualityUiState> = _historyQuality.asStateFlow()

        private var spotifyClientIdDraftDirty = false
        private val _runtimeStatus =
            MutableStateFlow(
                SettingsRuntimeStatus(
                    isXiaomiDevice = OemBackgroundHelper.isXiaomiDevice(),
                    spotifyBroadcastEnabled = SpotifyBroadcastPresence.enabled(context),
                    trackingStatusCheckedAt = System.currentTimeMillis(),
                ),
            )
        val runtimeStatus: StateFlow<SettingsRuntimeStatus> = _runtimeStatus.asStateFlow()

        // Keys
        private val NOTIF_DAILY_KEY = booleanPreferencesKey("notif_daily_summary")
        private val NOTIF_WEEKLY_KEY = booleanPreferencesKey("notif_weekly_recap")
        private val NOTIF_ACHIEVEMENTS_KEY = booleanPreferencesKey("notif_achievements")
        private val NOTIF_CHALLENGES_KEY = booleanPreferencesKey("notif_daily_challenges")
        private val EXTENDED_AUDIO_ANALYSIS_KEY = booleanPreferencesKey("extended_audio_analysis")
        private val USER_NAME_KEY =
            androidx.datastore.preferences.core
                .stringPreferencesKey("user_name")

        init {
            viewModelScope.launch {
                // Load DataStore preferences
                val dataStorePrefs = context.dataStore.data.first()

                // Load Room preferences
                val roomPrefs =
                    userPreferencesDao.getSync() ?: me.avinas.tempo.data.local.entities
                        .UserPreferences()

                _uiState.update {
                    SettingsUiState(
                        dailySummaryEnabled = dataStorePrefs[NOTIF_DAILY_KEY] ?: true,
                        weeklyRecapEnabled = dataStorePrefs[NOTIF_WEEKLY_KEY] ?: true,
                        achievementsEnabled = dataStorePrefs[NOTIF_ACHIEVEMENTS_KEY] ?: true,
                        dailyChallengesEnabled = dataStorePrefs[NOTIF_CHALLENGES_KEY] ?: true,
                        extendedAudioAnalysisEnabled = dataStorePrefs[EXTENDED_AUDIO_ANALYSIS_KEY] ?: false,
                        // Load preference directly from DataStore snapshot to avoid race conditions.
                        // decode() returns null both for "absent" and "not a known provider", and
                        // null *is* the Automatic value — falling back to the previous state here
                        // would keep a stale provider name selected after the user resets.
                        preferredCoverSource = CoverArtSourcePreference.decode(dataStorePrefs[CoverArtSourcePreference.PREFERENCE_KEY]),
                        userName = dataStorePrefs[USER_NAME_KEY] ?: "User",
                        profileImagePath = profileIdentityManager.getStoredProfileImagePath(),
                        mergeAlternateVersions = roomPrefs.mergeAlternateVersions,
                        filterPodcasts = roomPrefs.filterPodcasts,
                        filterAudiobooks = roomPrefs.filterAudiobooks,
                        spotifyApiOnlyMode = roomPrefs.spotifyApiOnlyMode,
                        isLastFmConnected = roomPrefs.lastfmConnected,
                        lastFmUsername = roomPrefs.lastfmUsername,
                        lastFmSyncFrequency = roomPrefs.lastfmSyncFrequency,
                        isGamificationEnabled = roomPrefs.isGamificationEnabled,
                        hideStreaks = roomPrefs.hideStreaks,
                        pauseTrackingOnLowBattery = roomPrefs.pauseTrackingOnLowBattery,
                        analyticsConfigured =
                            AnalyticsGate.isBuildConfigured(
                                endpoint = BuildConfig.ANALYTICS_ENDPOINT,
                                isDebug = BuildConfig.DEBUG,
                                debugPreview = BuildConfig.ANALYTICS_DEBUG_PREVIEW,
                            ),
                    )
                }
            }

            // Watch DataStore for updates
            viewModelScope.launch {
                profileIdentityManager.profileIdentity.collect { identity ->
                    _uiState.update {
                        it.copy(
                            userName = identity.userName,
                            profileImagePath = identity.profileImagePath,
                        )
                    }
                }
            }

            viewModelScope.launch {
                context.dataStore.data.collect { preferences ->
                    _uiState.update {
                        it.copy(
                            dailySummaryEnabled = preferences[NOTIF_DAILY_KEY] ?: true,
                            weeklyRecapEnabled = preferences[NOTIF_WEEKLY_KEY] ?: true,
                            achievementsEnabled = preferences[NOTIF_ACHIEVEMENTS_KEY] ?: true,
                            dailyChallengesEnabled = preferences[NOTIF_CHALLENGES_KEY] ?: true,
                            extendedAudioAnalysisEnabled = preferences[EXTENDED_AUDIO_ANALYSIS_KEY] ?: false,
                        )
                    }
                }
            }

            viewModelScope.launch {
                coverArtSourcePreference.preferredSource.collect { preferred ->
                    _uiState.update { it.copy(preferredCoverSource = preferred) }
                }
            }

            // Analytics consent is observed through AnalyticsConsent rather than by reading the
            // DataStore key directly, so the key stays owned in one place.
            viewModelScope.launch {
                analyticsConsent.isEnabled.collect { enabled ->
                    _uiState.update { it.copy(analyticsEnabled = enabled) }
                }
            }

            // Watch Room preferences for updates (Last.fm state, filters, etc.)
            viewModelScope.launch {
                userPreferencesDao.preferences().collect { roomPrefs ->
                    val prefs =
                        roomPrefs ?: me.avinas.tempo.data.local.entities
                            .UserPreferences()
                    _uiState.update {
                        it.copy(
                            mergeAlternateVersions = prefs.mergeAlternateVersions,
                            filterPodcasts = prefs.filterPodcasts,
                            filterAudiobooks = prefs.filterAudiobooks,
                            spotifyApiOnlyMode = prefs.spotifyApiOnlyMode,
                            isLastFmConnected = prefs.lastfmConnected,
                            lastFmUsername = prefs.lastfmUsername,
                            lastFmSyncFrequency = prefs.lastfmSyncFrequency,
                            isGamificationEnabled = prefs.isGamificationEnabled,
                            hideStreaks = prefs.hideStreaks,
                            pauseTrackingOnLowBattery = prefs.pauseTrackingOnLowBattery,
                        )
                    }
                }
            }
        }

        fun updateUserName(name: String) {
            viewModelScope.launch {
                profileIdentityManager.updateUserName(name)
            }
        }

        fun updateProfileImage(uri: Uri) {
            viewModelScope.launch {
                runCatching {
                    profileIdentityManager.updateProfileImage(uri)
                }.onSuccess {
                    _profileImageMessage.value = "Profile photo updated"
                }.onFailure { error ->
                    _profileImageMessage.value = error.message ?: "Failed to update profile photo"
                }
            }
        }

        fun removeProfileImage() {
            viewModelScope.launch {
                runCatching {
                    profileIdentityManager.clearProfileImage()
                }.onSuccess {
                    _profileImageMessage.value = "Profile photo removed"
                }.onFailure { error ->
                    _profileImageMessage.value = error.message ?: "Failed to remove profile photo"
                }
            }
        }

        fun toggleDailySummary(enabled: Boolean) {
            viewModelScope.launch {
                context.dataStore.edit { it[NOTIF_DAILY_KEY] = enabled }
                if (enabled) {
                    NotificationWorker.scheduleDaily(context)
                } else {
                    NotificationWorker.cancelDaily(context)
                }
            }
        }

        fun toggleWeeklyRecap(enabled: Boolean) {
            viewModelScope.launch {
                context.dataStore.edit { it[NOTIF_WEEKLY_KEY] = enabled }
                if (enabled) {
                    NotificationWorker.scheduleWeekly(context)
                } else {
                    NotificationWorker.cancelWeekly(context)
                }
            }
        }

        fun toggleAchievements(enabled: Boolean) {
            viewModelScope.launch {
                context.dataStore.edit { it[NOTIF_ACHIEVEMENTS_KEY] = enabled }
            }
        }

        fun toggleDailyChallenges(enabled: Boolean) {
            viewModelScope.launch {
                context.dataStore.edit { it[NOTIF_CHALLENGES_KEY] = enabled }
                if (enabled) {
                    // Re-enable: schedule a notification for today at the stored smart hour,
                    // falling back to 9 AM if no smart hour has been computed yet.
                    val storedHour = userPreferencesDao.getSync()?.smartChallengeNotifHour ?: 9
                    NotificationWorker.scheduleChallengeReady(context, storedHour)
                } else {
                    // Disabled: cancel any pending challenge notification
                    NotificationWorker.cancelChallengeReady(context)
                }
            }
        }

        fun setPreferredCoverSource(source: AlbumArtSource?) {
            viewModelScope.launch {
                coverArtSourcePreference.setPreferredSource(source)
                // Requeue rows that still hold art from another automatic provider so the
                // new preference can win. Returning to Automatic needs the same sweep: it
                // hands arbitration back to the fixed priority order, which may well pick
                // a different cover than the one the old preference pinned.
                val dao = database.enrichedMetadataDao()
                if (source != null) {
                    dao.requeueTracksNotMatchingCoverSource(source)
                } else {
                    dao.requeueAllAutomaticArtworkSources()
                }
                // Requeuing only flips rows to PENDING, so the preference does nothing until
                // something drains them. A plain immediate run is not enough: it processes one
                // 10-track batch, while the periodic worker only picks up 10 more per hour —
                // a library-wide preference change would take weeks to apply. The Enrich All
                // sweep is the existing mechanism that drains a backlog to empty.
                val pending = dao.countByStatus(EnrichmentStatus.PENDING)
                if (pending > 0) {
                    EnrichmentWorker.enqueueEnrichAll(context, pending)
                }
            }
        }

        fun toggleExtendedAudioAnalysis(enabled: Boolean) {
            viewModelScope.launch {
                context.dataStore.edit { it[EXTENDED_AUDIO_ANALYSIS_KEY] = enabled }
            }
        }

        /**
         * Turns anonymous app-health reporting on or off.
         *
         * Only the flag is written here. The app-level observer in `TempoApplication`
         * reacts to every change of it — from here, from the Home disclosure card, from a
         * data wipe — by purging anything buffered, cancelling the uploader when off and
         * re-registering it when on, so opting out takes effect immediately everywhere.
         */
        fun setAnalyticsEnabled(enabled: Boolean) {
            viewModelScope.launch {
                analyticsConsent.setEnabled(enabled)
                _uiState.update { it.copy(analyticsEnabled = enabled) }
            }
        }

        /**
         * Replaces the pseudonymous install id and forgets the daily reporting bookkeeping.
         *
         * This is the user-facing half of the promise in `docs/ANALYTICS.md`: everything
         * recorded under the old id becomes unlinkable to anything sent afterwards.
         *
         * No queue purge is needed, and that is a property worth keeping. The install id is
         * attached when a batch is *sent*, not when an event is enqueued, so rotating it
         * unlinks events already sitting in the queue — they simply go out under the new id.
         * Buffered-but-unsent events are only deleted when reporting is turned off.
         */
        fun resetAnalyticsId() {
            viewModelScope.launch {
                runCatching {
                    analyticsInstallId.reset()
                    analyticsDailyLedger.clear()
                    // The new id inherits no history, so feature adoption starts over too.
                    // Leaving the set behind would let an install inherit the previous one's
                    // "already reported" state, which is exactly the link this breaks.
                    analyticsFeatureLedger.clear()
                }
                _uiState.update { it.copy(analyticsIdResetAt = System.currentTimeMillis()) }
            }
        }

        /** Called once the reset has been acknowledged, so the confirmation does not repeat. */
        fun clearAnalyticsIdReset() {
            _uiState.update { it.copy(analyticsIdResetAt = null) }
        }

        fun toggleGamificationEnabled(enabled: Boolean) {
            viewModelScope.launch {
                val currentPrefs =
                    userPreferencesDao.getSync() ?: me.avinas.tempo.data.local.entities
                        .UserPreferences()
                userPreferencesDao.upsert(currentPrefs.copy(isGamificationEnabled = enabled))
                _uiState.update { it.copy(isGamificationEnabled = enabled) }
                if (!enabled) {
                    // If gamification is completely disabled, cancel workers that compute XP/challenges
                    NotificationWorker.cancelChallengeReady(context)
                    ChallengeWorker.cancelDaily(context)
                    GamificationWorker.cancelPeriodicRefresh(context)
                } else {
                    // Re-enable workers
                    ChallengeWorker.scheduleDaily(context)
                    GamificationWorker.enqueuePeriodicRefresh(context)
                }
            }
        }

        /**
         * Turns the display of listening streaks on or off.
         *
         * Only the preference is written here. Every streak surface reads it, so the change
         * applies on the next recomposition without clearing caches or recomputing statistics.
         */
        fun toggleHideStreaks(enabled: Boolean) {
            viewModelScope.launch {
                val currentPrefs =
                    userPreferencesDao.getSync() ?: me.avinas.tempo.data.local.entities
                        .UserPreferences()
                userPreferencesDao.upsert(currentPrefs.copy(hideStreaks = enabled))
                _uiState.update { it.copy(hideStreaks = enabled) }
            }
        }

        fun toggleMergeAlternateVersions(enabled: Boolean) {
            viewModelScope.launch {
                val currentPrefs =
                    userPreferencesDao.getSync() ?: me.avinas.tempo.data.local.entities
                        .UserPreferences()
                userPreferencesDao.upsert(currentPrefs.copy(mergeAlternateVersions = enabled))
                _uiState.update { it.copy(mergeAlternateVersions = enabled) }
            }
        }

        fun toggleFilterPodcasts(enabled: Boolean) {
            viewModelScope.launch {
                val currentPrefs =
                    userPreferencesDao.getSync() ?: me.avinas.tempo.data.local.entities
                        .UserPreferences()
                userPreferencesDao.upsert(currentPrefs.copy(filterPodcasts = enabled))
                _uiState.update { it.copy(filterPodcasts = enabled) }
            }
        }

        fun toggleFilterAudiobooks(enabled: Boolean) {
            viewModelScope.launch {
                val currentPrefs =
                    userPreferencesDao.getSync() ?: me.avinas.tempo.data.local.entities
                        .UserPreferences()
                userPreferencesDao.upsert(currentPrefs.copy(filterAudiobooks = enabled))
                _uiState.update { it.copy(filterAudiobooks = enabled) }
            }
        }

        /**
         * Toggle whether music tracking pauses automatically when battery drops ≤ 20%.
         * Persists to Room so the setting survives app restarts.
         */
        fun togglePauseTrackingOnLowBattery(enabled: Boolean) {
            viewModelScope.launch {
                val currentPrefs =
                    userPreferencesDao.getSync() ?: me.avinas.tempo.data.local.entities
                        .UserPreferences()
                userPreferencesDao.upsert(currentPrefs.copy(pauseTrackingOnLowBattery = enabled))
                _uiState.update { it.copy(pauseTrackingOnLowBattery = enabled) }
            }
        }

        /**
         * Owns Settings' platform/runtime observations. The composable only supplies lifecycle
         * visibility; permission, heartbeat, battery, OEM and Spotify reads stay below the UI.
         */
        suspend fun observeRuntimeStatus(section: SettingsSection?) {
            when (section) {
                SettingsSection.TRACKING -> {
                    while (true) {
                        refreshTrackingRuntimeStatus()
                        delay(RUNTIME_STATUS_REFRESH_MS)
                    }
                }
                SettingsSection.CONNECTIONS -> {
                    while (true) {
                        refreshConnectionsRuntimeStatus()
                        delay(RUNTIME_STATUS_REFRESH_MS)
                    }
                }
                else -> Unit
            }
        }

        private suspend fun refreshTrackingRuntimeStatus() {
            val snapshot =
                withContext(Dispatchers.IO) {
                    coroutineScope {
                        val listener = async {
                            NotificationManagerCompat.getEnabledListenerPackages(context)
                                .contains(context.packageName)
                        }
                        val heartbeat = async { TrackingServiceHeartbeat.snapshot(context) }
                        val battery = async { BatteryUtils.getBatteryLevel(context) }
                        val autostart =
                            if (_runtimeStatus.value.isXiaomiDevice) {
                                async { OemBackgroundHelper.getAutostartState(context) }
                            } else {
                                null
                            }

                        TrackingRuntimeSnapshot(
                            notificationListenerGranted = listener.await(),
                            trackingHeartbeat = heartbeat.await(),
                            batteryLevel = battery.await(),
                            autostartState =
                                autostart?.await()
                                    ?: OemBackgroundHelper.AutostartState.UNKNOWN,
                        )
                    }
                }

            _runtimeStatus.update {
                it.copy(
                    notificationListenerGranted = snapshot.notificationListenerGranted,
                    trackingHeartbeat = snapshot.trackingHeartbeat,
                    trackingStatusCheckedAt = System.currentTimeMillis(),
                    batteryLevel = snapshot.batteryLevel,
                    autostartState = snapshot.autostartState,
                )
            }
        }

        private suspend fun refreshConnectionsRuntimeStatus() {
            val previous = _runtimeStatus.value
            val snapshot =
                withContext(Dispatchers.IO) {
                    coroutineScope {
                        val account = async {
                            val auth = spotifyAuthManager.get()
                            auth.isConnected() to spotifyTokenStorage.get().getCustomClientId().orEmpty()
                        }
                        val battery = async { BatteryUtils.getBatteryLevel(context) }
                        val (connected, clientId) = account.await()
                        ConnectionsRuntimeSnapshot(
                            spotifyConnected = connected,
                            spotifyClientId = clientId,
                            spotifyBroadcastEnabled = SpotifyBroadcastPresence.enabled(context),
                            batteryLevel = battery.await(),
                        )
                    }
                }

            val newlyConnected =
                previous.spotifyAccountLoaded &&
                    snapshot.spotifyConnected &&
                    !previous.spotifyConnected
            _runtimeStatus.update { current ->
                current.copy(
                    spotifyConnected = snapshot.spotifyConnected,
                    spotifyAccountLoaded = true,
                    spotifyClientId =
                        if (!spotifyClientIdDraftDirty) snapshot.spotifyClientId
                        else current.spotifyClientId,
                    spotifyBroadcastEnabled = snapshot.spotifyBroadcastEnabled,
                    batteryLevel = snapshot.batteryLevel,
                )
            }
            if (newlyConnected && _uiState.value.spotifyApiOnlyMode) {
                SpotifyPollingWorker.syncNow(context)
            }
        }

        fun updateSpotifyClientIdDraft(value: String) {
            spotifyClientIdDraftDirty = true
            _runtimeStatus.update { it.copy(spotifyClientId = value) }
        }

        fun updateSpotifyConnectionUi(connected: Boolean) {
            if (!connected) spotifyClientIdDraftDirty = false
            _runtimeStatus.update { it.copy(spotifyConnected = connected, spotifyAccountLoaded = true) }
        }

        fun updateSpotifyBroadcastUi(enabled: Boolean) {
            _runtimeStatus.update { it.copy(spotifyBroadcastEnabled = enabled) }
        }

        suspend fun startSpotifyAuthorization(clientId: String): Intent? = withContext(Dispatchers.IO) {
            val auth = spotifyAuthManager.get()
            val value = clientId.trim()
            if (auth.isConnected()) return@withContext null
            if (value.isNotEmpty() && !value.matches(Regex("[0-9a-fA-F]{32}"))) {
                return@withContext null
            }
            auth.setClientId(value)
            if (auth.isConfigured()) auth.startLoginFlow() else null
        }

        suspend fun disconnectSpotify(): Unit = withContext(Dispatchers.IO) {
            spotifyAuthManager.get().disconnect()
            SpotifyPollingWorker.cancel(context)
            val prefs = userPreferencesDao.getSync() ?: me.avinas.tempo.data.local.entities.UserPreferences()
            userPreferencesDao.upsert(prefs.copy(spotifyApiOnlyMode = false))
            _uiState.update { it.copy(spotifyApiOnlyMode = false) }
        }

        /**
         * Toggle Spotify-API-Only mode.
         * When enabled, Spotify listening data is fetched from the API instead of notifications.
         */
        fun toggleSpotifyApiOnlyMode(enabled: Boolean) {
            viewModelScope.launch {
                val currentPrefs =
                    userPreferencesDao.getSync() ?: me.avinas.tempo.data.local.entities
                        .UserPreferences()
                userPreferencesDao.upsert(currentPrefs.copy(spotifyApiOnlyMode = enabled))
                _uiState.update { it.copy(spotifyApiOnlyMode = enabled) }

                // Schedule or cancel the polling worker based on mode
                if (enabled) {
                    SpotifyPollingWorker.schedule(context)
                    SpotifyPollingWorker.syncNow(context)
                } else {
                    SpotifyPollingWorker.cancel(context)
                }
            }
        }

        private data class TrackingRuntimeSnapshot(
            val notificationListenerGranted: Boolean,
            val trackingHeartbeat: TrackingServiceHeartbeat.Snapshot?,
            val batteryLevel: Int,
            val autostartState: OemBackgroundHelper.AutostartState,
        )

        private data class ConnectionsRuntimeSnapshot(
            val spotifyConnected: Boolean,
            val spotifyClientId: String,
            val spotifyBroadcastEnabled: Boolean,
            val batteryLevel: Int,
        )

        private companion object {
            const val RUNTIME_STATUS_REFRESH_MS = 30_000L
        }

        val resetState = appDataResetter.state

        fun clearAllData() {
            viewModelScope.launch(Dispatchers.IO) {
                // Android stops all app code and owns the deletion. Do not navigate to a
                // new onboarding screen while old services/imports can still write.
                appDataResetter.requestReset()
            }
        }

        /**
         * Export all data to a ZIP file.
         */
        fun exportData(uri: Uri) {
            viewModelScope.launch {
                val result = importExportManager.exportData(uri)
                _importExportResult.value = result
            }
        }

        /**
         * Start import process - shows conflict resolution dialog only if
         * existing data could conflict. On a fresh install, imports directly.
         */
        fun startImport(uri: Uri) {
            viewModelScope.launch {
                val hasExistingData =
                    database.trackDao().getCount() > 0 ||
                        database.artistDao().getCount() > 0 ||
                        database.listeningEventDao().getCount() > 0
                if (hasExistingData) {
                    _showConflictDialog.value = uri
                } else {
                    importData(uri, ImportConflictStrategy.REPLACE)
                }
            }
        }

        /**
         * Proceed with import after user selects conflict strategy.
         */
        fun importData(
            uri: Uri,
            strategy: ImportConflictStrategy,
        ) {
            _showConflictDialog.value = null
            viewModelScope.launch {
                val result = importExportManager.importData(uri, strategy)
                _importExportResult.value = result
            }
        }

        /**
         * Cancel import.
         */
        fun cancelImport() {
            _showConflictDialog.value = null
        }

        /**
         * Clear the import/export result after showing to user.
         */
        fun clearImportExportResult() {
            _importExportResult.value = null
        }

        fun clearProfileImageMessage() {
            _profileImageMessage.value = null
        }

        fun scanHistoryQuality() {
            viewModelScope.launch {
                _historyQuality.value = HistoryQualityUiState(isLoading = true)
                try {
                    val dao = database.listeningEventDao()
                    val api = withContext(Dispatchers.IO) { dao.countUnverifiableSpotifyApiPlays() }
                    val reconstructed = withContext(Dispatchers.IO) { dao.countReconstructedHistoryPlays() }
                    val exactDuplicates = withContext(Dispatchers.IO) { dao.countExactFingerprintDuplicates() }
                    _historyQuality.value = HistoryQualityUiState(
                        apiEstimatedCount = api,
                        reconstructedCount = reconstructed,
                        exactDuplicateCount = exactDuplicates,
                    )
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _historyQuality.value = HistoryQualityUiState(error = e.message ?: "Unable to scan history")
                }
            }
        }

        /**
         * Only call from the explicit backed-up + confirmed Settings dialog.
         * Do not delete any songs or events: only nullify invented API
         * listening minutes/completion and invalidate saved-stat aggregates.
         */
        fun repairHistoryAfterBackupConfirmation() {
            viewModelScope.launch {
                _historyQuality.value = _historyQuality.value.copy(isLoading = true)
                try {
                    val updated = withContext(Dispatchers.IO) {
                        database.listeningEventDao().clearUnverifiableSpotifyApiEstimates()
                    }
                    statsRepository.invalidateCache()
                    refreshCoordinator.notifyNewTrackRecorded()
                    val reconstructed = withContext(Dispatchers.IO) {
                        database.listeningEventDao().countReconstructedHistoryPlays()
                    }
                    _historyQuality.value = HistoryQualityUiState(
                        apiEstimatedCount = 0,
                        reconstructedCount = reconstructed,
                        exactDuplicateCount = database.listeningEventDao().countExactFingerprintDuplicates(),
                        repairedCount = updated,
                    )
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _historyQuality.value = _historyQuality.value.copy(
                        isLoading = false, error = e.message ?: "Unable to repair history"
                    )
                }
            }
        }
        /** Delete only proven, same-fingerprint duplicate event rows after the
         * user has exported a backup and explicitly requested this operation.
         * Older rows without fingerprints are intentionally left untouched.
         */
        fun repairExactDuplicatesAfterBackupConfirmation() {
            viewModelScope.launch {
                _historyQuality.value = _historyQuality.value.copy(isLoading = true)
                try {
                    val removed = withContext(Dispatchers.IO) {
                        database.listeningEventDao().deleteExactFingerprintDuplicates()
                    }
                    statsRepository.invalidateCache()
                    refreshCoordinator.notifyNewTrackRecorded()
                    val dao = database.listeningEventDao()
                    _historyQuality.value = HistoryQualityUiState(
                        apiEstimatedCount = withContext(Dispatchers.IO) { dao.countUnverifiableSpotifyApiPlays() },
                        reconstructedCount = withContext(Dispatchers.IO) { dao.countReconstructedHistoryPlays() },
                        exactDuplicateCount = 0,
                        repairedDuplicatesCount = removed,
                    )
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    _historyQuality.value = _historyQuality.value.copy(
                        isLoading = false, error = e.message ?: "Unable to repair duplicate history"
                    )
                }
            }
        }
    }

@Immutable
data class HistoryQualityUiState(
    val isLoading: Boolean = false,
    val apiEstimatedCount: Int = 0,
    val reconstructedCount: Int = 0,
    val exactDuplicateCount: Int = 0,
    val repairedCount: Int? = null,
    val repairedDuplicatesCount: Int? = null,
    val error: String? = null,
)

@Immutable
@Immutable
data class SettingsRuntimeStatus(
    val notificationListenerGranted: Boolean = false,
    val trackingHeartbeat: TrackingServiceHeartbeat.Snapshot? = null,
    val trackingStatusCheckedAt: Long = 0L,
    val batteryLevel: Int = 100,
    val isXiaomiDevice: Boolean = false,
    val autostartState: OemBackgroundHelper.AutostartState = OemBackgroundHelper.AutostartState.UNKNOWN,
    val spotifyConnected: Boolean = false,
    val spotifyAccountLoaded: Boolean = false,
    val spotifyClientId: String = "",
    val spotifyBroadcastEnabled: Boolean = false,
) {
    val isBatteryCritical: Boolean get() = batteryLevel <= BatteryUtils.CRITICAL_BATTERY_LEVEL
    val isLowBattery: Boolean get() = batteryLevel <= BatteryUtils.LOW_BATTERY_LEVEL
}

data class SettingsUiState(
    val dailySummaryEnabled: Boolean = true,
    val weeklyRecapEnabled: Boolean = true,
    val achievementsEnabled: Boolean = true,
    val dailyChallengesEnabled: Boolean = true,
    val extendedAudioAnalysisEnabled: Boolean = false,
    val preferredCoverSource: AlbumArtSource? = null,
    val isSpotifyConnected: Boolean = false,
    val spotifyUsername: String? = null,
    val userName: String = "User",
    val profileImagePath: String? = null,
    val mergeAlternateVersions: Boolean = true,
    val filterPodcasts: Boolean = true,
    val filterAudiobooks: Boolean = true,
    val spotifyApiOnlyMode: Boolean = false,
    // Last.fm connection state
    val isLastFmConnected: Boolean = false,
    val lastFmUsername: String? = null,
    val lastFmSyncFrequency: String = "NONE",
    // Gamification state
    val isGamificationEnabled: Boolean = true,
    // Listening streaks hidden from the interface at the user's request
    val hideStreaks: Boolean = false,
    // Battery saver tracking pause
    val pauseTrackingOnLowBattery: Boolean = true,
    // Anonymous app-health reporting. `analyticsConfigured` is a build constant: builds
    // without an analytics endpoint (e.g. compiled from source) never show the controls,
    // because offering a switch for collection that cannot happen would be misleading.
    val analyticsEnabled: Boolean = AnalyticsDefaults.ENABLED,
    val analyticsConfigured: Boolean = false,
    /**
     * Set when the user resets their analytics id, purely so the UI can confirm it happened.
     * Never sent anywhere — a local acknowledgement, not an event.
     */
    val analyticsIdResetAt: Long? = null,
)
