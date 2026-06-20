package com.suvojeet.suvmusic.data.error

import com.google.gson.JsonParseException
import com.suvojeet.suvmusic.core.model.AppError
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.CancellationException
import androidx.media3.common.PlaybackException

/**
 * Maps a platform [Throwable] to a typed [AppError].
 *
 * This is the single place that knows about JVM/OkHttp/Retrofit/Gson exception types,
 * keeping `core:model` platform-agnostic. Repositories call this at their catch sites
 * instead of collapsing everything to `emptyList()`/`null`.
 */
fun Throwable.toAppError(): AppError = when (this) {
    is CancellationException -> throw this // never swallow coroutine cancellation
    is UnknownHostException, is ConnectException -> AppError.NoNetwork(message)
    is SocketTimeoutException -> AppError.Timeout(message)
    is JsonParseException -> AppError.Parse(message)
    else -> {
        // Retrofit's HttpException is referenced by name so we don't hard-depend on it
        // being on the classpath of every caller. It carries an HTTP status code.
        val cn = this::class.qualifiedName
        when {
            cn == "retrofit2.HttpException" -> {
                val code = runCatching {
                    this::class.java.getMethod("code").invoke(this) as Int
                }.getOrNull() ?: -1
                code.toHttpError(message)
            }
            this is IOException -> AppError.NoNetwork(message)
            else -> AppError.Unknown(message ?: this::class.simpleName)
        }
    }
}

/** Classify a raw HTTP status code into the most specific [AppError]. */
fun Int.toHttpError(detail: String? = null): AppError = when (this) {
    401, 403 -> AppError.AuthExpired(detail ?: "HTTP $this")
    429 -> AppError.RateLimited(detail ?: "HTTP $this")
    in 500..599 -> AppError.Upstream(detail ?: "HTTP $this")
    else -> AppError.Http(this, detail)
}

/** Get a user-friendly error message for the given [AppError]. */
fun AppError.toUserFriendlyMessage(): String = when (this) {
    is AppError.NoNetwork -> "No network connection. Please check your internet connection."
    is AppError.Timeout -> "Connection timed out. Your internet connection might be slow."
    is AppError.RateLimited -> "Too many requests. Please wait a moment."
    is AppError.AuthExpired -> "Session expired. Please log in again."
    is AppError.Upstream -> "The music server is experiencing issues. Please try again later."
    is AppError.Parse -> "Could not read music data. (Format shift)"
    is AppError.Http -> "Server returned error (Code: $code)."
    is AppError.Unknown -> "Playback error: ${detail ?: "Unknown issue"}"
}

/** Get a user-friendly error message for the given [Throwable]. */
fun Throwable.toUserFriendlyMessage(): String {
    if (this is PlaybackException) {
        val isAudioSinkError = this.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED ||
                             this.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED
        val isDecoderError = this.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
                           this.errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ||
                           this.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                           this.errorCode == PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED
        when {
            isAudioSinkError -> return "Audio output error. Please check your audio output device."
            isDecoderError -> return "Audio format not supported or decoding failed."
        }
    }

    val cause = this.cause
    if (cause != null && cause != this) {
        return cause.toUserFriendlyMessage()
    }

    return this.toAppError().toUserFriendlyMessage()
}
