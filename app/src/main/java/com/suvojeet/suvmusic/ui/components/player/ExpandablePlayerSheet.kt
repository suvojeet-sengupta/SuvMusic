package com.suvojeet.suvmusic.ui.components.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.suvojeet.suvmusic.data.model.PlayerState
import com.suvojeet.suvmusic.data.model.MiniPlayerStyle
import com.suvojeet.suvmusic.ui.components.DominantColors
import kotlinx.coroutines.launch

import com.suvojeet.suvmusic.ui.components.player.miniplayer.PillMiniPlayer
import com.suvojeet.suvmusic.ui.components.player.miniplayer.StandardMiniPlayer
import com.suvojeet.suvmusic.ui.components.player.miniplayer.YTMusicMiniPlayer

/**
 * YouTube Music-style expandable player sheet.
 *
 * This composable manages both the collapsed mini-player row and the expanded
 * full-player content in a single, continuously draggable panel.
 *
 * Architecture:
 * - expansion = 0f → Collapsed mini player (64dp peek)
 * - expansion = 1f → Full-screen player
 * - 0..1 → Smooth interpolation of height, artwork, text alpha
 *
 * Drag gesture is handled internally. The parent only
 * needs to provide a slot for the expanded content.
 */

private val MiniPlayerHeight = 64.dp

@Composable
fun ExpandablePlayerSheet(
    playerState: PlayerState,
    dominantColors: DominantColors,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit,
    bottomPadding: Float,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    userAlpha: Float = 0f,
    swipeDownToDismissEnabled: Boolean = true,
    style: MiniPlayerStyle = MiniPlayerStyle.STANDARD,
    expandedContent: @Composable (onCollapse: () -> Unit) -> Unit
) {
    val song = playerState.currentSong ?: return
    val coroutineScope = rememberCoroutineScope()

    // Animation State
    val expansion = remember { Animatable(if (isExpanded) 1f else 0f) }

    // Sync with external state
    LaunchedEffect(isExpanded) {
        val target = if (isExpanded) 1f else 0f
        if (expansion.value != target) {
            expansion.animateTo(
                targetValue = target,
                animationSpec = tween(
                    durationMillis = 350,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    val density = LocalDensity.current
    val view = LocalView.current
    val screenHeightPx = view.height.toFloat()
    val miniPlayerHeightPx = with(density) { MiniPlayerHeight.toPx() }

    // Total drag range from (MiniPlayer + Nav Bar) to Full Screen
    // For YT_MUSIC, we reduce the visual gap (approx 12dp) 
    // to sit flush against the navbar content.
    val stylePaddingOffset = if (style == MiniPlayerStyle.YT_MUSIC) with(density) { 12.dp.toPx() } else 0f
    val adjustedBottomPadding = (bottomPadding - stylePaddingOffset).coerceAtLeast(0f)

    val collapsedHeightPx = miniPlayerHeightPx + adjustedBottomPadding
    val dragRange = (screenHeightPx - (miniPlayerHeightPx + bottomPadding)).coerceAtLeast(1f)

    // Back Handler to collapse on system back gesture
    BackHandler(enabled = isExpanded) {
        onExpandChange(false)
        coroutineScope.launch {
            expansion.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
        }
    }

    // Panel height: lerp from mini player (+ padding) to full screen
    val panelHeightPx = (miniPlayerHeightPx + bottomPadding) + (dragRange * expansion.value.coerceAtLeast(0f))
    val panelHeightDp = with(density) { panelHeightPx.toDp() }

    // The entire expandable panel
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(panelHeightDp)
            // Removed pointerInput from here to allow clicks to pass through to Nav Bar in the transparent area
    ) {
        // ── Collapsed Mini Player Row ──
        // Visible when expansion < ~0.4, fades out as expansion increases
        val miniPlayerAlpha = (1f - expansion.value * 2.5f).coerceIn(0f, 1f)
        if (miniPlayerAlpha > 0f) {
            // When collapsed (expansion=0), offset the mini player down to close the gap
            val collapsedOffsetPx = (bottomPadding - adjustedBottomPadding) * (1f - expansion.value.coerceAtLeast(0f))
            
            CollapsedMiniPlayer(
                song = song,
                playerState = playerState,
                dominantColors = dominantColors,
                progress = playerState.progress,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onClose = onClose,
                userAlpha = userAlpha,
                style = style,
                onTap = {
                    coroutineScope.launch {
                        expansion.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = 350,
                                easing = FastOutSlowInEasing
                            )
                        )
                        onExpandChange(true)
                    }
                },
                modifier = Modifier
                     .fillMaxWidth()
                     .height(MiniPlayerHeight)
                     .alpha(miniPlayerAlpha)
                     .align(Alignment.TopCenter)
                     .offset(y = with(density) { collapsedOffsetPx.toDp() })
                     .graphicsLayer {
                         // Visual feedback for swipe down to dismiss
                         if (expansion.value < 0f && swipeDownToDismissEnabled) {
                             translationY = -expansion.value * dragRange * 0.8f
                         }
                     }
                     .zIndex(if (isExpanded) 0f else 1f)
                     // Add gesture detection to MiniPlayer
                     .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                coroutineScope.launch {
                                    if (expansion.value < -0.1f && swipeDownToDismissEnabled) {
                                        // Swipe down to dismiss/stop
                                        onClose()
                                        // Reset expansion for next time it's shown
                                        expansion.snapTo(0f)
                                    } else {
                                        val targetValue = if (expansion.value > 0.4f) 1f else 0f
                                        expansion.animateTo(
                                            targetValue = targetValue,
                                            animationSpec = tween(
                                                durationMillis = 250,
                                                easing = FastOutSlowInEasing
                                            )
                                        )
                                        onExpandChange(targetValue == 1f)
                                    }
                                }
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    val targetValue = if (expansion.value > 0.4f) 1f else 0f
                                    expansion.animateTo(
                                        targetValue = targetValue,
                                        animationSpec = tween(
                                            durationMillis = 250,
                                            easing = FastOutSlowInEasing
                                        )
                                    )
                                    onExpandChange(targetValue == 1f)
                                }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                val delta = -dragAmount / dragRange
                                coroutineScope.launch {
                                    val minExpansion = if (swipeDownToDismissEnabled) -0.5f else 0f
                                    expansion.snapTo(
                                        (expansion.value + delta).coerceIn(minExpansion, 1f)
                                    )
                                }
                            }
                        )
                    }
             )
        }

        // ── Expanded Full Player ──
        // Visible when expansion > ~0.3, fades out as expansion increases
        val fullPlayerAlpha = ((expansion.value - 0.3f) / 0.7f).coerceIn(0f, 1f)
        if (fullPlayerAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(fullPlayerAlpha)
                    .zIndex(if (isExpanded) 1f else 0f)
                    // Add gesture detection to Full Player
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                // Threshold 0.8f makes it easier to collapse (only need to drag down 20%)
                                val targetValue = if (expansion.value > 0.8f) 1f else 0f 
                                
                                coroutineScope.launch {
                                     expansion.animateTo(
                                        targetValue = targetValue,
                                        animationSpec = tween(
                                            durationMillis = 250,
                                            easing = FastOutSlowInEasing
                                        )
                                    )
                                    onExpandChange(targetValue == 1f)
                                }
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    val targetValue = if (expansion.value > 0.8f) 1f else 0f
                                    expansion.animateTo(
                                        targetValue = targetValue,
                                        animationSpec = tween(
                                            durationMillis = 250,
                                            easing = FastOutSlowInEasing
                                        )
                                    )
                                    onExpandChange(targetValue == 1f)
                                }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                val delta = -dragAmount / dragRange
                                coroutineScope.launch {
                                    expansion.snapTo(
                                        (expansion.value + delta).coerceIn(0f, 1f)
                                    )
                                }
                            }
                        )
                    }
            ) {
                expandedContent {
                    // onCollapse callback
                    coroutineScope.launch {
                        expansion.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(
                                durationMillis = 300,
                                easing = FastOutSlowInEasing
                            )
                        )
                        onExpandChange(false)
                    }
                }
            }
        }
    }
}

