package com.suvojeet.suvmusic.ui.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

/**
 * Snackbar event data class.
 */
data class SnackbarEvent(
    val message: String,
    val action: SnackbarAction? = null,
    val duration: SnackbarDuration = SnackbarDuration.Short
)

data class SnackbarAction(
    val label: String,
    val action: () -> Unit
)

/**
 * Snackbar state holder for Compose screens.
 * Should be scoped to a screen or navigation destination.
 */
@Stable
class SnackbarState {
    private var _events = mutableStateOf<SnackbarEvent?>(null)
    val events: SnackbarEvent? get() = _events.value

    fun showSnackbar(event: SnackbarEvent) {
        _events.value = event
    }

    fun showInfo(message: String) {
        showSnackbar(SnackbarEvent(message = message))
    }

    fun showSuccess(message: String) {
        showSnackbar(SnackbarEvent(message = message))
    }

    fun showError(message: String) {
        showSnackbar(SnackbarEvent(message = message, duration = SnackbarDuration.Long))
    }

    fun showWarning(message: String) {
        showSnackbar(SnackbarEvent(message = message, duration = SnackbarDuration.Long))
    }

    fun showWithAction(
        message: String,
        actionLabel: String,
        onAction: () -> Unit
    ) {
        showSnackbar(
            SnackbarEvent(
                message = message,
                action = SnackbarAction(label = actionLabel, action = onAction)
            )
        )
    }

    fun consumeEvent() {
        _events.value = null
    }
}

/**
 * Creates and remembers a [SnackbarState].
 */
@Composable
fun rememberSnackbarState(): SnackbarState {
    return remember { SnackbarState() }
}

/**
 * Composable that observes and displays snackbars based on [SnackbarState].
 * Place this at the root of your Compose screen.
 */
@Composable
fun SnackbarHandler(
    snackbarState: SnackbarState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val scope = rememberCoroutineScope()
    val currentEvent = snackbarState.events

    LaunchedEffect(currentEvent) {
        if (currentEvent != null) {
            snackbarState.consumeEvent()
            val result = snackbarHostState.showSnackbar(
                message = currentEvent.message,
                actionLabel = currentEvent.action?.label,
                duration = currentEvent.duration
            )
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                currentEvent.action?.action?.invoke()
            }
        }
    }

    SnackbarHost(
        hostState = snackbarHostState,
        snackbar = { data ->
            Snackbar(
                snackbarData = data,
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface
            )
        }
    )
}

/**
 * A full-screen wrapper that provides a snackbar host.
 * Use this for screens that need snackbar support.
 */
@Composable
fun SnackbarScaffold(
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    content: @Composable (SnackbarHostState) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content(snackbarHostState)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
