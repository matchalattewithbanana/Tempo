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
internal object Cue {
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

internal fun seg(
    t: Float,
    from: Float,
    to: Float,
): Float = ((t - from) / (to - from)).coerceIn(0f, 1f)

internal fun ease(p: Float): Float = FastOutSlowInEasing.transform(p)

internal fun easeOut(p: Float): Float = LinearOutSlowInEasing.transform(p)

internal fun easeIn(p: Float): Float = FastOutLinearInEasing.transform(p)

/** Cut-to-black at the loop seam — the wrap reads as a video cut, not a snap
 *  of every element back to its start position. */
private fun loopDim(t: Float): Float =
    when {
        t < 0.45f -> 1f - ease(t / 0.45f)
        t > LoopSeconds - 0.7f -> ease(seg(t, LoopSeconds - 0.7f, LoopSeconds))
        else -> 0f
    }

/** One-shot tap ripple. Returns -1 when idle, 0→1 while expanding. */
internal fun ripple(
    t: Float,
    at: Float,
    dur: Float = 0.5f,
): Float {
    if (at.isNaN()) return -1f
    val p = (t - at) / dur
    return if (p < 0f || p > 1f) -1f else p
}

/** Touch-down dip for a tapped element: 1 → 0.87 → 1. */
internal fun press(
    t: Float,
    at: Float,
    dur: Float = 0.3f,
): Float {
    if (at.isNaN()) return 1f
    val p = (t - at) / dur
    return if (p < 0f || p > 1f) 1f else 1f - 0.13f * sin(p * PI).toFloat()
}

private data class TutorialStep(
    val at: Float,
    val title: String,
    val hint: String,
)

private val TakeoutSteps =
    listOf(
        TutorialStep(0f, "Deselect everything", "Click “Deselect all” — the export is huge otherwise."),
        TutorialStep(2.6f, "Select YouTube and YouTube Music", "Scroll to the bottom of the product list."),
        TutorialStep(5.7f, "Keep only “history”", "Open the content options, deselect all, tick history."),
        TutorialStep(10.1f, "Set History to JSON", "“Multiple formats” → History → JSON. HTML works too."),
        TutorialStep(15.0f, "Create the export, then pick the ZIP here", "Download the ZIP — no need to extract it."),
    )

// ── Google Material tokens ────────────────────────────────────
// The real surface colours, so the mockup reads as the actual site
// rather than a Tempo-shaped impression of it.
internal val GBlue = Color(0xFF1A73E8)
internal val GRed = Color(0xFFEA4335)
internal val GYellow = Color(0xFFFBBC04)
internal val GGreen = Color(0xFF34A853)
internal val GInk = Color(0xFF202124)
internal val GInkDim = Color(0xFF5F6368)
internal val GInkFaint = Color(0xFF80868B)
internal val GLine = Color(0xFFDADCE0)
internal val GFace = Color(0xFFFFFFFF)
internal val GWell = Color(0xFFF1F3F4)
internal val YtTile = Color(0xFFE53935)
private val PhoneBody = Color(0xFFD3D7DD)

// ──────────────────────────────────────────────────────────────
// Entry point
// ──────────────────────────────────────────────────────────────

/**
 * Animated walkthrough of the Google Takeout export flow, with a step
 * list that tracks it. Drop it anywhere; it owns its own clock.
 */
@Composable
fun TakeoutTutorial(modifier: Modifier = Modifier) {
    val reduced = rememberReducedMotion()
    var restarts by remember { mutableIntStateOf(0) }
    var elapsed by remember { mutableFloatStateOf(0f) }

    if (!reduced) {
        LaunchedEffect(restarts) {
            val start = withFrameNanos { it }
            while (true) {
                val e = ((withFrameNanos { it } - start) / 1_000_000_000.0).toFloat()
                if (e >= PlayThroughs * LoopSeconds) {
                    elapsed = PlayThroughs * LoopSeconds
                    break
                }
                elapsed = e
            }
        }
    }

    val finished = !reduced && elapsed >= PlayThroughs * LoopSeconds
    val t =
        when {
            reduced -> ReducedMotionFreeze

            // Park inside the seam black — blur and replay pill then fade in
            // over black, never over a frozen frame.
            finished -> LoopSeconds

            else -> elapsed % LoopSeconds
        }
    val active = TakeoutSteps.indexOfLast { t >= it.at }.coerceAtLeast(0)
    // Park/replay glide in and out instead of snapping.
    val park by animateFloatAsState(if (finished) 1f else 0f, tween(420), label = "park")

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier =
                Modifier.fillMaxWidth().pointerInput(finished) {
                    if (finished) detectTapGestures { restarts++ }
                },
        ) {
            // Blur is a no-op below Android 12 — the replay scrim carries the
            // parked state there.
            Box(Modifier.fillMaxWidth().blur(7.dp * park)) {
                MockupStage {
                    TakeoutFrame(dim = loopDim(t)) {
                        TakeoutPager(t)
                        ContentOptionsDialog(t)
                        FormatsSheet(t)
                    }
                }
            }
            if (park > 0.01f) TakeoutReplayOverlay(park)
        }

        Spacer(Modifier.height(18.dp))

