package me.avinas.tempo.ui.spotlight

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridItemSpan

import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Balance
import kotlinx.coroutines.delay
import me.avinas.tempo.ui.components.CachedAsyncImage
import me.avinas.tempo.ui.components.LocalInCaptureContext
import androidx.compose.ui.platform.LocalContext
import me.avinas.tempo.data.stats.TimeRange
import me.avinas.tempo.ui.components.GlassCard
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import me.avinas.tempo.R
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.text.font.FontStyle

@Composable
fun ListeningMinutesPage(page: SpotlightStoryPage.ListeningMinutes) {
    val skipEntryMotion = skipStoryEntryMotion()
    val animatedMinutes = remember { Animatable(0f) }
    LaunchedEffect(page.totalMinutes, skipEntryMotion) {
        if (skipEntryMotion) {
            animatedMinutes.snapTo(page.totalMinutes.toFloat())
        } else {
            delay(500)
            animatedMinutes.animateTo(
                targetValue = page.totalMinutes.toFloat(),
                animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing)
            )
        }
    }
    
    // Ambient pulse removed: the bare display number is this page's device —
    // a breathing glow behind it just adds a second focal point.
    
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val dimens = rememberSpotlightDimens(maxHeight)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = dimens.screenTopPadding,
                    start = dimens.horizontalPadding,
                    end = dimens.horizontalPadding,
                    bottom = dimens.screenBottomPadding
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EnterAnimation(delay = 0) {
                val context = LocalContext.current
                val titleText = SpotlightPeriodFormatter.storyTitle(
                    context = context,
                    timeRange = page.timeRange,
                    year = page.year
                )

                Text(
                    text = titleText,
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = dimens.textTitle),
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f),
                    letterSpacing = (1f * dimens.scale).sp,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            EnterAnimation(delay = 200) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.spotlight_listened_for),
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = dimens.textHeadline),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(dimens.spacerSmall))

                    // Scale down the display number by final digit count so 6-7 digit
                    // values never wrap on 360dp-wide phones (content width ~312dp).
                    val finalMinutesText = String.format(java.util.Locale.US, "%,d", page.totalMinutes)
                    val numberFontSize = when {
                        finalMinutesText.length >= 7 -> dimens.textDisplay * 0.66f
                        finalMinutesText.length >= 6 -> dimens.textDisplay * 0.78f
                        else -> dimens.textDisplay
                    }
                    Text(
                        text = String.format(java.util.Locale.US, "%,d", animatedMinutes.value.toInt()),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = numberFontSize,
                            fontWeight = FontWeight.Black,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.3f),
                                offset = Offset(0f, 4f),
                                blurRadius = 12f
                            )
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        lineHeight = (numberFontSize.value * 1.1f).sp
                    )

                    Text(
                        text = stringResource(R.string.spotlight_listened_minutes),
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = dimens.textHeadline),
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f),
                        letterSpacing = (2f * dimens.scale).sp
                    )

                    Spacer(modifier = Modifier.height(dimens.spacerSmall))

                    Text(
                        text = page.conversationalText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = dimens.textBody),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            
            Spacer(modifier = Modifier.weight(1f))
            
            val days = page.totalMinutes.toFloat() / (24 * 60)
            val daysInt = days.toInt()
            if (daysInt > 0) {
                EnterAnimation(delay = 600) {
                    GlassCard(
                        modifier = Modifier.wrapContentSize(),
                        shape = RoundedCornerShape(50),
                        backgroundColor = Color.White.copy(alpha = 0.1f),
                        contentPadding = PaddingValues(horizontal = dimens.horizontalPadding, vertical = dimens.spacerSmall)
                    ) {
                        Text(
                            text = stringResource(R.string.spotlight_days_nonstop, daysInt),
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = dimens.textBody),
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
            
            // Comparative surprise callout
            if (page.comparativeText != null) {
                EnterAnimation(delay = 800) {
                    Spacer(modifier = Modifier.height(dimens.spacerMedium))
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(50)
                            )
                            .border(1.dp * dimens.scale, Color.White.copy(alpha = 0.3f), RoundedCornerShape(50))
                            .padding(horizontal = 20.dp * dimens.scale, vertical = 10.dp * dimens.scale)
                    ) {
                        Text(
                            text = page.comparativeText,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = (dimens.textBody.value * 0.9f).sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFFFBBF24),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopArtistPage(page: SpotlightStoryPage.TopArtist) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val dimens = rememberSpotlightDimens(maxHeight)
        // Scale down hero image when displaying more than 5 artists
        val heroImageScale = if (page.topArtists.size > 5) 0.6f else 1.0f
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = dimens.screenTopPadding,
                    start = dimens.horizontalPadding,
                    end = dimens.horizontalPadding,
                    bottom = dimens.screenBottomPadding
                )
        ) {
            // HEADER - Fixed
            EnterAnimation(delay = 0) {
                 Text(
                     text = stringResource(R.string.spotlight_top_artist_label),
                     style = MaterialTheme.typography.titleLarge.copy(fontSize = dimens.textTitle),
                     fontWeight = FontWeight.Bold,
                     color = Color.White.copy(alpha = 0.85f),
                     modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                     textAlign = TextAlign.Center
                 )
             }
             
             // MAIN CONTENT - Weighted Distribution
             Column(modifier = Modifier.weight(1f)) {
                 
                 // 1. HERO SECTION (~45% weight) - Increased to reduce grid height further
                 Column(
                     modifier = Modifier.weight(0.45f).fillMaxWidth(),
                     horizontalAlignment = Alignment.CenterHorizontally,
                     verticalArrangement = Arrangement.Center
                 ) {
                     EnterAnimation(delay = 200) {
                         Column(horizontalAlignment = Alignment.CenterHorizontally) {
                             val imageSize = dimens.imageMain * heroImageScale
                             
                             Box(contentAlignment = Alignment.Center) {
                                 // Glow effect
                                 Box(
                                     modifier = Modifier
                                         .size(imageSize + (40.dp * dimens.scale * heroImageScale))
                                         .background(
                                             brush = Brush.radialGradient(
                                                 colors = listOf(
                                                     Color(0xFFA855F7).copy(alpha = 0.4f),
                                                     Color.Transparent
                                                 )
                                             ),
                                             shape = CircleShape
                                         )
                                 )
                                 
                                 CachedAsyncImage(
                                     imageUrl = page.topArtistImageUrl,
                                     contentDescription = null,
                                     modifier = Modifier
                                         .size(imageSize)
                                         .clip(CircleShape)
                                         .border(4.dp * dimens.scale * heroImageScale, Color.White.copy(alpha = 0.2f), CircleShape),
                                     contentScale = ContentScale.Crop,
                                     allowHardware = false
                                 )
                             }
                             
                             Spacer(modifier = Modifier.height(dimens.spacerSmall)) // Reduced spacer
                             
                             Text(
                                 text = page.topArtistName,
                                 style = MaterialTheme.typography.displaySmall.copy(
                                     fontSize = (dimens.textHeadline.value * heroImageScale).sp
                                 ),
                                 fontWeight = FontWeight.Black,
                                 color = Color.White,
                                 textAlign = TextAlign.Center,
                                 maxLines = 1,
                                 overflow = TextOverflow.Ellipsis
                             )

                             Spacer(modifier = Modifier.height(dimens.spacerSmall))

                             Text(
                                 text = page.conversationalText,
                                 style = MaterialTheme.typography.bodyMedium.copy(fontSize = dimens.textBody),
                                 fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                 color = Color.White.copy(alpha = 0.8f),
                                 textAlign = TextAlign.Center,
                                 maxLines = 2,
                                 overflow = TextOverflow.Ellipsis
                             )
                         }
                     }
                 }
                 
                 // 2. GRID SECTION (~40% weight) - 4 Rows - Further Reduced
                 Column(
                    modifier = Modifier.weight(0.40f).fillMaxWidth(),
                    verticalArrangement = Arrangement.SpaceEvenly 
                 ) {
                     val gridItems = page.topArtists.drop(1).take(8) // Rank 2 to 9
                     val chunkedItems = gridItems.chunked(2)
                     
                     chunkedItems.forEachIndexed { rowIndex, rowItems ->
                         Row(
                             modifier = Modifier.fillMaxWidth().weight(1f),
                             horizontalArrangement = Arrangement.spacedBy(dimens.gridSpacing),
                             verticalAlignment = Alignment.CenterVertically
                         ) {
                             rowItems.forEachIndexed { colIndex, artist ->
                                 Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center
                                 ) {
                                      EnterAnimation(delay = 400 + (rowIndex * 100) + (colIndex * 50)) {
                                          SolidCard(
                                              modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
                                              shape = RoundedCornerShape(dimens.cardCornerRadius * 0.6f),
                                              backgroundColor = Color.White.copy(alpha = 0.08f),
                                              borderColor = Color.White.copy(alpha = 0.15f),
                                              contentPadding = PaddingValues(horizontal = 8.dp * dimens.scale, vertical = 4.dp * dimens.scale)
                                          ) {
                                              Row(verticalAlignment = Alignment.CenterVertically) {
                                                  Text(
                                                      text = "#${artist.rank}",
                                                      style = MaterialTheme.typography.labelSmall,
                                                      color = Color.White.copy(alpha = 0.6f),
                                                      modifier = Modifier.width((20.dp * dimens.scale))
                                                  )
                                                  CachedAsyncImage(
                                                      imageUrl = artist.imageUrl,
                                                      contentDescription = null,
                                                      modifier = Modifier.size(dimens.imageGrid).clip(CircleShape),
                                                      contentScale = ContentScale.Crop,
                                                      allowHardware = false
                                                  )
                                                  Spacer(modifier = Modifier.width(8.dp * dimens.scale))
                                                  Text(
                                                      text = artist.name,
                                                      style = MaterialTheme.typography.bodySmall,
                                                      fontWeight = FontWeight.SemiBold,
                                                      color = Color.White,
                                                      maxLines = 2,
                                                      overflow = TextOverflow.Ellipsis
                                                  )
                                              }
                                          }
                                       }
                                 }
                             }
                             if (rowItems.size == 1) {
                                 Spacer(modifier = Modifier.weight(1f))
                             }
                         }
                     }
                 }
                 
                 // 3. FOOTER SECTION (Rank 10) (~15% weight)
                Box(
                    modifier = Modifier.weight(0.15f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val lastArtist = page.topArtists.drop(1).drop(8).firstOrNull() // Rank 10
                     if (lastArtist != null) {
                           EnterAnimation(delay = 800) {
                              // Flat finale: the #10 card drops the glass tint + border the
                              // grid uses — solid fill marks the list's ending beat.
                              SolidCard(
                                  modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f),
                                  shape = RoundedCornerShape(dimens.cardCornerRadius * 0.6f),
                                  backgroundColor = Color.White.copy(alpha = 0.16f),
                                  borderColor = Color.Transparent,
                                  contentPadding = PaddingValues(12.dp * dimens.scale)
                              ) {
                              Row(verticalAlignment = Alignment.CenterVertically) {
                                      Text(
                                          text = "#${lastArtist.rank}",
                                          style = MaterialTheme.typography.labelSmall.copy(fontSize = dimens.textLabel),
                                          color = Color.White.copy(alpha = 0.6f),
                                          modifier = Modifier.width(32.dp * dimens.scale)
                                      )
                                     CachedAsyncImage(
                                         imageUrl = lastArtist.imageUrl,
                                         contentDescription = null,
                                         modifier = Modifier.size(dimens.imageList).clip(CircleShape),
                                         contentScale = ContentScale.Crop,
                                         allowHardware = false
                                     )
                                     Spacer(modifier = Modifier.width(dimens.gridSpacing))
                                     Text(
                                         text = lastArtist.name,
                                         style = MaterialTheme.typography.bodySmall.copy(fontSize = dimens.textLabel),
                                         fontWeight = FontWeight.SemiBold,
                                         color = Color.White,
                                         maxLines = 2,
                                         overflow = TextOverflow.Ellipsis
                                     )
                                 }
                             }
                          }
                    }
                }
             }
        }
    }
}
@Composable
fun TopTrackSetupPage(page: SpotlightStoryPage.TopTrackSetup) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val dimens = rememberSpotlightDimens(maxHeight)
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = dimens.screenTopPadding,
                    start = dimens.horizontalPadding,
                    end = dimens.horizontalPadding,
                    bottom = dimens.screenBottomPadding
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            EnterAnimation(delay = 0) {
                Text(
                    text = stringResource(R.string.spotlight_top_track_setup_label),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = dimens.textTitle),
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(dimens.spacerMedium))

            EnterAnimation(delay = 200) {
                AutoFitText(
                    text = page.conversationalText,
                    maxFontSize = dimens.textHeadline,
                    baseStyle = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    minFontSize = dimens.textBody
                )
            }
            
            Spacer(modifier = Modifier.height(dimens.spacerLarge))
            
            EnterAnimation(delay = 400) {
                CachedAsyncImage(
                    imageUrl = page.topSongImageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(dimens.imageMain * 0.8f)
                        .clip(RoundedCornerShape(12.dp * dimens.scale))
                        .blur(8.dp * dimens.scale),
                    contentScale = ContentScale.Crop,
                    allowHardware = false
                )
            }
        }
    }
}

