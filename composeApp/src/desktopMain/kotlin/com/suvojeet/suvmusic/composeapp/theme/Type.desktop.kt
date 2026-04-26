package com.suvojeet.suvmusic.composeapp.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

/**
 * Desktop implementation — Skiko's default sans-serif. Outfit will be
 * wired in when CMP Resources is reachable from this module
 * (composeApp/src/commonMain/composeResources/font/outfit.ttf is on
 * disk and waiting for the resolver).
 */
@Composable
actual fun outfitFontFamily(): FontFamily = FontFamily.Default
