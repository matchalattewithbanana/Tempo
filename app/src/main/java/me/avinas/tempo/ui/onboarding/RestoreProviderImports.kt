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
fun SpotifyJsonImportCard(
    uiState: SpotifyJsonImportUiState,
    importState: SpotifyJsonImportService.ImportState,
    onImport: (List<Uri>) -> Unit,
    onReset: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    // ponytail: hoisted accordion state; null = self-managed (legacy callers)
    expanded: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
) {
    val spotifyGreen = Color(0xFF1DB954)
    var internalExpanded by remember { mutableStateOf(false) }
    val isExpanded = expanded ?: internalExpanded

    fun setExpanded(value: Boolean) {
        if (expanded == null) {
            internalExpanded = value
        } else {
            onExpandedChange?.invoke(value)
        }
    }
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val filePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenMultipleDocuments(),
        ) { uris ->
            selectedUris = uris
            if (uris.isNotEmpty()) {
                setExpanded(true)
            }
        }

    LaunchedEffect(uiState) {
        if (uiState is SpotifyJsonImportUiState.Importing || uiState is SpotifyJsonImportUiState.Completed) {
            setExpanded(true)
        }
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        variant = GlassCardVariant.LowProminence,
        contentPadding = PaddingValues(0.dp),
    ) {
        Column {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (uiState !is SpotifyJsonImportUiState.Importing) {
                                setExpanded(!isExpanded)
                            }
                        }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(adaptiveSizeByCategory(48.dp, 44.dp, 40.dp))
                            .background(spotifyGreen.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.spotify),
                        contentDescription = null,
                        modifier = Modifier.size(adaptiveSizeByCategory(28.dp, 26.dp, 24.dp)),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Spotify Data Export",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Import from JSON files",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (uiState !is SpotifyJsonImportUiState.Importing) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

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
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    when (uiState) {
                        is SpotifyJsonImportUiState.Idle -> {
                            Text(
                                text =
                                    buildAnnotatedString {
                                        append("Get your data from ")
                                        withLink(
                                            LinkAnnotation.Url(
                                                url = "https://spotify.com/account/privacy",
                                                styles =
                                                    TextLinkStyles(
                                                        style =
                                                            SpanStyle(
                                                                color = Color(0xFF1DB954),
                                                                textDecoration = TextDecoration.Underline,
                                                            ),
                                                    ),
                                            ),
                                        ) {
                                            append("spotify.com/account/privacy")
                                        }
                                        append(", then select the JSON files. You can also do this later from Settings.")
                                    },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    filePickerLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = spotifyGreen),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Select JSON Files")
                            }

                            if (selectedUris.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${selectedUris.size} file(s) selected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = spotifyGreen,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { onImport(selectedUris) },
                                    colors = ButtonDefaults.buttonColors(containerColor = spotifyGreen),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text("Start Import", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        is SpotifyJsonImportUiState.Importing -> {
                            val (message, progress) =
                                when (importState) {
                                    is SpotifyJsonImportService.ImportState.Parsing -> {
                                        "Parsing ${importState.fileName}..." to
                                            (importState.filesProcessed.toFloat() / importState.totalFiles.coerceAtLeast(1))
                                    }

                                    is SpotifyJsonImportService.ImportState.Importing -> {
                                        "Importing ${importState.current}/${importState.total}" to
                                            (importState.current.toFloat() / importState.total.coerceAtLeast(1))
                                    }

                                    else -> {
                                        "Preparing..." to 0f
                                    }
                                }

                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth(),
                                color = spotifyGreen,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            )
                        }

                        is SpotifyJsonImportUiState.Completed -> {
                            val result = uiState.result
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF27AE60),
                                modifier = Modifier.size(40.dp),
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Import Complete!",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                            )

                            Text(
                                text = "${result.tracksImported} tracks, ${result.eventsCreated} events",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = onFinish,
                                colors = ButtonDefaults.buttonColors(containerColor = spotifyGreen),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("Continue", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        is SpotifyJsonImportUiState.Error -> {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = Color(0xFFE74C3C),
                                modifier = Modifier.size(40.dp),
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = uiState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    onClick = onReset,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Retry")
                                }
                                Button(
                                    onClick = {
                                        selectedUris = emptyList()
                                        onReset()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = spotifyGreen),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Select Files")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun YouTubeMusicImportCard(
    uiState: YouTubeMusicImportUiState,
    importState: YouTubeMusicImportService.ImportState,
    onImport: (List<Uri>) -> Unit,
    onReset: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    // ponytail: hoisted accordion state; null = self-managed (legacy callers)
    expanded: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
) {
    val youTubeRed = Color(0xFFFF0000)
    var internalExpanded by remember { mutableStateOf(false) }
    var showGuide by remember { mutableStateOf(false) }
    val isExpanded = expanded ?: internalExpanded

    fun setExpanded(value: Boolean) {
        if (expanded == null) {
            internalExpanded = value
        } else {
            onExpandedChange?.invoke(value)
        }
    }
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val filePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenMultipleDocuments(),
        ) { uris ->
            selectedUris = uris
            if (uris.isNotEmpty()) {
                setExpanded(true)
                onImport(uris)
            }
        }

    LaunchedEffect(uiState) {
        if (uiState is YouTubeMusicImportUiState.Importing || uiState is YouTubeMusicImportUiState.Completed) {
            setExpanded(true)
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is YouTubeMusicImportUiState.Completed) {
            kotlinx.coroutines.delay(2000)
            onFinish()
        }
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        variant = GlassCardVariant.LowProminence,
        contentPadding = PaddingValues(0.dp),
    ) {
        Column {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (uiState !is YouTubeMusicImportUiState.Importing) {
                                setExpanded(!isExpanded)
                            }
                        }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(adaptiveSizeByCategory(48.dp, 44.dp, 40.dp))
                            .background(youTubeRed.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = youTubeRed,
                        modifier = Modifier.size(adaptiveSizeByCategory(28.dp, 26.dp, 24.dp)),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "YouTube Music Takeout",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Import from YouTube Takeout (ZIP or JSON)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (uiState !is YouTubeMusicImportUiState.Importing) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

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
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    when (uiState) {
                        is YouTubeMusicImportUiState.Idle -> {
                            Text(
                                text =
                                    buildAnnotatedString {
                                        append("Get your data from ")
                                        withLink(
                                            LinkAnnotation.Url(
                                                url = "https://takeout.google.com",
                                                styles =
                                                    TextLinkStyles(
                                                        style = SpanStyle(color = youTubeRed, textDecoration = TextDecoration.Underline),
                                                    ),
                                            ),
                                        ) {
                                            append("takeout.google.com")
                                        }
                                        append(
                                            ", deselect all → select only \"YouTube and YouTube Music\" → only \"history\" → JSON format. Download the ZIP and select it here directly (if Takeout split the export into several ZIPs, select all of them). You can also do this later from Settings.",
                                        )
                                    },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            TextButton(
                                onClick = { showGuide = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = youTubeRed,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "See how to do it",
                                    color = youTubeRed,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = {
                                    filePickerLauncher.launch(
                                        arrayOf("application/zip", "application/x-zip", "application/json", "text/plain", "*/*"),
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = youTubeRed),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Select File")
                            }
                        }

                        is YouTubeMusicImportUiState.Importing -> {
                            val (message, progress) =
                                when (importState) {
                                    is YouTubeMusicImportService.ImportState.Parsing -> {
                                        "Parsing ${importState.fileName}..." to
                                            (importState.filesProcessed.toFloat() / importState.totalFiles.coerceAtLeast(1))
                                    }

                                    is YouTubeMusicImportService.ImportState.Importing -> {
                                        "Importing ${importState.current}/${importState.total}" to
                                            (importState.current.toFloat() / importState.total.coerceAtLeast(1))
                                    }

                                    else -> {
                                        "Preparing..." to 0f
                                    }
                                }

                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth(),
                                color = youTubeRed,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            )
                        }

                        is YouTubeMusicImportUiState.Completed -> {
                            val result = uiState.result
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF27AE60),
                                modifier = Modifier.size(40.dp),
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Import Complete!",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                            )

                            Text(
                                text = "${result.tracksImported} tracks, ${result.eventsCreated} events",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Continuing automatically...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        is YouTubeMusicImportUiState.Error -> {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = Color(0xFFE74C3C),
                                modifier = Modifier.size(40.dp),
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = uiState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    onClick = onReset,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Retry")
                                }
                                Button(
                                    onClick = {
                                        selectedUris = emptyList()
                                        onReset()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = youTubeRed),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Select Files")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (showGuide) {
                TakeoutGuideDialog(onDismiss = { showGuide = false })
            }
        }
    }
}
