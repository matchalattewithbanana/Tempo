package me.avinas.tempo.ui.appearance

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.avinas.tempo.R
import me.avinas.tempo.ui.components.ShareTheme
import me.avinas.tempo.ui.components.shareThemeLabel
import me.avinas.tempo.ui.stats.statsDurationLabel
import me.avinas.tempo.ui.stats.statsSummaryLabel
import me.avinas.tempo.ui.components.appearanceBackdropBrush
import me.avinas.tempo.ui.components.GlassCard
import me.avinas.tempo.ui.theme.AppFontFamily
import me.avinas.tempo.ui.theme.DisplayFontFamily
import me.avinas.tempo.ui.theme.LocalTempoPalette
import me.avinas.tempo.ui.theme.TempoTheme

private val DraftSaver = Saver<AppearanceSettings, String>(
    save = { AppearanceCodec.encode(it) },
    restore = { runCatching { AppearanceCodec.decode(it) }.getOrDefault(AppearanceSettings()) },
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppearanceScreen(onNavigateBack: () -> Unit, viewModel: AppearanceViewModel = hiltViewModel()) {
    val loaded by viewModel.settings.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val saveFailed by viewModel.saveFailed.collectAsState()
    val looks by viewModel.savedLooks.collectAsState()
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
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
            AppearanceSection(R.string.appearance_presets, initiallyExpanded = true) {
                BoxWithConstraints {
                    val columns = if (maxWidth < 290.dp || LocalDensity.current.fontScale >= 1.4f) 1 else 2
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AppearancePreset.entries.chunked(columns).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { preset ->
                                    AppearancePresetCard(
                                        preset, saved.preset == preset, draft.preset == preset,
                                        onClick = { draft = draft.copy(preset = preset) },
                                        modifier = Modifier.weight(1f),
                                        tone = preview.tone,
                                        enabled = !busy,
                                    )
                                }
                            }
                        }
                    }
                }
            }
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
            AppearanceSection(R.string.appearance_effects_heading) {
                ChoiceRow(R.string.appearance_finish, AppearanceSurface.entries, draft.surface, !busy,
                    { if (it == AppearanceSurface.SOLID) R.string.appearance_solid else R.string.appearance_satin }) { draft = draft.copy(surface = it) }
                SettingToggle(R.string.appearance_reduce_motion, draft.reduceMotion, !busy) { draft = draft.copy(reduceMotion = it) }
                SettingToggle(R.string.appearance_adaptive, draft.adaptiveEffects, !busy) { draft = draft.copy(adaptiveEffects = it) }
                Text(stringResource(R.string.appearance_effect_policy), style = MaterialTheme.typography.bodySmall)
            }
            AppearanceSection(R.string.appearance_arrangement) {
                ChoiceRow(R.string.appearance_now_listening, AppearanceNowListening.entries, draft.nowListening, !busy, { when (it) {
                    AppearanceNowListening.INLINE -> R.string.appearance_inline
                    AppearanceNowListening.COMPACT -> R.string.appearance_compact
                    AppearanceNowListening.HIDDEN -> R.string.appearance_hidden
                } }) { draft = draft.copy(nowListening = it) }
                SettingToggle(R.string.appearance_home_summary_chart, draft.showHomeSummaryChart, !busy) {
                    draft = draft.copy(showHomeSummaryChart = it)
                }
                Text(stringResource(R.string.appearance_home_order), style = MaterialTheme.typography.titleSmall)
                draft.homeOrder.forEachIndexed { index, section ->
                    val sectionName = stringResource(sectionLabel(section))
                    val moveLabel = stringResource(R.string.appearance_move_up)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(modifier = Modifier.semantics { contentDescription = sectionName }, checked = section !in draft.hiddenHomeSections, enabled = !busy, onCheckedChange = { visible ->
                            draft = draft.copy(hiddenHomeSections = if (visible) draft.hiddenHomeSections - section else draft.hiddenHomeSections + section)
                        })
                        Text(stringResource(sectionLabel(section)), modifier = Modifier.weight(1f))
                        TextButton(modifier = Modifier.semantics { contentDescription = "$moveLabel: $sectionName" }, enabled = !busy && index > 0, onClick = {
                            val order = draft.homeOrder.toMutableList()
                            order[index] = order[index - 1]; order[index - 1] = section
                            draft = draft.copy(homeOrder = order)
                        }) { Text(stringResource(R.string.appearance_move_up)) }
                    }
                }
            }
            AppearanceSection(R.string.appearance_stats_settings) {
                ChoiceRow(R.string.appearance_stats_start, AppearanceStatsTab.entries, draft.statsTab, !busy, { when (it) {
                    AppearanceStatsTab.SONGS -> R.string.appearance_songs
                    AppearanceStatsTab.ARTISTS -> R.string.appearance_artists
                    AppearanceStatsTab.ALBUMS -> R.string.appearance_albums
                } }) { draft = draft.copy(statsTab = it) }
                ChoiceRow(R.string.appearance_stats_period, AppearanceStatsRange.entries, draft.statsRange, !busy, { when (it) {
                    AppearanceStatsRange.TODAY -> R.string.stats_share_period_today
                    AppearanceStatsRange.WEEK -> R.string.period_this_week
                    AppearanceStatsRange.MONTH -> R.string.period_this_month
                    AppearanceStatsRange.YEAR -> R.string.period_this_year
                    AppearanceStatsRange.ALL_TIME -> R.string.period_all_time
                } }) { draft = draft.copy(statsRange = it) }
                ChoiceRow(R.string.appearance_stats_sort, AppearanceStatsSort.entries, draft.statsSort, !busy, { when (it) {
                    AppearanceStatsSort.COMBINED -> R.string.stats_sort_combined
                    AppearanceStatsSort.PLAYS -> R.string.stats_sort_play_count
                    AppearanceStatsSort.TIME -> R.string.stats_sort_total_time
                } }) { draft = draft.copy(statsSort = it) }
                ChoiceRow(
                    R.string.stats_duration_display, AppearanceStatsDuration.entries,
                    draft.statsDuration, !busy, ::statsDurationLabel,
                ) { draft = draft.copy(statsDuration = it) }
                ChoiceRow(
                    R.string.stats_summary_count, AppearanceStatsSummary.entries,
                    draft.statsSummary, !busy && draft.showStatsSummary, ::statsSummaryLabel,
                ) { draft = draft.copy(statsSummary = it) }
                SettingToggle(R.string.appearance_stats_summary, draft.showStatsSummary, !busy) {
                    draft = draft.copy(showStatsSummary = it)
                }
                SettingToggle(R.string.appearance_stats_highlight, draft.highlightTopResult, !busy) {
                    draft = draft.copy(highlightTopResult = it)
                }
            }
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
            AppearanceSection(R.string.appearance_sharing_settings) {
                ChoiceRow(
                    R.string.appearance_share_theme, ShareTheme.entries, draft.defaultShareTheme, !busy, ::shareThemeLabel,
                ) { draft = draft.copy(defaultShareTheme = it) }
            }
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

