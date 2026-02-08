package com.suvojeet.suvmusic.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.suvojeet.suvmusic.util.dpadFocusable

/**
 * Animated button with Apple Music style pressed effect.
 * Shows a dark shadow/glow on press with smooth animation.
 */
@Composable
fun BounceButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp? = null,
    shape: Shape = CircleShape,
    contentAlignment: Alignment = Alignment.Center,
    clickEnabled: Boolean = true,
    content: @Composable BoxScope.(isPressed: Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Animate the background alpha for smooth fade in/out
    // This provides a subtle "pressed" overlay
    val pressedOverlayAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.1f else 0f,
        animationSpec = tween(durationMillis = if (isPressed) 50 else 200),
        label = "pressedOverlayAlpha"
    )
    
    // "Jump" / Scale effect
    // Apple Music style: noticeable scale down with a bit of bounce (spring)
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "pressedScale"
    )
    
    // Apply size if provided
    val sizeModifier = if (size != null) Modifier.size(size) else Modifier
    
    Box(
        modifier = modifier
            .then(sizeModifier)
            .scale(scale)
            // Use dpadFocusable for focus handling only, NOT click handling
            // This avoids dpadFocusable adding its own clickable with defaults
            .dpadFocusable(
                onClick = null, 
                shape = shape,
                focusedScale = 1.05f
            )
            .clip(shape)
            // Manual click handling to remove ripple (indication = null)
            .then(
                if (clickEnabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = contentAlignment
    ) {
        // Content
        content(isPressed)
        
        // Pressed overlay
        if (pressedOverlayAlpha > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = pressedOverlayAlpha))
            )
        }
    }
}
