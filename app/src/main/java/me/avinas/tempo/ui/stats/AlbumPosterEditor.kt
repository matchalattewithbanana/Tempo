package me.avinas.tempo.ui.stats

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import me.avinas.tempo.R
import me.avinas.tempo.data.repository.SortBy
import me.avinas.tempo.data.stats.TimeRange
import me.avinas.tempo.ui.components.CachedAsyncImage
import me.avinas.tempo.ui.components.ShareCardBackground
import me.avinas.tempo.ui.components.SharePreviewDialog
import me.avinas.tempo.ui.components.ShareTheme
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlbumPosterEditor(
    period: TimeRange,
    sortBy: SortBy,
    onDismiss: () -> Unit,
    viewModel: AlbumPosterViewModel = hiltViewModel(),
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val title = stringResource(R.string.album_poster_title)
    var selectedSlot by rememberSaveable { mutableStateOf<Int?>(null) }
    var preview by rememberSaveable { mutableStateOf(false) }
    var filling by remember { mutableStateOf(false) }
    var fillNotice by remember { mutableStateOf<Int?>(null) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, stringResource(R.string.share_close))
                        }
                    },
                )
            },
            bottomBar = {
                Surface(tonalElevation = 2.dp) {
                    Button(
                        onClick = { preview = true },
                        enabled = !filling && draft.albums.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    ) {
                        Text(stringResource(R.string.album_poster_preview))
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = Modifier.widthIn(max = 520.dp).fillMaxSize()
                        .verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        stringResource(R.string.album_poster_picked, draft.albums.size, POSTER_ALBUM_LIMIT),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    PosterGrid(
                        albums = draft.albums,
                        surface = MaterialTheme.colorScheme.surfaceContainerHigh,
                        foreground = MaterialTheme.colorScheme.onSurfaceVariant,
                        onSelect = if (filling) null else { index ->
                            selectedSlot = minOf(index, draft.albums.size)
                        },
                    ) { _, album ->
                        CachedAsyncImage(imageUrl = album.artwork, contentDescription = null, modifier = Modifier.fillMaxSize(), targetSizeDp = 144)
                    }
                    OutlinedButton(
                        enabled = !filling && draft.albums.size < POSTER_ALBUM_LIMIT,
                        onClick = {
                            filling = true
                            fillNotice = null
                            scope.launch {
                                try {
                                    if (viewModel.fillFromStats(period, sortBy) == 0) {
                                        fillNotice = R.string.album_poster_no_ranked
                                    }
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) {
                                    fillNotice = R.string.album_poster_search_failed
                                } finally {
                                    filling = false
                                }
                            }
                        },
                    ) {
                        Text(stringResource(if (filling) R.string.ui_loading else R.string.album_poster_fill))
                    }
                    fillNotice?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    OutlinedTextField(
                        value = draft.title ?: stringResource(R.string.album_poster_default_title),
                        onValueChange = viewModel::setTitle,
                        label = { Text(stringResource(R.string.album_poster_heading)) }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = draft.subtitle,
                        onValueChange = viewModel::setSubtitle,
                        label = { Text(stringResource(R.string.album_poster_subtitle)) }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = draft.showLabels, onCheckedChange = viewModel::setLabels)
                        Text(stringResource(R.string.album_poster_labels), modifier = Modifier.padding(start = 12.dp))
                    }
                    draft.albums.forEachIndexed { index, album ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                stringResource(R.string.share_ranked_text, index + 1, album.title),
                                modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis,
                            )
                            IconButton(onClick = { viewModel.move(index, index - 1) }, enabled = index > 0) {
                                Icon(Icons.Default.KeyboardArrowUp, stringResource(R.string.album_poster_move_earlier, album.title))
                            }
                            IconButton(onClick = { viewModel.move(index, index + 1) }, enabled = index < draft.albums.lastIndex) {
                                Icon(Icons.Default.KeyboardArrowDown, stringResource(R.string.album_poster_move_later, album.title))
                            }
                            IconButton(onClick = { viewModel.remove(index) }) {
                                Icon(Icons.Default.Close, stringResource(R.string.album_poster_remove, album.title))
                            }
                        }
                    }
                }
            }
        }
    }

    selectedSlot?.let { slot ->
        PosterAlbumPicker(
            viewModel = viewModel,
            unavailable = draft.albums.filterIndexed { index, _ -> index != slot }.map { it.identity }.toSet(),
            onPick = {
                viewModel.select(slot, it)
                selectedSlot = null
            },
            onDismiss = { selectedSlot = null },
        )
    }
    if (preview) AlbumPosterPreview(draft, viewModel::setTheme, onDismiss = { preview = false })
}

