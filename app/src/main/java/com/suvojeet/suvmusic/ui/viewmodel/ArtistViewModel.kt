package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.core.model.Artist
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.data.repository.JioSaavnRepository
import com.suvojeet.suvmusic.data.repository.LocalAudioRepository
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import com.suvojeet.suvmusic.navigation.Destination
import com.suvojeet.suvmusic.core.model.ArtistCreditInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ArtistError {
    NETWORK,
    AUTH_REQUIRED,
    UNKNOWN
}

data class ArtistUiState(
    val artist: Artist? = null,
    val isLoading: Boolean = false,
    val error: ArtistError? = null,
    val isSubscribing: Boolean = false,
    val isStartingRadio: Boolean = false,
    val radioStatus: String? = null,
    val showMultipleArtistsDialog: Boolean = false,
    val currentArtistCredits: List<ArtistCreditInfo> = emptyList()
)

@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
    private val jioSaavnRepository: JioSaavnRepository,
    private val localAudioRepository: LocalAudioRepository,
    private val sessionManager: SessionManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val artistId: String = checkNotNull(savedStateHandle[Destination.Artist.ARG_ARTIST_ID])

    private val _uiState = MutableStateFlow(ArtistUiState())
    val uiState: StateFlow<ArtistUiState> = _uiState.asStateFlow()

    init {
        loadArtist()
    }

    fun toggleMultipleArtistsDialog(show: Boolean, credits: List<ArtistCreditInfo> = emptyList()) {
        _uiState.update { 
            it.copy(
                showMultipleArtistsDialog = show,
                currentArtistCredits = if (show) credits else emptyList()
            ) 
        }
    }

    fun fetchArtistCreditsAndShow(artistString: String, source: com.suvojeet.suvmusic.core.model.SongSource) {
        viewModelScope.launch {
            val names = parseArtistNames(artistString)
            
            // Show dialog with placeholders immediately if multiple artists
            if (names.size > 1) {
                val placeholders = names.map { name ->
                    ArtistCreditInfo(name, "Vocals", null, null)
                }
                toggleMultipleArtistsDialog(true, placeholders)
                
                // Fetch thumbnails in background
                val updatedCredits = names.map { name ->
                    try {
                        val results = if (source == com.suvojeet.suvmusic.core.model.SongSource.JIOSAAVN) {
                            jioSaavnRepository.searchArtists(name)
                        } else {
                            youTubeRepository.searchArtists(name)
                        }
                        val match = results.firstOrNull { it.name.contains(name, true) || name.contains(it.name, true) } ?: results.firstOrNull()
                        ArtistCreditInfo(name, "Vocals", match?.thumbnailUrl, match?.id)
                    } catch (e: Exception) {
                        ArtistCreditInfo(name, "Vocals", null, null)
                    }
                }
                _uiState.update { it.copy(currentArtistCredits = updatedCredits) }
            } else {
                // Only one artist, handle directly or do nothing if already on that artist's page
                // But usually we don't need a dialog for 1 artist.
            }
        }
    }

    private fun parseArtistNames(artistString: String): List<String> {
        return artistString.split(",", "&", " feat.", " ft.", ";")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    fun loadArtist() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Heuristic to determine source
                val isYouTubeId = artistId.startsWith("UC") || artistId.startsWith("FE") || artistId.startsWith("VL")
                val isLocalId = artistId.toLongOrNull() != null
                
                val artist = if (isYouTubeId) {
                    youTubeRepository.getArtist(artistId)
                } else if (isLocalId) {
                    val id = artistId.toLong()
                    val artists = localAudioRepository.getAllLocalArtists()
                    val artistBase = artists.find { it.id == artistId }
                    if (artistBase != null) {
                        val songs = localAudioRepository.getSongsByArtist(id)
                        val albums = localAudioRepository.getAlbumsByArtist(id)
                        artistBase.copy(songs = songs, albums = albums)
                    } else null
                } else {
                    jioSaavnRepository.getArtist(artistId)
                }

                if (artist != null) {
                    _uiState.update {
                        it.copy(
                            artist = artist,
                            isLoading = false
                        )
                    }
                } else {
                    // Determine error type based on session state
                    val errorType = if (!sessionManager.isLoggedIn() && isYouTubeId) {
                        ArtistError.AUTH_REQUIRED
                    } else {
                        ArtistError.NETWORK
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = errorType
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = ArtistError.UNKNOWN,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun toggleSubscribe() {
        val currentArtist = _uiState.value.artist ?: return
        if (currentArtist.id.startsWith("UC") || currentArtist.id.startsWith("FE")) {
            viewModelScope.launch {
                _uiState.update { it.copy(isSubscribing = true) }
                val newStatus = !currentArtist.isSubscribed
                val success = youTubeRepository.subscribe(currentArtist.id, newStatus)
                
                if (success) {
                    // Update local state immediately for responsiveness
                    _uiState.update { 
                        it.copy(
                            artist = currentArtist.copy(isSubscribed = newStatus),
                            isSubscribing = false 
                        ) 
                    }
                    // Background refresh to get latest count/status
                    loadArtist()
                } else {
                    _uiState.update { it.copy(isSubscribing = false) }
                }
            }
        }
    }

    fun startRadio(onPlaylistReady: (List<Song>) -> Unit) {
        val currentArtist = _uiState.value.artist ?: return
        
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isStartingRadio = true,
                    radioStatus = "Connecting with ${currentArtist.name} radio station..."
                ) 
            }
            
            try {
                // 1. Get radio ID
                val radioId = currentArtist.channelId?.let { youTubeRepository.getArtistRadioId(it) }
                val allSongs = mutableListOf<Song>()

                if (radioId != null) {
                    // 2. Fetch songs from this radio/playlist
                    _uiState.update { it.copy(radioStatus = "Creating radio station...") }
                    val playlist = youTubeRepository.getPlaylist(radioId)
                    
                    if (playlist.songs.isNotEmpty()) {
                        // Tag songs as part of Artist Radio
                        val radioSongs = playlist.songs.map { song ->
                            song.copy(album = "Artist Radio: ${currentArtist.name}")
                        }
                        allSongs.addAll(radioSongs)
                    }
                }
                
                // 3. Supplement with more tracks from artist profile and search if needed
                if (allSongs.size < 20) {
                    _uiState.update { it.copy(radioStatus = "Searching for more ${currentArtist.name} tracks...") }
                    val topSongs = youTubeRepository.getArtistTopSongs(currentArtist.name, currentArtist.id)
                    allSongs.addAll(topSongs.filter { song -> 
                        allSongs.none { it.id == song.id }
                    })
                }

                if (allSongs.isNotEmpty()) {
                    onPlaylistReady(allSongs.distinctBy { it.id })
                } else {
                    onPlaylistReady(currentArtist.songs)
                }
            } catch (e: Exception) {
                onPlaylistReady(currentArtist.songs)
            } finally {
                _uiState.update { it.copy(isStartingRadio = false, radioStatus = null) }
            }
        }
    }
}
