package me.avinas.tempo.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal val LocalSettingsSectionTone =
    staticCompositionLocalOf<TempoTileTone?> { null }

/** Expressive section typography, shared by all Settings categories. */
@Composable
fun SettingsSectionHeader(text: String) {
    val tone = LocalSettingsSectionTone.current
    val swatch = tone?.let { tempoTileSwatch(it) }
    Row(
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (swatch != null) {
            Surface(
                modifier = Modifier.size(width = 4.dp, height = 18.dp),
                shape = MaterialTheme.shapes.full,
                color = swatch.iconContainer,
                content = {},
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Shared container for related Settings actions. The accent lives in the
 * category navigator; detail rows use one calm, readable Material surface.
 * Keeping a single container treatment prevents GlassCard/Solid/Surface
 * styles from changing unpredictably between Settings categories.
 */
@Composable
fun TempoSettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val tone = LocalSettingsSectionTone.current
    val container = tone?.let { selectedTone ->
        lerp(
            scheme.surfaceContainerLow,
            tempoTileSwatch(selectedTone).container,
            0.34f,
        )
    } ?: scheme.surfaceContainerLow
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = container,
        contentColor = scheme.onSurface,
        content = content,
    )
}

/** The official Material list item keeps supporting text, focus and touch targets
 * consistent throughout Settings without per-row shadow or glass layers.
 */
@Composable
fun SettingsOption(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    showArrow: Boolean = true,
) {
    ListItem(
        headlineContent = {
            Text(title, style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium, color = textColor)
        },
        supportingContent = if (subtitle == null) null else {
            { Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        trailingContent = if (!showArrow || onClick == null) null else {
            {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    )
}

@Composable
fun SettingsSwitch(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(title, style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium)
        },
        supportingContent = if (subtitle == null) null else {
            { Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    )
}
