package com.suvojeet.suvmusic.composeapp

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Entry composable shared between Android and Desktop. For now it just
 * renders the [SuvMusicAboutScreen] — the first real shared UI.
 *
 * Real screen routing lands in Phase 5 when the Compose-Multiplatform
 * navigation graph moves to commonMain.
 */
@Composable
fun App(
    appVersion: String = "0.0.0-dev",
    onOpenUrl: (String) -> Unit = {},
) {
    MaterialTheme {
        SuvMusicAboutScreen(
            appVersion = appVersion,
            onOpenUrl = onOpenUrl,
        )
    }
}
