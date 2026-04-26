package com.suvojeet.suvmusic.composeapp.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography — Material 3 Expressive scale powered by the platform's
 * Outfit font family. Values mirror `app/.../ui/theme/Type.kt`.
 *
 * Font loading is delegated to [outfitFontFamily], an expect/actual hook
 * so each platform can decide where the font comes from. The Android
 * actual will pull Outfit from `:app`'s R.font.outfit resource (or, in a
 * follow-up, from `composeApp/src/androidMain/res/font/outfit.ttf` if we
 * mirror the asset). The Desktop actual currently returns
 * [FontFamily.Default] — Skiko's default sans serif. Outfit on Desktop
 * lands when we wire CMP Resources properly (see notes in [Type] below).
 *
 * Earlier iteration tried CMP's `Res.font.outfit` accessor for a single
 * shared asset, but the generated `Res` class wasn't reachable from
 * commonMain in CMP 1.10 with our `androidLibrary {}` namespace setup.
 * Kept the asset at `composeApp/src/commonMain/composeResources/font/
 * outfit.ttf` — it'll snap into place once the resource hook works.
 */
@Composable
fun appTypography(): Typography {
    val outfit = outfitFontFamily()
    return Typography(
        displayLarge = TextStyle(
            fontFamily = outfit,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.25).sp,
        ),
        displayMedium = TextStyle(
            fontFamily = outfit,
            fontWeight = FontWeight.Bold,
            fontSize = 45.sp,
            lineHeight = 52.sp,
            letterSpacing = 0.sp,
        ),
        displaySmall = TextStyle(
            fontFamily = outfit,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = 0.sp,
        ),
        headlineLarge = TextStyle(
            fontFamily = outfit,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = outfit,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp,
        ),
        headlineSmall = TextStyle(
            fontFamily = outfit,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp,
        ),
        titleLarge = TextStyle(
            fontFamily = outfit,
            fontWeight = FontWeight.Medium,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = outfit,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp,
        ),
        titleSmall = TextStyle(
            fontFamily = outfit,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = outfit,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = outfit,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = outfit,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp,
        ),
        labelLarge = TextStyle(
            fontFamily = outfit,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = outfit,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = outfit,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
        ),
    )
}

/**
 * Platform-specific Outfit font family. Swap to a real Outfit-loading
 * implementation per platform once the resource path is finalised.
 */
@Composable
expect fun outfitFontFamily(): FontFamily
