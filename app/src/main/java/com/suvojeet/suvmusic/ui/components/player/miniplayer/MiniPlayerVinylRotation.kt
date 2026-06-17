package com.suvojeet.suvmusic.ui.components.player.miniplayer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suvojeet.suvmusic.data.SessionManager
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

/**
 * Spins the mini-player artwork like a record when the Vinyl artwork shape is selected,
 * mirroring the full player. Rotation is gated on the same "rotating vinyl animation"
 * setting the full player uses and only runs while playing, so the two stay in sync.
 *
 * Returns a `() -> Float` provider (degrees). Read it inside `Modifier.graphicsLayer { }`
 * so per-frame rotation only invalidates the draw layer, not the whole composition.
 *
 * All hooks are called unconditionally (Rules of Composition) — only the animation loop
 * is gated — so switching artwork shape at runtime never reshapes the slot table.
 */
@Composable
fun rememberMiniPlayerVinylRotation(
    artworkShape: String,
    isPlaying: Boolean
): () -> Float {
    val isVinyl = artworkShape == "VINYL"

    val sessionManager: SessionManager = koinInject()
    val rotatingEnabled by sessionManager.rotatingVinylAnimationEnabledFlow
        .collectAsStateWithLifecycle(initialValue = false)

    val rotation = remember { Animatable(0f) }

    LaunchedEffect(isVinyl, isPlaying, rotatingEnabled) {
        if (isVinyl && isPlaying && rotatingEnabled) {
            try {
                // One full turn every 8s, looped — matches the full player's cadence.
                while (true) {
                    rotation.animateTo(
                        targetValue = rotation.value + 360f,
                        animationSpec = tween(8000, easing = LinearEasing)
                    )
                }
            } finally {
                // Settle on cancellation so the Animatable isn't left mid-run.
                withContext(NonCancellable) { rotation.snapTo(rotation.value) }
            }
        } else {
            rotation.stop()
            // Reset when vinyl is deselected so a later re-select starts upright.
            if (!isVinyl) rotation.snapTo(0f)
        }
    }

    return { rotation.value }
}
