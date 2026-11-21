package me.avinas.tempo.ui.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import me.avinas.tempo.R
import me.avinas.tempo.ui.appearance.AppearancePaletteResolver
import me.avinas.tempo.data.local.dao.HistoryItem
import me.avinas.tempo.ui.components.*
import me.avinas.tempo.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryListItem(
    item: HistoryItem, 
    index: Int, 
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMarkContent: ((String, Boolean) -> Unit)? = null, // type, deleteFromHistory
    onMarkArtist: ((String, Boolean) -> Unit)? = null // type, deleteFromHistory - artist level
) {
    var showMenu by remember { mutableStateOf(false) }
    val walkthroughController = me.avinas.tempo.ui.components.LocalWalkthroughController.current
    val haptics = LocalHapticFeedback.current

    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .combinedClickable(
                    onClick = {
                        walkthroughController.dismiss()
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    },
                    onLongClick = {
                        walkthroughController.dismiss()
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = true
                    },
                ),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    if (!me.avinas.tempo.ui.theme.LocalTempoAppearance.current.showHistoryArtwork || item.album_art_url.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        CachedAsyncImage(
                            imageUrl = item.album_art_url,
                            contentDescription = "Album art for ${item.title}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            targetSizeDp = 52
                        )
                    }
                    
                    // Visual Badge for Non-Music Content on Art
                    if (item.content_type != "MUSIC") {
                        val badgeColor = when(item.content_type) {
                            "PODCAST" -> TempoCyan
                            "AUDIOBOOK" -> GoldenAmber
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                        }
                        
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(badgeColor)
                                .size(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (item.content_type == "PODCAST") 
                                    TempoIcons.Podcast 
                                else 
                                    TempoIcons.Audiobook,
                                contentDescription = item.content_type,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(14.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        
                        // Desktop source tag
                        if (item.source.startsWith("desktop:")) {
                            val palette = LocalTempoPalette.current
                            val badgeColor = remember(palette) {
                                AppearancePaletteResolver.readableAccent(palette, Color(0xFFA78BFA), palette.surfaceSunken)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(badgeColor.copy(alpha = 0.10f))
                                    .border(0.8.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "DESK",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                                    color = badgeColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.width(10.dp))
    
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.height(52.dp)
                ) {
                    Text(
                        text = formatRelativeTime(item.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                        fontWeight = FontWeight.Medium
                    )
                    
                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDeleteClick()
                        },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = TempoIcons.Trash,
                            contentDescription = stringResource(R.string.history_delete_button),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
        
        // Context Menu for Blocking Content
        TempoDropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            TempoMenuKicker(text = stringResource(R.string.tracking_always_music))
            TempoDropdownMenuItem(
                title = stringResource(R.string.history_always_music_track),
                leadingIcon = Icons.Rounded.MusicNote,
                leadingIconTint = MaterialTheme.colorScheme.primary,
                onClick = {
                    onMarkContent?.invoke("ALWAYS_MUSIC", false)
                    showMenu = false
                }
            )
            TempoDropdownMenuItem(
                title = stringResource(R.string.history_always_music_artist, item.artist),
                leadingIcon = Icons.Rounded.MusicNote,
                leadingIconTint = MaterialTheme.colorScheme.primary,
                enabled = !me.avinas.tempo.utils.ArtistParser.isUnknownArtist(item.artist),
                onClick = {
                    onMarkArtist?.invoke("ALWAYS_MUSIC", false)
                    showMenu = false
                }
            )

            TempoMenuDivider()

            TempoMenuKicker(text = stringResource(R.string.history_block_track))
            TempoDropdownMenuItem(
                title = stringResource(R.string.history_its_a_podcast),
                subtitle = stringResource(R.string.history_remove_and_block),
                leadingIcon = TempoIcons.Podcast,
                leadingIconTint = TempoCyan,
                onClick = {
                    onMarkContent?.invoke("PODCAST", true)
                    showMenu = false
                }
            )
            TempoDropdownMenuItem(
                title = stringResource(R.string.history_its_an_audiobook),
                subtitle = stringResource(R.string.history_remove_and_block),
                leadingIcon = TempoIcons.Audiobook,
                leadingIconTint = GoldenAmber,
                onClick = {
                    onMarkContent?.invoke("AUDIOBOOK", true)
                    showMenu = false
                }
            )
            TempoDropdownMenuItem(
                title = stringResource(R.string.history_video_non_music_track),
                subtitle = stringResource(R.string.history_video_non_music_track_description),
                leadingIcon = Icons.Default.PlayCircle,
                leadingIconTint = TempoError,
                isDestructive = true,
                onClick = {
                    onMarkContent?.invoke("NON_MUSIC", true)
                    showMenu = false
                }
            )

            TempoMenuDivider()

            TempoMenuKicker(
                text = stringResource(R.string.history_block_entire_artist),
                color = TempoError.copy(alpha = 0.8f)
            )
            TempoDropdownMenuItem(
                title = stringResource(R.string.history_artist_is_podcast, item.artist),
                subtitle = stringResource(R.string.history_remove_all_from_source),
                leadingIcon = TempoIcons.Podcast,
                leadingIconTint = TempoError,
                isDestructive = true,
                onClick = {
                    onMarkArtist?.invoke("PODCAST", true)
                    showMenu = false
                }
            )
            TempoDropdownMenuItem(
                title = stringResource(R.string.history_artist_is_audiobook, item.artist),
                subtitle = stringResource(R.string.history_remove_all_from_source),
                leadingIcon = TempoIcons.Audiobook,
                leadingIconTint = TempoError,
                isDestructive = true,
                onClick = {
                    onMarkArtist?.invoke("AUDIOBOOK", true)
                    showMenu = false
                }
            )
            TempoDropdownMenuItem(
                title = stringResource(R.string.history_video_non_music_artist, item.artist),
                subtitle = stringResource(R.string.history_video_non_music_artist_description),
                leadingIcon = Icons.Default.Close,
                leadingIconTint = TempoError,
                isDestructive = true,
                onClick = {
                    onMarkArtist?.invoke("NON_MUSIC", true)
                    showMenu = false
                }
            )

        }
    }
}

fun formatRelativeTime(timestamp: Long): String {
    val now = Instant.now()
    val time = Instant.ofEpochMilli(timestamp)
    val diff = ChronoUnit.MINUTES.between(time, now)

    return when {
        diff < 1 -> "Just now"
        diff < 60 -> "${diff}m ago"
        diff < 24 * 60 -> "${diff / 60}h ago"
        else -> {
            val date = time.atZone(ZoneId.systemDefault()).toLocalDate()
            val today = now.atZone(ZoneId.systemDefault()).toLocalDate()
            if (date.isEqual(today.minusDays(1))) "Yesterday"
            else date.format(RelativeDateFormatter)
        }
    }
}

@Composable
fun HistoryEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp, horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.history_empty_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.history_empty_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SearchEmptyState(query: String, onClear: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp, horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(GlassFrostSoft)
                    .border(1.dp, GlassBorderSoft, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No scrobbles found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "No matches for \"$query\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onClear) {
                Text(
                    text = stringResource(R.string.stats_search_clear),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
