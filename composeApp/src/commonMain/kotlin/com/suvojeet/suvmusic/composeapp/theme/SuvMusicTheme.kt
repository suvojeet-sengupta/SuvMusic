package com.suvojeet.suvmusic.composeapp.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.suvojeet.suvmusic.composeapp.image.DominantColors

/**
 * Shared SuvMusic theme — full 5-palette / dominant-color / animated
 * Material 3 Expressive scheme, ported from
 * `app/src/main/java/com/suvojeet/suvmusic/ui/theme/Theme.kt`.
 *
 * This commonMain version differs from the Android original in two ways:
 *  - Dynamic-Color (Material You) lookup is delegated to platform code via
 *    [DynamicColorScheme]. Android wires it through Build.VERSION_CODES.S +
 *    `dynamicDarkColorScheme(context)`; Desktop returns null and the
 *    static palettes always win.
 *  - Status / navigation bar styling moves to [ApplySystemBars] (expect /
 *    actual). Desktop's actual is a no-op.
 *
 * Everything else — colour tokens, animated dominant-art transitions,
 * pure-black AMOLED switch — is identical to the Android implementation.
 */
@Composable
fun SuvMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    pureBlack: Boolean = false,
    appTheme: AppTheme = AppTheme.DEFAULT,
    albumArtColors: DominantColors? = null,
    content: @Composable () -> Unit,
) {
    // M3E Fast Expressive animation — when album art changes, smoothly
    // morph the four canonical colours rather than snap.
    val animatedColors = if (albumArtColors != null) {
        val animatedPrimary by animateColorAsState(
            targetValue = albumArtColors.primary,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "theme_primary",
        )
        val animatedSecondary by animateColorAsState(
            targetValue = albumArtColors.secondary,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "theme_secondary",
        )
        val animatedAccent by animateColorAsState(
            targetValue = albumArtColors.accent,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "theme_accent",
        )
        val animatedOnBg by animateColorAsState(
            targetValue = albumArtColors.onBackground,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "theme_onBg",
        )
        remember(animatedPrimary, animatedSecondary, animatedAccent, animatedOnBg) {
            DominantColors(
                primary = animatedPrimary,
                secondary = animatedSecondary,
                accent = animatedAccent,
                onBackground = animatedOnBg,
            )
        }
    } else {
        null
    }

    val dynamicScheme = if (dynamicColor) DynamicColorScheme(darkTheme) else null

    var colorScheme: ColorScheme = when {
        animatedColors != null -> createColorSchemeFromDominantColors(animatedColors, darkTheme)
        dynamicScheme != null -> dynamicScheme
        darkTheme -> darkSchemeFor(appTheme)
        else -> lightSchemeFor(appTheme)
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
            scrim = Color.Black,
        )
    }

    ApplySystemBars(darkTheme = darkTheme)

    // NOTE: Android :app uses MaterialExpressiveTheme (M3E) here, but
    // that symbol is internal in CMP 1.10's material3 artifact. Falling
    // back to plain MaterialTheme — visually identical for our use
    // (M3E mostly adds motion-scheme + morph-shape support, not colour
    // semantics). Restore MaterialExpressiveTheme when CMP exposes it.
    MaterialTheme(
        colorScheme = colorScheme,
        typography = appTypography(),
        shapes = Shapes,
        content = content,
    )
}

// --------------------------------------------------------------------------
// Dark / Light static schemes — one per [AppTheme]. Verbatim values from
// the Android Theme.kt; just split into helpers so the chooser above reads
// cleanly.
// --------------------------------------------------------------------------

private fun darkSchemeFor(appTheme: AppTheme): ColorScheme = when (appTheme) {
    AppTheme.DEFAULT -> DefaultDark
    AppTheme.OCEAN -> OceanDark
    AppTheme.SUNSET -> SunsetDark
    AppTheme.NATURE -> NatureDark
    AppTheme.LOVE -> LoveDark
}

private fun lightSchemeFor(appTheme: AppTheme): ColorScheme = when (appTheme) {
    AppTheme.DEFAULT -> DefaultLight
    AppTheme.OCEAN -> OceanLight
    AppTheme.SUNSET -> SunsetLight
    AppTheme.NATURE -> NatureLight
    AppTheme.LOVE -> LoveLight
}

private val DefaultDark = darkColorScheme(
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
    scrim = Color.Black,
)

private val DefaultLight = lightColorScheme(
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
    scrim = Color.Black,
)

