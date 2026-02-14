package com.suvojeet.suvmusic.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onBandChange: (Int, Float) -> Unit,
    onReset: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    dominantColor: Color,
    initialEnabled: Boolean,
    initialBands: FloatArray
) {
    if (!isVisible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // We use local state for immediate UI feedback while dragging, 
    // but initialize it from the passed values.
    var isEnabled by remember(initialEnabled) { mutableStateOf(initialEnabled) }
    
    // Create a local copy of bands for smooth slider movement
    val localBands = remember(initialBands) { 
        val arr = FloatArray(10)
        initialBands.copyInto(arr)
        arr
    }

    val frequencies = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        contentWindowInsets = { androidx.compose.foundation.layout.WindowInsets(0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Equalizer",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "10-Band Parametric",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.TextButton(
                        onClick = onReset,
                        enabled = isEnabled
                    ) {
                        Text("Reset", color = if (isEnabled) dominantColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f))
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))

                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { 
                            isEnabled = it
                            onEnabledChange(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = dominantColor,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Sliders Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                localBands.forEachIndexed { index, gain ->
                    // We need a unique state for each slider to make it update properly
                    var sliderValue by remember(gain) { mutableStateOf(gain) }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(40.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .height(200.dp)
                                .width(40.dp)
                        ) {
                            Slider(
                                value = sliderValue,
                                onValueChange = { 
                                    sliderValue = it
                                    onBandChange(index, it)
                                },
                                valueRange = -12f..12f,
                                enabled = isEnabled,
                                colors = SliderDefaults.colors(
                                    thumbColor = dominantColor,
                                    activeTrackColor = dominantColor,
                                    activeTickColor = Color.Transparent,
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    disabledThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                    disabledActiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                ),
                                modifier = Modifier
                                    .graphicsLayer {
                                        rotationZ = 270f
                                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                                    }
                                    .layout { measurable, constraints ->
                                        val placeable = measurable.measure(
                                            Constraints(
                                                minWidth = constraints.minHeight,
                                                maxWidth = constraints.maxHeight,
                                                minHeight = constraints.minWidth,
                                                maxHeight = constraints.maxWidth
                                            )
                                        )
                                        layout(placeable.height, placeable.width) {
                                            placeable.place(-placeable.width / 2 + placeable.height / 2, -placeable.height / 2 + placeable.width / 2)
                                        }
                                    }
                                    .width(200.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = frequencies[index],
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}