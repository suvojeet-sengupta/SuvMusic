package com.suvojeet.suvmusic.ui.screens.player.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.ui.components.M3ELoadingIndicator
import com.suvojeet.suvmusic.ui.components.DominantColors
import com.suvojeet.suvmusic.data.model.DownloadState

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BottomActions(
    onLyricsClick: () -> Unit,
    onCastClick: () -> Unit,
    onQueueClick: () -> Unit,
    onDownloadClick: () -> Unit,
    downloadState: DownloadState,
    dominantColors: DominantColors,
    isYouTubeSong: Boolean = false,
    isVideoMode: Boolean = false,
    onVideoToggle: () -> Unit = {},
    compact: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Lyrics
        M3EBottomActionItem(
            icon = Icons.Default.Lyrics,
            label = "Lyrics",
            onClick = onLyricsClick,
            dominantColors = dominantColors
        )

        // Download
        M3EBottomActionItem(
            icon = when(downloadState) {
                DownloadState.DOWNLOADED -> Icons.Filled.CheckCircle
                DownloadState.FAILED -> Icons.Filled.Error
                else -> Icons.Filled.Download
            },
            label = "Download",
            onClick = onDownloadClick,
            dominantColors = dominantColors,
            isLoading = downloadState == DownloadState.DOWNLOADING,
            tint = if (downloadState == DownloadState.DOWNLOADED) dominantColors.accent else dominantColors.onBackground.copy(alpha = 0.7f)
        )

        // Video Toggle
        M3EBottomActionItem(
            icon = if (isVideoMode) Icons.Default.Videocam else Icons.Default.VideocamOff,
            label = if (isVideoMode) "Video" else "Audio",
            onClick = onVideoToggle,
            dominantColors = dominantColors,
            tint = if (isVideoMode) dominantColors.accent else dominantColors.onBackground.copy(alpha = 0.7f)
        )

        // Cast/Devices
        M3EBottomActionItem(
            icon = Icons.Default.Devices,
            label = "Devices",
            onClick = onCastClick,
            dominantColors = dominantColors
        )

        // Queue
        M3EBottomActionItem(
            icon = Icons.AutoMirrored.Filled.QueueMusic,
            label = "Queue",
            onClick = onQueueClick,
            dominantColors = dominantColors
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun M3EBottomActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    dominantColors: DominantColors,
    isLoading: Boolean = false,
    tint: androidx.compose.ui.graphics.Color = dominantColors.onBackground.copy(alpha = 0.7f)
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = Modifier.size(44.dp),
            shape = MaterialTheme.shapes.medium,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = dominantColors.onBackground.copy(alpha = 0.1f),
                contentColor = tint
            )
        ) {
            if (isLoading) {
                M3ELoadingIndicator(
                    modifier = Modifier.size(20.dp),
                    color = tint
                )
            } else {
                Icon(icon, label, modifier = Modifier.size(20.dp))
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = dominantColors.onBackground.copy(alpha = 0.6f)
        )
    }
}
