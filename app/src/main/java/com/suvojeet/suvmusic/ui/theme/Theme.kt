package com.suvojeet.suvmusic.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat

import com.suvojeet.suvmusic.core.model.AppTheme
import com.suvojeet.suvmusic.ui.components.DominantColors

/**
 * Dark color scheme - Primary for SuvMusic (Default/Purple)
 */
private val DarkColorScheme = darkColorScheme(
    primary = Purple70,
    onPrimary = Purple10,
    primaryContainer = Purple30,
    onPrimaryContainer = Purple90,
    secondary = Cyan70,
    onSecondary = Cyan10,
    secondaryContainer = Cyan30,
    onSecondaryContainer = Cyan90,
    tertiary = Magenta70,
    onTertiary = Magenta10,
    tertiaryContainer = Magenta30,
    onTertiaryContainer = Magenta90,
    background = Color.Black,
    onBackground = Neutral90,
    surface = Color.Black,
    onSurface = Neutral90,
    surfaceVariant = NeutralVar30,
    onSurfaceVariant = NeutralVar80,
    error = Error80,
    onError = Error20,
    errorContainer = Error30,
    onErrorContainer = Error90,
    outline = NeutralVar60,
    outlineVariant = NeutralVar30,
    scrim = Color.Black
)

/**
 * Light color scheme (Default/Purple)
 */
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    onPrimary = Color.White,
    primaryContainer = Purple90,
    onPrimaryContainer = Purple10,
    secondary = Cyan40,
    onSecondary = Color.White,
    secondaryContainer = Cyan90,
    onSecondaryContainer = Cyan10,
    tertiary = Magenta40,
    onTertiary = Color.White,
    tertiaryContainer = Magenta90,
    onTertiaryContainer = Magenta10,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = NeutralVar90,
    onSurfaceVariant = NeutralVar30,
    error = Error40,
    onError = Color.White,
    errorContainer = Error90,
    onErrorContainer = Error10,
    outline = NeutralVar50,
    outlineVariant = NeutralVar80,
    scrim = Color.Black
)

// --- Ocean Blue Schemes ---
private val OceanDarkColorScheme = darkColorScheme(
    primary = Blue80, onPrimary = Blue20, primaryContainer = Blue30, onPrimaryContainer = Blue90,
    secondary = Teal80, onSecondary = Teal20, secondaryContainer = Teal30, onSecondaryContainer = Teal90,
    tertiary = Purple80, onTertiary = Purple20, tertiaryContainer = Purple30, onTertiaryContainer = Purple90,
    background = Color.Black, onBackground = Neutral90, surface = Color.Black, onSurface = Neutral90
)

private val OceanLightColorScheme = lightColorScheme(
    primary = Blue40, onPrimary = Color.White, primaryContainer = Blue90, onPrimaryContainer = Blue10,
    secondary = Teal40, onSecondary = Color.White, secondaryContainer = Teal90, onSecondaryContainer = Teal10,
    tertiary = Purple40, onTertiary = Color.White, tertiaryContainer = Purple90, onTertiaryContainer = Purple10,
    background = Neutral99, onBackground = Neutral10, surface = Neutral99, onSurface = Neutral10
)

// --- Sunset Orange Schemes ---
private val SunsetDarkColorScheme = darkColorScheme(
    primary = Orange80, onPrimary = Orange20, primaryContainer = Orange30, onPrimaryContainer = Orange90,
    secondary = Gold80, onSecondary = Gold20, secondaryContainer = Gold30, onSecondaryContainer = Gold90,
    tertiary = Pink80, onTertiary = Pink20, tertiaryContainer = Pink30, onTertiaryContainer = Pink90,
    background = Color.Black, onBackground = Neutral90, surface = Color.Black, onSurface = Neutral90
)

private val SunsetLightColorScheme = lightColorScheme(
    primary = Orange40, onPrimary = Color.White, primaryContainer = Orange90, onPrimaryContainer = Orange10,
    secondary = Gold40, onSecondary = Color.White, secondaryContainer = Gold90, onSecondaryContainer = Gold10,
    tertiary = Pink40, onTertiary = Color.White, tertiaryContainer = Pink90, onTertiaryContainer = Pink10,
    background = Neutral99, onBackground = Neutral10, surface = Neutral99, onSurface = Neutral10
)

