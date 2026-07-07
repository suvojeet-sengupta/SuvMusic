package com.suvojeet.suvmusic.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView

/**
 * App-wide haptic vocabulary so interactions feel consistent:
 * - [tick]  — light feedback: play/pause, chip select, toggle
 * - [thump] — medium feedback: like, add to queue, drag-drop commit
 * - [reject] — something was refused/removed
 *
 * Usage: `val haptics = rememberHaptics()` then `haptics.tick()`.
 */
@Stable
class AppHaptics(
    private val compose: HapticFeedback,
    private val performViewFeedback: (Int) -> Unit
) {
    fun tick() {
        performViewFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
    }

    fun thump() {
        compose.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun reject() {
        performViewFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY_RELEASE)
    }
}

@Composable
fun rememberHaptics(): AppHaptics {
    val composeHaptics = LocalHapticFeedback.current
    val view = LocalView.current
    return remember(composeHaptics, view) {
        AppHaptics(composeHaptics) { constant -> view.performHapticFeedback(constant) }
    }
}
