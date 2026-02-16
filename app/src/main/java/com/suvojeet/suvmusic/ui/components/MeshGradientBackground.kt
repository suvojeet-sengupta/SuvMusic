package com.suvojeet.suvmusic.ui.components

import androidx.compose.animation.animateColorAsState
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

    // Animate colors for smooth transitions when song changes
    val animatedPrimary by animateColorAsState(
        targetValue = colors.primary,
        animationSpec = tween(1000),
        label = "primary_color_anim"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = colors.secondary,
        animationSpec = tween(1000),
        label = "secondary_color_anim"
    )
    val animatedAccent by animateColorAsState(
        targetValue = colors.accent,
        animationSpec = tween(1000),
        label = "accent_color_anim"
    )

    androidx.compose.runtime.LaunchedEffect(colors) {
        android.util.Log.d("MeshGradient", "Colors updated: primary=${colors.primary}")
    }

    val infiniteTransition = rememberInfiniteTransition(label = "mesh_gradient_motion")
    
    // Blob 1: Top-Left movement
    val x1 by infiniteTransition.animateFloat(
        initialValue = 0.1f, targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse),
        label = "x1"
    )
    val y1 by infiniteTransition.animateFloat(
        initialValue = 0.1f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Reverse),
        label = "y1"
    )

    // Blob 2: Top-Right movement
    val x2 by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Reverse),
        label = "x2"
    )
    val y2 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse),
        label = "y2"
    )

    // Blob 3: Bottom-Left movement
    val x3 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Reverse),
        label = "x3"
    )
    val y3 by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Reverse),
        label = "y3"
    )

    // Blob 4: Bottom-Right movement
    val x4 by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Reverse),
        label = "x4"
    )
    val y4 by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(19000, easing = LinearEasing), RepeatMode.Reverse),
        label = "y4"
    )

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            // Draw 4 distinct blobs with large radii and overlap
            
            // Blob 1: Primary
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(animatedPrimary.copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(width * x1, height * y1),
                    radius = width * 1.2f
                ),
                radius = width * 1.2f,
                center = Offset(width * x1, height * y1)
            )
            
            // Blob 2: Secondary
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(animatedSecondary.copy(alpha = 0.4f), Color.Transparent),
                    center = Offset(width * x2, height * y2),
                    radius = width * 1.1f
                ),
                radius = width * 1.1f,
                center = Offset(width * x2, height * y2)
            )
            
            // Blob 3: Accent
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(animatedAccent.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(width * x3, height * y3),
                    radius = width * 1.0f
                ),
                radius = width * 1.0f,
                center = Offset(width * x3, height * y3)
            )

            // Blob 4: Primary Variation
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(animatedPrimary.copy(alpha = 0.3f), Color.Transparent),
                    center = Offset(width * x4, height * y4),
                    radius = width * 1.3f
                ),
                radius = width * 1.3f,
                center = Offset(width * x4, height * y4)
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
