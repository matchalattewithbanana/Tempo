package me.avinas.tempo.ui.appearance

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.avinas.tempo.R
import me.avinas.tempo.ui.components.ShareTheme
import me.avinas.tempo.ui.components.shareThemeLabel
import me.avinas.tempo.ui.stats.statsDurationLabel
import me.avinas.tempo.ui.stats.statsDurationPrecisionLabel
import me.avinas.tempo.ui.stats.formatStatsDuration
import me.avinas.tempo.ui.stats.statsSummaryLabel
import me.avinas.tempo.ui.components.appearanceBackdropBrush
import me.avinas.tempo.ui.components.GlassCard
import me.avinas.tempo.ui.theme.AppFontFamily
import me.avinas.tempo.ui.theme.DisplayFontFamily
import me.avinas.tempo.ui.theme.LocalTempoPalette
import me.avinas.tempo.ui.theme.TempoTheme

private val NewAppearanceEditions = listOf(
    AppearancePreset.MIDNIGHT_BLOOM,
    AppearancePreset.SAKURA,
    AppearancePreset.MATCHA,
    AppearancePreset.COBALT,
    AppearancePreset.PORCELAIN,
    AppearancePreset.TERRACOTTA,
    AppearancePreset.CITRINE,
    AppearancePreset.MOCHA,
    AppearancePreset.LAGOON,
    AppearancePreset.LILAC,
)
private val SignatureAppearancePresets = AppearancePreset.entries.filterNot { it in NewAppearanceEditions }

