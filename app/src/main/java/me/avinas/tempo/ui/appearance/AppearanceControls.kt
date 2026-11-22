@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.avinas.tempo.ui.appearance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.avinas.tempo.R
import me.avinas.tempo.ui.components.connectedChoiceColumns
import me.avinas.tempo.ui.components.shouldUseFilterChips
import me.avinas.tempo.ui.theme.AppFontFamily
import me.avinas.tempo.ui.theme.DisplayFontFamily

@Composable
internal fun AppearanceSection(
    title: Int,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        targetValue = if (expanded) colors.surfaceContainerHigh else colors.surfaceContainerLow,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "appearance_section_tonal",
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = container,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clickable(role = Role.Button) { expanded = !expanded }
                    .heightIn(min = 60.dp)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(title), modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = colors.onSurface,
                )
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null, tint = colors.onSurfaceVariant,
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.IntSize>(),
                ) + fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
                exit = shrinkVertically(
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.IntSize>(),
                ) + fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
            ) {
                Column {
                    HorizontalDivider(color = colors.outlineVariant)
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) { content() }
                }
            }
        }
    }
}

/** The whole Settings-style row is one switch target, including its label. */
@Composable
internal fun SettingToggle(label: Int, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    val labelText = stringResource(label)
    Row(
        modifier = Modifier.fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onChange,
            )
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            labelText,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Switch(checked = checked, enabled = enabled, onCheckedChange = null)
    }
}

/**
 * M3 Expressive connected groups for up to five single-select options.
 * Larger collections (e.g. the six duration modes) use filter chips instead;
 * a single large segmented row would be hard to read on a narrow phone.
 * Layout uses the actual pane width, not the physical screen width.
 */
