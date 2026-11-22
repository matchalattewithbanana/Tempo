@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.avinas.tempo.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import me.avinas.tempo.ui.components.DeepOceanBackground
import me.avinas.tempo.ui.desktop.DesktopLinkScreen
import me.avinas.tempo.ui.home.HomeScreen
import me.avinas.tempo.ui.lastfm.LastFmImportScreen
import me.avinas.tempo.ui.settings.BackgroundProtectionScreen
import me.avinas.tempo.ui.settings.BackupRestoreScreen
import me.avinas.tempo.ui.settings.EnrichmentReportScreen
import me.avinas.tempo.ui.settings.ExtensionImportScreen
import me.avinas.tempo.ui.settings.SettingsScreen
import me.avinas.tempo.ui.settings.SettingsDestination
import me.avinas.tempo.ui.appearance.AppearanceScreen
import me.avinas.tempo.ui.branding.LauncherLogoPickerScreen
import me.avinas.tempo.ui.updates.UpdateCenterScreen
import me.avinas.tempo.ui.settings.SupportedAppsScreen
import me.avinas.tempo.ui.settings.YourDataScreen
import me.avinas.tempo.ui.spotlight.SpotlightScreen
import me.avinas.tempo.ui.theme.rememberReducedMotion
import me.avinas.tempo.ui.components.TempoPrimaryNavigation
import me.avinas.tempo.ui.components.tempoAdaptiveLayout

/** Main destinations use an expressive rail only when expanded-width space
 * is available; detail routes continue to use their own full-screen layouts. */
internal fun shouldUseWideTempoRail(widthDp: Float, primaryDestination: Boolean): Boolean =
    primaryDestination && tempoAdaptiveLayout(widthDp).primaryNavigation != TempoPrimaryNavigation.BOTTOM_BAR

internal fun shouldExpandWideTempoRail(widthDp: Float, primaryDestination: Boolean): Boolean =
    primaryDestination && tempoAdaptiveLayout(widthDp).primaryNavigation == TempoPrimaryNavigation.EXPANDED_RAIL

private const val WidgetPeriodKey = "widgetStatsPeriod"

/**
 * Safe navigate that prevents double-click / rapid-tap navigation.
 * Only navigates if the current back stack entry is in RESUMED state
 * (i.e., not already navigating away).
 */
private fun NavHostController.safeNavigate(route: String) {
    if (currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true) {
        navigate(route)
    }
}

private fun NavHostController.navigateSettingsDestination(destination: SettingsDestination) {
    when (destination) {
        SettingsDestination.BACKUP -> navigate(Screen.BackupRestore.route)
        SettingsDestination.SUPPORTED_APPS -> navigate(Screen.SupportedApps.route)
        SettingsDestination.BACKGROUND_PROTECTION -> navigate(Screen.BackgroundProtection.route)
        SettingsDestination.LASTFM_IMPORT -> navigate(Screen.LastFmImport.route)
        SettingsDestination.SPOTIFY_JSON_IMPORT -> navigate(Screen.SpotifyJsonImport.route)
        SettingsDestination.DEEZER_IMPORT -> navigate(Screen.DeezerImport.route)
        SettingsDestination.APPLE_MUSIC_IMPORT -> navigate(Screen.AppleMusicImport.route)
        SettingsDestination.YOUTUBE_MUSIC_IMPORT -> navigate(Screen.YouTubeMusicImport.route)
        SettingsDestination.MUSICOLET_IMPORT -> navigate(Screen.MusicoletImport.route)
        SettingsDestination.EXTENSION_IMPORT -> navigate(Screen.ExtensionImport.route)
        SettingsDestination.DESKTOP -> navigate(Screen.DesktopLink.route)
        SettingsDestination.ENRICHMENT_REPORT -> navigate(Screen.EnrichmentReport.route)
        SettingsDestination.YOUR_DATA -> navigate(Screen.YourData.route)
        SettingsDestination.APPEARANCE -> navigate(Screen.Appearance.route)
        SettingsDestination.LOGO_PICKER -> safeNavigate(Screen.LauncherLogoPicker.route)
        SettingsDestination.UPDATES -> safeNavigate(Screen.UpdateCenter.route)
    }
}

