package me.avinas.tempo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import me.avinas.tempo.R
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
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LiveNowPlayingCard(
    modifier: Modifier = Modifier,
    artworkViewModel: NowPlayingArtworkViewModel = hiltViewModel(),
    compact: Boolean = false,
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

    val scheme = MaterialTheme.colorScheme
    val iconEffects = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    val activePlayback = isRecent && presence?.playing == true && ageMs <= 2 * 60_000L
    val surface by animateColorAsState(
        targetValue = if (activePlayback) scheme.primaryContainer else scheme.surfaceContainerLow,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "playingContainer",
    )
    val foreground by animateColorAsState(
        targetValue = if (activePlayback) scheme.onPrimaryContainer else scheme.onSurface,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "playingForeground",
    )
    val stale = isRecent && ageMs > 2 * 60_000L
    val stateLabel = when {
        !isRecent -> stringResource(R.string.now_playing_idle)
        stale -> stringResource(R.string.now_playing_last_detected)
        presence?.playing == true -> stringResource(R.string.now_playing_playing)
        else -> stringResource(R.string.now_playing_paused)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraExtraLarge,
        colors = CardDefaults.cardColors(containerColor = surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(if (compact) 14.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = MaterialShapes.Cookie6Sided.toShape(),
                    color = if (activePlayback) scheme.onPrimaryContainer.copy(alpha = 0.12f)
                        else scheme.secondaryContainer,
                    contentColor = foreground,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AnimatedContent(
                            targetState = when {
                                activePlayback -> 0
                                isRecent && !stale -> 1
                                else -> 2
                            },
                            transitionSpec = {
                                fadeIn(animationSpec = iconEffects) togetherWith
                                    fadeOut(animationSpec = iconEffects)
                            },
                            label = "nowPlayingStateIcon",
                        ) { playbackState ->
                            Icon(
                                imageVector = when (playbackState) {
                                    0 -> Icons.Rounded.GraphicEq
                                    1 -> Icons.Rounded.PauseCircle
                                    else -> Icons.Rounded.MusicNote
                                },
                                contentDescription = null,
                                modifier = Modifier.size(23.dp),
                            )
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.now_playing_heading),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        color = foreground,
                    )
                    Text(
                        text = stateLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = foreground.copy(alpha = 0.80f),
                    )
                }
                // Artwork-accent selection still affects the tiny ornamental
                // dot, without compromising the semantic text contrast.
                Box(Modifier.size(7.dp).background(accent, CircleShape))
            }
            if (!isRecent || presence == null) {
                Text(
                    text = stringResource(R.string.now_playing_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = foreground,
                )
                Text(
                    text = stringResource(R.string.now_playing_empty_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            } else {
                val choice = artwork?.takeIf {
                    it.identity == NowPlayingArtworkIdentity(
                        presence.packageName, presence.title, presence.artist, presence.album,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Device / MediaSession art remains first choice, with enriched
                    // art used only as fallback. This is not another artwork fetch.
                    val preferredArtwork = presence.albumArtUri ?: choice?.localUrl
                    Surface(
                        modifier = Modifier.size(if (compact) 54.dp else 88.dp),
                        shape = MaterialTheme.shapes.large,
                        color = scheme.surfaceContainerHigh,
                    ) {
                        AlbumArtImage(
                            albumArtUrl = preferredArtwork ?: choice?.remoteUrl,
                            localArtUrl = if (preferredArtwork != null) choice?.remoteUrl else null,
                            contentDescription = stringResource(R.string.now_playing_cover_description, presence.title),
                            placeholderEmoji = "♫",
                            targetSizePx = 256,
                            modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.large),
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(
                            text = presence.title,
                            style = if (compact) MaterialTheme.typography.titleMediumEmphasized
                                else MaterialTheme.typography.titleLargeEmphasized,
                            color = foreground,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = listOfNotNull(presence.artist, presence.album)
                                .filter { it.isNotBlank() }.joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = foreground.copy(alpha = 0.80f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Status is informational: never imply a player-control API.
                    Text(
                        text = stateLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = foreground,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = if (presence.verifiedByApiAtMs != null &&
                            checkedAtMs - presence.verifiedByApiAtMs < 3 * 60_000L
                        ) "${presence.source} · API verified" else presence.source,
                        style = MaterialTheme.typography.labelSmall,
                        color = foreground.copy(alpha = 0.76f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!compact && !stale) LivePlaybackProgress(presence)
                if (stale) {
                    Text(
                        text = stringResource(R.string.now_playing_stale_help),
                        style = MaterialTheme.typography.labelSmall,
                        color = foreground.copy(alpha = 0.76f),
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
