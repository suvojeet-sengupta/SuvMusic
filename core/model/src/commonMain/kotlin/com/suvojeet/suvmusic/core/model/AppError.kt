package com.suvojeet.suvmusic.core.model

/**
 * A typed failure value.
 *
 * The app historically swallowed errors by returning `emptyList()` / `null`, which
 * threw away *why* something failed — the UI could not tell "no results" apart from
 * "the network is down" or "your session expired". [AppError] makes the failure a
 * first-class value so callers (and the UI) can react meaningfully.
 *
 * This type is platform-agnostic (lives in `core:model`, no Android/JVM deps). The
 * mapping from platform exceptions (IOException, HttpException, …) to an [AppError]
 * is done at the edge — see the Android `Throwable.toAppError()` mapper in `:app`.
 */
sealed interface AppError {
    /** Optional developer-facing detail; never assume it is safe to show verbatim to users. */
    val detail: String?

    /** No connectivity / host unreachable. */
    data class NoNetwork(override val detail: String? = null) : AppError

    /** A request was made but timed out. */
    data class Timeout(override val detail: String? = null) : AppError

    /** Auth/cookie expired or rejected — the user likely needs to sign in again. */
    data class AuthExpired(override val detail: String? = null) : AppError

    /** Upstream is throttling us (HTTP 429 or provider-specific signal). */
    data class RateLimited(override val detail: String? = null) : AppError

    /** A non-success HTTP status that isn't one of the more specific cases above. */
    data class Http(val code: Int, override val detail: String? = null) : AppError

    /** The response arrived but couldn't be parsed — often means the upstream schema shifted. */
    data class Parse(override val detail: String? = null) : AppError

    /** The upstream answered but signalled its own failure (e.g. `success: false`). */
    data class Upstream(override val detail: String? = null) : AppError

    /** Anything we couldn't classify. */
    data class Unknown(override val detail: String? = null) : AppError

    /** A short, stable key for telemetry/aggregation (e.g. "NoNetwork", "Http"). */
    val kind: String
        get() = when (this) {
            is NoNetwork -> "NoNetwork"
            is Timeout -> "Timeout"
            is AuthExpired -> "AuthExpired"
            is RateLimited -> "RateLimited"
            is Http -> "Http"
            is Parse -> "Parse"
            is Upstream -> "Upstream"
            is Unknown -> "Unknown"
        }
}
