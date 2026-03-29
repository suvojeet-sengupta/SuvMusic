package com.suvojeet.suvmusic.ui.screens.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.suvmusic.ui.components.DominantColors
import com.suvojeet.suvmusic.ui.theme.SquircleShape

@Composable
fun PlayerTopBar(
    onBack: () -> Unit,
    dominantColors: DominantColors,
    isVideoMode: Boolean = false,
    isYouTubeSong: Boolean = false,
    onVideoToggle: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    onCastClick: () -> Unit = {},
    audioArEnabled: Boolean = false,
    onRecenter: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 12.dp)
    ) {
        // Left side: Back Button
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(44.dp)
                .clip(SquircleShape)
                .background(dominantColors.onBackground.copy(alpha = 0.1f))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Close",
                tint = dominantColors.onBackground,
                modifier = Modifier.size(28.dp)
            )
        }

        // Absolute Center: Switch or Title
        Box(
            modifier = Modifier.align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            if (isYouTubeSong) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(dominantColors.onBackground.copy(alpha = 0.08f))
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (!isVideoMode) dominantColors.onBackground.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { if (isVideoMode) onVideoToggle() }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Audio",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (!isVideoMode) dominantColors.onBackground else dominantColors.onBackground.copy(alpha = 0.6f),
                            fontWeight = if (!isVideoMode) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isVideoMode) dominantColors.onBackground.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { if (!isVideoMode) onVideoToggle() }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Video",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isVideoMode) dominantColors.onBackground else dominantColors.onBackground.copy(alpha = 0.6f),
                            fontWeight = if (isVideoMode) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            } else {
                Text(
                    text = "NOW PLAYING",
                    style = MaterialTheme.typography.labelLarge,
                    color = dominantColors.onBackground.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }
        }

        // Right side: Cast, Audio AR and More Menu
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(SquircleShape)
                    .background(dominantColors.onBackground.copy(alpha = 0.1f))
                    .clickable(onClick = onCastClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Devices,
                    contentDescription = "Cast",
                    tint = dominantColors.onBackground,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            if (audioArEnabled) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(SquircleShape)
                        .background(dominantColors.onBackground.copy(alpha = 0.1f))
                        .clickable(onClick = onRecenter),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Recenter Audio",
                        tint = dominantColors.onBackground,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(SquircleShape)
                    .background(dominantColors.onBackground.copy(alpha = 0.1f))
                    .clickable(onClick = onMoreClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = dominantColors.onBackground,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
