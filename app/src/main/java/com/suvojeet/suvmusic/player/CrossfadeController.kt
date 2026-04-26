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

    /** True while a crossfade coroutine is owning the player's volume — set during
     *  crossfadeTo's fade-in phase so external listeners can refrain from restoring volume.
     */
    @Volatile
    var isFadingIn: Boolean = false
        private set

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
        val startIndex = player.currentMediaItemIndex
        val tickMs = 30L
        val steps = (durationMs / tickMs).coerceAtLeast(1)

        fadeJob = scope.launch {
            var autoAdvanced = false
            for (step in 0..steps) {
                if (!isActive) return@launch
                // If ExoPlayer auto-advanced to the next item during the fade,
                // stop attenuating — otherwise we clip the beginning of the next track.
                if (player.currentMediaItemIndex != startIndex) {
                    autoAdvanced = true
                    break
                }
                val t = step.toFloat() / steps.toFloat()
                val gain = if (equalPower) cos(t * (PI / 2)).toFloat() else (1f - t)
                player.volume = (startVolume * gain).coerceIn(0f, 1f)
                delay(tickMs)
            }
            // Restore volume first so whichever item is now current plays at full level.
            player.volume = startVolume
            // Only advance manually if ExoPlayer hasn't already done so naturally —
            // otherwise we'd skip an extra track.
            if (!autoAdvanced && player.currentMediaItemIndex == startIndex) {
                onFadeComplete()
            }
        }
    }

    /**
     * Symmetric crossfade: fade the current track out over the first half of [durationMs],
     * advance to the next track via [onSwitch], then fade the new track in over the second
     * half. Done in a single coroutine so the transition is sequenced correctly even when
     * ExoPlayer auto-advances mid-fade.
     *
     * The single-player constraint means there's no actual audio overlap, but perceptually
     * the user hears: "song 1 fades out → song 2 fades in", which matches expectations of a
     * crossfade much better than the bare fade-out-then-cut behaviour.
     */
    fun crossfadeTo(
        player: Player,
        durationMs: Int,
        onSwitch: () -> Unit
    ) {
        cancel()
        if (durationMs <= 0) {
            onSwitch()
            return
        }

        val startVolume = player.volume
        val startIndex = player.currentMediaItemIndex
        val tickMs = 30L
        // Bias slightly toward fade-out so song 1's tail is what we lose first; the
        // incoming track gets the leaner half so it reaches full volume promptly.
        val outDurationMs = durationMs * 60 / 100
        val inDurationMs = durationMs - outDurationMs
        val outSteps = (outDurationMs / tickMs).coerceAtLeast(1)
        val inSteps = (inDurationMs / tickMs).coerceAtLeast(1)

        fadeJob = scope.launch {
            // Fade out current track
            var autoAdvanced = false
            for (step in 0..outSteps) {
                if (!isActive) return@launch
                if (player.currentMediaItemIndex != startIndex) {
                    autoAdvanced = true
                    break
                }
                val t = step.toFloat() / outSteps.toFloat()
                val gain = cos(t * (PI / 2)).toFloat()
                player.volume = (startVolume * gain).coerceIn(0f, 1f)
                delay(tickMs)
            }

            // Switch to next track (unless ExoPlayer already did)
            if (!autoAdvanced && player.currentMediaItemIndex == startIndex) {
                player.volume = 0f
                onSwitch()
            }

            // Fade in new track from 0 → startVolume.
            // ExoPlayer's transition listener may try to restore volume to 1.0; the
            // isFadingIn flag tells the listener to leave the volume alone while we ramp.
            isFadingIn = true
            try {
                player.volume = 0f
                for (step in 0..inSteps) {
                    if (!isActive) return@launch
                    val t = step.toFloat() / inSteps.toFloat()
                    val gain = sin(t * (PI / 2)).toFloat()
                    player.volume = (startVolume * gain).coerceIn(0f, 1f)
                    delay(tickMs)
                }
                player.volume = startVolume
            } finally {
                isFadingIn = false
            }
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
