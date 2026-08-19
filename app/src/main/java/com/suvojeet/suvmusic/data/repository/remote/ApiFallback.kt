package com.suvojeet.suvmusic.data.repository.remote

import android.util.Log

private fun isPrimaryHealthy(result: Any?): Boolean = when (result) {
    is RemoteAudioSearchResponse -> result.success != false
    is RemoteAudioSongDetailsResponse -> result.success != false
    is ApiResponse<*> -> result.success
    else -> true
}

private fun isRetryable(e: Exception): Boolean = when (e) {
    is retrofit2.HttpException -> e.code() >= 500 || e.code() == 429
    else -> true
}

private fun httpCode(e: Exception): Int? = (e as? retrofit2.HttpException)?.code()

/**
 * The public fallback host rate-limits aggressively (often every request). Once it
 * answers 429, stop burning a doomed request per call for a while — the primary's
 * own error is surfaced instead.
 */
private const val FALLBACK_429_COOLDOWN_MS = 5 * 60_000L

@Volatile
private var fallbackThrottledUntilMs = 0L

suspend fun <S, T> withApiFallback(
    primary: S,
    fallback: S,
    tag: String,
    block: suspend S.() -> T
): T {
    return try {
        val result = primary.block()
        if (isPrimaryHealthy(result)) {
            RemoteAudioApiStatus.setPrimaryApiWorking(true)
        }
        result
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        if (!isRetryable(e)) {
            Log.w(tag, "Primary returned client error: ${e.message}")
            throw e
        }
        RemoteAudioApiStatus.setPrimaryApiWorking(false)
        if (System.currentTimeMillis() < fallbackThrottledUntilMs) {
            Log.w(tag, "Primary failed (${e.message}); fallback in 429 cooldown — surfacing primary error")
            com.suvojeet.suvmusic.util.HqDiagnostics.log("fallback", "$tag: primary failed (${e.javaClass.simpleName}: ${e.message}); fallback host in 429 cooldown — surfacing primary error")
            throw e
        }
        Log.e(tag, "Primary failed, falling back: ${e.message}")
        com.suvojeet.suvmusic.util.HqDiagnostics.log("fallback", "$tag: primary failed (${e.javaClass.simpleName}: ${e.message}) — trying fallback host")
        try {
            fallback.block()
        } catch (fallbackError: Exception) {
            if (fallbackError is kotlinx.coroutines.CancellationException) throw fallbackError
            if (httpCode(fallbackError) == 429) {
                fallbackThrottledUntilMs = System.currentTimeMillis() + FALLBACK_429_COOLDOWN_MS
                Log.w(tag, "Fallback host rate-limited — cooling it down for ${FALLBACK_429_COOLDOWN_MS / 60_000} min")
                com.suvojeet.suvmusic.util.HqDiagnostics.log("fallback", "$tag: fallback host answered 429 — cooling it for ${FALLBACK_429_COOLDOWN_MS / 60_000} min (NOT arming the HQ gate)")
            }
            Log.e(tag, "Fallback also failed (${fallbackError.message}) — surfacing primary error")
            com.suvojeet.suvmusic.util.HqDiagnostics.log("fallback", "$tag: fallback failed too (${fallbackError.javaClass.simpleName}: ${fallbackError.message}) — surfacing PRIMARY error")
            // Surface the PRIMARY's error, not the fallback's. The fallback host 429s
            // near-constantly; letting that 429 propagate made callers arm the shared
            // RemoteAudio backoff and freeze the perfectly healthy primary backend
            // ("HQ source busy" on every song).
            throw e
        }
    }
}