private val DraftSaver = Saver<AppearanceSettings, String>(
    save = { AppearanceCodec.encode(it) },
    restore = { runCatching { AppearanceCodec.decode(it) }.getOrDefault(AppearanceSettings()) },
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppearanceScreen(onNavigateBack: () -> Unit, viewModel: AppearanceViewModel = hiltViewModel()) {
    val loaded by viewModel.settings.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val saveFailed by viewModel.saveFailed.collectAsStateWithLifecycle()
    val looks by viewModel.savedLooks.collectAsStateWithLifecycle()
    val saved = loaded
    if (saved == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
            IconButton(onClick = onNavigateBack, modifier = Modifier.align(Alignment.TopStart).statusBarsPadding()) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.settings_back))
            }
        }
        return
    }
    var draft by rememberSaveable(saved, stateSaver = DraftSaver) { mutableStateOf(saved) }
    var presetsExpanded by rememberSaveable { mutableStateOf(true) }
    var applySuggestedDetails by rememberSaveable { mutableStateOf(true) }
    val columns = if (LocalConfiguration.current.screenWidthDp < 350 ||
        LocalDensity.current.fontScale >= 1.35f) 1 else 2
    val newRows = remember(columns) { NewAppearanceEditions.chunked(columns) }
    val signatureRows = remember(columns) { SignatureAppearancePresets.chunked(columns) }
    var lookName by rememberSaveable { mutableStateOf("") }
    var transferFailed by rememberSaveable { mutableStateOf(false) }
    var transferDone by rememberSaveable { mutableStateOf(false) }
    var transferBusy by remember { mutableStateOf(false) }
    var exportSnapshot by rememberSaveable { mutableStateOf<String?>(null) }
    val busy = saving || transferBusy
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val snapshot = exportSnapshot
        exportSnapshot = null
        if (uri != null && snapshot != null) {
            transferBusy = true
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        requireNotNull(context.contentResolver.openOutputStream(uri, "wt")).use { it.write(snapshot.toByteArray(Charsets.UTF_8)) }
                    }
                    transferDone = true
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled } catch (failure: Exception) { transferFailed = true } finally { transferBusy = false }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            transferBusy = true
            scope.launch {
                try {
                    val imported = withContext(Dispatchers.IO) {
                        requireNotNull(context.contentResolver.openInputStream(uri)).use { input ->
                            val bytes = java.io.ByteArrayOutputStream()
                            val buffer = ByteArray(4096)
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                require(bytes.size() + count <= AppearanceCodec.MAX_BYTES)
                                bytes.write(buffer, 0, count)
                            }
                            AppearanceCodec.decode(bytes.toString("UTF-8")).forLiveApp()
                        }
                    }
                    draft = imported
                    transferDone = true
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled } catch (failure: Exception) { transferFailed = true } finally { transferBusy = false }
            }
        }
    }
    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    var exitApplyTarget by remember { mutableStateOf<AppearanceSettings?>(null) }
    val requestBack: () -> Unit = {
        if (!busy && exitApplyTarget == null) {
            if (draft == saved) onNavigateBack() else showExitDialog = true
        }
    }
    BackHandler(onBack = requestBack)
    LaunchedEffect(saved, draft, saving, saveFailed, exitApplyTarget) {
        val target = exitApplyTarget
        if (target == null) {
            if (draft == saved && !saving) showExitDialog = false
            return@LaunchedEffect
        }
        if (!saving && saveFailed) {
            exitApplyTarget = null
        } else if (!saving && saved == target) {
            exitApplyTarget = null
            showExitDialog = false
            onNavigateBack()
        }
    }
    if (showExitDialog) {
        val exitBusy = busy || exitApplyTarget != null
        AlertDialog(
            onDismissRequest = { if (!exitBusy) showExitDialog = false },
            title = { Text(stringResource(R.string.appearance_exit_title)) },
            text = { Text(stringResource(if (saveFailed) R.string.appearance_save_failed else R.string.appearance_exit_body)) },
            confirmButton = {
                TextButton(enabled = !exitBusy, onClick = {
                    exitApplyTarget = draft.forLiveApp()
                    viewModel.apply(draft)
                }) { Text(stringResource(R.string.appearance_apply)) }
            },
            dismissButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(enabled = !exitBusy, onClick = onNavigateBack) {
                        Text(stringResource(R.string.canvas_draft_discard))
                    }
                    TextButton(enabled = !exitBusy, onClick = { showExitDialog = false }) {
                        Text(stringResource(R.string.canvas_draft_keep_editing))
                    }
                }
            },
        )
    }

    val preview = draft
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(
            title = { Text(stringResource(R.string.appearance_title)) },
            navigationIcon = { IconButton(onClick = requestBack, enabled = !busy && exitApplyTarget == null) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.settings_back))
            } },
        ) },
        bottomBar = {
            if (draft != saved) Surface(shadowElevation = 8.dp) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { draft = saved }, enabled = !busy, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.appearance_cancel))
                    }
                    Button(onClick = { viewModel.apply(draft) }, enabled = !busy, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.appearance_apply))
                    }
                }
            }
        },
    ) { padding ->
        // Compose only the visible studio sections. Previously the entire studio,
        // including presets, fonts and saved looks, was composed on every preference edit.
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "preview") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.appearance_gallery_heading), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.appearance_gallery_description), style = MaterialTheme.typography.bodyMedium)
            ChoiceRow(R.string.appearance_tone_heading, AppearanceTone.entries, draft.tone, !busy,
                { when (it) {
                    AppearanceTone.DARK -> R.string.appearance_dark
                    AppearanceTone.OLED -> R.string.appearance_oled
                    AppearanceTone.LIGHT -> R.string.appearance_light
                    AppearanceTone.SYSTEM -> R.string.appearance_system
                } }) {
                draft = draft.copy(tone = it)
            }
            ChoiceRow(R.string.appearance_accent_source, AppearanceAccentSource.entries.filter {
                it != AppearanceAccentSource.WALLPAPER || Build.VERSION.SDK_INT >= 31
            }, draft.accentSource, !busy, { when (it) {
                AppearanceAccentSource.PRESET -> R.string.appearance_preset_accent
                AppearanceAccentSource.WALLPAPER -> R.string.appearance_wallpaper
                AppearanceAccentSource.ARTWORK -> R.string.appearance_artwork
                AppearanceAccentSource.CUSTOM -> R.string.appearance_custom_accent
            } }) {
                draft = draft.copy(
                    accentSource = it,
                    customAccent = if (it == AppearanceAccentSource.CUSTOM) {
                        draft.customAccent ?: AccentSwatches.first().first
                    } else draft.customAccent,
                )
            }
            when (draft.accentSource) {
                AppearanceAccentSource.CUSTOM -> CustomAccentPicker(draft.customAccent, enabled = !busy) {
                    draft = draft.copy(customAccent = it)
                }
                AppearanceAccentSource.ARTWORK -> Text(
                    stringResource(R.string.appearance_artwork_scope),
                    style = MaterialTheme.typography.bodySmall,
                )
                else -> Unit
            }
            ChoiceRow(R.string.appearance_backdrop, AppearanceBackdrop.entries, draft.backdrop, !busy, { when (it) {
                AppearanceBackdrop.GRADIENT -> R.string.appearance_backdrop_gradient
                AppearanceBackdrop.FLAT -> R.string.appearance_backdrop_flat
                AppearanceBackdrop.WASH -> R.string.appearance_backdrop_wash
            } }) { draft = draft.copy(backdrop = it) }
            // Actual card/text tokens, isolated from saved app state and system bars.
            TempoTheme(appearance = preview, preview = true) {
                val palette = LocalTempoPalette.current
                val brush = remember(palette, preview.backdrop) { appearanceBackdropBrush(palette, preview.backdrop) }
                Surface(shape = RoundedCornerShape(22.dp), color = palette.background) {
                    Column(
                        Modifier.fillMaxWidth().background(brush).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(stringResource(R.string.appearance_preview_song), style = MaterialTheme.typography.headlineSmall)
                        GlassCard {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(stringResource(R.string.appearance_preview_artist), style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface)
                                LinearProgressIndicator(progress = { 0.64f }, modifier = Modifier.fillMaxWidth())
                                Text(stringResource(R.string.appearance_preview_pending), style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
                }
            }
            item(key = "preset_collection_header") {
                TextButton(onClick = { presetsExpanded = !presetsExpanded },
                    modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.appearance_presets), modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium)
                    Text(if (presetsExpanded) "−" else "+")
                }
            }
            if (presetsExpanded) {
                item(key = "new_edition_heading") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.appearance_new_editions),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.appearance_gallery_hint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall)
                        SettingToggle(R.string.appearance_apply_curated_style,
                            applySuggestedDetails, !busy) { applySuggestedDetails = it }
                    }
                }
                items(count = newRows.size, key = { "new_preset_$it" }) { index ->
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        newRows[index].forEach { preset ->
                            AppearancePresetCard(preset, saved.preset == preset, draft.preset == preset,
                                onClick = { draft = draft.withCuratedPreset(preset, applySuggestedDetails) },
                                modifier = Modifier.weight(1f), tone = preview.tone, enabled = !busy)
                        }
                    }
                }
                item(key = "signature_edition_heading") {
                    Text(stringResource(R.string.appearance_signature_editions),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleSmall)
                }
                items(count = signatureRows.size, key = { "signature_preset_$it" }) { index ->
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        signatureRows[index].forEach { preset ->
                            AppearancePresetCard(preset, saved.preset == preset, draft.preset == preset,
                                onClick = { draft = draft.withCuratedPreset(preset, applySuggestedDetails) },
                                modifier = Modifier.weight(1f), tone = preview.tone, enabled = !busy)
                        }
                    }
                }
            }
            item(key = "layout") {
                AppearanceSection(R.string.appearance_layout_heading) {
                BoxWithConstraints {
                    val columns = if (maxWidth < 290.dp || LocalDensity.current.fontScale >= 1.4f) 1 else 2
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppearanceFontStyle.entries.chunked(columns).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { style ->
                                    FontStyleCard(
                                        style, draft.fontStyle == style,
                                        onClick = { if (!busy) draft = draft.copy(fontStyle = style) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
                ChoiceRow(R.string.appearance_density, AppearanceDensity.entries, draft.density, !busy,
                    { if (it == AppearanceDensity.COMPACT) R.string.appearance_compact else R.string.appearance_comfortable }) { draft = draft.copy(density = it) }
                ChoiceRow(R.string.appearance_corners, AppearanceShape.entries, draft.shape, !busy, { when (it) {
                    AppearanceShape.SOFT -> R.string.appearance_soft
                    AppearanceShape.MODERATE -> R.string.appearance_moderate
                    AppearanceShape.SHARP -> R.string.appearance_sharp
                } }) { draft = draft.copy(shape = it) }
                SettingToggle(R.string.appearance_bold, draft.boldText, !busy) { draft = draft.copy(boldText = it) }
            }
            }
            item(key = "effects") {
                AppearanceSection(R.string.appearance_effects_heading) {
                ChoiceRow(R.string.appearance_finish, AppearanceSurface.entries, draft.surface, !busy,
                    { if (it == AppearanceSurface.SOLID) R.string.appearance_solid else R.string.appearance_satin }) { draft = draft.copy(surface = it) }
                SettingToggle(R.string.appearance_reduce_motion, draft.reduceMotion, !busy) { draft = draft.copy(reduceMotion = it) }
                SettingToggle(R.string.appearance_instant_navigation, draft.instantNavigation, !busy) {
                    draft = draft.copy(instantNavigation = it)
                }
                SettingToggle(R.string.appearance_navigation_labels, draft.showNavigationLabels, !busy) {
                    draft = draft.copy(showNavigationLabels = it)
                }
                SettingToggle(R.string.appearance_adaptive, draft.adaptiveEffects, !busy) { draft = draft.copy(adaptiveEffects = it) }
                Text(stringResource(R.string.appearance_effect_policy), style = MaterialTheme.typography.bodySmall)
            }
            }
            item(key = "arrangement") {
                AppearanceSection(R.string.appearance_arrangement) {
                ChoiceRow(R.string.appearance_now_listening, AppearanceNowListening.entries, draft.nowListening, !busy, { when (it) {
                    AppearanceNowListening.INLINE -> R.string.appearance_inline
                    AppearanceNowListening.COMPACT -> R.string.appearance_compact
                    AppearanceNowListening.HIDDEN -> R.string.appearance_hidden
                } }) { draft = draft.copy(nowListening = it) }
                ChoiceRow(
                    R.string.appearance_home_start_period, AppearanceStatsRange.entries, draft.homeStartPeriod, !busy,
                    ::appearancePeriodLabel,
                ) { draft = draft.copy(homeStartPeriod = it) }
                SettingToggle(R.string.appearance_home_summary_chart, draft.showHomeSummaryChart, !busy) {
                    draft = draft.copy(showHomeSummaryChart = it)
                }
                Text(stringResource(R.string.appearance_home_order), style = MaterialTheme.typography.titleSmall)
                draft.homeOrder.forEachIndexed { index, section ->
                    val sectionName = stringResource(sectionLabel(section))
                    val moveLabel = stringResource(R.string.appearance_move_up)
                    val moveDownLabel = stringResource(R.string.appearance_move_down)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(modifier = Modifier.semantics { contentDescription = sectionName }, checked = section !in draft.hiddenHomeSections, enabled = !busy, onCheckedChange = { visible ->
                            draft = draft.copy(hiddenHomeSections = if (visible) draft.hiddenHomeSections - section else draft.hiddenHomeSections + section)
                        })
                        Text(stringResource(sectionLabel(section)), modifier = Modifier.weight(1f))
                        TextButton(modifier = Modifier.semantics { contentDescription = "$moveLabel: $sectionName" }, enabled = !busy && index > 0, onClick = {
                            val order = draft.homeOrder.toMutableList()
                            order[index] = order[index - 1]; order[index - 1] = section
                            draft = draft.copy(homeOrder = order)
                        }) { Text(moveLabel) }
                        TextButton(
                            modifier = Modifier.semantics { contentDescription = "$moveDownLabel: $sectionName" },
                            enabled = !busy && index < draft.homeOrder.lastIndex,
                            onClick = {
                                val order = draft.homeOrder.toMutableList()
                                order[index] = order[index + 1]
                                order[index + 1] = section
                                draft = draft.copy(homeOrder = order)
                            },
                        ) { Text(moveDownLabel) }
                    }
                }
                TextButton(
                    enabled = !busy && (draft.homeOrder != AppearanceHomeSection.entries.toList() ||
                        draft.hiddenHomeSections.isNotEmpty()),
                    onClick = {
                        draft = draft.copy(homeOrder = AppearanceHomeSection.entries.toList(), hiddenHomeSections = emptySet())
                    },
                ) { Text(stringResource(R.string.appearance_reset_home_cards)) }
            }
            }
            item(key = "stats") {
                AppearanceSection(R.string.appearance_stats_settings) {
                ChoiceRow(R.string.appearance_stats_start, AppearanceStatsTab.entries, draft.statsTab, !busy, { when (it) {
                    AppearanceStatsTab.SONGS -> R.string.appearance_songs
                    AppearanceStatsTab.ARTISTS -> R.string.appearance_artists
                    AppearanceStatsTab.ALBUMS -> R.string.appearance_albums
                } }) { draft = draft.copy(statsTab = it) }
                ChoiceRow(
                    R.string.appearance_stats_period, AppearanceStatsRange.entries, draft.statsRange, !busy,
                    ::appearancePeriodLabel,
                ) { draft = draft.copy(statsRange = it) }
                ChoiceRow(R.string.appearance_stats_sort, AppearanceStatsSort.entries, draft.statsSort, !busy, { when (it) {
                    AppearanceStatsSort.COMBINED -> R.string.stats_sort_combined
                    AppearanceStatsSort.PLAYS -> R.string.stats_sort_play_count
                    AppearanceStatsSort.TIME -> R.string.stats_sort_total_time
                } }) { draft = draft.copy(statsSort = it) }
                ChoiceRow(
                    R.string.stats_duration_display, AppearanceStatsDuration.entries,
                    draft.statsDuration, !busy, ::statsDurationLabel,
                ) { draft = draft.copy(statsDuration = it) }
                Text(
                    stringResource(R.string.stats_duration_preview,
                        formatStatsDuration(context, 35_397_000L, draft.statsDuration, draft.statsDurationPrecision)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (draft.statsDuration == AppearanceStatsDuration.MINUTES ||
                    draft.statsDuration == AppearanceStatsDuration.HOURS ||
                    draft.statsDuration == AppearanceStatsDuration.DAYS) {
                    ChoiceRow(R.string.stats_duration_precision, AppearanceDurationPrecision.entries,
                        draft.statsDurationPrecision, !busy, ::statsDurationPrecisionLabel,
                    ) { draft = draft.copy(statsDurationPrecision = it) }
                    Text(stringResource(R.string.stats_duration_decimal_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ChoiceRow(
                    R.string.stats_summary_count, AppearanceStatsSummary.entries,
                    draft.statsSummary, !busy && draft.showStatsSummary, ::statsSummaryLabel,
                ) { draft = draft.copy(statsSummary = it) }
                SettingToggle(R.string.appearance_stats_summary, draft.showStatsSummary, !busy) {
                    draft = draft.copy(showStatsSummary = it)
                }
                SettingToggle(R.string.appearance_stats_artwork, draft.showStatsArtwork, !busy) {
                    draft = draft.copy(showStatsArtwork = it)
                }
                SettingToggle(R.string.appearance_stats_highlight, draft.highlightTopResult, !busy) {
                    draft = draft.copy(highlightTopResult = it)
                }
            }
            }
            item(key = "history") {
                AppearanceSection(R.string.nav_history) {
                SettingToggle(R.string.appearance_history_artwork, draft.showHistoryArtwork, !busy) {
                    draft = draft.copy(showHistoryArtwork = it)
                }
            }
            }
            item(key = "profile") {
                AppearanceSection(R.string.appearance_profile_settings) {
                ChoiceRow(R.string.appearance_profile_start, AppearanceProfileTab.entries, draft.profileTab, !busy, { when (it) {
                    AppearanceProfileTab.CHALLENGES -> R.string.share_profile_challenges
                    AppearanceProfileTab.BADGES -> R.string.profile_badges
                } }) { draft = draft.copy(profileTab = it) }
                SettingToggle(R.string.appearance_profile_now_playing, draft.showProfileNowPlaying, !busy) {
                    draft = draft.copy(showProfileNowPlaying = it)
                }
                SettingToggle(R.string.appearance_profile_progress, draft.showProfileProgress, !busy) {
                    draft = draft.copy(showProfileProgress = it)
                }
                SettingToggle(R.string.appearance_profile_streak_warning, draft.showProfileStreakWarning, !busy) {
                    draft = draft.copy(showProfileStreakWarning = it)
                }
            }
            }
            item(key = "sharing") {
                AppearanceSection(R.string.appearance_sharing_settings) {
                ChoiceRow(
                    R.string.appearance_share_theme, ShareTheme.entries, draft.defaultShareTheme, !busy, ::shareThemeLabel,
                ) { draft = draft.copy(defaultShareTheme = it) }
            }
            }
            item(key = "saved_looks") {
                AppearanceSection(R.string.appearance_saved_looks) {
                OutlinedTextField(value = lookName, onValueChange = { lookName = it.take(40) }, enabled = !busy,
                    label = { Text(stringResource(R.string.appearance_look_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedButton(enabled = !busy && lookName.isNotBlank() && (looks.size < 12 || lookName.trim() in looks),
                    onClick = { viewModel.saveLook(lookName, draft) }) { Text(stringResource(R.string.appearance_save_look)) }
                looks.forEach { (name, look) -> Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(enabled = !busy, onClick = { draft = look }, modifier = Modifier.weight(1f)) { Text(name) }
                    TextButton(enabled = !busy, onClick = { viewModel.deleteLook(name) }) { Text(stringResource(R.string.appearance_delete_look)) }
                } }
                Text(stringResource(R.string.appearance_transfer_hint), style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(enabled = !busy, onClick = { transferDone = false; transferFailed = false; importLauncher.launch(arrayOf("application/json", "text/plain")) }) {
                        Text(stringResource(R.string.appearance_import))
                    }
                    OutlinedButton(enabled = !busy, onClick = {
                        transferDone = false; transferFailed = false
                        exportSnapshot = AppearanceCodec.encode(draft)
                        exportLauncher.launch("tempo-appearance.json")
                    }) { Text(stringResource(R.string.appearance_export)) }
                }
            }
            }
            item(key = "restore_and_status") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(enabled = !busy, onClick = { draft = AppearanceSettings() }) {
                Text(stringResource(R.string.appearance_restore))
            }
            if (saveFailed) Text(stringResource(R.string.appearance_save_failed), color = MaterialTheme.colorScheme.error)
            if (transferFailed) Text(stringResource(R.string.appearance_transfer_failed), color = MaterialTheme.colorScheme.error)
            if (transferDone) Text(stringResource(R.string.appearance_transfer_done))
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun AppearanceSection(
    title: Int,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainerLow,
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.65f)),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .heightIn(min = 60.dp)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(title), modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold, color = colors.onSurface,
                )
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null, tint = colors.onSurfaceVariant,
                )
            }
            if (expanded) {
                HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.6f))
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) { content() }
            }
        }
    }
}

@Composable
private fun SettingToggle(label: Int, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    val labelText = stringResource(label)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(labelText, modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface)
        Switch(modifier = Modifier.semantics { contentDescription = labelText },
            checked = checked, enabled = enabled, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChoiceRow(label: Int, options: List<T>, selected: T, enabled: Boolean,
    text: (T) -> Int, onSelect: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(label), style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        val colors = MaterialTheme.colorScheme
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEach { option ->
                val isSelected = selected == option
                FilterChip(
                    selected = isSelected, enabled = enabled,
                    onClick = { onSelect(option) },
                    shape = MaterialTheme.shapes.small,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = colors.surfaceContainer,
                        labelColor = colors.onSurfaceVariant,
                        selectedContainerColor = colors.secondaryContainer,
                        selectedLabelColor = colors.onSecondaryContainer,
                    ),
                    label = { Text(stringResource(text(option)),
                        style = MaterialTheme.typography.labelLarge) },
                )
            }
        }
    }
}

