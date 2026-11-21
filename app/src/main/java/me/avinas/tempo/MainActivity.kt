package me.avinas.tempo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.animation.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import me.avinas.tempo.data.analytics.AccessResult
import me.avinas.tempo.data.analytics.ExemptionResult
import me.avinas.tempo.data.analytics.OnboardingAction
import me.avinas.tempo.data.analytics.OnboardingStepName
import me.avinas.tempo.ui.navigation.AppNavigation
import me.avinas.tempo.ui.navigation.StreakVisibilityViewModel
import me.avinas.tempo.ui.components.LocalHideStreaks
import me.avinas.tempo.ui.onboarding.BatteryOptimizationScreen
import me.avinas.tempo.ui.onboarding.HowItWorksScreen
import me.avinas.tempo.ui.onboarding.isBatteryOptimizationDisabled
import me.avinas.tempo.ui.permissions.isNotificationListenerEnabled
import me.avinas.tempo.ui.onboarding.OnboardingViewModel
import me.avinas.tempo.ui.onboarding.PrivacyExplainerScreen
import me.avinas.tempo.ui.onboarding.WelcomeScreen
import me.avinas.tempo.ui.permissions.PermissionScreen
import me.avinas.tempo.ui.theme.TempoTheme
import me.avinas.tempo.utils.OemBackgroundHelper
import me.avinas.tempo.worker.ServiceHealthWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import me.avinas.tempo.data.remote.spotify.SpotifyAuthManager
import me.avinas.tempo.data.local.dao.UserPreferencesDao
import me.avinas.tempo.worker.SpotifyPollingWorker

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    @javax.inject.Inject
    lateinit var walkthroughController: me.avinas.tempo.ui.components.WalkthroughController

    @javax.inject.Inject
    lateinit var analyticsDailyReporter: me.avinas.tempo.data.analytics.AnalyticsDailyReporter
    
    @javax.inject.Inject
    lateinit var spotifyAuthManager: SpotifyAuthManager

    @javax.inject.Inject
    lateinit var userPreferencesDao: UserPreferencesDao

    @javax.inject.Inject
    lateinit var appearancePreferences: me.avinas.tempo.ui.appearance.AppearancePreferencesRepository

    private val navigationTrigger = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Let Android remove its platform splash as soon as the app draws.
        // Do not scale the icon or impose an additional exit animation: it
        // delayed the first usable screen and could flash a mismatched surface.

        checkNavigationIntent(intent)

        // Repair the periodic Spotify sync after upgrades, process death or a
        // reboot. Only an authorized account with API sync enabled is scheduled.
        // WorkManager's unique job avoids duplicate periodic timers.
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (spotifyAuthManager.isConnected() &&
                    userPreferencesDao.getSync()?.spotifyApiOnlyMode == true
                ) {
                    // WorkManager UPDATE changes an existing 15-minute job to
                    // the new 20-minute interval, but only if schedule() runs.
                    // Store the schedule generation so an app upgrade updates
                    // existing installs exactly once without restarting the
                    // periodic timer on every Activity recreation.
                    val syncPrefs = getSharedPreferences("spotify_sync_cadence", MODE_PRIVATE)
                    if (syncPrefs.getInt("schedule_generation", 0) < 2 ||
                        !SpotifyPollingWorker.isScheduled(applicationContext)
                    ) {
                        SpotifyPollingWorker.schedule(applicationContext)
                        syncPrefs.edit().putInt("schedule_generation", 2).apply()
                    }
                    // Request an extra reconciliation only after a long gap.
                    // Device broadcasts and MediaSession remain real-time.
                    val now = System.currentTimeMillis()
                    if (now - syncPrefs.getLong("last_requested_at", 0L) > 20 * 60_000L) {
                        syncPrefs.edit().putLong("last_requested_at", now).apply()
                        SpotifyPollingWorker.syncNow(applicationContext)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("TempoSpotifySync", "Unable to restore scheduled sync", e)
            }
        }

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            // The initial value preserves the previous dark Abyss appearance
            // until DataStore finishes loading the user's chosen theme.
            val appearance by appearancePreferences.settings.collectAsStateWithLifecycle(
                initialValue = me.avinas.tempo.ui.appearance.AppearanceSettings()
            )
            TempoTheme(appearance = appearance) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Streak display is one app-wide switch. Providing it here means a screen, a
                    // dialog or a share canvas never has to load preferences just to know whether
                    // streaks belong on it — see LocalHideStreaks.
                    val streakVisibility: StreakVisibilityViewModel = hiltViewModel()
                    val hideStreaks by streakVisibility.hideStreaks.collectAsStateWithLifecycle()
                    CompositionLocalProvider(LocalHideStreaks provides hideStreaks) {
                        TempoApp(
                            walkthroughController = walkthroughController,
                            onSetupComplete = {
                                // Schedule the health worker after setup is complete
                                ServiceHealthWorker.schedule(this)
                            },
                            navigationTrigger = navigationTrigger.value,
                            onWidgetNavigationHandled = { navigationTrigger.value = null }
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Records that the user brought Tempo's UI to the foreground today.
     *
     * Deliberately `onResume` and not `onCreate`. The tracking service is a foreground service,
     * so this process outlives the activity for hours or days — `onCreate` therefore fires once
     * per *process*, not once per app open. A user who reopens Tempo from Recents the next
     * morning, with the process still alive, would never reach `onCreate` and would be missing
     * from that day's active users. That is an undercount in the one figure this exists to
     * produce, and it would have looked like a plausible dip rather than a bug.
     *
     * Repeated calls are free: after the first of the day, the reporter is one atomic
     * comparison. Fire-and-forget, so a reporting write never sits in front of drawing a frame.
     */
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            analyticsDailyReporter.markUiOpened()
            analyticsDailyReporter.reportPresenceIfDue()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        checkNavigationIntent(intent)
    }
    
    private fun checkNavigationIntent(intent: Intent?) {
        if (intent?.hasExtra("navigate_to") == true) {
            val dest = intent.getStringExtra("navigate_to")
            if (dest != null) {
                navigationTrigger.value = dest
                intent.removeExtra("navigate_to")
            }
        }
    }
}

