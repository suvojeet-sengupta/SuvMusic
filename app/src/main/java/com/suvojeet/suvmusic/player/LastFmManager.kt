package com.suvojeet.suvmusic.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.suvojeet.suvmusic.data.model.SongSource
import com.suvojeet.suvmusic.data.repository.LastFmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LastFmManager @Inject constructor(
    private val lastFmRepository: LastFmRepository
) : Player.Listener {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var scrobbleJob: Job? = null
    
    // Track state
    private var currentMediaId: String? = null
    private var currentStartTime: Long = 0
    private var currentDuration: Long = 0
    private var hasScrobbledCurrentTrack = false
    private var isPaused = false
    
    // Song metadata cache for the current track
    private var currentArtist: String = ""
    private var currentTitle: String = ""
    private var currentAlbum: String = ""

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (mediaItem == null) return
        
        // Handle previous track scrobble (if skipped/ended but logic didn't catch it yet? 
        // actually scrobble logic runs on timer/progress. Transition means new track.)
        // Reset state for new track
        handleNewTrack(mediaItem)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        isPaused = !isPlaying
        if (isPlaying) {
            // Resume/Start monitoring
            if (scrobbleJob == null || scrobbleJob?.isActive == false) {
                startScrobbleMonitor()
            }
        } else {
            // Paused: We might want to stop the timer or just let it handle pause state
        }
    }

    private fun handleNewTrack(mediaItem: MediaItem) {
        // Cancel pending scrobbles
        scrobbleJob?.cancel()
        hasScrobbledCurrentTrack = false
        
        // Extract Metadata
        currentMediaId = mediaItem.mediaId
        currentTitle = mediaItem.mediaMetadata.title?.toString() ?: "Unknown Track"
        currentArtist = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown Artist"
        currentAlbum = mediaItem.mediaMetadata.albumTitle?.toString() ?: ""
        
        // Duration is not always available immediately in MediaItem, often updated later
        // But we can try to get it if passed, or wait for onPlaybackStateChanged
        
        currentStartTime = System.currentTimeMillis()
        
        // 1. Update "Now Playing"
        if (shouldScrobble(mediaItem)) {
            scope.launch {
                lastFmRepository.updateNowPlaying(
                    artist = currentArtist,
                    track = currentTitle,
                    album = currentAlbum,
                    duration = 0 // Duration often unknown at start for streams, can update later
                )
            }
            // 2. Start monitoring for Scrobble (50% or 4 mins)
            startScrobbleMonitor()
        }
    }
    
    private fun startScrobbleMonitor() {
        scrobbleJob?.cancel()
        scrobbleJob = scope.launch {
            // Wait for duration to be known
            var attempts = 0
            while (currentDuration <= 0 && attempts < 10) {
                delay(1000)
                attempts++
            }
            
            // Logic: Scrobble after 50% or 4 minutes (240s), whichever is sooner.
            // But minimum track length 30s.
            if (currentDuration > 30_000) { // 30 seconds
                val targetTimeMs = kotlin.math.min(currentDuration / 2, 4 * 60 * 1000).toLong()
                
                // We need to track *playback time*, not wall clock time, to account for pauses.
                // Since valid playback time is hard to track without polling player position,
                // and we are just a Listener, we can't easily poll player.currentPosition here 
                // without reference to Player.
                
                // WAIT. As a Player.Listener, I don't get the Player instance passed in onMediaItemTransition.
                // I need the Player instance to poll position. 
                // Strategy: I will rely on the fact that I'm attached to the player.
                // BUT I don't have the reference inside this class unless I pass it or capture it.
                // Since I'm injected, I don't have the player.
                
                // Alternative: Use wall-clock time but subtract pause duration?
                // Or: Pass Player instance during attach? 
                
                // Let's make this class hold a weak reference to player or just be passed it?
                // Actually, listeners don't inherently know their player.
                // I will add a `setPlayer` or `attachTo` method.
            }
        }
    }
    
    // We need access to the player to check current position
    private var currentPlayer: Player? = null
    
    fun setPlayer(player: Player) {
        currentPlayer = player
        player.addListener(this)
    }
    
    private fun shouldScrobble(mediaItem: MediaItem?): Boolean {
        if (!lastFmRepository.isConnected()) return false
        if (mediaItem == null) return false
        
        // Filter out local clips/ads if necessary?
        // Assuming all main tracks are scrobble-able
        return true
    }
    
    // Improved Monitor with Player Reference
    private fun monitorPlayback() {
        scrobbleJob?.cancel()
        scrobbleJob = scope.launch {
            while (isActive) {
                val player = currentPlayer ?: break
                
                if (hasScrobbledCurrentTrack || mediaItemChanged(player)) {
                    // Stop if done or stale
                     if (mediaItemChanged(player)) {
                         // Detected change not caught by listener? 
                         // listener should catch it. Just safeguard.
                     }
                } else if (player.isPlaying) {
                    currentDuration = player.duration
                    val currentPos = player.currentPosition
                    
                    if (currentDuration > 30_000) {
                        val targetMs = kotlin.math.min(currentDuration / 2, 4 * 60 * 1000)
                        
                        if (currentPos >= targetMs) {
                            // Scrobble!
                            scrobbleCurrentTrack()
                            hasScrobbledCurrentTrack = true
                            break // Done for this track
                        }
                    }
                }
                
                delay(2000) // Check every 2 seconds
            }
        }
    }

    private fun mediaItemChanged(player: Player): Boolean {
        return player.currentMediaItem?.mediaId != currentMediaId
    }

    private suspend fun scrobbleCurrentTrack() {
        if (!shouldScrobble(currentPlayer?.currentMediaItem)) return
        
        lastFmRepository.scrobble(
            artist = currentArtist,
            track = currentTitle,
            album = currentAlbum,
            duration = currentDuration,
            timestamp = System.currentTimeMillis() / 1000
        )
    }
    
    // Restart monitor on playing state
    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_READY && !hasScrobbledCurrentTrack) {
             monitorPlayback()
        }
    }
}
