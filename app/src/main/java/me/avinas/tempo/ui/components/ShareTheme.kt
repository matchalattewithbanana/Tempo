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
internal val backdropBitmapCache = LruCache<String, Bitmap>(8)

/**
 * Backdrop construction. Each style builds its own scene from different
 * geometry rather than swapping the hue of one backdrop: [PHOTO_GLOW] builds on
 * the blurred artwork, [FLUTED_GLASS] refracts it through a vertical ribbed
 * pane, and [ASCII_ARTWORK] goes further, converting the artwork itself into
 * true ASCII art and painting that as the backdrop.
 *
 * Everything is drawn with plain brushes/paths on Canvas so it renders
 * identically on-screen and in the software-canvas share capture.
 */
enum class ShareBackdropStyle {
    /** Blurred artwork backdrop with ambient orbs bleeding from two corners. */
    PHOTO_GLOW,

    /** Fluted glass: the artwork refracted through a vertical ribbed pane. */
    FLUTED_GLASS,

    /** Terminal ASCII: a monospace glyph field shaded by layered waves. */
    ASCII_FIELD,

    /** Album art rendered as true ASCII art — brightness-mapped ramp glyphs
     *  tinted with the artwork's own colors on a terminal-dark field. */
    ASCII_ARTWORK,

    /** Editorial: flat field, hard vignette, two thin offset ring outlines. */
    RINGS_VIGNETTE,

    /** Morning paper: a visible sun disc with halo sinking in from above. */
    SUN_WASH,

    /** Grainy gradient: warm mesh blobs over the base gradient with film grain. */
    GRAIN_GRADIENT,
}

/**
 * Shared theme system for all share cards (stats, song details, artist
 * details). A theme declares color tone, backdrop construction, shape
 * language, and contrast rules; every card derives text, surfaces, badges,
 * and decorations from it so stats stay readable on dark and light backdrops.
 */
data class ShareThemePalette(
    val gradient: List<Color>,
    val overlay: List<Color>,
    val accent: Color,
    val glowTop: Color,
    val glowBottom: Color,
    val rank1Tint: Color,
    val isDark: Boolean = true,
    // Backdrop identity — which scene the theme builds, how strongly, and
    // whether the blurred artwork is the backdrop source at all. Themes with
    // usesArtwork = false ignore the item art and draw their own background.
    val backdrop: ShareBackdropStyle = ShareBackdropStyle.PHOTO_GLOW,
    val decorationAlpha: Float = 0.15f,
    val usesArtwork: Boolean = false,
    // Shape language — surfaces, thumbnails, and rank badges follow the theme;
    // winner rings (hero/podium avatars) stay circular by design.
    val cardShape: RoundedCornerShape = RoundedCornerShape(20.dp),
    val thumbShape: RoundedCornerShape = RoundedCornerShape(8.dp),
    val badgeShape: Shape = CircleShape,
    // Layout text system — lets a theme restyle the typography of every card,
    // not just its backdrop. Null means "use the card's default": only themes
    // that opt in (e.g. MINIMUM, with light headlines and thin, widely
    // tracked labels) change the type, so the other themes stay pixel-identical.
    val headlineWeight: FontWeight? = null,
    val labelWeight: FontWeight? = null,
    val labelTracking: TextUnit? = null,
) {
    // Tone-derived slots so every layout stays readable on light and dark themes.
    val textPrimary: Color get() = if (isDark) Color.White else Color(0xFF241C10)
    val textSecondary: Color get() = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF241C10).copy(alpha = 0.62f)

    // High-emphasis text; also used where semantic colors (fan badges, stat
    // icons) would lack contrast on a light backdrop.
    val textStrong: Color get() = if (isDark) Color.White else Color(0xFF241C10)
    val surface: Color get() = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
    val surfaceStrong: Color get() = if (isDark) Color.Black.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.5f)
    val divider: Color get() = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.12f)
    val cellBackground: Color get() = if (isDark) Color(0xFF1A1A1A) else Color(0xFFF0E6D2)
    val cellPlaceholder: Color get() = if (isDark) Color(0xFF2A2A2A) else Color(0xFFE6D7BC)
    val branding: Color get() = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF241C10).copy(alpha = 0.55f)
    val heroGlow: Color get() = if (isDark) Color.White.copy(alpha = 0.25f) else Color(0xFFB45309).copy(alpha = 0.2f)
}

