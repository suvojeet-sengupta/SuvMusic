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
import com.suvojeet.suvmusic.ui.theme.YtFlatBackground

/**
 * Flat, YouTube-Music-style player backdrop.
 *
 * Three fully static layers (no infinite/animated state — strictly cheaper than the
 * [com.suvojeet.suvmusic.ui.components.MeshGradientBackground] it replaces for the YT_MUSIC
 * style, which ran eight infinite float animations). It recomposes only when the song,
 * theme, or video-mode flag changes:
 *
 *  1. A solid base fill ([YtFlatBackground] in dark, white in light).
 *  2. A *subtle* blurred album-art wash (skipped in video mode / when no art is available).
 *  3. A single-color vertical scrim so the top is faintly washed and the bottom is fully
 *     flat — this is a scrim of one base color, not a decorative multi-color gradient.
 *
 * The blur is SDK-gated exactly like
 * [com.suvojeet.suvmusic.ui.screens.player.styles.LiquidGlassPlayerStyle]: hardware
 * [android.graphics.RenderEffect] on API 31+, falling back to [blur] below it (minSdk 26).
 */
@Composable
fun FlatArtTintBackground(
    thumbnailUrl: String?,
    isDarkTheme: Boolean,
    isVideoMode: Boolean,
    modifier: Modifier = Modifier
) {
    val base = if (isDarkTheme) YtFlatBackground else Color.White
    val artAlpha = if (isDarkTheme) 0.18f else 0.10f

    Box(modifier = modifier.fillMaxSize().background(base)) {
        // Layer 2 — subtle blurred album-art wash (audio-only).
        if (!thumbnailUrl.isNullOrBlank() && !isVideoMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.graphicsLayer {
                                renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                    80f,
                                    80f,
                                    android.graphics.Shader.TileMode.CLAMP
                                ).asComposeRenderEffect()
                            }
                        } else {
                            Modifier.blur(40.dp)
                        }
                    )
            ) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().graphicsLayer { alpha = artAlpha },
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Layer 3 — single-color scrim: faint at the top, solid at the bottom.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            base.copy(alpha = 0.55f),
                            base.copy(alpha = 0.85f),
                            base
                        )
                    )
                )
        )
    }
}
