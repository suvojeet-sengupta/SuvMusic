package com.suvojeet.suvmusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.suvojeet.suvmusic.data.model.Song

/**
 * Song actions bottom sheet with options like Pin, Download, Add to Playlist, etc.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongActionsSheet(
    song: Song,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onPinSong: () -> Unit = {},
    onDownload: () -> Unit = {},
    onDeleteFromLibrary: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {},
    onShare: () -> Unit = {},
    onViewCredits: () -> Unit = {},
    onCreateStation: () -> Unit = {},
    onSleepTimer: () -> Unit = {},
    onToggleFavorite: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState()
    
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                // Song header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = song.thumbnailUrl,
                        contentDescription = song.title,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (song.album != null) {
                            Text(
                                text = song.album,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Action items
                ActionItem(
                    icon = Icons.Default.PushPin,
                    title = "Pin Song",
                    onClick = { onPinSong(); onDismiss() }
                )
                
                ActionItem(
                    icon = Icons.Default.Download,
                    title = "Download",
                    onClick = { onDownload(); onDismiss() }
                )
                
                ActionItem(
                    icon = Icons.Default.Delete,
                    title = "Delete from Library",
                    iconTint = MaterialTheme.colorScheme.error,
                    onClick = { onDeleteFromLibrary(); onDismiss() }
                )
                
                ActionItem(
                    icon = Icons.Default.PlaylistAdd,
                    title = "Add to a Playlist...",
                    onClick = { onAddToPlaylist(); onDismiss() }
                )
                
                ActionItem(
                    icon = Icons.Default.Share,
                    title = "Share Song",
                    onClick = { onShare(); onDismiss() }
                )
                
                ActionItem(
                    icon = Icons.Default.Info,
                    title = "View Credits",
                    onClick = { onViewCredits(); onDismiss() }
                )
                
                ActionItem(
                    icon = Icons.Default.Radio,
                    title = "Create Station",
                    iconTint = MaterialTheme.colorScheme.primary,
                    onClick = { onCreateStation(); onDismiss() }
                )
                
                ActionItem(
                    icon = Icons.Default.Nightlight,
                    title = "Sleep Timer",
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    onClick = { onSleepTimer(); onDismiss() }
                )
                
                ActionItem(
                    icon = Icons.Default.StarOutline,
                    title = "Add to Favourites",
                    iconTint = MaterialTheme.colorScheme.primary,
                    onClick = { onToggleFavorite(); onDismiss() }
                )
            }
        }
    }
}

@Composable
private fun ActionItem(
    icon: ImageVector,
    title: String,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(20.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
