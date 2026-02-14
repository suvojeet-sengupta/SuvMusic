package com.suvojeet.suvmusic.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToQueue
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.RingVolume
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.ThumbDown
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.suvojeet.suvmusic.core.model.Song

/**
 * Song actions bottom sheet with options like Pin, Download, Add to Playlist, etc.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongActionsSheet(
    song: Song,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    isDownloaded: Boolean = false,
    onDownload: () -> Unit = {},
    onDeleteDownload: () -> Unit = {},
    onPlayNext: () -> Unit = {},
    onAddToQueue: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {},
    onViewInfo: () -> Unit = {},
    onViewComments: () -> Unit = {},
    onSleepTimer: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onToggleDislike: () -> Unit = {},
    isFavorite: Boolean = false,
    isDisliked: Boolean = false,
    onSetRingtone: () -> Unit = {},
    onStartRadio: () -> Unit = {},
    onListenTogether: () -> Unit = {},
    onPlaybackSpeed: () -> Unit = {},
    onEqualizerClick: () -> Unit = {},
    currentSpeed: Float = 1.0f,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onRemoveFromQueue: (() -> Unit)? = null,
    dominantColors: DominantColors? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    
    // Determine colors
    val backgroundColor = dominantColors?.secondary ?: MaterialTheme.colorScheme.surface
    val contentColor = dominantColors?.onBackground ?: MaterialTheme.colorScheme.onSurface
    val variantColor = dominantColors?.onBackground?.copy(alpha = 0.7f) ?: MaterialTheme.colorScheme.onSurfaceVariant
    
    // Share function
    val shareSong: () -> Unit = {
        val shareText = buildString {
            append("🎵 ${song.title}\n")
            append("🎤 ${song.artist}\n")
            if (!song.album.isNullOrBlank()) {
                append("💿 ${song.album}\n")
            }
            append("\n")
            
            // Clickable link first
            if (song.source == com.suvojeet.suvmusic.core.model.SongSource.JIOSAAVN) {
                val query = "${song.title} ${song.artist}".replace(" ", "+")
                append("https://www.google.com/search?q=$query")
            } else {
                append("https://music.youtube.com/watch?v=${song.id}")
            }
            
            // SuvMusic users note
            append("\n\n▶️ SuvMusic users: suvmusic://play?id=${song.id}")
        }
        
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "${song.title} - ${song.artist}")
            type = "text/plain"
        }
        
        val shareIntent = Intent.createChooser(sendIntent, "Share Song")
        context.startActivity(shareIntent)
    }
    
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    
    val handleAction: (() -> Unit) -> Unit = { action ->
        action()
        scope.launch {
            sheetState.hide()
            onDismiss()
        }
    }

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = backgroundColor,
            windowInsets = androidx.compose.foundation.layout.WindowInsets(0)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
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
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = variantColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (song.album != null) {
                            Text(
                                text = song.album,
                                style = MaterialTheme.typography.bodySmall,
                                color = variantColor.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = contentColor.copy(alpha = 0.2f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (isDownloaded) {
                    ActionItem(
                        icon = Icons.Default.Delete,
                        title = "Delete from downloads",
                        iconTint = contentColor,
                        textColor = contentColor,
                        onClick = { handleAction { onDeleteDownload() } }
                    )
                } else {
                    ActionItem(
                        icon = Icons.Default.Download,
                        title = "Download",
                        iconTint = contentColor,
                        textColor = contentColor,
                        onClick = { handleAction { onDownload() } }
                    )
                }

                ActionItem(
                    icon = Icons.Default.SkipNext,
                    title = "Play Next",
                    iconTint = contentColor,
                    textColor = contentColor,
                    onClick = { handleAction { onPlayNext() } }
                )

                ActionItem(
                    icon = Icons.Default.AddToQueue,
                    title = "Add to Queue",
                    iconTint = contentColor,
                    textColor = contentColor,
                    onClick = { handleAction { onAddToQueue() } }
                )

                ActionItem(
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    title = "Add to a Playlist...",
                    iconTint = contentColor,
                    textColor = contentColor,
                    onClick = { handleAction { onAddToPlaylist() } }
                )
                
                ActionItem(
                    icon = Icons.Default.Radio,
                    title = "Start a Radio",
                    iconTint = if (dominantColors != null) dominantColors.accent else MaterialTheme.colorScheme.primary,
                    textColor = contentColor,
                    onClick = { handleAction { onStartRadio() } }
                )

                ActionItem(
                    icon = Icons.Filled.Group,
                    title = "Listen With Together",
                    iconTint = if (dominantColors != null) dominantColors.accent else MaterialTheme.colorScheme.secondary,
                    textColor = contentColor,
                    onClick = { handleAction { onListenTogether() } }
                )
                
                ActionItem(
                    icon = Icons.Default.Share,
                    title = "Share Song",
                    iconTint = contentColor,
                    textColor = contentColor,
                    onClick = { handleAction { shareSong() } }
                )
                
                ActionItem(
                    icon = Icons.Default.Info,
                    title = "View Info",
                    iconTint = contentColor,
                    textColor = contentColor,
                    onClick = { handleAction { onViewInfo() } }
                )
                
                ActionItem(
                    icon = Icons.Default.Comment,
                    title = "View Comments",
                    iconTint = contentColor,
                    textColor = contentColor,
                    onClick = { handleAction { onViewComments() } }
                )
                
                val speedLabel = if (currentSpeed == 1.0f) "" else "($currentSpeed x)"
                ActionItem(
                    icon = Icons.Default.Speed,
                    title = "Speed & Tempo $speedLabel",
                    iconTint = if (dominantColors != null) dominantColors.accent else MaterialTheme.colorScheme.secondary,
                    textColor = contentColor,
                    onClick = { handleAction { onPlaybackSpeed() } }
                )

                ActionItem(
                    icon = Icons.Default.Tune, // Using Tune icon for Equalizer
                    title = "Equalizer",
                    iconTint = if (dominantColors != null) dominantColors.accent else MaterialTheme.colorScheme.secondary,
                    textColor = contentColor,
                    onClick = { handleAction { onEqualizerClick() } }
                )

                ActionItem(
                    icon = Icons.Default.Nightlight,
                    title = "Sleep Timer",
                    iconTint = if (dominantColors != null) dominantColors.accent else MaterialTheme.colorScheme.tertiary,
                    textColor = contentColor,
                    onClick = { handleAction { onSleepTimer() } }
                )

                // Dislike button removed as per request
                
                ActionItem(
                    icon = Icons.Default.RingVolume,
                    title = "Set as Ringtone",
                    iconTint = if (dominantColors != null) dominantColors.accent else MaterialTheme.colorScheme.secondary,
                    textColor = contentColor,
                    onClick = { handleAction { onSetRingtone() } }
                )

                if (onMoveUp != null || onMoveDown != null || onRemoveFromQueue != null) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        color = contentColor.copy(alpha = 0.2f)
                    )
                    
                    if (onMoveUp != null) {
                        ActionItem(
                            icon = Icons.Default.ArrowUpward,
                            title = "Move Up in Queue",
                            iconTint = contentColor,
                            textColor = contentColor,
                            onClick = { handleAction { onMoveUp() } }
                        )
                    }
                    
                    if (onMoveDown != null) {
                        ActionItem(
                            icon = Icons.Default.ArrowDownward,
                            title = "Move Down in Queue",
                            iconTint = contentColor,
                            textColor = contentColor,
                            onClick = { handleAction { onMoveDown() } }
                        )
                    }
                    
                    if (onRemoveFromQueue != null) {
                        ActionItem(
                            icon = Icons.Default.Delete,
                            title = "Remove from Queue",
                            iconTint = MaterialTheme.colorScheme.error,
                            textColor = contentColor,
                            onClick = { handleAction { onRemoveFromQueue() } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionItem(
    icon: ImageVector,
    title: String,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
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
            style = MaterialTheme.typography.bodyLarge,
            color = textColor
        )
    }
}
