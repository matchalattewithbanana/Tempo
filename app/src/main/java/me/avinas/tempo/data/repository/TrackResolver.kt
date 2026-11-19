package me.avinas.tempo.data.repository

import me.avinas.tempo.data.local.dao.TrackAliasDao
import me.avinas.tempo.data.local.entities.Track
import me.avinas.tempo.utils.ArtistParser
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Layer 3 + Layer 4 of the import reconciliation pipeline.
 *
 * Centralizes track identity resolution so every import source resolves an
 * incoming play to the same existing track row using one consistent, scored
 * strategy — which is what lets Layer 2 (cross-source temporal reconciliation)
 * actually connect a Spotify play and a Last.fm scrobble as the same play.
 *
 * Resolution order (highest confidence first):
 *  1. spotifyId
 *  2. musicbrainzId
 *  3. youtubeId
 *  4. exact title + artist (case-insensitive)
 *  5. the alias recorded for that title + artist — a name the user renamed away
 *     or merged into another row
 *  6. fuzzy title + artist
 * If nothing matches, a new track is created.
 *
 * Layer 4 (metadata merge): when an incoming query matches an existing track,
 * any authoritative IDs the existing track is missing are backfilled — a value
 * is only ever filled in, never overwritten. This gradually enriches tracks
 * created by a lower-fidelity source (e.g. Last.fm) when a higher-fidelity
 * source (e.g. Spotify) later identifies the same song.
 */