enum class ShareTheme(
    val palette: ShareThemePalette,
) {
    // The photo theme: blurred artwork under corner glow orbs. The only theme
    // whose backdrop is the listener's own art as a plain blurred photo.
    MIDNIGHT(
        ShareThemePalette(
            gradient = listOf(TempoDarkBackground, Color(0xFF1E1B4B), Color(0xFF312E81)),
            overlay =
                listOf(
                    Color.Black.copy(alpha = 0.55f),
                    Color(0xFF0F0F12).copy(alpha = 0.8f),
                    Color(0xFF0D0D10).copy(alpha = 0.95f),
                ),
            accent = Color(0xFFFBBF24),
            glowTop = Color(0xFFA855F7),
            glowBottom = Color(0xFFEC4899),
            rank1Tint = Color(0xFFF59E0B),
            backdrop = ShareBackdropStyle.PHOTO_GLOW,
            decorationAlpha = 0.4f,
            usesArtwork = true,
        ),
    ),

    // Fluted glass: the listener's own artwork refracted through vertical
    // glass ribs — the cover is converted into a fluted-glass texture (prism
    // bends, rib shadows, seam highlights) that becomes the card's backdrop,
    // under a smoked overlay that keeps the glass mood and text readable.
    GLASS(
        ShareThemePalette(
            gradient = listOf(Color(0xFF0B0D14), Color(0xFF1A1330), Color(0xFF0F0B18)),
            overlay =
                listOf(
                    Color.Black.copy(alpha = 0.38f),
                    Color(0xFF0F0C1A).copy(alpha = 0.56f),
                    Color(0xFF0A0812).copy(alpha = 0.84f),
                ),
            accent = Color(0xFFBAE6FD),
            glowTop = Color(0xFF67E8F9),
            glowBottom = Color(0xFFE879F9),
            rank1Tint = Color(0xFF7DD3FC),
            backdrop = ShareBackdropStyle.FLUTED_GLASS,
            decorationAlpha = 1f,
            usesArtwork = true,
            cardShape = RoundedCornerShape(24.dp),
            thumbShape = RoundedCornerShape(14.dp),
        ),
    ),

    // Terminal: the #1 item's cover art converted into true ASCII art
    // (brightness-mapped ramp glyphs tinted with the artwork's own colors)
    // on near-black, sharp corners, phosphor-green UI accents.
    ASCII(
        ShareThemePalette(
            gradient = listOf(Color(0xFF040704), Color(0xFF08120A), Color(0xFF050A06)),
            overlay =
                listOf(
                    Color.Black.copy(alpha = 0.30f),
                    Color.Black.copy(alpha = 0.42f),
                    Color(0xFF020503).copy(alpha = 0.75f),
                ),
            accent = Color(0xFF4ADE80),
            glowTop = Color(0xFF22C55E),
            glowBottom = Color(0xFF86EFAC),
            rank1Tint = Color(0xFF4ADE80),
            backdrop = ShareBackdropStyle.ASCII_ARTWORK,
            decorationAlpha = 1f,
            usesArtwork = true,
            cardShape = RoundedCornerShape(4.dp),
            thumbShape = RoundedCornerShape(4.dp),
            badgeShape = RoundedCornerShape(4.dp),
        ),
    ),

    // Minimum: the editorial monochrome field, extended into the type system —
    // light headlines and thin, widely tracked labels instead of heavy black.
    MINIMUM(
        ShareThemePalette(
            gradient = listOf(Color(0xFF161616), Color(0xFF0E0E0E), Color(0xFF1A1A1A)),
            overlay =
                listOf(
                    Color.Black.copy(alpha = 0.5f),
                    Color(0xFF111111).copy(alpha = 0.8f),
                    Color(0xFF0A0A0A).copy(alpha = 0.95f),
                ),
            accent = Color(0xFFE5E7EB),
            glowTop = Color(0xFF9CA3AF),
            glowBottom = Color(0xFFD1D5DB),
            rank1Tint = Color(0xFFE5E7EB),
            backdrop = ShareBackdropStyle.RINGS_VIGNETTE,
            decorationAlpha = 1f,
            cardShape = RoundedCornerShape(2.dp),
            thumbShape = RoundedCornerShape(2.dp),
            badgeShape = RoundedCornerShape(3.dp),
            headlineWeight = FontWeight.Light,
            labelWeight = FontWeight.Normal,
            labelTracking = 2.4.sp,
        ),
    ),

    // Morning paper: warm cream under a visible sun disc.
    DAYLIGHT(
        ShareThemePalette(
            gradient = listOf(Color(0xFFFDF6EC), Color(0xFFFDE68A), Color(0xFFFDBA74)),
            overlay =
                listOf(
                    Color.White.copy(alpha = 0.62f),
                    Color(0xFFFFFBEB).copy(alpha = 0.85f),
                    Color(0xFFFFF7ED).copy(alpha = 0.95f),
                ),
            accent = Color(0xFFB45309),
            glowTop = Color(0xFFF59E0B),
            glowBottom = Color(0xFFFB923C),
            rank1Tint = Color(0xFFC2410C),
            isDark = false,
            backdrop = ShareBackdropStyle.SUN_WASH,
            decorationAlpha = 1f,
            cardShape = RoundedCornerShape(12.dp),
            thumbShape = RoundedCornerShape(6.dp),
        ),
    ),

    // Grain: warm ember mesh with film grain — the only theme that ignores
    // the artwork entirely, so it reads distinct from every art-based theme.
    GRAIN(
        ShareThemePalette(
            gradient = listOf(Color(0xFF1B0B1E), Color(0xFF4A1230), Color(0xFF0D0A12)),
            overlay =
                listOf(
                    Color.Black.copy(alpha = 0.22f),
                    Color(0xFF1B0B1E).copy(alpha = 0.42f),
                    Color(0xFF0D0A12).copy(alpha = 0.68f),
                ),
            accent = Color(0xFFFBBF24),
            glowTop = Color(0xFFFB7185),
            glowBottom = Color(0xFFF59E0B),
            rank1Tint = Color(0xFFFBBF24),
            backdrop = ShareBackdropStyle.GRAIN_GRADIENT,
            decorationAlpha = 1f,
            cardShape = RoundedCornerShape(16.dp),
            thumbShape = RoundedCornerShape(10.dp),
            badgeShape = RoundedCornerShape(8.dp),
        ),
    ),
}

