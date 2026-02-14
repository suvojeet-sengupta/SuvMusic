package com.suvojeet.suvmusic.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.suvmusic.data.model.DeviceType
import com.suvojeet.suvmusic.data.model.OutputDevice
import androidx.compose.runtime.LaunchedEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutputDeviceSheet(
    isVisible: Boolean,
    devices: List<OutputDevice>,
    onDeviceSelected: (OutputDevice) -> Unit,
    onDismiss: () -> Unit,
    onRefreshDevices: () -> Unit = {},
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    // Refresh devices when sheet becomes visible
    LaunchedEffect(isVisible) {
        if (isVisible) {
            onRefreshDevices()
        }
    }
    
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            contentWindowInsets = { androidx.compose.foundation.layout.WindowInsets(0) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Audio Output",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(devices) { device ->
                        DeviceItem(
                            device = device,
                            onClick = {
                                onDeviceSelected(device)
                                // Don't auto-dismiss - let user close manually
                            },
                            accentColor = accentColor
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Info text
                Text(
                    text = "Connected devices will appear here automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun DeviceItem(
    device: OutputDevice,
    onClick: () -> Unit,
    accentColor: Color
) {
    val icon = when (device.type) {
        DeviceType.PHONE -> Icons.Default.PhoneAndroid
        DeviceType.SPEAKER -> Icons.Default.Speaker
        DeviceType.BLUETOOTH -> Icons.Default.Bluetooth
        DeviceType.HEADPHONES -> Icons.Default.Headset
        DeviceType.CAST -> Icons.Default.Cast
        DeviceType.UNKNOWN -> Icons.Default.Devices
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(10.dp),
            color = if (device.isSelected) accentColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (device.isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = device.name,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (device.isSelected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (device.isSelected) accentColor else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        if (device.isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
