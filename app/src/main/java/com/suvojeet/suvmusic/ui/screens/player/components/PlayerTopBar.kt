package com.suvojeet.suvmusic.ui.screens.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.suvmusic.ui.components.DominantColors
import com.suvojeet.suvmusic.ui.theme.SquircleShape

@Composable
fun PlayerTopBar(
    onBack: () -> Unit,
    dominantColors: DominantColors,
    audioArEnabled: Boolean = false,
    onRecenter: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
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

        Text(
            text = "NOW PLAYING",
            style = MaterialTheme.typography.labelLarge,
            color = dominantColors.onBackground.copy(alpha = 0.8f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )

        if (audioArEnabled) {
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
        } else {
            // Spacer to balance the back button and keep title centered
            Spacer(modifier = Modifier.size(44.dp))
        }
    }
}