/**
 * The shared blur base every artwork-backed theme sits on.
 *
 * A single heavy, slightly over-scaled blurred cover: a creamier 64.dp bake so
 * no harsh recognizable edges survive, 1.1x zoom so blur fringes never show the
 * gradient at the card edge. MIDNIGHT uses it as the final background under its
 * glow orbs; ASCII_ARTWORK and FLUTED_GLASS paint their effect (glyph art /
 * refracted glass) over it. Readability scrims stay per-backdrop
 * ([ShareThemePalette.overlay]) so text contrast is untouched.
 */
internal val ShareBlurRadius = 64.dp

@Composable
internal fun ShareBlurBase(
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    if (imageUrl.isNullOrBlank()) return
    Box(modifier = modifier.fillMaxSize().clipToBounds()) {
        CachedAsyncImage(
            imageUrl = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = 1.1f, scaleY = 1.1f),
            targetSizeDp = 240,
            allowHardware = false,
            blurRadius = ShareBlurRadius,
        )
    }
}

/**
 * Builds the theme's backdrop scene. Placed above the base gradient (and the
 * shared blurred artwork base when the theme uses artwork) and below the card
 * content. Clipped to its own bounds so light never leaks past the card edge
 * in preview dialogs.
 */
@Composable
fun ShareThemeDecorations(
    palette: ShareThemePalette,
    imageUrl: String? = null,
    backdropBitmap: Bitmap? = null,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().clipToBounds()) {
        when (palette.backdrop) {
            ShareBackdropStyle.PHOTO_GLOW -> {
                GlowOrb(
                    this,
                    Alignment.TopEnd,
                    x = 50.dp,
                    y = (-50).dp,
                    size = 300.dp,
                    color = palette.glowTop.copy(alpha = palette.decorationAlpha),
                )
                GlowOrb(
                    this,
                    Alignment.BottomStart,
                    x = (-50).dp,
                    y = 50.dp,
                    size = 300.dp,
                    color = palette.glowBottom.copy(alpha = palette.decorationAlpha),
                )
            }

            ShareBackdropStyle.ASCII_FIELD -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawAsciiWaveField(palette)
                }
            }

            // Fluted glass: the artwork refracted into a vertical ribbed pane.
            // The fluid pools keep the smoked pane alive while the artwork
            // decodes — or when it is missing entirely. Never a plain blurred
            // photo: the glass scene owns the backdrop from the first frame so
            // it never reads as MIDNIGHT with an overlay on top.
            ShareBackdropStyle.FLUTED_GLASS -> {
                if (imageUrl.isNullOrBlank() && backdropBitmap == null) {
                    Canvas(modifier = Modifier.fillMaxSize()) { drawGlassFluidPools(palette) }
                } else {
                    FlutedGlassBackdrop(
                        palette = palette,
                        imageUrl = imageUrl,
                        sourceBitmap = backdropBitmap,
                    )
                }
            }

            // True ASCII art of the cover; the wave field keeps the card alive
            // while the artwork decodes — or when it is missing entirely.
            ShareBackdropStyle.ASCII_ARTWORK -> {
                if (imageUrl.isNullOrBlank()) {
                    Canvas(modifier = Modifier.fillMaxSize()) { drawAsciiWaveField(palette) }
                } else {
                    AsciiArtworkBackdrop(palette = palette, imageUrl = imageUrl)
                }
            }

            ShareBackdropStyle.RINGS_VIGNETTE -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    // Hard vignette pulling the corners to black.
                    drawRect(
                        brush =
                            Brush.radialGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f * palette.decorationAlpha)),
                                center = center,
                                radius = maxOf(w, h) * 0.72f,
                            ),
                    )
                    // Two thin offset rings — the editorial mark.
                    drawCircle(
                        color = Color.White.copy(alpha = 0.10f * palette.decorationAlpha),
                        radius = w * 0.42f,
                        center = Offset(w * 0.84f, h * 0.14f),
                        style = Stroke(width = w * 0.004f),
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.06f * palette.decorationAlpha),
                        radius = w * 0.30f,
                        center = Offset(w * 0.08f, h * 0.92f),
                        style = Stroke(width = w * 0.003f),
                    )
                }
            }

            ShareBackdropStyle.SUN_WASH -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val sunCenter = Offset(w * 0.78f, h * 0.10f)
                    // Halo.
                    drawCircle(
                        brush =
                            Brush.radialGradient(
                                colors =
                                    listOf(
                                        Color(0xFFFFF7E0).copy(alpha = 0.75f * palette.decorationAlpha),
                                        palette.glowTop.copy(alpha = 0.30f * palette.decorationAlpha),
                                        Color.Transparent,
                                    ),
                                center = sunCenter,
                                radius = w * 0.62f,
                            ),
                        radius = w * 0.62f,
                        center = sunCenter,
                    )
                    // Warmth pooling at the bottom edge.
                    drawCircle(
                        brush =
                            Brush.radialGradient(
                                colors = listOf(palette.glowBottom.copy(alpha = 0.22f * palette.decorationAlpha), Color.Transparent),
                                center = Offset(w * 0.1f, h * 1.02f),
                                radius = w * 0.55f,
                            ),
                        radius = w * 0.55f,
                        center = Offset(w * 0.1f, h * 1.02f),
                    )
                }
            }

            ShareBackdropStyle.GRAIN_GRADIENT -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawGrainyGradient(palette)
                }
            }
        }
        // Premium finish: a light film-grain + cool top sheen over every scene
        // except GRAIN (which already carries its own heavy grain). This lifts
        // flat gradients and blurred photos away from the cheap digital-blur
        // look and gives all themes a shared frosted texture.
        if (palette.backdrop != ShareBackdropStyle.GRAIN_GRADIENT) {
            Canvas(modifier = Modifier.fillMaxSize()) { drawShareFinish(palette) }
        }
    }
}

