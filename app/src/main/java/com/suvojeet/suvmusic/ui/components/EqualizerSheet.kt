package com.suvojeet.suvmusic.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

val EqPresets = mapOf(
    "Flat" to floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
    "Bass Boost" to floatArrayOf(6f, 5f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f),
    "Treble Boost" to floatArrayOf(0f, 0f, 0f, 0f, 0f, 2f, 4f, 5f, 6f, 7f),
    "Rock" to floatArrayOf(4f, 3f, 2f, -1f, -2f, -1f, 1f, 2f, 3f, 4f),
    "Pop" to floatArrayOf(-1f, 1f, 2f, 3f, 2f, 0f, -1f, -1f, -1f, -1f),
    "Jazz" to floatArrayOf(3f, 2f, 1f, 2f, -1f, -1f, 0f, 1f, 2f, 3f),
    "Classical" to floatArrayOf(4f, 3f, 2f, 1f, 0f, 0f, 1f, 2f, 3f, 4f),
    "Vocal" to floatArrayOf(-2f, -1f, 0f, 2f, 4f, 4f, 2f, 0f, -1f, -2f),
    "Electronic" to floatArrayOf(5f, 4f, 1f, 0f, -2f, 2f, 1f, 3f, 4f, 5f)
)

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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current
    
    if (isVisible) {
        // State management
        var isEnabled by remember(initialEnabled) { mutableStateOf(initialEnabled) }
        var selectedPreset by remember { mutableStateOf("Custom") }
        
        // Local bands to sync with UI
        val localBands = remember(initialBands) { 
            val arr = FloatArray(10)
            initialBands.copyInto(arr)
            arr
        }

        val frequencies = remember { 
            listOf("31Hz", "62Hz", "125Hz", "250Hz", "500Hz", "1kHz", "2kHz", "4kHz", "8kHz", "16kHz")
        }

        // Pre-calculate all MaterialTheme properties to avoid @Composable invocation issues in sub-scopes
        val colorScheme = MaterialTheme.colorScheme
        val typography = MaterialTheme.typography
        
        val surfaceColor = colorScheme.surface
        val onSurfaceColor = colorScheme.onSurface
        val onSurfaceVariantColor = colorScheme.onSurfaceVariant
        val surfaceVariantColor = colorScheme.surfaceVariant
        val outlineColor = colorScheme.outline
        
        val titleLargeStyle = typography.titleLarge
        val labelSmallStyle = typography.labelSmall
        val labelLargeStyle = typography.labelLarge
        
        val resetEnabledColor = dominantColor
        val resetDisabledColor = onSurfaceVariantColor.copy(alpha = 0.38f)
        
        val switchColors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = dominantColor,
            uncheckedThumbColor = outlineColor,
            uncheckedTrackColor = surfaceVariantColor
        )
        
        val curveBgColor = onSurfaceVariantColor.copy(alpha = 0.08f)
        val centerLineColor = onSurfaceColor.copy(alpha = 0.3f)
        
        val dbMarkingColor = onSurfaceVariantColor.copy(alpha = 0.7f)
        val dividerColor = onSurfaceColor.copy(alpha = 0.15f)
        
        val sliderColors = SliderDefaults.colors(
            thumbColor = dominantColor,
            activeTrackColor = dominantColor,
            inactiveTrackColor = onSurfaceVariantColor.copy(alpha = 0.25f),
            disabledThumbColor = onSurfaceColor.copy(alpha = 0.38f),
            disabledActiveTrackColor = onSurfaceColor.copy(alpha = 0.12f),
            disabledInactiveTrackColor = onSurfaceColor.copy(alpha = 0.12f)
        )

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = surfaceColor,
            contentColor = onSurfaceColor,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(onSurfaceVariantColor.copy(alpha = 0.4f))
                )
            },
            contentWindowInsets = { androidx.compose.foundation.layout.WindowInsets(0) }
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Equalizer",
                        style = titleLargeStyle,
                        fontWeight = FontWeight.ExtraBold,
                        color = onSurfaceColor
                    )
                    Text(
                        text = "10-Band Parametric",
                        style = labelSmallStyle,
                        color = onSurfaceVariantColor
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            onReset()
                            selectedPreset = "Flat"
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        enabled = isEnabled
                    ) {
                        Text(
                            text = "Reset",
                            style = labelLargeStyle,
                            color = if (isEnabled) resetEnabledColor else resetDisabledColor
                        )
                    }
                    
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { 
                            isEnabled = it
                            onEnabledChange(it)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        colors = switchColors
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Visual EQ Curve Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(curveBgColor)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val centerY = height / 2
                    val step = width / (localBands.size - 1)
                    
                    val path = Path()
                    val fillPath = Path()
                    
                    localBands.forEachIndexed { index, gain ->
                        // Map gain (-12 to 12) to Y coordinate
                        val y = centerY - (gain / 12f) * (height / 2.2f)
                        val x = index * step
                        
                        if (index == 0) {
                            path.moveTo(x, y)
                            fillPath.moveTo(x, height)
                            fillPath.lineTo(x, y)
                        } else {
                            // Smooth Cubic Bézier
                            val prevX = (index - 1) * step
                            val prevY = centerY - (localBands[index - 1] / 12f) * (height / 2.2f)
                            
                            val cp1x = prevX + (x - prevX) / 2
                            val cp2x = prevX + (x - prevX) / 2
                            
                            path.cubicTo(cp1x, prevY, cp2x, y, x, y)
                            fillPath.cubicTo(cp1x, prevY, cp2x, y, x, y)
                        }
                        
                        if (index == localBands.size - 1) {
                            fillPath.lineTo(x, height)
                            fillPath.close()
                        }
                    }
                    
                    // Draw fill gradient
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                dominantColor.copy(alpha = 0.5f),
                                dominantColor.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
                    
                    // Draw main curve
                    drawPath(
                        path = path,
                        color = if (isEnabled) dominantColor else onSurfaceVariantColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                        alpha = if (isEnabled) 1f else 0.5f
                    )
                    
                    // Draw center line (0dB)
                    drawLine(
                        color = centerLineColor,
                        start = androidx.compose.ui.geometry.Offset(0f, centerY),
                        end = androidx.compose.ui.geometry.Offset(width, centerY),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Presets Horizontal List
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    PresetChip(
                        name = "Custom",
                        isSelected = selectedPreset == "Custom",
                        onClick = { selectedPreset = "Custom" },
                        dominantColor = dominantColor
                    )
                }
                items(EqPresets.keys.toList()) { presetName ->
                    PresetChip(
                        name = presetName,
                        isSelected = selectedPreset == presetName,
                        onClick = {
                            selectedPreset = presetName
                            val presetBands = EqPresets[presetName]!!
                            presetBands.forEachIndexed { index, gain ->
                                localBands[index] = gain
                                onBandChange(index, gain)
                            }
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        dominantColor = dominantColor,
                        enabled = isEnabled
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Slider Section with dB markings
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .padding(horizontal = 8.dp)
            ) {
                // Background dB markings
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 40.dp, top = 20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    val dbLabels = listOf("+12", "+6", "0", "-6", "-12")
                    for (db in dbLabels) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = db,
                                style = labelSmallStyle,
                                color = dbMarkingColor,
                                modifier = Modifier.width(28.dp),
                                fontSize = 9.sp
                            )
                            HorizontalDivider(
                                color = dividerColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Vertical Sliders
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Spacer(modifier = Modifier.width(20.dp)) // Offset for labels
                    
                    for (index in localBands.indices) {
                        val gain = localBands[index]
                        var sliderValue by remember(gain) { mutableStateOf(gain) }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(44.dp)
                                .fillMaxHeight()
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .width(44.dp)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onDoubleTap = {
                                                if (isEnabled) {
                                                    sliderValue = 0f
                                                    onBandChange(index, 0f)
                                                    selectedPreset = "Custom"
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                            }
                                        )
                                    }
                            ) {
                                Slider(
                                    value = sliderValue,
                                    onValueChange = { newValue ->
                                        // Snap to 0
                                        val finalValue = if (abs(newValue) < 0.5f) 0f else newValue
                                        
                                        // Haptic on center snap or extremes
                                        if ((sliderValue != 0f && finalValue == 0f) || 
                                            (finalValue >= 11.9f && sliderValue < 11.9f) ||
                                            (finalValue <= -11.9f && sliderValue > -11.9f)) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                        
                                        sliderValue = finalValue
                                        onBandChange(index, finalValue)
                                        selectedPreset = "Custom"
                                    },
                                    valueRange = -12f..12f,
                                    enabled = isEnabled,
                                    colors = sliderColors,
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
                                style = labelSmallStyle,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isEnabled) onSurfaceColor else onSurfaceVariantColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                }
            }
        }
    }
}
}

@Composable
private fun PresetChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    dominantColor: Color,
    enabled: Boolean = true
) {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val labelLarge = MaterialTheme.typography.labelLarge

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) dominantColor else surfaceVariant.copy(alpha = 0.5f),
        contentColor = if (isSelected) Color.White else onSurfaceVariant,
        modifier = Modifier.alpha(if (enabled || isSelected) 1f else 0.5f)
    ) {
        Text(
            text = name,
            style = labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

// Add alpha modifier extension if not present in scope or imports
private fun Modifier.alpha(alpha: Float): Modifier = this.then(
    Modifier.graphicsLayer { this.alpha = alpha }
)