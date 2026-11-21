package me.avinas.tempo.ui.settings

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.ui.graphics.vector.ImageVector
import me.avinas.tempo.R
import me.avinas.tempo.ui.components.TempoTileTone

/**
 * Canonical ownership model for Settings.
 *
 * A setting belongs to exactly one section. Overview and search are navigation
 * surfaces over this catalog, not additional owners.
 */
enum class SettingsSection(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @StringRes val searchTermsRes: Int,
    val icon: ImageVector,
    val tone: TempoTileTone,
) {
    GENERAL(
        titleRes = R.string.settings_category_general,
        descriptionRes = R.string.settings_category_general_desc,
        searchTermsRes = R.string.settings_search_general_terms,
        icon = Icons.Rounded.Settings,
        tone = TempoTileTone.LILAC,
    ),
    TRACKING(
        titleRes = R.string.settings_category_tracking,
        descriptionRes = R.string.settings_category_tracking_desc,
        searchTermsRes = R.string.settings_search_tracking_terms,
        icon = Icons.Rounded.GraphicEq,
        tone = TempoTileTone.AQUA,
    ),
    CONNECTIONS(
        titleRes = R.string.settings_category_connections,
        descriptionRes = R.string.settings_category_connections_desc,
        searchTermsRes = R.string.settings_search_connections_terms,
        icon = Icons.Rounded.Link,
        tone = TempoTileTone.SKY,
    ),
    DATA(
        titleRes = R.string.settings_category_data,
        descriptionRes = R.string.settings_category_data_desc,
        searchTermsRes = R.string.settings_search_data_terms,
        icon = Icons.Rounded.Storage,
        tone = TempoTileTone.AMBER,
    ),
    ABOUT(
        titleRes = R.string.settings_category_about,
        descriptionRes = R.string.settings_category_about_desc,
        searchTermsRes = R.string.settings_search_about_terms,
        icon = Icons.Rounded.Info,
        tone = TempoTileTone.ROSE,
    );

    companion object {
        /** Product order used by both the overview mosaic and search. */
        val browseOrder: List<SettingsSection> =
            listOf(TRACKING, GENERAL, CONNECTIONS, DATA, ABOUT)
    }
}
