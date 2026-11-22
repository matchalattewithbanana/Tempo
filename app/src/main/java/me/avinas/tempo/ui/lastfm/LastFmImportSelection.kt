package me.avinas.tempo.ui.lastfm

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import me.avinas.tempo.R
import me.avinas.tempo.data.lastfm.LastFmImportService
import me.avinas.tempo.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

/**
 * Helper data class for destructuring in ImportProgressContent
 */
@Composable
internal fun TierSelectionContent(
    discovery: LastFmImportService.DiscoveryResult,
    onSelectTier: (LastFmImportService.TierConfig) -> Unit,
    onBack: () -> Unit,
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header with account info
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = "Welcome, ${discovery.username}!",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        StatItem(
                            label = "Total Scrobbles",
                            value = numberFormat.format(discovery.totalScrobbles),
                        )
                        StatItem(
                            label = "Top Tracks",
                            value = numberFormat.format(discovery.topTracksCount),
                        )
                    }

                    // Recommend Standard for most users - it captures listening identity
                    // Quick is for testing, Deep for power users who want more history
                    val recommendedTier = "Standard"
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = LastFmRed,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Recommended: $recommendedTier",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LastFmRed,
                        )
                    }
                }
            }
        }

        // Tier selection title
        item {
            Text(
                text = "Choose Import Level",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        // Tier cards - Quick, Standard (recommended), Deep
        item {
            TierCard(
                tier = LastFmImportService.Companion.Tiers.QUICK,
                isRecommended = false,
                totalScrobbles = discovery.totalScrobbles,
                onClick = { onSelectTier(LastFmImportService.Companion.Tiers.QUICK) },
            )
        }

        item {
            TierCard(
                tier = LastFmImportService.Companion.Tiers.STANDARD,
                isRecommended = true, // Standard is recommended for most users
                totalScrobbles = discovery.totalScrobbles,
                onClick = { onSelectTier(LastFmImportService.Companion.Tiers.STANDARD) },
            )
        }

        item {
            TierCard(
                tier = LastFmImportService.Companion.Tiers.DEEP,
                isRecommended = false,
                totalScrobbles = discovery.totalScrobbles,
                onClick = { onSelectTier(LastFmImportService.Companion.Tiers.DEEP) },
            )
        }

        // User-friendly explanation
        item {
            Text(
                text = "💡 All your history is saved! This just affects how fast your charts load.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        // Back button
        item {
            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Use a different account",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TierCard(
    tier: LastFmImportService.TierConfig,
    isRecommended: Boolean,
    totalScrobbles: Long,
    onClick: () -> Unit,
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }
    val description =
        when (tier.name) {
            "QUICK" -> {
                "Last ${tier.recentMonths} months + loved tracks. Fast and lightweight. Perfect for getting started."
            }

            "STANDARD" -> {
                "Last ${tier.recentMonths} months + top ${numberFormat.format(
                    tier.topTracksCount,
                )} tracks + loved. Captures your listening identity. ~${tier.estimatedCoverage}% coverage."
            }

            "DEEP" -> {
                "Last ${tier.recentMonths} months + top ${numberFormat.format(
                    tier.topTracksCount,
                )} tracks + loved. For deep historical analysis. ~${tier.estimatedCoverage}% coverage."
            }

            else -> {
                "Import tier"
            }
        }

    val icon =
        when (tier.name) {
            "QUICK" -> "⚡"
            "STANDARD" -> "⭐"
            "DEEP" -> "📊"
            else -> "📁"
        }

    GlassCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        isHighlighted = isRecommended,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = icon, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = tier.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                if (isRecommended) {
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(LastFmRed.copy(alpha = 0.3f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "Recommended",
                            style = MaterialTheme.typography.labelSmall,
                            color = LastFmRed,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun ImportProgressContent(
    progress: LastFmImportService.ImportProgress,
    onCancel: () -> Unit,
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }

    // Extract values from the progress state
    val (progressPercent, phase, message, eventsCreated, archived, total) =
        when (progress) {
            is LastFmImportService.ImportProgress.Importing -> {
                val percent = if (progress.total > 0) ((progress.current * 100) / progress.total).toInt() else 0
                val msg = "Processing ${numberFormat.format(progress.current)} of ${numberFormat.format(progress.total)}"
                Tuple6(percent, progress.phase, msg, progress.eventsCreated, progress.archived, progress.total)
            }

            is LastFmImportService.ImportProgress.Discovering -> {
                Tuple6(0, "Discovering", progress.message, 0L, 0L, 0L)
            }

            is LastFmImportService.ImportProgress.Processing -> {
                Tuple6(50, "Processing", progress.message, 0L, 0L, 0L)
            }

            else -> {
                Tuple6(0, "Preparing", "Starting import...", 0L, 0L, 0L)
            }
        }

    val animatedProgress by animateFloatAsState(
        targetValue = (progressPercent.toFloat() / 100f).coerceIn(0f, 1f),
        label = "progress",
    )

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Progress indicator
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                progress = { animatedProgress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxSize(),
                color = LastFmRed,
                trackColor = GlassBorderMedium,
                strokeWidth = 8.dp,
            )

            Text(
                text = "$progressPercent%",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = phase,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Stats during import
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatItem(
                    label = "Events",
                    value = numberFormat.format(eventsCreated),
                )
                StatItem(
                    label = "Archived",
                    value = numberFormat.format(archived),
                )
                StatItem(
                    label = "Total",
                    value = numberFormat.format(total),
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Cancel button
        TextButton(onClick = onCancel) {
            Icon(
                Icons.Default.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Cancel Import",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

