package com.suvojeet.suvmusic.composeapp.ui.components.seekbar

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Waveform style seekbar — animated vertical bars like an audio waveform.
 * Port of `app/.../ui/components/seekbar/WaveformStyle.kt`. Replaces
 * Java's `Math.toRadians` with the kotlin.math equivalent for commonMain.
 */
object WaveformStyle {

    fun DrawScope.draw(
        progress: Float,
        isPlaying: Boolean,
        wavePhase: Float,
        waveAmplitudes: List<Float>,
        activeColor: Color,
        inactiveColor: Color,
        isDragging: Boolean,
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val barWidth = width / waveAmplitudes.size
        val maxBarHeight = height * 0.6f
        val progressX = progress * width

        waveAmplitudes.forEachIndexed { index, amplitude ->
            val x = index * barWidth + barWidth / 2
            val isPast = x < progressX

            val animatedAmplitude = if (isPlaying && isPast) {
                val phase = (wavePhase + index * 10) % 360
                val wave = sin(phase * PI / 180.0).toFloat()
                amplitude * (0.9f + wave * 0.1f)
            } else {
                amplitude
            }

            val barHeight = animatedAmplitude * maxBarHeight
            val topY = centerY - barHeight / 2

            if (isPast) {
                val barColor = Brush.verticalGradient(
                    colors = listOf(
                        activeColor.copy(alpha = 0.8f),
                        activeColor,
                        activeColor.copy(alpha = 0.8f),
                    ),
                    startY = topY,
                    endY = topY + barHeight,
                )

                drawRoundRect(
                    brush = barColor,
                    topLeft = Offset(x - barWidth * 0.3f, topY),
                    size = Size(barWidth * 0.6f, barHeight),
                    cornerRadius = CornerRadius(barWidth * 0.3f),
                )
            } else {
                drawRoundRect(
                    color = inactiveColor.copy(alpha = 0.4f),
                    topLeft = Offset(x - barWidth * 0.3f, topY),
                    size = Size(barWidth * 0.6f, barHeight),
                    cornerRadius = CornerRadius(barWidth * 0.3f),
                )
            }
        }

        val thumbWidth = barWidth * 1.5f
        val thumbHeight = maxBarHeight * 1.2f
        val thumbRect = Size(thumbWidth, thumbHeight)
        val thumbTopLeft = Offset(progressX - thumbWidth / 2, centerY - thumbHeight / 2)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(activeColor.copy(alpha = 0.4f), Color.Transparent),
                center = Offset(progressX, centerY),
                radius = thumbHeight * 0.8f,
            ),
            radius = thumbHeight * 0.8f,
            center = Offset(progressX, centerY),
        )

        drawRoundRect(
            color = activeColor,
            topLeft = thumbTopLeft,
            size = thumbRect,
            cornerRadius = CornerRadius(thumbWidth / 2),
        )

        drawRoundRect(
            color = Color.White.copy(alpha = 0.5f),
            topLeft = Offset(
                progressX - (thumbWidth * 0.2f) / 2,
                centerY - (thumbHeight * 0.6f) / 2,
            ),
            size = Size(thumbWidth * 0.2f, thumbHeight * 0.6f),
            cornerRadius = CornerRadius(thumbWidth * 0.1f),
        )
    }

    fun DrawScope.drawPreview(
        progress: Float,
        amplitudes: List<Float>,
        activeColor: Color,
        inactiveColor: Color,
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
                cornerRadius = CornerRadius(barWidth * 0.3f),
            )
        }
    }
}
