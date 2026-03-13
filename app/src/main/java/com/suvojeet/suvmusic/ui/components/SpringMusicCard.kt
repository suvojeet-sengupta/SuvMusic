package com.suvojeet.suvmusic.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import com.suvojeet.suvmusic.core.model.Song

/**
 * A wrapper around [MusicCard] that adds a bouncy spring-based scale animation on press.
 */
@Composable
fun SpringMusicCard(
    song: Song,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    isPlaying: Boolean = false,
    backgroundColor: Color? = null,
    shape: Shape? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_scale"
    )
    
    Box(
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }
    ) {
        MusicCard(
            song = song,
            onClick = onClick,
            onMoreClick = onMoreClick,
            isPlaying = isPlaying,
            backgroundColor = backgroundColor,
            interactionSource = interactionSource
        )
    }
}
