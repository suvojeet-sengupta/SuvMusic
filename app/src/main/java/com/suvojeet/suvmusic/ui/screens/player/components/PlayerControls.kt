package com.suvojeet.suvmusic.ui.screens.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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

        // Previous - Apple Music style (double triangles)
        Icon(
            imageVector = SkipPrevious,
            contentDescription = "Previous",
            tint = dominantColors.onBackground,
            modifier = Modifier
                .size(48.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPrevious
                )
        )

        // Play/Pause - Apple Music style (large icon without background)
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = dominantColors.onBackground,
            modifier = Modifier
                .size(72.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPlayPause
                )
        )

        // Next - Apple Music style (double triangles)
        Icon(
            imageVector = SkipNext,
            contentDescription = "Next",
            tint = dominantColors.onBackground.copy(alpha = 0.6f),
            modifier = Modifier
                .size(48.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onNext
                )
        )

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
