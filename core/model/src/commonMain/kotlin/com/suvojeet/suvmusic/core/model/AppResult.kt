package com.suvojeet.suvmusic.core.model

/**
 * Success-or-typed-failure wrapper.
 *
 * Prefer this over `List<T>?` / nullable returns for operations that hit the network
 * or parse untrusted data. [Success] with an empty list means "the call worked and
 * there genuinely were no results"; [Failure] means "the call did not work, and here
 * is why" — two states the old `emptyList()` convention could not distinguish.
 */
sealed interface AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

/** The value on success, or `null` on failure. Use to bridge to legacy nullable APIs. */
fun <T> AppResult<T>.getOrNull(): T? = when (this) {
    is AppResult.Success -> data
    is AppResult.Failure -> null
}

/** The value on success, or [fallback] on failure. */
fun <T> AppResult<T>.getOrDefault(fallback: T): T = when (this) {
    is AppResult.Success -> data
    is AppResult.Failure -> fallback
}

/** The [AppError] on failure, or `null` on success. */
fun <T> AppResult<T>.errorOrNull(): AppError? = when (this) {
    is AppResult.Success -> null
    is AppResult.Failure -> error
}

val AppResult<*>.isSuccess: Boolean get() = this is AppResult.Success

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <T> AppResult<T>.onFailure(action: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) action(error)
    return this
}

fun <T> T.asSuccess(): AppResult<T> = AppResult.Success(this)
fun AppError.asFailure(): AppResult<Nothing> = AppResult.Failure(this)
