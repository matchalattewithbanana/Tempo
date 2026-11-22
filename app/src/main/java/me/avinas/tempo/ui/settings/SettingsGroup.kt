package me.avinas.tempo.ui.settings

/**
 * Semantic identities of settings groups, in the same order they appear
 * in each section's LazyColumn. Search never owns a numeric scroll position.
 *
 * The owning sections reference these stable item keys directly. Analytics
 * is the sole optional group and is excluded from the visible index when off.
 */
internal enum class SettingsGroup(
    val section: SettingsSection,
    val itemKey: String,
    val requiresAnalytics: Boolean = false,
) {
    GENERAL_PROFILE(SettingsSection.GENERAL, "general-profile"),
    GENERAL_LANGUAGE(SettingsSection.GENERAL, "general-language"),
    GENERAL_NOTIFICATIONS(SettingsSection.GENERAL, "general-notifications"),
    GENERAL_PROGRESS(SettingsSection.GENERAL, "general-progress"),
    TRACKING_SOURCE(SettingsSection.TRACKING, "tracking-source"),
    TRACKING_FILTERING(SettingsSection.TRACKING, "tracking-filtering"),
    TRACKING_STATISTICS(SettingsSection.TRACKING, "tracking-statistics"),
    CONNECTIONS_SERVICES(SettingsSection.CONNECTIONS, "connections-services"),
    CONNECTIONS_DESKTOP(SettingsSection.CONNECTIONS, "connections-desktop"),
    DATA_BACKUPS(SettingsSection.DATA, "data-backups"),
    DATA_IMPORTS(SettingsSection.DATA, "data-imports"),
    DATA_ACCURACY(SettingsSection.DATA, "data-accuracy"),
    DATA_PRIVACY(SettingsSection.DATA, "data-privacy", requiresAnalytics = true),
    DATA_RESET(SettingsSection.DATA, "data-reset"),
    ABOUT_CREDITS(SettingsSection.ABOUT, "about-credits"),
    ABOUT_COMMUNITY(SettingsSection.ABOUT, "about-community"),
    ABOUT_LINKS(SettingsSection.ABOUT, "about-links"),
    ;

    fun scrollIndexOrNull(analyticsConfigured: Boolean): Int? {
        val visible = entries.filter {
            it.section == section && (!it.requiresAnalytics || analyticsConfigured)
        }
        return visible.indexOf(this).takeIf { it >= 0 }
    }
}
