package com.suvojeet.suvmusic.composeapp.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/**
 * Returns the platform's "dynamic colour" scheme (Material You on Android
 * 12+) or null if dynamic colour isn't available on this platform / OS
 * version. Desktop never has it; older Android versions return null too.
 *
 * The caller in [SuvMusicTheme] falls back to a static palette when this
 * returns null. Kept as an `expect` rather than a noop so each platform
 * can decide whether to peek at the OS theme.
 */
@Composable
expect fun DynamicColorScheme(darkTheme: Boolean): ColorScheme?
