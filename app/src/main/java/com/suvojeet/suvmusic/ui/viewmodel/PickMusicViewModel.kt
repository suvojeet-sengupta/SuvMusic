package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.model.Artist
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PickMusicViewModel @Inject constructor(
    private val repository: YouTubeRepository
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<Artist>>(emptyList())
    val searchResults: StateFlow<List<Artist>> = _searchResults.asStateFlow()

    private val _selectedArtists = MutableStateFlow<List<Artist>>(emptyList())
    val selectedArtists: StateFlow<List<Artist>> = _selectedArtists.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        // Load initial suggestions (e.g., trending artists or just some popular ones)
        // Since we don't have a direct "popular artists" endpoint handy, 
        // we can search for a generic term or leave it empty/show a message.
        // For now, let's search for "Trending Artists" to populate the grid.
        searchArtists("Trending Artists")
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.length > 2) {
            searchArtists(query)
        }
    }

    private fun searchArtists(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val results = repository.searchArtists(query)
                _searchResults.value = results
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleSelection(artist: Artist) {
        val currentSelection = _selectedArtists.value.toMutableList()
        val existing = currentSelection.find { it.id == artist.id }
        if (existing != null) {
            currentSelection.remove(existing)
        } else {
            // Limit selection? logic says "Pick artists", usually 5-6 is good.
            // visual logic in screenshot shows multiple.
            currentSelection.add(artist)
        }
        _selectedArtists.value = currentSelection
    }

    fun createMix(onMixReady: (List<Song>) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val mixPlaylist = mutableListOf<Song>()
            val artists = _selectedArtists.value
            
            if (artists.isEmpty()) {
                _isLoading.value = false
                return@launch
            }

            // Target roughly 100 songs
            val targetTotal = 100
            val songsPerArtist = (targetTotal / artists.size).coerceAtLeast(10)

            // Fetch songs for each selected artist
            artists.forEach { artist ->
                try {
                    // 1. Try to get artist details which usually includes top songs
                    val artistDetails = repository.getArtist(artist.id)
                    val collectedForArtist = mutableListOf<Song>()
                    
                    if (artistDetails != null) {
                        collectedForArtist.addAll(artistDetails.songs)
                    }
                    
                    // 2. If not enough, search for artist songs specifically
                    if (collectedForArtist.size < songsPerArtist) {
                        val searchSongs = repository.search("${artist.name} songs", YouTubeRepository.FILTER_SONGS)
                        collectedForArtist.addAll(searchSongs)
                    }
                    
                    // Add distinct songs to mix limits
                    mixPlaylist.addAll(collectedForArtist.distinctBy { it.id }.take(songsPerArtist))
                    
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Shuffle the result for a "Mix" feel and ensure uniqueness
            val finalMix = mixPlaylist.shuffled().distinctBy { it.id }
            
            // Create Playlist on YouTube Music if logged in
            if (finalMix.isNotEmpty()) {
                try {
                    val playlistName = "SuvMusic Mix ${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())}"
                    val playlistId = repository.createPlaylist(
                        title = playlistName,
                        description = "Created with SuvMusic featuring ${artists.joinToString { it.name }}",
                        privacyStatus = "PRIVATE"
                    )
                    
                    if (playlistId != null) {
                        repository.addSongsToPlaylist(playlistId, finalMix.map { it.id })
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                onMixReady(finalMix)
            }
            
            _isLoading.value = false
        }
    }
}
