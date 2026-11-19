package me.avinas.tempo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

internal data class ShareSnapshot(
    val title: String,
    val subtitle: String? = null,
    val artUrl: String? = null,
    val metrics: List<Pair<String, String>> = emptyList(),
    val rows: List<Pair<String, String>> = emptyList(),
) {
    fun asText(): String = buildString {
        appendLine(title)
        subtitle?.takeIf(String::isNotBlank)?.let { appendLine(it) }
        for ((label, value) in metrics) appendLine("$label · $value")
        for ((name, detail) in rows) {
            appendLine(if (detail.isBlank()) name else "$name — $detail")
        }
    }.trim()
}

@Composable
internal fun SnapshotShareDialog(
    snapshot: ShareSnapshot,
    onDismiss: () -> Unit,
    settings: (@Composable () -> Unit)? = null,
) {
    SharePreviewDialog(onDismiss = onDismiss, text = snapshot.asText(), settings = settings) { theme ->
        SnapshotShareCard(snapshot, theme)
    }
}

@Composable
private fun SnapshotShareCard(snapshot: ShareSnapshot, theme: ShareTheme) {
    val palette = theme.palette
    ShareCardBackground(imageUrl = snapshot.artUrl, palette = palette, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 38.dp, bottom = 70.dp),
            verticalArrangement = if (snapshot.rows.isEmpty()) Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
                else Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                if (!snapshot.artUrl.isNullOrBlank()) {
                    CachedAsyncImage(
                        imageUrl = snapshot.artUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(72.dp).clip(palette.thumbShape),
                        targetSizeDp = 144,
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        snapshot.title, color = palette.textStrong, style = MaterialTheme.typography.headlineSmall,
                        fontWeight = palette.headlineWeight ?: FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    if (!snapshot.subtitle.isNullOrBlank()) {
                        Text(snapshot.subtitle, color = palette.textSecondary, style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (snapshot.metrics.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(palette.cardShape).background(palette.surfaceStrong).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    snapshot.metrics.forEach { (label, value) ->
                        Column(Modifier.weight(1f)) {
                            Text(value, color = palette.textStrong, style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(label, color = palette.textSecondary, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            snapshot.rows.forEach { (name, detail) ->
                Column(
                    modifier = Modifier.fillMaxWidth().clip(palette.thumbShape).background(palette.surface)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(name, color = palette.textPrimary, style = MaterialTheme.typography.titleSmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (detail.isNotBlank()) {
                        Text(detail, color = palette.textSecondary, style = MaterialTheme.typography.bodySmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
