package com.suvojeet.suvmusic.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Playback speed options.
 */
enum class PlaybackSpeed(val value: Float, val label: String) {
    SPEED_0_5(0.5f, "0.5x"),
    SPEED_0_75(0.75f, "0.75x"),
    SPEED_1_0(1.0f, "1.0x (Normal)"),
    SPEED_1_25(1.25f, "1.25x"),
    SPEED_1_5(1.5f, "1.5x"),
    SPEED_1_75(1.75f, "1.75x"),
    SPEED_2_0(2.0f, "2.0x"),
    SPEED_CUSTOM(-1f, "Custom")
}

/**
 * Bottom sheet for selecting playback speed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSpeedSheet(
    isVisible: Boolean,
    currentSpeed: Float,
    onDismiss: () -> Unit,
    onSpeedSelected: (Float) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showCustomDialog by remember { mutableStateOf(false) }
    var customSpeedInput by remember { mutableStateOf("") }
    
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
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
                        contentDescription = "Playback Speed",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        text = "Playback Speed",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Speed options
                PlaybackSpeed.entries.forEach { speed ->
                    val isCustom = speed == PlaybackSpeed.SPEED_CUSTOM
                    
                    // Determine if this option is selected
                    val isSelected = if (isCustom) {
                        // Custom is selected if current speed doesn't match any standard preset
                        PlaybackSpeed.entries.none { 
                            it != PlaybackSpeed.SPEED_CUSTOM && abs(currentSpeed - it.value) < 0.01f 
                        }
                    } else {
                        abs(currentSpeed - speed.value) < 0.01f
                    }
                    
                    val displayText = if (isCustom && isSelected) {
                        "Custom (${currentSpeed}x)"
                    } else {
                        speed.label
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                if (isCustom) {
                                    customSpeedInput = currentSpeed.toString()
                                    showCustomDialog = true
                                } else {
                                    onSpeedSelected(speed.value)
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("Custom Playback Speed") },
            text = {
                Column {
                    Text("Enter a value between 0.1 and 5.0")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customSpeedInput,
                        onValueChange = { customSpeedInput = it },
                        label = { Text("Speed") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val speed = customSpeedInput.toFloatOrNull()
                        if (speed != null && speed in 0.1f..5.0f) {
                            onSpeedSelected(speed)
                            showCustomDialog = false
                        }
                    }
                ) {
                    Text("Set")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
