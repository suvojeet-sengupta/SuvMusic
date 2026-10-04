package com.suvojeet.suvmusic.ui.components.seekbar

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

/**
 * Classic style seekbar - Simple clean progress bar
 */
object ClassicStyle {
    
    fun DrawScope.draw(
        progress: Float,
        activeColor: Color,
        inactiveColor: Color,
        isDragging: Boolean
    ) {
        val width = size.width
        val centerY = size.height / 2
        val p = progress.coerceIn(0f, 1f)
        val trackHeight = (if (isDragging) 12.dp else 8.dp).toPx()
        val thumbWidth = (if (isDragging) 3.dp else 4.dp).toPx()
        val thumbHeight = trackHeight * 2.4f
        val gap = 4.dp.toPx()
        val thumbX = (p * width).coerceIn(thumbWidth / 2, width - thumbWidth / 2)
        val outer = CornerRadius(trackHeight / 2)
        val inner = CornerRadius(2.dp.toPx())

        val activeEnd = thumbX - thumbWidth / 2 - gap
        if (activeEnd > 0f) {
            drawPath(
                androidx.compose.ui.graphics.Path().apply {
                    addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            left = 0f, top = centerY - trackHeight / 2,
                            right = activeEnd, bottom = centerY + trackHeight / 2,
                            topLeftCornerRadius = outer, bottomLeftCornerRadius = outer,
                            topRightCornerRadius = inner, bottomRightCornerRadius = inner
                        )
                    )
                },
                color = activeColor
            )
        }

        val inactiveStart = thumbX + thumbWidth / 2 + gap
        if (inactiveStart < width) {
            drawPath(
                androidx.compose.ui.graphics.Path().apply {
                    addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            left = inactiveStart, top = centerY - trackHeight / 2,
                            right = width, bottom = centerY + trackHeight / 2,
                            topLeftCornerRadius = inner, bottomLeftCornerRadius = inner,
                            topRightCornerRadius = outer, bottomRightCornerRadius = outer
                        )
                    )
                },
                color = inactiveColor.copy(alpha = 0.28f)
            )
            val stopRadius = 2.dp.toPx()
            if (width - inactiveStart > trackHeight) {
                drawCircle(
                    color = activeColor,
                    radius = stopRadius,
                    center = Offset(width - trackHeight / 2, centerY)
                )
            }
        }

        drawRoundRect(
            color = activeColor,
            topLeft = Offset(thumbX - thumbWidth / 2, centerY - thumbHeight / 2),
            size = Size(thumbWidth, thumbHeight),
            cornerRadius = CornerRadius(thumbWidth / 2)
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
        val trackHeight = 4.dp.toPx()
        
        drawRoundRect(
            color = inactiveColor.copy(alpha = 0.3f),
            topLeft = Offset(0f, centerY - trackHeight / 2),
            size = Size(width, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2)
        )
        
        drawRoundRect(
            color = activeColor,
            topLeft = Offset(0f, centerY - trackHeight / 2),
            size = Size(progress * width, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2)
        )
    }
}
