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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.suvojeet.suvmusic.data.model.PlayerState
import com.suvojeet.suvmusic.data.model.MiniPlayerStyle
import com.suvojeet.suvmusic.ui.components.DominantColors
import kotlinx.coroutines.launch

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
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val screenHeightPx = with(density) { screenHeightDp.toPx() }
    val miniPlayerHeightPx = with(density) { MiniPlayerHeight.toPx() }

    // Total drag range from (MiniPlayer + Nav Bar) to Full Screen
    val collapsedHeightPx = miniPlayerHeightPx + bottomPadding
    val dragRange = screenHeightPx - collapsedHeightPx

    // Back Handler to collapse on system back gesture
    BackHandler(enabled = isExpanded) {
        onExpandChange(false)
        coroutineScope.launch {
            expansion.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
        }
    }

    // Panel height: lerp from mini player to full screen
    // Panel height: lerp from mini player (+ padding) to full screen
    val panelHeightPx = collapsedHeightPx + (dragRange * expansion.value)
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
                     .align(Alignment.TopCenter) // Align to top, leaving bottom padding area empty
                     .zIndex(if (isExpanded) 0f else 1f)
                     // Add gesture detection to MiniPlayer
                     .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                val targetValue = if (expansion.value > 0.4f) 1f else 0f
                                
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
                                    expansion.snapTo(
                                        (expansion.value + delta).coerceIn(0f, 1f)
                                    )
                                }
                            }
                        )
                    }
             )
        }

        // ── Expanded Full Player ──
        // Visible when expansion > ~0.3, fades in as expansion increases
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
                                val targetValue = if (expansion.value > 0.6f) 1f else 0f // Slightly harder to collapse by accident
                                
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
                                    val targetValue = if (expansion.value > 0.6f) 1f else 0f
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
    if (style == MiniPlayerStyle.FLOATING_PILL) {
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
    } else {
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

@Composable
private fun StandardMiniPlayer(
    song: com.suvojeet.suvmusic.core.model.Song,
    playerState: PlayerState,
    dominantColors: DominantColors,
    progress: Float,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    onTap: () -> Unit,
    userAlpha: Float = 0f,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onTap),
        color = Color.Transparent,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        // userAlpha 0f means 0% transparency (fully opaque, alpha 1f)
        // userAlpha 0.85f means 85% transparency (alpha 0.15f)
        val effectiveAlpha = 1f - userAlpha
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            dominantColors.primary.copy(alpha = effectiveAlpha),
                            dominantColors.secondary.copy(alpha = effectiveAlpha)
                        )
                    )
                )
        ) {
        Column {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art
                Box(
                    modifier = Modifier
                        .size(42.dp)
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
                    onClick = onPlayPause,
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
                    onClick = onNext,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = dominantColors.onBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (!playerState.isPlaying) {
                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = dominantColors.onBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Progress bar at bottom
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
}

@Composable
private fun PillMiniPlayer(
    song: com.suvojeet.suvmusic.core.model.Song,
    playerState: PlayerState,
    dominantColors: DominantColors,
    progress: Float,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    onTap: () -> Unit,
    userAlpha: Float = 0f,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(32.dp))
            .clickable(onClick = onTap),
        color = Color.Transparent,
        shape = RoundedCornerShape(32.dp),
        tonalElevation = 4.dp,
        shadowElevation = 6.dp
    ) {
        // userAlpha 0f (0%) -> effectiveAlpha 1f (Opaque)
        // userAlpha 0.85f (85%) -> effectiveAlpha 0.15f
        val effectiveAlpha = 1f - userAlpha
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            dominantColors.primary.copy(alpha = effectiveAlpha),
                            dominantColors.secondary.copy(alpha = effectiveAlpha)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art with Circular Progress
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Circular Progress
                    androidx.compose.material3.CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = dominantColors.accent,
                        trackColor = dominantColors.onBackground.copy(alpha = 0.2f),
                        strokeWidth = 2.dp,
                        strokeCap = StrokeCap.Round
                    )
                    
                    // Album Art
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (song.thumbnailUrl != null) {
                            AsyncImage(
                                model = song.thumbnailUrl,
                                contentDescription = song.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

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

                // Controls
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                        tint = dominantColors.onBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = dominantColors.onBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (!playerState.isPlaying) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(36.dp)
                    ) {
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
        }
    }
}
