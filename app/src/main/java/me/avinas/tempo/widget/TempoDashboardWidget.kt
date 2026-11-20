package me.avinas.tempo.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import me.avinas.tempo.R
import me.avinas.tempo.widget.theme.TempoWidgetTheme
import me.avinas.tempo.widget.utils.WidgetHub
import me.avinas.tempo.widget.utils.WidgetNavigation
import me.avinas.tempo.widget.utils.appWidgetBackground
import me.avinas.tempo.widget.utils.cancelPeriodicWidgetUpdateIfUnused
import me.avinas.tempo.widget.utils.scheduleImmediateWidgetUpdate
import me.avinas.tempo.widget.utils.schedulePeriodicWidgetUpdate

class TempoDashboardWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val millis = prefs[stringPreferencesKey(WidgetHub.PREF_TODAY_TIME)]?.toLongOrNull()
            val plays = prefs[stringPreferencesKey(WidgetHub.PREF_TODAY_PLAYS)]?.toIntOrNull()
            val songs = prefs[stringPreferencesKey(WidgetHub.PREF_TODAY_TRACKS)]?.toIntOrNull()

            ListeningWidgetCard(context, context.getString(R.string.widget_today_listening), today = true) {
                WidgetMetric(
                    prefs[stringPreferencesKey(WidgetHub.PREF_TODAY_TIME_LABEL)]
                ?: millis?.let { widgetListeningTime(context, it) } ?: "—",
                    context.getString(R.string.stats_period_listening_time),
                    GlanceModifier.defaultWeight(),
                    highlighted = true,
                )
                WidgetMetric(
                    plays?.let { context.getString(R.string.stats_period_number, it) } ?: "—",
                    context.getString(R.string.stats_period_plays),
                    GlanceModifier.defaultWeight(),
                )
                if (LocalSize.current.width >= 280.dp) {
                    WidgetMetric(
                        songs?.let { context.getString(R.string.stats_period_number, it) } ?: "—",
                        context.getString(R.string.stats_period_unique_songs),
                        GlanceModifier.defaultWeight(),
                    )
                }
            }
        }
    }
}

@Composable
internal fun ListeningWidgetCard(
    context: Context,
    title: String,
    today: Boolean = false,
    metrics: @Composable androidx.glance.layout.RowScope.() -> Unit,
) {
    TempoWidgetTheme {
        val compact = LocalSize.current.height < 105.dp
        Column(
            modifier = GlanceModifier.appWidgetBackground()
                .background(GlanceTheme.colors.surface)
                .clickable(WidgetNavigation.openStats(context, today))
                .padding(if (compact) 10.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
            Spacer(GlanceModifier.height(if (compact) 4.dp else 8.dp))
            Row(modifier = GlanceModifier.fillMaxWidth(), content = metrics)
        }
    }
}

@Composable
internal fun WidgetMetric(value: String, label: String, modifier: GlanceModifier, highlighted: Boolean = false) {
    Column(modifier = modifier.padding(end = 6.dp)) {
        Text(
            value,
            style = TextStyle(
                color = if (highlighted) GlanceTheme.colors.primary else GlanceTheme.colors.onSurface,
                fontSize = if (LocalSize.current.height < 105.dp) 18.sp else 22.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
        Text(label, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp), maxLines = 2)
    }
}

class TempoDashboardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TempoDashboardWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        scheduleImmediateWidgetUpdate(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        schedulePeriodicWidgetUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Shared periodic work: this only stops it once the last Tempo widget is gone,
        // so removing one widget does not silently kill refresh for the others.
        cancelPeriodicWidgetUpdateIfUnused(context)
    }
}
