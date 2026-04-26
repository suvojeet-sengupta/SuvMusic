package com.suvojeet.suvmusic.composeapp.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.suvojeet.suvmusic.composeapp.generated.resources.Res
import com.suvojeet.suvmusic.composeapp.generated.resources.outfit
import org.jetbrains.compose.resources.Font

/**
 * Typography — Material 3 Expressive scale powered by Outfit (variable font).
 * Verbatim port of `app/.../ui/theme/Type.kt`, but reading the font through
 * Compose Multiplatform Resources so Android and Desktop both pull from a
 * single `composeResources/font/outfit.ttf` asset.
 *
 * The variable font carries weights 400–800 in one file. We bind logical
 * weights via [FontWeight] in each [Font] declaration; the system rasteriser
 * picks the correct axis position when it loads the glyphs.
 */
@Composable
private fun outfitFamily(): FontFamily = FontFamily(
    Font(Res.font.outfit, weight = FontWeight.Normal),
    Font(Res.font.outfit, weight = FontWeight.Medium),
    Font(Res.font.outfit, weight = FontWeight.SemiBold),
    Font(Res.font.outfit, weight = FontWeight.Bold),
    Font(Res.font.outfit, weight = FontWeight.ExtraBold),
)

/**
 * Build the SuvMusic [Typography] inside a Composable scope (needed because
 * [outfitFamily] reads from CMP Resources). Called once by [SuvMusicTheme].
 */
@Composable
fun appTypography(): Typography {
    val outfit = outfitFamily()
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
