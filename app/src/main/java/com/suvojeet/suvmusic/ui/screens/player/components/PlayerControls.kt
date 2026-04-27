package com.suvojeet.suvmusic.ui.screens.player.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.core.model.RepeatMode
import com.suvojeet.suvmusic.ui.components.DominantColors
import com.suvojeet.suvmusic.ui.components.bounceClick
import com.suvojeet.suvmusic.ui.components.primitives.AnimatedPlayerButton

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
    // YT Music style sizes
    val playSize = if (compact) 64.dp else 84.dp
    val playIconSize = if (compact) 44.dp else 56.dp
    val skipIconSize = if (compact) 32.dp else 40.dp
    val secondaryIconSize = if (compact) 24.dp else 28.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle
        AnimatedPlayerButton(
            icon = Icons.Default.Shuffle,
            contentDescription = "Shuffle",
            tint = if (shuffleEnabled) dominantColors.accent else dominantColors.onBackground.copy(alpha = 0.6f),
            onClick = onShuffleToggle,
            size = 48.dp,
            iconSize = secondaryIconSize
        )

        // Previous
        AnimatedPlayerButton(
            icon = Icons.Default.SkipPrevious,
            contentDescription = "Previous",
            tint = dominantColors.onBackground,
            onClick = onPrevious,
            size = 56.dp,
            iconSize = skipIconSize
        )

        // Play/Pause — keeps its animated background ring + icon swap, so stays bespoke.
        Box(
            modifier = Modifier
                .size(playSize)
                .bounceClick(onClick = onPlayPause),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(dominantColors.onBackground.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
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
        }

        // Next
        AnimatedPlayerButton(
            icon = Icons.Default.SkipNext,
            contentDescription = "Next",
            tint = dominantColors.onBackground,
            onClick = onNext,
            size = 56.dp,
            iconSize = skipIconSize
        )

        // Repeat — keeps AnimatedContent for icon swap between Repeat/RepeatOne modes.
        Box(
            modifier = Modifier
                .size(48.dp)
                .bounceClick(onClick = onRepeatToggle),
            contentAlignment = Alignment.Center
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
                    modifier = Modifier.size(secondaryIconSize),
                    tint = if (mode != RepeatMode.OFF) dominantColors.accent else dominantColors.onBackground.copy(alpha = 0.6f)
                )
            }
        }
    }
}