enum class OnboardingStep {
    WELCOME, HOW_IT_WORKS, PRIVACY, PERMISSION, BATTERY, RESTORE, COMPLETED
}

/**
 * The onboarding step machine has no analytics equivalent for COMPLETED — it is the app, not
 * a setup screen — so that step reports nothing.
 */
private fun OnboardingStep.toAnalyticsStep(): OnboardingStepName? = when (this) {
    OnboardingStep.WELCOME -> OnboardingStepName.WELCOME
    OnboardingStep.HOW_IT_WORKS -> OnboardingStepName.HOW_IT_WORKS
    OnboardingStep.PRIVACY -> OnboardingStepName.PRIVACY
    OnboardingStep.PERMISSION -> OnboardingStepName.PERMISSION
    OnboardingStep.BATTERY -> OnboardingStepName.BATTERY
    OnboardingStep.RESTORE -> OnboardingStepName.RESTORE
    OnboardingStep.COMPLETED -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TempoApp(
    walkthroughController: me.avinas.tempo.ui.components.WalkthroughController,
    onSetupComplete: () -> Unit,
    navigationTrigger: String? = null,
    onWidgetNavigationHandled: () -> Unit = {},
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Wait for onboarding status to be loaded before deciding the initial step
    val initialStep = remember(uiState.isLoading, uiState.isOnboardingCompleted) {
        when {
            uiState.isLoading -> null
            uiState.isOnboardingCompleted -> OnboardingStep.COMPLETED
            else -> OnboardingStep.WELCOME
        }
    }
    
    var currentStep by remember(initialStep) { 
        mutableStateOf(initialStep ?: OnboardingStep.WELCOME) 
    }

    // Onboarding funnel reporting state. The step machine lives in composition rather than in
    // the ViewModel, so the timing state has to live here too — the ViewModel only receives
    // the finished measurements.
    var stepStartedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var onboardingStartedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var skippedSteps by remember { mutableIntStateOf(0) }

    /**
     * The single place a step transition happens. Routing every move through here is what
     * makes the funnel complete: a step cannot be reached without also reporting how it was
     * left and how long it was shown.
     */
    fun goTo(step: OnboardingStep, action: OnboardingAction) {
        val leaving = currentStep
        if (action == OnboardingAction.SKIP) skippedSteps++

        leaving.toAnalyticsStep()?.let { stepName ->
            viewModel.onStepLeft(
                step = stepName,
                action = action,
                stepMillis = System.currentTimeMillis() - stepStartedAt
            )
        }

        stepStartedAt = System.currentTimeMillis()
        currentStep = step
    }

    // If onboarding is already completed in DataStore, jump to COMPLETED
    LaunchedEffect(uiState.isOnboardingCompleted) {
        if (uiState.isOnboardingCompleted) {
            currentStep = OnboardingStep.COMPLETED
            onSetupComplete()
        }
    }
    
    // Show nothing while loading to prevent welcome screen flash
    if (uiState.isLoading) {
        return
    }

    // Xiaomi guidance popup state
    val isXiaomiDevice = remember { OemBackgroundHelper.isXiaomiDevice() }
    var showXiaomiGuidance by remember { mutableStateOf(false) }
    var xiaomiGuidanceDismissed by remember { mutableStateOf(false) }
    var localNavigationTrigger by remember { mutableStateOf<String?>(null) }
    // ponytail: one drift language app-wide — 8% width (~30px) + fade, enter
    // slower than exit; full-width pager slide fought every screen's entrance.
    val reducedMotion = me.avinas.tempo.ui.theme.rememberReducedMotion()
    val effectsSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.IntOffset>()

    // Show Xiaomi guidance popup after onboarding completes for first-time Xiaomi users
    LaunchedEffect(uiState.isOnboardingCompleted, uiState.xiaomiGuidanceShown) {
        if (uiState.isOnboardingCompleted && isXiaomiDevice && !uiState.xiaomiGuidanceShown && !xiaomiGuidanceDismissed) {
            showXiaomiGuidance = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                // The first usable Home frame must not be held behind a
                // second splash or cross-fade. Onboarding pages remain
                // spatially related and use the same Material physics.
                when {
                    reducedMotion ||
                        targetState == OnboardingStep.COMPLETED ||
                        initialState == OnboardingStep.COMPLETED ->
                        EnterTransition.None togetherWith ExitTransition.None
                    targetState.ordinal > initialState.ordinal ->
                        (slideInHorizontally(animationSpec = spatialSpec) { it / 12 } +
                            fadeIn(animationSpec = effectsSpec)) togetherWith
                            (slideOutHorizontally(animationSpec = spatialSpec) { -it / 14 } +
                                fadeOut(animationSpec = effectsSpec))
                    else ->
                        (slideInHorizontally(animationSpec = spatialSpec) { -it / 12 } +
                            fadeIn(animationSpec = effectsSpec)) togetherWith
                            (slideOutHorizontally(animationSpec = spatialSpec) { it / 14 } +
                                fadeOut(animationSpec = effectsSpec))
                }
            },
            label = "onboarding_step_transition"
        ) { step ->
            when (step) {
                OnboardingStep.WELCOME -> {
                    WelcomeScreen(
                        onGetStarted = { goTo(OnboardingStep.HOW_IT_WORKS, OnboardingAction.NEXT) },
                        onSkip = {
                            // Skip educational intro directly to permissions, never bypass tracking
                            goTo(OnboardingStep.PERMISSION, OnboardingAction.SKIP)
                        }
                    )
                }
                OnboardingStep.HOW_IT_WORKS -> {
                    HowItWorksScreen(
                        onNext = { goTo(OnboardingStep.PRIVACY, OnboardingAction.NEXT) },
                        onSkip = {
                            goTo(OnboardingStep.PERMISSION, OnboardingAction.SKIP)
                        }
                    )
                }
                OnboardingStep.PRIVACY -> {
                    PrivacyExplainerScreen(
                        onNext = { goTo(OnboardingStep.PERMISSION, OnboardingAction.NEXT) },
                        onSkip = {
                            goTo(OnboardingStep.PERMISSION, OnboardingAction.SKIP)
                        }
                    )
                }
                OnboardingStep.PERMISSION -> {
                    PermissionScreen(
                        onPermissionGranted = {
                            viewModel.onNotifAccess(AccessResult.GRANTED)
                            goTo(OnboardingStep.BATTERY, OnboardingAction.NEXT)
                        },
                        onSkip = {
                            // "Do it later" is not a refusal — reporting it as DENIED would
                            // make the permission look rejected when it was postponed.
                            viewModel.onNotifAccess(AccessResult.DEFERRED)
                            goTo(OnboardingStep.BATTERY, OnboardingAction.SKIP)
                        }
                    )
                }
                OnboardingStep.BATTERY -> {
                    BatteryOptimizationScreen(
                        onOptimize = {
                            viewModel.onBatteryExemption(ExemptionResult.GRANTED)
                            goTo(OnboardingStep.RESTORE, OnboardingAction.NEXT)
                        },
                        onSkip = {
                            viewModel.onBatteryExemption(ExemptionResult.SKIPPED)
                            goTo(OnboardingStep.RESTORE, OnboardingAction.SKIP)
                        },
                        onBack = {
                            // ponytail: PERMISSION auto-forwards when granted — skip it then
                            goTo(
                                if (!isNotificationListenerEnabled(context)) {
                                    OnboardingStep.PERMISSION
                                } else {
                                    OnboardingStep.PRIVACY
                                },
                                OnboardingAction.BACK
                            )
                        }
                    )
                }
                OnboardingStep.RESTORE -> {
                    me.avinas.tempo.ui.onboarding.RestoreScreen(
                        onFinish = {
                            viewModel.completeOnboarding()
                            viewModel.onOnboardingFinished(
                                skippedCount = skippedSteps,
                                totalMillis = System.currentTimeMillis() - onboardingStartedAt
                            )
                            goTo(OnboardingStep.COMPLETED, OnboardingAction.NEXT)
                        },
                        onBack = {
                            // ponytail: BATTERY/PERMISSION auto-forward when already
                            // provisioned — land on the nearest step that stays put
                            goTo(
                                when {
                                    !isBatteryOptimizationDisabled(context) -> OnboardingStep.BATTERY
                                    !isNotificationListenerEnabled(context) -> OnboardingStep.PERMISSION
                                    else -> OnboardingStep.PRIVACY
                                },
                                OnboardingAction.BACK
                            )
                        }
                    )
                }
                OnboardingStep.COMPLETED -> {
                    AppNavigation(
                        walkthroughController = walkthroughController,
                        navigationTrigger = localNavigationTrigger ?: navigationTrigger,
                        onWidgetNavigationHandled = onWidgetNavigationHandled
                    )
                    // Clear local trigger after passing it
                    LaunchedEffect(localNavigationTrigger) {
                        if (localNavigationTrigger != null) {
                            localNavigationTrigger = null
                        }
                    }
                }
            }
        }

        // Xiaomi first-time guidance popup
        if (showXiaomiGuidance) {
            me.avinas.tempo.ui.components.XiaomiGuidancePopup(
                onDismiss = {
                    showXiaomiGuidance = false
                    xiaomiGuidanceDismissed = true
                    viewModel.markXiaomiGuidanceShown()
                },
                onConfigure = {
                    showXiaomiGuidance = false
                    xiaomiGuidanceDismissed = true
                    viewModel.markXiaomiGuidanceShown()
                    localNavigationTrigger = "background_protection"
                }
            )
        }
    }
}
