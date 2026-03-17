package com.suvojeet.suvmusic.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
 * Apple Music-inspired Song Credits screen (Updated to M3 Expressive).
 * Shows detailed song information like artist, album, duration, etc.
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
    val isDarkTheme = isSystemInDarkTheme()
    
    // Get high resolution thumbnail URL
    val highResThumbnail = remember(song.thumbnailUrl, song.id) {
        getHighResThumbnailUrl(song.thumbnailUrl, song.id)
    }
    
    // Extract dominant colors from artwork or use provided ones
    val finalDominantColors = dominantColors ?: rememberDominantColors(highResThumbnail, isDarkTheme)
    
    // Fetch artist thumbnails when sheet becomes visible
    LaunchedEffect(isVisible, song.artist) {
        if (isVisible) {
            viewModel.fetchArtistCredits(song.artist, song.source)
        }
    }
    
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = Color.Transparent,
            dragHandle = null,
            contentWindowInsets = { WindowInsets(0) }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(finalDominantColors.primary)
            ) {
                // Blurred background artwork
                AsyncImage(
                    model = highResThumbnail,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(450.dp)
                        .blur(80.dp),
                    contentScale = ContentScale.Crop
                )
                
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(450.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    finalDominantColors.primary.copy(alpha = 0.2f),
                                    finalDominantColors.primary.copy(alpha = 0.7f),
                                    finalDominantColors.primary
                                )
                            )
                        )
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 48.dp)
                ) {
                    // Top bar with close button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = finalDominantColors.secondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(40.dp),
                            onClick = onDismiss
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Close",
                                    tint = finalDominantColors.onBackground,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                    
                    // Artwork - Expressive styling
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = highResThumbnail,
                            contentDescription = song.title,
                            modifier = Modifier
                                .size(240.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .shadow(16.dp, RoundedCornerShape(24.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Song title
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp,
                            lineHeight = 32.sp
                        ),
                        color = finalDominantColors.onBackground,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Artist name
                    val mainArtistId = song.artistId ?: artistCredits.firstOrNull()?.artistId
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (mainArtistId != null) finalDominantColors.accent else finalDominantColors.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = mainArtistId != null) {
                                mainArtistId?.let { onArtistClick(it) }
                            }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Quality & Source Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val sourceBadge = when (song.source) {
                            com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE -> "YOUTUBE"
                            com.suvojeet.suvmusic.core.model.SongSource.JIOSAAVN -> "HQ AUDIO"
                            com.suvojeet.suvmusic.core.model.SongSource.LOCAL -> "LOCAL"
                            com.suvojeet.suvmusic.core.model.SongSource.DOWNLOADED -> "OFFLINE"
                            else -> "UNKNOWN"
                        }
                        
                        InfoBadge(
                            text = sourceBadge, 
                            isPrimary = true,
                            backgroundColor = finalDominantColors.accent,
                            contentColor = if (isDarkTheme) Color.Black else Color.White
                        )
                        
                        // Show actual codec from player
                        if (audioCodec != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            InfoBadge(
                                text = audioCodec.uppercase(),
                                isPrimary = false,
                                backgroundColor = finalDominantColors.secondary.copy(alpha = 0.6f),
                                contentColor = finalDominantColors.onBackground
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // --- AUDIO INFORMATION Section ---
                    ExpressiveSectionHeader("AUDIO INFORMATION", finalDominantColors.accent)
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        color = finalDominantColors.secondary.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, finalDominantColors.onBackground.copy(alpha = 0.1f))
                    ) {
                        Column {
                            val bitrateDisplay = audioBitrate?.let { "${it}kbps" } ?: "Unknown"
                            CreditItem(
                                icon = Icons.Default.MusicNote,
                                label = "Bitrate",
                                value = bitrateDisplay,
                                tint = finalDominantColors.accent,
                                onSurface = finalDominantColors.onBackground
                            )
                            
                            CreditDivider(finalDominantColors.onBackground.copy(alpha = 0.1f))
                            
                            val codecDisplay = audioCodec?.uppercase() ?: "Unknown"
                            CreditItem(
                                icon = Icons.Default.Headphones,
                                label = "Codec",
                                value = codecDisplay,
                                tint = finalDominantColors.accent,
                                onSurface = finalDominantColors.onBackground
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // --- PERFORMING ARTISTS Section ---
                    ExpressiveSectionHeader("PERFORMING ARTISTS", finalDominantColors.accent)
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        color = finalDominantColors.secondary.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, finalDominantColors.onBackground.copy(alpha = 0.1f))
                    ) {
                        Column {
                            artistCredits.forEachIndexed { index, artistInfo ->
                                ArtistCreditRow(
                                    artistName = artistInfo.name,
                                    role = artistInfo.role,
                                    thumbnailUrl = artistInfo.thumbnailUrl,
                                    artistId = artistInfo.artistId,
                                    onSurface = finalDominantColors.onBackground,
                                    onClick = { 
                                        artistInfo.artistId?.let { onArtistClick(it) }
                                    }
                                )
                                if (index < artistCredits.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 76.dp, end = 16.dp),
                                        color = finalDominantColors.onBackground.copy(alpha = 0.1f)
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // --- CREDITS section ---
                    ExpressiveSectionHeader("CREDITS", finalDominantColors.accent)
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        color = finalDominantColors.secondary.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, finalDominantColors.onBackground.copy(alpha = 0.1f))
                    ) {
                        Column {
                            CreditItem(
                                icon = Icons.Default.Person,
                                label = "Artist",
                                value = song.artist,
                                tint = finalDominantColors.accent,
                                onSurface = finalDominantColors.onBackground
                            )
                            
                            CreditDivider(finalDominantColors.onBackground.copy(alpha = 0.1f))
                            
                            if (!song.album.isNullOrBlank()) {
                                CreditItem(
                                    icon = Icons.Default.Album,
                                    label = "Album",
                                    value = song.album,
                                    tint = finalDominantColors.accent,
                                    onSurface = finalDominantColors.onBackground
                                )
                                CreditDivider(finalDominantColors.onBackground.copy(alpha = 0.1f))
                            }
                            
                            CreditItem(
                                icon = Icons.Default.Timer,
                                label = "Duration",
                                value = formatDurationForCredits(song.duration),
                                tint = finalDominantColors.accent,
                                onSurface = finalDominantColors.onBackground
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // --- ABOUT Section ---
                    ExpressiveSectionHeader("ABOUT", finalDominantColors.accent)
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        color = finalDominantColors.secondary.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, finalDominantColors.onBackground.copy(alpha = 0.1f))
                    ) {
                        Column {
                            CreditItem(
                                icon = Icons.Default.Language,
                                label = "Source",
                                value = when {
                                    song.source == com.suvojeet.suvmusic.core.model.SongSource.DOWNLOADED -> {
                                        when (song.originalSource) {
                                            com.suvojeet.suvmusic.core.model.SongSource.JIOSAAVN -> "HQ Audio (Downloaded)"
                                            com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE -> "YouTube (Downloaded)"
                                            else -> "Downloaded"
                                        }
                                    }
                                    song.source == com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE -> "YouTube Music"
                                    song.source == com.suvojeet.suvmusic.core.model.SongSource.JIOSAAVN -> "HQ Audio (Streaming)"
                                    song.source == com.suvojeet.suvmusic.core.model.SongSource.LOCAL -> "Local Storage"
                                    else -> "Unknown"
                                },
                                tint = finalDominantColors.accent,
                                onSurface = finalDominantColors.onBackground
                            )
                            
                            CreditDivider(finalDominantColors.onBackground.copy(alpha = 0.1f))
                            
                            CreditItem(
                                icon = Icons.Default.RecordVoiceOver,
                                label = "Content ID",
                                value = song.id,
                                canCopy = true,
                                tint = finalDominantColors.accent,
                                onSurface = finalDominantColors.onBackground
                            )
                            
                            CreditDivider(finalDominantColors.onBackground.copy(alpha = 0.1f))
                            
                            CreditItem(
                                icon = Icons.Default.Copyright,
                                label = "Copyright",
                                value = "© ${song.artist}",
                                tint = finalDominantColors.accent,
                                onSurface = finalDominantColors.onBackground
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    // Footer
                    Text(
                        text = "Powered by SuvMusic",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = finalDominantColors.accent.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

@Composable
private fun ExpressiveSectionHeader(text: String, tint: Color = MaterialTheme.colorScheme.primary) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        ),
        color = tint.copy(alpha = 0.7f),
        modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp)
    )
}

@Composable
private fun InfoBadge(
    text: String,
    isPrimary: Boolean = false,
    backgroundColor: Color? = null,
    contentColor: Color? = null
) {
    Surface(
        color = backgroundColor ?: (if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
        contentColor = contentColor ?: (if (isPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(24.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            )
        }
    }
}

@Composable
private fun CreditItem(
    icon: ImageVector,
    label: String,
    value: String,
    canCopy: Boolean = false,
    tint: Color = MaterialTheme.colorScheme.primary,
    onSurface: Color = MaterialTheme.colorScheme.onSurface
) {
    val context = LocalContext.current
    val clipboard = androidx.compose.ui.platform.LocalClipboard.current
    val scope = rememberCoroutineScope()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canCopy) {
                if (canCopy) {
                    scope.launch {
                        clipboard.setClipEntry(androidx.compose.ui.platform.ClipEntry(android.content.ClipData.newPlainText(label, value)))
                    }
                    android.widget.Toast.makeText(context, "Copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(12.dp),
            color = onSurface.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        if (canCopy) {
            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = "Copy",
                tint = onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun CreditDivider(color: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        color = color
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
 */
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

/**
 * Apple Music-style artist row with circular profile image and role label
 */
@Composable
private fun ArtistCreditRow(
    artistName: String,
    role: String,
    thumbnailUrl: String?,
    artistId: String?,
    onSurface: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = artistId != null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circular avatar
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(onSurface.copy(alpha = 0.1f)),
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
                val initials = artistName.split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .map { it.first().uppercaseChar() }
                    .joinToString("")
                
                Text(
                    text = initials.ifEmpty { "?" },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black
                    ),
                    color = onSurface
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Artist name and role
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artistName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = role,
                style = MaterialTheme.typography.labelMedium,
                color = onSurface.copy(alpha = 0.7f)
            )
        }
        
        if (artistId != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(16.dp).rotate(180f),
                tint = onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

/**
 * ViewModel for fetching artist credits with thumbnails from YouTube Music
 */
@HiltViewModel
class SongInfoViewModel @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
    private val jioSaavnRepository: com.suvojeet.suvmusic.data.repository.JioSaavnRepository
) : ViewModel() {
    
    private val _artistCredits = MutableStateFlow<List<ArtistCreditInfo>>(emptyList())
    val artistCredits: StateFlow<List<ArtistCreditInfo>> = _artistCredits.asStateFlow()
    
    private var lastArtistString: String? = null
    
    fun fetchArtistCredits(artistString: String, source: com.suvojeet.suvmusic.core.model.SongSource = com.suvojeet.suvmusic.core.model.SongSource.YOUTUBE) {
        if (artistString == lastArtistString && _artistCredits.value.isNotEmpty()) return
        lastArtistString = artistString
        
        viewModelScope.launch {
            val artistNames = parseArtistNames(artistString)
            
            _artistCredits.value = artistNames.map { name ->
                ArtistCreditInfo(
                    name = name,
                    role = "Vocals",
                    thumbnailUrl = null,
                    artistId = null
                )
            }
            
            val updatedCredits = artistNames.map { name ->
                try {
                    val searchResults = if (source == com.suvojeet.suvmusic.core.model.SongSource.JIOSAAVN) {
                        jioSaavnRepository.searchArtists(name)
                    } else {
                        youTubeRepository.searchArtists(name)
                    }
                    
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
        val separatorRegex = Regex("[,&]|\\b(feat\\.?|ft\\.?|with|x)\\b", RegexOption.IGNORE_CASE)
        return artistString.split(separatorRegex)
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
}
