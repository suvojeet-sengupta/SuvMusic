package com.suvojeet.suvmusic.shareplay

import kotlin.math.abs

internal object ListenTogetherSync {
    const val HARD_SEEK_DRIFT_MS = 1_000L
    const val MIN_HARD_SEEK_GAP_MS = 3_000L
    const val MAX_SEEK_LEAD_MS = 2_000L
    const val ANCHOR_JITTER_MS = 80L
    const val DEFAULT_LATENCY_MS = 50L
    const val MAX_LATENCY_MS = 1_000L

    private const val START_CORRECTING_MS = 80L
    private const val STOP_CORRECTING_MS = 25L
    private const val LARGE_DRIFT_MS = 300L
    private const val GENTLE_RATE = 0.03f
    private const val FAST_RATE = 0.08f

    /** Corrects from 80 ms but only settles under 25 ms, so the speed doesn't oscillate. */
    fun targetSpeed(driftMs: Long, currentSpeed: Float): Float {
        val magnitude = abs(driftMs)
        val correcting = currentSpeed != 1f
        if (magnitude <= STOP_CORRECTING_MS) return 1f
        if (magnitude < START_CORRECTING_MS && !correcting) return 1f
        val rate = if (magnitude >= LARGE_DRIFT_MS) FAST_RATE else GENTLE_RATE
        return if (driftMs < 0) 1f + rate else 1f - rate
    }

    fun smoothRtt(previousMs: Long?, sampleMs: Long): Long =
        if (previousMs == null) sampleMs else (previousMs * 3 + sampleMs) / 4

    /** A sync message travels host to server to guest, roughly one guest round trip. */
    fun latencyCompensation(smoothedRttMs: Long?): Long =
        smoothedRttMs?.coerceIn(0L, MAX_LATENCY_MS) ?: DEFAULT_LATENCY_MS
}
