package com.suvojeet.suvmusic.providers.lyrics

import java.util.concurrent.ConcurrentHashMap

/**
 * Per-provider circuit breaker for the lyrics pipeline.
 *
 * A provider that fails [failureThreshold] times in a row (network error, HTTP
 * failure, parser blow-up — NOT "no lyrics found", which is a valid answer) is
 * skipped for [openDurationMs] instead of being retried on every song. This keeps
 * one dead provider from adding its full timeout to every lyrics lookup while the
 * remaining providers are healthy. One success closes the circuit again.
 */
class ProviderCircuitBreaker(
    private val failureThreshold: Int = 3,
    private val openDurationMs: Long = 10 * 60_000L,
) {
    private class State {
        @Volatile var consecutiveFailures: Int = 0
        @Volatile var openedAt: Long = 0L
    }

    private val states = ConcurrentHashMap<String, State>()

    /** True while the provider's circuit is open (skip it, don't call it). */
    fun isOpen(provider: String, now: Long = System.currentTimeMillis()): Boolean {
        val state = states[provider] ?: return false
        if (state.openedAt == 0L) return false
        if (now - state.openedAt >= openDurationMs) {
            // Half-open: allow one probe call through; a failure re-opens immediately.
            state.openedAt = 0L
            state.consecutiveFailures = failureThreshold - 1
            return false
        }
        return true
    }

    fun recordSuccess(provider: String) {
        states[provider]?.let {
            it.consecutiveFailures = 0
            it.openedAt = 0L
        }
    }

    fun recordFailure(provider: String, now: Long = System.currentTimeMillis()) {
        val state = states.getOrPut(provider) { State() }
        state.consecutiveFailures += 1
        if (state.consecutiveFailures >= failureThreshold && state.openedAt == 0L) {
            state.openedAt = now
            android.util.Log.w(
                "ProviderCircuitBreaker",
                "'$provider' failed ${state.consecutiveFailures}x in a row — skipping it for ${openDurationMs / 60_000} min",
            )
        }
    }
}
