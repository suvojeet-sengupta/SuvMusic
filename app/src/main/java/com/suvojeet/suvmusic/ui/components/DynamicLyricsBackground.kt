package com.suvojeet.suvmusic.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import coil3.compose.AsyncImage
import com.suvojeet.suvmusic.util.LyricsStyle

@Composable
fun DynamicLyricsBackground(
    artworkUrl: String?,
    style: LyricsStyle,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val overlayColor = if (isDarkTheme) Color.Black else Color.White
    
    Box(modifier = modifier.fillMaxSize().background(overlayColor)) {
        // 1. Moving Blobs (Apple Music Style)
        val infiniteTransition = rememberInfiniteTransition(label = "blobs")
        
        val blob1Offset by infiniteTransition.animateValue(
            initialValue = (-100).dp,
            targetValue = 100.dp,
            typeConverter = androidx.compose.ui.unit.Dp.VectorConverter,
            animationSpec = infiniteRepeatable(
                animation = tween(10000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "blob1"
        )
        
        val blob2Offset by infiniteTransition.animateValue(
            initialValue = 150.dp,
            targetValue = (-150).dp,
            typeConverter = androidx.compose.ui.unit.Dp.VectorConverter,
            animationSpec = infiniteRepeatable(
                animation = tween(12000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "blob2"
        )

        // Primary Blob
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(x = blob1Offset, y = blob2Offset)
                .alpha(0.4f)
                .blur(100.dp)
                .background(
                    when(style) {
                        LyricsStyle.Romantic -> Color(0xFFE91E63)
                        LyricsStyle.Energetic -> Color(0xFFFF5722)
                        LyricsStyle.Sad -> Color(0xFF607D8B)
                        LyricsStyle.Chill -> Color(0xFF2196F3)
                        else -> MaterialTheme.colorScheme.primary
                    },
                    CircleShape
                )
        )

        // Secondary Blob
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = blob2Offset, y = blob1Offset)
                .alpha(0.3f)
                .blur(80.dp)
                .background(
                    when(style) {
                        LyricsStyle.Romantic -> Color(0xFFFF80AB)
                        LyricsStyle.Energetic -> Color(0xFFFFC107)
                        LyricsStyle.Sad -> Color(0xFFB0BEC5)
                        LyricsStyle.Chill -> Color(0xFF81D4FA)
                        else -> MaterialTheme.colorScheme.secondary
                    },
                    CircleShape
                )
        )

        // 2. Blurred Artwork (Top Layer for Color matching)
        if (artworkUrl != null) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(if (style is LyricsStyle.Chill) 120.dp else 100.dp)
                    .alpha(if (isDarkTheme) 0.5f else 0.3f),
                contentScale = ContentScale.Crop
            )
        }
        
        // 3. Dynamic Overlay Gradient (Ensures legibility)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            overlayColor.copy(alpha = 0.2f),
                            overlayColor.copy(alpha = 0.6f),
                            overlayColor.copy(alpha = 0.85f)
                        )
                    )
                )
        )
    }
}

// Minimal MaterialTheme access if needed, or use the provided colors
@Composable
private fun BlobColor(style: LyricsStyle, isPrimary: Boolean): Color {
    return when(style) {
        LyricsStyle.Romantic -> if(isPrimary) Color(0xFFE91E63) else Color(0xFFFF80AB)
        LyricsStyle.Energetic -> if(isPrimary) Color(0xFFFF5722) else Color(0xFFFFC107)
        LyricsStyle.Sad -> if(isPrimary) Color(0xFF607D8B) else Color(0xFFB0BEC5)
        LyricsStyle.Chill -> if(isPrimary) Color(0xFF2196F3) else Color(0xFF81D4FA)
        else -> if(isPrimary) Color(0xFF6200EE) else Color(0xFF03DAC6)
    }
}
