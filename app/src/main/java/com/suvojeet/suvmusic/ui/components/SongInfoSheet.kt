package com.suvojeet.suvmusic.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.suvojeet.suvmusic.core.model.ArtistCreditInfo
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.model.SongSource
import com.suvojeet.suvmusic.ui.viewmodel.SongInfoViewModel

/**
 * YT Music inspired but unique Song Info screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongInfoSheet(
    song: Song,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onArtistClick: (String) -> Unit = {},
    audioCodec: String? = null,
    audioBitrate: Int? = null,
    dominantColors: DominantColors? = null,
    viewModel: SongInfoViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val artistCredits by viewModel.artistCredits.collectAsState()
    val releaseDate by viewModel.releaseDate.collectAsState()
    val isDarkTheme = isSystemInDarkTheme()
    
    // Get high resolution thumbnail URL
    val highResThumbnail = remember(song.thumbnailUrl, song.id) {
        getHighResThumbnailUrl(song.thumbnailUrl, song.id)
    }
    
    // Extract dominant colors
    val finalDominantColors = dominantColors ?: rememberDominantColors(highResThumbnail, isDarkTheme)
    
    LaunchedEffect(isVisible, song.artist, song.id) {
        if (isVisible) {
            viewModel.fetchArtistCredits(song.artist, song.source)
            if (song.releaseDate == null) {
                viewModel.fetchSongDetails(song.id, song.source, song.originalSource)
            }
        }
    }
    
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = if (isDarkTheme) Color.Black.copy(alpha = 0.95f) else MaterialTheme.colorScheme.surface,
            dragHandle = { 
                BottomSheetDefaults.DragHandle(
                    color = finalDominantColors.onBackground.copy(alpha = 0.2f)
                )
            },
            contentWindowInsets = { WindowInsets(0) },
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                finalDominantColors.primary.copy(alpha = 0.15f),
                                if (isDarkTheme) Color.Black else MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(bottom = 32.dp)
                ) {
                    // Header Section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = highResThumbnail,
                            contentDescription = null,
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .shadow(8.dp),
                            contentScale = ContentScale.Crop
                        )
                        
                        Spacer(modifier = Modifier.width(20.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = finalDominantColors.onBackground,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.bodyLarge,
                                color = finalDominantColors.accent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable {
                                    val id = song.artistId ?: artistCredits.firstOrNull()?.artistId
                                    id?.let { onArtistClick(it) }
                                }
                            )
                            
                            if (!song.album.isNullOrBlank()) {
                                Text(
                                    text = song.album!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = finalDominantColors.onBackground.copy(alpha = 0.5f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // TECHNICAL STATS
                    SectionHeader("TECHNICAL STATS", finalDominantColors.accent)
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        color = finalDominantColors.onBackground.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, finalDominantColors.onBackground.copy(alpha = 0.05f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val stats = listOf(
                                "Content ID" to song.id,
                                "Source" to song.source.name,
                                "Codec" to (audioCodec ?: "Opus"),
                                "Bitrate" to (audioBitrate?.let { "${it} kbps" } ?: "Variable"),
                                "Duration" to formatDurationForCredits(song.duration)
                            )
                            
                            stats.forEach { (label, value) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = finalDominantColors.onBackground.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = value,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        ),
                                        color = finalDominantColors.accent
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ARTISTS
                    SectionHeader("ARTISTS", finalDominantColors.accent)
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        artistCredits.forEach { artist ->
                            ArtistRow(
                                artist = artist,
                                onArtistClick = onArtistClick,
                                accentColor = finalDominantColors.accent,
                                onBackground = finalDominantColors.onBackground
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // RELEASE INFO
                    SectionHeader("RELEASE INFO", finalDominantColors.accent)
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        color = finalDominantColors.onBackground.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            InfoRow(Icons.Default.CalendarMonth, "Released", song.releaseDate ?: releaseDate ?: "Unknown", finalDominantColors.onBackground)
                            if (!song.album.isNullOrBlank()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = finalDominantColors.onBackground.copy(alpha = 0.05f))
                                InfoRow(Icons.Default.Album, "Album", song.album!!, finalDominantColors.onBackground)
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = finalDominantColors.onBackground.copy(alpha = 0.05f))
                            InfoRow(Icons.Default.Copyright, "Copyright", "© ${song.artist}", finalDominantColors.onBackground)
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))
                    
                    Text(
                        text = "SuvMusic Premium Metadata Engine",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = finalDominantColors.onBackground.copy(alpha = 0.3f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        ),
        color = color.copy(alpha = 0.8f),
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
    )
}

@Composable
private fun ArtistRow(
    artist: ArtistCreditInfo,
    onArtistClick: (String) -> Unit,
    accentColor: Color,
    onBackground: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(onBackground.copy(alpha = 0.05f))
            .clickable(enabled = artist.artistId != null) { artist.artistId?.let { onArtistClick(it) } }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = artist.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = onBackground
            )
            Text(
                text = artist.role,
                style = MaterialTheme.typography.labelMedium,
                color = onBackground.copy(alpha = 0.5f)
            )
        }
        
        if (artist.artistId != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = accentColor.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String, onBackground: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = onBackground.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = onBackground.copy(alpha = 0.4f))
            Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = onBackground)
        }
    }
}

private fun formatDurationForCredits(duration: Long): String {
    if (duration <= 0) return "Unknown"
    val totalSeconds = duration / 1000
    if (totalSeconds <= 0) return "Unknown"
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes}m ${seconds}s"
}

private fun getHighResThumbnailUrl(originalUrl: String?, videoId: String): String {
    if (originalUrl.isNullOrBlank()) {
        return "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"
    }
    
    if (originalUrl.contains("lh3.googleusercontent.com") || originalUrl.contains("yt3.ggpht.com")) {
        return originalUrl.replace(Regex("=w\\d+-h\\d+.*"), "=w544-h544")
            .replace(Regex("=s\\d+.*"), "=s544")
    }
    
    if (originalUrl.contains("ytimg.com") || originalUrl.contains("youtube.com")) {
        val ytVideoId = when {
            originalUrl.contains("/vi/") -> {
                originalUrl.substringAfter("/vi/").substringBefore("/")
            }
            else -> videoId
        }
        return "https://img.youtube.com/vi/$ytVideoId/hqdefault.jpg"
    }
    
    return originalUrl
}
