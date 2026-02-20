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
        val maxBarHeight = height * 0.6f // Balanced height for 100 bars
        val progressX = progress * width
        
        // Draw unplayed track line
        drawLine(
            color = inactiveColor.copy(alpha = 0.3f),
            start = Offset(progressX, centerY),
            end = Offset(width, centerY),
            strokeWidth = 3.dp.toPx(), // Slightly thinner line
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        waveAmplitudes.forEachIndexed { index, amplitude ->
            val x = index * barWidth + barWidth / 2
            val isPast = x < progressX
            
            if (isPast) {
                val animatedAmplitude = if (isPlaying) {
                    val phase = (wavePhase + index * 10) % 360 // Slower phase shift
                    val wave = sin(Math.toRadians(phase.toDouble())).toFloat()
                    amplitude * (0.9f + wave * 0.1f) // More stable animation
                } else {
                    amplitude
                }
                
                val barHeight = animatedAmplitude * maxBarHeight
                val topY = centerY - barHeight / 2
                
                val barColor = Brush.verticalGradient(
                    colors = listOf(
                        activeColor.copy(alpha = 0.8f),
                        activeColor,
                        activeColor.copy(alpha = 0.8f)
                    ),
                    startY = topY,
                    endY = topY + barHeight
                )
                
                drawRoundRect(
                    brush = barColor,
                    topLeft = Offset(x - barWidth * 0.3f, topY), // More space between bars
                    size = Size(barWidth * 0.6f, barHeight),
                    cornerRadius = CornerRadius(barWidth * 0.3f)
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
