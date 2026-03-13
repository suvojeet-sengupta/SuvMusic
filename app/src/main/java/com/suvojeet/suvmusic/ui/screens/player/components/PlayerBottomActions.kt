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
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
        ButtonGroup(
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(dominantColors.onBackground.copy(alpha = 0.08f))
                .padding(horizontal = containerPadding, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
        ) {
            // Lyrics button
            val lyricsInteractionSource = remember { MutableInteractionSource() }
            clickable(onClick = onLyricsClick, weight = 1f, interactionSource = lyricsInteractionSource) {
                IconButton(onClick = onLyricsClick, interactionSource = lyricsInteractionSource) {
                    Icon(
                        imageVector = Icons.Default.Lyrics,
                        contentDescription = "Lyrics",
                        tint = dominantColors.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.size(iconSize)
                    )
                }
            }

            // Download Button
            val downloadInteractionSource = remember { MutableInteractionSource() }
            clickable(onClick = onDownloadClick, weight = 1f, interactionSource = downloadInteractionSource) {
                IconButton(onClick = onDownloadClick, interactionSource = downloadInteractionSource) {
                    when(downloadState) {
                        com.suvojeet.suvmusic.data.model.DownloadState.DOWNLOADING -> {
                            LoadingIndicator(
                                modifier = Modifier.size(iconSize),
                                color = dominantColors.accent
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
            }

            // Video mode toggle
            if (isYouTubeSong) {
                val videoInteractionSource = remember { MutableInteractionSource() }
                clickable(onClick = onVideoToggle, weight = 1f, interactionSource = videoInteractionSource) {
                    IconButton(onClick = onVideoToggle, interactionSource = videoInteractionSource) {
                        AnimatedContent(
                            targetState = isVideoMode,
                            transitionSpec = {
                                scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn() togetherWith
                                scaleOut() + fadeOut()
                            },
                            label = "videoModeToggle"
                        ) { videoMode ->
                            Icon(
                                imageVector = if (videoMode) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                contentDescription = if (videoMode) "Audio Mode" else "Video Mode",
                                tint = if (videoMode) dominantColors.accent else dominantColors.onBackground.copy(alpha = 0.7f),
                                modifier = Modifier.size(iconSize)
                            )
                        }
                    }
                }
            }

            // Cast button
            val castInteractionSource = remember { MutableInteractionSource() }
            clickable(onClick = onCastClick, weight = 1f, interactionSource = castInteractionSource) {
                IconButton(onClick = onCastClick, interactionSource = castInteractionSource) {
                    Icon(
                        imageVector = Icons.Default.Devices,
                        contentDescription = "Output Device",
                        tint = dominantColors.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.size(iconSize)
                    )
                }
            }

            // Queue button
            val queueInteractionSource = remember { MutableInteractionSource() }
            clickable(onClick = onQueueClick, weight = 1f, interactionSource = queueInteractionSource) {
                IconButton(onClick = onQueueClick, interactionSource = queueInteractionSource) {
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
}
