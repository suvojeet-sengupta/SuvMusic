package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.core.model.Artist
import com.suvojeet.suvmusic.core.model.Song
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

    private val _uiState = MutableStateFlow<PickMusicUiState>(PickMusicUiState.Selection)
    val uiState: StateFlow<PickMusicUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false) // Deprecate or keep for search loading only
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadPersonalizedArtists()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.length > 2) {
            searchArtists(query)
        } else if (query.isEmpty()) {
            // Restore personalized list if query cleared
            loadPersonalizedArtists()
        }
    }

    private fun loadPersonalizedArtists() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Try to fetch personalized library artists
                val libraryArtists = repository.getLibraryArtists()
                if (libraryArtists.isNotEmpty()) {
                    _searchResults.value = libraryArtists
                } else {
                    // 2. Fallback to Trending/Popular if library is empty or not logged in
                    searchArtists("Trending Artists") // or "Top Artists"
                }
            } catch (e: Exception) {
                // Fallback
                 searchArtists("Trending Artists")
            } finally {
                _isLoading.value = false
            }
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
            if (!repository.isLoggedIn()) {
                _uiState.value = PickMusicUiState.LoginRequired
                return@launch
            }

            _uiState.value = PickMusicUiState.Loading
            val mixPlaylist = mutableListOf<Song>()
            val artists = _selectedArtists.value
            
            if (artists.isEmpty()) {
                _uiState.value = PickMusicUiState.Selection
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
                    ) // default to public/unlisted if needed, but private is safe
                    
                    if (playlistId != null) {
                        repository.addSongsToPlaylist(playlistId, finalMix.map { it.id })
                        
                        // Show Success State with first song's thumbnail as preview
                        val message = getRandomSuccessMessage()
                        val thumbnailUrl = finalMix.firstOrNull()?.thumbnailUrl
                        
                        _uiState.value = PickMusicUiState.Success(
                            playlistId = playlistId, 
                            message = message,
                            playlistName = playlistName,
                            thumbnailUrl = thumbnailUrl
                        )
                        return@launch
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                // Fallback if playlist creation failed but we have songs
                onMixReady(finalMix)
                // Or show error in state? For now, if local mix is ready, just proceed or show generic success?
                // Let's stick to the flow: if playlist creation fails, we might just pass the songs. 
                // BUT user wants share button, so we really need that ID.
                // For this task, let's assume success or stick to selection.
            }
            // If we reached here without returning, something failed or list empty
             _uiState.value = PickMusicUiState.Selection
        }
    }

    fun resetState() {
        _uiState.value = PickMusicUiState.Selection
    }

    private fun getRandomSuccessMessage(): String {
        val messages = listOf(
            "Your playlist is ready to vibe!",
            "This mix is going to be fire! 🔥",
            "Curated just for you!",
            "Music to your ears, literally.",
            "Ready to rock and roll!",
            "Your soundtrack is served.",
            "Beats tailored to your taste.",
            "Hope you have your dancing shoes on!",
            "Excellent choice, Maestro!",
            "Your ears will thank you."
        )
        return messages.random()
    }
}

sealed class PickMusicUiState {
    object Selection : PickMusicUiState()
    object Loading : PickMusicUiState()
    object LoginRequired : PickMusicUiState()
    data class Success(
        val playlistId: String, 
        val message: String,
        val playlistName: String,
        val thumbnailUrl: String? = null
    ) : PickMusicUiState()
}