        // Live caption — what the phone is doing right now. Crossfades between
        // steps so the narration glides with the phone instead of snapping.
        AnimatedContent(
            targetState = active,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
            label = "caption",
        ) { step ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(22.dp).background(GRed, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${step + 1}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = TakeoutSteps[step].title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = TakeoutSteps[step].hint,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Loop progress — tells you the phone is a recording, not a screen to poke.
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f)),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(t / LoopSeconds)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(GRed),
            )
        }

        Spacer(Modifier.height(14.dp))

        // The words. Also the whole guide when motion is off.
        Column(Modifier.fillMaxWidth()) {
            TakeoutSteps.forEachIndexed { i, step ->
                val on = i == active
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = "${i + 1}",
                        color = if (on) GRed else Color.White.copy(alpha = 0.35f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(16.dp),
                    )
                    Text(
                        text = step.title,
                        color = if (on) Color.White else Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

/** Scrim over the parked walkthrough: dimmed, blurred, one obvious way back in. */
@Composable
private fun BoxScope.TakeoutReplayOverlay(alpha: Float) {
    Box(
        modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.55f * alpha)),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier =
                Modifier
                    .alpha(alpha)
                    .clip(CircleShape)
                    .background(GRed)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Replay,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("Tap to replay", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** The walkthrough as a dismissible overlay — for places with no room for
 *  the inline tutorial (the restore screen's YouTube card). */
@Composable
fun TakeoutGuideDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val scrimSource = remember { MutableInteractionSource() }
        val swallowSource = remember { MutableInteractionSource() }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(scrimSource, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(TempoDarkBackground)
                        // Absorb taps so only the scrim (or the ✕) dismisses.
                        .clickable(swallowSource, indication = null, onClick = {})
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "How to get your file from Google Takeout",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }
                TakeoutTutorial()
            }
        }
    }
}

/** Lays content out at its full height even when it overflows [viewport].
 *  A plain Column inside a fixed-height Box gets its children clamped to zero
 *  height once the space runs out, so every row past the fold silently vanishes. */
internal fun Modifier.overflowScroll(scroll: Dp = 0.dp): Modifier = wrapContentHeight(Alignment.Top, unbounded = true).offset(y = -scroll)

// ──────────────────────────────────────────────────────────────
// Device + page router
// ──────────────────────────────────────────────────────────────

/** Silver bezel around a white screen — a phone, not a floating card.
 *  [dim] blacks the screen out across the loop seam. */
@Composable
private fun TakeoutFrame(
    dim: Float = 0f,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(width = 258.dp, height = 452.dp)
                .clay(
                    background = PhoneBody,
                    cornerRadius = 34.dp,
                    outset = Color.Black.copy(alpha = 0.45f),
                    outsetElevation = 18.dp,
                    rimFraction = 0.02f,
                ).clip(RoundedCornerShape(34.dp))
                .padding(6.dp),
    ) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)).background(GFace)) {
            Column(Modifier.fillMaxSize()) {
                AppBar()
                Box(Modifier.fillMaxWidth().height(1.dp).background(GLine))
                Box(Modifier.fillMaxSize()) { content() }
            }
            if (dim > 0.01f) {
                Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = dim)))
            }
        }
    }
}

/** Three pages, pushed left in sequence: products → export → ZIP → Tempo. */
@Composable
private fun BoxScope.TakeoutPager(t: Float) {
    val out1 = ease(seg(t, Cue.PageOutStart, Cue.PageOutStart + Cue.PageShift))
    val out2 = ease(seg(t, Cue.ZipStart, Cue.ZipStart + Cue.PageShift))

    Box(Modifier.offset(x = -ScreenW * out1)) { ProductListScreen(t) }
    Box(Modifier.offset(x = ScreenW * (1f - out1) - ScreenW * out2)) { ExportScreen(t) }
    Box(Modifier.offset(x = ScreenW * (1f - out2))) { ZipToTempoScreen(t) }
}

@Composable
private fun AppBar() {
    Row(
        modifier = Modifier.fillMaxWidth().height(30.dp).padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            tint = GInk,
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text("Google Takeout", color = GInk, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

// ──────────────────────────────────────────────────────────────
// Page 0 · Select data to include
// ──────────────────────────────────────────────────────────────

internal data class TakeoutProduct(
    val name: String,
    val desc: String,
    val icon: ImageVector? = null,
    val tint: Color = GInkDim,
    val googleMark: Boolean = false,
    val youTube: Boolean = false,
)

// Real product order (alphabetical, YouTube last) so the scroll to reach it
// is the same scroll the user has to do.
internal val Products =
    listOf(
        TakeoutProduct("Access Log Activity", "Collection of account activity logs", googleMark = true),
        TakeoutProduct("Alerts", "User Google Alerts subscriptions", Icons.Default.Notifications, GYellow),
        TakeoutProduct("Blogger", "Your blogs and their content", Icons.Default.Description, Color(0xFFF57C00)),
        TakeoutProduct("Calendar", "Your calendars and reminders", Icons.Default.DateRange, GBlue),
        TakeoutProduct("Chrome", "Your browsing history and bookmarks", Icons.Default.Explore, Color(0xFF4285F4)),
        TakeoutProduct("Contacts", "Your contacts and circles", Icons.Default.Person, GBlue),
        TakeoutProduct("Drive", "Your files and folders", Icons.Default.Cloud, GGreen),
        TakeoutProduct("Maps", "Your places and reviews", Icons.Default.Place, GGreen),
        TakeoutProduct("Location History", "Your Timeline data, like settings and locations.", Icons.Default.LocationOn, GBlue),
        TakeoutProduct("Workspace Studio", "Your flows and activity log.", Icons.Default.AutoAwesome, Color(0xFF5E5CE6)),
        TakeoutProduct("YouTube and YouTube Music", "Watch and search history, videos and other content", youTube = true),
    )

internal val ProductRowH = 44.dp
internal val ProductListHead = 26.dp
internal val ChipsH = 30.dp
internal val ListViewport = 314.dp

/** Bottom of the list, chips included — the exact scroll that shows YouTube. */
internal val ListMaxScroll = ProductRowH * Products.size + ProductListHead + ChipsH - ListViewport

