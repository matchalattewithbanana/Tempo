@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package me.avinas.tempo.ui.details

import me.avinas.tempo.ui.effects.tempoAmbientBackdrop
import me.avinas.tempo.ui.effects.tempoTiltSurface
import me.avinas.tempo.ui.effects.tempoListMotion

/* Tempo · AlbumDetails · Editorial album listening profile.
 * Shares the SongDetails design language: art-as-atmosphere background,
 * collapsing header title, frosted top-bar actions, a conditioned
 * dominant-color accent and the Obsidian stat masthead — extended with
 * the album's track sheet and inline edit mode.
 */

import me.avinas.tempo.ui.components.ShareSnapshot
import me.avinas.tempo.ui.components.SnapshotShareDialog
import androidx.compose.material.icons.filled.Share
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import me.avinas.tempo.R
import me.avinas.tempo.data.enrichment.MusicBrainzEnrichmentService
import me.avinas.tempo.data.local.entities.Track
import me.avinas.tempo.data.stats.AlbumDetails
import me.avinas.tempo.data.stats.TrackWithStats
import me.avinas.tempo.ui.components.AlbumArtImage
import me.avinas.tempo.ui.components.ArtAtmosphereLayer
import me.avinas.tempo.ui.components.DeepOceanBackground
import me.avinas.tempo.ui.components.GlassCard
import me.avinas.tempo.ui.components.GlassCardVariant
import me.avinas.tempo.ui.components.TempoDialogShape
import me.avinas.tempo.ui.theme.*
import me.avinas.tempo.ui.components.TempoDropdownMenu
import me.avinas.tempo.ui.components.TempoDropdownMenuItem
import me.avinas.tempo.ui.components.TempoDialogSurface
import me.avinas.tempo.ui.components.TempoDialogIcon
import me.avinas.tempo.ui.components.TempoDialogTitle
import me.avinas.tempo.ui.components.TempoDialogBody
import me.avinas.tempo.ui.components.TempoDialogDangerButton
import me.avinas.tempo.ui.components.TempoDialogSecondaryButton
import me.avinas.tempo.ui.components.TempoIcons
import java.util.Locale

@Composable
internal fun AddTrackToAlbumDialog(
    query: String,
    results: List<Track>,
    isSearching: Boolean,
    artistName: String,
    onQueryChange: (String) -> Unit,
    onTrackSelected: (Track) -> Unit,
    onDismiss: () -> Unit,
) {
    val appearancePalette = LocalTempoPalette.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .clip(TempoDialogShape.shape)
                .background(appearancePalette.elevatedSurface)
                .border(1.dp, appearancePalette.border.copy(alpha = 0.45f), TempoDialogShape.shape)
                .padding(20.dp)
        ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.album_add_song_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = appearancePalette.onSurface,
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(GlassFrostSoft)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.album_close_cd),
                            tint = appearancePalette.mutedText,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (artistName.isNotEmpty()) {
                        stringResource(R.string.album_add_song_desc, artistName)
                    } else {
                        stringResource(R.string.album_add_song_search)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = appearancePalette.mutedText,
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            stringResource(R.string.album_add_song_search),
                            color = appearancePalette.mutedText,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null,
                            tint = appearancePalette.mutedText,
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = appearancePalette.onSurface,
                        unfocusedTextColor = appearancePalette.onSurface,
                        cursorColor = appearancePalette.accent,
                        focusedBorderColor = appearancePalette.accent,
                        unfocusedBorderColor = TextQuaternary,
                    ),
                )
                Spacer(modifier = Modifier.height(16.dp))
                when {
                    isSearching -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = appearancePalette.accent)
                        }
                    }

                    results.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.album_no_candidates),
                                color = appearancePalette.mutedText,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(items = results, key = { it.id }) { track ->
                                TrackSearchItem(track = track) { onTrackSelected(track) }
                            }
                        }
                    }
                }
            }
    }
}

@Composable
internal fun RemoveFromAlbumDialog(
    trackTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val appearancePalette = LocalTempoPalette.current
    Dialog(onDismissRequest = onDismiss) {
        TempoDialogSurface {
            TempoDialogIcon(
                icon = TempoIcons.Trash,
                tint = TempoError,
                size = 48
            )
            Spacer(modifier = Modifier.height(16.dp))
            TempoDialogTitle(text = stringResource(R.string.album_remove_title))
            Spacer(modifier = Modifier.height(8.dp))
            TempoDialogBody(text = stringResource(R.string.album_remove_msg, trackTitle))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.album_remove_note),
                style = MaterialTheme.typography.bodySmall,
                color = appearancePalette.mutedText,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            TempoDialogDangerButton(
                text = stringResource(R.string.common_remove),
                onClick = onConfirm,
                icon = TempoIcons.Trash
            )
            Spacer(modifier = Modifier.height(6.dp))
            TempoDialogSecondaryButton(
                text = stringResource(R.string.common_cancel),
                onClick = onDismiss
            )
        }
    }
}

internal fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
