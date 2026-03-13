package com.suvojeet.suvmusic.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Using system fonts (can be replaced with custom fonts later)
// To use custom fonts: download Outfit and Inter from Google Fonts
// and place them in res/font/

// Typography with expressive, music-focused styles
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val Typography = Typography(
    // ── Standard styles (keep existing values) ──
    displayLarge  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold,  fontSize = 57.sp, lineHeight = 64.sp,  letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,       fontSize = 45.sp, lineHeight = 52.sp,  letterSpacing = 0.sp),
    displaySmall  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,       fontSize = 36.sp, lineHeight = 44.sp,  letterSpacing = 0.sp),
    headlineLarge  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,  fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,  fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,  fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,       fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,     fontSize = 16.sp, lineHeight = 24.sp,  letterSpacing = 0.15.sp),
    titleSmall  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,       fontSize = 14.sp, lineHeight = 20.sp,  letterSpacing = 0.1.sp),
    bodyLarge   = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,       fontSize = 16.sp, lineHeight = 24.sp,  letterSpacing = 0.5.sp),
    bodyMedium  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,       fontSize = 14.sp, lineHeight = 20.sp,  letterSpacing = 0.25.sp),
    bodySmall   = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal,       fontSize = 12.sp, lineHeight = 16.sp,  letterSpacing = 0.4.sp),
    labelLarge  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,       fontSize = 14.sp, lineHeight = 20.sp,  letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,       fontSize = 12.sp, lineHeight = 16.sp,  letterSpacing = 0.5.sp),
    labelSmall  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,       fontSize = 11.sp, lineHeight = 16.sp,  letterSpacing = 0.5.sp),

    // ── M3E Emphasized variants (heavier weight, slightly larger) ──
    displayLargeEmphasized  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black,    fontSize = 57.sp, lineHeight = 64.sp,  letterSpacing = (-0.25).sp),
    displayMediumEmphasized = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmallEmphasized  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLargeEmphasized  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,     fontSize = 32.sp, lineHeight = 40.sp),
    headlineMediumEmphasized = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,     fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmallEmphasized  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,     fontSize = 24.sp, lineHeight = 32.sp),
    titleLargeEmphasized  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,        fontSize = 22.sp, lineHeight = 28.sp),
    titleMediumEmphasized = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmallEmphasized  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,    fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLargeEmphasized   = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,      fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMediumEmphasized  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,      fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmallEmphasized   = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,      fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLargeEmphasized  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,    fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMediumEmphasized = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,    fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmallEmphasized  = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,    fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)