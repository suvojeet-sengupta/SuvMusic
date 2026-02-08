package com.suvojeet.suvmusic.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.util.TimeUtil

/**
 * Dialog for trimming a song before setting it as a ringtone
 */
@Composable
fun RingtoneTrimmerDialog(
    isVisible: Boolean,
    song: Song,
    onDismiss: () -> Unit,
    onResolveStreamUrl: suspend (String) -> String?,
    onConfirm: (Long, Long) -> Unit
) {
    if (!isVisible) return

    val context = LocalContext.current
    
    // ExoPlayer for preview
    val exoPlayer = remember { 
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }
    
    var isPlaying by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Initialize range to 0..duration, but max 30 seconds if duration is long enough
    val maxRingtoneDuration = 30000f
    val duration = if (song.duration > 0) song.duration.toFloat() else 180000f
    
    var range by remember(song.id) { 
        val end = if (duration > maxRingtoneDuration) maxRingtoneDuration else duration
        mutableStateOf(0f..end) 
    }

    // Effect to handle player lifecycle and stream URL
    LaunchedEffect(song.id, isVisible) {
        if (isVisible) {
            isLoading = true
            val uri = if (song.localUri != null) {
                song.localUri
            } else {
                val resolvedUrl = onResolveStreamUrl(song.id)
                resolvedUrl?.let { android.net.Uri.parse(it) }
            }
            
            if (uri != null) {
                val mediaItem = MediaItem.Builder()
                    .setUri(uri)
                    .build()
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
            }
            isLoading = false
        }
    }

    // Effect to stop player on dismiss
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Monitor playback position to stop at the end of the range
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying) {
                if (exoPlayer.currentPosition >= range.endInclusive.toLong()) {
                    exoPlayer.pause()
                    isPlaying = false
                }
                kotlinx.coroutines.delay(100)
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            exoPlayer.stop()
            onDismiss()
        },
        title = { Text("Trim Ringtone") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select the part of \"${song.title}\" to use as ringtone. (Max 30s recommended)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Preview Button
                Box(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        IconButton(
                            onClick = {
                                if (isPlaying) {
                                    exoPlayer.pause()
                                    isPlaying = false
                                } else {
                                    exoPlayer.seekTo(range.start.toLong())
                                    exoPlayer.play()
                                    isPlaying = true
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause Preview" else "Play Preview",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = TimeUtil.formatTime(range.start.toLong()),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = TimeUtil.formatTime(range.endInclusive.toLong()),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                RangeSlider(
                    value = range,
                    onValueChange = { 
                        range = it 
                        // If user moves range while playing, maybe stop or seek
                        if (isPlaying) {
                            exoPlayer.pause()
                            isPlaying = false
                        }
                    },
                    valueRange = 0f..duration,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val selectedDuration = (range.endInclusive - range.start).toLong()
                Text(
                    text = "Selected duration: ${TimeUtil.formatTime(selectedDuration)}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = if (selectedDuration > 40000) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    exoPlayer.stop()
                    onConfirm(range.start.toLong(), range.endInclusive.toLong())
                }
            ) {
                Text("Set as Ringtone")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                exoPlayer.stop()
                onDismiss()
            }) {
                Text("Cancel")
            }
        }
    )
}