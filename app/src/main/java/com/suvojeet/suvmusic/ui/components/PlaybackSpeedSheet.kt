package com.suvojeet.suvmusic.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.suvmusic.ui.theme.SquircleShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSpeedSheet(
    isVisible: Boolean,
    currentSpeed: Float,
    currentPitch: Float,
    onDismiss: () -> Unit,
    onApply: (speed: Float, pitch: Float) -> Unit,
    dominantColors: DominantColors? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var sliderSpeed by remember(currentSpeed) { mutableFloatStateOf(currentSpeed) }
    var sliderPitch by remember(currentPitch) { mutableFloatStateOf(currentPitch) }
    
    val backgroundColor = dominantColors?.primary?.copy(alpha = 0.98f) ?: MaterialTheme.colorScheme.surface
    val contentColor = dominantColors?.onBackground ?: MaterialTheme.colorScheme.onSurface
    val accentColor = dominantColors?.accent ?: MaterialTheme.colorScheme.primary

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = backgroundColor,
            contentWindowInsets = { WindowInsets(0) },
            dragHandle = null,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 32.dp)
            ) {
                // Modern Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 16.dp, start = 24.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Playback Controls",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = contentColor
                        )
                        Text(
                            text = "Adjust speed and pitch",
                            style = MaterialTheme.typography.labelMedium,
                            color = contentColor.copy(alpha = 0.5f)
                        )
                    }
                    
                    // Reset Button
                    FilledTonalButton(
                        onClick = {
                            sliderSpeed = 1.0f
                            sliderPitch = 1.0f
                            onApply(1.0f, 1.0f)
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = contentColor.copy(alpha = 0.05f),
                            contentColor = contentColor
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        shape = SquircleShape
                    ) {
                        Icon(Icons.Default.RestartAlt, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Speed Control
                ModernSliderSection(
                    icon = Icons.Default.Speed,
                    label = "Speed",
                    value = sliderSpeed,
                    valueDisplay = String.format("%.2fx", sliderSpeed),
                    valueRange = 0.25f..3.0f,
                    onValueChange = { 
                        sliderSpeed = it 
                        onApply(it, sliderPitch)
                    },
                    accentColor = accentColor,
                    contentColor = contentColor
                )
                
                Spacer(modifier = Modifier.height(32.dp))

                // Pitch Control
                ModernSliderSection(
                    icon = Icons.Default.GraphicEq,
                    label = "Pitch",
                    value = sliderPitch,
                    valueDisplay = String.format("%.2fx", sliderPitch),
                    valueRange = 0.5f..2.0f,
                    onValueChange = { 
                        sliderPitch = it
                        onApply(sliderSpeed, it)
                    },
                    accentColor = accentColor,
                    contentColor = contentColor
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ModernSliderSection(
    icon: ImageVector,
    label: String,
    value: Float,
    valueDisplay: String,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    accentColor: Color,
    contentColor: Color
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(SquircleShape)
                    .background(accentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, modifier = Modifier.size(18.dp), tint = accentColor)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = contentColor,
                modifier = Modifier.weight(1f)
            )
            Surface(
                color = accentColor.copy(alpha = 0.1f),
                shape = CircleShape
            ) {
                Text(
                    text = valueDisplay,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = contentColor.copy(alpha = 0.1f)
            )
        )
    }
}
