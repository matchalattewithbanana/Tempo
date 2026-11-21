package me.avinas.tempo.service

import me.avinas.tempo.data.AppTrackingPolicy
import me.avinas.tempo.data.DefaultMusicApps
import me.avinas.tempo.data.TrackingDecision
import me.avinas.tempo.data.local.entities.AppPreference
import me.avinas.tempo.data.local.entities.ManualContentMark
import me.avinas.tempo.data.local.entities.ManualContentRuleResolver
import me.avinas.tempo.data.local.entities.UserPreferences
import me.avinas.tempo.data.preferences.TrackingRulesPreferences

/**
 * Canonical runtime policy for whether Tempo should journal a playback candidate.
 *
 * Android callbacks should not know the precedence between app allow/block state,
 * manual content corrections, podcast/audiobook preferences and duration limits.
 * They provide the candidate; this module hides those policy details behind one
 * stable decision.
 *
 * The service feeds immutable Room snapshots through [updateAppPreferences],
 * [updateUserPreferences] and [updateManualContentMarks]. Reads are synchronous
 * and lock-free on the hot callback path.
 */
internal class TrackingRuntimePolicy(
    private val trackingRules: TrackingRulesPreferences,
) {
    enum class MediaVerdict {
        /** The user explicitly marked this media as music; bypass heuristics and duration limits. */
        FORCE_ALLOW,

        /** A user rule, configured filter or duration limit rejects this media. */
        REJECT,

        /** Policy has no objection; callers may continue with ordinary music heuristics. */
        DEFER,
    }

    private data class AppState(
        val enabled: Set<String> = emptySet(),
        val blocked: Set<String> = emptySet(),
        val disabled: Set<String> = emptySet(),
        val initialized: Boolean = false,
    )

    @Volatile
    private var appState = AppState()

    private data class UserState(
        val mergeAlternateVersions: Boolean = true,
        val filterPodcasts: Boolean = true,
        val filterAudiobooks: Boolean = true,
    )

    @Volatile
    private var userState = UserState()

    @Volatile
    private var manualContentMarks: List<ManualContentMark> = emptyList()

    val appPreferencesReady: Boolean
        get() = appState.initialized

    val minimumPlayDurationMs: Long
        get() = trackingRules.minimumPlayDurationMs

    val mergeAlternateVersions: Boolean
        get() = userState.mergeAlternateVersions

    /** Returns true when package eligibility changed and active sessions need re-evaluation. */
    fun updateAppPreferences(apps: List<AppPreference>): Boolean {
        val updated =
            AppState(
                enabled = apps.asSequence()
                    .filter { it.isEnabled && !it.isBlocked }
                    .map { it.packageName }
                    .toSet(),
                blocked = apps.asSequence()
                    .filter { it.isBlocked }
                    .map { it.packageName }
                    .toSet(),
                disabled = apps.asSequence()
                    .filter { !it.isEnabled && !it.isBlocked }
                    .map { it.packageName }
                    .toSet(),
                initialized = true,
            )
        val changed = updated != appState
        appState = updated
        return changed
    }

    /**
     * Updates only tracking-relevant user preferences.
     *
     * Returns true only when current playback eligibility changed. A matching-only change does not
     * justify rescanning MediaSessions/notifications already in progress.
     */
    fun updateUserPreferences(preferences: UserPreferences?): Boolean {
        val source = preferences ?: UserPreferences()
        val updated =
            UserState(
                mergeAlternateVersions = source.mergeAlternateVersions,
                filterPodcasts = source.filterPodcasts,
                filterAudiobooks = source.filterAudiobooks,
            )
        val previous = userState
        userState = updated
        return previous.filterPodcasts != updated.filterPodcasts ||
            previous.filterAudiobooks != updated.filterAudiobooks
    }

    /** Returns true when manual media eligibility changed. */
    fun updateManualContentMarks(marks: List<ManualContentMark>): Boolean {
        // Room emits a fresh list. Copy once so callback reads never observe caller mutation.
        val updated = marks.toList()
        val changed = updated != manualContentMarks
        manualContentMarks = updated
        return changed
    }

    fun decidePackage(packageName: String): TrackingDecision {
        val snapshot = appState
        return AppTrackingPolicy.decide(
            packageName = packageName,
            userEnabledPackages = snapshot.enabled,
            userBlockedPackages = snapshot.blocked,
            userDisabledPackages = snapshot.disabled,
            trackUnrecognizedApps = trackingRules.trackUnrecognizedApps,
        )
    }

    /**
     * Applies every user-controlled media policy that can be decided from the supplied evidence.
     *
     * [LocalMediaMetadata] is optional because notification-only paths often know only title,
     * artist and package at first. Package-level and manual rules still apply in that state.
     */
    fun evaluateMedia(
        packageName: String,
        title: String,
        artist: String,
        durationMs: Long = 0L,
        metadata: LocalMediaMetadata? = null,
    ): MediaVerdict {
        val manualMark = ManualContentRuleResolver.resolve(manualContentMarks, title, artist)
        when (manualMark?.contentType?.uppercase()) {
            "ALWAYS_MUSIC" -> return MediaVerdict.FORCE_ALLOW
            "NON_MUSIC", "VIDEO" -> return MediaVerdict.REJECT
        }

        if (durationMs > 0L) {
            val maxDuration = trackingRules.getMaxMusicDurationMs(packageName)
            if (maxDuration != null && durationMs > maxDuration) {
                return MediaVerdict.REJECT
            }
        }

        val preferences = userState
        if (preferences.filterPodcasts) {
            if (packageName in DefaultMusicApps.PODCAST_PACKAGES) return MediaVerdict.REJECT
            if (metadata?.isPodcast() == true) return MediaVerdict.REJECT
            if (manualMark?.contentType.equals("PODCAST", ignoreCase = true)) {
                return MediaVerdict.REJECT
            }
        }

        if (preferences.filterAudiobooks) {
            if (packageName in DefaultMusicApps.AUDIOBOOK_PACKAGES) return MediaVerdict.REJECT
            if (metadata?.isAudiobook() == true) return MediaVerdict.REJECT
            if (manualMark?.contentType.equals("AUDIOBOOK", ignoreCase = true)) {
                return MediaVerdict.REJECT
            }
        }

        return MediaVerdict.DEFER
    }
}
