package com.suvojeet.suvmusic.ui.components.player.miniplayer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.model.PlayerState
import com.suvojeet.suvmusic.ui.components.DominantColors
import com.suvojeet.suvmusic.ui.components.glass.LiquidGlassSurface

/**
 * iOS-style Liquid Glass mini player.
 *
 * A floating pill with a frosted glass surface. Tint derives from the current album's
 * dominant color so it feels cohesive with the rest of the UI.
 */
@Composable
fun LiquidGlassMiniPlayer(
    song: Song,
    playerState: PlayerState,
    dominantColors: DominantColors,
    progress: Float,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    onTap: () -> Unit,
    userAlpha: Float = 0f,
    artworkShape: String = "ROUNDED_SQUARE",
    blurAmount: Float = 50f,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isSystemInDarkTheme()
    val effectiveAlpha = 1f - userAlpha

    val artShape = when (artworkShape) {
        "CIRCLE", "VINYL" -> CircleShape
        "SQUARE" -> androidx.compose.ui.graphics.RectangleShape
        else -> RoundedCornerShape(14.dp)
    }

    val highResThumbnail = remember(song.thumbnailUrl) {
        com.suvojeet.suvmusic.util.ImageUtils.getHighResThumbnailUrl(song.thumbnailUrl, size = 544)
    }

    @Composable
    fun GlassButton(
        onClick: () -> Unit,
        size: Dp = 36.dp,
        content: @Composable () -> Unit
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.82f else 1f,
            animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium),
            label = "glassBtnScale"
        )
        val bgAlpha by animateFloatAsState(
            targetValue = if (isPressed) 0.18f else 0f,
            animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium),
            label = "glassBtnBg"
        )
        Box(
            modifier = Modifier
                .size(size)
                .scale(scale)
                .clip(CircleShape)
                .background(dominantColors.onBackground.copy(alpha = bgAlpha))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) { content() }
    }

    val pillShape = RoundedCornerShape(32.dp)

    Box(
        modifier = modifier
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        LiquidGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(pillShape)
                .clickable(onClick = onTap),
            shape = pillShape,
            blurAmount = blurAmount,
            intensity = effectiveAlpha.coerceAtLeast(0.7f),
            tint = dominantColors.primary,
            isDarkTheme = isDarkTheme
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Artwork
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(4.dp)
                        .clip(artShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (highResThumbnail != null) {
                        AsyncImage(
                            model = highResThumbnail,
                            contentDescription = song.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = dominantColors.onBackground,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = dominantColors.onBackground.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                GlassButton(onClick = onPlayPause) {
                    AnimatedContent(
                        targetState = playerState.isPlaying,
                        transitionSpec = {
                            (scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn()) togetherWith
                                (scaleOut() + fadeOut())
                        },
                        label = "glassPlayPause"
                    ) { playing ->
                        Icon(
                            imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playing) "Pause" else "Play",
                            tint = dominantColors.onBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                GlassButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = dominantColors.onBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (!playerState.isPlaying) {
                    GlassButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = dominantColors.onBackground,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))
            }

            // Progress line along the bottom inside the pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .align(Alignment.BottomCenter)
            ) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp)),
                    color = dominantColors.accent,
                    trackColor = dominantColors.onBackground.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}
