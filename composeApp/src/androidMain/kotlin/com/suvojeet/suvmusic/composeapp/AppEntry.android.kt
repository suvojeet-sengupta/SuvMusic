package com.suvojeet.suvmusic.composeapp

import androidx.compose.runtime.Composable

/**
 * Android entry point for the shared composeApp UI. The existing :app module
 * does not yet host this — it will be wired in during Phase 5 (UI to commonMain).
 * For Phase 0 this exists only so the androidMain source set has at least one
 * Kotlin file and compiles.
 */
@Composable
fun AndroidAppEntry() {
    App()
}
