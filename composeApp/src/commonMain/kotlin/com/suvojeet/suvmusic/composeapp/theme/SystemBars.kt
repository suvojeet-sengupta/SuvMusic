package com.suvojeet.suvmusic.composeapp.theme

import androidx.compose.runtime.Composable

/**
 * Apply transparent status / navigation bars and matching icon contrast
 * for the current theme. Android pulls the host [android.app.Activity]
 * window via LocalView and configures it through WindowCompat;
 * Desktop is a no-op (window chrome is owned by the OS / Skiko).
 *
 * This stays as an `expect` rather than a noop fallback because the
 * Android implementation needs LocalView, which doesn't exist in
 * commonMain.
 */
@Composable
expect fun ApplySystemBars(darkTheme: Boolean)
