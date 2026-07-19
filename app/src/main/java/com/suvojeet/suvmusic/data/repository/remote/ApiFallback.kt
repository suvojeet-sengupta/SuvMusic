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
    } catch (e: Exception) {
        if (!isRetryable(e)) {
            Log.w(tag, "Primary returned client error: ${e.message}")
            throw e
        }
        Log.e(tag, "Primary failed, falling back: ${e.message}")
        RemoteAudioApiStatus.setPrimaryApiWorking(false)
        try {
            fallback.block()
        } catch (fallbackError: Exception) {
            Log.e(tag, "Fallback also failed: ${fallbackError.message}")
            throw fallbackError
        }
    }
}
