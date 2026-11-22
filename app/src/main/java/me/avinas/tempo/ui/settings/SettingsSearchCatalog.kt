package me.avinas.tempo.ui.settings

import androidx.annotation.StringRes
import me.avinas.tempo.R

/**
 * Search entries for controls that live inside section screens.
 * Full-page destinations are indexed by SettingsDestination instead.
 *
 * The index points at the section's stable LazyColumn group, not at individual
 * switches; search only navigates and never duplicates preference state.
 */
internal data class SettingsSearchEntry(
    val section: SettingsSection,
    @StringRes val titleRes: Int,
    val groupIndex: Int,
    @StringRes val subtitleRes: Int? = null,
    val requires: SettingsSearchRequirement = SettingsSearchRequirement.ALWAYS,
)

internal enum class SettingsSearchRequirement {
    ALWAYS, GAMIFICATION, ANALYTICS, SPOTIFY_CONNECTED, SPOTIFY_CONFIGURABLE
}

internal object SettingsSearchCatalog {
    private val controls = listOf(
        SettingsSearchEntry(SettingsSection.GENERAL, R.string.settings_profile, 0),
        SettingsSearchEntry(SettingsSection.GENERAL, R.string.settings_language_title, 1),
        SettingsSearchEntry(SettingsSection.GENERAL, R.string.settings_daily_summary, 2,
            R.string.settings_daily_summary_desc),
        SettingsSearchEntry(SettingsSection.GENERAL, R.string.settings_weekly_recap, 2,
            R.string.settings_weekly_recap_desc),
        SettingsSearchEntry(SettingsSection.GENERAL, R.string.settings_daily_challenges, 2,
            R.string.settings_daily_challenges_desc, SettingsSearchRequirement.GAMIFICATION),
        SettingsSearchEntry(SettingsSection.GENERAL, R.string.settings_achievements, 2,
            R.string.settings_achievements_desc, SettingsSearchRequirement.GAMIFICATION),
        SettingsSearchEntry(SettingsSection.GENERAL, R.string.settings_enable_gamification, 3,
            R.string.settings_enable_gamification_desc),
        SettingsSearchEntry(SettingsSection.GENERAL, R.string.settings_hide_streaks, 3,
            R.string.settings_hide_streaks_desc),

        SettingsSearchEntry(SettingsSection.TRACKING, R.string.settings_manage_permissions, 0,
            R.string.settings_manage_permissions_desc),
        SettingsSearchEntry(SettingsSection.TRACKING, R.string.settings_health_last_signal, 0),
        SettingsSearchEntry(SettingsSection.TRACKING, R.string.settings_pause_tracking_low_battery, 0),
        SettingsSearchEntry(SettingsSection.TRACKING, R.string.settings_filter_podcasts, 1,
            R.string.settings_filter_podcasts_desc),
        SettingsSearchEntry(SettingsSection.TRACKING, R.string.settings_filter_audiobooks, 1,
            R.string.settings_filter_audiobooks_desc),
        SettingsSearchEntry(SettingsSection.TRACKING, R.string.settings_extended_audio, 2,
            R.string.settings_extended_audio_desc),

        SettingsSearchEntry(SettingsSection.CONNECTIONS, R.string.settings_spotify_service_title, 0),
        SettingsSearchEntry(SettingsSection.CONNECTIONS, R.string.settings_spotify_client_id_label, 0,
            requires = SettingsSearchRequirement.SPOTIFY_CONFIGURABLE),
        SettingsSearchEntry(SettingsSection.CONNECTIONS, R.string.settings_spotify_history_verification, 0,
            R.string.settings_spotify_history_verification_desc, SettingsSearchRequirement.SPOTIFY_CONNECTED),
        SettingsSearchEntry(SettingsSection.CONNECTIONS, R.string.settings_spotify_device_broadcasts, 0,
            R.string.settings_spotify_device_broadcasts_desc, SettingsSearchRequirement.SPOTIFY_CONNECTED),

        SettingsSearchEntry(SettingsSection.DATA, R.string.settings_smart_merge, 1,
            R.string.settings_smart_merge_desc),
        SettingsSearchEntry(SettingsSection.DATA, R.string.settings_history_accuracy_audit_title, 2,
            R.string.settings_history_accuracy_audit_desc),
        SettingsSearchEntry(SettingsSection.DATA, R.string.settings_cover_source_title, 2),
        SettingsSearchEntry(SettingsSection.DATA, R.string.settings_analytics_toggle, 3,
            R.string.settings_analytics_toggle_desc, SettingsSearchRequirement.ANALYTICS),
        SettingsSearchEntry(SettingsSection.DATA, R.string.settings_analytics_reset_id, 3,
            R.string.settings_analytics_reset_id_desc, SettingsSearchRequirement.ANALYTICS),

        SettingsSearchEntry(SettingsSection.ABOUT, R.string.settings_reddit, 1),
        SettingsSearchEntry(SettingsSection.ABOUT, R.string.settings_github, 1),
        SettingsSearchEntry(SettingsSection.ABOUT, R.string.settings_contribute, 1),
        SettingsSearchEntry(SettingsSection.ABOUT, R.string.settings_privacy_policy, 2),
        SettingsSearchEntry(SettingsSection.ABOUT, R.string.settings_version, 2),
    )

    fun available(
        gamificationEnabled: Boolean,
        analyticsConfigured: Boolean,
        spotifyConnected: Boolean,
        spotifyAccountLoaded: Boolean,
    ): List<SettingsSearchEntry> {
        val visible = controls.filter {
            when (it.requires) {
                SettingsSearchRequirement.ALWAYS -> true
                SettingsSearchRequirement.GAMIFICATION -> gamificationEnabled
                SettingsSearchRequirement.ANALYTICS -> analyticsConfigured
                SettingsSearchRequirement.SPOTIFY_CONNECTED -> spotifyConnected
                SettingsSearchRequirement.SPOTIFY_CONFIGURABLE -> spotifyAccountLoaded && !spotifyConnected
            }
        }
        // The optional analytics group is inserted before Reset only when configured.
        return visible + SettingsSearchEntry(
            SettingsSection.DATA, R.string.settings_clear_all,
            groupIndex = if (analyticsConfigured) 4 else 3,
        )
    }
}
