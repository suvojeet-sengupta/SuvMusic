package com.suvojeet.suvmusic.composeapp.ui.components.seekbar

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

/**
 * Classic style seekbar — simple clean progress bar. Verbatim port of
 * `app/.../ui/components/seekbar/ClassicStyle.kt`. Pure DrawScope
 * extensions, no platform deps.
 */
object ClassicStyle {

    fun DrawScope.draw(
        progress: Float,
        activeColor: Color,
        inactiveColor: Color,
        isDragging: Boolean,
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val progressX = progress * width
        val trackHeight = 6.dp.toPx()

        drawRoundRect(
            color = inactiveColor.copy(alpha = 0.3f),
            topLeft = Offset(0f, centerY - trackHeight / 2),
            size = Size(width, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2),
        )

        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(activeColor.copy(alpha = 0.8f), activeColor),
            ),
            topLeft = Offset(0f, centerY - trackHeight / 2),
            size = Size(progressX, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2),
        )

        val indicatorRadius = if (isDragging) 10.dp.toPx() else 7.dp.toPx()

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(activeColor.copy(alpha = 0.3f), Color.Transparent),
                center = Offset(progressX, centerY),
                radius = indicatorRadius * 2.5f,
            ),
            radius = indicatorRadius * 2.5f,
            center = Offset(progressX, centerY),
        )

        drawCircle(
            color = activeColor,
            radius = indicatorRadius,
            center = Offset(progressX, centerY),
        )

        drawCircle(
            color = Color.White.copy(alpha = 0.8f),
            radius = indicatorRadius * 0.3f,
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
        val trackHeight = 4.dp.toPx()

        drawRoundRect(
            color = inactiveColor.copy(alpha = 0.3f),
            topLeft = Offset(0f, centerY - trackHeight / 2),
            size = Size(width, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2),
        )

        drawRoundRect(
            color = activeColor,
            topLeft = Offset(0f, centerY - trackHeight / 2),
            size = Size(progress * width, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2),
        )
    }
}
