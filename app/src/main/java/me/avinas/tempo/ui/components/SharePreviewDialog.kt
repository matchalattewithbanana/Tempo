package me.avinas.tempo.ui.components

import me.avinas.tempo.ui.theme.LocalTempoPalette
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.avinas.tempo.R

/** Fixed design resolution for the share card. Both the hidden capture source and the
 * visible preview are laid out at this size so the captured bitmap is WYSIWYG with the
 * preview regardless of device density or screen aspect. */
private val CardDesignWidth = 360.dp
private val CardDesignHeight = 640.dp

/**
 * Generic Dialog to preview content and share it as an image.
 */
@Composable
fun SharePreviewDialog(
    onDismiss: () -> Unit,
    text: String? = null,
    links: List<Pair<String, String>> = emptyList(),
    settings: (@Composable () -> Unit)? = null,
    cardHeight: Dp = CardDesignHeight,
    fileName: String = "tempo-card.png",
    exportEnabled: Boolean = true,
    initialTheme: ShareTheme? = null,
    onThemeSelected: ((ShareTheme) -> Unit)? = null,
    contentToShare: @Composable (theme: ShareTheme) -> Unit
) {
    val palette = LocalTempoPalette.current
    val resources = LocalContext.current.resources
    val themeNames = ShareTheme.entries.associateWith { resources.getString(shareThemeLabel(it)) }
    val captureController = rememberCaptureController()
    val defaultTheme = initialTheme ?: me.avinas.tempo.ui.theme.LocalTempoAppearance.current.defaultShareTheme
    var theme by remember(defaultTheme) { mutableStateOf(defaultTheme) }
    val actions = rememberCardShareActions(captureController, fileName = fileName, onShared = { onDismiss() })

    Dialog(
        onDismissRequest = { if (!actions.busy) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false, // Full screen width
            decorFitsSystemWindows = false
        )
    ) {
        // Root Container
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 1. Hidden Capture Source (High Quality, WYSIWYG)
            // Rendered at the SAME fixed design size as the preview so the captured
            // bitmap's element proportions match exactly what the user sees, regardless
            // of device density or screen aspect. Invisible (alpha 0f) but laid out for
            // capture. requiredSize ignores parent constraints, so this stays stable in
            // portrait, landscape, and tablet modes.
            Box(
                modifier = Modifier
                    .requiredSize(CardDesignWidth, cardHeight)
                    .alpha(0f),
                contentAlignment = Alignment.Center
            ) {
                CaptureWrapper(
                    controller = captureController,
                    modifier = Modifier.fillMaxSize()
                ) {
                    contentToShare(theme)
                }
            }

            // 2. Dark Overlay Background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.background.copy(alpha = 0.97f))
            )

            // 3. Visible UI (Preview + Controls)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(bottom = 16.dp) // Clean spacing with systemBarsPadding
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.details_share_preview),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = palette.onSurface
                    )

                    IconButton(
                        onClick = onDismiss,
                        enabled = !actions.busy,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                palette.elevatedSurface,
                                androidx.compose.foundation.shape.CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.share_close),
                            tint = palette.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Responsive Preview card using BoxWithConstraints.
                // We layout the card at its designed 360dp x 640dp resolution, and scale it
                // using graphicsLayer to fit within the available screen area.
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val scaleX = maxWidth / CardDesignWidth
                    val scaleY = maxHeight / cardHeight
                    val scale = minOf(scaleX, scaleY).coerceAtMost(1f)
                    
                    val scaledWidth = CardDesignWidth * scale
                    val scaledHeight = cardHeight * scale
                    val cardShape = RoundedCornerShape((20 * scale).dp)
                    
                    Box(
                        modifier = Modifier
                            .size(scaledWidth, scaledHeight)
                            .shadow(24.dp, cardShape, spotColor = Color.Black.copy(alpha = 0.7f), ambientColor = Color.Black.copy(alpha = 0.4f))
                            .clip(cardShape)
                            .border(1.dp, palette.border, cardShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .requiredSize(CardDesignWidth, cardHeight)
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                                )
                        ) {
                            contentToShare(theme)
                        }
                    }
                }

                settings?.invoke()

                // Theme picker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${stringResource(R.string.share_theme_label).uppercase()} • ${stringResource(shareThemeLabel(theme))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.mutedText,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ShareTheme.entries.forEach { t ->
                            ThemeSwatch(
                                theme = t,
                                selected = theme == t,
                                onClick = {
                                    if (!actions.busy) {
                                        theme = t
                                        onThemeSelected?.invoke(t)
                                    }
                                },
                                modifier = Modifier.semantics {
                                    contentDescription = themeNames.getValue(t)
                                },
                            )
                        }
                    }
                }

                ShareOutputButtons(
                    actions = actions,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    text = text,
                    links = links,
                    enabled = exportEnabled,
                )
            }
        }
    }
}
