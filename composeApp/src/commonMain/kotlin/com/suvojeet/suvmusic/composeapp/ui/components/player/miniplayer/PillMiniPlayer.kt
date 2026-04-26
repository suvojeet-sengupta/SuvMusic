package com.suvojeet.suvmusic.composeapp.ui.components.player.miniplayer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.composeapp.image.DominantColors
import com.suvojeet.suvmusic.composeapp.ui.AlbumArt
import com.suvojeet.suvmusic.composeapp.util.ImageUtils
import com.suvojeet.suvmusic.core.model.Song

/**
 * Pill-shaped mini-player with a circular dashed-arc progress ring around
 * the album art. Port of
 * `app/.../ui/components/player/miniplayer/PillMiniPlayer.kt`.
 *
 * Same `PlayerState` → `isPlaying: Boolean` simplification as
 * [StandardMiniPlayer]. See the docstring there for rationale.
 */
@Composable
fun PillMiniPlayer(
    song: Song,
    isPlaying: Boolean,
    dominantColors: DominantColors,
    progress: Float,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    onTap: () -> Unit,
    userAlpha: Float = 0f,
    artworkShape: String = "ROUNDED_SQUARE",
    modifier: Modifier = Modifier,
) {
    val effectiveAlpha = 1f - userAlpha

    val artShape: Shape = when (artworkShape) {
        "CIRCLE", "VINYL" -> CircleShape
        "SQUARE" -> RectangleShape
        else -> RoundedCornerShape(32.dp)
    }

    val highResThumbnail = remember(song.thumbnailUrl) {
        ImageUtils.getHighResThumbnailUrl(song.thumbnailUrl, size = 544)
    }

    Surface(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(32.dp))
            .clickable(onClick = onTap),
        color = Color.Transparent,
        shape = RoundedCornerShape(32.dp),
        tonalElevation = 4.dp,
        shadowElevation = 6.dp,
    ) {
        Box(
            modifier = Modifier.background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        dominantColors.primary.copy(alpha = effectiveAlpha),
                        dominantColors.secondary.copy(alpha = effectiveAlpha),
                    ),
                ),
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Album art with dashed-arc progress ring.
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val activeColor = dominantColors.accent
                    val trackColor = dominantColors.onBackground.copy(alpha = 0.2f)

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 2.dp.toPx()
                        val dashSize = 2.dp.toPx()
                        val gapSize = 3.dp.toPx()

                        drawArc(
                            color = trackColor,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(dashSize, gapSize),
                                    0f,
                                ),
                            ),
                        )

                        drawArc(
                            color = activeColor,
                            startAngle = -90f,
                            sweepAngle = progress * 360f,
                            useCenter = false,
                            style = Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(dashSize, gapSize),
                                    0f,
                                ),
                            ),
                        )
                    }

                    AlbumArt(
                        thumbnailUrl = highResThumbnail,
                        contentDescription = song.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                            .clip(artShape),
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = dominantColors.onBackground,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(),
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = dominantColors.onBackground.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                MiniPlayerButton(
                    onClick = onPlayPause,
                    onBackgroundColor = dominantColors.onBackground,
                ) {
                    AnimatedContent(
                        targetState = isPlaying,
                        transitionSpec = {
                            (scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn()) togetherWith
                                (scaleOut() + fadeOut())
                        },
                        label = "miniPlayPause",
                    ) { playing ->
                        Icon(
                            imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playing) "Pause" else "Play",
                            tint = dominantColors.onBackground,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                MiniPlayerButton(
                    onClick = onNext,
                    onBackgroundColor = dominantColors.onBackground,
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = dominantColors.onBackground,
                        modifier = Modifier.size(24.dp),
                    )
                }

                if (!isPlaying) {
                    MiniPlayerButton(
                        onClick = onClose,
                        onBackgroundColor = dominantColors.onBackground,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = dominantColors.onBackground,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

/**
 * Small bouncy circular IconButton used inside the pill. Hoisted out of
 * [PillMiniPlayer] for re-use and so the inner @Composable function isn't
 * defined inside another Composable (which works but is harder to read).
 */
@Composable
private fun MiniPlayerButton(
    onClick: () -> Unit,
    onBackgroundColor: Color,
    size: Dp = 36.dp,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.78f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "miniPlayerBtnScale",
    )

    val bgAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.15f else 0f,
        animationSpec = spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium),
        label = "miniPlayerBtnBg",
    )

    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(onBackgroundColor.copy(alpha = bgAlpha))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
