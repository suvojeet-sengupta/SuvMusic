package com.suvojeet.suvmusic.ui.screens.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.ui.components.DominantColors

@Composable
fun BottomActions(
    onLyricsClick: () -> Unit,
    onCastClick: () -> Unit,
    onQueueClick: () -> Unit,
    dominantColors: DominantColors,
    isYouTubeSong: Boolean = false,
    isVideoMode: Boolean = false,
    onVideoToggle: () -> Unit = {},
    compact: Boolean = false
) {
    val buttonModifier = if (compact) Modifier.size(36.dp) else Modifier
    val iconSize = if (compact) 20.dp else 24.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(onClick = onLyricsClick, modifier = buttonModifier) {
            Icon(
                imageVector = Icons.Default.Lyrics,
                contentDescription = "Lyrics",
                tint = dominantColors.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.size(iconSize)
            )
        }

        // Video mode toggle - for all songs (searches YouTube if not native)
        IconButton(onClick = onVideoToggle, modifier = buttonModifier) {
            Icon(
                imageVector = if (isVideoMode) Icons.Default.Videocam else Icons.Default.VideocamOff,
                contentDescription = if (isVideoMode) "Switch to Audio" else "Switch to Video",
                tint = if (isVideoMode) dominantColors.accent else dominantColors.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.size(iconSize)
            )
        }

        IconButton(onClick = onCastClick, modifier = buttonModifier) {
            Icon(
                imageVector = Icons.Default.Devices,
                contentDescription = "Output Device",
                tint = dominantColors.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.size(iconSize)
            )
        }

        IconButton(onClick = onQueueClick, modifier = buttonModifier) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = "Queue",
                tint = dominantColors.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
