package me.avinas.tempo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.avinas.tempo.R

/**
 * Non-animated loading placeholders preserve the actual music-card geometry,
 * avoid blank screens and keep system battery/accessibility motion decisions.
 * Decorative blocks expose one loading announcement, never dozens of nodes.
 */
@Composable
private fun PlaceholderBar(width: Float, height: Dp = 12.dp) {
    Box(
        modifier = Modifier.fillMaxWidth(width)
            .height(height)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.11f)),
    )
}

@Composable
fun TempoHomeLoadingSkeleton(modifier: Modifier = Modifier) {
    val label = stringResource(R.string.ui_loading)
    Column(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clearAndSetSemantics { contentDescription = label },
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                PlaceholderBar(0.24f, 12.dp)
                PlaceholderBar(0.72f, 42.dp)
                PlaceholderBar(0.42f, 13.dp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    repeat(7) { i ->
                        Box(
                            Modifier.weight(1f).height((20 + ((i * 17) % 42)).dp)
                                .background(
                                    if (i == 4) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    MaterialTheme.shapes.small,
                                ),
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            repeat(2) {
                Surface(
                    modifier = Modifier.weight(1f).heightIn(min = 128.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(13.dp),
                    ) {
                        Box(Modifier.size(38.dp).background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.shapes.medium,
                        ))
                        PlaceholderBar(0.78f)
                        PlaceholderBar(0.48f, 20.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun TempoStatsLoadingSkeleton(modifier: Modifier = Modifier) {
    val label = stringResource(R.string.ui_loading)
    Column(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clearAndSetSemantics { contentDescription = label },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.tertiaryContainer,
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.size(76.dp).background(
                    MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.14f),
                    MaterialTheme.shapes.large,
                ))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PlaceholderBar(0.7f, 14.dp)
                    PlaceholderBar(0.9f, 25.dp)
                    PlaceholderBar(0.45f, 11.dp)
                }
            }
        }
        repeat(4) { index ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = when (index) {
                    0 -> MaterialTheme.shapes.large
                    else -> MaterialTheme.shapes.medium
                },
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(Modifier.size(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.medium))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        PlaceholderBar(if (index % 2 == 0) 0.8f else 0.62f)
                        PlaceholderBar(0.36f, 11.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun TempoHistoryLoadingSkeleton(modifier: Modifier = Modifier) {
    val label = stringResource(R.string.ui_loading)
    Column(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clearAndSetSemantics { contentDescription = label },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PlaceholderBar(0.34f, 17.dp)
        repeat(5) { index ->
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(Modifier.size(54.dp).background(
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.shapes.medium,
                    ))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PlaceholderBar(if (index % 2 == 0) 0.75f else 0.9f)
                        PlaceholderBar(0.45f, 10.dp)
                    }
                }
            }
        }
    }
}
