package me.avinas.tempo.ui.navigation

import android.net.Uri
import me.avinas.tempo.data.stats.TimeRange

/** Stable route identifiers and builders shared by navigation callers. */
sealed class Screen(
    val route: String,
) {
    object Home : Screen("home")

    object Stats : Screen("stats")

    object History : Screen("history")

    object Settings : Screen("settings")
    object Appearance : Screen("appearance")
    object LauncherLogoPicker : Screen("launcher_logo_picker")
    object UpdateCenter : Screen("update_center")

    object Spotlight : Screen("spotlight?timeRange={timeRange}&directLaunch={directLaunch}") {
        fun createRoute(
            timeRange: String? = null,
            directLaunch: Boolean = false,
        ): String {
            val params = mutableListOf<String>()
            if (timeRange != null) params.add("timeRange=$timeRange")
            if (directLaunch) params.add("directLaunch=true")
            return if (params.isEmpty()) "spotlight" else "spotlight?${params.joinToString("&")}"
        }
    }

    object SongDetails : Screen("song_details/{trackId}") {
        fun createRoute(trackId: Long) = "song_details/$trackId"
    }

    // Artist details now supports both ID-based (preferred) and name-based (fallback) navigation
    object ArtistDetails : Screen("artist_details/{artistId}?artistName={artistName}") {
        fun createRouteById(artistId: Long) = "artist_details/$artistId"

        fun createRouteByName(artistName: String) = "artist_details/0?artistName=${java.net.URLEncoder.encode(artistName, "UTF-8")}"

        // Legacy method for backwards compatibility
        fun createRoute(artistName: String) = createRouteByName(artistName)
    }

    object AlbumDetails : Screen("album_details/{albumId}") {
        fun createRoute(albumId: Long) = "album_details/$albumId"
    }

    object Insights : Screen("insights")

    data object BackupRestore : Screen("backup_restore")

    data object SupportedApps : Screen("supported_apps")

    data object BackgroundProtection : Screen("background_protection")

    data object LastFmImport : Screen("lastfm_import")

    data object SpotifyJsonImport : Screen("spotify_json_import")

    data object DeezerImport : Screen("deezer_import")
    data object AppleMusicImport : Screen("apple_music_import")

    data object YouTubeMusicImport : Screen("youtube_music_import")

    data object MusicoletImport : Screen("musicolet_import")

    data object ExtensionImport : Screen("extension_import")

    data object DesktopLink : Screen("desktop_link")

    data object EnrichmentReport : Screen("enrichment_report")

    data object YourData : Screen("your_data")

    object ShareCanvas : Screen("share_canvas/{initialCardId}?timeRange={timeRange}") {
        fun createRoute(initialCardId: String, timeRange: TimeRange) =
            "share_canvas/${Uri.encode(initialCardId)}?timeRange=${timeRange.name}"
    }

    object Profile : Screen("profile")
}
