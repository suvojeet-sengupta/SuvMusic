package com.suvojeet.suvmusic.composeapp.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * SuvMusic theme for Compose Multiplatform — mirrors the Android app's
 * "Deep Electric Purple / Neon Cyan / Hot Magenta" palette as defined
 * in app/.../ui/theme/Color.kt. Kept in commonMain so both Desktop and
 * future commonMain Android UI render identically.
 *
 * Phase 5.1 first slice — only the colour scheme. Typography, shapes,
 * and motion tokens come over in 5.2 as the actual screens migrate.
 */
private val Purple10 = Color(0xFF1D0035)
private val Purple20 = Color(0xFF2D0050)
private val Purple30 = Color(0xFF42007A)
private val Purple40 = Color(0xFF5A189A)
private val Purple70 = Color(0xFFB970F2)
private val Purple80 = Color(0xFFD194FF)
private val Purple90 = Color(0xFFE8BFFF)
private val Purple95 = Color(0xFFF6E5FF)

private val Cyan20 = Color(0xFF003640)
private val Cyan30 = Color(0xFF004D5C)
private val Cyan40 = Color(0xFF00687A)
private val Cyan70 = Color(0xFF00BDD6)
private val Cyan80 = Color(0xFF4DD9F0)
private val Cyan90 = Color(0xFFA5EFFF)

private val Magenta20 = Color(0xFF4A0039)
private val Magenta30 = Color(0xFF6A0050)
private val Magenta40 = Color(0xFF8C006A)
private val Magenta70 = Color(0xFFE65BB7)
private val Magenta80 = Color(0xFFFF8AD0)
private val Magenta90 = Color(0xFFFFD8EC)

private val SurfaceDark = Color(0xFF14101A)
private val SurfaceLight = Color(0xFFFDF8FF)

private val DarkColors = darkColorScheme(
    primary = Purple70,
    onPrimary = Purple10,
    primaryContainer = Purple30,
    onPrimaryContainer = Purple90,
    secondary = Cyan70,
    onSecondary = Cyan20,
    secondaryContainer = Cyan30,
    onSecondaryContainer = Cyan90,
    tertiary = Magenta70,
    onTertiary = Magenta20,
    tertiaryContainer = Magenta30,
    onTertiaryContainer = Magenta90,
    background = SurfaceDark,
    onBackground = Purple95,
    surface = SurfaceDark,
    onSurface = Purple95,
)

private val LightColors = lightColorScheme(
    primary = Purple40,
    onPrimary = Color.White,
    primaryContainer = Purple90,
    onPrimaryContainer = Purple10,
    secondary = Cyan40,
    onSecondary = Color.White,
    secondaryContainer = Cyan90,
    onSecondaryContainer = Cyan20,
    tertiary = Magenta40,
    onTertiary = Color.White,
    tertiaryContainer = Magenta90,
    onTertiaryContainer = Magenta20,
    background = SurfaceLight,
    onBackground = Purple10,
    surface = SurfaceLight,
    onSurface = Purple10,
)

@Composable
fun SuvMusicTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (useDarkTheme) DarkColors else LightColors,
        content = content,
    )
}
