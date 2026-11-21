package me.avinas.tempo.ui.settings

import androidx.annotation.StringRes
import me.avinas.tempo.R

/**
 * Typed navigation boundary for Settings.
 *
 * Each destination has one conceptual owner. Search may surface any destination,
 * but it does not create another owner or duplicate the destination definition.
 */
enum class SettingsDestination(
    val owner: SettingsSection,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
) {
    APPEARANCE(
        owner = SettingsSection.GENERAL,
        titleRes = R.string.appearance_title,
        subtitleRes = R.string.appearance_settings_summary,
    ),
    LOGO_PICKER(
        owner = SettingsSection.GENERAL,
        titleRes = R.string.logo_picker_title,
        subtitleRes = R.string.logo_picker_intro,
    ),
    UPDATES(
        owner = SettingsSection.ABOUT,
        titleRes = R.string.update_center_title,
        subtitleRes = R.string.update_center_desc,
    ),
    SUPPORTED_APPS(
        owner = SettingsSection.TRACKING,
        titleRes = R.string.settings_manage_apps,
        subtitleRes = R.string.settings_manage_apps_desc,
    ),
    BACKGROUND_PROTECTION(
        owner = SettingsSection.TRACKING,
        titleRes = R.string.settings_background_protection,
        subtitleRes = R.string.settings_search_tracking_terms,
    ),
    DESKTOP(
        owner = SettingsSection.CONNECTIONS,
        titleRes = R.string.desktop_link_settings_title,
        subtitleRes = R.string.desktop_link_settings_subtitle,
    ),
    BACKUP(
        owner = SettingsSection.DATA,
        titleRes = R.string.settings_backup_restore,
        subtitleRes = R.string.settings_backup_restore_desc,
    ),
    SPOTIFY_JSON_IMPORT(
        owner = SettingsSection.DATA,
        titleRes = R.string.settings_import_spotify_json,
        subtitleRes = R.string.settings_import_spotify_json_desc,
    ),
    APPLE_MUSIC_IMPORT(
        owner = SettingsSection.DATA,
        titleRes = R.string.settings_import_apple_music,
        subtitleRes = R.string.settings_import_apple_music_desc,
    ),
    DEEZER_IMPORT(
        owner = SettingsSection.DATA,
        titleRes = R.string.settings_import_deezer,
        subtitleRes = R.string.settings_import_deezer_desc,
    ),
    YOUTUBE_MUSIC_IMPORT(
        owner = SettingsSection.DATA,
        titleRes = R.string.settings_import_youtube_music,
        subtitleRes = R.string.settings_import_youtube_music_desc,
    ),
    LASTFM_IMPORT(
        owner = SettingsSection.DATA,
        titleRes = R.string.settings_import_lastfm,
        subtitleRes = R.string.settings_import_lastfm_desc,
    ),
    MUSICOLET_IMPORT(
        owner = SettingsSection.DATA,
        titleRes = R.string.settings_import_musicolet,
        subtitleRes = R.string.settings_import_musicolet_desc,
    ),
    EXTENSION_IMPORT(
        owner = SettingsSection.DATA,
        titleRes = R.string.settings_import_extension,
        subtitleRes = R.string.settings_import_extension_desc,
    ),
    YOUR_DATA(
        owner = SettingsSection.DATA,
        titleRes = R.string.settings_your_data,
        subtitleRes = R.string.settings_search_data_terms,
    ),
    ENRICHMENT_REPORT(
        owner = SettingsSection.DATA,
        titleRes = R.string.enrichment_report_settings_option,
        subtitleRes = R.string.enrichment_report_settings_option_desc,
    ),
}
