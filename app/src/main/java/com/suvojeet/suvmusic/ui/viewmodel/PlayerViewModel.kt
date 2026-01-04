package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.model.PlayerState
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.repository.DownloadRepository
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import com.suvojeet.suvmusic.player.MusicPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val musicPlayer: MusicPlayer,
    private val downloadRepository: DownloadRepository,
    private val youTubeRepository: YouTubeRepository
) : ViewModel() {
    
    val playerState: StateFlow<PlayerState> = musicPlayer.playerState
    
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
    
    fun downloadCurrentSong() {
        val song = playerState.value.currentSong ?: return
        viewModelScope.launch {
            downloadRepository.downloadSong(song)
        }
    }
    
    fun likeCurrentSong() {
        val song = playerState.value.currentSong ?: return
        viewModelScope.launch {
            youTubeRepository.rateSong(song.id, "LIKE")
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Don't release player here - it's shared
    }
}