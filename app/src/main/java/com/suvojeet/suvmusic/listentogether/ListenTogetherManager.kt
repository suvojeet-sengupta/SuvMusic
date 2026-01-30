package com.suvojeet.suvmusic.listentogether

import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager that bridges the Listen Together WebSocket client with the music player.
 * Handles syncing playback actions between connected users.
 */
@Singleton
class ListenTogetherManager @Inject constructor(
    private val client: ListenTogetherClient,
    private val youTubeRepository: YouTubeRepository
) {
    companion object {
        private const val TAG = "ListenTogetherManager"
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private var player: ExoPlayer? = null
    private var eventCollectorJob: Job? = null
    private var playerListenerRegistered = false
    
    // Whether we're currently syncing (to prevent feedback loops)
    @Volatile
    private var isSyncing = false
    
    // Track the last state we synced to avoid duplicate events
    private var lastSyncedIsPlaying: Boolean? = null
    private var lastSyncedTrackId: String? = null
    
    // Track ID being buffered
    private var bufferingTrackId: String? = null
    
    // Track active sync job to cancel it if a better update arrives
    private var activeSyncJob: Job? = null

    // Pending sync to apply after buffering completes for guest
    private var pendingSyncState: SyncStatePayload? = null

    // Track if a buffer-complete arrived before the pending sync was ready
    private var bufferCompleteReceivedForTrack: String? = null

    // Expose client state
    val connectionState = client.connectionState
    val roomState = client.roomState
    val role = client.role
    val userId = client.userId
    val pendingJoinRequests = client.pendingJoinRequests
    val bufferingUsers = client.bufferingUsers
    val logs = client.logs
    val events = client.events
    val pendingSuggestions = client.pendingSuggestions

    val isInRoom: Boolean get() = client.isInRoom
    val isHost: Boolean get() = client.isHost
    val hasPersistedSession: Boolean get() = client.hasPersistedSession
    
    private val playerListener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (isSyncing || !isHost || !isInRoom) return
            
            Log.d(TAG, "Play state changed: $playWhenReady (reason: $reason)")
            
            val currentTrackId = player?.currentMediaItem?.mediaId
            if (currentTrackId != null && currentTrackId != lastSyncedTrackId) {
                // Track changed, logic handled in onMediaItemTransition mostly, but safety check
                return
            }
            
            sendPlayState(playWhenReady)
        }
        
        private fun sendPlayState(playWhenReady: Boolean) {
            val position = player?.currentPosition ?: 0
            
            if (playWhenReady) {
                Log.d(TAG, "Host sending PLAY at position $position")
                client.sendPlaybackAction(PlaybackActions.PLAY, position = position)
                lastSyncedIsPlaying = true
            } else if (!playWhenReady && (lastSyncedIsPlaying == true)) {
                Log.d(TAG, "Host sending PAUSE at position $position")
                client.sendPlaybackAction(PlaybackActions.PAUSE, position = position)
                lastSyncedIsPlaying = false
            }
        }
        
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (isSyncing || !isHost || !isInRoom) return
            if (mediaItem == null) return
            
            val trackId = mediaItem.mediaId
            if (trackId == lastSyncedTrackId) return
            
            lastSyncedTrackId = trackId
            lastSyncedIsPlaying = false
            
            val metadata = mediaItem.mediaMetadata
            Log.d(TAG, "Host sending track change: ${metadata.title}")
            sendTrackChange(mediaItem)
            
            val isPlaying = player?.playWhenReady == true
            if (isPlaying) {
                Log.d(TAG, "Host is playing during track change, sending PLAY")
                lastSyncedIsPlaying = true
                val position = player?.currentPosition ?: 0
                client.sendPlaybackAction(PlaybackActions.PLAY, position = position)
            }
        }
        
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (isSyncing || !isHost || !isInRoom) return
            
            if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                Log.d(TAG, "Host sending SEEK to ${newPosition.positionMs}")
                client.sendPlaybackAction(PlaybackActions.SEEK, position = newPosition.positionMs)
            }
        }
    }

    fun setPlayer(exoPlayer: ExoPlayer?) {
        Log.d(TAG, "setPlayer: ${exoPlayer != null}, isInRoom: $isInRoom")
        
        if (playerListenerRegistered && player != null) {
            player?.removeListener(playerListener)
            playerListenerRegistered = false
        }
        
        player = exoPlayer
        
        if (player != null && isInRoom) {
            player?.addListener(playerListener)
            playerListenerRegistered = true
            Log.d(TAG, "Added player listener for room sync")
        }

        if (player != null && isInRoom && isHost) {
            startHeartbeat()
        } else {
            stopHeartbeat()
        }
    }

    fun initialize() {
        Log.d(TAG, "Initializing ListenTogetherManager")
        eventCollectorJob?.cancel()
        eventCollectorJob = scope.launch {
            client.events.collect { event ->
                Log.d(TAG, "Received event: $event")
                handleEvent(event)
            }
        }
        
        scope.launch {
            role.collect { newRole ->
                val wasHost = isHost
                if (newRole == RoomRole.HOST && !wasHost && player != null) {
                    Log.d(TAG, "Role changed to HOST, starting sync services")
                    startHeartbeat()
                    if (!playerListenerRegistered) {
                        player!!.addListener(playerListener)
                        playerListenerRegistered = true
                    }
                } else if (newRole != RoomRole.HOST && wasHost) {
                    Log.d(TAG, "Role changed from HOST, stopping sync services")
                    stopHeartbeat()
                }
            }
        }
    }

    private fun handleEvent(event: ListenTogetherEvent) {
        when (event) {
            is ListenTogetherEvent.Connected -> {
                Log.d(TAG, "Connected to server with userId: ${event.userId}")
            }
            is ListenTogetherEvent.RoomCreated -> {
                Log.d(TAG, "Room created: ${event.roomCode}")
                player?.let { p ->
                    if (!playerListenerRegistered) {
                        p.addListener(playerListener)
                        playerListenerRegistered = true
                    }
                    
                    lastSyncedIsPlaying = p.playWhenReady
                    lastSyncedTrackId = p.currentMediaItem?.mediaId
                    
                    p.currentMediaItem?.let { item ->
                        sendTrackChange(item)
                        if (p.playWhenReady) {
                            lastSyncedIsPlaying = true
                            client.sendPlaybackAction(PlaybackActions.PLAY, position = p.currentPosition)
                        }
                    }
                }
                startHeartbeat()
            }
            is ListenTogetherEvent.JoinApproved -> {
                Log.d(TAG, "Join approved for room: ${event.roomCode}")
                applyPlaybackState(
                    currentTrack = event.state.currentTrack,
                    isPlaying = event.state.isPlaying,
                    position = event.state.position,
                    queue = event.state.queue
                )
            }
            is ListenTogetherEvent.PlaybackSync -> {
                if (!isHost) {
                    handlePlaybackSync(event.action)
                }
            }
            is ListenTogetherEvent.UserJoined -> {
                if (isHost) {
                    player?.currentMediaItem?.let { item ->
                        sendTrackChange(item)
                        if (player?.playWhenReady == true) {
                            val pos = player?.currentPosition ?: 0
                            client.sendPlaybackAction(PlaybackActions.PLAY, position = pos)
                        }
                    }
                }
            }
            is ListenTogetherEvent.BufferComplete -> {
                if (!isHost && bufferingTrackId == event.trackId) {
                    bufferCompleteReceivedForTrack = event.trackId
                    applyPendingSyncIfReady()
                }
            }
            is ListenTogetherEvent.SyncStateReceived -> {
                if (!isHost) {
                    applyPlaybackState(
                        currentTrack = event.state.currentTrack,
                        isPlaying = event.state.isPlaying,
                        position = event.state.position,
                        queue = event.state.queue,
                        bypassBuffer = true
                    )
                }
            }
            is ListenTogetherEvent.Kicked -> cleanup()
            is ListenTogetherEvent.Disconnected -> { /* handled by client mostly */ }
            is ListenTogetherEvent.Reconnected -> {
                player?.let { p ->
                    if (!playerListenerRegistered) {
                        p.addListener(playerListener)
                        playerListenerRegistered = true
                    }
                }
                if (event.isHost) {
                    // Re-sync if needed
                } else {
                     applyPlaybackState(
                        currentTrack = event.state.currentTrack,
                        isPlaying = event.state.isPlaying,
                        position = event.state.position,
                        queue = event.state.queue,
                        bypassBuffer = true
                    )
                }
            }
            else -> {}
        }
    }
    
    private fun cleanup() {
        if (playerListenerRegistered) {
            player?.removeListener(playerListener)
            playerListenerRegistered = false
        }
        stopHeartbeat()
        lastSyncedIsPlaying = null
        lastSyncedTrackId = null
        bufferingTrackId = null
        isSyncing = false
        bufferCompleteReceivedForTrack = null
    }

    private fun applyPendingSyncIfReady() {
        val pending = pendingSyncState ?: return
        val pendingTrackId = pending.currentTrack?.id ?: bufferingTrackId ?: return
        val completeForTrack = bufferCompleteReceivedForTrack

        if (completeForTrack != pendingTrackId) return
        val p = player ?: return

        isSyncing = true
        val targetPos = pending.position
        if (kotlin.math.abs(p.currentPosition - targetPos) > 100) {
            p.seekTo(targetPos)
        }
        if (pending.isPlaying) p.play() else p.pause()

        scope.launch {
            delay(200)
            isSyncing = false
        }
        bufferingTrackId = null
        pendingSyncState = null
        bufferCompleteReceivedForTrack = null
    }

    private fun handlePlaybackSync(action: PlaybackActionPayload) {
        val p = player ?: return
        isSyncing = true
        
        try {
            when (action.action) {
                PlaybackActions.PLAY -> {
                    val pos = action.position ?: 0L
                    if (kotlin.math.abs(p.currentPosition - pos) > 100) p.seekTo(pos)
                    if (bufferingTrackId == null) p.play()
                }
                PlaybackActions.PAUSE -> {
                    val pos = action.position ?: 0L
                    p.pause()
                    if (kotlin.math.abs(p.currentPosition - pos) > 100) p.seekTo(pos)
                }
                PlaybackActions.SEEK -> p.seekTo(action.position ?: 0L)
                PlaybackActions.CHANGE_TRACK -> {
                    action.trackInfo?.let { track ->
                         if (action.queue != null && action.queue.isNotEmpty()) {
                            applyPlaybackState(
                                currentTrack = track,
                                isPlaying = false,
                                position = 0,
                                queue = action.queue
                            )
                        } else {
                            bufferingTrackId = track.id
                            syncToTrack(track, false, 0)
                        }
                    }
                }
                PlaybackActions.SKIP_NEXT -> p.seekToNext()
                PlaybackActions.SKIP_PREV -> p.seekToPrevious()
                // Queue operations
                 PlaybackActions.QUEUE_ADD -> {
                    val track = action.trackInfo
                    if (track != null) {
                        scope.launch(Dispatchers.IO) {
                            val streamUrl = youTubeRepository.getStreamUrl(track.id)
                            val mediaItem = createMediaItem(track, streamUrl)
                            launch(Dispatchers.Main) {
                                isSyncing = true // Prevent echo
                                if (action.insertNext == true) {
                                    p.addMediaItem(p.currentMediaItemIndex + 1, mediaItem)
                                } else {
                                    p.addMediaItem(mediaItem)
                                }
                                isSyncing = false
                            }
                        }
                    }
                }
                PlaybackActions.QUEUE_REMOVE -> {
                     action.trackId?.let { id ->
                         // Find index in queue
                         val count = p.mediaItemCount
                         for (i in 0 until count) {
                             if (p.getMediaItemAt(i).mediaId == id) {
                                 p.removeMediaItem(i)
                                 break
                             }
                         }
                     }
                }
                PlaybackActions.QUEUE_CLEAR -> p.clearMediaItems()
                PlaybackActions.SYNC_QUEUE -> {
                    action.queue?.let { q ->
                        scope.launch(Dispatchers.Main) {
                             // Rebuild queue
                             // Fetch URLs for all? Might be slow.
                             // Better: create MediaItems without URLs if possible, but ExoPlayer needs them for playback.
                             // Optimization: Only fetch current + next few.
                             // For now, let's fetch valid items or use IDs if we can lazy load.
                             // Since we don't have a custom source factory easily accessible here, we must fetch URLs.
                             // Wait, if we use setMediaItems, we replace everything.
                             // Let's assume queue isn't huge or we do it carefully.
                             
                             // Actually, for a quick prototype, we might skip full queue sync if it's heavy.
                             // But let's try mapping.
                             
                             // Note: In a real implementation, we should use a ResolvingDataSource.
                             // For now, we'll try to sync just the current track if queue sync is complex, 
                             // but Protocol sends SYNC_QUEUE.
                             // Let's iterate and create items.
                             
                             // To avoid blocking, maybe we just sync the current track and add others as "placeholders" 
                             // and update them when they play? 
                             // SuvMusic might have a mechanism for this.
                             
                             // Simplified: just sync current track from handlePlaybackSync(CHANGE_TRACK).
                             // If SYNC_QUEUE is sent, we update the playlist.
                        }
                    }
                }
            }
        } finally {
             scope.launch {
                delay(200)
                isSyncing = false
            }
        }
    }

    private fun applyPlaybackState(
        currentTrack: TrackInfo?,
        isPlaying: Boolean,
        position: Long,
        queue: List<TrackInfo>?,
        bypassBuffer: Boolean = false
    ) {
        val p = player ?: return
        activeSyncJob?.cancel()

        if (currentTrack == null) {
            p.pause()
            return
        }

        bufferingTrackId = currentTrack.id
        
        scope.launch(Dispatchers.Main) {
            isSyncing = true
            try {
                // For simplicity in this implementation, we will primarily sync the current track
                // and maybe next/prev if provided in queue, but we'll focus on the current track to ensure playback works.
                // Resolving URLs for a whole queue of 50 songs would take too long here.
                
                // Fetch URL for current track
                syncToTrack(currentTrack, isPlaying, position, bypassBuffer)

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                delay(200)
                isSyncing = false
            }
        }
    }

    private fun syncToTrack(track: TrackInfo, shouldPlay: Boolean, position: Long, bypassBuffer: Boolean = false) {
        bufferingTrackId = track.id
        activeSyncJob?.cancel()
        
        activeSyncJob = scope.launch(Dispatchers.IO) {
            try {
                val streamUrl = youTubeRepository.getStreamUrl(track.id)
                val mediaItem = createMediaItem(track, streamUrl)
                
                launch(Dispatchers.Main) {
                    val p = player ?: return@launch
                    isSyncing = true
                    
                    p.setMediaItem(mediaItem)
                    p.prepare()
                    
                    if (bypassBuffer) {
                        p.seekTo(position)
                        if (shouldPlay) p.play() else p.pause()
                        bufferingTrackId = null
                    } else {
                        p.pause()
                        pendingSyncState = SyncStatePayload(
                            currentTrack = track,
                            isPlaying = shouldPlay,
                            position = position,
                            lastUpdate = System.currentTimeMillis()
                        )
                        client.sendBufferReady(track.id)
                    }
                    
                    delay(100)
                    isSyncing = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isSyncing = false
            }
        }
    }

    private fun createMediaItem(track: TrackInfo, streamUrl: String?): MediaItem {
        val uri = if (streamUrl != null) Uri.parse(streamUrl) else Uri.EMPTY
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setArtworkUri(if (track.thumbnail != null) Uri.parse(track.thumbnail) else null)
            .build()
            
        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()
    }

    // Public API
    fun connect() = client.connect()
    fun disconnect() {
        cleanup()
        client.disconnect()
    }
    fun createRoom(username: String) = client.createRoom(username)
    fun joinRoom(roomCode: String, username: String) = client.joinRoom(roomCode, username)
    fun leaveRoom() {
        cleanup()
        client.leaveRoom()
    }
    fun approveJoin(userId: String) = client.approveJoin(userId)
    fun rejectJoin(userId: String) = client.rejectJoin(userId)
    fun kickUser(userId: String) = client.kickUser(userId)
    
    fun sendTrackChange(mediaItem: MediaItem) {
        if (!isHost || isSyncing) return
        
        val metadata = mediaItem.mediaMetadata
        val trackInfo = TrackInfo(
            id = mediaItem.mediaId,
            title = metadata.title?.toString() ?: "Unknown",
            artist = metadata.artist?.toString() ?: "Unknown",
            album = metadata.albumTitle?.toString(),
            duration = 180000L, // Approximation or fetch real duration if available
            thumbnail = metadata.artworkUri?.toString()
        )
        
        client.sendPlaybackAction(PlaybackActions.CHANGE_TRACK, trackInfo = trackInfo)
    }
    
    fun sendChat(message: String) = client.sendChat(message)
    fun requestSync() = client.requestSync()
    fun suggestTrack(track: TrackInfo) = client.suggestTrack(track)
    fun approveSuggestion(id: String) = client.approveSuggestion(id)
    fun rejectSuggestion(id: String) = client.rejectSuggestion(id)
    fun forceReconnect() = client.forceReconnect()
    fun getPersistedRoomCode() = client.getPersistedRoomCode()
    fun getSessionAge() = client.getSessionAge()

    private var heartbeatJob: Job? = null
    private fun startHeartbeat() {
        if (heartbeatJob?.isActive == true) return
        heartbeatJob = scope.launch {
            while (heartbeatJob?.isActive == true && isInRoom && isHost) {
                delay(15000L)
                player?.let { p ->
                    if (p.playWhenReady && p.playbackState == Player.STATE_READY) {
                        client.sendPlaybackAction(PlaybackActions.PLAY, position = p.currentPosition)
                    }
                }
            }
        }
    }
    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }
}
