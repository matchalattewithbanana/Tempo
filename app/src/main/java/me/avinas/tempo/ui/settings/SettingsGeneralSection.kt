package me.avinas.tempo.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.avinas.tempo.R
import me.avinas.tempo.ui.components.CachedAsyncImage
import me.avinas.tempo.ui.components.SettingsOption
import me.avinas.tempo.ui.components.SettingsSectionHeader
import me.avinas.tempo.ui.components.SettingsSwitch
import me.avinas.tempo.ui.components.TempoSettingsGroup

@Composable
internal fun GeneralSettingsSection(
    state: LazyListState,
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    currentLanguageSubtitle: String,
    onEditName: () -> Unit,
    onPickProfileImage: () -> Unit,
    onSelectLanguage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSectionList(state = state, modifier = modifier) {
        item(key = "general-profile") {
            SettingsSectionHeader(stringResource(R.string.settings_profile))
            TempoSettingsGroup(modifier = Modifier.clickable(onClick = onEditName)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.size(64.dp).clickable(onClick = onPickProfileImage),
                            contentAlignment = Alignment.BottomEnd,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.primaryContainer,
                                            ),
                                        ),
                                        shape = CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (uiState.profileImagePath.isNullOrBlank()) {
                                    Text(
                                        text = uiState.userName.firstOrNull()
                                            ?.toString()?.uppercase() ?: "U",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                } else {
                                    CachedAsyncImage(
                                        imageUrl = uiState.profileImagePath,
                                        contentDescription = stringResource(R.string.settings_profile_photo),
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                tonalElevation = 0.dp,
                                modifier = Modifier.size(22.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = stringResource(R.string.settings_change_photo),
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.userName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = stringResource(R.string.settings_tap_to_edit),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = stringResource(R.string.settings_tap_photo_to_change),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (uiState.profileImagePath.isNullOrBlank()) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.settings_update_name),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                    }
                    if (!uiState.profileImagePath.isNullOrBlank()) {
                        TextButton(
                            onClick = viewModel::removeProfileImage,
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Text(
                                text = stringResource(R.string.settings_remove_photo),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        item(key = "general-language") {
            SettingsSectionHeader(stringResource(R.string.settings_language))
            TempoSettingsGroup {
                SettingsOption(
                    title = stringResource(R.string.settings_language_title),
                    subtitle = currentLanguageSubtitle,
                    onClick = onSelectLanguage,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item(key = "general-notifications") {
            SettingsSectionHeader(stringResource(R.string.settings_notifications))
            TempoSettingsGroup {
                Column {
                    SettingsSwitch(
                        title = stringResource(R.string.settings_daily_summary),
                        subtitle = stringResource(R.string.settings_daily_summary_desc),
                        checked = uiState.dailySummaryEnabled,
                        onCheckedChange = viewModel::toggleDailySummary,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsSwitch(
                        title = stringResource(R.string.settings_weekly_recap),
                        subtitle = stringResource(R.string.settings_weekly_recap_desc),
                        checked = uiState.weeklyRecapEnabled,
                        onCheckedChange = viewModel::toggleWeeklyRecap,
                    )
                    if (uiState.isGamificationEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsSwitch(
                            title = stringResource(R.string.settings_daily_challenges),
                            subtitle = stringResource(R.string.settings_daily_challenges_desc),
                            checked = uiState.dailyChallengesEnabled,
                            onCheckedChange = viewModel::toggleDailyChallenges,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsSwitch(
                            title = stringResource(R.string.settings_achievements),
                            subtitle = stringResource(R.string.settings_achievements_desc),
                            checked = uiState.achievementsEnabled,
                            onCheckedChange = viewModel::toggleAchievements,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item(key = "general-progress") {
            SettingsSectionHeader(stringResource(R.string.settings_listening_goals))
            TempoSettingsGroup {
                Column {
                    SettingsSwitch(
                        title = stringResource(R.string.settings_enable_gamification),
                        subtitle = stringResource(R.string.settings_enable_gamification_desc),
                        checked = uiState.isGamificationEnabled,
                        onCheckedChange = viewModel::toggleGamificationEnabled,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsSwitch(
                        title = stringResource(R.string.settings_hide_streaks),
                        subtitle = stringResource(R.string.settings_hide_streaks_desc),
                        checked = uiState.hideStreaks,
                        onCheckedChange = viewModel::toggleHideStreaks,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