@Composable
private fun PosterAlbumPicker(
    viewModel: AlbumPosterViewModel,
    unavailable: Set<Pair<String, String>>,
    onPick: (PosterAlbum) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val term = query.trim()
    var library by remember(term) { mutableStateOf<List<PosterAlbum>?>(null) }
    var online by remember(term) { mutableStateOf<List<PosterAlbum>?>(null) }
    var catalogBusy by remember(term) { mutableStateOf(false) }
    var error by remember(term) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val locale = LocalConfiguration.current.locales[0]
    val country = (locale.country.takeIf { it.length == 2 } ?: "US").lowercase(Locale.ROOT)

    LaunchedEffect(term) {
        if (term.length < 2) return@LaunchedEffect
        delay(250)
        try {
            library = viewModel.searchLibrary(term)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            error = true
            library = emptyList()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.album_poster_choose)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it.take(100) },
                    label = { Text(stringResource(R.string.album_poster_album_artist)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(enabled = term.length >= 2 && !catalogBusy, onClick = {
                    catalogBusy = true
                    error = false
                    scope.launch {
                        try {
                            online = viewModel.searchCatalog(term, country)
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            error = true
                        } finally {
                            catalogBusy = false
                        }
                    }
                }) {
                    Text(stringResource(if (catalogBusy) R.string.ui_loading else R.string.album_poster_catalog))
                }
                if (error) Text(stringResource(R.string.album_poster_search_failed), color = MaterialTheme.colorScheme.error)
                val results = (library.orEmpty() + online.orEmpty()).distinctBy { it.identity }
                if (term.length >= 2 && library != null && results.isEmpty() && !catalogBusy && !error) {
                    Text(stringResource(R.string.album_poster_no_matches))
                }
                LazyColumn(modifier = Modifier.heightIn(max = 260.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(results) { album ->
                        TextButton(enabled = album.identity !in unavailable, onClick = { onPick(album) }) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                CachedAsyncImage(imageUrl = album.artwork, contentDescription = null,
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(5.dp)), targetSizeDp = 80)
                                Column(Modifier.weight(1f)) {
                                    Text(album.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(album.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.share_close)) } },
    )
}

@Composable
private fun AlbumPosterPreview(draft: AlbumPosterDraft, onThemeSelected: (ShareTheme) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val bitmaps by produceState<List<Bitmap?>?>(null, draft.albums) {
        value = coroutineScope {
            draft.albums.map { album ->
                async {
                    val url = album.artwork ?: return@async null
                    withTimeoutOrNull(10_000L) {
                        try {
                            val request = ImageRequest.Builder(context).data(url).size(384, 384).allowHardware(false).build()
                            (context.imageLoader.execute(request) as? SuccessResult)?.image?.toBitmap()
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            null
                        }
                    }
                }
            }.awaitAll()
        }
    }
    val title = draft.title?.ifBlank { null } ?: stringResource(R.string.album_poster_default_title)
    val text = buildString {
        appendLine(title)
        if (draft.subtitle.isNotBlank()) appendLine(draft.subtitle)
        draft.albums.forEach { appendLine("${it.title} · ${it.artist}") }
    }.trim()
    SharePreviewDialog(
        onDismiss = onDismiss,
        text = text,
        cardHeight = if (draft.showLabels) 565.dp else 480.dp,
        fileName = "tempo-album-poster.png",
        exportEnabled = bitmaps != null,
        initialTheme = draft.theme,
        onThemeSelected = onThemeSelected,
        settings = {
            if (bitmaps == null) Text(stringResource(R.string.album_poster_loading_art))
            else if (bitmaps.orEmpty().any { it == null }) Text(stringResource(R.string.album_poster_missing_art))
        },
    ) { theme ->
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1f)) {
            AlbumPosterCard(title, draft, bitmaps.orEmpty(), theme)
        }
    }
}

@Composable
private fun AlbumPosterCard(title: String, draft: AlbumPosterDraft, bitmaps: List<Bitmap?>, theme: ShareTheme) {
    val palette = theme.palette
    ShareCardBackground(palette = palette, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title, color = palette.textStrong, style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                if (draft.subtitle.isNotBlank()) {
                    Text(draft.subtitle, color = palette.textSecondary, style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            PosterGrid(
                albums = draft.albums,
                surface = palette.surfaceStrong,
                foreground = palette.textSecondary,
                labels = draft.showLabels,
            ) { index, _ ->
                val bitmap = bitmaps.getOrNull(index)
                if (bitmap != null) {
                    Image(bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }
        }
    }
}

@Composable
private fun PosterGrid(
    albums: List<PosterAlbum>,
    surface: Color,
    foreground: Color,
    labels: Boolean = false,
    onSelect: ((Int) -> Unit)? = null,
    cover: @Composable (Int, PosterAlbum) -> Unit,
) {
    val choose = stringResource(R.string.album_poster_choose)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(3) { column ->
                    val index = row * 3 + column
                    val album = albums.getOrNull(index)
                    Column(Modifier.weight(1f)) {
                        val tile = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(8.dp)).background(surface)
                        Box(
                            modifier = if (onSelect == null) tile else tile.clickable { onSelect(index) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (album == null && onSelect != null) Icons.Default.Add else Icons.Rounded.Album,
                                contentDescription = album?.title ?: choose.takeIf { onSelect != null },
                                tint = foreground,
                            )
                            if (album != null) cover(index, album)
                        }
                        if (labels) {
                            Text(album?.title.orEmpty(), color = foreground, style = MaterialTheme.typography.labelSmall,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(album?.artist.orEmpty(), color = foreground, style = MaterialTheme.typography.labelSmall,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