private val OceanDark = darkColorScheme(
    primary = Blue80, onPrimary = Blue20, primaryContainer = Blue30, onPrimaryContainer = Blue90,
    secondary = Teal80, onSecondary = Teal20, secondaryContainer = Teal30, onSecondaryContainer = Teal90,
    tertiary = Purple80, onTertiary = Purple20, tertiaryContainer = Purple30, onTertiaryContainer = Purple90,
    background = Color.Black, onBackground = Neutral90, surface = Color.Black, onSurface = Neutral90,
)
private val OceanLight = lightColorScheme(
    primary = Blue40, onPrimary = Color.White, primaryContainer = Blue90, onPrimaryContainer = Blue10,
    secondary = Teal40, onSecondary = Color.White, secondaryContainer = Teal90, onSecondaryContainer = Teal10,
    tertiary = Purple40, onTertiary = Color.White, tertiaryContainer = Purple90, onTertiaryContainer = Purple10,
    background = Neutral99, onBackground = Neutral10, surface = Neutral99, onSurface = Neutral10,
)
private val SunsetDark = darkColorScheme(
    primary = Orange80, onPrimary = Orange20, primaryContainer = Orange30, onPrimaryContainer = Orange90,
    secondary = Gold80, onSecondary = Gold20, secondaryContainer = Gold30, onSecondaryContainer = Gold90,
    tertiary = Pink80, onTertiary = Pink20, tertiaryContainer = Pink30, onTertiaryContainer = Pink90,
    background = Color.Black, onBackground = Neutral90, surface = Color.Black, onSurface = Neutral90,
)
private val SunsetLight = lightColorScheme(
    primary = Orange40, onPrimary = Color.White, primaryContainer = Orange90, onPrimaryContainer = Orange10,
    secondary = Gold40, onSecondary = Color.White, secondaryContainer = Gold90, onSecondaryContainer = Gold10,
    tertiary = Pink40, onTertiary = Color.White, tertiaryContainer = Pink90, onTertiaryContainer = Pink10,
    background = Neutral99, onBackground = Neutral10, surface = Neutral99, onSurface = Neutral10,
)
private val NatureDark = darkColorScheme(
    primary = Green80, onPrimary = Green20, primaryContainer = Green30, onPrimaryContainer = Green90,
    secondary = Lime80, onSecondary = Lime20, secondaryContainer = Lime30, onSecondaryContainer = Lime90,
    tertiary = Teal80, onTertiary = Teal20, tertiaryContainer = Teal30, onTertiaryContainer = Teal90,
    background = Color.Black, onBackground = Neutral90, surface = Color.Black, onSurface = Neutral90,
)
private val NatureLight = lightColorScheme(
    primary = Green40, onPrimary = Color.White, primaryContainer = Green90, onPrimaryContainer = Green10,
    secondary = Lime40, onSecondary = Color.White, secondaryContainer = Lime90, onSecondaryContainer = Lime10,
    tertiary = Teal40, onTertiary = Color.White, tertiaryContainer = Teal90, onTertiaryContainer = Teal10,
    background = Neutral99, onBackground = Neutral10, surface = Neutral99, onSurface = Neutral10,
)
private val LoveDark = darkColorScheme(
    primary = Pink80, onPrimary = Pink20, primaryContainer = Pink30, onPrimaryContainer = Pink90,
    secondary = Rose80, onSecondary = Rose20, secondaryContainer = Rose30, onSecondaryContainer = Rose90,
    tertiary = Orange80, onTertiary = Orange20, tertiaryContainer = Orange30, onTertiaryContainer = Orange90,
    background = Color.Black, onBackground = Neutral90, surface = Color.Black, onSurface = Neutral90,
)
private val LoveLight = lightColorScheme(
    primary = Pink40, onPrimary = Color.White, primaryContainer = Pink90, onPrimaryContainer = Pink10,
    secondary = Rose40, onSecondary = Color.White, secondaryContainer = Rose90, onSecondaryContainer = Rose10,
    tertiary = Orange40, onTertiary = Color.White, tertiaryContainer = Orange90, onTertiaryContainer = Orange10,
    background = Neutral99, onBackground = Neutral10, surface = Neutral99, onSurface = Neutral10,
)

/**
 * Builds a [ColorScheme] from extracted dominant album-art colours. Same
 * mapping the Android Theme.kt uses — primary derives from the saturated
 * accent, container colours from the muted secondary, and surfaces from
 * the dark primary.
 */
private fun createColorSchemeFromDominantColors(
    colors: DominantColors,
    darkTheme: Boolean,
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
        lightColorScheme(
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
    }
}
