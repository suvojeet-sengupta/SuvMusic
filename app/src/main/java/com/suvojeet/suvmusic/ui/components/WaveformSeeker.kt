package com.suvojeet.suvmusic.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.suvojeet.suvmusic.ui.components.seekbar.ClassicStyle
import com.suvojeet.suvmusic.ui.components.seekbar.DotsStyle
import com.suvojeet.suvmusic.ui.components.seekbar.GradientBarStyle
import com.suvojeet.suvmusic.ui.components.seekbar.WaveLineStyle
import com.suvojeet.suvmusic.ui.components.seekbar.WaveformStyle
import kotlin.random.Random

/**
 * Seekbar style options
 */
enum class SeekbarStyle {
    WAVEFORM,      // Animated waveform bars
    WAVE_LINE,     // Sine wave line
    CLASSIC,       // Simple progress bar
    DOTS,          // Animated dots
    GRADIENT_BAR   // Gradient progress bar with glow
}

/**
 * Animated waveform seeker with multiple style options.
 * Long-press to change style.
 */
@Composable
fun WaveformSeeker(
    progress: Float,
    isPlaying: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    initialStyle: SeekbarStyle = SeekbarStyle.WAVEFORM,
    onStyleChange: ((SeekbarStyle) -> Unit)? = null
) {
    // Current seekbar style - uses initial style from settings
    var currentStyle by remember { mutableStateOf(initialStyle) }
    var showStyleMenu by remember { mutableStateOf(false) }
    
    // Sync with external style changes
    LaunchedEffect(initialStyle) {
        currentStyle = initialStyle
    }
    
    // Animation for wave movement when playing
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )
    
    // Generate random wave amplitudes (simulating audio waveform)
    val waveAmplitudes = remember {
        List(60) { Random.nextFloat() * 0.6f + 0.4f }
    }
    
    var isDragging by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableFloatStateOf(progress) }
    
    // Update currentProgress from external progress only when NOT dragging
    LaunchedEffect(progress) {
        if (!isDragging) {
            currentProgress = progress
        }
    }
    
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 8.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                            currentProgress = newProgress
                            onSeek(newProgress)
                        },
                        onLongPress = {
                            showStyleMenu = true
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { 
                            onSeek(currentProgress)
                            isDragging = false
                        },
                        onHorizontalDrag = { change, _ ->
                            val newProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                            currentProgress = newProgress
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                when (currentStyle) {
                    SeekbarStyle.WAVEFORM -> with(WaveformStyle) {
                        draw(
                            progress = currentProgress,
                            isPlaying = isPlaying,
                            wavePhase = wavePhase,
                            waveAmplitudes = waveAmplitudes,
                            activeColor = activeColor,
                            inactiveColor = inactiveColor,
                            isDragging = isDragging
                        )
                    }
                    SeekbarStyle.WAVE_LINE -> with(WaveLineStyle) {
                        draw(
                            progress = currentProgress,
                            isPlaying = isPlaying,
                            wavePhase = wavePhase,
                            activeColor = activeColor,
                            inactiveColor = inactiveColor
                        )
                    }
                    SeekbarStyle.CLASSIC -> with(ClassicStyle) {
                        draw(
                            progress = currentProgress,
                            activeColor = activeColor,
                            inactiveColor = inactiveColor,
                            isDragging = isDragging
                        )
                    }
                    SeekbarStyle.DOTS -> with(DotsStyle) {
                        draw(
                            progress = currentProgress,
                            isPlaying = isPlaying,
                            wavePhase = wavePhase,
                            activeColor = activeColor,
                            inactiveColor = inactiveColor
                        )
                    }
                    SeekbarStyle.GRADIENT_BAR -> with(GradientBarStyle) {
                        draw(
                            progress = currentProgress,
                            activeColor = activeColor,
                            inactiveColor = inactiveColor,
                            isDragging = isDragging
                        )
                    }
                }
            }
        }
        
        // Style selection popup
        if (showStyleMenu) {
            Popup(
                alignment = Alignment.Center,
                onDismissRequest = { showStyleMenu = false },
                properties = PopupProperties(focusable = true)
            ) {
                SeekbarStyleMenu(
                    currentStyle = currentStyle,
                    activeColor = activeColor,
                    inactiveColor = inactiveColor,
                    onStyleSelected = { style ->
                        currentStyle = style
                        onStyleChange?.invoke(style)
                        showStyleMenu = false
                    }
                )
            }
        }
    }
}

/**
 * Seekbar style selection menu with previews
 */
@Composable
private fun SeekbarStyleMenu(
    currentStyle: SeekbarStyle,
    activeColor: Color,
    inactiveColor: Color,
    onStyleSelected: (SeekbarStyle) -> Unit
) {
    Surface(
        modifier = Modifier.padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 16.dp,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Seekbar Style",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            SeekbarStyle.entries.forEach { style ->
                StylePreviewItem(
                    style = style,
                    isSelected = style == currentStyle,
                    activeColor = activeColor,
                    inactiveColor = inactiveColor,
                    onClick = { onStyleSelected(style) }
                )
                
                if (style != SeekbarStyle.entries.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * Individual style preview item
 */
@Composable
private fun StylePreviewItem(
    style: SeekbarStyle,
    isSelected: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) activeColor.copy(alpha = 0.15f) else Color.Transparent,
        animationSpec = spring(),
        label = "bg"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else Color.Transparent,
        animationSpec = spring(),
        label = "border"
    )
    
    val styleName = when (style) {
        SeekbarStyle.WAVEFORM -> "Waveform"
        SeekbarStyle.WAVE_LINE -> "Wave Line"
        SeekbarStyle.CLASSIC -> "Classic"
        SeekbarStyle.DOTS -> "Dots"
        SeekbarStyle.GRADIENT_BAR -> "Gradient"
    }
    
    // Preview amplitudes
    val previewAmplitudes = remember {
        List(20) { Random.nextFloat() * 0.6f + 0.4f }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Preview canvas
        Canvas(
            modifier = Modifier
                .width(80.dp)
                .height(30.dp)
        ) {
            val previewProgress = 0.6f
            when (style) {
                SeekbarStyle.WAVEFORM -> with(WaveformStyle) {
                    drawPreview(previewProgress, previewAmplitudes, activeColor, inactiveColor)
                }
                SeekbarStyle.WAVE_LINE -> with(WaveLineStyle) {
                    drawPreview(previewProgress, activeColor, inactiveColor)
                }
                SeekbarStyle.CLASSIC -> with(ClassicStyle) {
                    drawPreview(previewProgress, activeColor, inactiveColor)
                }
                SeekbarStyle.DOTS -> with(DotsStyle) {
                    drawPreview(previewProgress, activeColor, inactiveColor)
                }
                SeekbarStyle.GRADIENT_BAR -> with(GradientBarStyle) {
                    drawPreview(previewProgress, activeColor, inactiveColor)
                }
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = styleName,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurface
        )
    }
}
