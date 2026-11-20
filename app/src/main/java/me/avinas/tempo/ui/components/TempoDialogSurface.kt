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
        modifier = modifier.safeDrawingPadding().fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 2.dp,
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
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = appearancePalette.onSurface,
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
    color: Color = LocalTempoPalette.current.mutedText
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
    containerColor: Color = LocalTempoPalette.current.accent,
    contentColor: Color = LocalTempoPalette.current.onAccent
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
@Composable
fun TempoDialogButtonRow(
    primaryText: String,
    onPrimary: () -> Unit,
    secondaryText: String,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier,
    primaryEnabled: Boolean = true,
    secondaryEnabled: Boolean = true,
    primaryColor: Color = LocalTempoPalette.current.accent,
    primaryContentColor: Color = LocalTempoPalette.current.onAccent
) {
    val appearancePalette = LocalTempoPalette.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        androidx.compose.material3.FilledTonalButton(
            onClick = onSecondary,
            enabled = secondaryEnabled,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = appearancePalette.mutedText.copy(alpha = 0.14f),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            shapes = ButtonDefaults.shapes()
        ) {
            Text(
                text = secondaryText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
        Button(
            onClick = onPrimary,
            enabled = primaryEnabled,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryColor,
                contentColor = primaryContentColor
            ),
            shapes = ButtonDefaults.shapes()
        ) {
            Text(
                text = primaryText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
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
        containerColor = TempoError,
        contentColor = Color.White
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