@Composable
internal fun <T> ChoiceRow(
    label: Int,
    options: List<T>,
    selected: T,
    enabled: Boolean,
    text: (T) -> Int,
    onSelect: (T) -> Unit,
) {
    if (options.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (shouldUseFilterChips(options.size)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                options.forEach { option ->
                    val isChosen = selected == option
                    FilterChip(
                        selected = isChosen,
                        enabled = enabled,
                        onClick = { if (!isChosen) onSelect(option) },
                        modifier = Modifier.heightIn(min = 48.dp),
                        label = {
                            Text(
                                stringResource(text(option)),
                                style = if (isChosen) MaterialTheme.typography.labelLargeEmphasized
                                    else MaterialTheme.typography.labelLarge,
                            )
                        },
                        leadingIcon = if (isChosen) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null,
                    )
                }
            }
        } else {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = connectedChoiceColumns(
                    widthDp = maxWidth.value.toInt(),
                    fontScale = LocalDensity.current.fontScale,
                    optionCount = options.size,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.chunked(columns).forEach { group ->
                        Row(
                            modifier = Modifier.fillMaxWidth().selectableGroup(),
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                        ) {
                            group.forEachIndexed { index, option ->
                                val isChosen = selected == option
                                ToggleButton(
                                    checked = isChosen,
                                    enabled = enabled,
                                    onCheckedChange = { if (!isChosen) onSelect(option) },
                                    shapes = when {
                                        group.size == 1 -> ToggleButtonShapes(
                                            shape = ToggleButtonDefaults.shape,
                                            pressedShape = ToggleButtonDefaults.pressedShape,
                                            checkedShape = ToggleButtonDefaults.checkedShape,
                                        )
                                        index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        index == group.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    },
                                    modifier = Modifier.weight(1f).heightIn(min = 52.dp)
                                        .semantics {
                                            role = Role.RadioButton
                                            this.selected = isChosen
                                        },
                                ) {
                                    if (isChosen) {
                                        Icon(Icons.Default.Check, contentDescription = null,
                                            modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    Text(
                                        stringResource(text(option)),
                                        style = if (isChosen) MaterialTheme.typography.labelLargeEmphasized
                                            else MaterialTheme.typography.labelLarge,
                                        maxLines = 2,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun HomeSectionOrderRow(
    sectionName: String,
    visible: Boolean,
    enabled: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    moveUpLabel: String,
    moveDownLabel: String,
    onVisibleChange: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val density = LocalDensity.current
    var dragDistance by remember(sectionName) { mutableFloatStateOf(0f) }
    val dragThresholdPx = with(density) { 40.dp.toPx() }
    val dragState = rememberDraggableState { delta ->
        dragDistance += delta
        when {
            dragDistance <= -dragThresholdPx && canMoveUp -> {
                onMoveUp()
                dragDistance = 0f
            }
            dragDistance >= dragThresholdPx && canMoveDown -> {
                onMoveDown()
                dragDistance = 0f
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.weight(1f)
                .toggleable(
                    value = visible,
                    enabled = enabled,
                    role = Role.Checkbox,
                    onValueChange = onVisibleChange,
                )
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(checked = visible, enabled = enabled, onCheckedChange = null)
            Text(
                sectionName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Icon(
            Icons.Default.DragHandle,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .draggable(
                    state = dragState,
                    orientation = Orientation.Vertical,
                    enabled = enabled && (canMoveUp || canMoveDown),
                    onDragStopped = { dragDistance = 0f },
                )
                .padding(12.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(onClick = onMoveUp, enabled = enabled && canMoveUp) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "$moveUpLabel: $sectionName")
        }
        IconButton(onClick = onMoveDown, enabled = enabled && canMoveDown) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "$moveDownLabel: $sectionName")
        }
    }
}

internal fun sectionLabel(section: AppearanceHomeSection): Int = when (section) {
    AppearanceHomeSection.SUMMARY -> R.string.appearance_summary
    AppearanceHomeSection.SPOTLIGHT -> R.string.appearance_spotlight
    AppearanceHomeSection.QUICK_STATS -> R.string.appearance_quick_stats
    AppearanceHomeSection.TODAY -> R.string.period_today
    AppearanceHomeSection.INSIGHTS -> R.string.appearance_insights
}

@Composable
internal fun AppearancePresetCard(
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
    // A selection border is sufficient feedback. Resizing/morphing the
    // preview cards on tap caused layout shifts and abrupt visual flashes.
    val shape = MaterialTheme.shapes.extraLarge
    val previewInnerShape = MaterialTheme.shapes.large
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
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .semantics(mergeDescendants = true) {
                role = Role.RadioButton
                this.selected = selected
            },
        shape = shape,
        color = if (selected) scheme.secondaryContainer else scheme.surfaceContainerLow,
        contentColor = if (selected) scheme.onSecondaryContainer else scheme.onSurface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            // Compact real-theme miniature. Preset browsing stays visual without
            // turning expansion into a full-screen wall of oversized cards.
            Box(
                modifier = Modifier.fillMaxWidth().height(104.dp)
                    .clip(previewInnerShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(palette.topBackground, palette.background),
                        ),
                    ),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Box(
                            Modifier.size(28.dp)
                                .clip(MaterialShapes.Cookie9Sided.toShape())
                                .background(palette.accent),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "♫",
                                color = palette.onAccent,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        Text(
                            stringResource(R.string.appearance_preview_song),
                            color = palette.onSurface,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.surface)
                            .padding(horizontal = 7.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            stringResource(R.string.stats_period_listening_time),
                            color = palette.mutedText,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "9h 49m",
                                color = palette.onSurface,
                                style = MaterialTheme.typography.titleSmallEmphasized,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(palette.accent, palette.surfaceSunken, palette.onSurface)
                                    .forEach { swatch ->
                                        Box(
                                            Modifier.size(11.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(swatch),
                                        )
                                    }
                            }
                        }
                    }
                }
                if (selected || active) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(7.dp).size(24.dp),
                        shape = CircleShape,
                        color = if (selected) palette.accent else scheme.surfaceContainerHighest,
                        contentColor = if (selected) palette.onAccent else scheme.onSurface,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }
            }
            Text(
                label,
                color = if (selected) scheme.onSecondaryContainer else scheme.onSurface,
                style = if (selected) {
                    MaterialTheme.typography.titleSmallEmphasized
                } else {
                    MaterialTheme.typography.titleSmall
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                description,
                color = if (selected) {
                    scheme.onSecondaryContainer
                } else {
                    scheme.onSurfaceVariant
                },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
 
@Composable
internal fun FontStyleCard(
    style: AppearanceFontStyle,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.large
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
                style = if (selected) MaterialTheme.typography.labelMediumEmphasized
                    else MaterialTheme.typography.labelMedium)
            if (selected) Text(
                stringResource(R.string.appearance_font_selected),
                color = scheme.primary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

internal val AccentSwatches = listOf(
    "008E77" to R.string.appearance_color_teal,
    "2879BB" to R.string.appearance_color_blue,
    "8056C7" to R.string.appearance_color_violet,
    "B94B70" to R.string.appearance_color_rose,
    "B37629" to R.string.appearance_color_amber,
    "608E37" to R.string.appearance_color_green,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CustomAccentPicker(selected: String?, enabled: Boolean, onSelect: (String) -> Unit) {
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


internal fun appearancePeriodLabel(period: AppearanceStatsRange): Int = when (period) {
    AppearanceStatsRange.TODAY -> R.string.stats_share_period_today
    AppearanceStatsRange.WEEK -> R.string.period_this_week
    AppearanceStatsRange.MONTH -> R.string.period_this_month
    AppearanceStatsRange.YEAR -> R.string.period_this_year
    AppearanceStatsRange.ALL_TIME -> R.string.period_all_time
}
