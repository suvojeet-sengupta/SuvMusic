package com.suvojeet.suvmusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.suvojeet.suvmusic.data.model.Song

/**
 * Apple Music-inspired Song Credits screen.
 * Shows detailed song information like artist, album, duration, etc.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongCreditsSheet(
    song: Song,
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = Color.Transparent,
            dragHandle = null
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D0D0D))
            ) {
                // Get high resolution thumbnail URL
                val highResThumbnail = getHighResThumbnailUrl(song.thumbnailUrl, song.id)
                
                // Blurred background artwork
                AsyncImage(
                    model = highResThumbnail,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .blur(60.dp),
                    contentScale = ContentScale.Crop
                )
                
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF0D0D0D).copy(alpha = 0.7f),
                                    Color(0xFF0D0D0D)
                                )
                            )
                        )
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 32.dp)
                ) {
                    // Top bar with close button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Artwork - High Quality
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = highResThumbnail,
                            contentDescription = song.title,
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Song title
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Artist name
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFFF6B9D), // Apple Music pink
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Credits section header
                    Text(
                        text = "CREDITS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 2.sp
                        ),
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Credits list
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            CreditItem(
                                icon = Icons.Default.Person,
                                label = "Artist",
                                value = song.artist
                            )
                            
                            CreditDivider()
                            
                            if (!song.album.isNullOrBlank()) {
                                CreditItem(
                                    icon = Icons.Default.Album,
                                    label = "Album",
                                    value = song.album
                                )
                                CreditDivider()
                            }
                            
                            CreditItem(
                                icon = Icons.Default.Timer,
                                label = "Duration",
                                value = formatDurationForCredits(song.duration)
                            )
                            
                            CreditDivider()
                            
                            CreditItem(
                                icon = Icons.Default.MusicNote,
                                label = "Format",
                                value = "M4A / AAC"
                            )
                            
                            CreditDivider()
                            
                            CreditItem(
                                icon = Icons.Default.Headphones,
                                label = "Quality",
                                value = "High Quality Audio"
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Additional info section
                    Text(
                        text = "ABOUT",
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 2.sp
                        ),
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            CreditItem(
                                icon = Icons.Default.Language,
                                label = "Source",
                                value = when (song.source) {
                                    com.suvojeet.suvmusic.data.model.SongSource.YOUTUBE -> "YouTube Music"
                                    com.suvojeet.suvmusic.data.model.SongSource.LOCAL -> "Local Storage"
                                    com.suvojeet.suvmusic.data.model.SongSource.DOWNLOADED -> "Downloaded"
                                    else -> "Unknown"
                                }
                            )
                            
                            CreditDivider()
                            
                            CreditItem(
                                icon = Icons.Default.RecordVoiceOver,
                                label = "Content ID",
                                value = song.id
                            )
                            
                            CreditDivider()
                            
                            CreditItem(
                                icon = Icons.Default.Copyright,
                                label = "Copyright",
                                value = "© ${song.artist}"
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(40.dp))
                    
                    // Footer
                    Text(
                        text = "Powered by SuvMusic",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.3f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun CreditItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(22.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CreditDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = Color.White.copy(alpha = 0.1f)
    )
}

private fun formatDurationForCredits(duration: Long): String {
    if (duration <= 0) return "Unknown"
    val totalSeconds = duration / 1000
    if (totalSeconds <= 0) return "Unknown"
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes}m ${seconds}s"
}

/**
 * Get high resolution thumbnail URL for YouTube videos.
 * Converts low-res thumbnails to maxresdefault quality.
 * Handles both ytimg.com and lh3.googleusercontent.com formats.
 */
private fun getHighResThumbnailUrl(originalUrl: String?, videoId: String): String {
    // If no URL, try to construct from video ID
    if (originalUrl.isNullOrBlank()) {
        return "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"
    }
    
    // Handle lh3.googleusercontent.com URLs (YT Music style)
    // These URLs have size parameters like =w60-h60 or =w120-h120
    if (originalUrl.contains("lh3.googleusercontent.com") || originalUrl.contains("yt3.ggpht.com")) {
        // Remove size constraints to get full resolution
        return originalUrl.replace(Regex("=w\\d+-h\\d+.*"), "=w544-h544")
            .replace(Regex("=s\\d+.*"), "=s544")
    }
    
    // If it's a YouTube thumbnail URL, upgrade to maxresdefault
    if (originalUrl.contains("ytimg.com") || originalUrl.contains("youtube.com")) {
        // Extract video ID from various YouTube thumbnail URL formats
        val ytVideoId = when {
            originalUrl.contains("/vi/") -> {
                originalUrl.substringAfter("/vi/").substringBefore("/")
            }
            else -> videoId
        }
        return "https://img.youtube.com/vi/$ytVideoId/maxresdefault.jpg"
    }
    
    // For non-YouTube URLs, return original
    return originalUrl
}
