package me.avinas.tempo.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.BitmapImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.avinas.tempo.data.enrichment.MusicBrainzEnrichmentService
import me.avinas.tempo.ui.theme.TempoDarkBackground
import me.avinas.tempo.utils.MAX_SAFE_BITMAP_EDGE_PX
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// In-memory LRU cache for CPU-rendered backdrops (fluted glass, ascii art)
// Avoids redundant computations across preview and capture view, and speeds up theme toggles.
internal fun DrawScope.drawGrainyGradient(palette: ShareThemePalette) {
    val w = size.width
    val h = size.height
    // Warm mesh: rose top-left, amber bottom-right, soft amber core.
    drawCircle(
        brush =
            Brush.radialGradient(
                colors =
                    listOf(
                        palette.glowTop.copy(alpha = 0.55f * palette.decorationAlpha),
                        Color.Transparent,
                    ),
                center = Offset(w * 0.18f, h * 0.16f),
                radius = w * 0.95f,
            ),
        radius = w * 0.95f,
        center = Offset(w * 0.18f, h * 0.16f),
    )
    drawCircle(
        brush =
            Brush.radialGradient(
                colors =
                    listOf(
                        palette.glowBottom.copy(alpha = 0.50f * palette.decorationAlpha),
                        Color.Transparent,
                    ),
                center = Offset(w * 0.85f, h * 0.78f),
                radius = w * 1.0f,
            ),
        radius = w * 1.0f,
        center = Offset(w * 0.85f, h * 0.78f),
    )
    drawCircle(
        brush =
            Brush.radialGradient(
                colors =
                    listOf(
                        palette.accent.copy(alpha = 0.28f * palette.decorationAlpha),
                        Color.Transparent,
                    ),
                center = Offset(w * 0.68f, h * 0.34f),
                radius = w * 0.55f,
            ),
        radius = w * 0.55f,
        center = Offset(w * 0.68f, h * 0.34f),
    )
    // Film grain: deterministic speckles, preview and capture stay identical.
    val rng = Random(GRAIN_SEED)
    repeat(1100) {
        val x = rng.nextFloat() * w
        val y = rng.nextFloat() * h
        val s = 1f + rng.nextFloat() * 1.6f
        val light = rng.nextBoolean()
        drawRect(
            color =
                if (light) {
                    Color.White.copy(alpha = 0.05f * palette.decorationAlpha)
                } else {
                    Color.Black.copy(alpha = 0.09f * palette.decorationAlpha)
                },
            topLeft = Offset(x, y),
            size = Size(s, s),
        )
    }
    // Soft vignette to seat the content.
    drawRect(
        brush =
            Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.42f * palette.decorationAlpha)),
                center = center,
                radius = maxOf(w, h) * 0.72f,
            ),
    )
}

/**
 * Shared premium finish drawn over every backdrop except [ShareBackdropStyle.GRAIN_GRADIENT]
 * (which already carries heavy grain): a light deterministic film grain that kills
 * flat-gradient banding, plus a cool white-blue frost washing down from the top edge.
 * Fixed low alphas — intentionally NOT scaled by decorationAlpha — so MIDNIGHT's faint
 * glow setting still gets texture. Pure Canvas so preview and capture match.
 */
internal fun DrawScope.drawShareFinish(palette: ShareThemePalette) {
    val w = size.width
    val h = size.height
    // Cool frost from the top: the white-blue lift that keeps dark scenes from
    // reading as crushed black blur.
    if (palette.isDark) {
        drawRect(
            brush =
                Brush.verticalGradient(
                    colors =
                        listOf(
                            Color(0xFFDBEAFE).copy(alpha = 0.07f),
                            Color.Transparent,
                        ),
                    startY = 0f,
                    endY = h * 0.22f,
                ),
        )
    } else {
        drawRect(
            brush =
                Brush.verticalGradient(
                    colors =
                        listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.Transparent,
                        ),
                    startY = 0f,
                    endY = h * 0.22f,
                ),
        )
    }
    // Light film grain.
    val rng = Random(GRAIN_SEED + palette.backdrop.ordinal * 0x9E37)
    repeat(650) {
        val x = rng.nextFloat() * w
        val y = rng.nextFloat() * h
        val s = 1f + rng.nextFloat() * 1.6f
        val light = rng.nextBoolean()
        drawRect(
            color =
                if (palette.isDark) {
                    if (light) {
                        Color.White.copy(alpha = 0.035f)
                    } else {
                        Color.Black.copy(alpha = 0.06f)
                    }
                } else {
                    if (light) {
                        Color.White.copy(alpha = 0.05f)
                    } else {
                        Color.Black.copy(alpha = 0.045f)
                    }
                },
            topLeft = Offset(x, y),
            size = Size(s, s),
        )
    }
}

