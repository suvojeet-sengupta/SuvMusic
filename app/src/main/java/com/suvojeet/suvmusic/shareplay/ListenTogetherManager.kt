package com.suvojeet.suvmusic.shareplay

import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import com.suvojeet.suvmusic.data.repository.RemoteAudioRepository
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
    private val youTubeRepository: YouTubeRepository,
    private val remoteAudioRepository: RemoteAudioRepository,
    private val sessionManager: com.suvojeet.suvmusic.data.SessionManager
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

    // Monotonic token guarding the isSyncing flag. Each "suppress echo" window bumps the
    // token; only the launch that owns the latest token is allowed to clear isSyncing.
    // This fixes the old race where an earlier delay(200) cleared the flag mid-way through
    // a later, still-in-progress sync operation.
    private val syncToken = java.util.concurrent.atomic.AtomicLong(0)

    // Track the last state we synced to avoid duplicate events
    @Volatile
    private var lastSyncedIsPlaying: Boolean? = null
    @Volatile
    private var lastSyncedTrackId: String? = null
    
    // Cache for resolved stream URLs to avoid repeating slow network requests.
    // Entries carry a timestamp because stream URLs (especially YouTube's) expire
    // after a few hours — serving a stale cached URL would fail to play.
    private data class CachedStreamUrl(val url: String, val resolvedAt: Long)
    private val streamUrlCache = android.util.LruCache<String, CachedStreamUrl>(50)
    private val streamUrlTtlMs = 60 * 60 * 1000L // 1 hour
    
    // Track ID being buffered
    @Volatile
    private var bufferingTrackId: String? = null
    @Volatile
    private var pendingBufferReadyTrackId: String? = null
    
    // Track active sync job to cancel it if a better update arrives
    @Volatile
    private var activeSyncJob: Job? = null
    private var previousRole: RoomRole = RoomRole.NONE

    // Drift Correction
    private var anchorTime: Long = 0
    private var anchorPosition: Long = 0
    // Track the anchors were taken against. The drift corrector must not act while the
    // player has moved on to a different track (e.g. auto-advanced to the next queue
    // item) — the wall-clock anchor would extrapolate a huge position and hard-seek the
    // fresh track to its end, which manifested as the song "stopping and looping".
    private var anchorTrackId: String? = null
    private var driftCorrectionJob: Job? = null

    // Pending sync to apply after buffering completes for guest
    @Volatile
    private var pendingSyncState: SyncStatePayload? = null

    // Track if a buffer-complete arrived before the pending sync was ready
    @Volatile
    private var bufferCompleteReceivedForTrack: String? = null

    // Expose client state
    val connectionState = client.connectionState
    val roomState = client.roomState
    val role = client.role
    val userId = client.userId
    val pendingJoinRequests = client.pendingJoinRequests
    val bufferingUsers = client.bufferingUsers
    val logs = client.logs
    val isLogActive = client.isLogActive
    val events = client.events
    val pendingSuggestions = client.pendingSuggestions
    val blockedUsers = client.blockedUsers

    val isInRoom: Boolean get() = client.isInRoom
    val isHost: Boolean get() = client.isHost
    val hasPersistedSession: Boolean get() = client.hasPersistedSession
    
    fun setLogActive(active: Boolean) = client.setLogActive(active)
    fun clearLogs() = client.clearLogs()
    fun blockUser(userId: String) = client.blockUser(userId)
    fun unblockUser(userId: String) = client.unblockUser(userId)
    
    fun getSessionDuration(): Long = client.getSessionDuration()
    
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
            val mediaItem = player?.currentMediaItem
            val trackId = mediaItem?.mediaId
            val trackInfo = mediaItem?.let { getTrackInfo(it) }
            
            if (playWhenReady) {
                Log.d(TAG, "Host sending PLAY at position $position for $trackId")
                client.sendPlaybackAction(PlaybackActions.PLAY, position = position, trackId = trackId, trackInfo = trackInfo)
                lastSyncedIsPlaying = true
            } else if (!playWhenReady && lastSyncedIsPlaying != false) {
                Log.d(TAG, "Host sending PAUSE at position $position for $trackId")
                client.sendPlaybackAction(PlaybackActions.PAUSE, position = position, trackId = trackId, trackInfo = trackInfo)
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
                lastSyncedIsPlaying = true
                val position = player?.currentPosition ?: 0
                client.sendPlaybackAction(PlaybackActions.PLAY, position = position, trackId = trackId)
            }
        }
        
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (isSyncing || !isHost || !isInRoom) return
            
            if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                val trackId = player?.currentMediaItem?.mediaId
                Log.d(TAG, "Host sending SEEK to ${newPosition.positionMs} for $trackId")
                client.sendPlaybackAction(PlaybackActions.SEEK, position = newPosition.positionMs, trackId = trackId)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (!isInRoom || isHost) return
            Log.d(TAG, "Playback state changed: $playbackState")
            if (playbackState == Player.STATE_READY) {
                val trackIdToSend = pendingBufferReadyTrackId
                val currentMediaId = player?.currentMediaItem?.mediaId
                if (trackIdToSend != null && currentMediaId == trackIdToSend) {
                    pendingBufferReadyTrackId = null
                    Log.d(TAG, "Player is STATE_READY for $trackIdToSend, sending buffer ready")
                    client.sendBufferReady(trackIdToSend)
                }
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
                val oldRole = previousRole
                previousRole = newRole
                if (player != null) {
                    if (newRole != RoomRole.NONE) {
                        if (!playerListenerRegistered) {
                            player?.addListener(playerListener)
                            playerListenerRegistered = true
                            Log.d(TAG, "Role changed to $newRole, added player listener")
                        }
                    } else {
                        if (playerListenerRegistered) {
                            player?.removeListener(playerListener)
                            playerListenerRegistered = false
                            Log.d(TAG, "Role changed to NONE, removed player listener")
                        }
                    }
                }
                
                if (newRole == RoomRole.HOST && oldRole != RoomRole.HOST && player != null) {
                    Log.d(TAG, "Role changed to HOST, starting sync services")
                    startHeartbeat()
                    stopDriftCorrection()
                } else if (newRole != RoomRole.HOST && oldRole == RoomRole.HOST) {
                    Log.d(TAG, "Role changed from HOST, stopping sync services")
                    stopHeartbeat()
                }
            }
        }
        
        // Monitor Incognito Mode
        scope.launch {
            sessionManager.incognitoModeEnabledFlow.collect { incognitoEnabled ->
                if (incognitoEnabled && isInRoom) {
                    Log.i(TAG, "Incognito Mode enabled, leaving Listen Together room.")
                    leaveRoom()
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
                            client.sendPlaybackAction(PlaybackActions.PLAY, position = p.currentPosition, trackId = p.currentMediaItem?.mediaId)
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
                        // Don't send separate CHANGE_TRACK — the guest already gets room state
                        // from JoinApproved. Only send FORCE_SYNC so the server creates ONE
                        // buffer coordination flow. Send it regardless of play/pause state so
                        // the guest always syncs to the host's current track + position.
                        val pos = player?.currentPosition ?: 0
                        val trackInfo = getTrackInfo(item)
                        client.sendPlaybackAction(
                            PlaybackActions.FORCE_SYNC,
                            position = pos,
                            trackId = item.mediaId,
                            trackInfo = trackInfo
                        )
                    }
                }
            }
            is ListenTogetherEvent.BufferWait -> {
                isSyncing = true
                player?.pause()
                if (!isHost) {
                    stopDriftCorrection()
                    // Setup state to resume playing after buffer is complete
                    if (pendingSyncState == null) {
                        bufferingTrackId = event.trackId
                        pendingSyncState = SyncStatePayload(
                            currentTrack = null,
                            isPlaying = true,
                            position = player?.currentPosition ?: 0L,
                            lastUpdate = System.currentTimeMillis(),
                            queue = emptyList(),
                            volume = 1f
                        )
                    }
                }
            }
            is ListenTogetherEvent.BufferComplete -> {
                if (isHost) {
                    // Host resumes automatically once all guests are ready
                    player?.play()
                    
                    // Since isSyncing is true, our listener ignores the play event.
                    // We MUST manually tell the server we are playing so it updates room.is_playing!
                    val pos = player?.currentPosition ?: 0
                    val trackId = player?.currentMediaItem?.mediaId
                    client.sendPlaybackAction(PlaybackActions.PLAY, position = pos, trackId = trackId)
                    lastSyncedIsPlaying = true
                    
                    scope.launch {
                        delay(200)
                        isSyncing = false
                    }
                } else {
                    // Guest: try the pending sync flow first
                    bufferCompleteReceivedForTrack = event.trackId
                    if (pendingSyncState != null && (bufferingTrackId == event.trackId)) {
                        applyPendingSyncIfReady()
                    } else if (bufferingTrackId == event.trackId || bufferingTrackId == null) {
                        // Track is already loaded (e.g., from JoinApproved flow). Just play.
                        Log.d(TAG, "BufferComplete: no pending sync, resuming playback for ${event.trackId}")
                        isSyncing = true
                        player?.let { p ->
                            anchorPosition = p.currentPosition
                            anchorTime = System.currentTimeMillis()
                            anchorTrackId = p.currentMediaItem?.mediaId
                            p.play()
                            startDriftCorrectionJob()
                        }
                        scope.launch {
                            delay(200)
                            isSyncing = false
                        }
                        bufferingTrackId = null
                        pendingSyncState = null
                        bufferCompleteReceivedForTrack = null
                    }
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
                    player?.currentMediaItem?.let { item ->
                        sendTrackChange(item)
                        if (player?.playWhenReady == true) {
                            val pos = player?.currentPosition ?: 0
                            client.sendPlaybackAction(PlaybackActions.PLAY, position = pos, trackId = item.mediaId)
                        }
                    }
                } else {
                      applyPlaybackState(
                        currentTrack = event.state.currentTrack,
                        isPlaying = event.state.isPlaying,
                        position = event.state.position,
                        queue = event.state.queue,
                        bypassBuffer = true
                    )
                    client.requestSync()
                }
            }
            else -> {}
        }
    }
    
    /**
     * Clear [isSyncing] after [delayMs], but only if no newer suppress-echo window has
     * started in the meantime. Prevents overlapping sync operations from clearing the
     * flag out from under each other.
     */
    private fun endSyncAfter(delayMs: Long = 200) {
        val my = syncToken.incrementAndGet()
        scope.launch {
            delay(delayMs)
            if (syncToken.get() == my) isSyncing = false
        }
    }

    private fun cleanup() {
        if (playerListenerRegistered) {
            player?.removeListener(playerListener)
            playerListenerRegistered = false
        }
        stopHeartbeat()
        stopDriftCorrection()
        lastSyncedIsPlaying = null
        lastSyncedTrackId = null
        bufferingTrackId = null
        pendingBufferReadyTrackId = null
        isSyncing = false
        bufferCompleteReceivedForTrack = null
    }

    private fun startDriftCorrectionJob() {
        driftCorrectionJob?.cancel()
        driftCorrectionJob = scope.launch {
            while (true) {
                delay(500) // Check every 500ms
                val p = player ?: continue
                if (!p.isPlaying) continue
                // Only correct while the player is actually ready. Seeking during a
                // buffer/stall creates a feedback loop: the wall-clock keeps advancing
                // while the player is stuck, the gap grows past the hard-seek threshold,
                // and we keep re-seeking the same spot.
                if (p.playbackState != Player.STATE_READY) continue
                // Don't correct a track the anchors weren't taken against — wait for the
                // host's next sync to re-anchor on the new track first.
                if (anchorTrackId != null && p.currentMediaItem?.mediaId != anchorTrackId) continue

                val rawExpected = anchorPosition + (System.currentTimeMillis() - anchorTime)
                // Never extrapolate past the end of the track. Without this clamp the
                // wall-clock position keeps growing beyond the song length and the hard
                // seek below jumps to/past the end, ending the track (and, with repeat on,
                // looping it) — the "stops at X:XX and starts looping" report.
                val duration = p.duration
                val expectedPosition = if (duration != androidx.media3.common.C.TIME_UNSET && duration > 0) {
                    rawExpected.coerceIn(0L, duration)
                } else {
                    rawExpected.coerceAtLeast(0L)
                }
                val currentPosition = p.currentPosition
                val drift = currentPosition - expectedPosition

                // Drift thresholds
                when {
                    kotlin.math.abs(drift) > 1500 -> {
                        // Large drift: silently hard-seek THIS guest back into sync. We
                        // deliberately do NOT broadcast FORCE_SYNC here: that paused and
                        // re-buffered the ENTIRE room whenever any single guest drifted,
                        // so one guest on a slow connection could make everyone stutter.
                        // Correct only ourselves.
                        Log.w(TAG, "Large drift ($drift ms), self-correcting with local seek")
                        if (p.playbackParameters.speed != 1.0f) {
                            p.playbackParameters = androidx.media3.common.PlaybackParameters(1.0f, 1.0f)
                        }
                        p.seekTo(expectedPosition)
                        anchorPosition = expectedPosition
                        anchorTime = System.currentTimeMillis()
                    }
                    drift < -150 -> {
                        // Behind by > 150ms: Speed up slightly (1.05x). Pitch pinned to 1.0
                        // so the time-stretch does not audibly raise the pitch.
                         p.playbackParameters = androidx.media3.common.PlaybackParameters(1.05f, 1.0f)
                    }
                    drift > 150 -> {
                         // Ahead by > 150ms: Slow down slightly (0.95x), pitch preserved.
                         p.playbackParameters = androidx.media3.common.PlaybackParameters(0.95f, 1.0f)
                    }
                    else -> {
                        // Within 150ms: Sync is good, reset to normal speed
                         if (p.playbackParameters.speed != 1.0f) {
                             p.playbackParameters = androidx.media3.common.PlaybackParameters(1.0f, 1.0f)
                         }
                    }
                }
            }
        }
    }

    private fun stopDriftCorrection() {
        driftCorrectionJob?.cancel()
        driftCorrectionJob = null
        anchorTrackId = null
        player?.playbackParameters = androidx.media3.common.PlaybackParameters(1.0f)
    }

    private fun applyPendingSyncIfReady() {
        val pending = pendingSyncState ?: return
        val pendingTrackId = pending.currentTrack?.id ?: bufferingTrackId ?: return
        val completeForTrack = bufferCompleteReceivedForTrack

        if (completeForTrack != pendingTrackId) return
        val p = player ?: return

        isSyncing = true
        val targetPos = pending.position
        
        // Initialize anchors for drift correction upon starting playback
        anchorPosition = targetPos
        anchorTime = System.currentTimeMillis()
        anchorTrackId = p.currentMediaItem?.mediaId

        if (kotlin.math.abs(p.currentPosition - targetPos) > 1000) {
            p.seekTo(targetPos)
        }
        
        if (pending.isPlaying) {
            p.play()
            startDriftCorrectionJob()
        } else {
            p.pause()
            stopDriftCorrection()
        }

        endSyncAfter(200)
        bufferingTrackId = null
        pendingSyncState = null
        bufferCompleteReceivedForTrack = null
    }

    private fun handlePlaybackSync(action: PlaybackActionPayload) {
        val p = player ?: return

        // Track checking: If action has a trackId, ensure we are on it
        // Treat empty string same as null (protobuf default for string is "")
        val targetTrackId = action.trackId?.takeIf { it.isNotEmpty() }
        val currentTrackId = p.currentMediaItem?.mediaId?.takeIf { it.isNotEmpty() }
        
        // If we have a target track but guest has nothing loaded, or it's a different track
        if (targetTrackId != null && targetTrackId != currentTrackId) {
            // If this track is already being buffered (e.g., from JoinApproved flow that started
            // syncToTrack but hasn't finished loading the stream yet), DON'T start a new syncToTrack.
            // That would cancel the in-progress one and skip buffer_ready, leaving the server stuck.
            if (bufferingTrackId == targetTrackId) {
                Log.d(TAG, "Track $targetTrackId already being buffered, skipping mismatch handling for ${action.action}")
                // For FORCE_SYNC, just update the position we'll seek to when ready
                if (action.action == PlaybackActions.FORCE_SYNC) {
                    val pos = action.position ?: 0L
                    pendingSyncState = pendingSyncState?.copy(position = pos) ?: SyncStatePayload(
                        currentTrack = action.trackInfo,
                        isPlaying = true,
                        position = pos,
                        lastUpdate = System.currentTimeMillis()
                    )
                }
                return
            }
            Log.d(TAG, "Guest track mismatch: current=$currentTrackId, target=$targetTrackId. Switching...")
            action.trackInfo?.let { track ->
                syncToTrack(track, action.action == PlaybackActions.PLAY, action.position ?: 0L, bypassBuffer = true)
            } ?: run {
                // If trackInfo missing but we have ID, try to resolve it
                Log.d(TAG, "Track info missing, resolving from ID: $targetTrackId")
                val placeholderTrack = TrackInfo(
                    id = targetTrackId,
                    title = "Loading...",
                    artist = "Please wait...",
                    duration = 0L
                )
                syncToTrack(placeholderTrack, action.action == PlaybackActions.PLAY, action.position ?: 0L, bypassBuffer = true)
            }
            return
        }
        
        // Defensive: if no track is loaded at all but we got play/force_sync with trackInfo, load it
        if (currentTrackId == null && action.trackInfo != null && 
            action.action in listOf(PlaybackActions.PLAY, PlaybackActions.FORCE_SYNC, PlaybackActions.CHANGE_TRACK)) {
            Log.d(TAG, "No track loaded on guest, loading from trackInfo: ${action.trackInfo.title}")
            syncToTrack(action.trackInfo, action.action == PlaybackActions.PLAY, action.position ?: 0L, bypassBuffer = true)
            return
        }

        isSyncing = true
        
        try {
            when (action.action) {
                PlaybackActions.PLAY -> {
                    // Server-time based position adjustment using raw clocks is inaccurate
                    // due to clock desync between host and guest. We just use the base position
                    // and a tiny estimated latency (50ms) to ensure we stay tightly synced.
                    val basePos = action.position ?: 0L
                    val targetPos = basePos + 50L
                    
                    // Update anchors
                    anchorPosition = targetPos
                    anchorTime = System.currentTimeMillis()
                    anchorTrackId = action.trackId ?: p.currentMediaItem?.mediaId

                    if (kotlin.math.abs(p.currentPosition - targetPos) > 1000) p.seekTo(targetPos)
                    
                    // FORCE PLAY even if buffering was "active" logic-wise
                    // We trust the host's command. If we aren't actually ready, ExoPlayer will buffer anyway.
                    p.play()
                    startDriftCorrectionJob()
                    
                    // Clear buffering state so we don't get stuck
                    bufferingTrackId = null
                    pendingSyncState = null
                }
                PlaybackActions.PAUSE -> {
                    val pos = action.position ?: 0L
                    p.pause()
                    stopDriftCorrection()
                    
                    if (kotlin.math.abs(p.currentPosition - pos) > 1000) p.seekTo(pos)
                }
                PlaybackActions.SEEK -> {
                    val pos = action.position ?: 0L
                    // Update anchors for seek too, assuming we will continue playing
                    if (p.isPlaying) {
                        anchorPosition = pos
                        anchorTime = System.currentTimeMillis()
                        anchorTrackId = p.currentMediaItem?.mediaId
                    }
                    p.seekTo(pos)
                }
                PlaybackActions.CHANGE_TRACK -> {
                    stopDriftCorrection()
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
                            val streamResult = youTubeRepository.getStreamUrlForDownload(track.id)
                            if (streamResult != null) {
                                val (streamUrl, _) = streamResult
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
                PlaybackActions.SET_VOLUME -> {
                    // Volume sync handled at UI level through room state
                }
                PlaybackActions.SYNC_QUEUE -> {
                    // Logic remains same or implemented later
                }
                PlaybackActions.FORCE_SYNC -> {
                    // Host has initiated a global sync pause. Pause playback, seek to
                    // the host's position, and prepare for the BufferWait/BufferComplete
                    // flow that will follow.
                    val pos = action.position ?: 0L
                    Log.d(TAG, "Guest received FORCE_SYNC: pausing and seeking to $pos for ${action.trackId}")
                    stopDriftCorrection()
                    p.pause()
                    if (kotlin.math.abs(p.currentPosition - pos) > 1000) {
                        p.seekTo(pos)
                    }
                    // The BufferWait event (which arrives separately) will set up 
                    // pendingSyncState if needed. We just pause and position here.
                    bufferingTrackId = action.trackId
                }
            }
        } finally {
            endSyncAfter(200)
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
                syncToTrack(currentTrack, isPlaying, position, bypassBuffer, queue)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                endSyncAfter(200)
            }
        }
    }

    private fun syncToTrack(
        track: TrackInfo, 
        shouldPlay: Boolean, 
        position: Long, 
        bypassBuffer: Boolean = false,
        queue: List<TrackInfo>? = null
    ) {
        bufferingTrackId = track.id
        pendingBufferReadyTrackId = null
        activeSyncJob?.cancel()
        
        activeSyncJob = scope.launch(Dispatchers.IO) {
            try {
                // 1. Determine track source based on ID characteristics (YouTube IDs are always 11 chars)
                val isYouTube = track.id.length == 11 && track.id.matches(Regex("[a-zA-Z0-9_-]{11}"))
                
                // 2. Resolve stream URL from cache (if not expired) or direct fetch
                val cached = streamUrlCache.get(track.id)
                val cachedUrl = cached?.takeIf {
                    System.currentTimeMillis() - it.resolvedAt < streamUrlTtlMs
                }?.url
                val streamUrl = if (cachedUrl != null) {
                    cachedUrl
                } else {
                    val resolved = if (isYouTube) {
                        youTubeRepository.getStreamUrl(track.id)
                            ?: youTubeRepository.getStreamUrlForDownload(track.id)?.first
                            ?: remoteAudioRepository.getStreamUrl(track.id)
                    } else {
                        remoteAudioRepository.getStreamUrl(track.id)
                            ?: youTubeRepository.getStreamUrl(track.id)
                            ?: youTubeRepository.getStreamUrlForDownload(track.id)?.first
                    }
                    if (resolved != null) {
                        streamUrlCache.put(track.id, CachedStreamUrl(resolved, System.currentTimeMillis()))
                    }
                    resolved
                }

                if (streamUrl == null) {
                    Log.e(TAG, "Failed to resolve stream URL for ${track.id}")
                    // Surface the failure to the user instead of silently leaving the
                    // guest stuck on a "Loading…" placeholder, and clear buffer/sync
                    // state so the room isn't held waiting on a track we can't play.
                    launch(Dispatchers.Main) {
                        val title = track.title.takeIf { it.isNotBlank() && it != "Loading..." } ?: "this track"
                        com.suvojeet.suvmusic.util.SnackbarUtil.showError(
                            "Couldn't load \"$title\" to sync with the room"
                        )
                        if (bufferingTrackId == track.id) bufferingTrackId = null
                        pendingBufferReadyTrackId = null
                        isSyncing = false
                    }
                    return@launch
                }

                // 3. Create MediaItem using the available track info metadata (no need for slow details endpoint)
                val currentMediaItem = createMediaItem(track, streamUrl)
                
                launch(Dispatchers.Main) {
                    val p = player ?: return@launch
                    isSyncing = true

                    // A guest's player only ever needs the single track the host is
                    // currently playing. We deliberately do NOT load the rest of the
                    // queue as placeholder MediaItems with empty URIs: if ExoPlayer
                    // auto-advanced into one of those it would hit Uri.EMPTY and error
                    // out. The shared queue is shown in the UI from the room state, and
                    // when the host changes track we receive a fresh CHANGE_TRACK and
                    // load that single track here.
                    p.setMediaItem(currentMediaItem)
                    p.prepare()
                    p.seekTo(position)

                    if (bypassBuffer) {
                        // CRITICAL FIX: We must anchor drift correction when switching tracks!
                        anchorPosition = position
                        anchorTime = System.currentTimeMillis()
                        anchorTrackId = track.id
                        
                        if (shouldPlay) {
                            p.play()
                            startDriftCorrectionJob()
                        } else {
                            p.pause()
                            stopDriftCorrection()
                        }
                        bufferingTrackId = null
                    } else {
                        // Standard sync: pause, wait for STATE_READY to send ready
                        p.pause()
                        
                        pendingSyncState = SyncStatePayload(
                            currentTrack = track,
                            isPlaying = shouldPlay,
                            position = position,
                            lastUpdate = System.currentTimeMillis()
                        )
                        
                        pendingBufferReadyTrackId = track.id
                        if (p.playbackState == Player.STATE_READY && p.currentMediaItem?.mediaId == track.id) {
                            pendingBufferReadyTrackId = null
                            Log.d(TAG, "Player already STATE_READY for ${track.id}, sending buffer ready immediately")
                            client.sendBufferReady(track.id)
                        }
                        
                        // Add a timeout fallback for buffering
                        scope.launch {
                            delay(5000) // 5 seconds timeout
                            if (bufferingTrackId == track.id && pendingSyncState != null) {
                                Log.w(TAG, "Buffer timeout for ${track.id}, forcing play")
                                if (pendingBufferReadyTrackId == track.id) {
                                    pendingBufferReadyTrackId = null
                                    client.sendBufferReady(track.id)
                                }
                                bufferCompleteReceivedForTrack = track.id
                                applyPendingSyncIfReady()
                            }
                        }
                    }
                    
                    endSyncAfter(100)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                launch(Dispatchers.Main) { isSyncing = false }
            }
        }
    }

    private fun createMediaItemFromSong(song: Song, streamUrl: String): MediaItem {
        val uri = Uri.parse(streamUrl)
        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .setArtworkUri(if (song.thumbnailUrl != null) Uri.parse(song.thumbnailUrl) else null)
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .build()
            
        return MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(uri)
            .setMediaMetadata(metadata)
            // Set custom cache key to match MusicPlayer's logic for consistent caching
            .setCustomCacheKey(song.id)
            .build()
    }

    private fun createMediaItem(track: TrackInfo, streamUrl: String?): MediaItem {
        val uri = if (streamUrl != null) Uri.parse(streamUrl) else Uri.EMPTY
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setArtworkUri(if (track.thumbnail != null) Uri.parse(track.thumbnail) else null)
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
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
    fun createRoom(username: String) {
        scope.launch {
            if (sessionManager.isIncognitoModeEnabled()) {
                Log.w(TAG, "Cannot create room in Incognito Mode")
                return@launch
            }
            client.createRoom(username)
        }
    }
    
    fun joinRoom(roomCode: String, username: String) {
         scope.launch {
            if (sessionManager.isIncognitoModeEnabled()) {
                Log.w(TAG, "Cannot join room in Incognito Mode")
                return@launch
            }
            client.joinRoom(roomCode, username)
        }
    }
    fun leaveRoom() {
        cleanup()
        client.leaveRoom()
    }
    fun approveJoin(userId: String) = client.approveJoin(userId)
    fun rejectJoin(userId: String) = client.rejectJoin(userId)
    fun kickUser(userId: String) = client.kickUser(userId)
    fun transferHost(newHostId: String) = client.transferHost(newHostId)
    
    fun sendTrackChange(mediaItem: MediaItem) {
        if (!isHost || isSyncing) return
        
        val trackInfo = getTrackInfo(mediaItem)
        
        client.sendPlaybackAction(PlaybackActions.CHANGE_TRACK, trackId = mediaItem.mediaId, trackInfo = trackInfo)
    }
    
    private fun getTrackInfo(mediaItem: MediaItem): TrackInfo {
        val metadata = mediaItem.mediaMetadata
        // Use the player's real duration when this is the currently loaded item and the
        // duration is known; otherwise 0 (unknown) instead of a fake fixed 3:00.
        val realDuration = player?.let { p ->
            if (p.currentMediaItem?.mediaId == mediaItem.mediaId) {
                val d = p.duration
                if (d != androidx.media3.common.C.TIME_UNSET && d > 0) d else 0L
            } else 0L
        } ?: 0L
        return TrackInfo(
            id = mediaItem.mediaId,
            title = metadata.title?.toString() ?: "Unknown",
            artist = metadata.artist?.toString() ?: "Unknown",
            album = metadata.albumTitle?.toString(),
            duration = realDuration,
            thumbnail = metadata.artworkUri?.toString()
        )
    }
    
    fun requestSync() = client.requestSync()
    fun suggestTrack(track: TrackInfo) = client.suggestTrack(track)
    fun approveSuggestion(id: String) {
        // The host must enqueue the approved track into its own player: the server only
        // broadcasts queue_add to guests, and the host never processes its own sync echo.
        // (Previously approving a suggestion did nothing on either side.)
        if (isHost) {
            val track = pendingSuggestions.value.firstOrNull { it.suggestionId == id }?.trackInfo
            if (track != null) {
                scope.launch(Dispatchers.IO) {
                    val streamResult = youTubeRepository.getStreamUrlForDownload(track.id)
                    if (streamResult != null) {
                        val (streamUrl, _) = streamResult
                        val mediaItem = createMediaItem(track, streamUrl)
                        launch(Dispatchers.Main) {
                            isSyncing = true
                            player?.addMediaItem(mediaItem)
                            endSyncAfter(200)
                        }
                    }
                }
            }
        }
        client.approveSuggestion(id)
    }
    fun rejectSuggestion(id: String) = client.rejectSuggestion(id)
    fun forceReconnect() = client.forceReconnect()
    fun getPersistedRoomCode() = client.getPersistedRoomCode()
    fun getSessionAge() = client.getSessionAge()
    
    suspend fun getSavedUsername() = client.getSavedUsername()
    suspend fun saveUsername(username: String) = client.saveUsername(username)

    suspend fun getServerUrl() = client.getServerUrl()
    suspend fun setServerUrl(url: String) = client.setServerUrl(url)
    
    suspend fun getAutoApproval() = client.getAutoApproval()
    suspend fun setAutoApproval(enabled: Boolean) = client.setAutoApproval(enabled)
    
    suspend fun getSyncVolume() = client.getSyncVolume()
    suspend fun setSyncVolume(enabled: Boolean) = client.setSyncVolume(enabled)
    
    suspend fun getMuteHost() = client.getMuteHost()
    suspend fun setMuteHost(enabled: Boolean) = client.setMuteHost(enabled)

    private var heartbeatJob: Job? = null
    private fun startHeartbeat() {
        if (heartbeatJob?.isActive == true) return
        heartbeatJob = scope.launch {
            while (heartbeatJob?.isActive == true && isInRoom && isHost) {
                delay(15000L)
                player?.let { p ->
                    if (p.playWhenReady && p.playbackState == Player.STATE_READY) {
                        val trackId = p.currentMediaItem?.mediaId
                        val trackInfo = p.currentMediaItem?.let { getTrackInfo(it) }
                        client.sendPlaybackAction(PlaybackActions.PLAY, position = p.currentPosition, trackId = trackId, trackInfo = trackInfo)
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
