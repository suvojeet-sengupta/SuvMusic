package com.suvojeet.suvmusic.ui.components.glass

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/**
 * iOS-style Liquid Glass surface.
 *
 * Layered composition:
 *   1. Adaptive shadow
 *   2. Blurred backdrop (RenderEffect on API 31+, .blur() fallback)
 *   3. Frosted base tint + grain
 *   4. Specular highlight gradient
 *   5. Luminous rim border
 *   6. Content slot
 *
 * The frosted effect refracts whatever is drawn behind it (album art, content, etc.).
 * Pass a [tint] derived from the current album dominant color to give the glass its hue.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    blurAmount: Float = 60f,
    intensity: Float = 1f,
    tint: Color = Color.Unspecified,
    isDarkTheme: Boolean,
    drawShadow: Boolean = true,
    drawRim: Boolean = true,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
    val i = intensity.coerceIn(0f, 1.5f)
    val baseAlpha = if (isDarkTheme) 0.45f * i else 0.65f * i
    val effectiveTint = if (tint == Color.Unspecified) {
        if (isDarkTheme) Color(0xFF1A1A1E).copy(alpha = baseAlpha)
        else Color.White.copy(alpha = baseAlpha)
    } else {
        tint.copy(alpha = baseAlpha)
    }

    val specularHighlight = Brush.verticalGradient(
        colors = if (isDarkTheme) listOf(
            Color.White.copy(alpha = 0.18f * i),
            Color.White.copy(alpha = 0.06f * i),
            Color.Transparent,
            Color.Black.copy(alpha = 0.08f * i)
        ) else listOf(
            Color.White.copy(alpha = 0.40f * i),
            Color.White.copy(alpha = 0.12f * i),
            Color.Transparent,
            Color.Black.copy(alpha = 0.05f * i)
        )
    )

    val borderBrush = Brush.verticalGradient(
        colors = if (isDarkTheme) listOf(
            Color.White.copy(alpha = 0.35f * i),
            Color.White.copy(alpha = 0.10f * i),
            Color.White.copy(alpha = 0.05f * i),
            Color.White.copy(alpha = 0.20f * i)
        ) else listOf(
            Color.White.copy(alpha = 0.60f * i),
            Color.White.copy(alpha = 0.25f * i),
            Color.Black.copy(alpha = 0.05f * i),
            Color.Black.copy(alpha = 0.10f * i)
        )
    )

    Box(modifier = modifier) {
        // Layer 1: Shadow
        if (drawShadow) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .shadow(
                        elevation = if (isDarkTheme) 32.dp else 20.dp,
                        shape = shape,
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = if (isDarkTheme) 0.45f else 0.15f),
                        spotColor = Color.Black.copy(alpha = if (isDarkTheme) 0.35f else 0.10f)
                    )
            )
        }

        // Layer 2: Blurred backdrop + Layer 3: frosted tint + grain
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurAmount > 0.5f) {
                        Modifier.graphicsLayer {
                            renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                blurAmount,
                                blurAmount,
                                android.graphics.Shader.TileMode.DECAL
                            ).asComposeRenderEffect()
                        }
                    } else if (blurAmount > 0.5f) {
                        Modifier.blur((blurAmount / 2).dp)
                    } else Modifier
                )
                .drawWithContent {
                    drawRect(color = effectiveTint)
                    // Subtle grain for realism
                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            alpha = if (isDarkTheme) 8 else 5
                        }
                        val rand = Random(42)
                        val count = 800
                        for (n in 0 until count) {
                            val x = rand.nextFloat() * size.width
                            val y = rand.nextFloat() * size.height
                            canvas.nativeCanvas.drawPoint(x, y, paint)
                        }
                    }
                    drawContent()
                }
        )

        // Layer 4: Specular highlight
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(specularHighlight)
        )

        // Layer 5: Luminous rim
        if (drawRim) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(width = 0.8.dp, brush = borderBrush, shape = shape)
            )
        }

        // Layer 6: Content slot
        content()
    }
}
