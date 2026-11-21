package me.avinas.tempo.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.avinas.tempo.R
import me.avinas.tempo.data.local.entities.Badge
import me.avinas.tempo.data.stats.ArtistDetails
import me.avinas.tempo.data.stats.GamificationEngine
import me.avinas.tempo.data.stats.TrackDetails
import me.avinas.tempo.ui.profile.BadgeEmblem
import me.avinas.tempo.ui.profile.getCategoryLabel
import me.avinas.tempo.ui.profile.getLevelTierAccent
import me.avinas.tempo.ui.profile.getRarityColor
import me.avinas.tempo.ui.profile.getUniqueBadgeColor
import me.avinas.tempo.ui.theme.DisplayFontFamily
import me.avinas.tempo.ui.theme.TempoDarkBackground
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Common background for share cards. Themed via [ShareThemePalette] so every
 * share surface (stats, song, artist) offers the same theme set and keeps
 * text readable on both dark and light backdrops.
 */
@Composable
fun ShareCardBackground(
    imageUrl: String? = null,
    backdropBitmap: Bitmap? = null,
    customBackdrop: (@Composable BoxScope.() -> Unit)? = null,
    palette: ShareThemePalette,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            modifier
                .background(brush = Brush.verticalGradient(palette.gradient)),
    ) {
        // Shared blur base or single custom canvas acting like image.
        // ASCII and FLUTED_GLASS skip this layer because their own backdrops
        // lay the same ShareBlurBase underneath their effect — still one blur
        // per card, never a double overlay.
        // On MINIMUM, DAYLIGHT and GRAIN (warm), they stay untouched as they have no relation with image.
        // On FLUTED_GLASS, the backdropBitmap is refracted via the authentic renderFlutedGlass pass.
        if (
            palette.usesArtwork &&
            palette.backdrop != ShareBackdropStyle.ASCII_ARTWORK &&
            palette.backdrop != ShareBackdropStyle.FLUTED_GLASS
        ) {
            if (!imageUrl.isNullOrBlank()) {
                ShareBlurBase(imageUrl = imageUrl)
                // Contrast gradient overlay
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(brush = Brush.verticalGradient(palette.overlay)),
                )
            } else if (customBackdrop != null) {
                // Single custom canva acting like image!
                customBackdrop()
                // Contrast gradient overlay
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(brush = Brush.verticalGradient(palette.overlay)),
                )
            }
        }

        // Theme-specific backdrop decoration (glow orbs, fluted glass, ascii, rings, sun wash, grain)
        ShareThemeDecorations(
            palette = palette,
            imageUrl = imageUrl,
            backdropBitmap = backdropBitmap,
        )

        content()

        // Minimal Branding at Bottom
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.share_brand),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = palette.branding,
                letterSpacing = 4.sp,
            )
        }
    }
}

/**
 * 9:16 Optimized Share Card for Artists
 */