@Composable
fun AppNavigation(
    walkthroughController: me.avinas.tempo.ui.components.WalkthroughController,
    navigationTrigger: String? = null,
    onWidgetNavigationHandled: () -> Unit = {},
) {
    val navController = rememberNavController()
    val reducedMotion = rememberReducedMotion()
    val animateNavigation = !reducedMotion && !me.avinas.tempo.ui.theme.LocalTempoAppearance.current.instantNavigation
    // M3 top-level navigation uses a quick fade-through (never a horizontal
    // slide, which wrongly suggests the destinations can be swiped). These
    // concise timing choices are Tempo's own, not official M3 timing tokens.
    // Detail routes still use theme-controlled spatial/effects springs.
    val topLevelEnter = fadeIn(animationSpec = tween(durationMillis = 125, delayMillis = 75))
    val topLevelExit = fadeOut(animationSpec = tween(durationMillis = 75))
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val effectsSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    val primaryRoutes = remember {
        setOf(Screen.Home.route, Screen.Stats.route, Screen.History.route)
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentDestination = navBackStackEntry?.destination
    var bottomBarHeightPx by remember { mutableIntStateOf(0) }
    var railWidthPx by remember { mutableIntStateOf(0) }
    var wideNowPlayingHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val bottomBarHeightDp = with(density) { bottomBarHeightPx.toDp() }
    // The 96dp initial inset is Tempo's pre-measure fallback, not an M3 rail token.
    val measuredRailWidthDp = with(density) {
        if (railWidthPx > 0) railWidthPx.toDp() else 96.dp
    }
    val wideNowPlayingHeightDp = with(density) { wideNowPlayingHeightPx.toDp() }

    androidx.compose.runtime.LaunchedEffect(navigationTrigger) {
        val widgetPeriod = me.avinas.tempo.widget.utils.WidgetNavigation.periodFor(navigationTrigger)
        if (widgetPeriod != null) {
            navController.navigate(Screen.Stats.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            navController.currentBackStackEntry?.savedStateHandle?.set(WidgetPeriodKey, widgetPeriod.name)
            onWidgetNavigationHandled()
        } else if (navigationTrigger == "profile_challenges") {
            navController.navigate(Screen.Profile.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        } else if (navigationTrigger == "background_protection") {
            navController.navigate(Screen.BackgroundProtection.route)
        } else if (navigationTrigger == "update_center") {
            navController.navigate(Screen.UpdateCenter.route) { launchSingleTop = true }
            onWidgetNavigationHandled()
        }
    }

    // Dismiss active walkthrough on route changes.
    androidx.compose.runtime.LaunchedEffect(currentRoute) {
        if (currentRoute != null) {
            walkthroughController.dismissCurrent()
        }
    }

    val showBottomBar =
        currentRoute in
            listOf(
                Screen.Home.route,
                Screen.Stats.route,
                Screen.History.route,
            )

    // The primary destinations share this backdrop during navigation.
    DeepOceanBackground {
        me.avinas.tempo.ui.components.WalkthroughOverlay(
            controller = walkthroughController,
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                // On tablet/expanded widths, the rail frees vertical space
                // and keeps Home, Stats, and History reachable at all times.
                val adaptiveLayout = remember(maxWidth.value) { tempoAdaptiveLayout(maxWidth.value) }
                val useWideRail = showBottomBar && adaptiveLayout.primaryNavigation != TempoPrimaryNavigation.BOTTOM_BAR
                val expandWideRail = showBottomBar && adaptiveLayout.primaryNavigation == TempoPrimaryNavigation.EXPANDED_RAIL
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier.fillMaxSize()
                        .then(if (useWideRail) Modifier.padding(
                            start = measuredRailWidthDp,
                            bottom = wideNowPlayingHeightDp,
                        ) else Modifier)
                        // Reserve the measured bar (including gesture insets
                        // and optional now-playing strip) instead of allowing
                        // it to cover the last rows in Home/Stats/History.
                        .then(if (showBottomBar && !useWideRail) {
                            Modifier.padding(bottom = bottomBarHeightDp)
                        } else Modifier),
                    enterTransition = {
                        when {
                            !animateNavigation -> EnterTransition.None
                            initialState.destination.route in primaryRoutes &&
                                targetState.destination.route in primaryRoutes -> topLevelEnter
                            else -> slideInHorizontally(animationSpec = spatialSpec) { it / 8 } +
                                fadeIn(animationSpec = effectsSpec)
                        }
                    },
                    exitTransition = {
                        when {
                            !animateNavigation -> ExitTransition.None
                            initialState.destination.route in primaryRoutes &&
                                targetState.destination.route in primaryRoutes -> topLevelExit
                            else -> slideOutHorizontally(animationSpec = spatialSpec) { -it / 10 } +
                                fadeOut(animationSpec = effectsSpec)
                        }
                    },
                    popEnterTransition = {
                        when {
                            !animateNavigation -> EnterTransition.None
                            initialState.destination.route in primaryRoutes &&
                                targetState.destination.route in primaryRoutes -> topLevelEnter
                            else -> slideInHorizontally(animationSpec = spatialSpec) { -it / 8 } +
                                fadeIn(animationSpec = effectsSpec)
                        }
                    },
                    popExitTransition = {
                        when {
                            !animateNavigation -> ExitTransition.None
                            initialState.destination.route in primaryRoutes &&
                                targetState.destination.route in primaryRoutes -> topLevelExit
                            else -> slideOutHorizontally(animationSpec = spatialSpec) { it / 10 } +
                                fadeOut(animationSpec = effectsSpec)
                        }
                    },
                ) {
                    composable(Screen.Home.route) {
                        me.avinas.tempo.ui.home.HomeScreen(
                            onNavigateToStats = {
                                navController.navigate(Screen.Stats.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onNavigateToHistory = {
                                navController.navigate(Screen.History.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                            onNavigateToTrack = { trackId -> navController.safeNavigate(Screen.SongDetails.createRoute(trackId)) },
                            onNavigateToArtist = { artistIdentifier ->
                                if (artistIdentifier.startsWith("id:")) {
                                    val artistId = artistIdentifier.removePrefix("id:").toLongOrNull()
                                    if (artistId != null && artistId > 0) {
                                        navController.safeNavigate(Screen.ArtistDetails.createRouteById(artistId))
                                    }
                                } else {
                                    navController.safeNavigate(Screen.ArtistDetails.createRouteByName(artistIdentifier))
                                }
                            },
                            onNavigateToSpotlight = { timeRange, directLaunch ->
                                val route = Screen.Spotlight.createRoute(timeRange?.name, directLaunch)
                                navController.navigate(route)
                            },
                            onNavigateToSupportedApps = { navController.navigate(Screen.SupportedApps.route) },
                            onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                        )
                    }

                    composable(Screen.Stats.route) { entry ->
                        val navigationViewModel: me.avinas.tempo.ui.navigation.NavigationViewModel = hiltViewModel()

                        val widgetPeriod by entry.savedStateHandle
                            .getStateFlow<String?>(WidgetPeriodKey, null).collectAsState()
                        me.avinas.tempo.ui.stats.StatsScreen(
                            widgetPeriod = widgetPeriod?.let { me.avinas.tempo.data.stats.TimeRange.valueOf(it) },
                            onWidgetPeriodHandled = { entry.savedStateHandle.set<String?>(WidgetPeriodKey, null) },
                            onNavigateToTrack = { trackId -> navController.safeNavigate(Screen.SongDetails.createRoute(trackId)) },
                            onNavigateToArtist = { artistIdentifier ->
                                if (artistIdentifier.startsWith("id:")) {
                                    val artistId = artistIdentifier.removePrefix("id:").toLongOrNull()
                                    if (artistId != null && artistId > 0) {
                                        navController.safeNavigate(Screen.ArtistDetails.createRouteById(artistId))
                                    }
                                } else {
                                    navController.safeNavigate(Screen.ArtistDetails.createRouteByName(artistIdentifier))
                                }
                            },
                            onNavigateToAlbum = navigateToAlbum@{ title, artist ->
                                if (navController.currentBackStackEntry != entry ||
                                    !entry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                                    return@navigateToAlbum true
                                }
                                if (title.isBlank() || artist.isBlank()) return@navigateToAlbum false
                                val albumId = navigationViewModel.getAlbumIdByTitleAndArtist(title, artist)
                                if (navController.currentBackStackEntry != entry ||
                                    !entry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                                    return@navigateToAlbum true
                                }
                                if (albumId == null || albumId <= 0) return@navigateToAlbum false
                                navController.safeNavigate(Screen.AlbumDetails.createRoute(albumId))
                                true
                            },
                            onNavigateToSupportedApps = { navController.navigate(Screen.SupportedApps.route) },
                        )
                    }

                    composable(Screen.History.route) {
                        me.avinas.tempo.ui.history.HistoryScreen(
                            onNavigateToTrack = { trackId -> navController.safeNavigate(Screen.SongDetails.createRoute(trackId)) },
                        )
                    }

                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNavigate = navController::navigateSettingsDestination,
                        )
                    }

                    composable(Screen.Appearance.route) {
                        AppearanceScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToLogoPicker = { navController.safeNavigate(Screen.LauncherLogoPicker.route) },
                        )
                    }

                    composable(Screen.LauncherLogoPicker.route) {
                        LauncherLogoPickerScreen(onNavigateBack = { navController.popBackStack() })
                    }

                    composable(Screen.UpdateCenter.route) {
                        UpdateCenterScreen(onNavigateBack = { navController.popBackStack() })
                    }

                    composable(Screen.BackupRestore.route) {
                        BackupRestoreScreen(
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }

                    composable(Screen.SupportedApps.route) {
                        SupportedAppsScreen(
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }

                    composable(Screen.BackgroundProtection.route) {
                        BackgroundProtectionScreen(
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }

                    composable(Screen.LastFmImport.route) {
                        LastFmImportScreen(
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }

                    composable(Screen.SpotifyJsonImport.route) {
                        me.avinas.tempo.ui.spotify.SpotifyJsonImportScreen(
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }

                    composable(Screen.DeezerImport.route) {
                        me.avinas.tempo.ui.deezer.DeezerImportScreen(
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }

                    composable(Screen.AppleMusicImport.route) {
                        me.avinas.tempo.ui.applemusic.AppleMusicImportScreen(
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }

                    composable(Screen.YouTubeMusicImport.route) {
                        me.avinas.tempo.ui.youtube.YouTubeMusicImportScreen(
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }

                    composable(Screen.MusicoletImport.route) {
                        me.avinas.tempo.ui.musicolet.MusicoletImportScreen(
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }

                    composable(Screen.ExtensionImport.route) {
                        ExtensionImportScreen(
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }

                    composable(Screen.DesktopLink.route) {
                        DesktopLinkScreen(
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }

                    composable(Screen.EnrichmentReport.route) {
                        EnrichmentReportScreen(
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }

                    composable(Screen.YourData.route) {
                        YourDataScreen(onNavigateBack = { navController.popBackStack() })
                    }

                    composable(
                        route = Screen.Spotlight.route,
                        arguments =
                            listOf(
                                androidx.navigation.navArgument("timeRange") {
                                    type = androidx.navigation.NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                },
                                androidx.navigation.navArgument("directLaunch") {
                                    type = androidx.navigation.NavType.BoolType
                                    defaultValue = false
                                },
                            ),
                    ) { backStackEntry ->
                        val timeRangeString = backStackEntry.arguments?.getString("timeRange")
                        val directLaunch = backStackEntry.arguments?.getBoolean("directLaunch") ?: false
                        val initialTimeRange =
                            when (timeRangeString) {
                                "THIS_WEEK" -> me.avinas.tempo.data.stats.TimeRange.THIS_WEEK
                                "THIS_MONTH" -> me.avinas.tempo.data.stats.TimeRange.THIS_MONTH
                                "THIS_YEAR" -> me.avinas.tempo.data.stats.TimeRange.THIS_YEAR
                                else -> null
                            }

                        me.avinas.tempo.ui.spotlight.SpotlightScreen(
                            navController = navController,
                            initialTimeRange = initialTimeRange,
                            directLaunch = directLaunch,
                        )
                    }

                    composable(
                        route = Screen.SongDetails.route,
                        arguments = listOf(androidx.navigation.navArgument("trackId") { type = androidx.navigation.NavType.LongType }),
                    ) { backStackEntry ->
                        val trackId = backStackEntry.arguments?.getLong("trackId") ?: return@composable
                        val scope = rememberCoroutineScope()
                        val navigationViewModel: me.avinas.tempo.ui.navigation.NavigationViewModel = hiltViewModel()

                        me.avinas.tempo.ui.details.SongDetailsScreen(
                            trackId = trackId,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToArtist = { artistIdentifier ->
                                if (artistIdentifier.startsWith("id:")) {
                                    val artistId = artistIdentifier.removePrefix("id:").toLongOrNull()
                                    if (artistId != null && artistId > 0) {
                                        navController.safeNavigate(Screen.ArtistDetails.createRouteById(artistId))
                                    }
                                } else {
                                    navController.safeNavigate(Screen.ArtistDetails.createRouteByName(artistIdentifier))
                                }
                            },
                            onNavigateToAlbum = { albumTitle, artistName ->
                                scope.launch {
                                    val albumId = navigationViewModel.getAlbumIdByTitleAndArtist(albumTitle, artistName)
                                    if (albumId != null) {
                                        navController.safeNavigate(Screen.AlbumDetails.createRoute(albumId))
                                    }
                                }
                            },
                        )
                    }

                    composable(
                        route = Screen.ArtistDetails.route,
                        arguments =
                            listOf(
                                androidx.navigation.navArgument("artistId") {
                                    type = androidx.navigation.NavType.LongType
                                    defaultValue = 0L
                                },
                                androidx.navigation.navArgument("artistName") {
                                    type = androidx.navigation.NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                },
                            ),
                    ) { backStackEntry ->
                        val artistId = backStackEntry.arguments?.getLong("artistId") ?: 0L
                        val artistName =
                            backStackEntry.arguments?.getString("artistName")?.let {
                                java.net.URLDecoder.decode(it, "UTF-8")
                            }

                        if (artistId > 0) {
                            me.avinas.tempo.ui.details.ArtistDetailsScreen(
                                artistId = artistId,
                                artistName = null,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToSong = { trackId -> navController.safeNavigate(Screen.SongDetails.createRoute(trackId)) },
                            )
                        } else if (artistName != null) {
                            me.avinas.tempo.ui.details.ArtistDetailsScreen(
                                artistId = null,
                                artistName = artistName,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToSong = { trackId -> navController.safeNavigate(Screen.SongDetails.createRoute(trackId)) },
                            )
                        } else {
                            navController.popBackStack()
                        }
                    }

                    composable(
                        route = Screen.AlbumDetails.route,
                        arguments = listOf(androidx.navigation.navArgument("albumId") { type = androidx.navigation.NavType.LongType }),
                    ) { backStackEntry ->
                        val albumId = backStackEntry.arguments?.getLong("albumId") ?: return@composable
                        me.avinas.tempo.ui.details.AlbumDetailsScreen(
                            albumId = albumId,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToSong = { trackId -> navController.safeNavigate(Screen.SongDetails.createRoute(trackId)) },
                            onNavigateToArtist = { artistId -> navController.safeNavigate(Screen.ArtistDetails.createRouteById(artistId)) },
                            onNavigateToAlbum = { targetAlbumId ->
                                navController.popBackStack()
                                navController.safeNavigate(Screen.AlbumDetails.createRoute(targetAlbumId))
                            },
                        )
                    }

                    composable(
                        route = Screen.ShareCanvas.route,
                        arguments =
                            listOf(
                                androidx.navigation.navArgument("initialCardId") {
                                    type = androidx.navigation.NavType.StringType
                                },
                                androidx.navigation.navArgument("timeRange") {
                                    type = androidx.navigation.NavType.StringType
                                    defaultValue = me.avinas.tempo.data.stats.TimeRange.ALL_TIME.name
                                },
                            ),
                    ) { entry ->
                        val snapshot by entry.savedStateHandle
                            .getStateFlow<me.avinas.tempo.ui.spotlight.SpotlightCardData?>("shareCardSnapshot", null)
                            .collectAsState()
                        me.avinas.tempo.ui.spotlight.canvas.ShareCanvasScreen(
                            snapshot = snapshot,
                            onClose = { navController.popBackStack() },
                        )
                    }

                    composable(Screen.Profile.route) {
                        me.avinas.tempo.ui.profile.ProfileScreen(
                            onBack = { navController.popBackStack() },
                            onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                        )
                    }
                }

                if (showBottomBar) {
                    // Keep the same navigation back-stack semantics on all sizes.
                    // Reselecting the active destination creates a fresh screen,
                    // returning its scroll position to the top. Switching between
                    // destinations still saves/restores each destination's state.
                    val navigatePrimary: (String) -> Unit = { route ->
                        if (currentRoute == route) {
                            navController.navigate(route) {
                                popUpTo(route) { inclusive = true }
                                launchSingleTop = true
                                restoreState = false
                            }
                        } else {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                    val goHome = { navigatePrimary(Screen.Home.route) }
                    val goStats = { navigatePrimary(Screen.Stats.route) }
                    val goHistory = { navigatePrimary(Screen.History.route) }
                    if (useWideRail) {
                        TempoMainNavigationRail(
                            currentDestination = currentDestination,
                            expanded = expandWideRail,
                            onNavigateToHome = goHome,
                            onNavigateToStats = goStats,
                            onNavigateToHistory = goHistory,
                            modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight()
                                .onSizeChanged { railWidthPx = it.width },
                        )
                        // Bottom-bar mode previously owned this strip. Preserve it
                        // on wide screens without placing it underneath the rail.
                        Box(
                            modifier = Modifier.align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(start = measuredRailWidthDp)
                                .onSizeChanged { wideNowPlayingHeightPx = it.height },
                        ) {
                            CompactNowPlayingStrip()
                        }
                    } else {
                        TempoBottomNavigation(
                            currentDestination = currentDestination,
                            onNavigateToHome = goHome,
                            onNavigateToStats = goStats,
                            onNavigateToHistory = goHistory,
                            modifier = Modifier.align(Alignment.BottomCenter)
                                .onSizeChanged { bottomBarHeightPx = it.height },
                        )
                    }
                }
            }
        }
    }
}
