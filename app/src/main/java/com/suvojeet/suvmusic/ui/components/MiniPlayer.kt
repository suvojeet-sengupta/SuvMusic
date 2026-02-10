package com.suvojeet.suvmusic.ui.components

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.suvojeet.suvmusic.data.model.PlayerState
import com.suvojeet.suvmusic.ui.utils.SharedTransitionKeys
import kotlin.math.roundToInt

/**
 * Mini player that appears at the bottom of the screen.
 * Shows current playing song with basic controls.
 */
@Composable
fun MiniPlayer(
    playerState: PlayerState,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onPlayerClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCloseClick: (() -> Unit)? = null,
    onLikeClick: () -> Unit = {},
    progressProvider: () -> Float = { playerState.progress },
    alpha: Float = 1f,
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null
) {
    val song = playerState.currentSong

    val cornerRadius = 14.dp
    val playerShape = RoundedCornerShape(cornerRadius)
    
    // Extract dominant colors
    val dominantColors = rememberDominantColors(
        imageUrl = song?.thumbnailUrl,
        isDarkTheme = true // Force dark for miniplayer usually looks better or match system
    )
    
    // Animate background color
    val backgroundColor by animateColorAsState(
        targetValue = if (song != null) dominantColors.primary.copy(alpha = alpha) 
                      else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = alpha),
        label = "MiniPlayerBackground"
    )

    // IMPORTANT: Shared Transition Logic
    // We apply sharedElement to the Surface (container) and the Artwork
    
    song?.let {
        // Swipe Logic
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }
        val swipeThreshold = 100f

        val sharedModifier = Modifier
            .offset { 
                // Apply resistance to the drag visualization so it doesn't float freely
                // Only allow small vertical movement to indicate swipe intent
                IntOffset(
                    (offsetX * 0.1f).roundToInt(), 
                    (offsetY * 0.2f).roundToInt().coerceAtMost(0) // Only allow pulling up visually
                ) 
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (offsetY < -swipeThreshold) onPlayerClick()
                        else if (offsetX > swipeThreshold) onPreviousClick()
                        else if (offsetX < -swipeThreshold) onNextClick()
                        offsetX = 0f
                        offsetY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                )
            }

        // Get style
        val context = androidx.compose.ui.platform.LocalContext.current
        val sessionManager = remember { com.suvojeet.suvmusic.data.SessionManager(context) }
        val miniPlayerStyle by sessionManager.miniPlayerStyleFlow.collectAsState(initial = com.suvojeet.suvmusic.data.model.MiniPlayerStyle.STANDARD)

        val animatedProgress by animateFloatAsState(
            targetValue = progressProvider(),
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "progress"
        )
        
        if (miniPlayerStyle == com.suvojeet.suvmusic.data.model.MiniPlayerStyle.FLOATING_PILL) {
            FloatingPillMiniPlayer(
                song = song,
                playerState = playerState,
                dominantColors = dominantColors,
                modifier = sharedModifier,
                onPlayPauseClick = onPlayPauseClick,
                onNextClick = onNextClick,
                onPreviousClick = onPreviousClick,
                onPlayerClick = onPlayerClick,
                onCloseClick = onCloseClick,
                onLikeClick = onLikeClick,
                isLiked = playerState.isLiked,
                progress = animatedProgress,
                alpha = alpha,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
        } else {
            StandardMiniPlayer(
                song = song,
                playerState = playerState,
                dominantColors = dominantColors,
                modifier = sharedModifier,
                backgroundColor = backgroundColor,
                onPlayPauseClick = onPlayPauseClick,
                onNextClick = onNextClick,
                onPlayerClick = onPlayerClick,
                onCloseClick = onCloseClick,
                progress = animatedProgress,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    }
}



@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun StandardMiniPlayer(
    song: com.suvojeet.suvmusic.core.model.Song,
    playerState: PlayerState,
    dominantColors: DominantColors,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPlayerClick: () -> Unit,
    onCloseClick: (() -> Unit)?,
    progress: Float,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null
) {
    val cornerRadius = 14.dp
    val playerShape = RoundedCornerShape(cornerRadius)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .shadow(
                elevation = 16.dp,
                shape = playerShape,
                spotColor = dominantColors.primary.copy(alpha = 0.5f)
            )
            .clip(playerShape)
            .clickable(onClick = onPlayerClick),
        color = backgroundColor,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art
                val artworkSharedModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                    with(sharedTransitionScope) {
                        Modifier.sharedElement(
                            sharedContentState = rememberSharedContentState(key = SharedTransitionKeys.playerArtwork(song.id)),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                } else {
                    Modifier
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .then(artworkSharedModifier)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (song.thumbnailUrl != null) {
                        AsyncImage(
                            model = song.thumbnailUrl,
                            contentDescription = song.title,
                            modifier = Modifier.size(42.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(10.dp))
                
                // Song Info
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
                        color = dominantColors.onBackground.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Controls
                IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                        tint = dominantColors.onBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(4.dp))
                
                IconButton(
                    onClick = onNextClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = dominantColors.onBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (onCloseClick != null) {
                    IconButton(
                        onClick = onCloseClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = dominantColors.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                trackColor = dominantColors.onBackground.copy(alpha = 0.2f),
                color = dominantColors.accent,
                strokeCap = StrokeCap.Round
            )
        }
    }
}