// --- Nature Green Schemes ---
private val NatureDarkColorScheme = darkColorScheme(
    primary = Green80, onPrimary = Green20, primaryContainer = Green30, onPrimaryContainer = Green90,
    secondary = Lime80, onSecondary = Lime20, secondaryContainer = Lime30, onSecondaryContainer = Lime90,
    tertiary = Teal80, onTertiary = Teal20, tertiaryContainer = Teal30, onTertiaryContainer = Teal90,
    background = Color.Black, onBackground = Neutral90, surface = Color.Black, onSurface = Neutral90
)

private val NatureLightColorScheme = lightColorScheme(
    primary = Green40, onPrimary = Color.White, primaryContainer = Green90, onPrimaryContainer = Green10,
    secondary = Lime40, onSecondary = Color.White, secondaryContainer = Lime90, onSecondaryContainer = Lime10,
    tertiary = Teal40, onTertiary = Color.White, tertiaryContainer = Teal90, onTertiaryContainer = Teal10,
    background = Neutral99, onBackground = Neutral10, surface = Neutral99, onSurface = Neutral10
)

// --- Love Pink Schemes ---
private val LoveDarkColorScheme = darkColorScheme(
    primary = Pink80, onPrimary = Pink20, primaryContainer = Pink30, onPrimaryContainer = Pink90,
    secondary = Rose80, onSecondary = Rose20, secondaryContainer = Rose30, onSecondaryContainer = Rose90,
    tertiary = Orange80, onTertiary = Orange20, tertiaryContainer = Orange30, onTertiaryContainer = Orange90,
    background = Color.Black, onBackground = Neutral90, surface = Color.Black, onSurface = Neutral90
)

private val LoveLightColorScheme = lightColorScheme(
    primary = Pink40, onPrimary = Color.White, primaryContainer = Pink90, onPrimaryContainer = Pink10,
    secondary = Rose40, onSecondary = Color.White, secondaryContainer = Rose90, onSecondaryContainer = Rose10,
    tertiary = Orange40, onTertiary = Color.White, tertiaryContainer = Orange90, onTertiaryContainer = Orange10,
    background = Neutral99, onBackground = Neutral10, surface = Neutral99, onSurface = Neutral10
)

/**
 * Creates a Material3 ColorScheme from extracted dominant colors.
 */
private fun createColorSchemeFromDominantColors(
    colors: DominantColors,
    darkTheme: Boolean
): ColorScheme {
    val primary = colors.accent
    val onPrimary = if (darkTheme) Color.Black else Color.White
    
    return if (darkTheme) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = colors.secondary,
            onPrimaryContainer = colors.onBackground,
            secondary = colors.secondary,
            onSecondary = colors.onBackground,
            secondaryContainer = colors.secondary.copy(alpha = 0.3f),
            onSecondaryContainer = colors.onBackground,
            tertiary = colors.accent,
            onTertiary = onPrimary,
            background = colors.primary,
            onBackground = colors.onBackground,
            surface = colors.primary,
            onSurface = colors.onBackground,
            surfaceVariant = colors.secondary,
            onSurfaceVariant = colors.onBackground,
            outline = colors.secondary,
            outlineVariant = colors.primary,
        )
    } else {
        val lightPrimary = vividLightAccent(colors.accent)
        lightColorScheme(
            primary = lightPrimary,
            onPrimary = Color.White,
            primaryContainer = lerp(Color.White, lightPrimary, 0.16f),
            onPrimaryContainer = lerp(lightPrimary, Color.Black, 0.55f),
            secondary = lerp(lightPrimary, Neutral40, 0.5f),
            onSecondary = Color.White,
            secondaryContainer = lerp(Color.White, lightPrimary, 0.11f),
            onSecondaryContainer = lerp(lightPrimary, Color.Black, 0.6f),
            tertiary = lightPrimary,
            onTertiary = Color.White,
            onBackground = Neutral10,
            onSurface = Neutral10,
            onSurfaceVariant = NeutralVar30,
        ).withTintedLightSurfaces(lightPrimary)
    }
}

