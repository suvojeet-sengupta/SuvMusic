package com.suvojeet.suvmusic.composeapp.theme

import androidx.compose.runtime.Composable

/**
 * Desktop implementation — no-op. Compose Desktop windows draw inside an
 * OS-decorated frame; status / navigation bar styling isn't a concept
 * here. Skiko handles its own surface rendering and the Windows window
 * manager owns the chrome.
 */
@Composable
actual fun ApplySystemBars(darkTheme: Boolean) {
    // intentionally empty
}
