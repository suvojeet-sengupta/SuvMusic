package com.suvojeet.suvmusic.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.ui.theme.SquircleShape

/**
 * Premium Song actions bottom sheet with modern layout.
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
    isFromQueue: Boolean = false,
    isCurrentlyPlaying: Boolean = false,
    showShare: Boolean = true,
    dominantColors: DominantColors? = null,
    isDarkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    
    val backgroundColor = if (isDarkTheme) {
        dominantColors?.primary?.copy(alpha = 0.98f) ?: MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (isDarkTheme) {
        dominantColors?.onBackground ?: Color.White
    } else {
        Color.Black
    }
    val accentColor = dominantColors?.accent ?: MaterialTheme.colorScheme.primary
    
    val shareSong = androidx.compose.runtime.remember(song) {
        {
            val shareText = buildString {
                append("🎵 ${song.title}\n")
                append("🎤 ${song.artist}\n")
                if (!song.album.isNullOrBlank()) {
                    append("💿 ${song.album}\n")
                }
                append("\n")
                if (song.source == com.suvojeet.suvmusic.core.model.SongSource.JIOSAAVN) {
                    val query = "${song.title} ${song.artist}".replace(" ", "+")
                    append("https://www.google.com/search?q=$query")
                } else {
                    append("https://music.youtube.com/watch?v=${song.id}")
                }
                append("\n\n▶️ SuvMusic users: suvmusic://play?id=${song.id}")
            }
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_SUBJECT, "${song.title} - ${song.artist}")
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share Song"))
        }
    }
    
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val handleAction = androidx.compose.runtime.remember(sheetState, onDismiss) {
        { action: () -> Unit ->
            scope.launch {
                sheetState.hide()
                onDismiss()
                action()
            }
        }
    }

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = backgroundColor,
            contentWindowInsets = { WindowInsets(0) },
            dragHandle = null,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            ) {
                // Centered Premium Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = song.thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(SquircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.6f),
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                // Grid Actions (Modern Pill style)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ModernActionPill(
                        icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                        label = "Next",
                        onClick = { handleAction { onPlayNext() } },
                        modifier = Modifier.weight(1f),
                        containerColor = contentColor.copy(alpha = 0.05f),
                        contentColor = contentColor
                    )
                    ModernActionPill(
                        icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                        label = "Save",
                        onClick = { handleAction { onAddToPlaylist() } },
                        modifier = Modifier.weight(1f),
                        containerColor = contentColor.copy(alpha = 0.05f),
                        contentColor = contentColor
                    )
                    if (showShare) {
                        ModernActionPill(
                            icon = Icons.Default.Share,
                            label = "Share",
                            onClick = { handleAction { shareSong() } },
                            modifier = Modifier.weight(1f),
                            containerColor = contentColor.copy(alpha = 0.05f),
                            contentColor = contentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                // List of Actions with Icons and Dividers
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "PLAYBACK & QUEUE",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                        color = contentColor.copy(alpha = 0.4f),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                    
                    if (!isFromQueue && !isCurrentlyPlaying) {
                        ModernActionItem(Icons.Default.AddToQueue, "Add to Queue", contentColor, { handleAction(onAddToQueue) })
                    }
                    ModernActionItem(Icons.Default.Radio, "Start Radio", contentColor, { handleAction(onStartRadio) })
                    ModernActionItem(Icons.Default.Group, "Listen Together", contentColor, { handleAction(onListenTogether) })
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "OPTIONS",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                        color = contentColor.copy(alpha = 0.4f),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                    
                    ModernActionItem(
                        if (isDownloaded) Icons.Default.FileDownloadDone else Icons.Default.FileDownload,
                        if (isDownloaded) "Downloaded" else "Download",
                        if (isDownloaded) accentColor else contentColor,
                        { handleAction { if (isDownloaded) onDeleteDownload() else onDownload() } }
                    )
                    
                    val speedLabel = if (currentSpeed == 1.0f) "" else "($currentSpeed x)"
                    ModernActionItem(Icons.Default.Speed, "Playback Speed $speedLabel", contentColor, { handleAction(onPlaybackSpeed) })
                    ModernActionItem(Icons.Default.Tune, "Equalizer", contentColor, { handleAction(onEqualizerClick) })
                    ModernActionItem(Icons.Default.Nightlight, "Sleep Timer", contentColor, { handleAction(onSleepTimer) })
                    ModernActionItem(Icons.Default.RingVolume, "Set as Ringtone", contentColor, { handleAction(onSetRingtone) })
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "INFO",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                        color = contentColor.copy(alpha = 0.4f),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                    ModernActionItem(Icons.Default.Info, "Song Details", contentColor, { handleAction(onViewInfo) })
                    ModernActionItem(Icons.Default.Comment, "View Comments", contentColor, { handleAction(onViewComments) })

                    if (onRemoveFromQueue != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = contentColor.copy(alpha = 0.1f))
                        ModernActionItem(Icons.Default.Delete, "Remove from Queue", MaterialTheme.colorScheme.error, { handleAction(onRemoveFromQueue) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernActionPill(
    icon: ImageVector, label: String, onClick: () -> Unit,
    modifier: Modifier, containerColor: Color, contentColor: Color
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = CircleShape,
        color = containerColor
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = contentColor)
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = contentColor)
        }
    }
}

@Composable
private fun ModernActionItem(
    icon: ImageVector, title: String, tint: Color, onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(24.dp), tint = tint.copy(alpha = 0.8f))
        Spacer(modifier = Modifier.width(20.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = tint)
    }
}