private fun sectionLabel(section: AppearanceHomeSection): Int = when (section) {
    AppearanceHomeSection.SUMMARY -> R.string.appearance_summary
    AppearanceHomeSection.SPOTLIGHT -> R.string.appearance_spotlight
    AppearanceHomeSection.QUICK_STATS -> R.string.appearance_quick_stats
    AppearanceHomeSection.TODAY -> R.string.period_today
    AppearanceHomeSection.INSIGHTS -> R.string.appearance_insights
}

@Composable
private fun AppearancePresetCard(
    preset: AppearancePreset,
    active: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: AppearanceTone = AppearanceTone.DARK,
    enabled: Boolean = true,
) {
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val palette = remember(preset, tone, systemDark) {
        AppearancePaletteResolver.resolve(AppearanceSettings(preset = preset, tone = tone), systemDark)
    }
    val shape = RoundedCornerShape(22.dp)
    val label = when (preset) {
        AppearancePreset.ABYSS -> stringResource(R.string.appearance_abyss)
        AppearancePreset.OBSIDIAN_GOLD -> stringResource(R.string.appearance_gold)
        AppearancePreset.AURORA -> stringResource(R.string.appearance_aurora)
        AppearancePreset.GLACIER -> stringResource(R.string.appearance_glacier)
        AppearancePreset.EMBER -> stringResource(R.string.appearance_ember)
        AppearancePreset.SOLSTICE -> stringResource(R.string.appearance_solstice)
        AppearancePreset.VERDANT -> stringResource(R.string.appearance_verdant)
        AppearancePreset.SLATE -> stringResource(R.string.appearance_slate)
        AppearancePreset.MIDNIGHT_BLOOM -> stringResource(R.string.appearance_midnight_bloom)
        AppearancePreset.SAKURA -> stringResource(R.string.appearance_sakura)
        AppearancePreset.MATCHA -> stringResource(R.string.appearance_matcha)
        AppearancePreset.COBALT -> stringResource(R.string.appearance_cobalt)
        AppearancePreset.PORCELAIN -> stringResource(R.string.appearance_porcelain)
        AppearancePreset.TERRACOTTA -> stringResource(R.string.appearance_terracotta)
        AppearancePreset.CITRINE -> stringResource(R.string.appearance_citrine)
        AppearancePreset.MOCHA -> stringResource(R.string.appearance_mocha)
        AppearancePreset.LAGOON -> stringResource(R.string.appearance_lagoon)
        AppearancePreset.LILAC -> stringResource(R.string.appearance_lilac)
    }
    val description = when (preset) {
        AppearancePreset.ABYSS -> stringResource(R.string.appearance_abyss_desc)
        AppearancePreset.OBSIDIAN_GOLD -> stringResource(R.string.appearance_gold_desc)
        AppearancePreset.AURORA -> stringResource(R.string.appearance_aurora_desc)
        AppearancePreset.GLACIER -> stringResource(R.string.appearance_glacier_desc)
        AppearancePreset.EMBER -> stringResource(R.string.appearance_ember_desc)
        AppearancePreset.SOLSTICE -> stringResource(R.string.appearance_solstice_desc)
        AppearancePreset.VERDANT -> stringResource(R.string.appearance_verdant_desc)
        AppearancePreset.SLATE -> stringResource(R.string.appearance_slate_desc)
        AppearancePreset.MIDNIGHT_BLOOM -> stringResource(R.string.appearance_midnight_bloom_desc)
        AppearancePreset.SAKURA -> stringResource(R.string.appearance_sakura_desc)
        AppearancePreset.MATCHA -> stringResource(R.string.appearance_matcha_desc)
        AppearancePreset.COBALT -> stringResource(R.string.appearance_cobalt_desc)
        AppearancePreset.PORCELAIN -> stringResource(R.string.appearance_porcelain_desc)
        AppearancePreset.TERRACOTTA -> stringResource(R.string.appearance_terracotta_desc)
        AppearancePreset.CITRINE -> stringResource(R.string.appearance_citrine_desc)
        AppearancePreset.MOCHA -> stringResource(R.string.appearance_mocha_desc)
        AppearancePreset.LAGOON -> stringResource(R.string.appearance_lagoon_desc)
        AppearancePreset.LILAC -> stringResource(R.string.appearance_lilac_desc)
    }
    Column(
        modifier = modifier
            .clip(shape)
            .border(
                BorderStroke(if (selected) 2.dp else 1.dp,
                    if (selected) palette.accent else MaterialTheme.colorScheme.outlineVariant),
                shape,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.RadioButton
                this.selected = selected
            }
            .padding(7.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // A real miniature of the theme: canvas, raised card, typography,
        // progress accent and surface swatches, including the selected light tone.
        Box(
            modifier = Modifier.fillMaxWidth().height(170.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(palette.topBackground, palette.background))),
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Box(Modifier.size(33.dp).clip(RoundedCornerShape(9.dp))
                        .background(palette.accent), contentAlignment = Alignment.Center) {
                        Text("♫", color = palette.onAccent, style = MaterialTheme.typography.titleLarge)
                    }
                    Text(stringResource(R.string.appearance_preview_song),
                        color = palette.onSurface, style = MaterialTheme.typography.labelSmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.surface).padding(9.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(stringResource(R.string.stats_period_listening_time),
                        color = palette.mutedText, style = MaterialTheme.typography.labelSmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("9h 49m", color = palette.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Box(Modifier.fillMaxWidth().height(4.dp)
                        .clip(RoundedCornerShape(4.dp)).background(palette.elevatedSurface)) {
                        Box(Modifier.fillMaxWidth(0.68f).height(4.dp)
                            .background(palette.accent))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    listOf(palette.accent, palette.elevatedSurface,
                        palette.surfaceSunken, palette.onSurface).forEach { swatch ->
                        Box(Modifier.size(13.dp).clip(RoundedCornerShape(5.dp))
                            .background(swatch)
                            .border(0.5.dp, palette.border.copy(alpha = 0.7f),
                                RoundedCornerShape(5.dp)))
                    }
                }
            }
            if (active) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                        .size(22.dp).clip(RoundedCornerShape(30.dp))
                        .background(palette.accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Check, contentDescription = null,
                        modifier = Modifier.size(15.dp), tint = palette.onAccent)
                }
            }
        }
        Text(label, color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall)
    }
}
 
