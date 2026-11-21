package me.avinas.tempo.ui.settings

import android.graphics.drawable.Drawable
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import me.avinas.tempo.ui.components.TempoCircularLoadingIndicator
import me.avinas.tempo.ui.components.TempoLinearLoadingIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import me.avinas.tempo.R
import me.avinas.tempo.data.InstalledApp
import me.avinas.tempo.data.local.entities.AppPreference
import me.avinas.tempo.data.local.entities.ManualContentMark
import me.avinas.tempo.data.preferences.TrackingRulesPreferences
import me.avinas.tempo.data.preferences.TrackingRulesPreferences.ContentOverrideType
import me.avinas.tempo.data.preferences.TrackingRulesPreferences.DurationMode
import me.avinas.tempo.ui.components.DeepOceanBackground
import me.avinas.tempo.ui.components.GlassCard
import me.avinas.tempo.ui.components.GlassCardVariant
import me.avinas.tempo.ui.components.SettingsSectionHeader
import me.avinas.tempo.ui.components.TempoDialogShape
import me.avinas.tempo.ui.theme.GlassBorderSoft
import me.avinas.tempo.ui.theme.GlassFrostSoft
import me.avinas.tempo.ui.theme.TempoDarkSurfaceElevated
import me.avinas.tempo.ui.theme.TempoPrimary
import me.avinas.tempo.ui.theme.TempoSuccessBright
import me.avinas.tempo.ui.theme.TempoSurfaceDialog
import me.avinas.tempo.ui.theme.TextOnAccent
import me.avinas.tempo.ui.theme.TextPrimary
import me.avinas.tempo.ui.theme.TextQuaternary
import me.avinas.tempo.ui.theme.TextSecondary
import me.avinas.tempo.ui.theme.TextTertiary

/**
 * Semaphore that limits the number of concurrent PackageManager icon loads.
 * PackageManager.getApplicationIcon() internally acquires locks on AssetManager/ResourcesImpl.
 * Without this limit, loading many icons in parallel causes severe lock contention which
 * can starve the main thread and trigger an ANR (Input dispatching timed out).
 */
private val iconLoadingSemaphore = Semaphore(permits = 4)

@Composable
internal fun TrackingRulesCard(
    minimumPlayDurationMs: Long,
    defaultMaxMusicDurationMs: Long?,
    overrideCount: Int,
    trackUnrecognizedApps: Boolean,
    onMinimumPlayClick: () -> Unit,
    onDefaultMaxClick: () -> Unit,
    onOverridesClick: () -> Unit,
    onTrackUnrecognizedChange: (Boolean) -> Unit
) {
    GlassCard(
        contentPadding = PaddingValues(0.dp),
        variant = GlassCardVariant.LowProminence
    ) {
        Column {
            TrackingRuleRow(
                icon = Icons.Default.HourglassBottom,
                title = stringResource(R.string.tracking_count_listen_after),
                subtitle = formatDuration(minimumPlayDurationMs),
                onClick = onMinimumPlayClick
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            TrackingRuleRow(
                icon = Icons.Default.Timer,
                title = stringResource(R.string.tracking_default_max_duration),
                subtitle = defaultMaxMusicDurationMs?.let(::formatDuration)
                    ?: stringResource(R.string.tracking_no_limit),
                onClick = onDefaultMaxClick
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            TrackingRuleRow(
                icon = Icons.Default.FilterAlt,
                title = stringResource(R.string.tracking_content_exceptions),
                subtitle = if (overrideCount == 0) {
                    stringResource(R.string.tracking_content_exception_types)
                } else {
                    pluralStringResource(R.plurals.tracking_saved_rules, overrideCount, overrideCount)
                },
                onClick = onOverridesClick
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            // Escape hatch for the default-deny model: users running a niche player they would
            // rather not allow one by one can opt back into the old permissive behaviour.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Track unrecognized apps",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Off by default. Tempo tracks only apps it knows are music " +
                            "players, and lists anything else for review.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = trackUnrecognizedApps,
                    onCheckedChange = onTrackUnrecognizedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        checkedBorderColor = Color.Transparent,
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }
        }
    }
}

@Composable
internal fun TrackingRuleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun AppPreferenceItem(
    app: AppPreference,
    onToggle: (Boolean) -> Unit,
    onDurationClick: () -> Unit,
    durationSummary: String,
    onBlock: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    showDivider: Boolean = true,
    isUserAdded: Boolean = false,
    isInstalled: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(
                packageName = app.packageName,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = durationSummary,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (isInstalled && app.isEnabled) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Installed • Start listening to track",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(onClick = onDurationClick) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = stringResource(R.string.tracking_duration_limit),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                )
            }

            if (isUserAdded && onRemove != null) {
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    )
                }
            }

            if (!isUserAdded && onBlock != null) {
                IconButton(onClick = onBlock) {
                    Icon(
                        Icons.Default.Block,
                        contentDescription = "Block",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    )
                }
            }

            Switch(
                checked = app.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    checkedBorderColor = Color.Transparent,
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }

        if (showDivider) {
            HorizontalDivider(
                color = GlassBorderSoft,
                modifier = Modifier.padding(start = 72.dp, end = 16.dp) // Indented divider
            )
        }
    }
}

