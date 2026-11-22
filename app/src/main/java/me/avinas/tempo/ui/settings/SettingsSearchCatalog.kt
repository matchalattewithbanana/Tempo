package me.avinas.tempo.ui.settings

import androidx.annotation.StringRes
import me.avinas.tempo.R

/**
 * Search entries for controls that live inside section screens.
 * Full-page destinations are indexed by SettingsDestination instead.
 *
 * Each entry points to a typed section group rather than a numeric scroll index;
 * search navigates and never duplicates preference state.
 */
internal data class SettingsSearchEntry(
    val group: SettingsGroup,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int? = null,
    val requires: SettingsSearchRequirement = SettingsSearchRequirement.ALWAYS,
) {
    val section: SettingsSection get() = group.section
}

internal enum class SettingsSearchRequirement {
    ALWAYS, GAMIFICATION, SPOTIFY_CONNECTED, SPOTIFY_CONFIGURABLE
}

internal object SettingsSearchCatalog {
    private val controls = listOf(
        SettingsSearchEntry(SettingsGroup.GENERAL_PROFILE, R.string.settings_profile),
        SettingsSearchEntry(SettingsGroup.GENERAL_LANGUAGE, R.string.settings_language_title),
        SettingsSearchEntry(SettingsGroup.GENERAL_NOTIFICATIONS, R.string.settings_daily_summary,
            R.string.settings_daily_summary_desc),
        SettingsSearchEntry(SettingsGroup.GENERAL_NOTIFICATIONS, R.string.settings_weekly_recap,
            R.string.settings_weekly_recap_desc),
        SettingsSearchEntry(SettingsGroup.GENERAL_NOTIFICATIONS, R.string.settings_daily_challenges,
            R.string.settings_daily_challenges_desc, SettingsSearchRequirement.GAMIFICATION),
        SettingsSearchEntry(SettingsGroup.GENERAL_NOTIFICATIONS, R.string.settings_achievements,
            R.string.settings_achievements_desc, SettingsSearchRequirement.GAMIFICATION),
        SettingsSearchEntry(SettingsGroup.GENERAL_PROGRESS, R.string.settings_enable_gamification,
            R.string.settings_enable_gamification_desc),
        SettingsSearchEntry(SettingsGroup.GENERAL_PROGRESS, R.string.settings_hide_streaks,
            R.string.settings_hide_streaks_desc),

        SettingsSearchEntry(SettingsGroup.TRACKING_SOURCE, R.string.settings_manage_permissions,
            R.string.settings_manage_permissions_desc),
        SettingsSearchEntry(SettingsGroup.TRACKING_SOURCE, R.string.settings_health_last_signal),
        SettingsSearchEntry(SettingsGroup.TRACKING_SOURCE, R.string.settings_pause_tracking_low_battery),
        SettingsSearchEntry(SettingsGroup.TRACKING_FILTERING, R.string.settings_filter_podcasts,
            R.string.settings_filter_podcasts_desc),
        SettingsSearchEntry(SettingsGroup.TRACKING_FILTERING, R.string.settings_filter_audiobooks,
            R.string.settings_filter_audiobooks_desc),
        SettingsSearchEntry(SettingsGroup.TRACKING_STATISTICS, R.string.settings_extended_audio,
            R.string.settings_extended_audio_desc),

        SettingsSearchEntry(SettingsGroup.CONNECTIONS_SERVICES, R.string.settings_spotify_service_title),
        SettingsSearchEntry(SettingsGroup.CONNECTIONS_SERVICES, R.string.settings_spotify_client_id_label,
            requires = SettingsSearchRequirement.SPOTIFY_CONFIGURABLE),
        SettingsSearchEntry(SettingsGroup.CONNECTIONS_SERVICES, R.string.settings_spotify_history_verification,
            R.string.settings_spotify_history_verification_desc, SettingsSearchRequirement.SPOTIFY_CONNECTED),
        SettingsSearchEntry(SettingsGroup.CONNECTIONS_SERVICES, R.string.settings_spotify_device_broadcasts,
            R.string.settings_spotify_device_broadcasts_desc),

        SettingsSearchEntry(SettingsGroup.DATA_IMPORTS, R.string.settings_smart_merge,
            R.string.settings_smart_merge_desc),
        SettingsSearchEntry(SettingsGroup.DATA_ACCURACY, R.string.settings_history_accuracy_audit_title,
            R.string.settings_history_accuracy_audit_desc),
        SettingsSearchEntry(SettingsGroup.DATA_ACCURACY, R.string.settings_cover_source_title),
        SettingsSearchEntry(SettingsGroup.DATA_PRIVACY, R.string.settings_analytics_toggle,
            R.string.settings_analytics_toggle_desc),
        SettingsSearchEntry(SettingsGroup.DATA_PRIVACY, R.string.settings_analytics_reset_id,
            R.string.settings_analytics_reset_id_desc),

        SettingsSearchEntry(SettingsGroup.ABOUT_COMMUNITY, R.string.settings_reddit),
        SettingsSearchEntry(SettingsGroup.ABOUT_COMMUNITY, R.string.settings_github),
        SettingsSearchEntry(SettingsGroup.ABOUT_COMMUNITY, R.string.settings_contribute),
        SettingsSearchEntry(SettingsGroup.ABOUT_LINKS, R.string.settings_privacy_policy),
        SettingsSearchEntry(SettingsGroup.ABOUT_LINKS, R.string.settings_data_diagnostics,
            R.string.settings_data_diagnostics_desc),
        SettingsSearchEntry(SettingsGroup.ABOUT_LINKS, R.string.settings_version),
    )

    fun available(
        gamificationEnabled: Boolean,
        analyticsConfigured: Boolean,
        spotifyConnected: Boolean,
        spotifyAccountLoaded: Boolean,
    ): List<SettingsSearchEntry> {
        val visible = controls.filter { entry ->
            (!entry.group.requiresAnalytics || analyticsConfigured) &&
                when (entry.requires) {
                    SettingsSearchRequirement.ALWAYS -> true
                    SettingsSearchRequirement.GAMIFICATION -> gamificationEnabled
                    SettingsSearchRequirement.SPOTIFY_CONNECTED -> spotifyConnected
                    SettingsSearchRequirement.SPOTIFY_CONFIGURABLE ->
                        spotifyAccountLoaded && !spotifyConnected
                }
        }
        // The optional analytics group is inserted before Reset only when configured.
        return visible + SettingsSearchEntry(
            SettingsGroup.DATA_RESET, R.string.settings_clear_all,
        )
    }
}
