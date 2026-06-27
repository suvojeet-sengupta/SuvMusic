package com.suvojeet.suvmusic.ui.screens.player.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.suvojeet.suvmusic.ui.components.DominantColors

/**
 * Transparent, iOS-style "liquid glass" player backdrop.
 *
 * This is the signature look that used to live behind the (now removed) Liquid Glass
 * player style — extracted here so the default YT Music style can use it. Unlike
 * [FlatArtTintBackground] (a mostly-solid surface with a barely-visible art wash), this
 * shows the blurred album art prominently through a translucent scrim, so the whole
 * screen feels like frosted glass over the artwork.
 *
 * Layers:
 *   1. Heavily blurred album artwork filling the screen (the glass backdrop).
 *   2. An adaptive dark/light scrim gradient for text legibility.
 *   3. A subtle dominant-color radial wash.
 *
 * The blur is SDK-gated exactly like the old Liquid Glass style: hardware
 * [android.graphics.RenderEffect] on API 31+, falling back to [blur] below it.
 */
@Composable
fun GlassArtBackground(
    thumbnailUrl: String?,
    isDarkTheme: Boolean,
    isVideoMode: Boolean,
    dominantColors: DominantColors,
    modifier: Modifier = Modifier,
    blurRadius: Float = 60f,
    intensity: Float = 1f
) {
    val scrimAlpha = if (isDarkTheme) 0.55f else 0.40f
    val i = intensity.coerceIn(0.3f, 1.5f)

    Box(modifier = modifier.fillMaxSize()) {
        if (!thumbnailUrl.isNullOrBlank() && !isVideoMode) {
            // Layer 1 — blurred album artwork as the entire backdrop.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.graphicsLayer {
                                renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                    blurRadius * 1.8f,
                                    blurRadius * 1.8f,
                                    android.graphics.Shader.TileMode.CLAMP
                                ).asComposeRenderEffect()
                            }
                        } else {
                            Modifier.blur((blurRadius * 0.9f).dp)
                        }
                    )
            ) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Layer 2 — adaptive scrim for legibility.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = if (isDarkTheme) {
                                listOf(
                                    Color.Black.copy(alpha = scrimAlpha * i * 0.7f),
                                    Color.Black.copy(alpha = scrimAlpha * i),
                                    Color.Black.copy(alpha = scrimAlpha * i * 1.2f)
                                )
                            } else {
                                listOf(
                                    Color.White.copy(alpha = scrimAlpha * i * 0.5f),
                                    Color.White.copy(alpha = scrimAlpha * i * 0.8f),
                                    Color.White.copy(alpha = scrimAlpha * i * 1.1f)
                                )
                            }
                        )
                    )
            )

            // Layer 3 — dominant color radial wash.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                (if (isDarkTheme) dominantColors.primary else dominantColors.primary.copy(alpha = 0.5f)).copy(alpha = 0.18f * i),
                                Color.Transparent
                            )
                        )
                    )
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