// ---------------------------------------------------------------------------
// ASCII_ARTWORK backdrop — the cover art converted into true ASCII art,
// learned from ascii-magic's styles: the brightness-mapped ramp of
// "Characters", the mixed letter/punctuation families of "Mixed Glyphs", and
// the ordered-dither texture of "Dither", on top of a per-artwork dynamic
// tone mapping so every cover — dark, hazy or blown-out — fills the ramp.
// ---------------------------------------------------------------------------

/** Glyph advance as a fraction of the monospace text size. */
internal const val ASCII_COL_STEP = 0.72f

/** Line height as a fraction of the monospace text size. */
internal const val ASCII_ROW_STEP = 1.25f

/** Horizontal resolution of the ASCII conversion, in glyph columns. */
internal const val ASCII_COLUMNS = 84

/**
 * Mixed-family density ramp (ascii-magic "Mixed Glyphs"): punctuation flows
 * into letters and symbols from sparse to dense, giving portraits and
 * high-detail covers richer texture than a pure punctuation ramp.
 */
internal const val ASCII_RAMP = " .,:;=+*coxm#%@"

/**
 * Ordered-dither strength (ascii-magic "Dither"): how far the Bayer noise may
 * swing a cell's brightness before ramp quantization, in 0..1 units.
 */
