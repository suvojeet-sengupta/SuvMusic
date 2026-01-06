package com.suvojeet.suvmusic.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.ui.theme.GradientEnd
import com.suvojeet.suvmusic.ui.theme.GradientMiddle
import com.suvojeet.suvmusic.ui.theme.GradientStart
import kotlin.math.sin
import kotlin.random.Random

/**
 * Animated waveform seeker with sound wave visualization.
 * Material Expressive style with gradient colors.
 */
@Composable
fun WaveformSeeker(
    progress: Float,
    isPlaying: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
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
    
    var isDragging by remember { androidx.compose.runtime.mutableStateOf(false) }
    var currentProgress by remember { mutableFloatStateOf(progress) }
    
    // Update currentProgress from external progress only when NOT dragging
    androidx.compose.runtime.LaunchedEffect(progress) {
        if (!isDragging) {
            currentProgress = progress
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 8.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    currentProgress = newProgress
                    onSeek(newProgress)
                }
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
            val width = size.width
            val height = size.height
            val centerY = height / 2
            val barWidth = width / waveAmplitudes.size
            val maxBarHeight = height * 0.8f
            val progressX = currentProgress * width
            
            // Draw waveform bars
            waveAmplitudes.forEachIndexed { index, amplitude ->
                val x = index * barWidth + barWidth / 2
                val isPast = x < progressX
                
                // Animate bar height when playing
                val animatedAmplitude = if (isPlaying && isPast) {
                    val phase = (wavePhase + index * 12) % 360
                    val wave = sin(Math.toRadians(phase.toDouble())).toFloat()
                    amplitude * (0.7f + wave * 0.3f)
                } else {
                    amplitude
                }
                
                val barHeight = animatedAmplitude * maxBarHeight
                val topY = centerY - barHeight / 2
                
                // Gradient for played portion
                val barColor = if (isPast) {
                    Brush.verticalGradient(
                        colors = listOf(activeColor.copy(alpha = 0.7f), activeColor, activeColor.copy(alpha = 0.7f)),
                        startY = topY,
                        endY = topY + barHeight
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            inactiveColor.copy(alpha = 0.5f),
                            inactiveColor.copy(alpha = 0.3f)
                        )
                    )
                }
                
                // Draw rounded bar
                drawRoundRect(
                    brush = barColor,
                    topLeft = Offset(x - barWidth * 0.35f, topY),
                    size = Size(barWidth * 0.7f, barHeight),
                    cornerRadius = CornerRadius(barWidth * 0.35f)
                )
            }
            
            // Draw progress indicator (glowing dot)
            val indicatorRadius = if (isDragging) 12.dp.toPx() else 8.dp.toPx()
            
            // Glow effect
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        activeColor.copy(alpha = 0.4f),
                        Color.Transparent
                    ),
                    center = Offset(progressX, centerY),
                    radius = indicatorRadius * 2
                ),
                radius = indicatorRadius * 2,
                center = Offset(progressX, centerY)
            )
            
            // Main indicator
            drawCircle(
                color = Color.White,
                radius = indicatorRadius,
                center = Offset(progressX, centerY)
            )
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(activeColor, activeColor),
                    center = Offset(progressX, centerY)
                ),
                radius = indicatorRadius - 2.dp.toPx(),
                center = Offset(progressX, centerY)
            )
        }
    }
}

/**
 * Simple wave line seeker (alternative style).
 */
@Composable
fun WaveLineSeeker(
    progress: Float,
    isPlaying: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    
    val infiniteTransition = rememberInfiniteTransition(label = "waveLine")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    
    var isDragging by remember { androidx.compose.runtime.mutableStateOf(false) }
    var currentProgress by remember { mutableFloatStateOf(progress) }
    
    // Update currentProgress from external progress only when NOT dragging
    androidx.compose.runtime.LaunchedEffect(progress) {
        if (!isDragging) {
            currentProgress = progress
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 16.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    currentProgress = newProgress
                    onSeek(newProgress)
                }
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
            val width = size.width
            val height = size.height
            val centerY = height / 2
            val progressX = currentProgress * width
            val amplitude = height * 0.3f
            val frequency = 0.03f
            
            // Draw unplayed wave path
            val unplayedPath = Path().apply {
                moveTo(progressX, centerY)
                var x = progressX
                while (x <= width) {
                    val y = centerY + sin(x * frequency + phase) * amplitude * 0.5f
                    lineTo(x, y.toFloat())
                    x += 2f
                }
            }
            
            drawPath(
                path = unplayedPath,
                color = surfaceColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
            
            // Draw played wave path with animation
            val playedPath = Path().apply {
                moveTo(0f, centerY)
                var x = 0f
                while (x <= progressX) {
                    val waveAmplitude = if (isPlaying) amplitude else amplitude * 0.5f
                    val y = centerY + sin(x * frequency + (if (isPlaying) phase else 0f)) * waveAmplitude
                    lineTo(x, y.toFloat())
                    x += 2f
                }
            }
            
            drawPath(
                path = playedPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(GradientStart, GradientMiddle, GradientEnd)
                ),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
            
            // Draw indicator
            drawCircle(
                color = Color.White,
                radius = 10.dp.toPx(),
                center = Offset(progressX, centerY)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(GradientStart, GradientEnd)
                ),
                radius = 7.dp.toPx(),
                center = Offset(progressX, centerY)
            )
        }
    }
}
