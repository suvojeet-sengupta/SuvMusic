package com.suvojeet.suvmusic.updater

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SquircleShape = RoundedCornerShape(28.dp)

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit,
    onUpdate: (UpdateInfo) -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!updateInfo.forceUpdate) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = { 
            Text(
                text = "New Update Available", 
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall
            ) 
        },
        text = { 
            Column {
                Text(
                    text = "v${updateInfo.versionName}",
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = updateInfo.changelog.ifBlank { 
                        "A new version of SuvMusic is available. It is recommended to stay up to date for the best experience." 
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                ) 
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onUpdate(updateInfo)
                    onDismiss()
                },
                shape = SquircleShape,
                modifier = Modifier.height(48.dp)
            ) {
                Text("Update Now", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            if (!updateInfo.forceUpdate) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Later", fontWeight = FontWeight.Medium)
                }
            }
        },
        shape = SquircleShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}
