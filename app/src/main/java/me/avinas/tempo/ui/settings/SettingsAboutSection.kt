package me.avinas.tempo.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.avinas.tempo.BuildConfig
import me.avinas.tempo.R
import me.avinas.tempo.ui.components.SettingsOption
import me.avinas.tempo.ui.components.SettingsSectionHeader
import me.avinas.tempo.ui.components.TempoSettingsGroup
import me.avinas.tempo.utils.ReviewUtils

@Composable
internal fun AboutSettingsSection(
    state: LazyListState,
    versionName: String,
    onNavigate: (SettingsDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    SettingsSectionList(
        state = state,
        tone = SettingsSection.ABOUT.tone,
        modifier = modifier,
    ) {
        item(key = "about-credits") {
            TempoAboutCredits(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(24.dp))
        }
        item(key = "about-community") {
            SettingsSectionHeader(stringResource(R.string.settings_community))
            TempoSettingsGroup {
                Column {
                    SettingsOption(
                        title = stringResource(R.string.settings_reddit),
                        subtitle = stringResource(R.string.settings_reddit_sub),
                        onClick = {
                            context.openSettingsUrl(
                                "https://www.reddit.com/r/TempoStats/",
                                resources.getString(R.string.settings_no_browser),
                            )
                        },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsOption(
                        title = stringResource(R.string.settings_github),
                        subtitle = stringResource(R.string.settings_github_desc),
                        onClick = {
                            context.openSettingsUrl(
                                "https://github.com/matchalattewithbanana/Tempo",
                                resources.getString(R.string.settings_no_browser),
                            )
                        },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsOption(
                        title = stringResource(R.string.settings_contribute),
                        subtitle = stringResource(R.string.settings_contribute_desc),
                        onClick = {
                            context.openSettingsUrl(
                                "https://github.com/matchalattewithbanana/Tempo/blob/main/CONTRIBUTING.md",
                                resources.getString(R.string.settings_no_browser),
                            )
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        item(key = "about-links") {
            SettingsSectionHeader(stringResource(R.string.settings_about_resources))
            TempoSettingsGroup {
                Column {
                    SettingsOption(
                        title = stringResource(
                            if (BuildConfig.PLAY_STORE_REVIEWS) {
                                R.string.settings_rate_play_store
                            } else {
                                R.string.settings_report_issue
                            },
                        ),
                        onClick = {
                            if (BuildConfig.PLAY_STORE_REVIEWS) {
                                ReviewUtils.openPlayStoreListing(context)
                            } else {
                                context.openSettingsUrl(
                                    "https://github.com/matchalattewithbanana/Tempo/issues",
                                    resources.getString(R.string.settings_no_browser),
                                )
                            }
                        },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsOption(
                        title = stringResource(R.string.settings_privacy_policy),
                        onClick = {
                            context.openSettingsUrl(
                                "https://github.com/matchalattewithbanana/Tempo/blob/main/PRIVACY.md",
                                resources.getString(R.string.settings_no_browser),
                            )
                        },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsOption(
                        title = stringResource(R.string.settings_data_diagnostics),
                        subtitle = stringResource(R.string.settings_data_diagnostics_desc),
                        onClick = { onNavigate(SettingsDestination.YOUR_DATA) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsOption(
                        title = stringResource(R.string.update_check_now),
                        subtitle = versionName,
                        onClick = { onNavigate(SettingsDestination.UPDATES) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsOption(
                        title = stringResource(R.string.settings_version),
                        subtitle = versionName,
                        showArrow = false,
                    )
                }
            }
        }
    }
}

private fun Context.openSettingsUrl(url: String, noBrowserMessage: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, noBrowserMessage, Toast.LENGTH_SHORT).show()
    }
}
