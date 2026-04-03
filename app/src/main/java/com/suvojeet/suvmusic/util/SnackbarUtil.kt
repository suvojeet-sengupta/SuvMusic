package com.suvojeet.suvmusic.util

import android.view.View
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Centralized utility for showing Snackbars instead of Toasts.
 * Provides a consistent UX across the app with Material Design guidelines.
 */
object SnackbarUtil {

    enum class Duration {
        SHORT,
        LONG,
        INDEFINITE
    }

    private fun mapDuration(duration: Duration): Int = when (duration) {
        Duration.SHORT -> Snackbar.LENGTH_SHORT
        Duration.LONG -> Snackbar.LENGTH_LONG
        Duration.INDEFINITE -> Snackbar.LENGTH_INDEFINITE
    }

    /**
     * Shows a simple snackbar with a message.
     */
    fun showMessage(
        view: View,
        message: String,
        duration: Duration = Duration.SHORT
    ) {
        Snackbar.make(view, message, mapDuration(duration)).show()
    }

    /**
     * Shows a snackbar with a string resource.
     */
    fun showMessage(
        view: View,
        @StringRes messageResId: Int,
        duration: Duration = Duration.SHORT,
        vararg args: Any
    ) {
        val message = view.resources.getString(messageResId, *args)
        showMessage(view, message, duration)
    }

    /**
     * Shows a snackbar with an action button.
     */
    fun showMessageWithAction(
        view: View,
        message: String,
        actionText: String,
        action: () -> Unit,
        duration: Duration = Duration.SHORT
    ) {
        Snackbar.make(view, message, mapDuration(duration))
            .setAction(actionText) { action() }
            .show()
    }

    /**
     * Shows an error snackbar (with error color tint).
     */
    fun showError(
        view: View,
        message: String,
        duration: Duration = Duration.LONG
    ) {
        Snackbar.make(view, message, mapDuration(duration))
            .setBackgroundTint(view.resources.getColor(android.R.color.holo_red_dark, view.context.theme))
            .setTextColor(view.resources.getColor(android.R.color.white, view.context.theme))
            .show()
    }

    /**
     * Shows an error snackbar with a string resource.
     */
    fun showError(
        view: View,
        @StringRes messageResId: Int,
        duration: Duration = Duration.LONG,
        vararg args: Any
    ) {
        val message = view.resources.getString(messageResId, *args)
        showError(view, message, duration)
    }

    /**
     * Shows a success snackbar (with green tint).
     */
    fun showSuccess(
        view: View,
        message: String,
        duration: Duration = Duration.SHORT
    ) {
        Snackbar.make(view, message, mapDuration(duration))
            .setBackgroundColor(0xFF2E7D32.toInt())
            .setTextColor(0xFFFFFFFF.toInt())
            .show()
    }

    /**
     * Shows a success snackbar with a string resource.
     */
    fun showSuccess(
        view: View,
        @StringRes messageResId: Int,
        duration: Duration = Duration.SHORT,
        vararg args: Any
    ) {
        val message = view.resources.getString(messageResId, *args)
        showSuccess(view, message, duration)
    }

    /**
     * Shows a warning snackbar (with orange tint).
     */
    fun showWarning(
        view: View,
        message: String,
        duration: Duration = Duration.LONG
    ) {
        Snackbar.make(view, message, mapDuration(duration))
            .setBackgroundColor(0xFFE65C00.toInt())
            .setTextColor(0xFFFFFFFF.toInt())
            .show()
    }

    /**
     * Async wrapper for showing snackbars from coroutines.
     */
    fun showAsync(
        view: View,
        message: String,
        duration: Duration = Duration.SHORT,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    ) {
        scope.launch {
            showMessage(view, message, duration)
        }
    }

    /**
     * Async error wrapper.
     */
    fun showErrorAsync(
        view: View,
        message: String,
        duration: Duration = Duration.LONG,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    ) {
        scope.launch {
            showError(view, message, duration)
        }
    }
}
