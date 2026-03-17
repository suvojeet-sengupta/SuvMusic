package com.suvojeet.suvmusic.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
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
import com.suvojeet.suvmusic.ui.components.DominantColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutputDeviceSheet(
    isVisible: Boolean,
    devices: List<OutputDevice>,
    onDeviceSelected: (OutputDevice) -> Unit,
    onDismiss: () -> Unit,
    onRefreshDevices: () -> Unit = {},
    accentColor: Color = MaterialTheme.colorScheme.primary,
    dominantColors: DominantColors? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Determine colors
    val finalBackgroundColor = dominantColors?.secondary ?: MaterialTheme.colorScheme.surfaceContainerHigh
    val finalContentColor = dominantColors?.onBackground ?: MaterialTheme.colorScheme.onSurface
    val finalAccentColor = dominantColors?.accent ?: accentColor

    // Refresh devices when sheet becomes visible
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
            dragHandle = { BottomSheetDefaults.DragHandle() },
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            contentWindowInsets = { WindowInsets(0) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Audio Output",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        ),
                        color = finalContentColor
                    )
                    
                    FilledTonalIconButton(
                        onClick = onRefreshDevices,
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = finalAccentColor.copy(alpha = 0.15f),
                            contentColor = finalAccentColor
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (devices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                strokeWidth = 4.dp,
                                color = finalAccentColor,
                                strokeCap = StrokeCap.Round
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Scanning for devices...",
                                style = MaterialTheme.typography.titleMedium,
                                color = finalContentColor.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        items(
                            items = devices,
                            key = { it.id }
                        ) { device ->
                            DeviceItem(
                                device = device,
                                onClick = {
                                    onDeviceSelected(device)
                                },
                                accentColor = finalAccentColor,
                                contentColorOnBackground = finalContentColor
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = finalContentColor.copy(alpha = 0.05f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = finalAccentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Switch between connected audio outputs like Bluetooth, Cast, or Phone speakers.",
                            style = MaterialTheme.typography.labelMedium,
                            color = finalContentColor.copy(alpha = 0.7f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceItem(
    device: OutputDevice,
    onClick: () -> Unit,
    accentColor: Color,
    contentColorOnBackground: Color = MaterialTheme.colorScheme.onSurface
) {
    val isSelected = device.isSelected
    
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) accentColor.copy(alpha = 0.15f) 
                      else Color.Transparent,
        animationSpec = tween(durationMillis = 400),
        label = "containerColor"
    )
    
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) accentColor 
                      else contentColorOnBackground,
        animationSpec = tween(durationMillis = 400),
        label = "contentColor"
    )

    val iconContainerColor by animateColorAsState(
        targetValue = if (isSelected) accentColor.copy(alpha = 0.25f)
                      else contentColorOnBackground.copy(alpha = 0.08f),
        animationSpec = tween(durationMillis = 400),
        label = "iconContainerColor"
    )

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

    val icon = when (device.type) {
        DeviceType.PHONE -> Icons.Default.PhoneAndroid
        DeviceType.SPEAKER -> Icons.Default.Speaker
        DeviceType.BLUETOOTH -> Icons.Default.Bluetooth
        DeviceType.HEADPHONES -> Icons.Default.Headset
        DeviceType.CAST -> Icons.Default.Cast
        DeviceType.UNKNOWN -> Icons.Default.Devices
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Pulse effect for selected device
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = pulseAlpha))
                    )
                }
                
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = iconContainerColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                        letterSpacing = 0.1.sp
                    ),
                    color = contentColor
                )
                
                if (isSelected) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Currently Active",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = accentColor.copy(alpha = 0.9f)
                        )
                    }
                } else {
                    Text(
                        text = device.type.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColorOnBackground.copy(alpha = 0.6f)
                    )
                }
            }

            if (isSelected) {
                Surface(
                    modifier = Modifier.size(28.dp),
                    shape = CircleShape,
                    color = accentColor
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = contentColorOnBackground.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
