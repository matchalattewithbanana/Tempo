package me.avinas.tempo.service

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import java.lang.reflect.Proxy
import me.avinas.tempo.data.TrackingDecision
import me.avinas.tempo.data.local.entities.AppPreference
import me.avinas.tempo.data.local.entities.ManualContentMark
import me.avinas.tempo.data.local.entities.UserPreferences
import me.avinas.tempo.data.preferences.TrackingRulesPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class PolicyRulesContext : ContextWrapper(null) {
    private val values = mutableMapOf<String, Any>()

    private val editor = Proxy.newProxyInstance(
        SharedPreferences.Editor::class.java.classLoader,
        arrayOf(SharedPreferences.Editor::class.java),
    ) { proxy, method, args ->
        when (method.name) {
            "putLong", "putBoolean" -> {
                values[args!![0] as String] = args[1]!!
                proxy
            }
            "remove" -> {
                values.remove(args!![0] as String)
                proxy
            }
            "apply" -> null
            "commit" -> true
            else -> error("Unexpected editor call: ${method.name}")
        }
    } as SharedPreferences.Editor

    private val preferences = Proxy.newProxyInstance(
        SharedPreferences::class.java.classLoader,
        arrayOf(SharedPreferences::class.java),
    ) { _, method, args ->
        when (method.name) {
            "getLong" -> values[args!![0]] as? Long ?: args[1]
            "getBoolean" -> values[args!![0]] as? Boolean ?: args[1]
            "contains" -> values.containsKey(args!![0])
            "edit" -> editor
            else -> error("Unexpected preference call: ${method.name}")
        }
    } as SharedPreferences

    override fun getApplicationContext(): Context = this

    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = preferences
}

class TrackingRuntimePolicyTest {
    private val rules = TrackingRulesPreferences(PolicyRulesContext())
    private val policy = TrackingRuntimePolicy(rules)

    @Test
    fun appSnapshotPreservesCuratedAllowlistWhileHonoringExplicitDisable() {
        assertEquals(
            TrackingDecision.ALLOW,
            policy.decidePackage("com.spotify.music"),
        )

        policy.updateAppPreferences(
            listOf(
                AppPreference(
                    packageName = "com.spotify.music",
                    displayName = "Spotify",
                    isEnabled = false,
                ),
            ),
        )

        assertTrue(policy.appPreferencesReady)
        assertEquals(
            TrackingDecision.BLOCKED,
            policy.decidePackage("com.spotify.music"),
        )
    }

    @Test
    fun userPreferenceSnapshotUpdatesAllTrackingFieldsTogether() {
        policy.updateUserPreferences(
            UserPreferences(
                mergeAlternateVersions = false,
                filterPodcasts = false,
                filterAudiobooks = false,
            ),
        )

        assertFalse(policy.mergeAlternateVersions)
        assertEquals(
            TrackingRuntimePolicy.MediaVerdict.DEFER,
            policy.evaluateMedia(
                packageName = "au.com.shiftyjelly.pocketcasts",
                title = "Episode 5",
                artist = "Podcast",
            ),
        )
        assertEquals(
            TrackingRuntimePolicy.MediaVerdict.DEFER,
            policy.evaluateMedia(
                packageName = "com.audible.application",
                title = "Chapter 5",
                artist = "Author",
            ),
        )
    }

    @Test
    fun alwaysMusicIsOneAuthoritativeOverrideAcrossAllHeuristics() {
        policy.updateManualContentMarks(
            listOf(
                mark(
                    contentType = "ALWAYS_MUSIC",
                    title = "Long live set",
                    artist = "Artist",
                ),
            ),
        )

        assertEquals(
            TrackingRuntimePolicy.MediaVerdict.FORCE_ALLOW,
            policy.evaluateMedia(
                packageName = "au.com.shiftyjelly.pocketcasts",
                title = "Long live set",
                artist = "Artist",
                durationMs = 4 * 60 * 60 * 1000L,
                metadata = LocalMediaMetadata(
                    title = "Long live set",
                    artist = "Artist",
                    genre = "podcast",
                    durationMs = 4 * 60 * 60 * 1000L,
                ),
            ),
        )
    }

    @Test
    fun nonMusicOverrideRejectsBeforeOrdinaryHeuristics() {
        policy.updateManualContentMarks(
            listOf(
                mark(
                    contentType = "NON_MUSIC",
                    title = "Voice note",
                    artist = "Friend",
                ),
            ),
        )

        assertEquals(
            TrackingRuntimePolicy.MediaVerdict.REJECT,
            policy.evaluateMedia(
                packageName = "com.example.player",
                title = "Voice note",
                artist = "Friend",
            ),
        )
    }

    @Test
    fun durationAndContentFiltersShareOneVerdictPath() {
        assertEquals(
            TrackingRuntimePolicy.MediaVerdict.REJECT,
            policy.evaluateMedia(
                packageName = "com.spotify.music",
                title = "Very long programme",
                artist = "Artist",
                durationMs = 21 * 60 * 1000L,
            ),
        )

        assertEquals(
            TrackingRuntimePolicy.MediaVerdict.REJECT,
            policy.evaluateMedia(
                packageName = "com.spotify.music",
                title = "Episode 12",
                artist = "Host",
                metadata = LocalMediaMetadata(
                    title = "Episode 12",
                    artist = "Host",
                    genre = "podcast",
                ),
            ),
        )
    }

    @Test
    fun trackingUnrecognizedSwitchRemainsPartOfPackagePolicy() {
        assertEquals(
            TrackingDecision.UNKNOWN,
            policy.decidePackage("com.example.niche"),
        )

        rules.trackUnrecognizedApps = true

        assertEquals(
            TrackingDecision.ALLOW,
            policy.decidePackage("com.example.niche"),
        )
    }

    private fun mark(
        contentType: String,
        title: String,
        artist: String,
    ) = ManualContentMark(
        targetTrackId = 0,
        patternType = "TITLE_ARTIST",
        originalTitle = title,
        originalArtist = artist,
        patternValue = title,
        contentType = contentType,
    )
}
