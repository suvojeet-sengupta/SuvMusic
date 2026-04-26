package com.suvojeet.suvmusic.composeapp.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/**
 * Desktop implementation — Material You doesn't apply outside Android, so
 * this always returns null and lets the static palette path win.
 */
@Composable
actual fun DynamicColorScheme(darkTheme: Boolean): ColorScheme? = null
