package me.avinas.tempo.service

import java.security.MessageDigest

/** Stable short key for per-track local caches that identify a play by title + artist. */
internal object PlaybackIdentityKey {
    fun of(
        title: String,
        artist: String,
    ): String {
        val raw = "$title|$artist".lowercase()
        val digest = MessageDigest.getInstance("MD5").digest(raw.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }.take(16)
    }
}
