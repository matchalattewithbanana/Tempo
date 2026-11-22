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

@Composable
internal fun ExportScreen(t: Float) {
    val progress = easeIn(seg(t, Cue.ProgressStart, Cue.ProgressEnd))
    val created = t >= Cue.CreateTap

    Column(Modifier.fillMaxSize()) {
        StepHeader(badge = "2", title = "Choose file type & destination", trailing = "")
        Box(Modifier.fillMaxWidth().height(1.dp).background(GLine))
        Spacer(Modifier.height(10.dp))

        Column(Modifier.padding(horizontal = 12.dp)) {
            listOf(
                "Frequency" to "Export once",
                "File type" to ".zip",
                "Destination" to "Send download link via email",
            ).forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, color = GInkDim, fontSize = 9.sp, modifier = Modifier.weight(1f))
                    Text(value, color = GInk, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(10.dp))

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(GWell)
                        .padding(12.dp),
            ) {
                Text("Export progress", color = GInk, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(GLine),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth(progress)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(GBlue),
                    )
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    text =
                        when {
                            !created -> "Waiting for you to create the export…"
                            progress < 1f -> "In progress · ${(progress * 100).toInt()}%"
                            else -> "Your archive is ready to download"
                        },
                    color = GInkDim,
                    fontSize = 8.sp,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            val at = if (progress >= 1f) Cue.DownloadTap else Cue.CreateTap
            Rippled(p = ripple(t, at), shape = CircleShape, modifier = Modifier.scale(press(t, at))) {
                Box(Modifier.background(GBlue, CircleShape).padding(horizontal = 16.dp, vertical = 7.dp)) {
                    Text(
                        text = if (progress >= 1f) "Download" else "Create export",
                        color = Color.White,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────
// Page 2 · The ZIP lands in Tempo
// ──────────────────────────────────────────────────────────────

@Composable
internal fun ZipToTempoScreen(t: Float) {
    val drop = easeOut(seg(t, Cue.ZipStart + 0.15f, Cue.ZipEnd))
    val hold = ripple(t, Cue.ZipEnd + 0.15f, 0.6f)

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Your export", color = GInkDim, fontSize = 9.sp)
        Spacer(Modifier.height(8.dp))

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .offset(y = (-24).dp * (1f - drop))
                    .alpha(drop)
                    .clip(RoundedCornerShape(10.dp))
                    .background(GWell)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = GInkDim, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "takeout-20260919T074424Z-1-001.zip",
                    color = GInk,
                    fontSize = 8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("Select this file — don't unzip it", color = GInkFaint, fontSize = 7.5.sp)
            }
        }

        Spacer(Modifier.height(12.dp))
        ArrowDown(alpha = drop)
        Spacer(Modifier.height(12.dp))

        // Real Tempo launcher art, so the loop ends where the user is.
        Box(modifier = Modifier.size(46.dp).scale(1f + hold * 0.06f), contentAlignment = Alignment.Center) {
            if (hold >= 0f) {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(GRed.copy(alpha = (1f - hold) * 0.25f)),
                )
            }
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().scale(1.5f),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text("Tempo fills in albums and art", color = GInkDim, fontSize = 8.sp, textAlign = TextAlign.Center)
        Text("automatically in the background.", color = GInkFaint, fontSize = 7.5.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ArrowDown(alpha: Float) {
    Canvas(Modifier.size(width = 10.dp, height = 20.dp).alpha(alpha)) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.14f
        drawLine(
            color = GInkFaint,
            start = Offset(w / 2f, 0f),
            end = Offset(w / 2f, h * 0.78f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(GInkFaint, Offset(w * 0.2f, h * 0.56f), Offset(w / 2f, h * 0.82f), stroke, StrokeCap.Round)
        drawLine(GInkFaint, Offset(w * 0.8f, h * 0.56f), Offset(w / 2f, h * 0.82f), stroke, StrokeCap.Round)
    }
}

// ──────────────────────────────────────────────────────────────
// Shared pieces
// ──────────────────────────────────────────────────────────────

/** Material-style tap ripple clipped to [shape]. [p] < 0 means idle. */
@Composable
internal fun Rippled(
    p: Float,
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.clip(shape), contentAlignment = Alignment.Center) {
        if (p >= 0f) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(
                    color = GBlue.copy(alpha = (1f - p) * 0.28f),
                    radius = size.maxDimension * (0.4f + p * 0.75f),
                )
            }
        }
        content()
    }
}

@Composable
internal fun GoogleCheckbox(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(2.5.dp)
    Box(
        modifier =
            modifier
                .size(15.dp)
                .scale(0.9f + 0.1f * progress)
                .clip(shape)
                .background(GBlue.copy(alpha = progress))
                .then(
                    if (progress < 0.99f) {
                        Modifier.border(1.4.dp, GLine, shape)
                    } else {
                        Modifier
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (progress > 0.02f) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White.copy(alpha = progress),
                modifier = Modifier.size(11.dp),
            )
        }
    }
}

@Composable
internal fun BrandTile(
    @DrawableRes markRes: Int,
    brand: Color,
    size: Dp,
) {
    Box(
        modifier = Modifier.size(size).clip(RoundedCornerShape(size * 0.28f)).background(brand),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = markRes),
            contentDescription = null,
            modifier = Modifier.size(size * 0.66f),
        )
    }
}

/** The four-colour G, drawn — no asset, reads at 18dp. */
@Composable
internal fun GoogleGMark(size: Dp) {
    Canvas(Modifier.size(size)) {
        val d = this.size.minDimension
        val stroke = d * 0.22f
        val ring = d - stroke
        val origin = Offset(stroke / 2f, stroke / 2f)
        val box = Size(ring, ring)
        val style = Stroke(width = stroke, cap = StrokeCap.Butt)
        drawArc(GRed, -150f, 120f, false, origin, box, style = style)
        drawArc(GYellow, 150f, 60f, false, origin, box, style = style)
        drawArc(GGreen, 60f, 90f, false, origin, box, style = style)
        drawArc(GBlue, -30f, 90f, false, origin, box, style = style)
        // The bar that closes the G.
        drawRect(
            color = GBlue,
            topLeft = Offset(d * 0.46f, d / 2f - stroke / 2f),
            size = Size(d * 0.54f, stroke),
        )
    }
}

@Composable
internal fun MockChip(
    label: String,
    icon: ImageVector,
) {
    Row(
        modifier =
            Modifier
                .clip(CircleShape)
                .border(1.dp, GLine, CircleShape)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = GBlue, modifier = Modifier.size(9.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = GBlue, fontSize = 8.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}
