package me.avinas.tempo.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import me.avinas.tempo.R
import me.avinas.tempo.ui.components.SettingsOption
import me.avinas.tempo.ui.components.SettingsSectionHeader
import me.avinas.tempo.ui.components.TempoSettingsBento
import me.avinas.tempo.ui.components.TempoSettingsDestination
import me.avinas.tempo.ui.components.TempoTileTone

/**
 * Settings overview is a navigation/search surface only.
 * It owns no preference state and does not duplicate settings implementations.
 */
@Composable
internal fun SettingsOverview(
    query: String,
    onQueryChange: (String) -> Unit,
    listState: LazyListState,
    onSectionSelected: (SettingsSection) -> Unit,
    onSettingSelected: (SettingsSection, Int) -> Unit,
    onNavigate: (SettingsDestination) -> Unit,
    gamificationEnabled: Boolean,
    analyticsConfigured: Boolean,
    spotifyConnected: Boolean,
    spotifyAccountLoaded: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text(stringResource(R.string.settings_search_label)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (query.isBlank()) {
                    null
                } else {
                    {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.stats_search_clear),
                            )
                        }
                    }
                },
                shape = MaterialTheme.shapes.extraLarge,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .widthIn(max = 960.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }

        val tokens = query.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val directMatches = if (tokens.isEmpty()) {
            emptyList()
        } else {
            SettingsDestination.entries.map { destination ->
                Triple(
                    destination,
                    stringResource(destination.titleRes),
                    stringResource(destination.subtitleRes),
                )
            }.filter { (_, title, subtitle) ->
                tokens.all { token ->
                    title.contains(token, ignoreCase = true) ||
                        subtitle.contains(token, ignoreCase = true)
                }
            }
        }
        val sectionMatches = if (tokens.isEmpty()) {
            emptyList()
        } else {
            SettingsSection.browseOrder.map { section ->
                Triple(
                    section,
                    stringResource(section.titleRes),
                    stringResource(section.searchTermsRes),
                )
            }.filter { (_, title, terms) ->
                tokens.all { token ->
                    title.contains(token, ignoreCase = true) ||
                        terms.contains(token, ignoreCase = true)
                }
            }
        }

        val optionMatches = if (tokens.isEmpty()) {
            emptyList()
        } else {
            SettingsSearchCatalog.available(
                gamificationEnabled = gamificationEnabled,
                analyticsConfigured = analyticsConfigured,
                spotifyConnected = spotifyConnected,
                spotifyAccountLoaded = spotifyAccountLoaded,
            ).map { entry ->
                Triple(
                    entry,
                    stringResource(entry.titleRes),
                    entry.subtitleRes?.let { stringResource(it) }.orEmpty(),
                )
            }.filter { (_, title, subtitle) ->
                tokens.all { token ->
                    title.contains(token, ignoreCase = true) ||
                        subtitle.contains(token, ignoreCase = true)
                }
            }
        }

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .widthIn(max = 960.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                if (tokens.isEmpty()) {
                    item(key = "settings-overview") {
                        SettingsSectionHeader(stringResource(R.string.settings_category_browse))
                        val appearance = SettingsDestination.APPEARANCE
                        val appearanceDestination = TempoSettingsDestination(
                            title = stringResource(appearance.titleRes),
                            subtitle = stringResource(appearance.subtitleRes),
                            icon = Icons.Rounded.Palette,
                            onClick = { onNavigate(appearance) },
                            featured = true,
                            tone = TempoTileTone.ROSE,
                        )
                        val sectionDestinations = SettingsSection.browseOrder.map { section ->
                            TempoSettingsDestination(
                                title = stringResource(section.titleRes),
                                subtitle = stringResource(section.descriptionRes),
                                icon = section.icon,
                                tone = section.tone,
                                onClick = { onSectionSelected(section) },
                            )
                        }
                        TempoSettingsBento(
                            destinations = listOf(appearanceDestination) + sectionDestinations,
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                } else {
                    items(
                        items = directMatches,
                        key = { (destination, _, _) -> "destination-${destination.name}" },
                    ) { (destination, title, subtitle) ->
                        SettingsOption(
                            title = title,
                            subtitle = subtitle,
                            onClick = { onNavigate(destination) },
                        )
                    }
                    items(
                        items = optionMatches,
                        key = { (entry, _, _) -> "setting-${entry.section.name}-${entry.titleRes}" },
                    ) { (entry, title, subtitle) ->
                        val sectionName = stringResource(entry.section.titleRes)
                        SettingsOption(
                            title = title,
                            subtitle = if (subtitle.isBlank()) sectionName
                                else "$sectionName · $subtitle",
                            onClick = { onSettingSelected(entry.section, entry.groupIndex) },
                        )
                    }
                    items(
                        items = sectionMatches,
                        key = { (section, _, _) -> "section-${section.name}" },
                    ) { (section, title, terms) ->
                        SettingsOption(
                            title = title,
                            subtitle = terms,
                            onClick = { onSectionSelected(section) },
                        )
                    }
                    if (directMatches.isEmpty() && sectionMatches.isEmpty() && optionMatches.isEmpty()) {
                        item(key = "settings-search-empty") {
                            Text(stringResource(R.string.settings_search_empty))
                        }
                    }
                }
            }
        }
    }
}
