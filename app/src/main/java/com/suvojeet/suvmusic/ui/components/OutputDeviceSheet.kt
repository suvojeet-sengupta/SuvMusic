package com.suvojeet.suvmusic.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.suvmusic.data.model.DeviceType
import com.suvojeet.suvmusic.data.model.OutputDevice
import com.suvojeet.suvmusic.ui.theme.SquircleShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutputDeviceSheet(
    isVisible: Boolean,
    devices: List<OutputDevice>,
    onDeviceSelected: (OutputDevice) -> Unit,
    onDismiss: () -> Unit,
    onRefreshDevices: () -> Unit = {},
    accentColor: Color = MaterialTheme.colorScheme.primary,
    dominantColors: DominantColors? = null,
    isDarkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Determine colors
    val finalBackgroundColor = if (isDarkTheme) {
        dominantColors?.primary?.copy(alpha = 0.98f) ?: MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surface
    }
    val finalContentColor = if (isDarkTheme) {
        dominantColors?.onBackground ?: Color.White
    } else {
        Color.Black
    }
    val finalAccentColor = dominantColors?.accent ?: accentColor

    LaunchedEffect(isVisible) {
        if (isVisible) {
            onRefreshDevices()
        }
    }
    
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = finalBackgroundColor,
            dragHandle = null,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            contentWindowInsets = { WindowInsets(0) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            ) {
                // Modern Header with Refresh
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, bottom = 12.dp, start = 24.dp, end = 24.dp)
                ) {
                    Column(modifier = Modifier.align(Alignment.CenterStart)) {
                        Text(
                            text = "Audio Output",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            ),
                            color = finalContentColor
                        )
                        Text(
                            text = "Choose where to play",
                            style = MaterialTheme.typography.labelMedium,
                            color = finalContentColor.copy(alpha = 0.5f)
                        )
                    }
                    
                    FilledTonalIconButton(
                        onClick = onRefreshDevices,
                        modifier = Modifier
                            .size(44.dp)
                            .align(Alignment.CenterEnd),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = finalContentColor.copy(alpha = 0.05f),
                            contentColor = finalContentColor
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                val currentDevice = devices.find { it.isSelected }
                
                // Hero Section for Active Device
                if (currentDevice != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        shape = SquircleShape,
                        color = finalAccentColor.copy(alpha = 0.12f),
                        border = null
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HeroDeviceIcon(currentDevice, finalAccentColor)
                            Spacer(modifier = Modifier.width(20.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Current Output",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        letterSpacing = 1.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = finalAccentColor.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = currentDevice.name,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = finalContentColor
                                )
                            }
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = finalAccentColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Available Devices Section
                Text(
                    text = "AVAILABLE DEVICES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = finalContentColor.copy(alpha = 0.4f),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )

                if (devices.isEmpty()) {
                    ScanningState(finalAccentColor, finalContentColor)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(items = devices.filter { !it.isSelected }, key = { it.id }) { device ->
                            DeviceListItem(
                                device = device,
                                onClick = { onDeviceSelected(device) },
                                contentColor = finalContentColor,
                                accentColor = finalAccentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroDeviceIcon(device: OutputDevice, accentColor: Color) {
    val icon = getDeviceIcon(device.type)
    
    // Pulsing animation for the active indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = pulseAlpha))
        )
        Surface(
            modifier = Modifier.size(56.dp),
            shape = SquircleShape,
            color = accentColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
private fun DeviceListItem(
    device: OutputDevice,
    onClick: () -> Unit,
    contentColor: Color,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = SquircleShape,
            color = contentColor.copy(alpha = 0.05f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = getDeviceIcon(device.type),
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = contentColor
            )
            Text(
                text = device.type.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.4f)
            )
        }
        
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(contentColor.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ScanningState(accentColor: Color, contentColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                strokeWidth = 3.dp,
                color = accentColor,
                strokeCap = StrokeCap.Round
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Looking for devices...",
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.5f)
            )
        }
    }
}

private fun getDeviceIcon(type: DeviceType) = when (type) {
    DeviceType.PHONE -> Icons.Default.Smartphone
    DeviceType.SPEAKER -> Icons.Default.Speaker
    DeviceType.BLUETOOTH -> Icons.Default.Bluetooth
    DeviceType.HEADPHONES -> Icons.Default.Headset
    DeviceType.CAST -> Icons.Default.Cast
    DeviceType.UNKNOWN -> Icons.Default.Devices
}
