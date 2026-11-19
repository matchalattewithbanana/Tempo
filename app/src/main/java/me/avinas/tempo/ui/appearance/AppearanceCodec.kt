package me.avinas.tempo.ui.appearance

import me.avinas.tempo.ui.components.ShareTheme
import org.json.JSONArray
import org.json.JSONObject

/** Portable, size-bounded appearance-only format. No paths, account data or history. */
object AppearanceCodec {
    const val VERSION = 2
    const val MAX_BYTES = 32 * 1024
    fun encode(value: AppearanceSettings): String = JSONObject().apply {
        put("version", VERSION)
        put("preset", value.preset.name); put("tone", value.tone.name)
        put("font", value.fontStyle.name); put("reduceMotion", value.reduceMotion)
        put("accent", value.accentSource.name); put("density", value.density.name)
        put("customAccent", normalizeAccentHex(value.customAccent))
        put("backdrop", value.backdrop.name)
        put("shape", value.shape.name); put("bold", value.boldText)
        put("surface", value.surface.name); put("adaptive", value.adaptiveEffects)
        put("nowListening", value.nowListening.name)
        put("hiddenHome", JSONArray(value.hiddenHomeSections.map { it.name }))
        put("homeOrder", JSONArray(value.homeOrder.map { it.name }))
        put("statsTab", value.statsTab.name); put("statsRange", value.statsRange.name)
        put("statsSort", value.statsSort.name)
        put("showStatsSummary", value.showStatsSummary)
        put("highlightTopResult", value.highlightTopResult)
        put("profileTab", value.profileTab.name)
        put("showProfileNowPlaying", value.showProfileNowPlaying)
        put("showProfileProgress", value.showProfileProgress)
        put("defaultShareTheme", value.defaultShareTheme.name)
    }.toString()

    fun decode(raw: String): AppearanceSettings {
        require(raw.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Appearance file too large" }
        val json = JSONObject(raw)
        require(json.getInt("version") in 1..VERSION) { "Unsupported appearance version" }
        fun sections(key: String): List<AppearanceHomeSection> {
            val array = json.optJSONArray(key) ?: return emptyList()
            return (0 until array.length()).mapNotNull { i ->
                AppearanceHomeSection.entries.firstOrNull { it.name == array.optString(i) }
            }.distinct()
        }
        val order = sections("homeOrder")
        return AppearanceSettings(
            preset = AppearancePreset.decode(json.optString("preset")),
            tone = decodeAppearanceTone(json.optString("tone")),
            fontStyle = AppearanceFontStyle.decode(json.optString("font")),
            reduceMotion = json.optBoolean("reduceMotion"),
            accentSource = enumOrDefault(json.optString("accent"), AppearanceAccentSource.PRESET),
            density = enumOrDefault(json.optString("density"), AppearanceDensity.COMFORTABLE),
            shape = enumOrDefault(json.optString("shape"), AppearanceShape.SOFT),
            boldText = json.optBoolean("bold"),
            surface = enumOrDefault(json.optString("surface"), AppearanceSurface.SATIN),
            adaptiveEffects = json.optBoolean("adaptive", true),
            nowListening = enumOrDefault(json.optString("nowListening"), AppearanceNowListening.INLINE),
            hiddenHomeSections = sections("hiddenHome").toSet(),
            homeOrder = order + AppearanceHomeSection.entries.filter { it !in order },
            statsTab = enumOrDefault(json.optString("statsTab"), AppearanceStatsTab.SONGS),
            statsRange = enumOrDefault(json.optString("statsRange"), AppearanceStatsRange.WEEK),
            customAccent = normalizeAccentHex(json.optString("customAccent")),
            backdrop = enumOrDefault(json.optString("backdrop"), AppearanceBackdrop.GRADIENT),
            statsSort = enumOrDefault(json.optString("statsSort"), AppearanceStatsSort.COMBINED),
            showStatsSummary = json.optBoolean("showStatsSummary", true),
            highlightTopResult = json.optBoolean("highlightTopResult", true),
            profileTab = enumOrDefault(json.optString("profileTab"), AppearanceProfileTab.CHALLENGES),
            showProfileNowPlaying = json.optBoolean("showProfileNowPlaying", true),
            showProfileProgress = json.optBoolean("showProfileProgress", true),
            defaultShareTheme = enumOrDefault(json.optString("defaultShareTheme"), ShareTheme.MIDNIGHT),
        )
    }
}
