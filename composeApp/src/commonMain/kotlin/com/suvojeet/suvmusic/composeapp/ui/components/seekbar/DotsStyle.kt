package com.suvojeet.suvmusic.composeapp.ui.components.seekbar

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Dots style seekbar — animated bouncing dots. Port of
 * `app/.../ui/components/seekbar/DotsStyle.kt`. Java's `Math.toRadians`
 * replaced with `kotlin.math.PI` so the file works in commonMain.
 */
object DotsStyle {

    fun DrawScope.draw(
        progress: Float,
        isPlaying: Boolean,
        wavePhase: Float,
        activeColor: Color,
        inactiveColor: Color,
        isDragging: Boolean,
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val progressX = progress * width
        val dotCount = 30
        val dotSpacing = width / dotCount
        val trackHeight = 6.dp.toPx()

        drawLine(
            color = inactiveColor.copy(alpha = 0.3f),
            start = Offset(progressX, centerY),
            end = Offset(width, centerY),
            strokeWidth = trackHeight,
            cap = StrokeCap.Round,
        )

        for (i in 0 until dotCount) {
            val x = i * dotSpacing + dotSpacing / 2
            val isPast = x < progressX

            if (isPast) {
                val baseRadius = 3.dp.toPx()
                val animatedRadius = if (isPlaying) {
                    val phase = (wavePhase + i * 20) % 360
                    val wave = sin(phase * PI / 180.0).toFloat()
                    baseRadius * (1f + wave * 0.4f)
                } else {
                    baseRadius
                }

                drawCircle(
                    color = activeColor,
                    radius = animatedRadius,
                    center = Offset(x, centerY),
                )
            }
        }

        val thumbRadius = if (isDragging) 8.dp.toPx() else 6.dp.toPx()

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(activeColor.copy(alpha = 0.5f), Color.Transparent),
                center = Offset(progressX, centerY),
                radius = thumbRadius * 3f,
            ),
            radius = thumbRadius * 3f,
            center = Offset(progressX, centerY),
        )

        drawCircle(
            color = activeColor,
            radius = thumbRadius,
            center = Offset(progressX, centerY),
        )

        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = thumbRadius * 0.4f,
            center = Offset(progressX, centerY),
        )
    }

    fun DrawScope.drawPreview(
        progress: Float,
        activeColor: Color,
        inactiveColor: Color,
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
                center = Offset(x, centerY),
            )
        }
    }
}
