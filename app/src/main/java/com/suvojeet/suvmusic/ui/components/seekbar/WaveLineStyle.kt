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
        inactiveColor: Color
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val progressX = progress * width
        val amplitude = height * 0.15f // Reduced height (was 0.3f)
        val frequency = 0.045f // Slightly tighter frequency (was 0.03f)
        val phase = wavePhase * (Math.PI.toFloat() / 180f)
        
        // Unplayed path - Straight line
        val unplayedPath = Path().apply {
            moveTo(progressX, centerY)
            lineTo(width, centerY)
        }
        
        drawPath(
            path = unplayedPath,
            color = inactiveColor.copy(alpha = 0.3f),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )
        
        // Played path
        val playedPath = Path().apply {
            moveTo(0f, centerY)
            var x = 0f
            while (x <= progressX) {
                val waveAmp = if (isPlaying) amplitude else amplitude * 0.4f
                val y = centerY + sin(x * frequency + (if (isPlaying) phase else 0f)) * waveAmp
                lineTo(x, y.toFloat())
                x += 2f
            }
        }
        
        drawPath(
            path = playedPath,
            color = activeColor, // Use album art color instead of hardcoded gradient
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )
        
        // Indicator
        drawCircle(
            color = Color.White,
            radius = 10.dp.toPx(),
            center = Offset(progressX, centerY)
        )
        drawCircle(
            color = activeColor, // Use album art color for inner circle
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
        val amplitude = height * 0.3f
        
        val path = Path().apply {
            moveTo(0f, centerY)
            var x = 0f
            while (x <= width) {
                val y = centerY + sin(x * 0.1f) * amplitude
                lineTo(x, y.toFloat())
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
                lineTo(x, y.toFloat())
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
