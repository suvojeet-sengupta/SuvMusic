package com.suvojeet.suvmusic.composeapp.ui.components.seekbar

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Available seekbar visual styles. Mirrors `app/.../data/model/SeekbarStyle.kt`
 * but defined here so commonMain code can switch between styles without a
 * dependency back into `:app`.
 */
enum class SeekbarStyle { CLASSIC, DOTS, GRADIENT_BAR, WAVE_LINE, WAVEFORM }

/**
 * Composable host for the five seekbar styles. Renders a [Canvas] sized
 * for the chosen style, runs a wave-phase animation when [isPlaying] is
 * true, and converts pointer drags into [onSeek] callbacks normalised to
 * [0f, 1f].
 *
 * NEW file (not a port — Android has individual seekbar usages scattered
 * across [WaveformSeeker], [DynamicSeekbarView], etc., none of which
 * exposes a single drop-in composable). Built here so commonMain UI
 * (NowPlayingScreen, mini-player overlays) has one consistent way to
 * render and drag any of the five styles.
 */
@Composable
fun Seekbar(
    progress: Float,
    isPlaying: Boolean,
    style: SeekbarStyle,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    height: Dp = 48.dp,
    waveAmplitudes: List<Float> = sampleAmplitudes,
) {
    val transition = rememberInfiniteTransition(label = "seekbar_phase")
    val wavePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "seekbar_phase",
    )

    var isDragging by remember { mutableStateOf(false) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val ratio = (tapOffset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onSeek(ratio)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, _ ->
                        val ratio = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeek(ratio)
                    },
                )
            },
    ) {
        when (style) {
            SeekbarStyle.CLASSIC -> with(ClassicStyle) {
                draw(progress, activeColor, inactiveColor, isDragging)
            }
            SeekbarStyle.DOTS -> with(DotsStyle) {
                draw(progress, isPlaying, wavePhase, activeColor, inactiveColor, isDragging)
            }
            SeekbarStyle.GRADIENT_BAR -> with(GradientBarStyle) {
                draw(progress, activeColor, inactiveColor, isDragging)
            }
            SeekbarStyle.WAVE_LINE -> with(WaveLineStyle) {
                draw(progress, isPlaying, wavePhase, activeColor, inactiveColor, isDragging)
            }
            SeekbarStyle.WAVEFORM -> with(WaveformStyle) {
                draw(
                    progress,
                    isPlaying,
                    wavePhase,
                    waveAmplitudes,
                    activeColor,
                    inactiveColor,
                    isDragging,
                )
            }
        }
    }
}

/**
 * Stand-in amplitude data for [SeekbarStyle.WAVEFORM] when the caller
 * doesn't have real audio analysis on hand. 100 pseudo-random values
 * generated once at class-init so the visual stays stable across recomp.
 */
private val sampleAmplitudes: List<Float> = run {
    val seed = 0xC0FFEEL
    var state = seed
    List(100) {
        state = state * 6364136223846793005L + 1442695040888963407L
        val v = ((state ushr 32) and 0xFFFF).toFloat() / 0xFFFF
        0.25f + v * 0.75f
    }
}
