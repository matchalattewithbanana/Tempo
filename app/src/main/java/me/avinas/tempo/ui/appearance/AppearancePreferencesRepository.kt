package me.avinas.tempo.ui.appearance

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.delay
import java.io.IOException
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import me.avinas.tempo.widget.utils.scheduleImmediateWidgetUpdate

private val Context.appearanceDataStore by preferencesDataStore(
    name = "tempo_appearance",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/**
 * The appearance store is intentionally separate from listening data and playback settings.
 * Appearance changes cannot migrate Room, reset permissions, or touch tracking services.
 */
@Singleton
class AppearancePreferencesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private companion object {
        val VERSION = intPreferencesKey("appearance_schema_version")
        const val CURRENT_VERSION = AppearanceCodec.VERSION
        val CONFIG = stringPreferencesKey("appearance_config")
        val LOOKS = stringPreferencesKey("appearance_saved_looks")
        val PRESET = stringPreferencesKey("appearance_preset_id")
        val TONE = stringPreferencesKey("appearance_tone")
        val FONT = stringPreferencesKey("appearance_font_style")
        val REDUCED_MOTION = booleanPreferencesKey("appearance_reduce_motion")
    }

    val settings: Flow<AppearanceSettings> = context.appearanceDataStore.data.retryWhen { cause, _ ->
        if (cause is IOException) {
            // Retain the last real value. Emitting defaults here would reset an open draft.
            delay(1_000)
            true
        } else false
    }.map(::decodeSettings)

    private fun decodeSettings(prefs: androidx.datastore.preferences.core.Preferences): AppearanceSettings {
        val encoded = prefs[CONFIG]
        val decoded = encoded?.let { runCatching { AppearanceCodec.decode(it) }.getOrNull() }
        return decoded ?: AppearanceSettings(
            preset = AppearancePreset.decode(prefs[PRESET]),
            tone = decodeAppearanceTone(prefs[TONE]),
            fontStyle = AppearanceFontStyle.decode(prefs[FONT]),
            reduceMotion = prefs[REDUCED_MOTION] ?: false,
        )
    }

    /** Apply one complete draft in a single transaction. Legacy unversioned data is v0. */
    suspend fun apply(settings: AppearanceSettings) {
        context.appearanceDataStore.edit { prefs ->
            check((prefs[VERSION] ?: 0) <= CURRENT_VERSION) { "Unsupported appearance schema" }
            prefs.writeSettings(settings)
        }
        scheduleImmediateWidgetUpdate(context)
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.writeSettings(settings: AppearanceSettings) {
        val live = settings.forLiveApp()
        this[VERSION] = CURRENT_VERSION
        this[PRESET] = live.preset.name
        this[TONE] = live.tone.name
        this[CONFIG] = AppearanceCodec.encode(live)
        this[FONT] = live.fontStyle.name
        this[REDUCED_MOTION] = live.reduceMotion
    }

    val savedLooks: Flow<Map<String, AppearanceSettings>> = context.appearanceDataStore.data.retryWhen { cause, _ ->
        if (cause is IOException) { delay(1_000); true } else false
    }.map { prefs -> decodeLooks(prefs[LOOKS]) }

    private fun decodeLooks(raw: String?): Map<String, AppearanceSettings> {
        val json = runCatching { JSONObject(raw ?: "{}") }.getOrElse { JSONObject() }
        return json.keys().asSequence().take(12).mapNotNull { name ->
            runCatching { name to AppearanceCodec.decode(json.getString(name)) }.getOrNull()
        }.toMap()
    }

    /** Read the applied settings and named looks from the same DataStore snapshot. */
    internal suspend fun exportBackup(): String {
        val prefs = context.appearanceDataStore.data.first()
        check((prefs[VERSION] ?: 0) <= CURRENT_VERSION) { "Unsupported appearance schema" }
        return JSONObject().apply {
            put("settings", AppearanceCodec.encode(decodeSettings(prefs)))
            put("looks", encodeLooks(decodeLooks(prefs[LOOKS])))
        }.toString()
    }

    /** Replace the appearance snapshot or keep configured settings and fill unused look slots. */
    internal suspend fun restoreBackup(raw: String, replace: Boolean): Boolean {
        val (settings, importedLooks) = runCatching {
            require(raw.length <= 512 * 1024)
            val json = JSONObject(raw)
            val looks = json.getJSONObject("looks")
            require(looks.length() <= 12)
            AppearanceCodec.decode(json.getString("settings")) to looks.keys().asSequence().associateWith { name ->
                require(name.isNotBlank() && name.length <= 40 && name == name.trim())
                AppearanceCodec.decode(looks.getString(name))
            }
        }.getOrNull() ?: return false

        var applied = false
        context.appearanceDataStore.edit { prefs ->
            if ((prefs[VERSION] ?: 0) > CURRENT_VERSION) return@edit
            val hasSettings = prefs[CONFIG] != null || prefs[PRESET] != null || prefs[TONE] != null ||
                prefs[FONT] != null || prefs[REDUCED_MOTION] != null
            if (replace || !hasSettings) {
                prefs.writeSettings(settings)
                applied = true
            }
            val existing = decodeLooks(prefs[LOOKS])
            val updated = if (replace) importedLooks else (importedLooks + existing)
                .let { combined -> (existing.keys + importedLooks.keys).take(12).associateWith(combined::getValue) }
            if (updated != existing) {
                prefs[VERSION] = CURRENT_VERSION
                prefs[LOOKS] = encodeLooks(updated).toString()
                applied = true
            }
        }
        if (applied) scheduleImmediateWidgetUpdate(context)
        return applied
    }

    private fun encodeLooks(looks: Map<String, AppearanceSettings>): JSONObject = JSONObject().apply {
        looks.forEach { (name, settings) -> put(name, AppearanceCodec.encode(settings.forLiveApp())) }
    }

    suspend fun saveLook(name: String, settings: AppearanceSettings) {
        val key = name.trim().take(40)
        require(key.isNotEmpty())
        context.appearanceDataStore.edit { prefs ->
            check((prefs[VERSION] ?: 0) <= CURRENT_VERSION)
            val json = runCatching { org.json.JSONObject(prefs[LOOKS] ?: "{}") }.getOrElse { org.json.JSONObject() }
            require(json.has(key) || json.length() < 12)
            json.put(key, AppearanceCodec.encode(settings.forLiveApp()))
            prefs[VERSION] = CURRENT_VERSION
            prefs[LOOKS] = json.toString()
        }
    }

    suspend fun deleteLook(name: String) {
        context.appearanceDataStore.edit { prefs ->
            check((prefs[VERSION] ?: 0) <= CURRENT_VERSION)
            val json = runCatching { org.json.JSONObject(prefs[LOOKS] ?: "{}") }.getOrElse { org.json.JSONObject() }
            json.remove(name)
            prefs[VERSION] = CURRENT_VERSION
            prefs[LOOKS] = json.toString()
        }
    }

}
