package com.suvojeet.suvmusic.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Bottom sheet for adjusting Playback Speed and Pitch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSpeedSheet(
    isVisible: Boolean,
    currentSpeed: Float,
    currentPitch: Float,
    onDismiss: () -> Unit,
    onApply: (speed: Float, pitch: Float) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Local state for smooth slider interaction
    var sliderSpeed by remember(currentSpeed) { mutableFloatStateOf(currentSpeed) }
    var sliderPitch by remember(currentPitch) { mutableFloatStateOf(currentPitch) }

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            contentWindowInsets = { androidx.compose.foundation.layout.WindowInsets(0) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Playback Controls",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        text = "Speed & Tempo",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Reset Button
                    IconButton(
                        onClick = {
                            sliderSpeed = 1.0f
                            sliderPitch = 1.0f
                            onApply(1.0f, 1.0f)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Speed Control
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Speed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = String.format("%.2fx", sliderSpeed),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Slider(
                        value = sliderSpeed,
                        onValueChange = { 
                            sliderSpeed = it 
                            // Apply changes live? Or postpone? 
                            // Usually better to apply live for feedback, but debounce might be needed.
                            // For local playback, live is usually fine.
                            onApply(it, sliderPitch)
                        },
                        valueRange = 0.25f..3.0f,
                        steps = 0, // Continuous
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                // Pitch Control
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                             imageVector = Icons.Default.GraphicEq,
                             contentDescription = null,
                             modifier = Modifier.size(18.dp),
                             tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = "Pitch",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = String.format("%.2fx", sliderPitch),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Slider(
                        value = sliderPitch,
                        onValueChange = { 
                            sliderPitch = it
                            onApply(sliderSpeed, it)
                        },
                        valueRange = 0.5f..2.0f,
                        steps = 0,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