// ASCII backdrop parameters: render width of the baked composite, the blurred
// art base that gives the card its colour atmosphere, and the phosphor bloom
// drawn under the crisp glyphs so they read as luminous terminals.
private const val ASCII_RENDER_WIDTH = 576
private const val ASCII_BASE_BLUR_RADIUS = 14
private const val ASCII_BASE_DIM = 0.55f
private const val ASCII_GLOW_BLUR_RADIUS = 7
private const val ASCII_GLOW_ALPHA = 0.85f
private const val ASCII_BLUR_ROUNDS = 2

/**
 * Loads the artwork through Coil (sharing the app's memory/disk cache) and
 * converts it into an [AsciiArtGrid]. The conversion is *dynamic*: the
 * artwork's own luminance histogram is auto-leveled and re-gamma'd per image,
 * so dark, hazy and blown-out covers all fill the ramp instead of collapsing
 * into a black screen.
 */
private suspend fun buildAsciiArtGrid(
    context: Context,
    imageUrl: String,
    aspect: Float,
): AsciiArtGrid? {
    val request =
        ImageRequest
            .Builder(context)
            .data(MusicBrainzEnrichmentService.fixHttpUrl(imageUrl))
            .size(640, 640)
            .allowHardware(false)
            .build()
    val source =
        (context.imageLoader.execute(request).image as? BitmapImage)?.bitmap
            ?: return null
    val safeSource =
        if (source.config == Bitmap.Config.HARDWARE) {
            source.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            source
        }
    return withContext(Dispatchers.Default) {
        // Row density that keeps the artwork undistorted on glyph cells
        // taller than wide: rows/columns must be (colStep/rowStep)/aspect.
        // Bounded like every other aspect-derived size: this drives the sampled grid and its
        // IntArray, so a degenerate aspect must not blow them up.
        val rows =
            (ASCII_COLUMNS * (ASCII_COL_STEP / ASCII_ROW_STEP) / aspect)
                .roundToInt()
                .coerceIn(8, MAX_SAFE_BITMAP_EDGE_PX)
        val cropped = safeSource.centerCroppedToAspect(aspect)
        val sampled = cropped.downscaledTo(ASCII_COLUMNS, rows)
        val pixels = IntArray(ASCII_COLUMNS * rows)
        sampled.getPixels(pixels, 0, ASCII_COLUMNS, 0, 0, ASCII_COLUMNS, rows)

        // Pass 1 — raw Rec.709 luminance per cell + luminance histogram.
        val count = pixels.size
        val lum = FloatArray(count)
        val hist = IntArray(64)
        for (i in 0 until count) {
            val px = pixels[i]
            val r = (px shr 16) and 0xFF
            val g = (px shr 8) and 0xFF
            val b = px and 0xFF
            val l = (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f
            lum[i] = l
            hist[(l * 63f).toInt().coerceIn(0, 63)]++
        }

        // Pass 2 — dynamic tone mapping. A fixed contrast curve only works
        // for well-exposed art; dark or hazy covers collapse into empty
        // cells and the card reads as a black screen. Instead, auto-level
        // the artwork's own histogram (5th–98th percentile stretch) and
        // pick a gamma that re-centres the mean, so every artwork fills
        // the full ramp.
        var lo = 0f
        var hi = 1f
        val loTarget = (count * 0.05f).toInt()
        val hiTarget = (count * 0.98f).toInt()
        var acc = 0
        for (bin in 0..63) {
            acc += hist[bin]
            if (acc >= loTarget) {
                lo = bin / 63f
                break
            }
        }
        acc = 0
        for (bin in 0..63) {
            acc += hist[bin]
            if (acc >= hiTarget) {
                hi = bin / 63f
                break
            }
        }
        // Very flat art (near-solid covers): keep the full range so the
        // ordered dither still carries texture instead of one flat glyph.
        if (hi - lo < 0.20f) {
            lo = 0f
            hi = 1f
        }
        val range = (hi - lo).coerceAtLeast(0.05f)
        var sum = 0f
        val stretched = FloatArray(count)
        for (i in 0 until count) {
            val b = ((lum[i] - lo) / range).coerceIn(0f, 1f)
            stretched[i] = b
            sum += b
        }
        val mean = (sum / count).coerceIn(0.02f, 0.95f)
        val gamma = (ln(0.48f) / ln(mean)).coerceIn(0.6f, 1.8f)

        // Pass 3 — ramp selection + colour.
        val rampIndices = IntArray(count)
        val brightness = FloatArray(count)
        val colors = IntArray(count)
        val rampMax = ASCII_RAMP.length - 1
        val hsv = FloatArray(3)
        for (i in 0 until count) {
            val x = i % ASCII_COLUMNS
            val y = i / ASCII_COLUMNS
            val b = stretched[i].pow(gamma)
            brightness[i] = b
            // Ordered (Bayer 4x4) dithering before quantization — the
            // ascii-magic retro trick: smooth gradients cross-hatch into
            // glyph texture instead of banding into flat empty bands.
            val dither =
                ((ASCII_BAYER_4X4[y and 3][x and 3] + 0.5f) / 16f - 0.5f) *
                    ASCII_DITHER
            rampIndices[i] = ((b + dither) * rampMax + 0.5f).toInt().coerceIn(0, rampMax)
            // Richer colour: the artwork's own hue, saturated for the dark
            // field, with the value riding the normalized brightness — but
            // with a floor, so dark covers still paint a clearly legible
            // picture. The brightest cells lean toward white for pop.
            val px = pixels[i]
            val r = (px shr 16) and 0xFF
            val g = (px shr 8) and 0xFF
            val bl = px and 0xFF
            android.graphics.Color.RGBToHSV(r, g, bl, hsv)
            hsv[1] = (hsv[1] * 1.25f + 0.06f).coerceAtMost(1f)
            hsv[2] = (0.45f + 0.55f * b).coerceAtMost(1f)
            var color = android.graphics.Color.HSVToColor(hsv)
            if (b > 0.85f) {
                color = mixTowardWhite(color, (b - 0.85f) / 0.6f)
            }
            colors[i] = color
        }
        AsciiArtGrid(ASCII_COLUMNS, rows, rampIndices, brightness, colors)
    }
}

/**
 * Builds the full ASCII backdrop as one baked bitmap:
 *
 * 1. Base — the artwork heavily blurred and dimmed to terminal darkness, so the
 *    card picks up the art's colour atmosphere instead of floating glyphs on a
 *    flat void (the "cheap poster" look).
 * 2. Glow — the glyph layer box-blurred into a phosphor bloom under the crisp
 *    glyphs, so they read as luminous, not painted.
 * 3. Glyphs — the crisp ASCII conversion, same ramp/dither/tone mapping as
 *    before, on the wave field's monospace geometry.
 *
 * CPU-rendered into a single bitmap, so the live preview and the software-canvas
 * share capture stay pixel-identical.
 */
private suspend fun buildAsciiArtBackdrop(
    context: Context,
    imageUrl: String,
    aspect: Float,
): Bitmap? {
    val cacheKey = "ascii:$imageUrl:$aspect"
    backdropBitmapCache.get(cacheKey)?.let { if (!it.isRecycled) return it }
    val grid = buildAsciiArtGrid(context, imageUrl, aspect) ?: return null
    // Separate (cache-served) decode for the blurred colour base.
    val request =
        ImageRequest
            .Builder(context)
            .data(MusicBrainzEnrichmentService.fixHttpUrl(imageUrl))
            .size(720, 720)
            .allowHardware(false)
            .build()
    val source = (context.imageLoader.execute(request).image as? BitmapImage)?.bitmap
    val palette = ShareTheme.ASCII.palette
    return withContext(Dispatchers.Default) {
        val w = ASCII_RENDER_WIDTH
        // Aspect-derived height, bounded to the painter ceiling (see the fluted-glass path).
        val h = (w / aspect).roundToInt().coerceIn(64, MAX_SAFE_BITMAP_EDGE_PX)
        val composite = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(composite)

        // 1 — blurred, dimmed artwork as the colour base.
        if (source != null) {
            val safeSource =
                if (source.config == Bitmap.Config.HARDWARE) {
                    source.copy(Bitmap.Config.ARGB_8888, false)
                } else {
                    source
                }
            val base =
                safeSource
                    .centerCroppedToAspect(aspect)
                    .downscaledTo(w, h)
                    .boxBlurredCopy(ASCII_BASE_BLUR_RADIUS, ASCII_BLUR_ROUNDS)
                    .dimmed(ASCII_BASE_DIM)
            canvas.drawBitmap(base, 0f, 0f, null)
        }

        // 2 — the soft phosphor halo behind the glyph art.
        val halo = Paint().apply { isAntiAlias = true }
        halo.shader =
            RadialGradient(
                w * 0.5f,
                h * 0.28f,
                w * 0.9f,
                intArrayOf(
                    palette.glowTop.copy(alpha = 0.10f * palette.decorationAlpha).toArgb(),
                    Color.Transparent.toArgb(),
                ),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), halo)

        // 3 — glyph layer: phosphor glow underneath, crisp glyphs on top.
        val glyphs = renderAsciiGlyphLayer(grid, w, h, palette)
        val glow = glyphs.boxBlurredCopy(ASCII_GLOW_BLUR_RADIUS, ASCII_BLUR_ROUNDS)
        val glowPaint = Paint().apply { alpha = (ASCII_GLOW_ALPHA * 255f).roundToInt() }
        canvas.drawBitmap(glow, 0f, 0f, glowPaint)
        canvas.drawBitmap(glyphs, 0f, 0f, null)
        backdropBitmapCache.put(cacheKey, composite)
        composite
    }
}