internal const val ASCII_DITHER = 0.35f

/** Bayer 4x4 ordered-dither matrix, values 0..15. */
internal val ASCII_BAYER_4X4 =
    arrayOf(
        intArrayOf(0, 8, 2, 10),
        intArrayOf(12, 4, 14, 6),
        intArrayOf(3, 11, 1, 9),
        intArrayOf(15, 7, 13, 5),
    )

/**
 * Precomputed ASCII conversion of one artwork. All the per-pixel work happens
 * once at build time; the draw pass only scales and paints, so the live
 * preview and the software-canvas share capture render the identical
 * composition.
 */
internal class AsciiArtGrid(
    val columns: Int,
    val rows: Int,
    /** Ramp index per cell; 0 (space) marks an empty cell. */
    val rampIndices: IntArray,
    /** Per-cell brightness after the contrast curve, 0..1 — drives alpha. */
    val brightness: FloatArray,
    /** Per-cell glyph colour (opaque ARGB), sampled from the artwork. */
    val colors: IntArray,
)

/**
 * Renders [imageUrl]'s artwork as ASCII art. The conversion runs once per
 * (url, aspect) on a background dispatcher; until it lands the deterministic
 * wave field keeps the card alive over the shared blur base. Stack is shared
 * blur + glyph effect + readability overlay + finish grain.
 */
@Composable
private fun AsciiArtworkBackdrop(
    palette: ShareThemePalette,
    imageUrl: String,
) {
    // 9:16 until the first layout reports the real card aspect; the backdrop is
    // rebuilt only if the aspect actually differs.
    var aspect by remember { mutableStateOf(9f / 16f) }
    val context = LocalContext.current
    val backdrop by produceState<Bitmap?>(initialValue = null, imageUrl, aspect) {
        value =
            runCatching {
                buildAsciiArtBackdrop(context, imageUrl, aspect)
            }.getOrNull()
    }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    if (size.width > 0 && size.height > 0) {
                        val measured = size.width.toFloat() / size.height
                        if (measured != aspect) aspect = measured
                    }
                }.clipToBounds(),
    ) {
        // Shared blur base under the glyphs — one blur system, effect on top.
        ShareBlurBase(imageUrl = imageUrl)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val art = backdrop
            if (art != null) {
                // The bitmap is pre-cropped to the card aspect, so a full-size
                // destination is a 1:1 map.
                drawImage(
                    image = art.asImageBitmap(),
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                    filterQuality = FilterQuality.Medium,
                )
            } else {
                drawAsciiWaveField(palette)
            }
        }
        // Readability overlay above the glyph art.
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(brush = Brush.verticalGradient(palette.overlay)),
        )
    }
}