@Composable
private fun AppearanceSection(
    title: Int,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(title), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Text(if (expanded) "−" else "+")
        }
        if (expanded) content()
    }
}

@Composable
private fun SettingToggle(label: Int, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    val labelText = stringResource(label)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(labelText, modifier = Modifier.weight(1f))
        Switch(modifier = Modifier.semantics { contentDescription = labelText }, checked = checked, enabled = enabled, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChoiceRow(label: Int, options: List<T>, selected: T, enabled: Boolean,
    text: (T) -> Int, onSelect: (T) -> Unit) {
    Column {
        Text(stringResource(label), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option -> FilterChip(selected = selected == option, enabled = enabled,
                onClick = { onSelect(option) }, label = { Text(stringResource(text(option))) }) }
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
    val palette = AppearancePaletteResolver.resolve(AppearanceSettings(preset = preset, tone = tone), androidx.compose.foundation.isSystemInDarkTheme())
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
            .semantics {
                role = Role.RadioButton
                this.selected = selected
            }
            .padding(7.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = 148.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(listOf(palette.topBackground, palette.background))
                ),
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(11.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(43.dp).clip(RoundedCornerShape(8.dp))
                        .background(palette.accent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("♫", color = palette.onAccent,
                            style = MaterialTheme.typography.headlineSmall)
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(stringResource(R.string.appearance_preview_song),
                            color = palette.onSurface, maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium)
                        Text(stringResource(R.string.appearance_preview_artist),
                            color = palette.mutedText,
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
                Box(
                    Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(4.dp))
                        .background(palette.elevatedSurface),
                ) {
                    Box(Modifier.fillMaxWidth(0.64f).height(4.dp)
                        .background(palette.accent))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().height(39.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.surface),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    listOf(0.36f, 0.70f, 0.48f, 0.9f, 0.60f, 0.80f).forEach { proportion ->
                        Box(
                            Modifier.weight(1f)
                                .fillMaxHeight(proportion)
                                .background(palette.accent.copy(alpha = 0.60f)),
                        )
                    }
                }
            }
            if (active) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).padding(9.dp)
                        .size(23.dp).clip(RoundedCornerShape(30.dp))
                        .background(palette.accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Check, contentDescription = null,
                        modifier = Modifier.size(16.dp), tint = palette.onAccent)
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
