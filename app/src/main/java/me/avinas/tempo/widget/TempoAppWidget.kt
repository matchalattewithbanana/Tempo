package me.avinas.tempo.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.LocalContext
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import me.avinas.tempo.R
import me.avinas.tempo.widget.theme.TempoWidgetColors
import me.avinas.tempo.widget.theme.TempoWidgetTheme
import me.avinas.tempo.widget.utils.WidgetHub
import me.avinas.tempo.widget.utils.WidgetNavigation

class TempoAppWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(SMALL_SIZE, MEDIUM_SIZE, LARGE_SIZE)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val size = LocalSize.current
            
            TempoWidgetTheme {
                // Widgets use the same semantic roles as Tempo, adapted to
                // Android Glance and the launcher light/dark appearance.
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .cornerRadius(28.dp)
                        .clickable(WidgetNavigation.openStats(context))
                        .padding(
                            horizontal = getPadding(size),
                            vertical = if (size.height < 60.dp) 3.dp else getPadding(size),
                        )
                ) {
                    when {
                        size.height >= LARGE_SIZE.height -> {
                            LargeWidgetContent(prefs)
                        }
                        size.width >= MEDIUM_SIZE.width -> {
                            MediumWidgetContent(prefs, size.height)
                        }
                        else -> {
                            SmallWidgetContent(prefs)
                        }
                    }
                }
            }
        }
    }
    
    private fun getPadding(size: DpSize): androidx.compose.ui.unit.Dp {
        // Less padding for small widgets to maximize detailed view
         return if (size.width < MEDIUM_SIZE.width) 12.dp else 16.dp
    }

    @Composable
    private fun SmallWidgetContent(prefs: Preferences) {
        val totalTime = prefs[stringPreferencesKey(WidgetHub.PREF_WEEKLY_TIME_LABEL)]
            ?: prefs[stringPreferencesKey(WidgetHub.PREF_WEEKLY_TIME)]?.toLongOrNull()
                ?.let { widgetListeningTime(LocalContext.current, it) } ?: "—"
        
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = LocalContext.current.getString(R.string.stats_share_period_this_week),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp),
                maxLines = 1,
            )
            Text(
                text = totalTime,
                style = TextStyle(
                    color = ColorProvider(TempoWidgetColors.Primary),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }
    }

    @Composable
    private fun MediumWidgetContent(prefs: Preferences, availableHeight: androidx.compose.ui.unit.Dp) {
        val chartDataStr = prefs[stringPreferencesKey(WidgetHub.PREF_WEEKLY_CHART_DATA)] ?: ""
        val totalTime = prefs[stringPreferencesKey(WidgetHub.PREF_WEEKLY_TIME_LABEL)]
            ?: prefs[stringPreferencesKey(WidgetHub.PREF_WEEKLY_TIME)]?.toLongOrNull()
                ?.let { widgetListeningTime(LocalContext.current, it) } ?: "—"
        val chartData = if (chartDataStr.isNotEmpty()) {
            chartDataStr.split(",").map { it.toFloatOrNull() ?: 0f }
        } else emptyList()

        Row(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Side: Stats
            Column(
                modifier = GlanceModifier.defaultWeight().padding(end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                 Text(
                    text = "Weekly",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                )
                Text(
                    text = totalTime,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                     text = "This Week",
                     style = TextStyle(
                         color = GlanceTheme.colors.primary,
                         fontSize = 12.sp
                     )
                )
            }
            
            // Right Side: Chart
            if (chartData.any { it > 0f }) {
                 Box(
                     modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .background(GlanceTheme.colors.surface)
                        .cornerRadius(12.dp)
                        .padding(8.dp)
                 ) {
                     // Calculate chart height (approx available minus padding)
                     // or just pass availableHeight? Box fills max height, so pass availableHeight or a fraction
                     // Since parent Box fillMaxHeight of Row, it has the Row's height.
                     // The Row fills MaxSize of Widget.
                     // So we can use availableHeight.
                     BarChart(
                         data = chartData,
                         maxHeight = (availableHeight - 48.dp).coerceAtLeast(16.dp),
                         modifier = GlanceModifier.fillMaxSize(),
                     )
                 }
            }
        }
    }

    @Composable
    private fun LargeWidgetContent(prefs: Preferences) {
        val totalTime = prefs[stringPreferencesKey(WidgetHub.PREF_WEEKLY_TIME_LABEL)]
            ?: prefs[stringPreferencesKey(WidgetHub.PREF_WEEKLY_TIME)]?.toLongOrNull()
                ?.let { widgetListeningTime(LocalContext.current, it) } ?: "—"
        val context = LocalContext.current
        val plays = prefs[stringPreferencesKey(WidgetHub.PREF_WEEKLY_PLAYS)]?.toIntOrNull()
        val topArtist = prefs[stringPreferencesKey(WidgetHub.PREF_TOP_ARTIST_NAME)] ?: "No Data"
        val artistImagePath = prefs[stringPreferencesKey(WidgetHub.PREF_TOP_ARTIST_IMAGE)]
        
        val chartDataStr = prefs[stringPreferencesKey(WidgetHub.PREF_WEEKLY_CHART_DATA)] ?: ""
        val chartData = if (chartDataStr.isNotEmpty()) {
            chartDataStr.split(",").map { it.toFloatOrNull() ?: 0f }
        } else emptyList()

        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Header
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                 Column(modifier = GlanceModifier.defaultWeight()) {
                     Text("This Week's Listening", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp))
                     Text(totalTime, style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 28.sp, fontWeight = FontWeight.Bold))
                 }
                 
                 if (plays != null) {
                     Text(
                         text = context.resources.getQuantityString(R.plurals.widget_play_count, plays, plays),
                         style = TextStyle(color = GlanceTheme.colors.secondary, fontSize = 14.sp),
                         maxLines = 1,
                     )
                 }
            }
            
            Spacer(modifier = GlanceModifier.height(16.dp))
            
            // Artist Card (Hero section)
            Box(
                 modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(80.dp) // Fixed height for hero
                    .background(GlanceTheme.colors.surface)
                    .cornerRadius(12.dp),
                 contentAlignment = Alignment.CenterStart
            ) {
                // Background Image (if avail)
                 if (artistImagePath != null) {
                     val safeBitmap = me.avinas.tempo.widget.utils.BitmapGenerator.decodeBitmapSafe(artistImagePath, 512)
                     if (safeBitmap != null) {
                         Image(
                             provider = ImageProvider(safeBitmap),
                             contentDescription = null,
                             contentScale = androidx.glance.layout.ContentScale.Crop,
                             modifier = GlanceModifier.fillMaxSize().cornerRadius(12.dp)
                         )
                     }
                     // Dark overlay for text readability
                     Box(
                         modifier = GlanceModifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .cornerRadius(12.dp)
                     ) {}
                 }
                 
                 Row(
                     modifier = GlanceModifier.padding(12.dp).fillMaxWidth(),
                     verticalAlignment = Alignment.CenterVertically
                 ) {
                     Column {
                         Text(
                             "Top Artist",
                             style = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.7f)), fontSize = 11.sp)
                         )
                         Text(
                             topArtist,
                             style = TextStyle(color = ColorProvider(Color.White), fontSize = 16.sp, fontWeight = FontWeight.Bold),
                             maxLines = 1
                         )
                     }
                 }
            }

            Spacer(modifier = GlanceModifier.defaultWeight())
            
            // Chart at bottom
            if (chartData.any { it > 0f }) {
                Text("This Week", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp))
                Spacer(modifier = GlanceModifier.height(4.dp))
                Box(modifier = GlanceModifier.fillMaxWidth().height(60.dp)) {
                     BarChart(data = chartData, maxHeight = 60.dp, modifier = GlanceModifier.fillMaxSize())
                }
            }
        }
    }

    @Composable
    private fun BarChart(data: List<Float>, maxHeight: androidx.compose.ui.unit.Dp, modifier: GlanceModifier = GlanceModifier) {
        val max = data.maxOrNull() ?: 1f
        val safeMax = if (max == 0f) 1f else max
        
        Row(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { value ->
                val heightPercent = (value / safeMax).coerceIn(0f, 1f)
                val barHeight = maxHeight * heightPercent
                
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .padding(horizontal = 2.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(barHeight)
                            .background(GlanceTheme.colors.primary)
                            .cornerRadius(4.dp)
                    ) {}
                }
            }
        }
    }

    companion object {
        private val SMALL_SIZE = DpSize(100.dp, 40.dp) // Adjusted slightly
        private val MEDIUM_SIZE = DpSize(200.dp, 100.dp)
        private val LARGE_SIZE = DpSize(200.dp, 200.dp)
    }
}
