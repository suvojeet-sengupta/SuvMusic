package com.suvojeet.suvmusic.ui.screens.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.ui.components.DominantColors

@Composable
fun BottomActions(
    onLyricsClick: () -> Unit,
    onCastClick: () -> Unit,
    onQueueClick: () -> Unit,
    onDownloadClick: () -> Unit,
    downloadState: com.suvojeet.suvmusic.data.model.DownloadState,
    dominantColors: DominantColors,
    isYouTubeSong: Boolean = false,
    isVideoMode: Boolean = false,
    onVideoToggle: () -> Unit = {},
    compact: Boolean = false
) {
    val iconSize = if (compact) 20.dp else 22.dp
    val containerPadding = if (compact) 4.dp else 6.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(dominantColors.onBackground.copy(alpha = 0.08f))
                .padding(horizontal = containerPadding),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onLyricsClick, modifier = Modifier.size(if (compact) 36.dp else 44.dp)) {
                Icon(
                    imageVector = Icons.Default.Lyrics,
                    contentDescription = "Lyrics",
                    tint = dominantColors.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.size(iconSize)
                )
            }

            // Download Button
            IconButton(
                onClick = onDownloadClick,
                modifier = Modifier.size(if (compact) 36.dp else 44.dp)
            ) {
                when(downloadState) {
                    com.suvojeet.suvmusic.data.model.DownloadState.DOWNLOADING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(iconSize),
                            color = dominantColors.accent,
                            strokeWidth = 2.dp
                        )
                    }
                    com.suvojeet.suvmusic.data.model.DownloadState.DOWNLOADED -> {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Downloaded",
                            tint = dominantColors.accent,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                    com.suvojeet.suvmusic.data.model.DownloadState.FAILED -> {
                        Icon(
                            imageVector = Icons.Filled.Error,
                            contentDescription = "Retry Download",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = "Download",
                            tint = dominantColors.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }
            }

            // Video mode toggle
            IconButton(onClick = onVideoToggle, modifier = Modifier.size(if (compact) 36.dp else 44.dp)) {
                Icon(
                    imageVector = if (isVideoMode) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    contentDescription = if (isVideoMode) "Audio Mode" else "Video Mode",
                    tint = if (isVideoMode) dominantColors.accent else dominantColors.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.size(iconSize)
                )
            }

            IconButton(onClick = onCastClick, modifier = Modifier.size(if (compact) 36.dp else 44.dp)) {
                Icon(
                    imageVector = Icons.Default.Devices,
                    contentDescription = "Output Device",
                    tint = dominantColors.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.size(iconSize)
                )
            }

            IconButton(onClick = onQueueClick, modifier = Modifier.size(if (compact) 36.dp else 44.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = "Queue",
                    tint = dominantColors.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}