/**
 * Renders the precomputed [AsciiArtGrid] into a transparent bitmap with the
 * monospace geometry of the wave field ([ASCII_COL_STEP] advance,
 * [ASCII_ROW_STEP]-derived line height). The grid was built with matching row
 * density, so the artwork lands on the card undistorted — exactly like
 * ContentScale.Crop would place it.
 */
internal fun renderAsciiGlyphLayer(
    grid: AsciiArtGrid,
    w: Int,
    h: Int,
    palette: ShareThemePalette,
): Bitmap {
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val cellW = w.toFloat() / grid.columns
    val cellH = h.toFloat() / grid.rows
    val textSize = cellW / ASCII_COL_STEP
    val paint =
        Paint().apply {
            isAntiAlias = true
            typeface = Typeface.MONOSPACE
            this.textSize = textSize
        }
    val ramp = ASCII_RAMP
    // Baseline that visually centres each glyph in its cell.
    var y = cellH * 0.5f + textSize * 0.38f
    var row = 0
    while (row < grid.rows && y - textSize < h) {
        var x = cellW * 0.14f
        var column = 0
        while (column < grid.columns && x < w) {
            val index = row * grid.columns + column
            val rampIndex = grid.rampIndices[index]
            if (rampIndex > 0) {
                val b = grid.brightness[index]
                // High visibility floor: every cell keeps its colour so
                // dark covers still read as a picture, not a void.
                val alpha =
                    ((0.42f + 0.58f * b) * palette.decorationAlpha)
                        .coerceIn(0f, 1f)
                paint.color = (grid.colors[index] and 0x00FFFFFF) or
                    ((alpha * 255f).toInt().coerceIn(0, 255) shl 24)
                canvas.drawText(ramp[rampIndex].toString(), x, y, paint)
            }
            x += cellW
            column++
        }
        y += cellH
        row++
    }
    return bitmap
}