private fun vividLightAccent(accent: Color): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(accent.toArgb(), hsl)
    if (hsl[1] < 0.08f) return Color(0xFF4A5568)
    hsl[1] = hsl[1].coerceIn(0.45f, 0.85f)
    hsl[2] = hsl[2].coerceIn(0.36f, 0.44f)
    return Color(ColorUtils.HSLToColor(hsl))
}

private fun ColorScheme.withTintedLightSurfaces(tint: Color): ColorScheme {
    val base = Color(0xFFFCFCFD)
    return copy(
        background = lerp(base, tint, 0.025f),
        surface = lerp(base, tint, 0.025f),
        surfaceBright = lerp(base, tint, 0.025f),
        surfaceDim = lerp(Color(0xFFDCDCDF), tint, 0.06f),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = lerp(Color(0xFFF7F7F8), tint, 0.035f),
        surfaceContainer = lerp(Color(0xFFF2F2F4), tint, 0.045f),
        surfaceContainerHigh = lerp(Color(0xFFECECEF), tint, 0.055f),
        surfaceContainerHighest = lerp(Color(0xFFE6E6EA), tint, 0.065f),
        surfaceVariant = lerp(Color(0xFFE9E9ED), tint, 0.06f),
        surfaceTint = tint,
        outline = lerp(Color(0xFF7A7A80), tint, 0.08f),
        outlineVariant = lerp(Color(0xFFCACACF), tint, 0.08f),
        inverseSurface = Color(0xFF2F3033),
        inverseOnSurface = Color(0xFFF1F1F3),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SuvMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    pureBlack: Boolean = false,
    appTheme: AppTheme = AppTheme.DEFAULT,
    albumArtColors: DominantColors? = null,
    content: @Composable () -> Unit
) {
    // M3E Fast Expressive animation for song-based color changes
    val animatedColors = if (albumArtColors != null) {
        val animatedPrimary by animateColorAsState(
            targetValue = albumArtColors.primary,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
            label = "theme_primary"
        )
        val animatedSecondary by animateColorAsState(
            targetValue = albumArtColors.secondary,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
            label = "theme_secondary"
        )
        val animatedAccent by animateColorAsState(
            targetValue = albumArtColors.accent,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
            label = "theme_accent"
        )
        val animatedOnBg by animateColorAsState(
            targetValue = albumArtColors.onBackground,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
            label = "theme_onBg"
        )
        remember(animatedPrimary, animatedSecondary, animatedAccent, animatedOnBg) {
            DominantColors(
                primary = animatedPrimary,
                secondary = animatedSecondary,
                accent = animatedAccent,
                onBackground = animatedOnBg
            )
        }
    } else null

    var colorScheme = when {
        animatedColors != null -> {
            createColorSchemeFromDominantColors(animatedColors, darkTheme)
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> when (appTheme) {
            AppTheme.DEFAULT -> DarkColorScheme
            AppTheme.OCEAN -> OceanDarkColorScheme
            AppTheme.SUNSET -> SunsetDarkColorScheme
            AppTheme.NATURE -> NatureDarkColorScheme
            AppTheme.LOVE -> LoveDarkColorScheme
        }
        else -> when (appTheme) {
            AppTheme.DEFAULT -> LightColorScheme
            AppTheme.OCEAN -> OceanLightColorScheme
            AppTheme.SUNSET -> SunsetLightColorScheme
            AppTheme.NATURE -> NatureLightColorScheme
            AppTheme.LOVE -> LoveLightColorScheme
        }.let { it.withTintedLightSurfaces(it.primary) }
    }

    if (darkTheme && pureBlack) {
        colorScheme = colorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color.Black,
            surfaceContainer = Color.Black,
            surfaceContainerHigh = Color.Black,
            surfaceContainerHighest = Color.Black,
            surfaceContainerLow = Color.Black,
            surfaceContainerLowest = Color.Black,
            scrim = Color.Black
        )
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}