@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.avinas.tempo.ui.appearance

import me.avinas.tempo.ui.effects.tempoAmbientBackdrop
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import me.avinas.tempo.ui.components.TempoCircularLoadingIndicator
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.avinas.tempo.R
import me.avinas.tempo.ui.components.ShareTheme
import me.avinas.tempo.ui.components.shareThemeLabel
import me.avinas.tempo.ui.stats.statsDurationLabel
import me.avinas.tempo.ui.stats.statsDurationPrecisionLabel
import me.avinas.tempo.ui.stats.formatStatsDuration
import me.avinas.tempo.ui.stats.statsSummaryLabel

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
fun AppearanceScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogoPicker: () -> Unit = {},
    viewModel: AppearanceViewModel = hiltViewModel(),
) {
    val loaded by viewModel.settings.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val saveFailed by viewModel.saveFailed.collectAsStateWithLifecycle()
    val looks by viewModel.savedLooks.collectAsStateWithLifecycle()
    val saved = loaded
    val context = LocalContext.current
    if (saved == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TempoCircularLoadingIndicator()
            IconButton(onClick = onNavigateBack, modifier = Modifier.align(Alignment.TopStart).statusBarsPadding()) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.settings_back))
            }
        }
        return
    }
    var draft by rememberSaveable(saved, stateSaver = DraftSaver) { mutableStateOf(saved) }
    var presetsExpanded by rememberSaveable { mutableStateOf(false) }
    var applySuggestedDetails by rememberSaveable { mutableStateOf(false) }
    var lookName by rememberSaveable { mutableStateOf("") }
    val transfer by viewModel.transferState.collectAsStateWithLifecycle()
    val busy = saving || transfer.busy
    val exportLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json"),
        ) { uri ->
            if (uri == null) viewModel.cancelPreparedExport()
            else viewModel.exportPrepared(uri)
        }
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let(viewModel::importAppearance)
        }
    LaunchedEffect(transfer.imported) {
        transfer.imported?.let { imported ->
            draft = imported
            viewModel.consumeImportedAppearance()
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(
            title = { Text(stringResource(R.string.appearance_title),
                style = MaterialTheme.typography.titleLargeEmphasized) },
            navigationIcon = { IconButton(onClick = requestBack, enabled = !busy && exitApplyTarget == null) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.settings_back))
            } },
        ) },
        bottomBar = {
            // The unsaved-changes actions enter from the bottom rather than
            // suddenly pushing the preview's last rows upward.
            AnimatedVisibility(
                visible = draft != saved,
                enter = expandVertically(
                    expandFrom = Alignment.Bottom,
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                ) + fadeIn(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()),
                exit = shrinkVertically(
                    shrinkTowards = Alignment.Bottom,
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                ) + fadeOut(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 3.dp,
                ) {
                    Row(
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = { draft = saved },
                            enabled = !busy,
                            modifier = Modifier.weight(1f),
                            shapes = ButtonDefaults.shapes(),
                        ) {
                            Text(stringResource(R.string.appearance_cancel))
                        }
                        Button(
                            onClick = { viewModel.apply(draft) },
                            enabled = !busy,
                            modifier = Modifier.weight(1f),
                            shapes = ButtonDefaults.shapes(),
                        ) {
                            Text(stringResource(R.string.appearance_apply))
                        }
                    }
                }
            }
        },
    ) { padding ->
        // Compose only the visible studio sections. Previously the entire studio,
        // including presets, fonts and saved looks, was composed on every preference edit.
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).tempoAmbientBackdrop(),
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
                AppearanceAccentSource.CUSTOM -> {
                    CustomAccentPicker(draft.customAccent, enabled = !busy) {
                        draft = draft.copy(customAccent = it)
                    }
                    CustomPaletteMaker(
                        draft = draft,
                        enabled = !busy,
                        onSecondaryChange = { draft = draft.copy(customSecondary = it) },
                        onTertiaryChange = { draft = draft.copy(customTertiary = it) },
                    )
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
            AppearancePreviewPanel(draft)
                }
            }
            item(key = "preset_collection") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        onClick = { presetsExpanded = !presetsExpanded },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .heightIn(min = 64.dp)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    stringResource(R.string.appearance_presets),
                                    style = MaterialTheme.typography.titleMediumEmphasized,
                                )
                                Text(
                                    stringResource(R.string.appearance_preset_preview_explainer),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                )
                            }
                            Text(
                                if (presetsExpanded) "−" else "+",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = presetsExpanded,
                        enter = expandVertically(
                            animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                        ) + fadeIn(
                            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                        ),
                        exit = shrinkVertically(
                            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                        ) + fadeOut(
                            animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
                        ),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    stringResource(R.string.appearance_new_editions),
                                    style = MaterialTheme.typography.titleSmallEmphasized,
                                )
                                Text(
                                    stringResource(R.string.appearance_gallery_hint),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                SettingToggle(
                                    R.string.appearance_apply_curated_style,
                                    applySuggestedDetails,
                                    !busy,
                                ) { applySuggestedDetails = it }
                            }
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp),
                            ) {
                                items(
                                    count = NewAppearanceEditions.size,
                                    key = { NewAppearanceEditions[it].name },
                                ) { index ->
                                    val preset = NewAppearanceEditions[index]
                                    AppearancePresetCard(
                                        preset = preset,
                                        active = saved.preset == preset,
                                        selected = draft.preset == preset,
                                        onClick = {
                                            draft = draft.withCuratedPreset(
                                                preset,
                                                applySuggestedDetails,
                                            )
                                        },
                                        modifier = Modifier.width(184.dp),
                                        tone = draft.tone,
                                        enabled = !busy,
                                    )
                                }
                            }

                            Text(
                                stringResource(R.string.appearance_signature_editions),
                                style = MaterialTheme.typography.titleSmallEmphasized,
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp),
                            ) {
                                items(
                                    count = SignatureAppearancePresets.size,
                                    key = { SignatureAppearancePresets[it].name },
                                ) { index ->
                                    val preset = SignatureAppearancePresets[index]
                                    AppearancePresetCard(
                                        preset = preset,
                                        active = saved.preset == preset,
                                        selected = draft.preset == preset,
                                        onClick = {
                                            draft = draft.withCuratedPreset(
                                                preset,
                                                applySuggestedDetails,
                                            )
                                        },
                                        modifier = Modifier.width(184.dp),
                                        tone = draft.tone,
                                        enabled = !busy,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item(key = "launcher_logo") {
                AppearanceSection(R.string.logo_picker_title) {
                    Text(stringResource(R.string.logo_picker_settings_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(
                        onClick = onNavigateToLogoPicker,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.logo_picker_choose)) }
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
                SettingToggle(R.string.appearance_adaptive, draft.adaptiveEffects, !busy) { draft = draft.copy(adaptiveEffects = it) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(stringResource(R.string.appearance_effects_opt_in_heading),
                    style = MaterialTheme.typography.titleMediumEmphasized)
                Text(stringResource(R.string.appearance_effects_opt_in_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                ChoiceRow(
                    R.string.appearance_navigation_style,
                    AppearanceNavigationStyle.entries,
                    draft.navigationStyle, !busy,
                    { when (it) {
                        AppearanceNavigationStyle.BAR -> R.string.appearance_short_bar
                        AppearanceNavigationStyle.FLOATING -> R.string.appearance_floating_dock
                        AppearanceNavigationStyle.EXPRESSIVE -> R.string.appearance_expressive_dock
                    } },
                ) { draft = draft.copy(navigationStyle = it) }
                SettingToggle(R.string.appearance_ambient_animation, draft.ambientAnimation, !busy) {
                    draft = draft.copy(ambientAnimation = it)
                }
                SettingToggle(R.string.appearance_sensor_tilt, draft.sensorTilt, !busy) {
                    draft = draft.copy(sensorTilt = it)
                }
                SettingToggle(R.string.appearance_list_motion, draft.listEntranceMotion, !busy) {
                    draft = draft.copy(listEntranceMotion = it)
                }
                ChoiceRow(
                    R.string.appearance_effect_intensity,
                    AppearanceEffectIntensity.entries,
                    draft.effectIntensity, !busy,
                    { if (it == AppearanceEffectIntensity.GENTLE)
                        R.string.appearance_effect_gentle else R.string.appearance_effect_expressive },
                ) { draft = draft.copy(effectIntensity = it) }
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
                    HomeSectionOrderRow(
                        sectionName = sectionName,
                        visible = section !in draft.hiddenHomeSections,
                        enabled = !busy,
                        canMoveUp = index > 0,
                        canMoveDown = index < draft.homeOrder.lastIndex,
                        moveUpLabel = moveLabel,
                        moveDownLabel = moveDownLabel,
                        onVisibleChange = { visible ->
                            draft = draft.copy(
                                hiddenHomeSections = if (visible) {
                                    draft.hiddenHomeSections - section
                                } else {
                                    draft.hiddenHomeSections + section
                                },
                            )
                        },
                        onMoveUp = {
                            val order = draft.homeOrder.toMutableList()
                            order[index] = order[index - 1]
                            order[index - 1] = section
                            draft = draft.copy(homeOrder = order)
                        },
                        onMoveDown = {
                            val order = draft.homeOrder.toMutableList()
                            order[index] = order[index + 1]
                            order[index + 1] = section
                            draft = draft.copy(homeOrder = order)
                        },
                    )
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
                    OutlinedButton(
                        enabled = !busy,
                        onClick = {
                            viewModel.clearTransferFeedback()
                            importLauncher.launch(arrayOf("application/json", "text/plain"))
                        },
                    ) {
                        Text(stringResource(R.string.appearance_import))
                    }
                    OutlinedButton(
                        enabled = !busy,
                        onClick = {
                            viewModel.prepareExport(draft)
                            exportLauncher.launch("tempo-appearance.json")
                        },
                    ) {
                        Text(stringResource(R.string.appearance_export))
                    }
                }
            }
            }
            item(key = "restore_and_status") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(enabled = !busy, onClick = { draft = AppearanceSettings() }) {
                Text(stringResource(R.string.appearance_restore))
            }
            if (saveFailed) Text(stringResource(R.string.appearance_save_failed), color = MaterialTheme.colorScheme.error)
            if (transfer.failed) {
                Text(
                    stringResource(R.string.appearance_transfer_failed),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (transfer.completed) Text(stringResource(R.string.appearance_transfer_done))
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}
