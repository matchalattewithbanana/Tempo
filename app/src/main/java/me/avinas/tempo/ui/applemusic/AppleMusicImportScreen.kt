package me.avinas.tempo.ui.applemusic

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.avinas.tempo.R
import me.avinas.tempo.data.applemusic.AppleMusicImportService
import me.avinas.tempo.ui.applemusic.AppleMusicImportViewModel.State

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleMusicImportScreen(
    onNavigateBack: () -> Unit,
    viewModel: AppleMusicImportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val files by viewModel.files.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
        if (it.isNotEmpty()) viewModel.selectFiles(it)
    }
    val importing = state is State.Importing
    BackHandler(enabled = importing) { viewModel.cancel() }

    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_import_apple_music)) },
                    navigationIcon = {
                        IconButton(onClick = { if (importing) viewModel.cancel() else onNavigateBack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.settings_back),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (val current = state) {
                    State.Ready -> {
                        Text(
                            stringResource(R.string.apple_music_import_instructions),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        TextButton(onClick = { uriHandler.openUri("https://privacy.apple.com/") }) {
                            Text(stringResource(R.string.apple_music_import_open_apple))
                        }
                        OutlinedButton(onClick = { picker.launch(arrayOf("*/*")) }) {
                            Text(stringResource(R.string.apple_music_import_choose_files))
                        }
                        if (files.isNotEmpty()) {
                            Text(
                                stringResource(R.string.apple_music_import_files_selected, files.size),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Button(onClick = viewModel::start, enabled = files.isNotEmpty()) {
                            Text(stringResource(R.string.apple_music_import_start))
                        }
                    }
                    is State.Importing -> {
                        Text(
                            stringResource(R.string.apple_music_import_running),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        ImportSummary(current.summary)
                        OutlinedButton(onClick = viewModel::cancel) {
                            Text(stringResource(R.string.apple_music_import_cancel))
                        }
                    }
                    is State.Complete -> {
                        ImportSummary(current.summary)
                        Button(onClick = onNavigateBack) {
                            Text(stringResource(R.string.apple_music_import_done))
                        }
                        OutlinedButton(onClick = viewModel::reset) {
                            Text(stringResource(R.string.apple_music_import_another))
                        }
                    }
                    is State.Cancelled -> {
                        Text(
                            stringResource(R.string.apple_music_import_cancelled),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        ImportSummary(current.summary)
                        Button(onClick = viewModel::reset) {
                            Text(stringResource(R.string.apple_music_import_another))
                        }
                    }
                    is State.Failed -> {
                        val message = when (current.problem) {
                            AppleMusicImportService.Problem.MISSING_ACTIVITY ->
                                R.string.apple_music_import_missing_activity
                            AppleMusicImportService.Problem.UNSUPPORTED_COLUMNS ->
                                R.string.apple_music_import_unsupported_columns
                            AppleMusicImportService.Problem.TOO_LARGE ->
                                R.string.apple_music_import_too_large
                            AppleMusicImportService.Problem.UNREADABLE ->
                                R.string.apple_music_import_unreadable
                        }
                        Text(stringResource(message), color = MaterialTheme.colorScheme.error)
                        if (current.summary.imported > 0) ImportSummary(current.summary)
                        Button(onClick = viewModel::reset) {
                            Text(stringResource(R.string.apple_music_import_another))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportSummary(summary: AppleMusicImportService.Summary) {
    val color = MaterialTheme.colorScheme.onSurface
    Text(stringResource(R.string.apple_music_import_imported, summary.imported), color = color)
    if (summary.duplicates > 0) {
        Text(stringResource(R.string.apple_music_import_duplicates, summary.duplicates), color = color)
    }
    if (summary.shortPlays > 0) {
        Text(stringResource(R.string.apple_music_import_short, summary.shortPlays), color = color)
    }
    if (summary.missingArtist > 0) {
        Text(stringResource(R.string.apple_music_import_missing_artist, summary.missingArtist), color = color)
    }
    if (summary.invalid > 0) {
        Text(stringResource(R.string.apple_music_import_invalid, summary.invalid), color = color)
    }
}
