package me.avinas.tempo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.animation.core.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.ui.tooling.preview.Preview
import me.avinas.tempo.ui.theme.TempoTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.avinas.tempo.data.stats.TimeRange
import me.avinas.tempo.ui.theme.TempoRed
import androidx.compose.ui.res.stringResource
import me.avinas.tempo.R




@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    timeRange: TimeRange? = null,
    onCheckSupportedApps: () -> Unit
) {
    // Container that fills the available space
    Box(
         modifier = modifier.fillMaxSize(),
         contentAlignment = Alignment.Center
    ) {
        if (timeRange != null && timeRange != TimeRange.ALL_TIME && timeRange != TimeRange.TODAY) {
            TimeRangeEmptyState(
                timeRange = timeRange,
                onCheckSupportedApps = onCheckSupportedApps
            )
        } else {
            SetupGuideEmptyState(onCheckSupportedApps = onCheckSupportedApps)
        }
    }
}

@Composable
private fun TimeRangeEmptyState(
    timeRange: TimeRange,
    onCheckSupportedApps: () -> Unit
) {
    // Calculate precise date label for the "Context"
    val contextLabel = remember(timeRange) {
        val now = java.time.LocalDate.now()
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d")
        
        when (timeRange) {
            TimeRange.THIS_WEEK -> {
                // Assuming Monday start for simplicity, or localized
                val startOfWeek = now.minusDays(now.dayOfWeek.value.toLong() - 1)
                val endOfWeek = startOfWeek.plusDays(6)
                "${startOfWeek.format(formatter)} - ${endOfWeek.format(formatter)}"
            }
            TimeRange.THIS_MONTH -> java.time.format.DateTimeFormatter.ofPattern("MMMM").format(now)
            TimeRange.THIS_YEAR -> java.time.format.DateTimeFormatter.ofPattern("yyyy").format(now)
            else -> ""
        }
    }

    // Distinct copy for "Reassurance" vs just "Empty"
    // The goal is to tell the user: "We are looking, just nothing found YET."
    val (icon, title, message) = when (timeRange) {
        TimeRange.THIS_WEEK -> Triple(
            Icons.Rounded.DateRange, 
            "Quiet week so far", 
            "We're listening, but haven't seen any plays yet. Start streaming to see your weekly pulse."
        )
        TimeRange.THIS_MONTH -> Triple(
            Icons.Rounded.DateRange, 
            "Fresh month, fresh start", 
            "Your history is safe. Play music to spark this month's stats."
        )
        TimeRange.THIS_YEAR -> Triple(
            Icons.Rounded.AutoAwesome,
            "${java.time.LocalDate.now().year} listening story",
            "Your year in review grows with every track. Keep listening to build your story."
        )
        else -> Triple(
            Icons.Rounded.DateRange, 
            "No Data Yet", 
            "Start listening to see your stats here."
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // "System Status" Badge - Reassures the user the app is working
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pulsing Dot effect (Radar Animation)
                    PulsingRadar(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.empty_waiting_for_listening),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
            
            // Icon
            TempoEmblem(
                icon = icon,
                size = 88.dp,
                style = TempoEmblemStyle.MEMORY,
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Title & Context
            Text(
                text = title,
                style = MaterialTheme.typography.titleLargeEmphasized,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Explicit Date Context
            // Helps user understand "Oh, this is why it's empty, it's a new week"
            if (contextLabel.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = contextLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Reassuring Body
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Secondary Action - Just in case they suspect a connection issue
            Button(
                onClick = onCheckSupportedApps,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary, 
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = MaterialTheme.shapes.large,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
               Text(stringResource(R.string.empty_check_apps), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) 
            }
        }
    }
}

@Composable
private fun SetupGuideEmptyState(
    onCheckSupportedApps: () -> Unit
) {
    // Logic: Provide clear instruction in a premium container.
    // Differentiator: This card is "Louder" (HighProminence) and focused on ACTION.
    
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TempoEmblem(
                icon = Icons.Rounded.GraphicEq,
                size = 96.dp,
                style = TempoEmblemStyle.PULSE,
            )
            Spacer(modifier = Modifier.height(20.dp))
            // Header
            Text(
                text = stringResource(R.string.empty_getting_started),
                style = MaterialTheme.typography.headlineSmallEmphasized,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.empty_stats_flowing),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Trust Signal
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                shape = MaterialTheme.shapes.extraLarge
            ) {
                 Text(
                    text = stringResource(R.string.empty_no_account),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Visual Steps - Compact & Clean
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StepItem(
                    icon = Icons.AutoMirrored.Rounded.QueueMusic, 
                    text = stringResource(R.string.empty_open_apps)
                )
                StepItem(
                    icon = Icons.Rounded.Headphones, 
                    text = stringResource(R.string.empty_play_songs)
                )
                StepItem(
                    icon = Icons.Rounded.GraphicEq, 
                    text = stringResource(R.string.empty_return_insights)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Primary Action
            Button(
                onClick = onCheckSupportedApps,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = MaterialTheme.shapes.extraLarge,
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.empty_check_apps),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StepItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.primary, 
                modifier = Modifier.size(18.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = text, 
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
            fontWeight = FontWeight.Medium,
            lineHeight = 20.sp
        )
    }
}

/** A static status marker avoids an infinite timer in an otherwise empty view. */
@Composable
private fun PulsingRadar(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun EmptyStatePreview() {
    TempoTheme {
        EmptyState(
            modifier = Modifier.fillMaxSize(),
            onCheckSupportedApps = {}
        )
    }
}
