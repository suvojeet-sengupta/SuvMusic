package com.suvojeet.suvmusic.ui.screens.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OndemandVideo
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.suvmusic.ui.components.DominantColors

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
    // Row-based layout: a fixed-width back button, a flexible center zone that
    // takes the remaining space, and a fixed-width right cluster. The center
    // content (audio/video pill or "NOW PLAYING" title) is centered *within*
    // the gap between the two clusters, so it can never overlap them no matter
    // the screen density or font scale.
    //
    // A previous "truly center on screen" Box layout overlapped the title with
    // the right-side buttons on high-density / large-font devices because the
    // screen-centered title's right edge ran underneath the button cluster.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 2.dp, end = 0.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Back Button
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(dominantColors.onBackground.copy(alpha = 0.07f))
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

        // Center: Switch or Title — fills the space between the side clusters
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isYouTubeSong) {
                // Icon-only pill switcher: Audio / Video
                Row(
                    modifier = Modifier
                        .wrapContentSize()
                        .clip(CircleShape)
                        .background(dominantColors.onBackground.copy(alpha = 0.08f))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Audio Mode Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (!isVideoMode) dominantColors.onBackground.copy(alpha = 0.14f) else Color.Transparent)
                            .clickable { if (isVideoMode) onVideoToggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = "Audio Mode",
                            tint = if (!isVideoMode) dominantColors.onBackground else dominantColors.onBackground.copy(alpha = 0.55f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Video Mode Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isVideoMode) dominantColors.onBackground.copy(alpha = 0.14f) else Color.Transparent)
                            .clickable { if (!isVideoMode) onVideoToggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.OndemandVideo,
                            contentDescription = "Video Mode",
                            tint = if (isVideoMode) dominantColors.onBackground else dominantColors.onBackground.copy(alpha = 0.55f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
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
                    .clip(CircleShape)
                    .background(dominantColors.onBackground.copy(alpha = 0.07f))
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
                        .clip(CircleShape)
                        .background(dominantColors.onBackground.copy(alpha = 0.07f))
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
                    .clip(CircleShape)
                    .background(dominantColors.onBackground.copy(alpha = 0.07f))
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
