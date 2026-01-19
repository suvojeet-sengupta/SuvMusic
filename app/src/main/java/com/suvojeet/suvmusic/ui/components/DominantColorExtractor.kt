package com.suvojeet.suvmusic.ui.components

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Data class holding dominant colors extracted from an image
 */
data class DominantColors(
    val primary: Color = Color(0xFF1A1A1A),
    val secondary: Color = Color(0xFF2A2A2A),
    val accent: Color = Color(0xFF888888),
    val onBackground: Color = Color.White
)

/**
 * Extracts dominant colors from an image URL
 */
@Composable
fun rememberDominantColors(
    imageUrl: String?,
    isDarkTheme: Boolean = true,
    defaultColors: DominantColors? = null
): DominantColors {
    // Use theme-aware default colors to prevent flickering when song changes
    val themeAwareDefaults = defaultColors ?: if (isDarkTheme) {
        DominantColors(
            primary = Color(0xFF1A1A1A),
            secondary = Color(0xFF2A2A2A),
            accent = Color(0xFF888888),
            onBackground = Color.White
        )
    } else {
        DominantColors(
            primary = Color(0xFFF5F5F5),
            secondary = Color(0xFFE8E8E8),
            accent = Color(0xFF666666),
            onBackground = Color(0xFF1A1A1A)
        )
    }
    
    var colors by remember(imageUrl, isDarkTheme) { mutableStateOf(themeAwareDefaults) }
    val context = LocalContext.current
    
    LaunchedEffect(imageUrl, isDarkTheme) {
        if (imageUrl == null) {
            colors = themeAwareDefaults
            return@LaunchedEffect
        }
        
        withContext(Dispatchers.IO) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .size(100) // Small size for faster processing
                    .build()
                
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    bitmap?.let {
                        colors = extractColorsFromBitmap(it, isDarkTheme)
                    }
                }
            } catch (e: Exception) {
                colors = themeAwareDefaults
            }
        }
    }
    
    return colors
}

/**
 * Extract colors from bitmap using averaging and palette detection
 * @param isDarkTheme If true, generates dark backgrounds; if false, generates light backgrounds
 */
private fun extractColorsFromBitmap(bitmap: Bitmap, isDarkTheme: Boolean = true): DominantColors {
    val width = bitmap.width
    val height = bitmap.height
    
    val colors = mutableListOf<Int>()
    val step = maxOf(1, minOf(width, height) / 10)
    
    // Sample pixels
    for (x in 0 until width step step) {
        for (y in 0 until height step step) {
            val pixel = bitmap.getPixel(x, y)
            colors.add(pixel)
        }
    }
    
    if (colors.isEmpty()) return DominantColors()
    
    // Calculate average color
    var totalR = 0L
    var totalG = 0L
    var totalB = 0L
    
    colors.forEach { color ->
        totalR += android.graphics.Color.red(color)
        totalG += android.graphics.Color.green(color)
        totalB += android.graphics.Color.blue(color)
    }
    
    val avgR = (totalR / colors.size).toInt()
    val avgG = (totalG / colors.size).toInt()
    val avgB = (totalB / colors.size).toInt()
    
    // Create colors based on theme
    val primary: Color
    val secondary: Color
    val onBackground: Color
    
    if (isDarkTheme) {
        // Dark theme: dark backgrounds with white text
        primary = Color(
            red = (avgR * 0.3f / 255f).coerceIn(0f, 1f),
            green = (avgG * 0.3f / 255f).coerceIn(0f, 1f),
            blue = (avgB * 0.3f / 255f).coerceIn(0f, 1f)
        )
        
        secondary = Color(
            red = (avgR * 0.5f / 255f).coerceIn(0f, 1f),
            green = (avgG * 0.5f / 255f).coerceIn(0f, 1f),
            blue = (avgB * 0.5f / 255f).coerceIn(0f, 1f)
        )
        
        onBackground = Color.White
    } else {
        // Light theme: light backgrounds with dark text
        // Mix with white to create light, pastel-like backgrounds
        primary = Color(
            red = (avgR * 0.2f / 255f + 0.85f).coerceIn(0f, 1f),
            green = (avgG * 0.2f / 255f + 0.85f).coerceIn(0f, 1f),
            blue = (avgB * 0.2f / 255f + 0.85f).coerceIn(0f, 1f)
        )
        
        secondary = Color(
            red = (avgR * 0.3f / 255f + 0.75f).coerceIn(0f, 1f),
            green = (avgG * 0.3f / 255f + 0.75f).coerceIn(0f, 1f),
            blue = (avgB * 0.3f / 255f + 0.75f).coerceIn(0f, 1f)
        )
        
        onBackground = Color(0xFF1A1A1A) // Dark gray, not pure black
    }
    
    // Create accent color (saturated version) - works for both themes
    val hsl = FloatArray(3)
    ColorUtils.RGBToHSL(avgR, avgG, avgB, hsl)
    hsl[1] = minOf(1f, hsl[1] * 1.5f) // Boost saturation
    hsl[2] = if (isDarkTheme) 0.6f else 0.5f // Slightly darker accent for light theme
    val accentInt = ColorUtils.HSLToColor(hsl)
    val accent = Color(accentInt)
    
    return DominantColors(
        primary = primary,
        secondary = secondary,
        accent = accent,
        onBackground = onBackground
    )
}
