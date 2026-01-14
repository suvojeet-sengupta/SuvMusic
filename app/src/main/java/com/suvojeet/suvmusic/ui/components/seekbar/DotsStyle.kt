package com.suvojeet.suvmusic.ui.components.seekbar

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * Dots style seekbar - Animated bouncing dots
 */
object DotsStyle {
    
    fun DrawScope.draw(
        progress: Float,
        isPlaying: Boolean,
        wavePhase: Float,
        activeColor: Color,
        inactiveColor: Color
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val progressX = progress * width
        val dotCount = 30
        val dotSpacing = width / dotCount
        
        // Draw unplayed straight line
        drawLine(
            color = inactiveColor.copy(alpha = 0.3f),
            start = Offset(progressX, centerY),
            end = Offset(width, centerY),
            strokeWidth = 4.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        for (i in 0 until dotCount) {
            val x = i * dotSpacing + dotSpacing / 2
            val isPast = x < progressX
            
            if (isPast) {
                val baseRadius = 4.dp.toPx()
                val animatedRadius = if (isPlaying) {
                    val phase = (wavePhase + i * 20) % 360
                    val wave = sin(Math.toRadians(phase.toDouble())).toFloat()
                    baseRadius * (1f + wave * 0.4f)
                } else {
                    baseRadius
                }
                
                drawCircle(
                    color = activeColor,
                    radius = animatedRadius,
                    center = Offset(x, centerY)
                )
            }
        }
        
        // Main indicator
        drawCircle(
            color = Color.White,
            radius = 10.dp.toPx(),
            center = Offset(progressX, centerY)
        )
        drawCircle(
            color = activeColor,
            radius = 7.dp.toPx(),
            center = Offset(progressX, centerY)
        )
    }
    
    fun DrawScope.drawPreview(
        progress: Float,
        activeColor: Color,
        inactiveColor: Color
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val progressX = progress * width
        val dotCount = 12
        val dotSpacing = width / dotCount
        
        for (i in 0 until dotCount) {
            val x = i * dotSpacing + dotSpacing / 2
            val isPast = x < progressX
            drawCircle(
                color = if (isPast) activeColor else inactiveColor.copy(alpha = 0.4f),
                radius = 3.dp.toPx(),
                center = Offset(x, centerY)
            )
        }
    }
}
