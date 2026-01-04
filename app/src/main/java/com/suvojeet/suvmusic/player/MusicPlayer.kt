package com.suvojeet.suvmusic.player

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.suvojeet.suvmusic.data.model.DownloadState
import com.suvojeet.suvmusic.data.model.PlayerState
import com.suvojeet.suvmusic.data.model.RepeatMode
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.model.SongSource
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import com.suvojeet.suvmusic.service.MusicPlayerService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper around MediaController connected to MusicPlayerService.
 * This enables media notifications and proper audio focus handling.
 */
@Singleton
@OptIn(UnstableApi::class)
class MusicPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val youTubeRepository: YouTubeRepository
) {
    
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()
    
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    
    private var positionUpdateJob: Job? = null
    
    init {
        connectToService()
    }
    
    private fun connectToService() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicPlayerService::class.java)
        )
        
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                mediaController?.addListener(playerListener)
                
                // Restore state if player has media
                if (mediaController?.mediaItemCount ?: 0 > 0) {
                    startPositionUpdates()
                }
            } catch (e: Exception) {
                _playerState.update { it.copy(error = "Failed to connect to music service") }
            }
        }, MoreExecutors.directExecutor())
    }
    
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.update { it.copy(isPlaying = isPlaying) }
        }
        
        override fun onPlaybackStateChanged(playbackState: Int) {
            _playerState.update { 
                it.copy(
                    isLoading = playbackState == Player.STATE_BUFFERING,
                    error = null
                )
            }
            
            if (playbackState == Player.STATE_READY) {
                startPositionUpdates()
            }
        }
        
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.let { item ->
                val controller = mediaController ?: return@let
                val index = controller.currentMediaItemIndex
                val song = _playerState.value.queue.getOrNull(index)
                _playerState.update { 
                    it.copy(
                        currentSong = song,
                        currentIndex = index,
                        currentPosition = 0L,
                        duration = controller.duration.coerceAtLeast(0L),
                        // Reset metadata states on song change (will be updated by VM)
                        isLiked = false,
                        downloadState = DownloadState.NOT_DOWNLOADED
                    )
                }
            }
        }
        
        override fun onPlayerError(error: PlaybackException) {
            _playerState.update { 
                it.copy(
                    error = error.message ?: "Playback error",
                    isLoading = false
                )
            }
        }
    }
    
    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = scope.launch {
            while (true) {
                mediaController?.let { controller ->
                    _playerState.update { 
                        it.copy(
                            currentPosition = controller.currentPosition.coerceAtLeast(0L),
                            duration = controller.duration.coerceAtLeast(0L),
                            bufferedPercentage = controller.bufferedPercentage
                        )
                    }
                }
                delay(500)
            }
        }
    }
    
    /**
     * Play a single song.
     */
    fun playSong(song: Song, queue: List<Song> = listOf(song), startIndex: Int = 0) {
        scope.launch {
            _playerState.update { 
                it.copy(
                    queue = queue,
                    currentIndex = startIndex,
                    currentSong = song,
                    isLoading = true
                )
            }
            
            try {
                _playerState.update { it.copy(isLoading = true) }
                
                val mediaItems = queue.mapIndexed { index, s -> createMediaItem(s, index == startIndex) }
                mediaController?.let { controller ->
                    // Instant switch: stop and clear immediately
                    controller.stop()
                    controller.clearMediaItems()
                    
                    controller.setMediaItems(mediaItems, startIndex, 0L)
                    controller.prepare()
                    controller.play()
                } ?: run {
                    _playerState.update { it.copy(error = "Music service not connected", isLoading = false) }
                }
            } catch (e: Exception) {
                _playerState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
    
    private suspend fun createMediaItem(song: Song, resolveStream: Boolean = true): MediaItem {
        val uri = when (song.source) {
            SongSource.LOCAL, SongSource.DOWNLOADED -> song.localUri.toString()
            else -> {
                if (resolveStream) {
                    youTubeRepository.getStreamUrl(song.id) ?: "https://youtube.com/watch?v=${song.id}"
                } else {
                    "https://youtube.com/watch?v=${song.id}"
                }
            }
        }
        
        return MediaItem.Builder()
            .setUri(uri)
            .setMediaId(song.id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setArtworkUri(song.thumbnailUrl?.let { android.net.Uri.parse(it) })
                    .build()
            )
            .build()
    }
    
    fun play() {
        mediaController?.play()
    }
    
    fun pause() {
        mediaController?.pause()
    }
    
    fun togglePlayPause() {
        mediaController?.let { controller ->
            if (controller.isPlaying) pause() else play()
        }
    }
    
    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
    }
    
    fun seekToNext() {
        val state = _playerState.value
        val queue = state.queue
        if (queue.isEmpty()) return

        // Determine next index based on shuffle/repeat/order
        val nextIndex = if (state.shuffleEnabled) {
            // Ensure we don't pick the same song if queue > 1
            if (queue.size > 1) {
                var random = queue.indices.random()
                while (random == state.currentIndex) {
                    random = queue.indices.random()
                }
                random
            } else 0
        } else {
            state.currentIndex + 1
        }
        
        if (nextIndex in queue.indices) {
            playSong(queue[nextIndex], queue, nextIndex)
        } else {
            // End of queue logic
             if (state.repeatMode == RepeatMode.ALL) {
                 playSong(queue[0], queue, 0)
             } else if (state.isAutoplayEnabled) {
                 // Mock Autoplay: Just pick a random song from queue to mimic 'radio'
                 if (queue.isNotEmpty()) {
                     val random = queue.indices.random()
                     playSong(queue[random], queue, random)
                 }
             }
             // Else: Stop or do nothing
        }
    }
    
    fun seekToPrevious() {
        val state = _playerState.value
        // If played more than 3 seconds, restart current song
        if (state.currentPosition > 3000) {
            seekTo(0)
            return
        }
        
        val queue = state.queue
        if (queue.isEmpty()) return
        
        val prevIndex = if (state.shuffleEnabled) {
            if (queue.size > 1) {
                 var random = queue.indices.random() // Ideally we'd have a history stack
                 while (random == state.currentIndex) {
                     random = queue.indices.random()
                 }
                 random
             } else 0
        } else {
            state.currentIndex - 1
        }

        if (prevIndex in queue.indices) {
             playSong(queue[prevIndex], queue, prevIndex)
        } else {
            // If at start and repeat all is on, go to end? Or just stop.
            if (state.repeatMode == RepeatMode.ALL && queue.isNotEmpty()) {
                val lastIndex = queue.lastIndex
                playSong(queue[lastIndex], queue, lastIndex)
            }
        }
    }
    
    fun setRepeatMode(mode: RepeatMode) {
        mediaController?.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
        _playerState.update { it.copy(repeatMode = mode) }
    }
    
    fun toggleShuffle() {
        mediaController?.let { controller ->
            val newShuffleState = !controller.shuffleModeEnabled
            controller.shuffleModeEnabled = newShuffleState
            _playerState.update { it.copy(shuffleEnabled = newShuffleState) }
        }
    }
    
    fun toggleRepeat() {
        val currentMode = _playerState.value.repeatMode
        val nextMode = when (currentMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        setRepeatMode(nextMode)
    }
    
    fun updateLikeStatus(isLiked: Boolean) {
        _playerState.update { it.copy(isLiked = isLiked) }
    }
    
    fun updateDownloadState(state: DownloadState) {
        _playerState.update { it.copy(downloadState = state) }
    }
    
    fun getPlayer(): Player? = mediaController
    
<<<<<<< HEAD
    fun toggleAutoplay() {
        _playerState.update { it.copy(isAutoplayEnabled = !it.isAutoplayEnabled) }
    }

    /**
     * Reorders the queue by moving a song from [fromIndex] to [toIndex].
     */
    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val currentQueue = _playerState.value.queue.toMutableList()
        if (fromIndex in currentQueue.indices && toIndex in currentQueue.indices) {
            val item = currentQueue.removeAt(fromIndex)
            currentQueue.add(toIndex, item)
            
            // Update media controller if needed
             if (mediaController != null) {
                 mediaController?.moveMediaItem(fromIndex, toIndex)
             }
             
             // Update local state to reflect change immediately
             _playerState.update { 
                 it.copy(
                     queue = currentQueue,
                     // Simple index update - ideal logic would track current song ID
                     currentIndex = if (it.currentIndex == fromIndex) toIndex 
                                    else if (it.currentIndex in fromIndex..toIndex) it.currentIndex - 1
                                    else if (it.currentIndex in toIndex..fromIndex) it.currentIndex + 1
                                    else it.currentIndex
                 ) 
             }
        }
    }
    
     fun addToQueue(song: Song) {
          val currentQueue = _playerState.value.queue.toMutableList()
          currentQueue.add(song)
          _playerState.update { it.copy(queue = currentQueue) }
          
          val uri = if (song.source == SongSource.LOCAL) song.localUri 
                    else android.net.Uri.parse("https://youtube.com/watch?v=${song.id}")
                    
          if (uri != null) {
              mediaController?.addMediaItem(MediaItem.fromUri(uri))
          }
     }

=======
>>>>>>> parent of 6f6630d (feat(ui): Implement functional Queue/Now Playing screen - Added Queue Header and Controls Row (Shuffle, Repeat, Autoplay) - Updated QueueItem with drag handle visual - Added Autoplay logic placeholder)
    fun release() {
        positionUpdateJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
    }
}