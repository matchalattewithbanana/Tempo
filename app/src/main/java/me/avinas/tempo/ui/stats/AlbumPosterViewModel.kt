package me.avinas.tempo.ui.stats

import android.os.Bundle
import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.avinas.tempo.data.local.dao.AlbumDao
import me.avinas.tempo.data.remote.itunes.iTunesApi
import me.avinas.tempo.data.repository.SortBy
import me.avinas.tempo.data.repository.StatsRepository
import me.avinas.tempo.data.stats.TimeRange
import me.avinas.tempo.ui.components.ShareTheme
import java.io.IOException
import javax.inject.Inject

internal const val POSTER_ALBUM_LIMIT = 9

internal data class PosterAlbum(val title: String, val artist: String, val artwork: String?) {
    val identity: Pair<String, String> get() = title.trim().lowercase() to artist.trim().lowercase()

    fun bounded(): PosterAlbum = copy(
        title = title.take(180), artist = artist.take(180), artwork = artwork?.takeIf { it.length <= 4096 },
    )
}

internal data class AlbumPosterDraft(
    val title: String? = null,
    val subtitle: String = "",
    val showLabels: Boolean = true,
    val theme: ShareTheme? = null,
    val albums: List<PosterAlbum> = emptyList(),
)

@HiltViewModel
internal class AlbumPosterViewModel @Inject constructor(
    private val state: SavedStateHandle,
    private val albumDao: AlbumDao,
    private val stats: StatsRepository,
    private val catalog: iTunesApi,
) : ViewModel() {
    private val _draft = MutableStateFlow(readDraft())
    val draft = _draft.asStateFlow()

    fun setTitle(value: String) = save(_draft.value.copy(title = value.take(72)))
    fun setSubtitle(value: String) = save(_draft.value.copy(subtitle = value.take(100)))
    fun setLabels(value: Boolean) = save(_draft.value.copy(showLabels = value))
    fun setTheme(value: ShareTheme) = save(_draft.value.copy(theme = value))

    fun select(index: Int, album: PosterAlbum) {
        val current = _draft.value
        val bounded = album.bounded()
        if (current.albums.withIndex().any { (i, other) -> i != index && other.identity == bounded.identity }) return
        val albums = current.albums.toMutableList()
        if (index in albums.indices) albums[index] = bounded
        else if (albums.size < POSTER_ALBUM_LIMIT) albums.add(bounded)
        save(current.copy(albums = albums))
    }

    fun move(index: Int, target: Int) {
        val current = _draft.value
        if (index !in current.albums.indices || target !in current.albums.indices || index == target) return
        val moved = current.albums.toMutableList()
        moved.add(target, moved.removeAt(index))
        save(current.copy(albums = moved))
    }

    fun remove(index: Int) {
        val current = _draft.value
        save(current.copy(albums = current.albums.filterIndexed { position, _ -> position != index }))
    }

    suspend fun fillFromStats(period: TimeRange, sort: SortBy): Int {
        val ranked = stats.getTopAlbums(period, sort, pageSize = 24, withLeeway = false)
            .items.map { PosterAlbum(it.album, it.artist, it.albumArtUrl).bounded() }
        val current = _draft.value
        val albums = (current.albums + ranked).distinctBy { it.identity }.take(POSTER_ALBUM_LIMIT)
        save(current.copy(albums = albums))
        return albums.size - current.albums.size
    }

    suspend fun searchLibrary(query: String): List<PosterAlbum> =
        albumDao.searchAlbumsWithArtist(query, limit = 15).map {
            PosterAlbum(it.title, it.artistName, it.artworkUrl).bounded()
        }.distinctBy { it.identity }

    suspend fun searchCatalog(query: String, country: String): List<PosterAlbum> = catalogLock.withLock {
        val wait = nextCatalogRequest - SystemClock.elapsedRealtime()
        if (wait > 0) delay(wait)
        nextCatalogRequest = SystemClock.elapsedRealtime() + iTunesApi.RATE_LIMIT_MS
        val response = catalog.search(term = query, limit = 15, country = country)
        if (!response.isSuccessful) throw IOException("Catalog search failed")
        response.body()?.results.orEmpty().mapNotNull {
            val title = it.collectionName?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val artist = it.artistName?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            PosterAlbum(title, artist, it.getBestArtworkUrl()).bounded()
        }.distinctBy { it.identity }
    }

    private fun save(value: AlbumPosterDraft) {
        state[STATE_KEY] = Bundle().apply {
            putString("title", value.title)
            putString("subtitle", value.subtitle)
            putBoolean("labels", value.showLabels)
            putString("theme", value.theme?.name)
            putStringArrayList("titles", ArrayList(value.albums.map { it.title }))
            putStringArrayList("artists", ArrayList(value.albums.map { it.artist }))
            putStringArrayList("art", ArrayList(value.albums.map { it.artwork.orEmpty() }))
        }
        _draft.value = value
    }

    private fun readDraft(): AlbumPosterDraft {
        val data = state.get<Bundle>(STATE_KEY) ?: return AlbumPosterDraft()
        val titles = data.getStringArrayList("titles").orEmpty()
        val artists = data.getStringArrayList("artists").orEmpty()
        val art = data.getStringArrayList("art").orEmpty()
        return AlbumPosterDraft(
            title = data.getString("title"),
            subtitle = data.getString("subtitle").orEmpty(),
            showLabels = data.getBoolean("labels", true),
            theme = ShareTheme.entries.firstOrNull { it.name == data.getString("theme") },
            albums = titles.take(POSTER_ALBUM_LIMIT).mapIndexed { index, title ->
                PosterAlbum(title, artists.getOrElse(index) { "" }, art.getOrNull(index)?.ifBlank { null })
            },
        )
    }

    private companion object {
        const val STATE_KEY = "album-poster-draft"
        val catalogLock = Mutex()
        var nextCatalogRequest = 0L
    }
}
