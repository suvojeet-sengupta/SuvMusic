package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.model.DownloadState
import com.suvojeet.suvmusic.data.model.PlayerState
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.repository.DownloadRepository
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
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val musicPlayer: MusicPlayer,
    private val downloadRepository: DownloadRepository,
    private val youTubeRepository: YouTubeRepository,
    private val sleepTimerManager: SleepTimerManager,
    private val sessionManager: SessionManager
) : ViewModel() {
    
    val playerState: StateFlow<PlayerState> = musicPlayer.playerState
    
    private val _lyricsState = kotlinx.coroutines.flow.MutableStateFlow<com.suvojeet.suvmusic.data.model.Lyrics?>(null)
    val lyricsState: StateFlow<com.suvojeet.suvmusic.data.model.Lyrics?> = _lyricsState.asStateFlow()
    
    private val _isFetchingLyrics = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isFetchingLyrics: StateFlow<Boolean> = _isFetchingLyrics.asStateFlow()
    
    // Sleep Timer
    val sleepTimerOption: StateFlow<SleepTimerOption> = sleepTimerManager.currentOption
    val sleepTimerRemainingMs: StateFlow<Long?> = sleepTimerManager.remainingTimeMs
    
    fun setSleepTimer(option: SleepTimerOption) {
        sleepTimerManager.startTimer(option)
    }
    
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
    

    
    private fun fetchLyrics(videoId: String) {
        viewModelScope.launch {
            _isFetchingLyrics.value = true
            _lyricsState.value = null
            
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
