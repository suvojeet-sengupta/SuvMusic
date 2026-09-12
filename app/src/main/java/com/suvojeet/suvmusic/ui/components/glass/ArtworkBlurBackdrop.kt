package com.suvojeet.suvmusic.ui.components.glass

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
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
 * The now-playing artwork that glass surfaces frost themselves against.
 *
 * Provided by the player screen and read by any sheet opened from it, so those sheets pick
 * up the player's own backdrop without every call site having to pass artwork down. Null
 * outside the player — surfaces then fall back to a plain container.
 */
val LocalGlassArtwork = compositionLocalOf<GlassArtwork?> { null }

@Immutable
data class GlassArtwork(
    val artworkUrl: String?,
    val colors: DominantColors,
    val isDarkTheme: Boolean
)

/**
 * Heavily blurred album art with a legibility scrim and a dominant-colour wash — the
 * frosted-glass look of the player screen, as a standalone layer.
 *
 * Shared by the player backdrop and by every sheet that sits over it so the two can't
 * drift; the blur is hardware [android.graphics.RenderEffect] on API 31+ and falls back to
 * [blur] below that.
 */
@Composable
fun ArtworkBlurBackdrop(
    artworkUrl: String?,
    isDarkTheme: Boolean,
    dominantColors: DominantColors,
    modifier: Modifier = Modifier,
    blurRadius: Float = MAX_BLUR_RADIUS,
    intensity: Float = 1f,
    scrimAlpha: Float = if (isDarkTheme) 0.55f else 0.40f
) {
    val i = intensity.coerceIn(0.3f, 1.5f)

    // Cache the RenderEffect — building one is expensive and this recomposes with
    // playback state. The radius is used as-is (the slider is labelled in px, and the
    // old 1.4x-then-cap-at-80 made everything above ~57 px produce identical output),
    // and the pre-S fallback uses the same number so both paths agree.
    val radius = remember(blurRadius) { blurRadius.coerceIn(0f, MAX_BLUR_RADIUS) }
    val fallbackBlurDp = radius
    val blurEffect = remember(radius) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            android.graphics.RenderEffect.createBlurEffect(
                radius,
                radius,
                android.graphics.Shader.TileMode.CLAMP
            ).asComposeRenderEffect()
        } else {
            null
        }
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (blurEffect != null) {
                        Modifier.graphicsLayer { renderEffect = blurEffect }
                    } else {
                        Modifier.blur(fallbackBlurDp.dp)
                    }
                )
        ) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Scrim, so text stays readable over whatever the artwork happens to be.
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

        // Dominant-colour wash, which is what ties the surface to the current track.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            (if (isDarkTheme) dominantColors.primary else dominantColors.primary.copy(alpha = 0.5f))
                                .copy(alpha = 0.18f * i),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

/** Beyond this a Gaussian blur costs more without looking different. */
const val MAX_BLUR_RADIUS = 80f
