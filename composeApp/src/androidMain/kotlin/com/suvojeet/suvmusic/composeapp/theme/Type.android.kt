package com.suvojeet.suvmusic.composeapp.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

/**
 * Android implementation. The shared composeApp module currently doesn't
 * carry an Android `res/font/outfit.ttf`, so we fall back to the system
 * sans-serif here as well. When `:app` switches to consume this theme
 * (Phase B), it can supply Outfit via `LocalFontFamilyResolver` /
 * `MaterialTheme(typography = appTypography().with(...))` or by adding
 * the asset to `composeApp/src/androidMain/res/font/outfit.ttf`.
 */
@Composable
actual fun outfitFontFamily(): FontFamily = FontFamily.Default