@Composable
fun TopSongsPage(page: SpotlightStoryPage.TopSongs) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val dimens = rememberSpotlightDimens(maxHeight)
        // Scale down hero image when displaying more than 5 songs to preserve list space
        val heroImageScale = if (page.topSongs.size > 5) 0.6f else 1.0f
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = dimens.screenTopPadding,
                    start = dimens.horizontalPadding,
                    end = dimens.horizontalPadding,
                    bottom = dimens.screenBottomPadding
                )
        ) {
            // HEADER - Fixed
            EnterAnimation(delay = 0) {
                Text(
                    text = stringResource(R.string.spotlight_top_song_label),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = dimens.textTitle),
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    textAlign = TextAlign.Center
                )
            }
            
            // MAIN CONTENT - Weighted Distribution
            Column(modifier = Modifier.weight(1f)) {
                
                // 1. HERO SECTION (~45% weight) - Increased to reduce grid height further
                Column(
                    modifier = Modifier.weight(0.45f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    EnterAnimation(delay = 200) {
                         Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(contentAlignment = Alignment.Center) {
                                CachedAsyncImage(
                                    imageUrl = page.topSongImageUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(dimens.imageMain * heroImageScale)
                                        .clip(RoundedCornerShape(24.dp * dimens.scale))
                                        .border(1.dp * dimens.scale, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp * dimens.scale)),
                                    contentScale = ContentScale.Crop,
                                    allowHardware = false
                                )
                            }
                            Spacer(modifier = Modifier.height(dimens.spacerSmall))
                            Text(
                                text = page.topSongTitle,
                                style = MaterialTheme.typography.headlineMedium.copy(fontSize = (dimens.textHeadline.value * heroImageScale).sp),
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = page.topSongArtist,
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = (dimens.textTitle.value * heroImageScale).sp),
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                            // Total playtime chip for #1 song
                            if (page.totalTimeMs > 0L) {
                                val playtimeText = me.avinas.tempo.ui.stats.displayStatsDuration(page.totalTimeMs)
                                Spacer(modifier = Modifier.height(dimens.spacerSmall))
                                // Playtime highlight chip for #1 song
                                SolidCard(
                                    modifier = Modifier.wrapContentSize(),
                                    shape = RoundedCornerShape(50),
                                    backgroundColor = Color(0xFFFBBF24),
                                    borderColor = Color.Transparent,
                                    contentPadding = PaddingValues(horizontal = 14.dp * dimens.scale, vertical = 5.dp * dimens.scale)
                                ) {
                                    Text(
                                        text = playtimeText,
                                        style = MaterialTheme.typography.labelLarge.copy(fontSize = (dimens.textBody.value * 0.85f).sp),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF220F08),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. GRID SECTION (~40% weight) - 4 Rows - Further Reduced
                Box(
                    modifier = Modifier.weight(0.40f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                     val gridItems = page.topSongs.drop(1).take(8) // Rank 2 to 9
                     Column(
                         modifier = Modifier.fillMaxSize(),
                         verticalArrangement = Arrangement.spacedBy(8.dp * dimens.scale)
                     ) {
                         gridItems.chunked(2).forEachIndexed { rowIndex, rowItems ->
                             Row(
                                 modifier = Modifier.fillMaxWidth().weight(1f), // Each row gets equal height
                                 horizontalArrangement = Arrangement.spacedBy(8.dp * dimens.scale)
                             ) {
                                 rowItems.forEachIndexed { colIndex, item ->
                                     EnterAnimation(delay = 400 + (rowIndex * 2 + colIndex) * 50) {
                                         // Inner Item
                                         SolidCard(
                                             modifier = Modifier.weight(1f).fillMaxHeight(),
                                             shape = RoundedCornerShape(dimens.cardCornerRadius * 0.6f),
                                             backgroundColor = Color.White.copy(alpha = 0.08f),
                                             borderColor = Color.White.copy(alpha = 0.15f),
                                             contentPadding = PaddingValues(8.dp * dimens.scale)
                                         ) {
                                             Row(
                                                 verticalAlignment = Alignment.CenterVertically,
                                                 modifier = Modifier.fillMaxSize()
                                             ) {
                                                 Text(
                                                     text = "#${item.rank}",
                                                     style = MaterialTheme.typography.labelSmall.copy(fontSize = dimens.textLabel),
                                                     fontWeight = FontWeight.Bold,
                                                     color = Color.White.copy(alpha = 0.6f),
                                                     modifier = Modifier.width(20.dp * dimens.scale)
                                                 )
                                                 CachedAsyncImage(
                                                     imageUrl = item.imageUrl,
                                                     contentDescription = null,
                                                     modifier = Modifier.size(dimens.imageList * 0.8f).clip(RoundedCornerShape(6.dp * dimens.scale)),
                                                     contentScale = ContentScale.Crop,
                                                     allowHardware = false
                                                 )
                                                 Spacer(modifier = Modifier.width(6.dp * dimens.scale))
                                                 Column(modifier = Modifier.weight(1f)) {
                                                     Text(
                                                         text = item.title,
                                                         style = MaterialTheme.typography.bodyMedium.copy(fontSize = (dimens.textBody.value * 0.85f).sp),
                                                         fontWeight = FontWeight.Bold,
                                                         color = Color.White,
                                                         maxLines = 1,
                                                         overflow = TextOverflow.Ellipsis
                                                     )
                                                     Text(
                                                         text = item.artist,
                                                         style = MaterialTheme.typography.labelSmall.copy(fontSize = (dimens.textLabel.value * 0.8f).sp),
                                                         color = Color.White.copy(alpha = 0.7f),
                                                         maxLines = 1,
                                                         overflow = TextOverflow.Ellipsis
                                                     )
                                                 }
                                             }
                                         }
                                     }
                                 }
                                 // If row has only 1 item (shouldn't happen with logic but safe-guard), add spacer
                                 if (rowItems.size == 1) {
                                     Spacer(modifier = Modifier.weight(1f))
                                 }
                             }
                         }
                     }
                }

                // 3. FOOTER SECTION (Rank 10) (~15% weight)
                Box(
                    modifier = Modifier.weight(0.15f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val lastItem = page.topSongs.drop(1).drop(8).firstOrNull() // Rank 10
                     if (lastItem != null) {
                         EnterAnimation(delay = 800) {
                             // #10 song item
                             SolidCard(
                                 modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f),
                                 shape = RoundedCornerShape(dimens.cardCornerRadius * 0.6f),
                                 backgroundColor = Color.White.copy(alpha = 0.16f),
                                 borderColor = Color.Transparent,
                                 contentPadding = PaddingValues(12.dp * dimens.scale)
                             ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                     Text(
                                        text = "#${lastItem.rank}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = dimens.textLabel),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.width(32.dp * dimens.scale)
                                    )
                                    CachedAsyncImage(
                                        imageUrl = lastItem.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier.size(dimens.imageList).clip(RoundedCornerShape(8.dp * dimens.scale)),
                                        contentScale = ContentScale.Crop,
                                        allowHardware = false
                                    )
                                    Spacer(modifier = Modifier.width(12.dp * dimens.scale))
                                    Column {
                                        Text(
                                            text = lastItem.title,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = dimens.textBody),
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = lastItem.artist,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = (dimens.textBody.value * 0.85f).sp),
                                            color = Color.White.copy(alpha = 0.7f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopGenresPage(page: SpotlightStoryPage.TopGenres) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val dimens = rememberSpotlightDimens(maxHeight)
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = dimens.screenTopPadding,
                    start = dimens.horizontalPadding,
                    end = dimens.horizontalPadding,
                    bottom = dimens.screenBottomPadding
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            EnterAnimation(delay = 0) {
                Text(
                    text = stringResource(R.string.spotlight_top_genres_label),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = dimens.textTitle),
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(bottom = dimens.spacerSmall)
                )
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                // Floating Animation
                val infiniteTransition = me.avinas.tempo.ui.theme.rememberMotionAwareInfiniteTransition(label = "floating")
                val floatOffset by infiniteTransition.animateFloat(
                    initialValue = -10f,
                    targetValue = 10f,
                    animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                        animation = tween(2000, easing = FastOutSlowInEasing),
                        repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                    ),
                    label = "floatOffset"
                )

                // Top genre center bubble
                EnterAnimation(delay = 200) {
                    val bubbleSize = dimens.bubbleMain
                    Box(
                        modifier = Modifier
                            .offset(y = floatOffset.dp)
                            .size(bubbleSize)
                            .background(Color(0xFF10B981), CircleShape)
                            .border(1.dp * dimens.scale, Color.White.copy(alpha = 0.3f), CircleShape)
                            .padding(24.dp * dimens.scale),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = page.topGenre,
                                style = MaterialTheme.typography.displaySmall.copy(fontSize = dimens.textHeadline),
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                lineHeight = dimens.textHeadline
                            )
                            
                            Spacer(modifier = Modifier.height(dimens.spacerSmall))

                            Text(
                                text = page.conversationalText,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = dimens.textBody),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = Color.White.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp * dimens.scale))
                            Text(
                                text = "${page.topGenrePercentage}%",
                                style = MaterialTheme.typography.headlineSmall.copy(fontSize = dimens.textTitle),
                                color = Color.White.copy(alpha = 0.95f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                // Surrounding Bubbles
                val otherGenres = page.genres.drop(1).take(4)
                val positions = listOf(
                    Alignment.TopStart,
                    Alignment.TopEnd,
                    Alignment.BottomStart,
                    Alignment.BottomEnd
                )
                
                otherGenres.forEachIndexed { index, genre ->
                    if (index < positions.size) {
                        // Staggered floating for other bubbles
                        val otherFloatOffset by infiniteTransition.animateFloat(
                            initialValue = if (index % 2 == 0) 5f else -5f,
                            targetValue = if (index % 2 == 0) -5f else 5f,
                            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                                animation = tween(2500 + (index * 200), easing = FastOutSlowInEasing),
                                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                            ),
                            label = "otherFloatOffset"
                        )

                        Box(
                            modifier = Modifier
                                .align(positions[index])
                                .offset(
                                    x = if (index % 2 == 0) 20.dp * dimens.scale else -(20.dp * dimens.scale),
                                    y = (if (index < 2) 20.dp * dimens.scale else -(20.dp * dimens.scale)) + otherFloatOffset.dp
                                )
                        ) {
                            EnterAnimation(delay = 400 + (index * 150)) {
                                val smallBubbleSize = dimens.bubbleMain * 0.5f
                                Box(
                                    modifier = Modifier
                                        .size(smallBubbleSize)
                                        .background(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.15f),
                                                    Color.White.copy(alpha = 0.05f)
                                                )
                                            ),
                                            shape = CircleShape
                                        )
                                        .border(1.dp * dimens.scale, Color.White.copy(alpha = 0.2f), CircleShape)
                                        .padding(12.dp * dimens.scale),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = genre.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontSize = dimens.textLabel),
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${genre.percentage}%",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = (dimens.textLabel.value * 0.8f).sp),
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