/** Multiplies every pixel's RGB by [factor]; alpha is kept. */
internal fun Bitmap.dimmed(factor: Float): Bitmap {
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    for (i in pixels.indices) {
        val c = pixels[i]
        val r = ((c shr 16 and 0xFF) * factor).toInt().coerceIn(0, 255)
        val g = ((c shr 8 and 0xFF) * factor).toInt().coerceIn(0, 255)
        val b = ((c and 0xFF) * factor).toInt().coerceIn(0, 255)
        pixels[i] = (c and 0xFF000000.toInt()) or (r shl 16) or (g shl 8) or b
    }
    val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    out.setPixels(pixels, 0, width, 0, 0, width, height)
    return out
}

/** Gaussian-approximating box blur (via [blurBoxPass]) as a new bitmap. */
internal fun Bitmap.boxBlurredCopy(
    radius: Int,
    rounds: Int,
): Bitmap {
    var pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    repeat(rounds) {
        pixels = blurBoxPass(pixels, width, height, radius, horizontal = true)
        pixels = blurBoxPass(pixels, width, height, radius, horizontal = false)
    }
    val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    out.setPixels(pixels, 0, width, 0, 0, width, height)
    return out
}

/**
 * The liquid-glass pools, kept as the fluted glass loading/missing-art state:
 * iridescent blobs and sheen streaks on the smoked pane.
 */
