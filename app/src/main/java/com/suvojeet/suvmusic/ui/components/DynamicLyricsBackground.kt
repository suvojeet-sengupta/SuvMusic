package com.suvojeet.suvmusic.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.suvojeet.suvmusic.util.LyricsStyle

@Composable
fun DynamicLyricsBackground(
    artworkUrl: String?,
    style: LyricsStyle,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val overlayColor = if (isDarkTheme) Color.Black else Color.White
    
    Box(modifier = modifier.fillMaxSize()) {
        if (artworkUrl != null) {
            
            // Animation for Energetic/Happy
            val infiniteTransition = rememberInfiniteTransition()
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = if (style is LyricsStyle.Energetic || style is LyricsStyle.Happy) 1.1f else 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(if (style is LyricsStyle.Energetic) 500 else 4000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
            
            // Saturation for Sad
            val colorFilter = if (style is LyricsStyle.Sad) {
                 ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
            } else {
                null
            }

            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(scale)
                    .blur(if (style is LyricsStyle.Chill) 100.dp else 80.dp)
                    .alpha(if (isDarkTheme) 0.6f else 0.4f),
                contentScale = ContentScale.Crop,
                colorFilter = colorFilter
            )
        }
        
        // Dynamic Overlay Gradient
        val gradientColors = when (style) {
            LyricsStyle.Romantic -> listOf(
                Color(0xFFE91E63).copy(alpha = 0.2f),
                overlayColor.copy(alpha = 0.7f),
                overlayColor.copy(alpha = 0.9f)
            )
            LyricsStyle.Energetic -> listOf(
                Color(0xFFFF5722).copy(alpha = 0.2f),
                overlayColor.copy(alpha = 0.7f),
                overlayColor.copy(alpha = 0.9f)
            )
            LyricsStyle.Sad -> listOf(
                Color(0xFF607D8B).copy(alpha = 0.2f),
                overlayColor.copy(alpha = 0.7f),
                overlayColor.copy(alpha = 0.9f)
            )
            LyricsStyle.Chill -> listOf(
                Color(0xFF2196F3).copy(alpha = 0.2f),
                overlayColor.copy(alpha = 0.7f),
                overlayColor.copy(alpha = 0.9f)
            )
            else -> listOf(
                overlayColor.copy(alpha = 0.4f),
                overlayColor.copy(alpha = 0.7f),
                overlayColor.copy(alpha = 0.9f)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = gradientColors
                    )
                )
        )
    }
}
