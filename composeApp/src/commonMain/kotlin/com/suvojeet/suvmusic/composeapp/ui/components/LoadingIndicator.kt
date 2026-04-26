package com.suvojeet.suvmusic.composeapp.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Loading-indicator family — port of `app/.../ui/components/LoadingIndicator.kt`.
 *
 * The Android original delegated [LoadingIndicator] to the M3 Expressive
 * `androidx.compose.material3.LoadingIndicator` ("bouncy dots"). To keep
 * commonMain stable across Compose Multiplatform versions where that
 * symbol may still be experimental, this port uses
 * [CircularProgressIndicator] for the standard variant. Visually similar
 * spinner; we can swap to the bouncy-dots variant in a follow-up once
 * CMP exposes it as stable.
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    CircularProgressIndicator(
        modifier = modifier,
        color = color,
    )
}

/**
 * A pulsing loading indicator that mimics a mix of Apple Music and Spotify
 * styles — a pulsing glow behind a central music icon. Verbatim port.
 */
@Composable
fun PulseLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "PulseScale",
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "PulseAlpha",
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .scale(scale)
                .background(color.copy(alpha = alpha * 0.5f), CircleShape),
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .scale(scale * 0.9f)
                .background(color.copy(alpha = alpha), CircleShape),
        )
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = "Loading",
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
    }
}

/**
 * Overlay placed on top of artwork while a song loads. Same semantics as
 * the Android original.
 */
@Composable
fun LoadingArtworkOverlay(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!isVisible) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center,
    ) {
        PulseLoadingIndicator(color = Color.White)
    }
}
