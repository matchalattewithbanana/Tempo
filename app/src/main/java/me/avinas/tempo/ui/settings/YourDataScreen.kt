package me.avinas.tempo.ui.settings

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import me.avinas.tempo.R
import me.avinas.tempo.ui.appearance.LocalAppearanceRuntimePolicy
import me.avinas.tempo.data.analytics.AnalyticsCatalog
import me.avinas.tempo.ui.components.DeepOceanBackground
import me.avinas.tempo.ui.components.GlassCard
import me.avinas.tempo.ui.components.GlassCardVariant
import me.avinas.tempo.ui.theme.GlassBorderSoft
import me.avinas.tempo.ui.theme.TempoPrimary
import me.avinas.tempo.ui.theme.TextPrimary
import me.avinas.tempo.ui.theme.TextTertiary

/**
 * The single "Data & diagnostics" surface: what Tempo sends on its own, and how it is
 * running on this device — one card each, both collapsed until opened.
 *
 * The card bodies answer the two questions a privacy screen gets asked. "What we collect"
 * renders [AnalyticsCatalog], which `AnalyticsCatalogTest` keeps matching the events the
 * app can actually send. The diagnostics card renders the user-initiated report, which
 * never leaves the device unless the user taps Share. Collapsed by default, the screen
 * reads as two plain summaries rather than a wall of data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YourDataScreen(
    onNavigateBack: () -> Unit,
    viewModel: DiagnosticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val resources = LocalResources.current
    var confirmRecoveryDiscard by rememberSaveable { mutableStateOf(false) }

    if (confirmRecoveryDiscard && uiState.recoveryRequired) {
        AlertDialog(
            onDismissRequest = { confirmRecoveryDiscard = false },
            title = { Text(stringResource(R.string.offline_queue_discard)) },
            text = { Text(stringResource(R.string.offline_queue_discard_confirmation)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRecoveryDiscard = false
                    viewModel.discardRecoveryData()
                }) { Text(stringResource(R.string.offline_queue_discard)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRecoveryDiscard = false }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            },
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_data_diagnostics), color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            )
        },
    ) { padding ->
        DeepOceanBackground {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.your_data_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                )

                if (uiState.recoveryRequired) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                stringResource(R.string.offline_queue_recovery_title),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(stringResource(R.string.offline_queue_recovery_description))
                            if (uiState.recoveryDiscardFailed) {
                                Text(stringResource(R.string.offline_queue_discard_failed))
                            }
                            OutlinedButton(
                                onClick = { confirmRecoveryDiscard = true },
                                enabled = !uiState.discardingRecovery && !uiState.isLoading,
                            ) { Text(stringResource(R.string.offline_queue_discard)) }
                        }
                    }
                }

                CollapsibleCard(
                    title = stringResource(R.string.what_we_collect_title),
                    description = stringResource(R.string.what_we_collect_intro),
                ) {
                    Text(
                        text = stringResource(R.string.what_we_collect_properties),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                    )

                    AnalyticsCatalog.entries.forEachIndexed { index, entry ->
                        if (index > 0) HorizontalDivider(color = GlassBorderSoft)
                        Column(modifier = Modifier.padding(vertical = 12.dp)) {
                            Text(
                                text = entry.event,
                                style = MaterialTheme.typography.titleSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = entry.what,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = entry.properties.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.what_we_collect_footer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                CollapsibleCard(
                    title = stringResource(R.string.diagnostics_title),
                    description = stringResource(R.string.diagnostics_intro),
                    onExpand = viewModel::refresh,
                ) {
                    val report = uiState.report
                    when {
                        uiState.isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                            }
                        }

                        report == null -> {
                            Text(
                                text = stringResource(R.string.diagnostics_failed),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )
                        }

                        else -> {
                            SelectionContainer {
                                Text(
                                    text = report,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    onClick = viewModel::refresh,
                                    modifier = Modifier.weight(1f),
                                    enabled = !uiState.isLoading,
                                ) {
                                    Text(stringResource(R.string.diagnostics_refresh))
                                }

                                Button(
                                    onClick = {
                                        val text = report ?: return@Button
                                        val intent =
                                            Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(
                                                    Intent.EXTRA_SUBJECT,
                                                    resources.getString(R.string.diagnostics_share_subject),
                                                )
                                                putExtra(Intent.EXTRA_TEXT, text)
                                            }
                                        context.startActivity(Intent.createChooser(intent, null))
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = report != null,
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                        ),
                                ) {
                                    Text(stringResource(R.string.diagnostics_share))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact Material expandable section. Refresh is a side effect of expanding
 * the section, never of drawing it: recomposition must not re-run diagnostics.
 */
@Composable
private fun CollapsibleCard(
    title: String,
    description: String,
    onExpand: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val latestOnExpand by rememberUpdatedState(onExpand)
    val reduceMotion = LocalAppearanceRuntimePolicy.current.reduceMotion
    LaunchedEffect(expanded) {
        if (expanded) latestOnExpand()
    }

    Surface(
        modifier = Modifier.fillMaxWidth().then(
            if (reduceMotion) Modifier else Modifier.animateContentSize()
        ),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.rotate(if (expanded) 180f else 0f),
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                content()
            }
        }
    }
}
