package me.avinas.tempo.widget.utils

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.action.Action
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import me.avinas.tempo.MainActivity
import me.avinas.tempo.TempoApplication
import me.avinas.tempo.data.stats.TimeRange
import me.avinas.tempo.widget.*
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.cornerRadius
import androidx.glance.layout.fillMaxSize
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import me.avinas.tempo.utils.MAX_SAFE_BITMAP_EDGE_PX
import java.util.concurrent.TimeUnit

object WidgetHub {
    val CornerRadius = 24.dp

    const val PREF_WEEKLY_TIME = "weekly_time_ms"
    const val PREF_WEEKLY_TIME_LABEL = "weekly_time_display"
    const val PREF_WEEKLY_PLAYS = "weekly_plays"
    const val PREF_WEEKLY_CHART_DATA = "weekly_chart_data"
    const val PREF_TODAY_TIME = "today_time_ms"
    const val PREF_TODAY_TIME_LABEL = "today_time_display"
    const val PREF_TODAY_PLAYS = "today_plays"
    const val PREF_TODAY_TRACKS = "today_tracks"
    const val PREF_TOP_ARTIST_NAME = "top_artist_name"
    const val PREF_TOP_ARTIST_IMAGE = "top_artist_image"
    const val PREF_TOP_ARTIST_HOURS = "top_artist_hours"
    const val PREF_TOP_GENRES_JSON = "top_genres_json"
    const val PREF_MILESTONE_IMAGE = "milestone_image"
    const val PREF_MILESTONE_TRACK_NAME = "milestone_track_name"
    const val PREF_MILESTONE_ARTIST_NAME = "milestone_artist_name"
    const val PREF_MILESTONE_PLAY_COUNT = "milestone_play_count"
    const val PREF_DISCOVERY_NEW_TRACKS = "discovery_new_tracks"
    const val PREF_DISCOVERY_NEW_ARTISTS = "discovery_new_artists"
    const val PREF_MIX_CHART_DATA = "mix_chart_data"
}

fun GlanceModifier.appWidgetBackground(): GlanceModifier {
    return this.fillMaxSize()
        .cornerRadius(WidgetHub.CornerRadius)
}

private const val PERIODIC_WORK_NAME = "TempoWidgetPeriodicWork"

// Launcher broadcasts can arrive together for multiple widget providers.
private const val IMMEDIATE_WORK_NAME = "TempoWidgetImmediateWork"
private const val HISTORY_WORK_NAME = "TempoWidgetHistoryWork"

internal enum class WidgetType(
    val receiver: Class<out GlanceAppWidgetReceiver>,
    val createWidget: () -> GlanceAppWidget,
) {
    WEEKLY(TempoWidgetReceiver::class.java, ::TempoAppWidget),
    ARTIST(TempoArtistWidgetReceiver::class.java, ::TempoArtistWidget),
    DASHBOARD(TempoDashboardWidgetReceiver::class.java, ::TempoDashboardWidget),
    DISCOVERY(TempoDiscoveryWidgetReceiver::class.java, ::TempoDiscoveryWidget),
    HEATMAP(TempoHeatmapWidgetReceiver::class.java, ::TempoHeatmapWidget),
    MILESTONE(TempoMilestoneWidgetReceiver::class.java, ::TempoMilestoneWidget),
    MIX(TempoMixWidgetReceiver::class.java, ::TempoMixWidget),
}

internal fun placedWidgets(context: Context): Map<WidgetType, IntArray> {
    val manager = AppWidgetManager.getInstance(context) ?: return emptyMap()
    return WidgetType.entries.associateWith { manager.getAppWidgetIds(ComponentName(context, it.receiver)) }
        .filterValues { it.isNotEmpty() }
}

internal object WidgetNavigation {
    private const val TODAY_STATS = "widget_today_stats"
    private const val WEEKLY_STATS = "widget_weekly_stats"

