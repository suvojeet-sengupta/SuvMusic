package com.suvojeet.suvmusic.ui.components.primitives

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.suvojeet.suvmusic.ui.components.bounceClick

/**
 * Consolidates the repeated Box + bounceClick + Icon pattern in
 * [com.suvojeet.suvmusic.ui.screens.player.components.PlaybackControls]. Caller passes
 * the tap target size, the icon size, and the visual content.
 *
 * Not a full Button — intentionally minimal so it can sit inside the `SpaceEvenly` Row
 * layout the player uses without introducing a framed/inked container.
 */
@Composable
fun AnimatedPlayerButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .bounceClick(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}
