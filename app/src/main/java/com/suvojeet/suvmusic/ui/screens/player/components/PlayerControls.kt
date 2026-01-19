package com.suvojeet.suvmusic.ui.screens.player.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.data.model.RepeatMode
import com.suvojeet.suvmusic.ui.components.DominantColors

/**
 * Custom skip previous icon (double left triangles) like Apple Music
 */
private val SkipPrevious: ImageVector
    get() = Icons.Default.FastRewind

/**
 * Custom skip next icon (double right triangles) like Apple Music
 */
private val SkipNext: ImageVector
    get() = Icons.Default.FastForward

/**
 * Animated button with Apple Music style pressed effect.
 * Shows a dark shadow/glow on press with smooth animation.
 */
@Composable
private fun AppleMusicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 36.dp,
    isLarge: Boolean = false,
    content: @Composable (isPressed: Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    
    // Animate the background alpha for smooth fade in/out
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.25f else 0f,
        animationSpec = tween(durationMillis = if (isPressed) 50 else 200),
        label = "pressedAlpha"
    )
    
    // Slight scale effect on press
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "pressedScale"
    )
    
    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = backgroundAlpha))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        try {
                            awaitRelease()
                        } finally {
                            isPressed = false
                        }
                        onClick()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        content(isPressed)
    }
}

@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    dominantColors: DominantColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle
        IconButton(
            onClick = onShuffleToggle,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = "Shuffle",
                tint = if (shuffleEnabled) dominantColors.accent else dominantColors.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }

        // Previous - Apple Music style with press animation
        AppleMusicButton(
            onClick = onPrevious,
            size = 56.dp,
            iconSize = 40.dp
        ) { _ ->
            Icon(
                imageVector = SkipPrevious,
                contentDescription = "Previous",
                tint = dominantColors.onBackground,
                modifier = Modifier.size(40.dp)
            )
        }

        // Play/Pause - Large button with press animation
        AppleMusicButton(
            onClick = onPlayPause,
            size = 80.dp,
            iconSize = 56.dp,
            isLarge = true
        ) { _ ->
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = dominantColors.onBackground,
                modifier = Modifier.size(56.dp)
            )
        }

        // Next - Apple Music style with press animation
        AppleMusicButton(
            onClick = onNext,
            size = 56.dp,
            iconSize = 40.dp
        ) { _ ->
            Icon(
                imageVector = SkipNext,
                contentDescription = "Next",
                tint = dominantColors.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.size(40.dp)
            )
        }

        // Repeat
        IconButton(
            onClick = onRepeatToggle,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = when (repeatMode) {
                    RepeatMode.ONE -> Icons.Default.RepeatOne
                    else -> Icons.Default.Repeat
                },
                contentDescription = "Repeat",
                tint = if (repeatMode != RepeatMode.OFF) dominantColors.accent else dominantColors.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