/**
 * Fluted-glass backdrop: the listener's artwork refracted through a vertical
 * ribbed pane — the paper.design fluted-glass model, tuned for background use
 * behind card content. The effect is computed on the CPU into a bitmap (prism
 * refraction per rib, rib shadows, seam highlight strokes, one-directional
 * soften) so it renders identically on-screen and in the software-canvas
 * share capture. Stack is shared blur + refracted glass + readability overlay +
 * finish grain; the fluid pools stand in while the artwork decodes.
 */
@Composable
private fun FlutedGlassBackdrop(
    palette: ShareThemePalette,
    imageUrl: String? = null,
    sourceBitmap: Bitmap? = null,
) {
    // 9:16 until the first layout reports the real card aspect; the texture is
    // rebuilt only if the aspect actually differs.
    var aspect by remember { mutableStateOf(9f / 16f) }
    val context = LocalContext.current
    val fluted by produceState<Bitmap?>(initialValue = null, imageUrl, sourceBitmap, aspect) {
        value =
            withContext(Dispatchers.Default) {
                runCatching {
                    if (sourceBitmap != null) {
                        val safeSource =
                            if (sourceBitmap.config == Bitmap.Config.HARDWARE) {
                                sourceBitmap.copy(Bitmap.Config.ARGB_8888, false)
                            } else {
                                sourceBitmap
                            }
                        val cropped = safeSource.centerCroppedToAspect(aspect)
                        val targetH =
                            (FLUTED_RENDER_WIDTH / aspect)
                                .roundToInt()
                                .coerceIn(64, MAX_SAFE_BITMAP_EDGE_PX)
                        cropped.downscaledTo(FLUTED_RENDER_WIDTH, targetH).renderFlutedGlass()
                    } else if (!imageUrl.isNullOrBlank()) {
                        buildFlutedGlassBitmap(context, imageUrl, aspect)
                    } else {
                        null
                    }
                }.getOrNull()
            }
    }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    if (size.width > 0 && size.height > 0) {
                        val measured = size.width.toFloat() / size.height
                        if (measured != aspect) aspect = measured
                    }
                }.clipToBounds(),
    ) {
        // Shared blur base under the glass — one blur system, effect on top.
        // No-op when only a badge sourceBitmap is provided (no artwork URL).
        ShareBlurBase(imageUrl = imageUrl)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val art = fluted
            if (art != null) {
                // The bitmap is pre-cropped to the card aspect, so a full-size
                // destination is a 1:1 map.
                drawImage(
                    image = art.asImageBitmap(),
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                    filterQuality = FilterQuality.Medium,
                )
            } else {
                drawGlassFluidPools(palette)
            }
        }
        // Readability overlay above the glass.
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(brush = Brush.verticalGradient(palette.overlay)),
        )
    }
}

/**
 * Loads the artwork through Coil (sharing the app's memory/disk cache) and
 * converts it into a fluted-glass texture at the card's aspect ratio.
 */
private suspend fun buildFlutedGlassBitmap(
    context: Context,
    imageUrl: String,
    aspect: Float,
): Bitmap? {
    val cacheKey = "fluted:$imageUrl:$aspect"
    backdropBitmapCache.get(cacheKey)?.let { if (!it.isRecycled) return it }
    val request =
        ImageRequest
            .Builder(context)
            .data(MusicBrainzEnrichmentService.fixHttpUrl(imageUrl))
            .size(720, 720)
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
        val cropped = safeSource.centerCroppedToAspect(aspect)
        // Height follows the card's measured aspect, so it is bounded here: a very tall or
        // degenerate aspect must not drive Bitmap.createBitmap past the painter ceiling.
        val targetH =
            (FLUTED_RENDER_WIDTH / aspect).roundToInt().coerceIn(64, MAX_SAFE_BITMAP_EDGE_PX)
        val result = cropped.downscaledTo(FLUTED_RENDER_WIDTH, targetH).renderFlutedGlass()
        backdropBitmapCache.put(cacheKey, result)
        result
    }
}

