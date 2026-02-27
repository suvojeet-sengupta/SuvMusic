package com.suvojeet.suvmusic.ui.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

/**
 * Modifier to animate item entrance with a staggered slide-up and fade-in effect.
 * Best used in LazyLists where items appear sequentially.
 *
 * @param index The index of the item in the list, used to calculate delay.
 */
fun Modifier.animateEnter(
    index: Int,
    delayPerItem: Int = 15, // Faster stagger
    slideDistance: Float = 30f // Reduced distance for snappier feel
): Modifier = composed {
    val alpha = remember { Animatable(0f) }
    val translationY = remember { Animatable(slideDistance) }

    LaunchedEffect(Unit) {
        val delay = index * delayPerItem
        
        // Parallel animations using launch inside the LaunchedEffect scope
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 200, delayMillis = delay)
            )
        }
        launch {
            translationY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 200, delayMillis = delay)
            )
        }
    }

    this.graphicsLayer {
        this.alpha = alpha.value
        this.translationY = translationY.value
        this.clip = false // Optimization: avoid clipping during entrance if not needed
    }
}
