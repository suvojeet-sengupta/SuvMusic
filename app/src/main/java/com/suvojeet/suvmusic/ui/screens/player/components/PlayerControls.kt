package com.suvojeet.suvmusic.ui.screens.player.components

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.data.model.RepeatMode
import com.suvojeet.suvmusic.ui.components.DominantColors
import com.suvojeet.suvmusic.util.dpadFocusable

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
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable (isPressed: Boolean) -> Unit
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Animate the background alpha for smooth fade in/out
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.18f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "m3ePressedAlpha"
    )
    
    // "Jump" / Scale effect
    // Apple Music style: noticeable scale down with a bit of bounce (spring)
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.82f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,   // M3E uses lower for more expressive bounce
            stiffness = Spring.StiffnessLow                // smoother, more physical feel
        ),
        label = "m3eButtonScale"
    )
    
    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            // Use dpadFocusable for focus handling only, NOT click handling
            .dpadFocusable(
                onClick = null, 
                shape = CircleShape,
                focusedScale = 1.1f,
                borderColor = Color.White
            )
            .clip(CircleShape)
            // Manual click handling to remove ripple (indication = null)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = backgroundAlpha)),
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
    dominantColors: DominantColors,
    compact: Boolean = false
) {
    // Adaptive sizing for compact (16:9) vs standard screens
    val playSize = if (compact) 56.dp else 80.dp
    val playIconSize = if (compact) 40.dp else 56.dp
    val skipSize = if (compact) 40.dp else 56.dp
    val skipIconSize = if (compact) 28.dp else 40.dp
    val secondarySize = if (compact) 36.dp else 48.dp
    val secondaryIconSize = if (compact) 22.dp else 28.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle - M3E IconToggleButton
        IconToggleButton(
            checked = shuffleEnabled,
            onCheckedChange = { onShuffleToggle() },
            modifier = Modifier.size(secondarySize),
            colors = IconButtonDefaults.iconToggleButtonColors(
                containerColor = Color.Transparent,
                contentColor = dominantColors.onBackground.copy(alpha = 0.6f),
                checkedContainerColor = dominantColors.accent.copy(alpha = 0.18f),
                checkedContentColor = dominantColors.accent
            )
        ) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = "Shuffle",
                modifier = Modifier.size(secondaryIconSize)
            )
        }

        // Previous
        AppleMusicButton(onClick = onPrevious, size = skipSize) { _ ->
            Icon(
                imageVector = SkipPrevious,
                contentDescription = "Previous",
                tint = dominantColors.onBackground,
                modifier = Modifier.size(skipIconSize)
            )
        }

        // Play/Pause - Large button with bounce animation
        AppleMusicButton(onClick = onPlayPause, size = playSize, isLarge = true) { _ ->
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    (scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
                        + fadeIn()) togetherWith
                    (scaleOut() + fadeOut())
                },
                label = "playPauseSwap"
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    tint = dominantColors.onBackground,
                    modifier = Modifier.size(playIconSize)
                )
            }
        }

        // Next
        AppleMusicButton(onClick = onNext, size = skipSize) { _ ->
            Icon(
                imageVector = SkipNext,
                contentDescription = "Next",
                tint = dominantColors.onBackground,
                modifier = Modifier.size(skipIconSize)
            )
        }

        // Repeat - M3E IconToggleButton
        IconToggleButton(
            checked = repeatMode != RepeatMode.OFF,
            onCheckedChange = { onRepeatToggle() },
            modifier = Modifier.size(secondarySize),
            colors = IconButtonDefaults.iconToggleButtonColors(
                containerColor = Color.Transparent,
                contentColor = dominantColors.onBackground.copy(alpha = 0.6f),
                checkedContainerColor = dominantColors.accent.copy(alpha = 0.18f),
                checkedContentColor = dominantColors.accent
            )
        ) {
            AnimatedContent(
                targetState = repeatMode,
                transitionSpec = {
                    scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn() togetherWith
                    scaleOut() + fadeOut()
                },
                label = "repeatIcon"
            ) { mode ->
                Icon(
                    imageVector = when (mode) {
                        RepeatMode.ONE -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    },
                    contentDescription = "Repeat",
                    modifier = Modifier.size(secondaryIconSize)
                )
            }
        }
    }
}

