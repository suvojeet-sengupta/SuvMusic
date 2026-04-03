package com.suvojeet.suvmusic.util

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Compose-compatible Snackbar manager for Jetpack Compose screens.
 * Replaces Toast usage with Material 3 Snackbars.
 *
 * Usage:
 * ```
 * val snackbarManager = SnackbarManager()
 * SnackbarHost(hostState = snackbarManager.snackbarHostState)
 *
 * // Show snackbar
 * snackbarManager.showInfo("Saved to Library")
 * ```
 */
@Stable
class SnackbarManager(
    val snackbarHostState: SnackbarHostState = SnackbarHostState()
) {
    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Shows an informational snackbar.
     */
    fun showInfo(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    /**
     * Shows a success snackbar.
     */
    fun showSuccess(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    /**
     * Shows an error snackbar.
     */
    fun showError(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long
            )
        }
    }

    /**
     * Shows a warning snackbar.
     */
    fun showWarning(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long
            )
        }
    }

    /**
     * Shows a snackbar with a custom action.
     */
    suspend fun showWithAction(
        message: String,
        actionLabel: String,
        duration: SnackbarDuration = SnackbarDuration.Short,
        onAction: () -> Unit
    ) {
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = duration
        )
        if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
            onAction()
        }
    }

    /**
     * Shows an indefinite snackbar (requires manual dismissal).
     */
    fun showIndefinite(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Indefinite
            )
        }
    }

    /**
     * Dismisses the current snackbar.
     */
    fun dismiss() {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }
}
