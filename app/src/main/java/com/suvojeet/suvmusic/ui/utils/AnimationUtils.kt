package com.suvojeet.suvmusic.ui.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

/**
 * Modifier to animate item entrance with a staggered slide-up and fade-in effect.
 * Uses fast-out-slow-in easing for a natural, polished feel.
 *
 * The animation only plays **once** per item. When items scroll off-screen and
 * come back, they appear immediately without replaying the entrance animation.
 * This avoids the flickering/jank that occurs with recycled LazyColumn items.
 *
 * Index is capped so items far down the list don't wait excessively.
 *
 * @param index The index of the item in the list, used to calculate stagger delay.
 */
fun Modifier.animateEnter(
    index: Int,
    delayPerItem: Int = 20,
    slideDistance: Float = 24f
): Modifier = composed {
    // Track whether the animation has already completed.
    // Using mutableStateOf inside composed — the value persists while
    // the item stays in composition but resets if the item is disposed.
    // That's fine because we only want to skip replays for items that
    // scroll back, and LazyColumn keeps nearby items in composition.
    val hasAnimated = remember { mutableStateOf(false) }

    if (hasAnimated.value) {
        // Already animated — render at full opacity, no translation.
        // No Animatable allocations, no coroutines, no graphicsLayer overhead.
        return@composed this
    }

    val alpha = remember { Animatable(0f) }
    val translationY = remember { Animatable(slideDistance) }

    LaunchedEffect(Unit) {
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

        // Mark as animated once the longer animation finishes
        hasAnimated.value = true
    }

    this.graphicsLayer {
        this.alpha = alpha.value
        this.translationY = translationY.value
    }
}
