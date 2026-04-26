package com.suvojeet.suvmusic.composeapp.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Android implementation — surfaces Material You on API 31+ via
 * `dynamicDarkColorScheme` / `dynamicLightColorScheme`. Returns null on
 * older devices so the caller picks a static palette.
 */
@Composable
actual fun DynamicColorScheme(darkTheme: Boolean): ColorScheme? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val context = LocalContext.current
    return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}
