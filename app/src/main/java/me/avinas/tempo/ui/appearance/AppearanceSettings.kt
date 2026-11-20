package me.avinas.tempo.ui.appearance

import me.avinas.tempo.ui.components.ShareTheme

/** Stable persisted identifiers. Unknown values fall back without touching listening data. */
enum class AppearancePreset {
    ABYSS, OBSIDIAN_GOLD, AURORA, GLACIER, EMBER, SOLSTICE, VERDANT, SLATE;
    companion object {
        fun decode(raw: String?): AppearancePreset = entries.firstOrNull { it.name == raw } ?: ABYSS
    }
}
enum class AppearanceTone { DARK, LIGHT, SYSTEM, OLED }
enum class AppearanceFontStyle {
    TEMPO, NEO_SANS, ARCHIVE_SERIF, STUDIO_MONO;
    companion object {
        fun decode(raw: String?): AppearanceFontStyle = entries.firstOrNull { it.name == raw } ?: TEMPO
    }
}
enum class AppearanceAccentSource { PRESET, WALLPAPER, ARTWORK, CUSTOM }
enum class AppearanceDensity { COMFORTABLE, COMPACT }
enum class AppearanceShape { SOFT, MODERATE, SHARP }
enum class AppearanceSurface { SATIN, SOLID }
enum class AppearanceBackdrop { GRADIENT, FLAT, WASH }
enum class AppearanceNowListening { INLINE, COMPACT, HIDDEN }
enum class AppearanceHomeSection { SUMMARY, SPOTLIGHT, QUICK_STATS, TODAY, INSIGHTS }
enum class AppearanceStatsTab { SONGS, ARTISTS, ALBUMS }
enum class AppearanceStatsRange { WEEK, MONTH, YEAR, ALL_TIME, TODAY }
enum class AppearanceStatsSort { COMBINED, PLAYS, TIME }
enum class AppearanceStatsDuration { AUTO, MINUTES, HOURS, DAYS, CLOCK, CLOCK_SECONDS }
enum class AppearanceDurationPrecision(val digits: Int) { WHOLE(0), ONE(1), TWO(2), THREE(3) }
enum class AppearanceStatsSummary { TRACKS, ARTISTS, ALBUMS }
enum class AppearanceProfileTab { CHALLENGES, BADGES }

data class AppearanceSettings(
    val preset: AppearancePreset = AppearancePreset.ABYSS,
    val tone: AppearanceTone = AppearanceTone.DARK,
    val fontStyle: AppearanceFontStyle = AppearanceFontStyle.TEMPO,
    val reduceMotion: Boolean = false,
    val instantNavigation: Boolean = false,
    val showNavigationLabels: Boolean = true,
    val showHistoryArtwork: Boolean = true,
    val accentSource: AppearanceAccentSource = AppearanceAccentSource.PRESET,
    val density: AppearanceDensity = AppearanceDensity.COMFORTABLE,
    val shape: AppearanceShape = AppearanceShape.SOFT,
    val boldText: Boolean = false,
    val surface: AppearanceSurface = AppearanceSurface.SATIN,
    val adaptiveEffects: Boolean = true,
    val nowListening: AppearanceNowListening = AppearanceNowListening.INLINE,
    val hiddenHomeSections: Set<AppearanceHomeSection> = emptySet(),
    val homeOrder: List<AppearanceHomeSection> = AppearanceHomeSection.entries.toList(),
    val showHomeSummaryChart: Boolean = true,
    val homeStartPeriod: AppearanceStatsRange = AppearanceStatsRange.WEEK,
    val statsTab: AppearanceStatsTab = AppearanceStatsTab.SONGS,
    val statsRange: AppearanceStatsRange = AppearanceStatsRange.WEEK,
    val customAccent: String? = null,
    val backdrop: AppearanceBackdrop = AppearanceBackdrop.GRADIENT,
    val statsSort: AppearanceStatsSort = AppearanceStatsSort.COMBINED,
    val showStatsSummary: Boolean = true,
    val showStatsArtwork: Boolean = true,
    val highlightTopResult: Boolean = true,
    val profileTab: AppearanceProfileTab = AppearanceProfileTab.CHALLENGES,
    val showProfileNowPlaying: Boolean = true,
    val showProfileProgress: Boolean = true,
    val showProfileStreakWarning: Boolean = true,
    val defaultShareTheme: ShareTheme = ShareTheme.MIDNIGHT,
    val statsDuration: AppearanceStatsDuration = AppearanceStatsDuration.AUTO,
    val statsSummary: AppearanceStatsSummary = AppearanceStatsSummary.TRACKS,
    // Append new persisted options so older positional constructors remain source-compatible.
    val statsDurationPrecision: AppearanceDurationPrecision = AppearanceDurationPrecision.TWO,
) {
    /** Retain opt-in beta tones when applying or importing an appearance. */
    fun forLiveApp(): AppearanceSettings = this
}
internal fun decodeAppearanceTone(raw: String?): AppearanceTone = enumOrDefault(raw, AppearanceTone.DARK)
internal inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == raw } ?: fallback

/** Canonical six-digit RGB, with opaque alpha applied by the palette resolver. */
internal fun normalizeAccentHex(raw: String?): String? {
    val rgb = raw?.trim()?.removePrefix("#") ?: return null
    return rgb.takeIf { it.length == 6 && it.all { char -> char in '0'..'9' || char in 'a'..'f' || char in 'A'..'F' } }
        ?.uppercase()
}
