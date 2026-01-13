package com.suvojeet.suvmusic.ui.components.seekbar

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
        val height = size.height
        val centerY = height / 2
        val progressX = progress * width
        val trackHeight = 6.dp.toPx()
        
        // Background track
        drawRoundRect(
            color = inactiveColor.copy(alpha = 0.3f),
            topLeft = Offset(0f, centerY - trackHeight / 2),
            size = Size(width, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2)
        )
        
        // Progress track
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(activeColor.copy(alpha = 0.8f), activeColor)
            ),
            topLeft = Offset(0f, centerY - trackHeight / 2),
            size = Size(progressX, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2)
        )
        
        drawProgressIndicator(progressX, centerY, activeColor, isDragging)
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
