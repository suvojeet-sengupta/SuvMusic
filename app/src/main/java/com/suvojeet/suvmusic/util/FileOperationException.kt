package com.suvojeet.suvmusic.util

import android.app.PendingIntent

/**
 * Custom exception to handle file operation failures.
 */
sealed class FileOperationException(message: String) : Exception(message) {
    /**
     * Exception thrown when Scoped Storage (Android 10+) requires user permission to delete/edit a file.
     * Contains the [PendingIntent] required to prompt the user.
     */
    class FilePermissionException(
        message: String,
        val pendingIntent: PendingIntent
    ) : FileOperationException(message)

    /**
     * General IO error.
     */
    class GeneralIOException(message: String) : FileOperationException(message)
}