/**
 * The collapsed mini player row — a compact horizontal bar showing
 * artwork, song title/artist, and play/next controls.
 */
@Composable
private fun CollapsedMiniPlayer(
    song: com.suvojeet.suvmusic.core.model.Song,
    playerState: PlayerState,
    dominantColors: DominantColors,
    progress: Float,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    onTap: () -> Unit,
    userAlpha: Float = 0f,
    style: MiniPlayerStyle = MiniPlayerStyle.STANDARD,
    modifier: Modifier = Modifier
) {
    when (style) {
        MiniPlayerStyle.FLOATING_PILL -> {
            PillMiniPlayer(
                song = song,
                playerState = playerState,
                dominantColors = dominantColors,
                progress = progress,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onClose = onClose,
                onTap = onTap,
                userAlpha = userAlpha,
                modifier = modifier
            )
        }
        MiniPlayerStyle.YT_MUSIC -> {
            YTMusicMiniPlayer(
                song = song,
                playerState = playerState,
                dominantColors = dominantColors,
                progress = progress,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onClose = onClose,
                onTap = onTap,
                userAlpha = userAlpha,
                modifier = modifier
            )
        }
        else -> {
            StandardMiniPlayer(
                song = song,
                playerState = playerState,
                dominantColors = dominantColors,
                progress = progress,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onClose = onClose,
                onTap = onTap,
                userAlpha = userAlpha,
                modifier = modifier
            )
        }
    }
}