internal fun DrawScope.drawGlassFluidPools(palette: ShareThemePalette) {
    val w = size.width
    val h = size.height
    val colors = listOf(palette.glowTop, palette.glowBottom, palette.accent)

    // Vertical fluted glass ribs (cylindrical architectural glass pane texture)
    val stripeCount = 36
    val stripeW = w / stripeCount
    for (i in 0 until stripeCount) {
        val x = i * stripeW
        // Soft cylindrical shadow seam on left
        drawLine(
            color = Color.Black.copy(alpha = 0.16f * palette.decorationAlpha),
            start = Offset(x, 0f),
            end = Offset(x, h),
            strokeWidth = 1.dp.toPx(),
        )
        // Specular highlight seam on right
        drawLine(
            color = Color.White.copy(alpha = 0.10f * palette.decorationAlpha),
            start = Offset(x + stripeW, 0f),
            end = Offset(x + stripeW, h),
            strokeWidth = 0.8.dp.toPx(),
        )
    }

    // Iridescent fluid pools: elongated, tilted radial blobs pooling
    // like molten glass inside a dark smoked pane.
    GlassFluidBlobs.forEach { blob ->
        val color = colors[blob.colorIndex % colors.size]
        val center = Offset(blob.cx * w, blob.cy * h)
        val radius = maxOf(blob.rx, blob.ry) * w
        rotate(degrees = blob.rot, pivot = center) {
            drawOval(
                brush =
                    Brush.radialGradient(
                        colors =
                            listOf(
                                color.copy(alpha = blob.alpha * palette.decorationAlpha),
                                color.copy(alpha = blob.alpha * 0.45f * palette.decorationAlpha),
                                Color.Transparent,
                            ),
                        center = center,
                        radius = radius,
                    ),
                topLeft = Offset(center.x - blob.rx * w, center.y - blob.ry * w),
                size = Size(blob.rx * 2 * w, blob.ry * 2 * w),
            )
        }
    }
    // Diagonal sheen streaks — the specular highlights that sell
    // the pane-of-glass read.
    rotate(degrees = 26f, pivot = Offset(w * 0.5f, h * 0.5f)) {
        drawRect(
            brush =
                Brush.verticalGradient(
                    colors =
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.10f * palette.decorationAlpha),
                            Color.Transparent,
                        ),
                ),
            topLeft = Offset(w * 0.16f, -h * 0.45f),
            size = Size(w * 0.10f, h * 1.9f),
        )
        drawRect(
            brush =
                Brush.verticalGradient(
                    colors =
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.06f * palette.decorationAlpha),
                            Color.Transparent,
                        ),
                ),
            topLeft = Offset(w * 0.62f, -h * 0.45f),
            size = Size(w * 0.045f, h * 1.9f),
        )
    }
    // Gloss along the top edge, like light catching the rim.
    drawRect(
        brush =
            Brush.verticalGradient(
                colors =
                    listOf(
                        Color.White.copy(alpha = 0.10f * palette.decorationAlpha),
                        Color.Transparent,
                    ),
                startY = 0f,
                endY = h * 0.12f,
            ),
    )
}

