package me.avinas.tempo.ui.youtube

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.avinas.tempo.R
import me.avinas.tempo.ui.clay.clay
import me.avinas.tempo.ui.onboarding.MockupStage
import me.avinas.tempo.ui.theme.TempoDarkBackground
import me.avinas.tempo.ui.theme.rememberReducedMotion
import kotlin.math.PI
import kotlin.math.sin

// ──────────────────────────────────────────────────────────────
// Takeout walkthrough — a phone playing the real takeout.google.com
// flow. Plays twice, then parks on the last frame under a blur with a
// tap-to-replay scrim: an endless loop reads as a screensaver, a
// stopped one as a lesson you can restart.
//
// Navigation is the real navigation: the dialog scales in over a
// scrim, the format editor slides up as a sheet, pages push left,
// the list scrolls. Every tick, chip and ripple is drawn — the only
// thing that isn't real Google UI is the size.
//
// One frame-clock drives all of it; each screen slices the clock for
// its own cue (see [Cue]). Frozen on the most informative frame when
// the system asks for reduced motion.
//
// ponytail: no Lottie, no assets, no per-screen state machines — one
// float timeline and pure functions of it, so replaying is free.
// ──────────────────────────────────────────────────────────────

internal const val LoopSeconds = 23.0f

/** How many times the walkthrough plays before parking under the replay scrim. */
internal const val PlayThroughs = 2

/** Where the loop parks when the system disables animations: the dialog
 *  with only "history" ticked — the single frame that answers the question. */
internal const val ReducedMotionFreeze = 8.6f

/** Screen (content) width inside the bezel — every page push uses it. */
internal val ScreenW = 246.dp

// ── Cue table ─────────────────────────────────────────────────
// Every beat of the walkthrough, in seconds. Screens read these so a
// timing tweak never needs two edits.
private object Cue {
    const val DeselectTap = 1.15f
    const val DeselectRun = 1.45f
    const val ScrollStart = 2.6f
    const val ScrollEnd = 3.6f
    const val YouTubeTap = 3.85f
    const val YouTubeCheck = 4.05f
    const val YouTubeCheckDone = 4.5f
    const val ChipsIn = 4.6f
    const val ChipsDone = 5.1f

    // The two chips are separate buttons tapped seconds apart. Sharing one cue
    // made them light up together and read as a single control.
    const val FormatsChipTap = 10.1f
    const val DataChipTap = 5.5f
    const val DialogIn = 5.7f
    const val DialogDone = 6.3f
    const val DialogDeselectTap = 6.7f
    const val DialogDeselectRun = 6.95f
    const val DialogDeselectDone = 7.9f
    const val HistoryTap = 8.15f
    const val HistoryCheck = 8.3f
    const val HistoryDone = 8.75f
    const val OkTap = 9.1f
    const val DialogOut = 9.25f
    const val DialogOutDone = 9.8f
    const val FormatsTap = 10.1f
    const val SheetIn = 10.3f
    const val SheetInDone = 10.75f
    const val FormatsScrollStart = 11.1f
    const val FormatsScrollEnd = 11.9f
    const val DropdownTap = 12.15f
    const val MenuIn = 12.3f
    const val MenuInDone = 12.7f
    const val JsonTap = 13.05f
    const val MenuOut = 13.2f
    const val JsonSet = 13.5f
    const val SheetOkTap = 14.3f
    const val SheetOut = 14.45f
    const val SheetOutDone = 15.05f
    const val NextTap = 15.5f
    const val PageOutStart = 15.7f

    // Pages cross rather than sliding out into a blank screen and back in: both
    // pages travel one width inside a single window.
    const val PageShift = 0.35f
    const val CreateTap = 17.5f
    const val ProgressStart = 17.7f
    const val ProgressEnd = 19.3f
    const val DownloadTap = 19.8f
    const val ZipStart = 20.1f
    const val ZipEnd = 20.9f
}

