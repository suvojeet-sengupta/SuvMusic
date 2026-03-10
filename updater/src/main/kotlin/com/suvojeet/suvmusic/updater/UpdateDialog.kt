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
    onDismiss: () -> Unit,
    onUpdate: (UpdateInfo) -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!updateInfo.forceUpdate) onDismiss() },
        title = { Text(text = "New Update Available: ${updateInfo.versionName}") },
        text = { 
            Text(
                text = updateInfo.changelog.ifBlank { 
                    "A new version of SuvMusic is available. Check the Updater screen for detailed release notes." 
                }
            ) 
        },
        confirmButton = {
            Button(onClick = {
                onUpdate(updateInfo)
                onDismiss()
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