@Singleton
class TrackResolver @Inject constructor(
    private val trackRepository: TrackRepository,
    private val trackAliasDao: TrackAliasDao
) {

    /** What an import knows about a track it wants to attach a play to. */
    data class Query(
        val title: String,
        val artist: String,
        val album: String? = null,
        val albumArtUrl: String? = null,
        val duration: Long? = null,
        val spotifyId: String? = null,
        val musicbrainzId: String? = null,
        val youtubeId: String? = null
    )

    /** The outcome of resolving a [Query]. */
    data class Resolution(
        val trackId: Long,
        val isNewTrack: Boolean,
        val track: Track
    )

    /** Resolve [query] to an existing track, or create one if none matches. */
    suspend fun resolve(query: Query, contentType: String = "MUSIC"): Resolution {
        // Blank IDs must be skipped: Last.fm returns mbid="" (not null) for non-MusicBrainz
        // tracks, and a WHERE musicbrainz_id = '' lookup collides with the first track that
        // also has an empty mbid — clubbing every empty-mbid scrobble onto one track row.
        query.spotifyId?.takeIf { it.isNotBlank() }?.let { id ->
            trackRepository.findBySpotifyId(id)?.let {
                if (identifiersCompatible(it, query)) return merge(it, query)
                // Spotify ID is UNIQUE in tracks. Creating a new row with
                // the same ID would make INSERT IGNORE return -1 and corrupt
                // event foreign keys. Preserve the existing canonical row but
                // refuse the conflicting secondary metadata instead.
                return Resolution(it.id, isNewTrack = false, track = it)
            }
        }
        query.musicbrainzId?.takeIf { it.isNotBlank() }?.let { id ->
            trackRepository.findByMusicBrainzId(id)?.let {
                if (identifiersCompatible(it, query)) return merge(it, query)
            }
        }
        query.youtubeId?.takeIf { it.isNotBlank() }?.let { id ->
            trackRepository.findByYoutubeId(id)?.let {
                if (identifiersCompatible(it, query)) return merge(it, query)
                // Youtube ID is UNIQUE in tracks. Creating a new row with
                // the same ID would make INSERT IGNORE return -1 and corrupt
                // event foreign keys. Preserve the existing canonical row but
                // refuse the conflicting secondary metadata instead.
                return Resolution(it.id, isNewTrack = false, track = it)
            }
        }
        trackRepository.findByTitleAndArtist(query.title, query.artist)?.let {
            if (identifiersCompatible(it, query)) return merge(it, query)
        }

        // A name the user renamed away or merged into another row still means that song.
        // Without this, every later import of the old title re-created the duplicate the
        // user had just cleaned up — and because the play then lands on a fresh track_id,
        // neither the content fingerprint nor Layer 2's per-track reconciliation can
        // recognise it as a repeat, so the play counts split for real.
        // Exact title first, alias second: a row that literally carries this title is the
        // better answer, which also means duplicates created before this step existed are
        // left alone rather than silently absorbed.
        trackAliasDao.findAliasTargetTrack(query.title, query.artist)?.let {
            if (identifiersCompatible(it, query)) return merge(it, query)
        }

        trackRepository.findByTitleAndArtistFuzzy(query.title, query.artist)?.let {
            // SQL's substring search is a candidate lookup. Complete artist credits
            // must intersect before this candidate can absorb another recording.
            if (identifiersCompatible(it, query) && sharesCreditedArtist(it.artist, query.artist)) {
                return merge(it, query)
            }
        }

        val track = Track(
            title = query.title,
            artist = query.artist,
            album = query.album,
            duration = query.duration,
            albumArtUrl = query.albumArtUrl,
            spotifyId = query.spotifyId,
            youtubeId = query.youtubeId,
            musicbrainzId = query.musicbrainzId,
            primaryArtistId = null,
            contentType = contentType
        )
        val newId = trackRepository.insert(track)
        if (newId <= 0L) {
            // Room's INSERT IGNORE can return -1 if another observer created
            // this provider ID after our initial lookup. Never create an event
            // pointing to track_id=-1: retry identity resolution once.
            val racedTrack = query.spotifyId?.takeIf { it.isNotBlank() }
                ?.let { trackRepository.findBySpotifyId(it) }
                ?: query.youtubeId?.takeIf { it.isNotBlank() }
                    ?.let { trackRepository.findByYoutubeId(it) }
                ?: query.musicbrainzId?.takeIf { it.isNotBlank() }
                    ?.let { trackRepository.findByMusicBrainzId(it) }
                ?: trackRepository.findByTitleAndArtist(query.title, query.artist)
            if (racedTrack != null) {
                return Resolution(racedTrack.id, isNewTrack = false, track = racedTrack)
            }
            error("Track could not be created or resolved: insert returned $newId")
        }
        return Resolution(newId, isNewTrack = true, track = track.copy(id = newId))
    }

    /**
     * Two recordings with different authoritative IDs must not share a track row,
     * even when a fuzzy title/artist search says they look alike.
     */
    internal fun identifiersCompatible(existing: Track, query: Query): Boolean =
        !((!existing.spotifyId.isNullOrBlank() && !query.spotifyId.isNullOrBlank()
                && existing.spotifyId != query.spotifyId) ||
          (!existing.musicbrainzId.isNullOrBlank() && !query.musicbrainzId.isNullOrBlank()
                && existing.musicbrainzId != query.musicbrainzId) ||
          (!existing.youtubeId.isNullOrBlank() && !query.youtubeId.isNullOrBlank()
                && existing.youtubeId != query.youtubeId))

    private fun sharesCreditedArtist(existing: String, incoming: String): Boolean {
        val credits = ArtistParser.getAllArtists(existing)
        return ArtistParser.getAllArtists(incoming).any { artist ->
            !ArtistParser.isUnknownArtist(artist) && credits.any {
                !ArtistParser.isUnknownArtist(it) && ArtistParser.isStrictSameArtist(it, artist)
            }
        }
    }

    /** Layer 4 for one track: gap-fill only, never overwrite. */
    private suspend fun merge(existing: Track, query: Query): Resolution {
        var updated = existing
        var dirty = false

        if (query.spotifyId != null && existing.spotifyId == null) {
            updated = updated.copy(spotifyId = query.spotifyId)
            dirty = true
        }
        if (query.musicbrainzId != null && existing.musicbrainzId == null) {
            updated = updated.copy(musicbrainzId = query.musicbrainzId)
            dirty = true
        }
        if (query.youtubeId != null && existing.youtubeId == null) {
            trackRepository.updateYoutubeIdIfMissing(existing.id, query.youtubeId)
            updated = updated.copy(youtubeId = query.youtubeId)
        }
        if (query.album != null && existing.album == null) {
            updated = updated.copy(album = query.album)
            dirty = true
        }
        val automaticArtworkCandidate = query.albumArtUrl
            ?.takeIf { it.isNotBlank() && existing.albumArtUrl.isNullOrBlank() }

        if (query.duration != null && existing.duration == null) {
            updated = updated.copy(duration = query.duration)
            dirty = true
        }
        if (dirty) {
            trackRepository.update(updated)
        }

        // Artwork has a separate guarded write path. Generic full-row updates deliberately
        // preserve the currently stored artwork, so routing automatic backfill through
        // update() would silently discard a newly discovered cover.
        if (automaticArtworkCandidate != null) {
            val persistedArtwork = trackRepository.updateAutomaticAlbumArtUrl(
                existing.id,
                automaticArtworkCandidate,
            )
            updated = updated.copy(albumArtUrl = persistedArtwork)
        }

        return Resolution(existing.id, isNewTrack = false, track = updated)
    }
}
