package com.suvojeet.suvmusic.ui.utils

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Utility composables for responsive layouts.
 */

/**
 * Returns true if the device is in landscape orientation.
 */
@Composable
fun isLandscape(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
}

/**
 * Returns true if the device is a tablet (smallest width >= 600dp).
 */
@Composable
fun isTablet(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.smallestScreenWidthDp >= 600
}

/**
 * Returns true if the device is a large tablet (smallest width >= 840dp).
 */
@Composable
fun isLargeTablet(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.smallestScreenWidthDp >= 840
}

/**
 * Sealed class representing window size categories.
 */
sealed class WindowSize {
    data object Compact : WindowSize()    // Phone portrait
    data object Medium : WindowSize()     // Phone landscape or small tablet
    data object Expanded : WindowSize()   // Large tablet
}

/**
 * Returns the current window size category based on screen width.
 */
@Composable
fun rememberWindowSize(): WindowSize {
    val configuration = LocalConfiguration.current
    return remember(configuration.screenWidthDp) {
        when {
            configuration.screenWidthDp >= 840 -> WindowSize.Expanded
            configuration.screenWidthDp >= 600 -> WindowSize.Medium
            else -> WindowSize.Compact
        }
    }
}

/**
 * Returns the screen width in dp.
 */
@Composable
fun screenWidthDp(): Int {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp
}

/**
 * Returns the screen height in dp.
 */
@Composable
fun screenHeightDp(): Int {
    val configuration = LocalConfiguration.current
    return configuration.screenHeightDp
}
