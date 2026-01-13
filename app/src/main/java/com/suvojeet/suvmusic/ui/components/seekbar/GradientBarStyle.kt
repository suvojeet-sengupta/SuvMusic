package com.suvojeet.suvmusic.ui.components.seekbar

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.ui.theme.GradientEnd
import com.suvojeet.suvmusic.ui.theme.GradientMiddle
import com.suvojeet.suvmusic.ui.theme.GradientStart

/**
 * Gradient Bar style seekbar - Progress bar with colorful gradient and glow
 */
object GradientBarStyle {
    
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
        val trackHeight = 8.dp.toPx()
        
        // Background track
        drawRoundRect(
            color = inactiveColor.copy(alpha = 0.2f),
            topLeft = Offset(0f, centerY - trackHeight / 2),
            size = Size(width, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2)
        )
        
        // Glow effect
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    GradientStart.copy(alpha = 0.3f),
                    GradientMiddle.copy(alpha = 0.3f),
                    GradientEnd.copy(alpha = 0.3f)
                )
            ),
            topLeft = Offset(0f, centerY - trackHeight),
            size = Size(progressX, trackHeight * 2),
            cornerRadius = CornerRadius(trackHeight)
        )
        
        // Progress track with gradient
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(GradientStart, GradientMiddle, GradientEnd)
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
        val trackHeight = 5.dp.toPx()
        
        drawRoundRect(
            color = inactiveColor.copy(alpha = 0.2f),
            topLeft = Offset(0f, centerY - trackHeight / 2),
            size = Size(width, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2)
        )
        
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(GradientStart, GradientMiddle, GradientEnd)
            ),
            topLeft = Offset(0f, centerY - trackHeight / 2),
            size = Size(progress * width, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2)
        )
    }
}
