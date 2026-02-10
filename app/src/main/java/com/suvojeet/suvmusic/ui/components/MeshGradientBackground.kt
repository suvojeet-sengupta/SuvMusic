package com.suvojeet.suvmusic.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.dp

/**
 * A dynamic, mesh-like gradient background that subtly animates based on dominant colors.
 * Replaces static backgrounds with a fluid, alive feel similar to YouTube Music.
 */
@Composable
fun MeshGradientBackground(
    dominantColors: DominantColors?, // Nullable to handle loading states gracefully
    modifier: Modifier = Modifier,
    speedMultiplier: Float = 1f
) {
    // If no colors provided yet, just show default background or a simple placeholder
    val colors = dominantColors ?: DominantColors(
        primary = MaterialTheme.colorScheme.primaryContainer,
        secondary = MaterialTheme.colorScheme.secondaryContainer,
        accent = MaterialTheme.colorScheme.tertiaryContainer,
        onBackground = MaterialTheme.colorScheme.onBackground
    )

    val infiniteTransition = rememberInfiniteTransition(label = "mesh_gradient_motion")
    
    // Animate positions of blobs to create "breathing" effect
    val offset1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob1_motion"
    )
    
    val offset2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob2_motion"
    )

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // We use a Canvas with a heavy blur effect to simulate mesh gradients
        // Since Blur needs SDK 31+ for RenderEffect, we'll use a gradient approximation for compatibility
        // or just large radial gradients which look like blobs.
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            // Primary Blob (Top-Right moving Left)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors.primary.copy(alpha = 0.4f), Color.Transparent),
                    center = Offset(
                        x = width * 0.8f - (width * 0.4f * offset1),
                        y = height * 0.2f + (height * 0.1f * offset2)
                    ),
                    radius = width * 0.8f,
                    tileMode = TileMode.Clamp
                ),
                radius = width * 0.8f,
                center = Offset(
                    x = width * 0.8f - (width * 0.4f * offset1),
                    y = height * 0.2f + (height * 0.1f * offset2)
                )
            )
            
            // Secondary Blob (Bottom-Left moving Right)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors.secondary.copy(alpha = 0.3f), Color.Transparent),
                    center = Offset(
                        x = width * 0.2f + (width * 0.3f * offset1),
                        y = height * 0.8f - (height * 0.2f * offset2)
                    ),
                    radius = width * 0.7f,
                    tileMode = TileMode.Clamp
                ),
                radius = width * 0.7f,
                center = Offset(
                     x = width * 0.2f + (width * 0.3f * offset1),
                    y = height * 0.8f - (height * 0.2f * offset2)
                )
            )
            
            // Accent Blob (Center-ish, pulsating)
             drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors.accent.copy(alpha = 0.2f), Color.Transparent),
                    center = Offset(
                        x = width * 0.5f,
                        y = height * 0.5f
                    ),
                    radius = width * 0.6f * (0.8f + (0.2f * offset2)), // Pulsating size
                     tileMode = TileMode.Clamp
                ),
                radius = width * 0.6f * (0.8f + (0.2f * offset2)),
                center = Offset(
                    x = width * 0.5f,
                    y = height * 0.5f
                )
            )
        }
        
        // Overlay a gradient to fade to pure background at the very bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.background
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )
    }
}