@Composable
private fun FontStyleCard(
    style: AppearanceFontStyle,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    val scheme = MaterialTheme.colorScheme
    val label = when (style) {
        AppearanceFontStyle.TEMPO -> stringResource(R.string.appearance_font_tempo)
        AppearanceFontStyle.NEO_SANS -> stringResource(R.string.appearance_font_neo)
        AppearanceFontStyle.ARCHIVE_SERIF -> stringResource(R.string.appearance_font_archive)
        AppearanceFontStyle.STUDIO_MONO -> stringResource(R.string.appearance_font_studio)
    }
    val family = when (style) {
        AppearanceFontStyle.TEMPO -> DisplayFontFamily
        AppearanceFontStyle.NEO_SANS -> AppFontFamily
        AppearanceFontStyle.ARCHIVE_SERIF -> FontFamily.Serif
        AppearanceFontStyle.STUDIO_MONO -> FontFamily.Monospace
    }
    Row(
        modifier = modifier
            .clip(shape)
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) scheme.primary else scheme.outlineVariant,
                shape,
            )
            .clickable(onClick = onClick)
            .semantics {
                role = Role.RadioButton
                this.selected = selected
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Aa", fontFamily = family,
            color = if (selected) scheme.primary else scheme.onSurface,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1)
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = scheme.onSurface,
                style = MaterialTheme.typography.labelMedium)
            if (selected) Text(
                stringResource(R.string.appearance_font_selected),
                color = scheme.primary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

private val AccentSwatches = listOf(
    "008E77" to R.string.appearance_color_teal,
    "2879BB" to R.string.appearance_color_blue,
    "8056C7" to R.string.appearance_color_violet,
    "B94B70" to R.string.appearance_color_rose,
    "B37629" to R.string.appearance_color_amber,
    "608E37" to R.string.appearance_color_green,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomAccentPicker(selected: String?, enabled: Boolean, onSelect: (String) -> Unit) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    Column {
        FlowRow(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AccentSwatches.forEach { (hex, label) ->
                FilterChip(
                    selected = selected == hex,
                    enabled = enabled,
                    onClick = { onSelect(hex) },
                    leadingIcon = {
                        Box(Modifier.size(16.dp).clip(RoundedCornerShape(50)).background(Color(0xFF000000L or hex.toLong(16))))
                    },
                    label = { Text(stringResource(label)) },
                )
            }
        }
        OutlinedButton(enabled = enabled, onClick = { showPicker = true }) {
            Text(stringResource(R.string.appearance_custom_hex, selected?.let { "#$it" } ?: ""))
        }
        Text(stringResource(R.string.appearance_accent_contrast), style = MaterialTheme.typography.bodySmall)
    }
    if (showPicker) {
        var input by rememberSaveable { mutableStateOf(selected?.let { "#$it" }.orEmpty()) }
        val hex = normalizeAccentHex(input)
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text(stringResource(R.string.appearance_custom_accent)) },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.take(16) },
                    singleLine = true,
                    label = { Text(stringResource(R.string.appearance_hex_label)) },
                    supportingText = { Text("#008E77") },
                    isError = input.isNotBlank() && hex == null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                )
            },
            confirmButton = {
                TextButton(enabled = enabled && hex != null, onClick = {
                    hex?.let(onSelect)
                    showPicker = false
                }) { Text(stringResource(R.string.appearance_choose_color)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.appearance_cancel)) }
            },
        )
    }
}


private fun appearancePeriodLabel(period: AppearanceStatsRange): Int = when (period) {
    AppearanceStatsRange.TODAY -> R.string.stats_share_period_today
    AppearanceStatsRange.WEEK -> R.string.period_this_week
    AppearanceStatsRange.MONTH -> R.string.period_this_month
    AppearanceStatsRange.YEAR -> R.string.period_this_year
    AppearanceStatsRange.ALL_TIME -> R.string.period_all_time
}
