package com.suvojeet.suvmusic.ui.components.seekbar

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.ui.theme.GradientEnd
import com.suvojeet.suvmusic.ui.theme.GradientMiddle
import com.suvojeet.suvmusic.ui.theme.GradientStart
import kotlin.math.sin

/**
 * Wave Line style seekbar - Sine wave line that animates when playing
 */
object WaveLineStyle {
    
    fun DrawScope.draw(
        progress: Float,
        isPlaying: Boolean,
        wavePhase: Float,
        activeColor: Color,
        inactiveColor: Color,
        isDragging: Boolean
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val progressX = progress * width
        val trackHeight = 6.dp.toPx()
        val amplitude = height * 0.12f 
        val frequency = 0.08f 
        val phase = wavePhase * (Math.PI.toFloat() / 180f)
        
        // Unplayed path - Straight line
        val unplayedPath = Path().apply {
            moveTo(progressX, centerY)
            lineTo(width, centerY)
        }
        
        drawPath(
            path = unplayedPath,
            color = inactiveColor.copy(alpha = 0.3f),
            style = Stroke(width = trackHeight, cap = StrokeCap.Round)
        )
        
        // Played path
        val playedPath = Path().apply {
            moveTo(0f, centerY)
            var x = 0f
            while (x <= progressX) {
                val waveAmp = if (isPlaying) amplitude else amplitude * 0.2f
                val y = centerY + sin(x * frequency + (if (isPlaying) phase else 0f)) * waveAmp
                lineTo(x, y)
                x += 2f
            }
        }
        
        drawPath(
            path = playedPath,
            color = activeColor,
            style = Stroke(width = trackHeight, cap = StrokeCap.Round)
        )
        
        // Custom glowing orb thumb that rides the wave for Wave Line style
        val waveAmp = if (isPlaying) amplitude else amplitude * 0.2f
        val currentWaveY = centerY + sin(progressX * frequency + (if (isPlaying) phase else 0f)) * waveAmp
        val thumbRadius = if (isDragging) 8.dp.toPx() else 6.dp.toPx()
        
        // Wave-aligned Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(activeColor.copy(alpha = 0.5f), Color.Transparent),
                center = Offset(progressX, currentWaveY),
                radius = thumbRadius * 3f
            ),
            radius = thumbRadius * 3f,
            center = Offset(progressX, currentWaveY)
        )
        
        // Solid orb
        drawCircle(
            color = activeColor,
            radius = thumbRadius,
            center = Offset(progressX, currentWaveY)
        )
        
        // Inner bright spot
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = thumbRadius * 0.4f,
            center = Offset(progressX, currentWaveY)
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
        val amplitude = height * 0.15f
        
        val path = Path().apply {
            moveTo(0f, centerY)
            var x = 0f
            while (x <= width) {
                val y = centerY + sin(x * 0.1f) * amplitude
                lineTo(x, y)
                x += 3f
            }
        }
        
        drawPath(
            path = path,
            color = inactiveColor,
            style = Stroke(width = 2.dp.toPx())
        )
        
        val playedPath = Path().apply {
            moveTo(0f, centerY)
            var x = 0f
            while (x <= progressX) {
                val y = centerY + sin(x * 0.1f) * amplitude
                lineTo(x, y)
                x += 3f
            }
        }
        
        drawPath(
            path = playedPath,
            color = activeColor,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
