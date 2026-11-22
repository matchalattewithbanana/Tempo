package me.avinas.tempo.ui.lastfm

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import me.avinas.tempo.R
import me.avinas.tempo.data.lastfm.LastFmImportService
import me.avinas.tempo.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

/**
 * Helper data class for destructuring in ImportProgressContent
 */
internal data class Tuple6<A, B, C, D, E, F>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F,
)

/**
 * Screen for Last.fm import flow.
 * Handles: username input → discovery → tier selection → import progress → completion
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastFmImportScreen(
    onNavigateBack: () -> Unit,
    viewModel: LastFmViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val importProgress by viewModel.importProgress.collectAsState()

    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.settings_import_lastfm),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.settings_back),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                        ),
                )
            },
            containerColor = Color.Transparent,
        ) { paddingValues ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
            ) {
                when {
                    // Show import completion
                    uiState.importResult != null -> {
                        ImportCompletionContent(
                            result = uiState.importResult!!,
                            onDone = {
                                viewModel.reset()
                                onNavigateBack()
                            },
                        )
                    }

                    // Show import progress
                    uiState.isImporting -> {
                        ImportProgressContent(
                            progress = importProgress,
                            onCancel = viewModel::cancelImport,
                        )
                    }

                    // Show tier selection
                    uiState.showTierSelection && uiState.discoveryResult != null -> {
                        TierSelectionContent(
                            discovery = uiState.discoveryResult!!,
                            onSelectTier = viewModel::startImportDirect,
                            onBack = viewModel::reset,
                        )
                    }

                    // Show loading while discovering
                    uiState.isLoading -> {
                        LoadingContent()
                    }

                    // Already connected — show management screen with Sync Now
                    uiState.isConnected && uiState.username != null -> {
                        ConnectedContent(
                            username = uiState.username!!,
                            syncFrequency = uiState.syncFrequency,
                            isSyncing = uiState.isSyncing,
                            lastSyncResult = uiState.lastSyncResult,
                            error = uiState.error,
                            onSyncNow = viewModel::syncNow,
                            onSetFrequency = viewModel::setSyncFrequency,
                            onDisconnect = viewModel::disconnect,
                            onClearError = viewModel::clearError,
                        )
                    }

                    // Show username input
                    else -> {
                        UsernameInputContent(
                            error = uiState.error,
                            onSubmit = viewModel::discoverUser,
                            onClearError = viewModel::clearError,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UsernameInputContent(
    error: String?,
    onSubmit: (String) -> Unit,
    onClearError: () -> Unit,
) {
    var username by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Last.fm Logo/Title
        Icon(
            painter =
                androidx.compose.ui.res
                    .painterResource(id = me.avinas.tempo.R.drawable.ic_lastfm),
            contentDescription = null,
            tint = LastFmRed,
            modifier = Modifier.size(64.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Connect Last.fm",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Import your scrobble history to unlock powerful listening insights",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Username input
        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it
                if (error != null) onClearError()
            },
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
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions =
                KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (username.isNotBlank()) {
                            onSubmit(username.trim())
                        }
                    },
                ),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = LastFmRed,
                    unfocusedBorderColor = GlassBorderMedium,
                    focusedLabelColor = LastFmRed,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = LastFmRed,
                ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Connect button
        Button(
            onClick = {
                focusManager.clearFocus()
                if (username.isNotBlank()) {
                    onSubmit(username.trim())
                }
            },
            enabled = username.isNotBlank(),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = LastFmRed,
                    disabledContainerColor = LastFmRed.copy(alpha = 0.5f),
                ),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                text = "Connect",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info text
        Text(
            text = "We'll analyze your listening history and import recent scrobbles. Your data stays on your device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(
                color = LastFmRed,
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Discovering your Last.fm account...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

