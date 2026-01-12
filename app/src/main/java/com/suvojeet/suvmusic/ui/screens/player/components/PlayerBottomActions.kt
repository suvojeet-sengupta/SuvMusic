package com.suvojeet.suvmusic.ui.screens.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.suvojeet.suvmusic.ui.components.DominantColors

@Composable
fun BottomActions(
    onLyricsClick: () -> Unit,
    onCastClick: () -> Unit,
    onQueueClick: () -> Unit,
    dominantColors: DominantColors,
    isYouTubeSong: Boolean = false,
    isVideoMode: Boolean = false,
    onVideoToggle: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(onClick = onLyricsClick) {
            Icon(
                imageVector = Icons.Default.Lyrics,
                contentDescription = "Lyrics",
                tint = dominantColors.onBackground.copy(alpha = 0.6f)
            )
        }

        // Video mode toggle - only for YouTube songs
        if (isYouTubeSong) {
            IconButton(onClick = onVideoToggle) {
                Icon(
                    imageVector = if (isVideoMode) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    contentDescription = if (isVideoMode) "Switch to Audio" else "Switch to Video",
                    tint = if (isVideoMode) dominantColors.accent else dominantColors.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        IconButton(onClick = onCastClick) {
            Icon(
                imageVector = Icons.Default.Cast,
                contentDescription = "Cast",
                tint = dominantColors.onBackground.copy(alpha = 0.6f)
            )
        }

        IconButton(onClick = onQueueClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = "Queue",
                tint = dominantColors.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}
