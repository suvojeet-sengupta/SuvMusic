package com.suvojeet.suvmusic.composeapp

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "SuvMusic",
        state = rememberWindowState(size = DpSize(1024.dp, 720.dp)),
    ) {
        App()
    }
}
