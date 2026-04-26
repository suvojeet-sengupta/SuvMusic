package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.core.model.ArtistCreditInfo
import com.suvojeet.suvmusic.core.model.SongSource
import com.suvojeet.suvmusic.data.repository.JioSaavnRepository
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for fetching artist credits with thumbnails from YouTube Music or JioSaavn
 */
class SongInfoViewModel @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
    private val jioSaavnRepository: JioSaavnRepository
) : ViewModel() {
    
    private val _artistCredits = MutableStateFlow<List<ArtistCreditInfo>>(emptyList())
    val artistCredits: StateFlow<List<ArtistCreditInfo>> = _artistCredits.asStateFlow()
    
    private val _releaseDate = MutableStateFlow<String?>(null)
    val releaseDate: StateFlow<String?> = _releaseDate.asStateFlow()
    
    private var lastArtistString: String? = null
    private var lastSongId: String? = null
    
    fun fetchSongDetails(songId: String, source: SongSource, originalSource: SongSource? = null) {
        if (songId == lastSongId) return
        lastSongId = songId
        _releaseDate.value = null
        
        viewModelScope.launch {
            try {
                // Bug Fix #3: Handle DOWNLOADED source by checking originalSource for details
                val effectiveSource = if (source == SongSource.DOWNLOADED && originalSource != null) {
                    originalSource
                } else {
                    source
                }

                val song = if (effectiveSource == SongSource.YOUTUBE) {
                    youTubeRepository.getSongDetails(songId)
                } else if (effectiveSource == SongSource.JIOSAAVN) {
                    jioSaavnRepository.getSongDetails(songId)
                } else {
                    null
                }
                
                if (song?.releaseDate != null) {
                    _releaseDate.value = if (song.releaseDate!!.contains("T")) {
                        formatDateString(song.releaseDate!!)
                    } else {
                        song.releaseDate
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun formatDateString(isoDate: String): String {
        return try {
            // ISO8601 is yyyy-MM-ddTHH:mm:ss
            val parts = isoDate.split("T")[0].split("-")
            if (parts.size >= 3) {
                val year = parts[0]
                val month = parts[1]
                val day = parts[2]
                
                val monthName = when (month) {
                    "01" -> "Jan"
                    "02" -> "Feb"
                    "03" -> "Mar"
                    "04" -> "Apr"
                    "05" -> "May"
                    "06" -> "Jun"
                    "07" -> "Jul"
                    "08" -> "Aug"
                    "09" -> "Sep"
                    "10" -> "Oct"
                    "11" -> "Nov"
                    "12" -> "Dec"
                    else -> month
                }
                
                "$monthName $day, $year"
            } else {
                isoDate
            }
        } catch (e: Exception) {
            isoDate
        }
    }
    
    fun fetchArtistCredits(artistString: String, source: SongSource) {
        // Bug Fix #2: Avoid flicker by checking if the artist string has actually changed
        if (artistString == lastArtistString && _artistCredits.value.isNotEmpty()) return
        lastArtistString = artistString
        
        viewModelScope.launch {
            val artistNames = parseArtistNames(artistString)
            
            // Set initial state with placeholders ONLY if we don't have existing credits
            if (_artistCredits.value.isEmpty()) {
                _artistCredits.value = artistNames.map { name ->
                    ArtistCreditInfo(
                        name = name,
                        role = "Vocals",
                        thumbnailUrl = null,
                        artistId = null
                    )
                }
            }
            
            // Bug Fix #1: Parallel metadata fetching using async/awaitAll
            val updatedCredits = artistNames.map { name ->
                async {
                    try {
                        val searchResults = if (source == SongSource.JIOSAAVN) {
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
            }.awaitAll()
            
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
