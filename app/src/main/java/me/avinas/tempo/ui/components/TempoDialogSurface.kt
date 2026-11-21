package me.avinas.tempo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.avinas.tempo.ui.theme.*

/**
 * Compatibility tokens for older dedicated dialogs. TempoDialogSurface itself
 * uses the live Material shape scale and native tonal Surface, not these
 * static corners or an additional shadow layer.
 */
object TempoDialogShape {
    val radius = 24.dp
    val shape = RoundedCornerShape(radius)
}

@Composable
fun TempoDialogSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Dialog already lives in a modal window; drawing a second 28dp shadow
    // and several gradient overlays wastes work and looks muddy in light mode.
    androidx.compose.material3.Surface(
        modifier = modifier.widthIn(max = 560.dp).fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

/**
 * Header icon badge for dialogs.
 */
@Composable
fun TempoDialogIcon(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Int = 48
) {
    val shape = RoundedCornerShape((size * 0.30).dp)
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(shape)
            .background(tint.copy(alpha = 0.10f))
            .border(
                width = 1.dp,
                color = tint.copy(alpha = 0.22f),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size((size * 0.5).dp)
        )
    }
}

/**
 * Standard dialog title.
 */
@Composable
fun TempoDialogTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    val appearancePalette = LocalTempoPalette.current
    Text(
        text = text,
        style = MaterialTheme.typography.titleLargeEmphasized,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

/**
 * Standard dialog body text.
 */
@Composable
fun TempoDialogBody(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        textAlign = TextAlign.Center,
        lineHeight = MaterialTheme.typography.bodyMedium.fontSize * 1.5,
        modifier = modifier
    )
}

/**
 * Primary action button — full width, TempoPrimary, 12dp radius.
 */
@Composable
fun TempoDialogPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        shapes = ButtonDefaults.shapes(),
    ) {
        if (icon != null) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Secondary/dismiss action — tonal pill, quiet but tappable.
 */
@Composable
fun TempoDialogSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = LocalTempoPalette.current.mutedText
) {
    androidx.compose.material3.FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = color.copy(alpha = 0.14f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        shapes = ButtonDefaults.shapes()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Two-button row: secondary left, primary right.
 */
/**
 * Standard confirmation pair: secondary action first, primary action second.
 * Preserve focus order when stacked on small screens or with large fonts.
 */
@Composable
fun TempoDialogButtonRow(
    primaryText: String,
    onPrimary: () -> Unit,
    secondaryText: String,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier,
    primaryEnabled: Boolean = true,
    secondaryEnabled: Boolean = true,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    primaryContentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        // Adaptive breakpoint is a Tempo decision; Material requires the
        // touch targets to remain usable as typography and width change.
        val stacked = maxWidth < 360.dp ||
            androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.3f
        if (stacked) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TempoDialogRowSecondary(secondaryText, onSecondary, secondaryEnabled, Modifier.fillMaxWidth())
                TempoDialogRowPrimary(
                    primaryText, onPrimary, primaryEnabled,
                    primaryColor, primaryContentColor, Modifier.fillMaxWidth(),
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TempoDialogRowSecondary(secondaryText, onSecondary, secondaryEnabled, Modifier.weight(1f))
                TempoDialogRowPrimary(
                    primaryText, onPrimary, primaryEnabled,
                    primaryColor, primaryContentColor, Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TempoDialogRowSecondary(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier,
) {
    androidx.compose.material3.FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shapes = ButtonDefaults.shapes(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 3,
        )
    }
}

@Composable
private fun TempoDialogRowPrimary(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    container: Color,
    foreground: Color,
    modifier: Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = foreground,
        ),
        shapes = ButtonDefaults.shapes(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLargeEmphasized,
            maxLines = 3,
        )
    }
}

/**
 * Destructive action button using [TempoError] color.
 */
@Composable
fun TempoDialogDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    TempoDialogPrimaryButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        containerColor = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError
    )
}

/**
 * Text field for dialog inputs such as renaming, editing titles, or custom tags.
 */
@Composable
fun TempoDialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    maxLines: Int = 1
) {
    val appearancePalette = LocalTempoPalette.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = label?.let { { Text(it, style = MaterialTheme.typography.bodyMedium) } },
        placeholder = placeholder?.let { { Text(it, style = MaterialTheme.typography.bodyMedium, color = appearancePalette.mutedText) } },
        singleLine = singleLine,
        maxLines = maxLines,
        isError = isError,
        supportingText = supportingText?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isError) TempoError else appearancePalette.mutedText
                )
            }
        },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = appearancePalette.onSurface,
            unfocusedTextColor = appearancePalette.onSurface,
            focusedBorderColor = appearancePalette.accent,
            unfocusedBorderColor = appearancePalette.border.copy(alpha = 0.45f),
            focusedLabelColor = appearancePalette.accent,
            unfocusedLabelColor = appearancePalette.mutedText,
            cursorColor = appearancePalette.accent,
            focusedContainerColor = appearancePalette.surfaceSunken,
            unfocusedContainerColor = appearancePalette.surfaceSunken,
            errorBorderColor = TempoError,
            errorContainerColor = appearancePalette.surfaceSunken
        )
    )
}
