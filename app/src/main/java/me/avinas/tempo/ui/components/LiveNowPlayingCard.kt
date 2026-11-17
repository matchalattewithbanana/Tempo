package me.avinas.tempo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import me.avinas.tempo.service.SpotifyBroadcastPresence

/**
 * Local, privacy-preserving live presence card shared by Home/Profile/Settings.
 * Artwork is resolved only on track changes and decoded at thumbnail size.
 * The second-by-second timer is isolated below so it does not recompose the
 * cover and track text every second.
 */
@Composable
fun LiveNowPlayingCard(
    modifier: Modifier = Modifier,
    artworkViewModel: NowPlayingArtworkViewModel = hiltViewModel(),
    compact: Boolean = false,
    embedded: Boolean = false,
) {
    val current by SpotifyBroadcastPresence.presence.collectAsStateWithLifecycle()
    val artwork by artworkViewModel.artwork.collectAsStateWithLifecycle()
    val choiceForAccent = artwork?.takeIf {
        it.identity == current?.let { p -> NowPlayingArtworkIdentity(p.packageName, p.title, p.artist, p.album) }
    }
    val accent = me.avinas.tempo.ui.appearance.rememberNowPlayingAccent(
        current?.let { "${it.packageName}|${it.title}|${it.artist}|${it.album}" },
        current?.albumArtUri ?: choiceForAccent?.localUrl ?: choiceForAccent?.remoteUrl,
    )
    val lifecycleOwner = LocalLifecycleOwner.current

    // Slow freshness clock, distinct from the progress ticker. It also expires
    // a paused/stopped presence without keeping a 1 Hz coroutine alive.
    var checkedAtMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lifecycleOwner, current?.updatedAtMs) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            checkedAtMs = System.currentTimeMillis()
            while (true) {
                delay(60_000L)
                checkedAtMs = System.currentTimeMillis()
            }
        }
    }

    val presence = current
    val ageMs = if (presence == null) Long.MAX_VALUE
        else (checkedAtMs - presence.updatedAtMs).coerceAtLeast(0L)
    val isRecent = presence != null && ageMs < 15 * 60_000L

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = appearanceCardShape(),
        colors = CardDefaults.cardColors(
            containerColor = if (embedded) Color.Transparent
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.66f)
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(if (embedded) 0.dp else if (compact) 12.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (ageMs > 2 * 60_000L && isRecent) androidx.compose.ui.res.stringResource(me.avinas.tempo.R.string.now_playing_last_detected) else "NOW PLAYING",
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.Bold,
            )
            if (!isRecent || presence == null) {
                Text(
                    text = "Nothing playing on this device",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Play music with Tempo's tracking service running.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val choice = artwork?.takeIf {
                    it.identity == NowPlayingArtworkIdentity(
                        presence.packageName, presence.title, presence.artist, presence.album
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // The player's own cover is the best match for the
                    // currently playing release. Use the enriched image only
                    // when local / MediaSession artwork cannot be loaded.
                    val preferredArtwork = presence.albumArtUri ?: choice?.localUrl
                    AlbumArtImage(
                        albumArtUrl = preferredArtwork ?: choice?.remoteUrl,
                        localArtUrl = if (preferredArtwork != null) choice?.remoteUrl else null,
                        contentDescription = "Cover art for ${presence.title}",
                        placeholderEmoji = "♫",
                        targetSizePx = 256,
                        modifier = Modifier.size(if (compact) 48.dp else 84.dp).clip(RoundedCornerShape(12.dp)),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = presence.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = listOfNotNull(presence.artist, presence.album)
                                .filter { it.isNotBlank() }.joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (ageMs > 2 * 60_000L) androidx.compose.ui.res.stringResource(me.avinas.tempo.R.string.now_playing_last_detected) else if (presence.playing) "▶ Playing" else "Ⅱ Paused",
                        color = accent,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = if (presence.verifiedByApiAtMs != null &&
                            checkedAtMs - presence.verifiedByApiAtMs < 3 * 60_000L
                        ) "${presence.source} · API verified" else presence.source,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (!compact && ageMs <= 2 * 60_000L) LivePlaybackProgress(presence)

                if (ageMs > 2 * 60_000L) {
                    Text(
                        text = "Last status may be stale; waiting for another playback update",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** The only subtree ticking at 1 Hz, and only while visible and playing. */
@Composable
private fun LivePlaybackProgress(current: SpotifyBroadcastPresence.Presence) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var nowMs by remember(current.updatedAtMs) {
        mutableLongStateOf(System.currentTimeMillis())
    }
    LaunchedEffect(lifecycleOwner, current.updatedAtMs, current.playing) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            nowMs = System.currentTimeMillis()
            while (current.playing) {
                delay(1_000L)
                nowMs = System.currentTimeMillis()
            }
        }
    }

    val ageMs = (nowMs - current.updatedAtMs).coerceAtLeast(0L)
    if (current.listenedMs != null) {
        val liveListenedMs = (current.listenedMs +
            if (current.playing) ageMs else 0L).coerceAtLeast(0L)
        Text(
            text = "Listening this session · ${liveListenedMs / 60_000}:" +
                (liveListenedMs / 1000 % 60).toString().padStart(2, '0'),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Live estimate · saved to statistics after playback ends",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (current.durationMs > 0L) {
        val position = (current.positionMs + if (current.playing) ageMs else 0L)
            .coerceIn(0L, current.durationMs)
        LinearProgressIndicator(
            progress = { position.toFloat() / current.durationMs.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "${position / 60_000}:${(position / 1000 % 60).toString().padStart(2, '0')} / " +
                "${current.durationMs / 60_000}:${(current.durationMs / 1000 % 60).toString().padStart(2, '0')}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
