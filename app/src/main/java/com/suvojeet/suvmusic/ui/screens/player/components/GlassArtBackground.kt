package com.suvojeet.suvmusic.ui.screens.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.suvojeet.suvmusic.ui.components.DominantColors
import com.suvojeet.suvmusic.ui.components.glass.ArtworkBlurBackdrop

/**
 * Transparent, iOS-style "liquid glass" player backdrop.
 *
 * Unlike [FlatArtTintBackground] (a mostly-solid surface with a barely-visible art wash),
 * this shows the blurred album art prominently through a translucent scrim, so the whole
 * screen feels like frosted glass over the artwork.
 *
 * The layers themselves live in [ArtworkBlurBackdrop], which the player's sheets render too
 * — that shared piece is what keeps a sheet's frosting identical to the screen behind it.
 */
@Composable
fun GlassArtBackground(
    thumbnailUrl: String?,
    isDarkTheme: Boolean,
    isVideoMode: Boolean,
    dominantColors: DominantColors,
    modifier: Modifier = Modifier,
    blurRadius: Float = 80f,
    intensity: Float = 1f
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (!thumbnailUrl.isNullOrBlank() && !isVideoMode) {
            ArtworkBlurBackdrop(
                artworkUrl = thumbnailUrl,
                isDarkTheme = isDarkTheme,
                dominantColors = dominantColors,
                modifier = Modifier.fillMaxSize(),
                blurRadius = blurRadius,
                intensity = intensity
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isDarkTheme) Color(0xFF0B0B0F) else Color(0xFFF2F2F6))
            )
        }
    }
}
