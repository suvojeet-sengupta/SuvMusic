package com.suvojeet.suvmusic.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

import androidx.compose.material3.CircularProgressIndicator

/**
 * Standard loading indicator. Uses M3E expressive bouncy dots.
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    androidx.compose.material3.LoadingIndicator(
        modifier = modifier,
        color = color
    )
}

/**
 * A unique pulsing loading indicator that mimics a mix of Apple Music and Spotify styles.
 * It features a pulsing glow effect behind a central music icon.
 */
@Composable
fun PulseLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing glow
        Box(
            modifier = Modifier
                .size(60.dp)
                .scale(scale)
                .background(color.copy(alpha = alpha * 0.5f), CircleShape)
        )
        
        // Inner pulsing circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .scale(scale * 0.9f)
                .background(color.copy(alpha = alpha), CircleShape)
        )
        
        // Icon
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = "Loading",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Overlay to be placed on top of artwork when loading.
 */
@Composable
fun LoadingArtworkOverlay(
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)), // Semi-transparent overlay
        contentAlignment = Alignment.Center
    ) {
        PulseLoadingIndicator(
            color = Color.White
        )
    }
}
