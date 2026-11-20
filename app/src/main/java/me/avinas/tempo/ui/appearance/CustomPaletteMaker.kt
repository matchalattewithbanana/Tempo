package me.avinas.tempo.ui.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.avinas.tempo.R
import androidx.compose.ui.res.stringResource

/**
 * Three-seed editor works with the existing Appearance Studio draft. The
 * accent picker supplies primary; the optional secondary/tertiary seeds only
 * affect Material's color roles. Nothing is persisted until Studio Apply.
 */
@Composable
internal fun CustomPaletteMaker(
    draft: AppearanceSettings,
    enabled: Boolean,
    onSecondaryChange: (String?) -> Unit,
    onTertiaryChange: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(R.string.appearance_palette_maker_title),
            style = MaterialTheme.typography.titleMediumEmphasized,
        )
        Text(
            stringResource(R.string.appearance_palette_maker_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Text fields need enough width for a six-digit hex value and a swatch.
        // Never force two minimum-width Material text fields into a phone row.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val compact = maxWidth < 580.dp
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PaletteSeedField(
                        value = draft.customSecondary,
                        label = stringResource(R.string.appearance_palette_secondary),
                        onValidChange = onSecondaryChange,
                        enabled = enabled,
                    )
                    PaletteSeedField(
                        value = draft.customTertiary,
                        label = stringResource(R.string.appearance_palette_tertiary),
                        onValidChange = onTertiaryChange,
                        enabled = enabled,
                    )
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PaletteSeedField(
                        value = draft.customSecondary,
                        label = stringResource(R.string.appearance_palette_secondary),
                        onValidChange = onSecondaryChange,
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                    )
                    PaletteSeedField(
                        value = draft.customTertiary,
                        label = stringResource(R.string.appearance_palette_tertiary),
                        onValidChange = onTertiaryChange,
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Text(
            stringResource(R.string.appearance_palette_tone_preview),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(AppearanceTone.LIGHT, AppearanceTone.DARK, AppearanceTone.OLED)
                .forEach { tone ->
                    val palette = remember(draft, tone) {
                        AppearancePaletteResolver.resolve(draft.copy(tone = tone))
                    }
                    val scheme = palette.materialColorScheme()
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = scheme.surfaceContainerHigh,
                        contentColor = scheme.onSurface,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                tone.name.lowercase().replaceFirstChar { it.uppercase() },
                                color = scheme.onSurface,
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(scheme.primary, scheme.secondary, scheme.tertiary).forEach { color ->
                                    Box(
                                        Modifier.weight(1f).height(20.dp)
                                            .background(color, MaterialTheme.shapes.small),
                                    )
                                }
                            }
                            Icon(Icons.Rounded.Album, contentDescription = null,
                                tint = scheme.primary, modifier = Modifier.size(22.dp))
                        }
                    }
                }
        }
        Text(
            stringResource(R.string.appearance_palette_preview_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PaletteSeedField(
    value: String?,
    label: String,
    enabled: Boolean,
    onValidChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var input by remember(value) { mutableStateOf(value?.let { "#$it" } ?: "") }
    val valid = normalizeAccentHex(input)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        OutlinedTextField(
            value = input,
            onValueChange = { next ->
                input = next.take(7)
                val normalized = normalizeAccentHex(next)
                if (next.isBlank()) onValidChange(null)
                else if (normalized != null) onValidChange(normalized)
            },
            enabled = enabled,
            singleLine = true,
            label = { Text(label) },
            placeholder = { Text("#78ABCD") },
            isError = input.isNotBlank() && valid == null,
            trailingIcon = {
                val color = valid?.toLong(16)?.let { Color(0xFF000000L or it) }
                Box(
                    modifier = Modifier.size(18.dp)
                        .background(color ?: MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(
            enabled = enabled && value != null,
            onClick = { input = ""; onValidChange(null) },
        ) { Text(stringResource(R.string.appearance_palette_auto)) }
    }
}