/** ContentScale.Crop equivalent: centre-crops the bitmap to [aspect] (w/h). */
internal fun Bitmap.centerCroppedToAspect(aspect: Float): Bitmap {
    val srcAspect = width.toFloat() / height
    val cropW: Int
    val cropH: Int
    if (srcAspect > aspect) {
        cropH = height
        cropW = (height * aspect).roundToInt().coerceAtMost(width)
    } else {
        cropW = width
        cropH = (width / aspect).roundToInt().coerceAtMost(height)
    }
    val left = (width - cropW) / 2
    val top = (height - cropH) / 2
    return Bitmap.createBitmap(this, left, top, cropW.coerceAtLeast(1), cropH.coerceAtLeast(1))
}

/**
 * High-quality area-average downscale to one pixel per glyph cell: successive
 * halvings keep the cells faithful to the artwork — a single bilinear step
 * this deep would alias most of the detail away.
 */
internal fun Bitmap.downscaledTo(
    targetW: Int,
    targetH: Int,
): Bitmap {
    var current = this
    while (current.width > targetW * 2 && current.height > targetH * 2) {
        current =
            Bitmap.createScaledBitmap(
                current,
                (current.width / 2).coerceAtLeast(targetW),
                (current.height / 2).coerceAtLeast(targetH),
                true,
            )
    }
    return Bitmap.createScaledBitmap(current, targetW, targetH, true)
}

/** Linear blend of [color] toward white by [fraction] (0..1). */
internal fun mixTowardWhite(
    color: Int,
    fraction: Float,
): Int {
    val f = fraction.coerceIn(0f, 1f)
    val r = (color shr 16) and 0xFF
    val g = (color shr 8) and 0xFF
    val b = color and 0xFF
    val nr = (r + (255 - r) * f).toInt().coerceIn(0, 255)
    val ng = (g + (255 - g) * f).toInt().coerceIn(0, 255)
    val nb = (b + (255 - b) * f).toInt().coerceIn(0, 255)
    return (0xFF shl 24) or (nr shl 16) or (ng shl 8) or nb
}

/**
 * Deterministic wave-shaded monospace glyph field: the [ASCII_FIELD] look,
 * and the no-artwork fallback for [ASCII_ARTWORK]. Brightness is shaped by
 * layered sine waves so the field reads as flowing terrain rather than
 * uniform static, and being a pure function of position keeps the live
 * preview and the share capture pixel-identical.
 */
