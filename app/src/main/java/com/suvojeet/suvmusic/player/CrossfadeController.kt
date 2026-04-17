package com.suvojeet.suvmusic.player

import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.sqrt

/**
 * Crossfade controller for smooth transitions between tracks.
 *
 * Strategy: volume-ramp the primary [Player] down to zero and, at the same time,
 * instruct the provided [onStartNext] hook to bring the next track up to full volume.
 * When [durationMs] is 0 the controller is a no-op (pure gapless fallback).
 *
 * This is a single-player volume-fade implementation that layers cleanly on top of
 * ExoPlayer's existing gapless preloading. A more elaborate dual-player crossfade
 * (secondary ExoPlayer instance overlapping the primary) can be added later if
 * needed — the public API stays the same.
 */
class CrossfadeController(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    private var fadeJob: Job? = null

    /**
     * Kick off a fade-out on [player] over [durationMs].
     * The caller is responsible for advancing the queue / starting the next item
     * after the fade completes — done inside [onFadeComplete].
     *
     * @param equalPower when true, uses an equal-power (constant-energy) curve so the
     *                   perceptual loudness stays flatter through the crossover. When false,
     *                   uses a plain linear ramp.
     */
    fun fadeOut(
        player: Player,
        durationMs: Int,
        equalPower: Boolean = true,
        onFadeComplete: () -> Unit
    ) {
        cancel()
        if (durationMs <= 0) {
            onFadeComplete()
            return
        }

        val startVolume = player.volume
        val tickMs = 30L
        val steps = (durationMs / tickMs).coerceAtLeast(1)

        fadeJob = scope.launch {
            for (step in 0..steps) {
                if (!isActive) return@launch
                val t = step.toFloat() / steps.toFloat()
                val gain = if (equalPower) cos(t * (PI / 2)).toFloat() else (1f - t)
                player.volume = (startVolume * gain).coerceIn(0f, 1f)
                delay(tickMs)
            }
            player.volume = 0f
            onFadeComplete()
            // Restore volume so the next track starts at full level.
            player.volume = startVolume
        }
    }

    /**
     * Fade the incoming track [player] from 0 → target volume over [durationMs].
     */
    fun fadeIn(
        player: Player,
        durationMs: Int,
        targetVolume: Float = 1f,
        equalPower: Boolean = true
    ) {
        if (durationMs <= 0) {
            player.volume = targetVolume
            return
        }
        scope.launch {
            player.volume = 0f
            val tickMs = 30L
            val steps = (durationMs / tickMs).coerceAtLeast(1)
            for (step in 0..steps) {
                if (!isActive) return@launch
                val t = step.toFloat() / steps.toFloat()
                val gain = if (equalPower) sin(t * (PI / 2)).toFloat() else t
                player.volume = (targetVolume * gain).coerceIn(0f, 1f)
                delay(tickMs)
            }
            player.volume = targetVolume
        }
    }

    fun cancel() {
        fadeJob?.cancel()
        fadeJob = null
    }
}
