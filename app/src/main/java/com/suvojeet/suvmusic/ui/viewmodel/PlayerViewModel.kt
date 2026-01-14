package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.model.DownloadState
import com.suvojeet.suvmusic.data.model.PlayerState
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.repository.DownloadRepository
import com.suvojeet.suvmusic.data.repository.JioSaavnRepository
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import com.suvojeet.suvmusic.player.MusicPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.suvojeet.suvmusic.player.SleepTimerManager
import com.suvojeet.suvmusic.player.SleepTimerOption
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.model.SongSource
import com.suvojeet.suvmusic.recommendation.RecommendationEngine
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val musicPlayer: MusicPlayer,
    private val downloadRepository: DownloadRepository,
    private val youTubeRepository: YouTubeRepository,
    private val jioSaavnRepository: JioSaavnRepository,
    private val sleepTimerManager: SleepTimerManager,
    private val sessionManager: SessionManager,
    private val recommendationEngine: RecommendationEngine
) : ViewModel() {
    
    val playerState: StateFlow<PlayerState> = musicPlayer.playerState
    
    private val _lyricsState = kotlinx.coroutines.flow.MutableStateFlow<com.suvojeet.suvmusic.data.model.Lyrics?>(null)
    val lyricsState: StateFlow<com.suvojeet.suvmusic.data.model.Lyrics?> = _lyricsState.asStateFlow()
    
    private val _isFetchingLyrics = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isFetchingLyrics: StateFlow<Boolean> = _isFetchingLyrics.asStateFlow()
    
    // Sleep Timer
    val sleepTimerOption: StateFlow<SleepTimerOption> = sleepTimerManager.currentOption
    val sleepTimerRemainingMs: StateFlow<Long?> = sleepTimerManager.remainingTimeMs
    
    fun setSleepTimer(option: SleepTimerOption, customMinutes: Int? = null) {
        sleepTimerManager.startTimer(option, customMinutes)
    }
    
    // Radio Mode State
    private val _isRadioMode = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isRadioMode: StateFlow<Boolean> = _isRadioMode.asStateFlow()
    
    private val _isLoadingMoreSongs = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isLoadingMoreSongs: StateFlow<Boolean> = _isLoadingMoreSongs.asStateFlow()
    
    private var radioBaseSongId: String? = null
    
    init {
        observeCurrentSong()
        observeDownloads()
    }
    
    private fun observeCurrentSong() {
        viewModelScope.launch {
            playerState.map { it.currentSong }
                .distinctUntilChanged()
                .collectLatest { song ->
                    if (song != null) {
                        checkLikeStatus(song)
                        checkDownloadStatus(song)
                        fetchLyrics(song.id)
                    } else {
                        _lyricsState.value = null
                    }
                }
        }
    }
    
    private fun observeDownloads() {
        viewModelScope.launch {
            // Wait a bit for downloads to be loaded, then check initial state
            kotlinx.coroutines.delay(500)
            val currentSong = playerState.value.currentSong
            if (currentSong != null) {
                checkDownloadStatus(currentSong)
            }
        }
        
        viewModelScope.launch {
            downloadRepository.downloadedSongs.collect {
                val currentSong = playerState.value.currentSong
                if (currentSong != null) {
                    checkDownloadStatus(currentSong)
                }
            }
        }
        
        viewModelScope.launch {
            downloadRepository.downloadingIds.collect { downloadingIds ->
                val currentSong = playerState.value.currentSong ?: return@collect
                if (downloadingIds.contains(currentSong.id)) {
                    musicPlayer.updateDownloadState(DownloadState.DOWNLOADING)
                } else {
                     // If it was downloading and now it's not, check if it's downloaded or failed
                     // This overlaps with the downloadedSongs collector but handles the transition faster
                     if (downloadRepository.isDownloaded(currentSong.id)) {
                         musicPlayer.updateDownloadState(DownloadState.DOWNLOADED)
                     } else {
                         musicPlayer.updateDownloadState(DownloadState.NOT_DOWNLOADED)
                     }
                }
            }
        }
    }
    
    private fun checkLikeStatus(song: Song) {
        viewModelScope.launch {
            val likedSongs = youTubeRepository.getLikedMusic()
            val isLiked = likedSongs.any { it.id == song.id }
            musicPlayer.updateLikeStatus(isLiked)
        }
    }
    
    private fun checkDownloadStatus(song: Song) {
        if (downloadRepository.isDownloading(song.id)) {
            musicPlayer.updateDownloadState(DownloadState.DOWNLOADING)
        } else if (downloadRepository.isDownloaded(song.id)) {
            musicPlayer.updateDownloadState(DownloadState.DOWNLOADED)
        } else {
            musicPlayer.updateDownloadState(DownloadState.NOT_DOWNLOADED)
        }
    }
    
    fun playSong(song: Song, queue: List<Song> = listOf(song), startIndex: Int = 0) {
        musicPlayer.playSong(song, queue, startIndex)
    }
    
    fun play() {
        musicPlayer.play()
    }
    
    fun pause() {
        musicPlayer.pause()
    }
    
    fun togglePlayPause() {
        musicPlayer.togglePlayPause()
    }
    
    fun seekTo(position: Long) {
        musicPlayer.seekTo(position)
    }
    
    fun seekToNext() {
        musicPlayer.seekToNext()
    }
    
    fun seekToPrevious() {
        musicPlayer.seekToPrevious()
    }
    
    fun toggleShuffle() {
        musicPlayer.toggleShuffle()
    }
    
    fun toggleRepeat() {
        musicPlayer.toggleRepeat()
    }
    
    fun toggleAutoplay() {
        musicPlayer.toggleAutoplay()
    }
    
    fun toggleVideoMode() {
        musicPlayer.toggleVideoMode()
    }

    fun switchOutputDevice(device: com.suvojeet.suvmusic.data.model.OutputDevice) {
        musicPlayer.switchOutputDevice(device)
    }
    
    fun refreshDevices() {
        musicPlayer.refreshDevices()
    }
    
    fun getPlayer() = musicPlayer.getPlayer()
    
    /**
     * Start a radio based on the given song.
     * Uses YT Music recommendations when logged in, local history-based recommendations when not.
     * Creates an endless queue that auto-loads more songs as you near the end.
     */
    fun startRadio(song: Song) {
        viewModelScope.launch {
            _isRadioMode.value = true
            radioBaseSongId = song.id
            
            val radioSongs = mutableListOf<Song>()
            radioSongs.add(song) // Current song first
            
            try {
                // Try YT Music recommendations first (works best when logged in)
                if (song.source == SongSource.YOUTUBE || song.source == SongSource.DOWNLOADED) {
                    val relatedSongs = youTubeRepository.getRelatedSongs(song.id)
                    if (relatedSongs.isNotEmpty()) {
                        radioSongs.addAll(relatedSongs.take(30))
                    }
                }
                
                // If not enough songs, use local recommendation engine
                if (radioSongs.size < 10) {
                    val localRecommendations = recommendationEngine.getPersonalizedRecommendations(30)
                    // Filter out songs already in queue
                    val existingIds = radioSongs.map { it.id }.toSet()
                    val newSongs = localRecommendations.filter { it.id !in existingIds }
                    radioSongs.addAll(newSongs)
                }
                
                // Play the radio queue
                if (radioSongs.isNotEmpty()) {
                    musicPlayer.playSong(song, radioSongs, 0)
                }
            } catch (e: Exception) {
                android.util.Log.e("PlayerViewModel", "Error starting radio", e)
                // Fallback: just play the song
                musicPlayer.playSong(song)
            }
        }
    }
    
    /**
     * Load more songs for endless radio queue.
     * Called automatically when near end of queue (infinite scroll).
     */
    fun loadMoreRadioSongs() {
        if (!_isRadioMode.value) return
        if (!sessionManager.isEndlessQueueEnabled()) return
        if (_isLoadingMoreSongs.value) return // Prevent duplicate loads
        
        val currentSong = playerState.value.currentSong ?: return
        val baseSongId = radioBaseSongId ?: currentSong.id
        
        viewModelScope.launch {
            _isLoadingMoreSongs.value = true
            try {
                val moreSongs = youTubeRepository.getRelatedSongs(baseSongId)
                if (moreSongs.isNotEmpty()) {
                    // Filter out songs already in queue
                    val currentQueue = playerState.value.queue
                    val existingIds = currentQueue.map { it.id }.toSet()
                    val newSongs = moreSongs.filter { it.id !in existingIds }
                    
                    if (newSongs.isNotEmpty()) {
                        musicPlayer.addToQueue(newSongs.take(10))
                        // Update base song for next batch (use last added song for variety)
                        radioBaseSongId = newSongs.lastOrNull()?.id ?: baseSongId
                    } else {
                        // If no new songs from YT, try local recommendations
                        val localRecs = recommendationEngine.getPersonalizedRecommendations(15)
                        val newLocalSongs = localRecs.filter { it.id !in existingIds }
                        if (newLocalSongs.isNotEmpty()) {
                            musicPlayer.addToQueue(newLocalSongs.take(10))
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PlayerViewModel", "Error loading more radio songs", e)
            } finally {
                _isLoadingMoreSongs.value = false
            }
        }
    }
    
    /**
     * Stop radio mode and clear the endless queue behavior.
     */
    fun stopRadio() {
        _isRadioMode.value = false
        radioBaseSongId = null
    }
    
    /**
     * Play a song from a deep link (YouTube/YouTube Music URL).
     * Fetches song details from YouTube and starts playback.
     */
    fun playFromDeepLink(videoId: String) {
        viewModelScope.launch {
            try {
                // Fetch song details from YouTube
                val song = youTubeRepository.getSongDetails(videoId)
                if (song != null) {
                    playSong(song)
                }
            } catch (e: Exception) {
                // Handle error - could show a toast or error state
                e.printStackTrace()
            }
        }
    }
    
    fun downloadCurrentSong() {
        val song = playerState.value.currentSong ?: return
        if (downloadRepository.isDownloaded(song.id) || downloadRepository.isDownloading(song.id)) return
        
        musicPlayer.updateDownloadState(DownloadState.DOWNLOADING)
        viewModelScope.launch {
            val success = downloadRepository.downloadSong(song)
            if (success) {
                musicPlayer.updateDownloadState(DownloadState.DOWNLOADED)
            } else {
                musicPlayer.updateDownloadState(DownloadState.FAILED)
            }
        }
    }
    
    /**
     * Download current song with progressive playback.
     * Starts playing after first ~30 seconds download, continues downloading in background.
     * Use this for "Download & Play" feature.
     */
    fun downloadAndPlayCurrentSong() {
        val song = playerState.value.currentSong ?: return
        if (downloadRepository.isDownloading(song.id)) return
        
        // If already downloaded, just play from local
        if (downloadRepository.isDownloaded(song.id)) {
            // Already playing or can seek to start
            return
        }
        
        musicPlayer.updateDownloadState(DownloadState.DOWNLOADING)
        viewModelScope.launch {
            val success = downloadRepository.downloadSongProgressive(song) { tempUri ->
                // First chunk ready - start playing from temp file
                android.util.Log.d("PlayerViewModel", "Progressive download ready, playing from: $tempUri")
                // The song is already playing (streaming), we just continue
                // The file will be saved when download completes
            }
            
            if (success) {
                musicPlayer.updateDownloadState(DownloadState.DOWNLOADED)
            } else {
                musicPlayer.updateDownloadState(DownloadState.FAILED)
            }
        }
    }
    
    /**
     * Download and immediately start playing a song (not current).
     * Perfect for clicking download on a song and having it play while downloading.
     */
    fun downloadAndPlay(song: Song) {
        if (downloadRepository.isDownloading(song.id)) return
        
        // If already downloaded, play from local
        if (downloadRepository.isDownloaded(song.id)) {
            val downloadedSong = downloadRepository.downloadedSongs.value.find { it.id == song.id }
            if (downloadedSong != null) {
                playSong(downloadedSong)
            }
            return
        }
        
        viewModelScope.launch {
            downloadRepository.downloadSongProgressive(song) { tempUri ->
                // First chunk ready - start playback from temp file
                val tempSong = song.copy(
                    source = SongSource.DOWNLOADED,
                    localUri = tempUri
                )
                playSong(tempSong)
            }
        }
    }    

    
    private fun fetchLyrics(videoId: String) {
        viewModelScope.launch {
            _isFetchingLyrics.value = true
            _lyricsState.value = null
            
            val currentSong = playerState.value.currentSong
            
            // For JioSaavn songs, use JioSaavn's synced lyrics API
            if (currentSong?.source == SongSource.JIOSAAVN || 
                currentSong?.originalSource == SongSource.JIOSAAVN) {
                try {
                    val lyrics = jioSaavnRepository.getSyncedLyrics(
                        songId = currentSong.id,
                        title = currentSong.title,
                        artist = currentSong.artist,
                        duration = currentSong.duration
                    )
                    if (lyrics != null) {
                        _lyricsState.value = lyrics
                        _isFetchingLyrics.value = false
                        return@launch
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PlayerViewModel", "Error fetching JioSaavn lyrics", e)
                }
            }
            
            // For YouTube songs or as fallback
            val lyrics = youTubeRepository.getLyrics(videoId)
            _lyricsState.value = lyrics
            
            _isFetchingLyrics.value = false
        }
    }
    
    fun likeCurrentSong() {
        val song = playerState.value.currentSong ?: return
        val currentLikeState = playerState.value.isLiked
        
        // Optimistic update
        musicPlayer.updateLikeStatus(!currentLikeState)
        
        viewModelScope.launch {
            val rating = if (!currentLikeState) "LIKE" else "INDIFFERENT" // Toggle
            val success = youTubeRepository.rateSong(song.id, rating)
            if (!success) {
                // Revert on failure
                musicPlayer.updateLikeStatus(currentLikeState)
            } else {
                // Ideally refresh liked songs list in background
                // youTubeRepository.refreshLikedSongs() 
            }
        }
    }
    
    /**
     * Restore last playback state if available.
     * @return true if playback was restored, false otherwise.
     */
    fun restoreLastPlayback(): Boolean {
        val lastState = sessionManager.getLastPlaybackState() ?: return false
        
        try {
            val jsonArray = org.json.JSONArray(lastState.queueJson)
            val queue = mutableListOf<Song>()
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                queue.add(
                    Song(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        artist = obj.getString("artist"),
                        album = obj.optString("album", ""),
                        thumbnailUrl = obj.optString("thumbnailUrl", null),
                        duration = obj.getLong("duration"),
                        source = try { 
                            SongSource.valueOf(obj.getString("source")) 
                        } catch (e: Exception) { 
                            SongSource.YOUTUBE 
                        }
                    )
                )
            }
            
            if (queue.isNotEmpty() && lastState.index in queue.indices) {
                val song = queue[lastState.index]
                // Load song without auto-playing (user can resume manually)
                musicPlayer.playSong(song, queue, lastState.index, autoPlay = false)
                
                // Seek to saved position after a delay (allow media to load)
                viewModelScope.launch {
                    kotlinx.coroutines.delay(1000)
                    musicPlayer.seekTo(lastState.position)
                }
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
    
    override fun onCleared() {
        super.onCleared()
        // Don't release player here - it's shared
    }
}
