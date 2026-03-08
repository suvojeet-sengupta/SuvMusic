package com.suvojeet.suvmusic.updater

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = { if (!updateInfo.forceUpdate) onDismiss() },
        title = { Text(text = "New Update Available: ${updateInfo.versionName}") },
        text = { Text(text = updateInfo.changelog) },
        confirmButton = {
            Button(onClick = {
                uriHandler.openUri(updateInfo.downloadUrl)
            }) {
                Text("Update Now")
            }
        },
        dismissButton = {
            if (!updateInfo.forceUpdate) {
                TextButton(onClick = onDismiss) {
                    Text("Later")
                }
            }
        }
    )
}
