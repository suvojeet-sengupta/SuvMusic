package com.suvojeet.suvmusic.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.suvojeet.suvmusic.providers.lastfm.LastFmRepository
import com.suvojeet.suvmusic.data.SessionManager
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
    private val lastFmRepository: LastFmRepository,
    private val sessionManager: SessionManager
) : Player.Listener {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var scrobbleJob: Job? = null
    
    // Track state
    private var currentMediaId: String? = null
    private var currentDuration: Long = 0
    private var hasScrobbledCurrentTrack = false
    
    // Song metadata cache for the current track
    private var currentArtist: String = ""
    private var currentTitle: String = ""
    private var currentAlbum: String = ""
    
    // We need access to the player to check current position
    private var currentPlayer: Player? = null
    
    fun setPlayer(player: Player) {
        currentPlayer = player
        player.addListener(this)
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (mediaItem == null) return
        handleNewTrack(mediaItem)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) {
            // Resume monitoring if playing and not yet scrobbled
            if (!hasScrobbledCurrentTrack) {
                monitorPlayback()
            }
        } else {
            // Stop monitoring to save resources when paused
            scrobbleJob?.cancel()
        }
    }
    
    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_READY && !hasScrobbledCurrentTrack && currentPlayer?.isPlaying == true) {
             monitorPlayback()
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
        currentDuration = 0 // Reset duration, will be updated from player
        
        // 1. Update "Now Playing" and Start Monitoring
        if (shouldScrobble(mediaItem)) {
            scope.launch {
                val sessionKey = sessionManager.getLastFmSessionKey() ?: return@launch
                if (!sessionManager.isLastFmScrobblingEnabled()) return@launch

                if (sessionManager.isLastFmUseNowPlayingEnabled()) {
                    try {
                        lastFmRepository.updateNowPlaying(
                            sessionKey = sessionKey,
                            artist = currentArtist,
                            track = currentTitle,
                            album = currentAlbum,
                            duration = 0 
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("LastFmManager", "Failed to update Now Playing", e)
                    }
                }
                
                // 2. Start monitoring
                monitorPlayback()
            }
        }
    }
    
    private fun shouldScrobble(mediaItem: MediaItem?): Boolean {
        if (mediaItem == null) return false
        
        // Don't scrobble ads or placeholder clips
        if (mediaItem.mediaId.startsWith("ad_")) return false
        
        return true
    }
    
    private fun monitorPlayback() {
        scrobbleJob?.cancel()
        scrobbleJob = scope.launch {
            // Read settings
            val delayPercent = sessionManager.getScrobbleDelayPercent()
            val minDurationSeconds = sessionManager.getScrobbleMinDuration()
            val maxDelaySeconds = sessionManager.getScrobbleDelaySeconds()
            
            while (isActive) {
                val player = currentPlayer ?: break
                
                // Safety check: if player changed track and we missed listener (rare), abort
                if (player.currentMediaItem?.mediaId != currentMediaId) {
                    break
                }
                
                if (hasScrobbledCurrentTrack) {
                    break
                }

                if (player.isPlaying) {
                    val currentPos = player.currentPosition
                    val duration = player.duration
                    
                    // Update duration if it became available
                    if (duration > 0) {
                        currentDuration = duration
                    }
                    
                    // Minimum track length from settings
                    if (currentDuration > minDurationSeconds * 1000) {
                        // Calculate target time based on percentage and max delay
                        val targetMsByPercent = (currentDuration * delayPercent).toLong()
                        val targetMsByMaxDelay = maxDelaySeconds * 1000L
                        
                        // Rule: Scrobble after X% OR Y seconds, whichever is sooner
                        val targetMs = kotlin.math.min(targetMsByPercent, targetMsByMaxDelay)
                        
                        if (currentPos >= targetMs) {
                            scrobbleCurrentTrack()
                            hasScrobbledCurrentTrack = true
                            break 
                        }
                    }
                }
                
                delay(2000) // Check every 2 seconds
            }
        }
    }

    private suspend fun scrobbleCurrentTrack() {
        val player = currentPlayer ?: return
        val item = player.currentMediaItem ?: return
        
        if (!shouldScrobble(item)) return
        
        val sessionKey = sessionManager.getLastFmSessionKey() ?: return
        
        try {
            android.util.Log.d("LastFmManager", "Scrobbling: $currentTitle by $currentArtist")
            lastFmRepository.scrobble(
                sessionKey = sessionKey,
                artist = currentArtist,
                track = currentTitle,
                album = currentAlbum,
                duration = currentDuration,
                timestamp = System.currentTimeMillis() / 1000
            )
        } catch (e: Exception) {
            android.util.Log.e("LastFmManager", "Failed to scrobble", e)
        }
    }
}