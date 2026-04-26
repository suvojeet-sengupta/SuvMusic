package com.suvojeet.suvmusic.composeapp.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * Holder for the colors derived from an album-art image. Mirrors the
 * Android `DominantColors` data class in app/.../ui/components/
 * DominantColorExtractor.kt — same fields, same defaults — so the
 * downstream render code reads identically on both targets.
 */
data class DominantColors(
    val primary: Color = Color(0xFF1A1A1A),
    val secondary: Color = Color(0xFF2A2A2A),
    val accent: Color = Color(0xFF888888),
    val onBackground: Color = Color.White,
)

/**
 * Theme-aware defaults so the UI doesn't flicker between songs.
 */
internal fun defaultDominantColors(isDarkTheme: Boolean): DominantColors = if (isDarkTheme) {
    DominantColors(
        primary = Color(0xFF1A1A1A),
        secondary = Color(0xFF2A2A2A),
        accent = Color(0xFF888888),
        onBackground = Color.White,
    )
} else {
    DominantColors(
        primary = Color(0xFFF5F5F5),
        secondary = Color(0xFFE8E8E8),
        accent = Color(0xFF666666),
        onBackground = Color(0xFF1A1A1A),
    )
}

/**
 * Platform-specific extractor. Each actual fetches the image bytes via
 * its own HTTP path and decodes via the platform-native bitmap API
 * (android.graphics.Bitmap on Android, javax.imageio.BufferedImage on
 * Desktop) before sampling pixels.
 *
 * Returns null if the image can't be loaded or decoded — callers fall
 * back to the theme-aware defaults.
 */
internal expect class PlatformDominantColorExtractor() {
    suspend fun extract(imageUrl: String, isDarkTheme: Boolean): DominantColors?
}

/**
 * Composable wrapper. Loads the image off the main thread, samples the
 * average RGB, and synthesises primary / secondary / accent colours
 * shaded for the current theme. Same algorithm shape as the Android
 * version (tuned for Material 3 Expressive backgrounds).
 *
 * `imageUrl == null` returns the theme defaults immediately and never
 * dispatches a load.
 */
@Composable
fun rememberDominantColors(
    imageUrl: String?,
    isDarkTheme: Boolean = true,
): DominantColors {
    val extractor = remember { PlatformDominantColorExtractor() }
    val themeAwareDefaults = remember(isDarkTheme) { defaultDominantColors(isDarkTheme) }
    var colors by remember(themeAwareDefaults) { mutableStateOf(themeAwareDefaults) }

    LaunchedEffect(imageUrl, isDarkTheme) {
        if (imageUrl == null) {
            colors = themeAwareDefaults
            return@LaunchedEffect
        }
        extractor.extract(imageUrl, isDarkTheme)?.let { colors = it }
    }

    return colors
}

/**
 * Build [DominantColors] from average RGB plus the theme. Same scaling
 * the Android version uses (dark = 0.3x/0.5x of avg; light = mix with
 * 85% white for pastel backgrounds).
 */
internal fun buildDominantColors(
    avgR: Int,
    avgG: Int,
    avgB: Int,
    isDarkTheme: Boolean,
): DominantColors {
    val primary: Color
    val secondary: Color
    val onBackground: Color

    if (isDarkTheme) {
        primary = Color(
            red = (avgR * 0.3f / 255f).coerceIn(0f, 1f),
            green = (avgG * 0.3f / 255f).coerceIn(0f, 1f),
            blue = (avgB * 0.3f / 255f).coerceIn(0f, 1f),
        )
        secondary = Color(
            red = (avgR * 0.5f / 255f).coerceIn(0f, 1f),
            green = (avgG * 0.5f / 255f).coerceIn(0f, 1f),
            blue = (avgB * 0.5f / 255f).coerceIn(0f, 1f),
        )
        onBackground = Color.White
    } else {
        primary = Color(
            red = (avgR * 0.2f / 255f + 0.85f).coerceIn(0f, 1f),
            green = (avgG * 0.2f / 255f + 0.85f).coerceIn(0f, 1f),
            blue = (avgB * 0.2f / 255f + 0.85f).coerceIn(0f, 1f),
        )
        secondary = Color(
            red = (avgR * 0.3f / 255f + 0.75f).coerceIn(0f, 1f),
            green = (avgG * 0.3f / 255f + 0.75f).coerceIn(0f, 1f),
            blue = (avgB * 0.3f / 255f + 0.75f).coerceIn(0f, 1f),
        )
        onBackground = Color(0xFF1A1A1A)
    }

    val accent = saturatedAccent(avgR, avgG, avgB, isDarkTheme)
    return DominantColors(primary, secondary, accent, onBackground)
}

/**
 * Build the saturated accent. Same recipe as Android: convert RGB to HSL,
 * boost saturation 1.2x, force lightness to 0.6 (dark theme) or 0.5
 * (light theme), then back to RGB. Pure math, no platform deps.
 */
private fun saturatedAccent(r: Int, g: Int, b: Int, isDarkTheme: Boolean): Color {
    val (hue, sat, _) = rgbToHsl(r, g, b)
    val newSat = (sat * 1.2f).coerceIn(0f, 1f)
    val lightness = if (isDarkTheme) 0.6f else 0.5f
    val (rr, gg, bb) = hslToRgb(hue, newSat, lightness)
    return Color(red = rr / 255f, green = gg / 255f, blue = bb / 255f)
}

private fun rgbToHsl(r: Int, g: Int, b: Int): Triple<Float, Float, Float> {
    val rf = r / 255f
    val gf = g / 255f
    val bf = b / 255f
    val max = maxOf(rf, gf, bf)
    val min = minOf(rf, gf, bf)
    val l = (max + min) / 2f
    if (max == min) return Triple(0f, 0f, l)
    val d = max - min
    val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
    val h = when (max) {
        rf -> ((gf - bf) / d + if (gf < bf) 6f else 0f)
        gf -> ((bf - rf) / d + 2f)
        else -> ((rf - gf) / d + 4f)
    } * 60f
    return Triple(h, s, l)
}

private fun hslToRgb(h: Float, s: Float, l: Float): Triple<Int, Int, Int> {
    if (s == 0f) {
        val gray = (l * 255f).toInt().coerceIn(0, 255)
        return Triple(gray, gray, gray)
    }
    val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
    val p = 2f * l - q
    val hk = h / 360f
    val tr = hueToRgb(p, q, hk + 1f / 3f)
    val tg = hueToRgb(p, q, hk)
    val tb = hueToRgb(p, q, hk - 1f / 3f)
    return Triple(
        (tr * 255f).toInt().coerceIn(0, 255),
        (tg * 255f).toInt().coerceIn(0, 255),
        (tb * 255f).toInt().coerceIn(0, 255),
    )
}

private fun hueToRgb(p: Float, q: Float, tIn: Float): Float {
    var t = tIn
    if (t < 0f) t += 1f
    if (t > 1f) t -= 1f
    return when {
        t < 1f / 6f -> p + (q - p) * 6f * t
        t < 1f / 2f -> q
        t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f
        else -> p
    }
}