@Composable
internal fun ProductListScreen(t: Float) {
    val scroll = ListMaxScroll * ease(seg(t, Cue.ScrollStart, Cue.ScrollEnd))
    val chips = ease(seg(t, Cue.ChipsIn, Cue.ChipsDone))
    val ytCheck = maxOf(1f - productClear(t, Products.lastIndex), ease(seg(t, Cue.YouTubeCheck, Cue.YouTubeCheckDone)))
    val selected =
        when {
            t < Cue.DeselectRun -> 59
            t < Cue.YouTubeCheck -> 0
            else -> 1
        }

    Column(Modifier.fillMaxSize()) {
        StepHeader(badge = "1", title = "Select data to include", trailing = "$selected of 60 selected")
        Box(Modifier.fillMaxWidth().height(1.dp).background(GLine))

        Box(Modifier.fillMaxWidth().height(ListViewport).clipToBounds()) {
            Column(Modifier.overflowScroll(scroll)) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(ProductListHead).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Products", color = GInk, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.weight(1f))
                    Rippled(p = ripple(t, Cue.DeselectTap), shape = RoundedCornerShape(6.dp)) {
                        Text(
                            "Deselect all",
                            color = GBlue,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
                        )
                    }
                }

                Products.forEachIndexed { i, p ->
                    ProductRow(
                        product = p,
                        check = if (p.youTube) ytCheck else 1f - productClear(t, i),
                        tapAt = if (p.youTube) Cue.YouTubeTap else Float.NaN,
                        t = t,
                    )
                }

                // Appear only once YouTube is ticked — same as the real page.
                if (chips > 0.01f) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(ChipsH)
                                .padding(start = 12.dp)
                                .alpha(chips),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Rippled(p = ripple(t, Cue.FormatsChipTap), shape = CircleShape) {
                            MockChip("Multiple formats", Icons.Default.FolderOpen)
                        }
                        Rippled(p = ripple(t, Cue.DataChipTap), shape = CircleShape) {
                            MockChip("All YouTube data included", Icons.Default.Check)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Box(Modifier.fillMaxWidth().height(1.dp).background(GLine))
        Row(
            modifier = Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            Rippled(
                p = ripple(t, Cue.NextTap),
                shape = CircleShape,
                modifier = Modifier.scale(press(t, Cue.NextTap)),
            ) {
                Box(
                    modifier = Modifier.background(GBlue, CircleShape).padding(horizontal = 16.dp, vertical = 7.dp),
                ) {
                    Text("Next step", color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/** 0→1 stagger for "Deselect all" clearing product [index]. */
private fun productClear(
    t: Float,
    index: Int,
): Float {
    val start = Cue.DeselectRun + index * 0.045f
    return ease(seg(t, start, start + 0.6f))
}

@Composable
private fun ProductRow(
    product: TakeoutProduct,
    check: Float,
    tapAt: Float,
    t: Float,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(ProductRowH).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            product.googleMark -> {
                GoogleGMark(18.dp)
            }

            product.youTube -> {
                BrandTile(R.drawable.ic_ytmusic_mark, YtTile, 20.dp)
            }

            product.icon != null -> {
                Icon(
                    imageVector = product.icon,
                    contentDescription = null,
                    tint = product.tint,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = product.name,
                color = GInk,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = product.desc,
                color = GInkFaint,
                fontSize = 8.sp,
                lineHeight = 10.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(6.dp))
        Rippled(
            p = ripple(t, tapAt),
            shape = CircleShape,
            modifier = Modifier.scale(press(t, tapAt)),
        ) {
            GoogleCheckbox(check, Modifier.padding(4.dp))
        }
    }
}

@Composable
internal fun StepHeader(
    badge: String,
    title: String,
    trailing: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(30.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(13.dp).clip(CircleShape).background(GWell),
            contentAlignment = Alignment.Center,
        ) {
            Text(badge, color = GInkDim, fontSize = 7.5.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.width(7.dp))
        Text(title, color = GInk, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        Text(trailing, color = GInkDim, fontSize = 8.5.sp)
    }
}

// ──────────────────────────────────────────────────────────────
// Overlay · "YouTube and YouTube Music content options" dialog
// ──────────────────────────────────────────────────────────────

// Exactly the real list, in the real order — "history" is third.
private val ContentItems =
    listOf(
        "channels",
        "creator-demographics",
        "history",
        "kids",
        "live chats",
        "music (library and uploads)",
        "playables",
        "playlists",
        "shopping",
        "subscriptions",
        "support issues",
        "video metadata",
        "videos",
    )

private const val HistoryIndex = 2

@Composable
internal fun BoxScope.ContentOptionsDialog(t: Float) {
    val appear = ease(seg(t, Cue.DialogIn, Cue.DialogDone))
    val gone = ease(seg(t, Cue.DialogOut, Cue.DialogOutDone))
    val shown = appear - gone
    if (shown <= 0.01f) return

    val clearing = ease(seg(t, Cue.DialogDeselectRun, Cue.DialogDeselectDone))
    val historyOn = ease(seg(t, Cue.HistoryCheck, Cue.HistoryDone))
    val allOff = clearing > 0.999f && historyOn < 0.5f

    Box(
        modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.32f * shown)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .width(208.dp)
                    .scale(0.92f + 0.08f * shown)
                    .alpha(shown)
                    .shadow(12.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(GFace)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "YouTube and YouTube Music content options",
                color = GInk,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Choose specific YouTube data for your export",
                color = GInkDim,
                fontSize = 8.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))

            Row(Modifier.fillMaxWidth()) {
                Rippled(p = ripple(t, Cue.DialogDeselectTap), shape = RoundedCornerShape(6.dp)) {
                    Text(
                        text = if (allOff) "Select all" else "Deselect all",
                        color = GBlue,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
                    )
                }
            }

            // Long list, clipped — the real dialog scrolls too.
            Column(Modifier.fillMaxWidth().height(186.dp).clipToBounds()) {
                Column(Modifier.overflowScroll()) {
                    ContentItems.forEachIndexed { i, name ->
                        val check =
                            if (i == HistoryIndex) {
                                maxOf(1f - contentClear(clearing, i), historyOn)
                            } else {
                                1f - contentClear(clearing, i)
                            }
                        Row(
                            modifier = Modifier.fillMaxWidth().height(26.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Rippled(
                                p = if (i == HistoryIndex) ripple(t, Cue.HistoryTap) else -1f,
                                shape = CircleShape,
                                modifier = Modifier.scale(press(t, if (i == HistoryIndex) Cue.HistoryTap else Float.NaN)),
                            ) {
                                GoogleCheckbox(check, Modifier.padding(4.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(name, color = GInk, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    "Cancel",
                    color = GInkDim,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
                Spacer(Modifier.width(4.dp))
                Rippled(
                    p = ripple(t, Cue.OkTap),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.scale(press(t, Cue.OkTap)),
                ) {
                    Text(
                        "OK",
                        color = GBlue,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

private fun contentClear(
    clearing: Float,
    index: Int,
): Float = seg(clearing, index * 0.02f, index * 0.02f + 0.7f)

// ──────────────────────────────────────────────────────────────
// Overlay · format editor sheet ("Multiple formats")
// ──────────────────────────────────────────────────────────────

private data class FormatRow(
    val name: String,
    val desc: String,
    val value: String,
)

private val FormatRows =
    listOf(
        FormatRow("video media", "Videos you've uploaded", "Original format"),
        FormatRow("videos", "The video entity data", "CSV"),
        FormatRow("video interactions", "Interactions on a YouTube Video.", "CSV"),
        FormatRow("playlists", "Playlists you've created", "CSV"),
        FormatRow("channel", "Information about your channel.", "CSV"),
        FormatRow("clips", "Clips you've created", "CSV"),
        FormatRow("music media", "Music media you've uploaded", "Original format"),
        FormatRow("music library songs", "Songs added to your YouTube Music library.", "CSV"),
        FormatRow("history", "Your watch and search history from YouTube.", "HTML"),
        FormatRow("creator demographics", "Self-ID survey responses.", "CSV"),
        FormatRow("product save list", "YouTube Shopping wishlist.", "CSV"),
        FormatRow("playables metadata", "Your YouTube Playable saved games.", "CSV"),
        FormatRow("subscriptions", "Channels you're subscribed to.", "CSV"),
        FormatRow("live chats", "Messages you sent in live chats.", "CSV"),
    )

private const val HistoryRow = 8
private val FormatRowH = 36.dp
private val FormatViewport = 190.dp

/** Scroll that parks the history row at a comfortable reading height. */
private val FormatScroll = FormatRowH * HistoryRow - 60.dp

/** Same island as the content-options dialog — Google uses one dialog shape
 *  for both steps, so the sheet must not be a different species. */
@Composable
internal fun BoxScope.FormatsSheet(t: Float) {
    val up = ease(seg(t, Cue.SheetIn, Cue.SheetInDone))
    val down = ease(seg(t, Cue.SheetOut, Cue.SheetOutDone))
    val shown = up - down
    if (shown <= 0.01f) return

    val scroll = FormatScroll * ease(seg(t, Cue.FormatsScrollStart, Cue.FormatsScrollEnd))
    val menu = (ease(seg(t, Cue.MenuIn, Cue.MenuInDone)) - ease(seg(t, Cue.MenuOut, Cue.JsonSet))).coerceIn(0f, 1f)

    Box(
        modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.32f * shown)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .width(208.dp)
                    .scale(0.92f + 0.08f * shown)
                    .alpha(shown)
                    .shadow(12.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(GFace)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "YouTube and YouTube Music options",
                color = GInk,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Choose specific formats for your archive",
                color = GInkDim,
                fontSize = 8.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Edit file formats",
                color = GInk,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(2.dp))

            Box(Modifier.fillMaxWidth().height(FormatViewport).clipToBounds()) {
                Column(Modifier.overflowScroll(scroll)) {
                    FormatRows.forEachIndexed { i, row ->
                        FormatRowItem(
                            row = row,
                            isHistory = i == HistoryRow,
                            menu = menu,
                            set = t >= Cue.JsonSet,
                            t = t,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    "Cancel",
                    color = GInkDim,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
                Spacer(Modifier.width(4.dp))
                Rippled(
                    p = ripple(t, Cue.SheetOkTap),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.scale(press(t, Cue.SheetOkTap)),
                ) {
                    Text(
                        "OK",
                        color = GBlue,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FormatRowItem(
    row: FormatRow,
    isHistory: Boolean,
    menu: Float,
    set: Boolean,
    t: Float,
) {
    val tapAt = if (isHistory) Cue.DropdownTap else Float.NaN
    // History starts on HTML; the whole point of the step is switching it.
    val value = if (isHistory && set) "JSON" else row.value
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().height(FormatRowH).padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(row.name, color = GInk, fontSize = 9.5.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(
                    text = row.desc,
                    color = GInkFaint,
                    fontSize = 7.5.sp,
                    lineHeight = 9.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(6.dp))
            Rippled(
                p = ripple(t, tapAt),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.scale(press(t, tapAt)),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
                ) {
                    Text(value, color = GInkDim, fontSize = 8.5.sp)
                    Spacer(Modifier.width(3.dp))
                    Caret(up = menu > 0.5f)
                }
            }
        }
        if (isHistory && menu > 0.01f) {
            // The real dropdown, anchored under the value it belongs to.
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Column(
                    modifier =
                        Modifier
                            .padding(end = 2.dp)
                            .width(72.dp)
                            .alpha(menu)
                            .shadow(8.dp, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .background(GFace)
                            .padding(vertical = 4.dp),
                ) {
                    MenuItem("HTML", selected = !set)
                    Rippled(p = ripple(t, Cue.JsonTap), shape = RoundedCornerShape(4.dp)) {
                        MenuItem("JSON", selected = set)
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItem(
    label: String,
    selected: Boolean,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (selected) GWell else Color.Transparent)
                .padding(horizontal = 8.dp, vertical = 5.dp),
    ) {
        Text(
            text = label,
            color = GInk,
            fontSize = 8.5.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End,
        )
    }
}

/** The little ▾ on a value pill, flipping up while its menu is open. */
@Composable
private fun Caret(up: Boolean) {
    Canvas(Modifier.size(6.dp)) {
        val w = size.width
        val h = size.height
        val path = Path()
        if (up) {
            path.moveTo(w * 0.1f, h * 0.72f)
            path.lineTo(w * 0.5f, h * 0.28f)
            path.lineTo(w * 0.9f, h * 0.72f)
        } else {
            path.moveTo(w * 0.1f, h * 0.28f)
            path.lineTo(w * 0.5f, h * 0.72f)
            path.lineTo(w * 0.9f, h * 0.28f)
        }
        drawPath(path, GInkFaint, style = Stroke(width = w * 0.16f, cap = StrokeCap.Round))
    }
}

// ──────────────────────────────────────────────────────────────
// Page 1 · Create export
// ──────────────────────────────────────────────────────────────