    fun periodFor(destination: String?): TimeRange? = when (destination) {
        TODAY_STATS -> TimeRange.TODAY
        WEEKLY_STATS -> TimeRange.THIS_WEEK
        else -> null
    }

    @OptIn(ExperimentalGlanceApi::class)
    fun openStats(context: Context, today: Boolean = false): Action {
        val destination = if (today) TODAY_STATS else WEEKLY_STATS
        val intent = Intent(context, MainActivity::class.java)
            .setAction("${context.packageName}.$destination")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra("navigate_to", destination)
        return actionStartActivity(intent)
    }
}

internal fun scheduleWidgetUpdateAfterHistoryChange(context: Context) {
    val work = OneTimeWorkRequest.Builder(WidgetWorker::class.java)
        .setInitialDelay(30, TimeUnit.SECONDS)
        .build()
    WorkManager.getInstance(context).enqueueUniqueWork(HISTORY_WORK_NAME, ExistingWorkPolicy.KEEP, work)
}

fun scheduleImmediateWidgetUpdate(context: Context) {
    val workRequest = OneTimeWorkRequest.Builder(WidgetWorker::class.java).build()
    WorkManager.getInstance(context).enqueueUniqueWork(
        IMMEDIATE_WORK_NAME,
        // KEEP: a queued-but-not-yet-started refresh already covers this request, because the
        // worker rebuilds every widget from current data. REPLACE would restart the work and
        // delay it each time another widget's onUpdate arrived.
        ExistingWorkPolicy.KEEP,
        workRequest,
    )
}

fun schedulePeriodicWidgetUpdate(context: Context) {
    (context.applicationContext as? TempoApplication)?.syncWidgetRefreshObserver()
    val constraints = Constraints.Builder()
        .setRequiresBatteryNotLow(true)
        .build()

    val periodicWork = PeriodicWorkRequest.Builder(
        WidgetWorker::class.java,
        3, TimeUnit.HOURS
    )
        .setConstraints(constraints)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        PERIODIC_WORK_NAME,
        ExistingPeriodicWorkPolicy.KEEP,
        periodicWork
    )
}

/**
 * Cancels the shared periodic refresh, but only once no Tempo widget is left.
 *
 * The periodic work has a single name shared by all seven providers, so cancelling it on any one
 * widget's removal would silently stop the 3-hourly refresh for widgets the user still has
 * placed. Only the last widget removed stops the work. Every receiver's `onDisabled` should call
 * this rather than `cancelUniqueWork` directly.
 */
fun cancelPeriodicWidgetUpdateIfUnused(context: Context) {
    (context.applicationContext as? TempoApplication)?.syncWidgetRefreshObserver()
    if (placedWidgets(context).isNotEmpty()) return
    WorkManager.getInstance(context).apply {
        cancelUniqueWork(PERIODIC_WORK_NAME)
        cancelUniqueWork(HISTORY_WORK_NAME)
    }
}

    /**
     * Calculates the optimal pixel size for generating a widget bitmap.
     * 
     * We simply convert the DP size to Pixels using the device density.
     * Previously invalidly scaled by 2.5x which caused RemoteViews memory crashes (20MB+ bitmaps).
     * Now returns exact screen-density pixels.
     */
    fun getPixelSize(context: Context, size: androidx.compose.ui.unit.DpSize): android.util.Size {
        val density = context.resources.displayMetrics.density
        // Just convert DP to PX. No extra usage of "scaleFactor" needed as density already covers it.
        // Clamped to the app-wide bitmap ceiling so a host reporting an absurd widget size cannot
        // drive BitmapGenerator's createBitmap(width, height) allocations past it.
        val widthPx = (size.width.value * density).toInt().coerceIn(100, MAX_SAFE_BITMAP_EDGE_PX)
        val heightPx = (size.height.value * density).toInt().coerceIn(100, MAX_SAFE_BITMAP_EDGE_PX)
        return android.util.Size(widthPx, heightPx)
    }