/**
 * CPU fluted-glass pass (the paper.design model): soften the art into a glassy
 * base, refract it rib by rib, then shade the rods.
 */
private fun Bitmap.renderFlutedGlass(): Bitmap {
    val w = width
    val h = height
    var src = IntArray(w * h)
    getPixels(src, 0, w, 0, 0, w, h)
    // Stage 1 — soften the art into a glassy base (two h/v box rounds ≈ Gaussian).
    src = blurBoxPass(src, w, h, FLUTED_SOFTEN_RADIUS, horizontal = true)
    src = blurBoxPass(src, w, h, FLUTED_SOFTEN_RADIUS, horizontal = false)
    src = blurBoxPass(src, w, h, FLUTED_SOFTEN_RADIUS, horizontal = true)
    src = blurBoxPass(src, w, h, FLUTED_SOFTEN_RADIUS, horizontal = false)
    val out = IntArray(w * h)
    val stripeW = w.toFloat() / FLUTED_STRIPE_COUNT
    val compressW = stripeW * FLUTED_COMPRESS
    val shiftPx = stripeW * FLUTED_SHIFT
    val smearPx = stripeW * FLUTED_BLUR
    for (y in 0 until h) {
        val row = y * w
        for (x in 0 until w) {
            val s = x / stripeW
            // Rib-local coordinate u ∈ [-1, 1) from the rib centre.
            val rib = (s + 0.5f).toInt()
            val u = (s - rib) * 2f
            // Cylindrical-lens refraction: the rib compresses a wider window
            // of the art into itself, nudged sideways for the melt.
            val ownX = rib * stripeW + u * compressW + shiftPx
            // Neighbour window across the nearest seam; the crossfade keeps
            // sampling continuous so seams flow instead of snapping.
            val neighbourX =
                if (u >= 0f) {
                    (rib + 1) * stripeW + (u - 2f) * compressW + shiftPx
                } else {
                    (rib - 1) * stripeW + (u + 2f) * compressW + shiftPx
                }
            val fadeT =
                ((abs(u) - (1f - FLUTED_SEAM_FADE)) / FLUTED_SEAM_FADE)
                    .coerceIn(0f, 1f)
            val fade = fadeT * fadeT * (3f - 2f * fadeT)
            val sampleX = ownX * (1f - fade) + neighbourX * fade
            // Directional smear along the refraction axis.
            val c1 = sampleFlutedColumn(src, w, sampleX - smearPx, y)
            val c2 = sampleFlutedColumn(src, w, sampleX, y)
            val c3 = sampleFlutedColumn(src, w, sampleX + smearPx, y)
            var r = ((c1 shr 16 and 0xFF) + (c2 shr 16 and 0xFF) + (c3 shr 16 and 0xFF)) / 3
            var g = ((c1 shr 8 and 0xFF) + (c2 shr 8 and 0xFF) + (c3 shr 8 and 0xFF)) / 3
            var b = ((c1 and 0xFF) + (c2 and 0xFF) + (c3 and 0xFF)) / 3
            // Cylindrical rod shading, luminance-adaptive: glass only reads
            // where the art is bright, so smooth fields stay hairline-free.
            val edge = abs(u).coerceIn(0f, 1f)
            val lum = (r + g + b) / 765f
            val shade = 1f - FLUTED_SHADOW * edge.pow(1.2f) * lum
            val strokeT = ((edge - 0.86f) / 0.14f).coerceIn(0f, 1f)
            val stroke = FLUTED_HIGHLIGHT * strokeT * strokeT * lum
            r = (r * shade + 255f * stroke).toInt().coerceIn(0, 255)
            g = (g * shade + 255f * stroke).toInt().coerceIn(0, 255)
            b = (b * shade + 255f * stroke).toInt().coerceIn(0, 255)
            out[row + x] = 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
        }
    }
    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    result.setPixels(out, 0, w, 0, 0, w, h)
    return result
}

/**
 * One separable box-blur pass. Alternating horizontal/vertical rounds
 * approximate a Gaussian; the sliding-window sums keep the cost linear in
 * the pixel count regardless of radius.
 */
