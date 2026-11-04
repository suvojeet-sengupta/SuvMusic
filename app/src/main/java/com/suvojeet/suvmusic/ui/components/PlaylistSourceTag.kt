package com.suvojeet.suvmusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

private const val LOCAL_PLAYLIST_PREFIX = "local_"

/** Tells playlists synced from the YouTube Music profile apart from ones only on this device. */
@Composable
fun PlaylistSourceTag(playlistId: String, modifier: Modifier = Modifier) {
    val isLocal = playlistId.startsWith(LOCAL_PLAYLIST_PREFIX)
    val colors = MaterialTheme.colorScheme
    Text(
        text = if (isLocal) "Local" else "Synced",
        style = MaterialTheme.typography.labelSmall,
        color = if (isLocal) colors.onSurfaceVariant else colors.onPrimaryContainer,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isLocal) colors.surfaceVariant else colors.primaryContainer)
            .padding(horizontal = 6.dp, vertical = 1.dp)
    )
}
