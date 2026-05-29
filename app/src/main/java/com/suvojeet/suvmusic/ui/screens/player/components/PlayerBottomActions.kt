package com.suvojeet.suvmusic.ui.screens.player.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.suvmusic.core.model.DownloadState
import com.suvojeet.suvmusic.ui.components.DominantColors

@Composable
fun BottomActions(
    onLyricsClick: () -> Unit,
    onCastClick: () -> Unit,
    onQueueClick: () -> Unit,
    onRelatedClick: () -> Unit,
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
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomTabButton(
            label = "UP NEXT",
            onClick = onQueueClick,
            dominantColors = dominantColors
        )

        BottomTabButton(
            label = "LYRICS",
            onClick = onLyricsClick,
            dominantColors = dominantColors
        )

        BottomTabButton(
            label = "RELATED",
            onClick = onRelatedClick,
            dominantColors = dominantColors,
            enabled = true
        )

        // Download icon: previously the BottomActions row received
        // `downloadState` and `onDownloadClick` but never rendered them,
        // so the YT-Music-style player had no visual feedback that a
        // download had completed — users had to open the Downloads tab
        // to confirm. The icon now animates between four states
        // (idle / downloading / done / failed) so completion is
        // immediately obvious in the player itself.
        DownloadAction(
            downloadState = downloadState,
            onClick = onDownloadClick,
            dominantColors = dominantColors,
        )
    }
}

/**
 * State-driven download icon. AnimatedContent gives the state change a
 * subtle scale/fade so the user notices the moment a download finishes
 * without us needing a separate toast.
 */
@Composable
private fun DownloadAction(
    downloadState: DownloadState,
    onClick: () -> Unit,
    dominantColors: DominantColors,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(
                // Block taps while in flight — repeated clicks during a
                // download just spawn duplicate jobs that get deduped at
                // the repo layer; better to short-circuit here.
                enabled = downloadState != DownloadState.DOWNLOADING,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = downloadState,
            transitionSpec = {
                (scaleIn(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(180)))
                    .togetherWith(scaleOut(tween(120)) + fadeOut(tween(120)))
            },
            label = "download-state",
        ) { state ->
            when (state) {
                DownloadState.DOWNLOADING -> {
                    // Indeterminate spinner — the per-song progress
                    // fraction lives on the Downloads screen, the player
                    // just needs to communicate "in progress".
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = dominantColors.accent,
                    )
                }
                DownloadState.DOWNLOADED -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Downloaded",
                        tint = dominantColors.accent,
                        modifier = Modifier.size(22.dp),
                    )
                }
                DownloadState.FAILED -> {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Download failed — tap to retry",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp),
                    )
                }
                DownloadState.NOT_DOWNLOADED -> {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download",
                        tint = dominantColors.onBackground.copy(alpha = 0.8f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomTabButton(
    label: String,
    onClick: () -> Unit,
    dominantColors: DominantColors,
    enabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) dominantColors.onBackground.copy(alpha = 0.8f) else dominantColors.onBackground.copy(alpha = 0.3f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}
