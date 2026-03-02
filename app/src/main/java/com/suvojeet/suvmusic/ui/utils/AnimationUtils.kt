package com.suvojeet.suvmusic.ui.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

/**
 * Modifier to animate item entrance with a staggered slide-up and fade-in effect.
 * Uses fast-out-slow-in easing for a natural, polished feel.
 * Index is capped so items far down the list don't wait excessively.
 *
 * @param index The index of the item in the list, used to calculate delay.
 */
fun Modifier.animateEnter(
    index: Int,
    delayPerItem: Int = 20,
    slideDistance: Float = 24f
): Modifier = composed {
    val alpha = remember { Animatable(0f) }
    val translationY = remember { Animatable(slideDistance) }

    LaunchedEffect(Unit) {
        // Cap delay so deeply-nested items still feel snappy
        val cappedIndex = index.coerceAtMost(8)
        val delay = cappedIndex * delayPerItem
        
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 280,
                    delayMillis = delay,
                    easing = FastOutSlowInEasing
                )
            )
        }
        launch {
            translationY.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 320,
                    delayMillis = delay,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    this.graphicsLayer {
        this.alpha = alpha.value
        this.translationY = translationY.value
    }
}
