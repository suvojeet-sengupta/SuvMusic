package com.suvojeet.suvmusic.composeapp

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Desktop
import java.net.URI

private const val APP_VERSION = "2.3.0.0"

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "SuvMusic",
        state = rememberWindowState(size = DpSize(1024.dp, 720.dp)),
    ) {
        App(
            appVersion = APP_VERSION,
            onOpenUrl = ::openInBrowser,
        )
    }
}

/**
 * Open a URL in the user's default browser. Uses the AWT Desktop API,
 * which works on Windows / macOS / most Linux DEs. Silently no-ops if the
 * platform doesn't support it (headless, unusual desktop env) — the
 * About screen UI doesn't depend on the result.
 */
private fun openInBrowser(url: String) {
    try {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url))
        }
    } catch (t: Throwable) {
        // Best-effort only.
    }
}
