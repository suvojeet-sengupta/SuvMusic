package com.suvojeet.suvmusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import javax.inject.Inject
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Data class to hold artist credit information with thumbnail
 */
data class ArtistCreditInfo(
    val name: String,
    val role: String,
    val thumbnailUrl: String?,
    val artistId: String?
)

/**
 * Apple Music-inspired Song Credits screen.
 * Shows detailed song information like artist, album, duration, etc.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongCreditsSheet(
    song: Song,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onArtistClick: (String) -> Unit = {},
    viewModel: SongCreditsViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val artistCredits by viewModel.artistCredits.collectAsState()
    
    // Fetch artist thumbnails when sheet becomes visible
    LaunchedEffect(isVisible, song.artist) {
        if (isVisible) {
            viewModel.fetchArtistCredits(song.artist)
        }
    }
    
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
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // PERFORMING ARTISTS Section (Apple Music style)
                    Text(
                        text = "PERFORMING ARTISTS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 2.sp
                        ),
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Artists vertical list in a Surface container
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column {
                            artistCredits.forEachIndexed { index, artistInfo ->
                                ArtistCreditRow(
                                    artistName = artistInfo.name,
                                    role = artistInfo.role,
                                    thumbnailUrl = artistInfo.thumbnailUrl,
                                    artistId = artistInfo.artistId,
                                    onClick = { 
                                        artistInfo.artistId?.let { onArtistClick(it) }
                                    }
                                )
                                if (index < artistCredits.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 72.dp),
                                        color = Color.White.copy(alpha = 0.1f)
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
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

/**
 * Parse artist string into list of individual artists.
 * Handles common separators: comma, "&", "feat.", "ft.", "x", "with"
 * Returns list of pairs: (artistName, artistId?) - artistId is null for now
 */
private fun parseArtists(artistString: String): List<Pair<String, String?>> {
    if (artistString.isBlank()) return emptyList()
    
    // Split by common separators
    val separatorRegex = Regex("[,&]|\\b(feat\\.?|ft\\.?|with|x)\\b", RegexOption.IGNORE_CASE)
    val artists = artistString.split(separatorRegex)
        .map { it.trim() }
        .filter { it.isNotBlank() }
    
    // Return as pairs with null artistId (would need API to get actual IDs)
    return artists.map { it to null }
}

/**
 * Apple Music-style artist row with circular profile image and role label
 */
@Composable
private fun ArtistCreditRow(
    artistName: String,
    role: String,
    thumbnailUrl: String?,
    artistId: String?,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = artistId != null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circular avatar - show thumbnail or initials
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF3A3A3C)),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = artistName,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Fallback to initials
                val initials = artistName.split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .map { it.first().uppercaseChar() }
                    .joinToString("")
                
                Text(
                    text = initials.ifEmpty { "?" },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Artist name and role
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artistName,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = role,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * ViewModel for fetching artist credits with thumbnails from YouTube Music
 */
@HiltViewModel
class SongCreditsViewModel @Inject constructor(
    private val youTubeRepository: YouTubeRepository
) : ViewModel() {
    
    private val _artistCredits = MutableStateFlow<List<ArtistCreditInfo>>(emptyList())
    val artistCredits: StateFlow<List<ArtistCreditInfo>> = _artistCredits.asStateFlow()
    
    private var lastArtistString: String? = null
    
    fun fetchArtistCredits(artistString: String) {
        // Avoid refetching if same artist string
        if (artistString == lastArtistString && _artistCredits.value.isNotEmpty()) return
        lastArtistString = artistString
        
        viewModelScope.launch {
            // First, parse the artist string to get individual names
            val artistNames = parseArtistNames(artistString)
            
            // Show artists immediately with no thumbnail
            _artistCredits.value = artistNames.map { name ->
                ArtistCreditInfo(
                    name = name,
                    role = "Vocals",
                    thumbnailUrl = null,
                    artistId = null
                )
            }
            
            // Then fetch thumbnails for each artist
            val updatedCredits = artistNames.map { name ->
                try {
                    val searchResults = youTubeRepository.searchArtists(name)
                    val matchingArtist = searchResults.firstOrNull { 
                        it.name.contains(name, ignoreCase = true) || 
                        name.contains(it.name, ignoreCase = true)
                    } ?: searchResults.firstOrNull()
                    
                    ArtistCreditInfo(
                        name = name,
                        role = "Vocals",
                        thumbnailUrl = matchingArtist?.thumbnailUrl,
                        artistId = matchingArtist?.id
                    )
                } catch (e: Exception) {
                    ArtistCreditInfo(
                        name = name,
                        role = "Vocals",
                        thumbnailUrl = null,
                        artistId = null
                    )
                }
            }
            
            _artistCredits.value = updatedCredits
        }
    }
    
    private fun parseArtistNames(artistString: String): List<String> {
        if (artistString.isBlank()) return emptyList()
        
        // Split by common separators
        val separatorRegex = Regex("[,&]|\\b(feat\\.?|ft\\.?|with|x)\\b", RegexOption.IGNORE_CASE)
        return artistString.split(separatorRegex)
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
}
