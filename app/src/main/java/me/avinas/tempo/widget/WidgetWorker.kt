package me.avinas.tempo.widget

import android.content.Context
import android.graphics.Bitmap
import android.util.AtomicFile
import android.util.Log
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.avinas.tempo.R
import me.avinas.tempo.data.repository.SortBy
import me.avinas.tempo.data.repository.StatsRepository
import me.avinas.tempo.data.stats.TimeRange
import me.avinas.tempo.ui.appearance.AppearancePreferencesRepository
import me.avinas.tempo.ui.stats.formatStatsDuration
import kotlinx.coroutines.flow.first
import me.avinas.tempo.widget.utils.WidgetHub
import me.avinas.tempo.widget.utils.WidgetType
import me.avinas.tempo.widget.utils.placedWidgets
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek

@HiltWorker
class WidgetWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val statsRepository: StatsRepository,
    private val imageLoader: ImageLoader,
    private val appearancePreferences: AppearancePreferencesRepository,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = try {
        refreshLock.withLock { refreshPlacedWidgets() }
        Result.success()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Log.w(TAG, "Could not refresh listening widgets", failure)
        if (runAttemptCount < 2) Result.retry() else Result.failure()
    }

    private suspend fun refreshPlacedWidgets() {
        val placed = placedWidgets(context)
        if (placed.isEmpty()) return

        // One appearance snapshot for the entire batch; do not read DataStore per widget.
        val appearance = appearancePreferences.settings.first()
        fun durationLabel(millis: Long): String = formatStatsDuration(
            context, millis, appearance.statsDuration, appearance.statsDurationPrecision
        )
        val states = placed.keys.associateWith { mutableMapOf<String, String?>() }

        val weekly = states[WidgetType.WEEKLY]
        val mix = states[WidgetType.MIX]
        if (weekly != null || mix != null) {
            val daily = statsRepository.getDailyListening(TimeRange.THIS_WEEK, limit = 8, withLeeway = false)
                .associate { it.date to it.totalTimeMs }
            val monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val chart = (0L..6L).joinToString(",") { day ->
                (daily[monday.plusDays(day).toString()] ?: 0L).toString()
            }
            weekly?.set(WidgetHub.PREF_WEEKLY_CHART_DATA, chart)
            mix?.set(WidgetHub.PREF_MIX_CHART_DATA, chart)
        }
        if (weekly != null) {
            val totals = statsRepository.getListeningTotals(TimeRange.THIS_WEEK, withLeeway = false)
            weekly[WidgetHub.PREF_WEEKLY_TIME] = totals.totalTimeMs.toString()
            weekly[WidgetHub.PREF_WEEKLY_TIME_LABEL] = durationLabel(totals.totalTimeMs)
            weekly[WidgetHub.PREF_WEEKLY_PLAYS] = totals.playCount.toString()
        }

        val artist = states[WidgetType.ARTIST]
        if (weekly != null || artist != null || mix != null) {
            val lead = statsRepository.getTopArtists(TimeRange.THIS_WEEK, pageSize = 1, withLeeway = false)
                .items.firstOrNull()
            val fallback = context.getString(R.string.widget_no_listening)
            val name = lead?.artist ?: fallback
            val artwork = lead?.imageUrl?.let { cacheArtwork(it, "widget_artist_bg.jpg") }
            weekly?.set(WidgetHub.PREF_TOP_ARTIST_NAME, name)
            weekly?.set(WidgetHub.PREF_TOP_ARTIST_IMAGE, artwork)
            artist?.set(WidgetHub.PREF_TOP_ARTIST_NAME, name)
            artist?.set(WidgetHub.PREF_TOP_ARTIST_IMAGE, artwork)
            artist?.set(WidgetHub.PREF_TOP_ARTIST_HOURS, durationLabel(lead?.totalTimeMs ?: 0L))
            mix?.set(WidgetHub.PREF_TOP_ARTIST_IMAGE, artwork)
        }

        states[WidgetType.DASHBOARD]?.let { state ->
            val today = statsRepository.getListeningTotals(TimeRange.TODAY, withLeeway = false)
            state[WidgetHub.PREF_TODAY_TIME] = today.totalTimeMs.toString()
            state[WidgetHub.PREF_TODAY_TIME_LABEL] = durationLabel(today.totalTimeMs)
            state[WidgetHub.PREF_TODAY_PLAYS] = today.playCount.toString()
            state[WidgetHub.PREF_TODAY_TRACKS] = today.uniqueTracks.toString()
        }

        states[WidgetType.DISCOVERY]?.let { state ->
            val discovery = statsRepository.getDiscoveryStats(TimeRange.THIS_WEEK, withLeeway = false)
            state[WidgetHub.PREF_DISCOVERY_NEW_TRACKS] = discovery.newTracksCount.toString()
            state[WidgetHub.PREF_DISCOVERY_NEW_ARTISTS] = discovery.newArtistsCount.toString()
        }

        states[WidgetType.HEATMAP]?.let { state ->
            val genres = statsRepository.getTopGenres(TimeRange.THIS_WEEK, limit = 5, withLeeway = false)
            val total = genres.sumOf { it.playCount.toLong() }.coerceAtLeast(1L)
            val json = JSONArray()
            for (genre in genres) {
                json.put(JSONObject()
                    .put("genre", genre.genre)
                    .put("count", genre.playCount)
                    .put("percent", genre.playCount * 100L / total))
            }
            state[WidgetHub.PREF_TOP_GENRES_JSON] = json.toString()
        }

        states[WidgetType.MILESTONE]?.let { state ->
            val track = statsRepository.getTopTracks(
                TimeRange.THIS_WEEK, sortBy = SortBy.PLAY_COUNT, pageSize = 1, withLeeway = false,
            ).items.firstOrNull()
            state[WidgetHub.PREF_MILESTONE_TRACK_NAME] = track?.title
            state[WidgetHub.PREF_MILESTONE_ARTIST_NAME] = track?.artist
            state[WidgetHub.PREF_MILESTONE_PLAY_COUNT] = track?.playCount?.toString()
            state[WidgetHub.PREF_MILESTONE_IMAGE] = track?.albumArtUrl?.let { cacheArtwork(it, "widget_milestone_art.jpg") }
        }

        val manager = GlanceAppWidgetManager(context)
        for ((type, ids) in placed) {
            val widget = type.createWidget()
            val values = states.getValue(type)
            for (appWidgetId in ids) {
                val id = try {
                    manager.getGlanceIdBy(appWidgetId)
                } catch (_: IllegalArgumentException) {
                    continue // The launcher removed the widget during the refresh.
                }
                updateAppWidgetState(context, id) { preferences ->
                    for ((name, value) in values) {
                        val key = stringPreferencesKey(name)
                        if (value == null) preferences.remove(key) else preferences[key] = value
                    }
                }
                widget.update(context, id)
            }
        }
    }

    private suspend fun cacheArtwork(url: String, fileName: String): String? = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, fileName)
        val cache = context.getSharedPreferences("widget_image_cache", Context.MODE_PRIVATE)
        if (file.exists() && cache.getString(fileName, null) == url) return@withContext file.absolutePath

        try {
            val request = ImageRequest.Builder(context).data(url).size(512, 512).allowHardware(false).build()
            val bitmap = (imageLoader.execute(request) as? SuccessResult)?.image?.toBitmap()
                ?: return@withContext null
            val atomic = AtomicFile(file)
            val output = atomic.startWrite()
            try {
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)) {
                    throw IOException("Could not encode widget artwork")
                }
                atomic.finishWrite(output)
            } catch (failure: Exception) {
                atomic.failWrite(output)
                throw failure
            }
            cache.edit().putString(fileName, url).apply()
            file.absolutePath
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Log.w(TAG, "Could not cache widget artwork", failure)
            null
        }
    }

    private companion object {
        const val TAG = "WidgetWorker"
        val refreshLock = Mutex()
    }
}

internal fun widgetListeningTime(context: Context, millis: Long): String {
    if (millis == 0L) return context.getString(R.string.stats_period_zero_time)
    val minutes = millis / 60_000L
    if (minutes == 0L) return context.getString(R.string.widget_duration_under_minute)
    val hours = minutes / 60
    return if (hours > 0) context.getString(R.string.widget_duration_hours_minutes, hours, minutes % 60)
    else context.getString(R.string.widget_duration_minutes, minutes)
}
