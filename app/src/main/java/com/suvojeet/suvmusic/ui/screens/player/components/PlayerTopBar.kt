package com.suvojeet.suvmusic.ui.screens.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 2.dp, end = 0.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Back Button
        Box(
            modifier = Modifier
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

        // Center: Switch or Title — uses weight to take available space, prevents overlap
        Box(
            modifier = Modifier.weight(1f, fill = false),
            contentAlignment = Alignment.Center
        ) {
            if (isYouTubeSong) {
                // Single-icon toggle: shows the mode you'll switch TO when tapped.
                // In audio mode → video icon (tap → switch to video).
                // In video mode → headphones icon (tap → switch back to audio).
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(SquircleShape)
                        .background(dominantColors.onBackground.copy(alpha = 0.1f))
                        .clickable(onClick = onVideoToggle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isVideoMode) Icons.Default.Headphones else Icons.Default.PlayArrow,
                        contentDescription = if (isVideoMode) "Switch to audio" else "Switch to video",
                        tint = dominantColors.onBackground,
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else {
                Text(
                    text = "NOW PLAYING",
                    style = MaterialTheme.typography.labelLarge,
                    color = dominantColors.onBackground.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Right side: Cast, Audio AR and More Menu
        Row(
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
