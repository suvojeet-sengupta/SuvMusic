package com.suvojeet.suvmusic.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.util.TimeUtil

/**
 * Dialog for trimming a song before setting it as a ringtone
 */
@Composable
fun RingtoneTrimmerDialog(
    isVisible: Boolean,
    song: Song,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long) -> Unit
) {
    if (!isVisible) return

    // Initialize range to 0..duration, but max 30 seconds if duration is long enough
    // standard ringtones are usually around 30 seconds
    val maxRingtoneDuration = 30000f
    val duration = if (song.duration > 0) song.duration.toFloat() else 180000f
    
    var range by remember(song.id) { 
        val end = if (duration > maxRingtoneDuration) maxRingtoneDuration else duration
        mutableStateOf(0f..end) 
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Trim Ringtone") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select the part of \"${song.title}\" to use as ringtone. (Max 30s recommended)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
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
                        // Optional: Limit selection to max 60 seconds if you want
                        range = it 
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
                    onConfirm(range.start.toLong(), range.endInclusive.toLong())
                }
            ) {
                Text("Set as Ringtone")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