@Composable
internal fun BlockedAppItem(
    app: AppPreference,
    onUnblock: () -> Unit,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(
                packageName = app.packageName,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            TextButton(
                onClick = onUnblock,
                colors = ButtonDefaults.textButtonColors(contentColor = TempoSuccessBright)
            ) {
                Text("Unblock")
            }
        }

        if (showDivider) {
            HorizontalDivider(
                color = GlassBorderSoft,
                modifier = Modifier.padding(start = 72.dp, end = 16.dp)
            )
        }
    }
}

/**
 * An inbox row: an app Tempo refused to track, with what it was playing and how often.
 * Allow grants tracking; Block writes a real preference so it stops being offered and moves to
 * Blocked Apps, where the decision can be reversed.
 */
@Composable
internal fun DetectedAppItem(
    app: DetectedAppUi,
    onAllow: () -> Unit,
    onBlock: () -> Unit,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(packageName = app.packageName, modifier = Modifier.size(40.dp))

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val detail = buildString {
                    app.lastTitle?.takeIf { it.isNotBlank() }?.let { append("Heard: $it") }
                    if (app.detectionCount > 1) {
                        if (isNotEmpty()) append(" • ")
                        append("${app.detectionCount}×")
                    }
                }
                if (detail.isNotEmpty()) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            TextButton(
                onClick = onBlock,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                Text("Block")
            }
            TextButton(
                onClick = onAllow,
                colors = ButtonDefaults.textButtonColors(contentColor = TempoSuccessBright)
            ) {
                Text("Allow")
            }
        }

        if (showDivider) {
            HorizontalDivider(
                color = GlassBorderSoft,
                modifier = Modifier.padding(start = 72.dp, end = 16.dp)
            )
        }
    }
}

/** An installed app Tempo knows nothing about, surfaced by search with an Allow action. */
@Composable
internal fun AllowableAppItem(
    app: InstalledApp,
    onAllow: () -> Unit,
    onBlock: () -> Unit,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(packageName = app.packageName, modifier = Modifier.size(40.dp))

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            TextButton(
                onClick = onBlock,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                Text("Block")
            }
            TextButton(
                onClick = onAllow,
                colors = ButtonDefaults.textButtonColors(contentColor = TempoSuccessBright)
            ) {
                Text("Allow")
            }
        }

        if (showDivider) {
            HorizontalDivider(
                color = GlassBorderSoft,
                modifier = Modifier.padding(start = 72.dp, end = 16.dp)
            )
        }
    }
}

/**
 * A voice-note/call/recorder app. Read-only on purpose: there is no switch, because the answer
 * is always no and a toggle would imply otherwise.
 */
@Composable
internal fun NeverTrackedAppItem(
    app: InstalledApp,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(packageName = app.packageName, modifier = Modifier.size(40.dp))

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "Never tracked",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (showDivider) {
            HorizontalDivider(
                color = GlassBorderSoft,
                modifier = Modifier.padding(start = 72.dp, end = 16.dp)
            )
        }
    }
}

@Composable
fun AppIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var icon by remember(packageName) { mutableStateOf<Drawable?>(null) }

    LaunchedEffect(packageName) {
        withContext(Dispatchers.IO) {
            try {
                iconLoadingSemaphore.withPermit {
                    icon = context.packageManager.getApplicationIcon(packageName)
                }
            } catch (_: Exception) {
                // Keep null on error (e.g. package not found)
            }
        }
    }

    if (icon != null) {
        Image(
            painter = coil3.compose.rememberAsyncImagePainter(icon),
            contentDescription = null,
            modifier = modifier,
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
    } else {
        Box(
            modifier = modifier.background(GlassFrostSoft, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Android,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}

