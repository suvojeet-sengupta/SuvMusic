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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val musicPlayer: MusicPlayer,
    private val downloadRepository: DownloadRepository,
    private val youTubeRepository: YouTubeRepository
) : ViewModel() {
    
    val playerState: StateFlow<PlayerState> = musicPlayer.playerState
    
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
    
    override fun onCleared() {
        super.onCleared()
        // Don't release player here - it's shared
    }
}
