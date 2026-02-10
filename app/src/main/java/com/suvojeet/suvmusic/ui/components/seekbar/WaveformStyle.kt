package com.suvojeet.suvmusic.ui.components.seekbar

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * Waveform style seekbar - Animated vertical bars like audio waveform
 */
object WaveformStyle {
    
    fun DrawScope.draw(
        progress: Float,
        isPlaying: Boolean,
        wavePhase: Float,
        waveAmplitudes: List<Float>,
        activeColor: Color,
        inactiveColor: Color,
        isDragging: Boolean
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val barWidth = width / waveAmplitudes.size
        val maxBarHeight = height * 0.5f // Reduced height
        val progressX = progress * width
        
        // Draw unplayed track line
        drawLine(
            color = inactiveColor.copy(alpha = 0.3f),
            start = Offset(progressX, centerY),
            end = Offset(width, centerY),
            strokeWidth = 4.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        waveAmplitudes.forEachIndexed { index, amplitude ->
            val x = index * barWidth + barWidth / 2
            val isPast = x < progressX
            
            if (isPast) {
                val animatedAmplitude = if (isPlaying) {
                    val phase = (wavePhase + index * 12) % 360
                    val wave = sin(Math.toRadians(phase.toDouble())).toFloat()
                    amplitude * (0.85f + wave * 0.15f) // Reduced wiggle (was 0.3f)
                } else {
                    amplitude
                }
                
                val barHeight = animatedAmplitude * maxBarHeight
                val topY = centerY - barHeight / 2
                
                val barColor = Brush.verticalGradient(
                    colors = listOf(
                        activeColor.copy(alpha = 0.7f),
                        activeColor,
                        activeColor.copy(alpha = 0.7f)
                    ),
                    startY = topY,
                    endY = topY + barHeight
                )
                
                drawRoundRect(
                    brush = barColor,
                    topLeft = Offset(x - barWidth * 0.35f, topY),
                    size = Size(barWidth * 0.7f, barHeight),
                    cornerRadius = CornerRadius(barWidth * 0.35f)
                )
            }
        }
        
        drawProgressIndicator(progressX, centerY, activeColor, isDragging)
    }
    
    fun DrawScope.drawPreview(
        progress: Float,
        amplitudes: List<Float>,
        activeColor: Color,
        inactiveColor: Color
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val barWidth = width / amplitudes.size
        val maxBarHeight = height * 0.8f
        val progressX = progress * width
        
        amplitudes.forEachIndexed { index, amplitude ->
            val x = index * barWidth + barWidth / 2
            val isPast = x < progressX
            val barHeight = amplitude * maxBarHeight
            val topY = centerY - barHeight / 2
            
            drawRoundRect(
                color = if (isPast) activeColor else inactiveColor.copy(alpha = 0.4f),
                topLeft = Offset(x - barWidth * 0.3f, topY),
                size = Size(barWidth * 0.6f, barHeight),
                cornerRadius = CornerRadius(barWidth * 0.3f)
            )
        }
    }
}

/**
 * Common progress indicator drawing
 */
fun DrawScope.drawProgressIndicator(
    progressX: Float,
    centerY: Float,
    activeColor: Color,
    isDragging: Boolean
) {
    val indicatorRadius = if (isDragging) 12.dp.toPx() else 8.dp.toPx()
    
    // Glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(activeColor.copy(alpha = 0.4f), Color.Transparent),
            center = Offset(progressX, centerY),
            radius = indicatorRadius * 2
        ),
        radius = indicatorRadius * 2,
        center = Offset(progressX, centerY)
    )
    
    // White outer
    drawCircle(
        color = Color.White,
        radius = indicatorRadius,
        center = Offset(progressX, centerY)
    )
    
    // Colored inner
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(activeColor, activeColor),
            center = Offset(progressX, centerY)
        ),
        radius = indicatorRadius - 2.dp.toPx(),
        center = Offset(progressX, centerY)
    )
}
