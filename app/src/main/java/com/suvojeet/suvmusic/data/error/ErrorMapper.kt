package com.suvojeet.suvmusic.data.error

import com.google.gson.JsonParseException
import com.suvojeet.suvmusic.core.model.AppError
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.CancellationException

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