internal fun DrawScope.drawAsciiWaveField(palette: ShareThemePalette) {
    val w = size.width
    val h = size.height
    // Soft phosphor halo behind the glyph field.
    drawCircle(
        brush =
            Brush.radialGradient(
                colors =
                    listOf(
                        palette.glowTop.copy(alpha = 0.12f * palette.decorationAlpha),
                        Color.Transparent,
                    ),
                center = Offset(w * 0.5f, h * 0.30f),
                radius = w * 0.85f,
            ),
        radius = w * 0.85f,
        center = Offset(w * 0.5f, h * 0.30f),
    )
    val paint =
        Paint().apply {
            isAntiAlias = true
            typeface = Typeface.MONOSPACE
            textSize = w * 0.030f
        }
    val ramp = " .,:;=+*#%@"
    val cell = paint.textSize
    drawIntoCanvas { canvas ->
        var y = cell * 1.15f
        while (y < h) {
            var x = cell * 0.30f
            while (x < w) {
                val nx = x / w
                val ny = y / h
                val wave =
                    sin(nx * 9.4f + sin(ny * 7.3f) * 2.1f) *
                        cos(ny * 6.1f - sin(nx * 4.2f) * 1.7f)
                val b = ((wave + 1f) * 0.5f).coerceIn(0f, 1f)
                val glyph = ramp[(b * (ramp.length - 1)).toInt()]
                if (glyph != ' ') {
                    paint.color =
                        if (b > 0.72f) {
                            palette.accent
                                .copy(alpha = (0.20f + (b - 0.72f) * 1.1f).coerceAtMost(0.55f) * palette.decorationAlpha)
                                .toArgb()
                        } else {
                            Color.White
                                .copy(alpha = (0.05f + b * 0.13f) * palette.decorationAlpha)
                                .toArgb()
                        }
                    canvas.nativeCanvas.drawText(glyph.toString(), x, y, paint)
                }
                x += cell * ASCII_COL_STEP
            }
            y += cell * ASCII_ROW_STEP
        }
    }
}

/**
 * Radial decoration inside a fixed-size circle: fades from [color] at its
 * centre to transparent (or to [edge] when given).
 */
@Composable
internal fun GlowOrb(
    scope: BoxScope,
    alignment: Alignment,
    x: Dp,
    y: Dp,
    size: Dp,
    color: Color,
    edge: Color = Color.Transparent,
) {
    with(scope) {
        Box(
            modifier =
                Modifier
                    .align(alignment)
                    .offset(x = x, y = y)
                    .size(size)
                    .background(
                        brush = Brush.radialGradient(colors = listOf(color, edge)),
                        shape = CircleShape,
                    ),
        )
    }
}

// Scene geometry — fixed layouts so the backdrop is identical on every render
// and in the share capture. Coordinates are fractions of card width/height.

private data class GlassFluidBlob(
    val cx: Float,
    val cy: Float,
    val rx: Float,
    val ry: Float,
    val rot: Float,
    val colorIndex: Int,
    val alpha: Float,
)

private val GlassFluidBlobs =
    listOf(
        GlassFluidBlob(0.28f, 0.14f, 0.55f, 0.30f, -24f, 0, 0.34f),
        GlassFluidBlob(0.86f, 0.32f, 0.45f, 0.26f, 18f, 1, 0.30f),
        GlassFluidBlob(0.10f, 0.54f, 0.50f, 0.24f, 30f, 2, 0.26f),
        GlassFluidBlob(0.70f, 0.80f, 0.60f, 0.28f, -14f, 1, 0.28f),
        GlassFluidBlob(0.38f, 0.96f, 0.45f, 0.22f, 10f, 0, 0.24f),
        GlassFluidBlob(0.52f, 0.42f, 0.35f, 0.18f, -32f, 1, 0.16f),
    )

/**
 * Text color for rank badges, pedestal numbers, and swatch selection dots.
 * Pure luminance pick: bright chips (gold/silver metals on dark themes) get
 * black text, dark chips (light-theme accents) get white. Threshold 0.5 is
 * the crossover that keeps the original black-on-metal look while making
 * dark chips on the Daylight theme readable.
 */
internal fun ShareThemePalette.contrastingText(chip: Color): Color =
    if (0.299f * chip.red + 0.587f * chip.green + 0.114f * chip.blue > 0.5f) Color.Black else Color.White

/**
 * Circular gradient swatch used by share dialogs to pick a [ShareTheme].
 */
@Composable
fun ThemeSwatch(
    theme: ShareTheme,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = theme.palette
    Box(
        modifier =
            modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(palette.gradient))
                .clickable(onClick = onClick)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) Color.White else Color.White.copy(alpha = 0.15f),
                    shape = CircleShape,
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(modifier = Modifier.size(6.dp).background(palette.contrastingText(palette.gradient.first()), CircleShape))
        }
    }
}