internal fun blurBoxPass(
    src: IntArray,
    w: Int,
    h: Int,
    radius: Int,
    horizontal: Boolean,
): IntArray {
    val out = IntArray(src.size)
    val lines = if (horizontal) h else w
    val len = if (horizontal) w else h
    val div = (radius * 2 + 1).toFloat()
    for (line in 0 until lines) {
        var asum = 0
        var rs = 0
        var gs = 0
        var bs = 0
        for (d in -radius..radius) {
            val c = src[blurIndex(line, d.coerceIn(0, len - 1), w, horizontal)]
            asum += (c ushr 24) and 0xFF
            rs += (c shr 16) and 0xFF
            gs += (c shr 8) and 0xFF
            bs += c and 0xFF
        }
        for (i in 0 until len) {
            val a = (asum / div + 0.5f).toInt().coerceIn(0, 255)
            val r = (rs / div + 0.5f).toInt().coerceIn(0, 255)
            val g = (gs / div + 0.5f).toInt().coerceIn(0, 255)
            val b = (bs / div + 0.5f).toInt().coerceIn(0, 255)
            out[blurIndex(line, i, w, horizontal)] =
                (a shl 24) or (r shl 16) or (g shl 8) or b
            val addC = src[blurIndex(line, (i + radius + 1).coerceIn(0, len - 1), w, horizontal)]
            val remC = src[blurIndex(line, (i - radius).coerceIn(0, len - 1), w, horizontal)]
            asum += ((addC ushr 24) and 0xFF) - ((remC ushr 24) and 0xFF)
            rs += ((addC shr 16) and 0xFF) - ((remC shr 16) and 0xFF)
            gs += ((addC shr 8) and 0xFF) - ((remC shr 8) and 0xFF)
            bs += (addC and 0xFF) - (remC and 0xFF)
        }
    }
    return out
}

private fun blurIndex(
    line: Int,
    i: Int,
    w: Int,
    horizontal: Boolean,
): Int = if (horizontal) line * w + i else i * w + line

/** Horizontal bilinear sample with edge clamping. */
private fun sampleFlutedColumn(
    src: IntArray,
    w: Int,
    xf: Float,
    y: Int,
): Int {
    val x0 = xf.toInt()
    val f = (xf - x0).coerceIn(0f, 1f)
    val xa = x0.coerceIn(0, w - 1)
    val xb = (x0 + 1).coerceIn(0, w - 1)
    val ca = src[y * w + xa]
    val cb = src[y * w + xb]
    val r = (ca shr 16 and 0xFF) * (1f - f) + (cb shr 16 and 0xFF) * f
    val g = (ca shr 8 and 0xFF) * (1f - f) + (cb shr 8 and 0xFF) * f
    val b = (ca and 0xFF) * (1f - f) + (cb and 0xFF) * f
    return (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
}

// Fluted-glass parameters, tuned for background use behind card content:
// rib count across the card width, the base softening baked into the art,
// the cylindrical-lens compression (window width in rib widths) and its
// sideways nudge, the directional smear, the seam crossfade width that
// keeps sampling continuous, and the luminance-adaptive rod shading.
internal const val FLUTED_RENDER_WIDTH = 576
internal const val FLUTED_STRIPE_COUNT = 26
internal const val FLUTED_SOFTEN_RADIUS = 10
internal const val FLUTED_COMPRESS = 1.9f
internal const val FLUTED_SHIFT = 0.35f
internal const val FLUTED_BLUR = 0.35f
internal const val FLUTED_SEAM_FADE = 0.35f
internal const val FLUTED_SHADOW = 0.12f
internal const val FLUTED_HIGHLIGHT = 0.10f

// Grainy-gradient backdrop seed: deterministic film grain so preview and capture match.
internal const val GRAIN_SEED = 0x6A17

/**
 * Grainy-gradient backdrop: warm mesh blobs over the base gradient with
 * deterministic film grain and a soft vignette. Pure Canvas (no artwork, no
 * CPU bitmaps) so preview and capture match, and cheap enough to redraw.
 */
