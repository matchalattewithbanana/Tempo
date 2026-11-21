@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.avinas.tempo.ui.onboarding

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import me.avinas.tempo.R
import me.avinas.tempo.data.drive.DriveBackupInfo
import me.avinas.tempo.data.drive.DriveRestoreResult
import me.avinas.tempo.data.importexport.ImportConflictStrategy
import me.avinas.tempo.data.importexport.ImportExportResult
import me.avinas.tempo.data.lastfm.LastFmImportService
import me.avinas.tempo.data.spotify.SpotifyJsonImportService
import me.avinas.tempo.data.youtube.YouTubeMusicImportService
import me.avinas.tempo.ui.components.GlassCard
import me.avinas.tempo.ui.components.GlassCardVariant
import me.avinas.tempo.ui.components.TempoSnackbar
import me.avinas.tempo.ui.lastfm.LastFmUiState
import me.avinas.tempo.ui.lastfm.LastFmViewModel
import me.avinas.tempo.ui.onboarding.restore.ExpandableRestoreCard
import me.avinas.tempo.ui.onboarding.restore.RestoreHeader
import me.avinas.tempo.ui.onboarding.restore.RestoreSectionTitle
import me.avinas.tempo.ui.onboarding.restore.RestoreTopBar
import me.avinas.tempo.ui.settings.BackupRestoreViewModel
import me.avinas.tempo.ui.settings.DriveOperationState
import me.avinas.tempo.ui.spotify.SpotifyJsonImportUiState
import me.avinas.tempo.ui.spotify.SpotifyJsonImportViewModel
import me.avinas.tempo.ui.theme.*
import me.avinas.tempo.ui.theme.TempoDarkBackground
import me.avinas.tempo.ui.utils.adaptiveSizeByCategory
import me.avinas.tempo.ui.utils.adaptiveTextUnitByCategory
import me.avinas.tempo.ui.utils.rememberClampedHeightPercentage
import me.avinas.tempo.ui.utils.rememberScreenHeightPercentage
import me.avinas.tempo.ui.utils.scaledSize
import me.avinas.tempo.ui.youtube.TakeoutGuideDialog
import me.avinas.tempo.ui.youtube.YouTubeMusicImportUiState
import me.avinas.tempo.ui.youtube.YouTubeMusicImportViewModel
import me.avinas.tempo.utils.FormatUtils.formatBytes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LastFmImportCard(
    uiState: LastFmUiState,
    importProgress: LastFmImportService.ImportProgress,
    onUsernameSubmit: (String) -> Unit,
    onSelectTier: (LastFmImportService.TierConfig) -> Unit,
    onCancel: () -> Unit,
    onCancelImport: () -> Unit,
    onClearError: () -> Unit,
    onFinishImport: () -> Unit,
    modifier: Modifier = Modifier,
    // ponytail: hoisted accordion state; null = self-managed (legacy callers)
    expanded: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
) {
    val lastFmPurple = Color(0xFF8B5CF6) // Last.fm red
    var username by remember { mutableStateOf("") }
    var internalExpanded by remember { mutableStateOf(false) }
    val isExpanded = expanded ?: internalExpanded

    fun setExpanded(value: Boolean) {
        if (expanded == null) {
            internalExpanded = value
        } else {
            onExpandedChange?.invoke(value)
        }
    }

    // Reset username when state is reset (e.g., when going back from tier selection)
    LaunchedEffect(uiState.discoveryResult, uiState.showTierSelection) {
        if (uiState.discoveryResult == null && !uiState.showTierSelection && !uiState.isImporting) {
            username = ""
        }
    }

    // Auto-expand when there's discovery result or import in progress
    LaunchedEffect(uiState.discoveryResult, uiState.isImporting, uiState.importResult) {
        if (uiState.discoveryResult != null || uiState.isImporting || uiState.importResult != null) {
            setExpanded(true)
        }
    }

    // Handle successful import - don't auto-finish, let user click Continue
    // This avoids race conditions with the button click

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        variant = GlassCardVariant.LowProminence,
        contentPadding = PaddingValues(0.dp),
    ) {
        Column {
            // Header row - always visible
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!uiState.isImporting && !uiState.isLoading) {
                                setExpanded(!isExpanded)
                            }
                        }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(adaptiveSizeByCategory(48.dp, 44.dp, 40.dp))
                            .background(lastFmPurple.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_lastfm),
                        contentDescription = "Last.fm",
                        tint = lastFmPurple,
                        modifier =
                            Modifier
                                .size(24.dp)
                                .offset(y = (-1).dp), // Visual correction
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Last.fm",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Import years of listening history",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Expand/collapse indicator
                if (!uiState.isImporting && !uiState.isLoading) {
                    Icon(
                        imageVector =
                            if (isExpanded) {
                                Icons.Default.KeyboardArrowUp
                            } else {
                                Icons.Default.KeyboardArrowDown
                            },
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Expandable content — clip-reveal in place, same as ExpandableRestoreCard
            AnimatedVisibility(
                visible = isExpanded,
                enter =
                    expandVertically(expandFrom = Alignment.Top,
                        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.IntSize>()) +
                        fadeIn(MaterialTheme.motionScheme.fastEffectsSpec<Float>()),
                exit =
                    shrinkVertically(shrinkTowards = Alignment.Top,
                        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.IntSize>()) +
                        fadeOut(MaterialTheme.motionScheme.fastEffectsSpec<Float>()),
            ) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant)

                    when {
                        // Import complete
                        uiState.importResult != null -> {
                            LastFmImportComplete(
                                result = uiState.importResult!!,
                                onDone = onFinishImport,
                            )
                        }

                        // Import in progress
                        uiState.isImporting -> {
                            LastFmImportProgress(
                                progress = importProgress,
                                onCancel = onCancelImport,
                            )
                        }

                        // Tier selection
                        uiState.showTierSelection && uiState.discoveryResult != null -> {
                            LastFmTierSelection(
                                discovery = uiState.discoveryResult!!,
                                onSelectTier = onSelectTier,
                                onBack = onCancel,
                            )
                        }

                        // Loading/discovering
                        uiState.isLoading -> {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color = lastFmPurple,
                                        modifier = Modifier.size(32.dp),
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Discovering your account...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        // Username input (default state)
                        else -> {
                            LastFmUsernameInput(
                                username = username,
                                onUsernameChange = { newValue ->
                                    username = newValue
                                    if (uiState.error != null) onClearError()
                                },
                                error = uiState.error,
                                onSubmit = { onUsernameSubmit(username.trim()) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LastFmUsernameInput(
    username: String,
    onUsernameChange: (String) -> Unit,
    error: String?,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(16.dp),
    ) {
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Last.fm Username") },
            placeholder = { Text("Enter your username") },
            singleLine = true,
            isError = error != null,
            supportingText =
                if (error != null) {
                    { Text(error, color = MaterialTheme.colorScheme.error) }
                } else {
                    null
                },
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onSubmit,
            enabled = username.isNotBlank(),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8B5CF6),
                    disabledContainerColor = Color(0xFF8B5CF6).copy(alpha = 0.5f),
                ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Connect", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LastFmTierSelection(
    discovery: LastFmImportService.DiscoveryResult,
    onSelectTier: (LastFmImportService.TierConfig) -> Unit,
    onBack: () -> Unit,
) {
    val numberFormat = remember { java.text.NumberFormat.getNumberInstance(java.util.Locale.getDefault()) }

    Column(
        modifier = Modifier.padding(16.dp),
    ) {
        Text(
            text = "Welcome, ${discovery.username}!",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = "${numberFormat.format(discovery.totalScrobbles)} total scrobbles",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your complete history is imported.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Choose which tracks power your leaderboards:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Tier options - using new Quick/Standard/Deep system
        LastFmTierOption(
            name = "Quick",
            description = "Recent 3 months in leaderboards",
            icon = Icons.Default.Bolt,
            isRecommended = false,
            onClick = { onSelectTier(LastFmImportService.Companion.Tiers.QUICK) },
        )

        Spacer(modifier = Modifier.height(8.dp))

        LastFmTierOption(
            name = "Standard",
            description = "Last year + top tracks in leaderboards",
            icon = Icons.Default.Star,
            isRecommended = true, // Standard is recommended for most users
            onClick = { onSelectTier(LastFmImportService.Companion.Tiers.STANDARD) },
        )

        Spacer(modifier = Modifier.height(8.dp))

        LastFmTierOption(
            name = "Deep",
            description = "Last 2 years + more tracks in leaderboards",
            icon = Icons.Default.BarChart,
            isRecommended = false,
            onClick = { onSelectTier(LastFmImportService.Companion.Tiers.DEEP) },
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Explanation text - user-friendly
        Text(
            text = "All your tracks are saved. This just affects chart speed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(
                text = "Use different account",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Native M3 choice surface for the Last.fm import depth. */
@Composable
private fun LastFmTierOption(
    name: String,
    description: String,
    icon: ImageVector,
    isRecommended: Boolean,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        shape = MaterialTheme.shapes.large,
        color = if (isRecommended) scheme.tertiaryContainer else scheme.surfaceContainerLow,
        contentColor = if (isRecommended) scheme.onTertiaryContainer else scheme.onSurface,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.medium,
                color = if (isRecommended) scheme.tertiary else scheme.secondaryContainer,
                contentColor = if (isRecommended) scheme.onTertiary else scheme.onSecondaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(22.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = if (isRecommended) scheme.onTertiaryContainer else scheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isRecommended) scheme.onTertiaryContainer else scheme.onSurfaceVariant,
                )
            }
            if (isRecommended) {
                Surface(
                    color = scheme.tertiary,
                    contentColor = scheme.onTertiary,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text(
                        text = "Recommended",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LastFmImportProgress(
    progress: LastFmImportService.ImportProgress,
    onCancel: () -> Unit,
) {
    val numberFormat = remember { java.text.NumberFormat.getNumberInstance(java.util.Locale.getDefault()) }
    val lastFmRed = Color(0xFF8B5CF6)

    // Extract progress details including tier info
    val progressData =
        when (progress) {
            is LastFmImportService.ImportProgress.Importing -> {
                val percent = if (progress.total > 0) ((progress.current * 100) / progress.total).toInt() else 0
                ProgressData(
                    percent = percent,
                    phase = progress.phase,
                    current = progress.current,
                    total = progress.total,
                    eventsCreated = progress.eventsCreated,
                    archived = progress.archived,
                    isEverythingTier = progress.tierName == "EVERYTHING",
                )
            }

            is LastFmImportService.ImportProgress.Discovering -> {
                ProgressData(0, "Discovering", 0, 0, 0, 0, false)
            }

            is LastFmImportService.ImportProgress.Processing -> {
                ProgressData(0, "Processing", 0, 0, 0, 0, false)
            }

            else -> {
                ProgressData(0, "Preparing", 0, 0, 0, 0, false)
            }
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Circular progress with percentage
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                progress = { (progressData.percent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxSize(),
                color = lastFmRed,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeWidth = 6.dp,
            )

            Text(
                text = "${progressData.percent}%",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Phase indicator
        Text(
            text = progressData.phase,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )

        // Progress text
        if (progressData.total > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${numberFormat.format(progressData.current)} of ${numberFormat.format(progressData.total)} scrobbles",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "This may take a few minutes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live stats row - show different labels based on tier
        if (progressData.isEverythingTier) {
            // Full import: just show total imported
            ImportStatItem(
                label = "Imported",
                value = numberFormat.format(progressData.eventsCreated),
                color = Color(0xFF4CAF50),
            )
        } else {
            // Tiered import: show active vs archived breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ImportStatItem(
                    label = "Active",
                    value = numberFormat.format(progressData.eventsCreated),
                    color = Color(0xFF4CAF50), // Green for active tracks
                )
                ImportStatItem(
                    label = "Archived",
                    value = numberFormat.format(progressData.archived),
                    color = Color(0xFF9E9E9E), // Gray for archived
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cancel button
        TextButton(onClick = onCancel) {
            Text(
                text = "Cancel",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Data class for progress extraction */
private data class ProgressData(
    val percent: Int,
    val phase: String,
    val current: Long,
    val total: Long,
    val eventsCreated: Long,
    val archived: Long,
    val isEverythingTier: Boolean,
)

@Composable
private fun ImportStatItem(
    label: String,
    value: String,
    color: Color,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LastFmImportComplete(
    result: LastFmImportService.ImportResult,
    onDone: () -> Unit,
) {
    val numberFormat = remember { java.text.NumberFormat.getNumberInstance(java.util.Locale.getDefault()) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color(0xFF27AE60),
            modifier = Modifier.size(48.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Import Complete!",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )

        Text(
            // A re-run whose scrobbles are already in the library must not read as a failed
            // import of "0 scrobbles".
            text =
                when {
                    result.totalImported > 0 -> "${numberFormat.format(result.totalImported)} scrobbles imported"

                    result.alreadyImported -> "Nothing new — your history is already in Tempo"

                    else -> "No new scrobbles found this time"
                },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (result.skippedPages > 0) {
            // The import reported success but left holes; say so here too, otherwise this screen
            // tells the user their history is complete when pages were dropped.
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text =
                    "${result.skippedPages} page(s) of history could not be fetched and were " +
                        "skipped. Re-run the import to fill the gaps.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE74C3C),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onDone,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8B5CF6),
                ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Continue", fontWeight = FontWeight.SemiBold)
        }
    }
}

