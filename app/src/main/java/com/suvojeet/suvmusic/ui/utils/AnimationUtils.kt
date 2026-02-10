package com.suvojeet.suvmusic.ui.utils

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Modifier to animate item entrance with a staggered slide-up and fade-in effect.
 * Best used in LazyLists where items appear sequentially.
 *
 * @param index The index of the item in the list, used to calculate delay.
 */
fun Modifier.animateEnter(
    index: Int,
    delayPerItem: Int = 50,
    slideDistance: Float = 50f
): Modifier = composed {
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
    val transition = rememberTransition(visibleState, label = "enter_transition")
    
    val alpha by transition.animateFloat(
        transitionSpec = {
            tween(durationMillis = 300, delayMillis = index * delayPerItem)
        },
        label = "alpha"
    ) { visible -> if (visible) 1f else 0f }
    
    val translationY by transition.animateFloat(
        transitionSpec = {
            tween(durationMillis = 300, delayMillis = index * delayPerItem)
        },
        label = "translationY"
    ) { visible -> if (visible) 0f else slideDistance }

    this.graphicsLayer {
        this.alpha = alpha
        this.translationY = translationY
    }
}
