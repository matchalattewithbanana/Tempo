package me.avinas.tempo.ui.appearance

/** Stable persisted identifiers. Unknown values fall back without touching listening data. */
enum class AppearancePreset {
    ABYSS, OBSIDIAN_GOLD, AURORA, GLACIER, EMBER, SOLSTICE;
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
enum class AppearanceAccentSource { PRESET, WALLPAPER, ARTWORK }
enum class AppearanceDensity { COMFORTABLE, COMPACT }
enum class AppearanceShape { SOFT, MODERATE, SHARP }
enum class AppearanceSurface { SATIN, SOLID }
enum class AppearanceNowListening { INLINE, COMPACT, HIDDEN }
enum class AppearanceHomeSection { SUMMARY, SPOTLIGHT, QUICK_STATS, TODAY, INSIGHTS }
enum class AppearanceStatsTab { SONGS, ARTISTS, ALBUMS }
enum class AppearanceStatsRange { WEEK, MONTH, YEAR, ALL_TIME }

data class AppearanceSettings(
    val preset: AppearancePreset = AppearancePreset.ABYSS,
    val tone: AppearanceTone = AppearanceTone.DARK,
    val fontStyle: AppearanceFontStyle = AppearanceFontStyle.TEMPO,
    val reduceMotion: Boolean = false,
    val accentSource: AppearanceAccentSource = AppearanceAccentSource.PRESET,
    val density: AppearanceDensity = AppearanceDensity.COMFORTABLE,
    val shape: AppearanceShape = AppearanceShape.SOFT,
    val boldText: Boolean = false,
    val surface: AppearanceSurface = AppearanceSurface.SATIN,
    val adaptiveEffects: Boolean = true,
    val nowListening: AppearanceNowListening = AppearanceNowListening.INLINE,
    val hiddenHomeSections: Set<AppearanceHomeSection> = emptySet(),
    val homeOrder: List<AppearanceHomeSection> = AppearanceHomeSection.entries.toList(),
    val statsTab: AppearanceStatsTab = AppearanceStatsTab.SONGS,
    val statsRange: AppearanceStatsRange = AppearanceStatsRange.WEEK,
) {
    /** Retain opt-in beta tones when applying or importing an appearance. */
    fun forLiveApp(): AppearanceSettings = this
}
internal fun decodeAppearanceTone(raw: String?): AppearanceTone = enumOrDefault(raw, AppearanceTone.DARK)
internal inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == raw } ?: fallback
