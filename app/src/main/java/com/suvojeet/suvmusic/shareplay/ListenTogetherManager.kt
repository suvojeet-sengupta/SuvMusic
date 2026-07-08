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

        // --- HQ (RemoteAudio) staggered resolution across the room ---
        // The RemoteAudio backend rate-limits hard and shares one backoff gate, so N
        // guests resolving the same HQ track at once = an instant 429 storm. Each guest
        // waits (its slot * STAGGER_MS) before hitting the backend, fanning the requests
        // out. The cap stays under the 5s buffer timeout so a staggered guest never
        // misses its buffer-ready window.
        private const val STAGGER_MS = 400L
        private const val STAGGER_MAX_MS = 3200L
        private const val STAGGER_SLOTS = 8
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

    // Track a syncToTrack load that is genuinely in flight (stream resolving /
    // MediaItem loading). This is DISTINCT from bufferingTrackId, which BufferWait
    // also sets before any load starts. The track-mismatch guard must key off THIS
    // field — keying off bufferingTrackId made a BufferWait (which arrives before
    // change_track) permanently block the change_track from ever loading the new
    // track, stranding the guest on the previous song until the server's 12s
    // buffer timeout.
    @Volatile
    private var loadingTrackId: String? = null

    // Synchronous mirror of client.exactSyncEnabled for the hot playback-sync path.
    @Volatile
    private var exactSyncOn: Boolean = true
    
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
    /** Live server stats over the WebSocket (PONG keep-alive). */
    val serverStats = client.serverStats

    val isInRoom: Boolean get() = client.isInRoom
    val isHost: Boolean get() = client.isHost
    val hasPersistedSession: Boolean get() = client.hasPersistedSession

    // --- Jam permissions (Spotify Jam parity) ---
    /** True when this user may add tracks to the shared queue. */
    val canQueue: Boolean get() = client.canQueue
    /** True when this user may control playback (play/pause/seek/skip/change track). */
    val canControlPlayback: Boolean get() = client.canControlPlayback
    /** Host-only: change the room's Jam permissions. */
    fun setRoomSettings(settings: RoomSettings) = client.setRoomSettings(settings)

    /** Whether strict buffer coordination is used on track change. */
    val exactSyncEnabled: kotlinx.coroutines.flow.StateFlow<Boolean> get() = client.exactSyncEnabled
    /** Toggle exact sync. Off = faster song switches, looser cross-device sync. */
    fun setExactSync(enabled: Boolean) = client.setExactSync(enabled)

    /**
     * Whether this device should broadcast its local player actions to the room:
     * the host always does; a guest only when the host allowed guest control.
     */
    private val broadcastsControl: Boolean get() = isInRoom && client.canControlPlayback
    
    fun setLogActive(active: Boolean) = client.setLogActive(active)
    fun clearLogs() = client.clearLogs()
    fun blockUser(userId: String) = client.blockUser(userId)
    fun unblockUser(userId: String) = client.unblockUser(userId)
    
    fun getSessionDuration(): Long = client.getSessionDuration()
    
    private val playerListener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (isSyncing || !isInRoom || !broadcastsControl) return
            
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
            if (isSyncing || !isInRoom || !broadcastsControl) return
            if (mediaItem == null) return

            // A controlling guest only broadcasts deliberate track changes (playing a
            // new song locally). Auto-advance/repeat transitions are the HOST's to
            // announce — a guest echoing them would start a duplicate buffer
            // coordination for a change the host is already broadcasting.
            if (!isHost &&
                (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                 reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT)) {
                return
            }

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
            if (isSyncing || !isInRoom || !broadcastsControl) return
            
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
        
        // Keep the synchronous exact-sync mirror current.
        scope.launch {
            client.exactSyncEnabled.collect { exactSyncOn = it }
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
                } else {
                    // Jam mode: a guest with permission acted — the server validated it
                    // and relayed it here. Apply it to the host player, which is the
                    // room's source of truth.
                    handleGuestActionAsHost(event.action)
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
            is ListenTogetherEvent.BufferWait -> if (!exactSyncOn) {
                // Fast mode: never freeze the room waiting for every member to buffer.
                // The host keeps playing; a guest just pauses briefly to cut the old
                // track and the following change_track load (bypassBuffer) resumes the
                // new one the instant this device's own stream is ready. The server's
                // buffer coordination still runs but times out harmlessly, unwaited-on.
                if (!isHost) {
                    isSyncing = true
                    stopDriftCorrection()
                    player?.pause()
                    endSyncAfter(300)
                }
            } else {
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
                    // If we're already on the awaited track we never go through
                    // syncToTrack (e.g. WE initiated this change as a controlling
                    // guest), so report readiness here or the room waits on us
                    // until the server's buffer timeout.
                    val p = player
                    if (p?.currentMediaItem?.mediaId == event.trackId &&
                        pendingBufferReadyTrackId != event.trackId) {
                        if (p.playbackState == Player.STATE_READY) {
                            client.sendBufferReady(event.trackId)
                        } else {
                            pendingBufferReadyTrackId = event.trackId
                        }
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
        loadingTrackId = null
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

        // Queue edits reference the queued track, NOT the currently playing one — the
        // track-mismatch handling below must never run for them, or a guest would
        // switch playback to every track that gets added to the shared queue.
        val isQueueOp = action.action in listOf(
            PlaybackActions.QUEUE_ADD, PlaybackActions.QUEUE_REMOVE,
            PlaybackActions.QUEUE_CLEAR, PlaybackActions.SYNC_QUEUE
        )

        // Track checking: If action has a trackId, ensure we are on it
        // Treat empty string same as null (protobuf default for string is "")
        val targetTrackId = action.trackId?.takeIf { it.isNotEmpty() }
        val currentTrackId = p.currentMediaItem?.mediaId?.takeIf { it.isNotEmpty() }

        // If we have a target track but guest has nothing loaded, or it's a different track
        if (!isQueueOp && targetTrackId != null && targetTrackId != currentTrackId) {
            // Only bail if a syncToTrack load for this exact track is genuinely in
            // flight — otherwise we'd cancel the in-progress load and skip buffer_ready.
            // We key off loadingTrackId, NOT bufferingTrackId: BufferWait sets
            // bufferingTrackId before any load starts, and keying off it here made the
            // guest permanently skip loading the new track (stuck on the old song until
            // the server's 12s buffer timeout).
            if (loadingTrackId == targetTrackId) {
                Log.d(TAG, "Track $targetTrackId already loading, skipping mismatch handling for ${action.action}")
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
            // A CHANGE_TRACK under exact sync must go through buffer coordination
            // (bypassBuffer=false) so we send buffer_ready the instant our stream is
            // ready and the room resumes together — instead of the host waiting out the
            // full 12s timeout. In fast mode (exact sync off), or for a PLAY/SEEK
            // catch-up, load and play immediately.
            val isChange = action.action == PlaybackActions.CHANGE_TRACK
            val bypass = !(isChange && exactSyncOn)
            // Whether to start playing once loaded. In exact mode a change loads
            // paused and the host's post-BufferComplete PLAY resumes everyone. In
            // fast mode there is no BufferComplete step and the host's initial PLAY
            // may arrive while we're still resolving the stream (then dropped by the
            // guard above), so a change must play on load or the guest would stall
            // paused until the next heartbeat. A genuine paused-change is corrected
            // by the host's following PAUSE.
            val shouldPlay = action.action == PlaybackActions.PLAY || (isChange && !exactSyncOn)
            action.trackInfo?.let { track ->
                syncToTrack(track, shouldPlay, action.position ?: 0L, bypassBuffer = bypass)
            } ?: run {
                // If trackInfo missing but we have ID, try to resolve it
                Log.d(TAG, "Track info missing, resolving from ID: $targetTrackId")
                val placeholderTrack = TrackInfo(
                    id = targetTrackId,
                    title = "Loading...",
                    artist = "Please wait...",
                    duration = 0L
                )
                syncToTrack(placeholderTrack, shouldPlay, action.position ?: 0L, bypassBuffer = bypass)
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
                            // Resolve from the same backend + quality as the host so HQ
                            // tracks queue correctly (plain YouTube lookup can't serve them).
                            val streamUrl = resolveStreamUrlForRoom(track)
                            if (streamUrl != null) {
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

    /**
     * Apply a guest-originated playback action on the HOST player (Jam mode). The
     * server already validated the guest's permission and updated the room state;
     * the host player is the room's source of truth, so it must follow.
     *
     * Play/pause/seek/queue edits are applied with echo suppression — the server
     * already broadcast the action to the other guests. Skips are applied WITHOUT
     * suppression so the resulting track transition re-broadcasts exactly as if
     * the host had skipped locally.
     */
    private fun handleGuestActionAsHost(action: PlaybackActionPayload) {
        val p = player ?: return
        when (action.action) {
            PlaybackActions.PLAY -> {
                isSyncing = true
                val pos = action.position
                if (pos != null && kotlin.math.abs(p.currentPosition - pos) > 1000) p.seekTo(pos)
                p.play()
                lastSyncedIsPlaying = true
                endSyncAfter(200)
            }
            PlaybackActions.PAUSE -> {
                isSyncing = true
                p.pause()
                val pos = action.position
                if (pos != null && kotlin.math.abs(p.currentPosition - pos) > 1000) p.seekTo(pos)
                lastSyncedIsPlaying = false
                endSyncAfter(200)
            }
            PlaybackActions.SEEK -> {
                isSyncing = true
                action.position?.let { p.seekTo(it) }
                endSyncAfter(200)
            }
            PlaybackActions.SKIP_NEXT -> p.seekToNext()
            PlaybackActions.SKIP_PREV -> p.seekToPrevious()
            PlaybackActions.CHANGE_TRACK -> {
                val track = action.trackInfo ?: return
                if (p.currentMediaItem?.mediaId == track.id) return
                // The server has already set the room's current track and started
                // buffer coordination from the guest's message, so the load here is
                // suppressed (no re-broadcast). BUFFER_COMPLETE resumes everyone.
                lastSyncedTrackId = track.id
                lastSyncedIsPlaying = false
                var index = -1
                for (i in 0 until p.mediaItemCount) {
                    if (p.getMediaItemAt(i).mediaId == track.id) { index = i; break }
                }
                if (index >= 0) {
                    isSyncing = true
                    p.seekTo(index, 0)
                    p.pause()
                    endSyncAfter(300)
                } else {
                    scope.launch(Dispatchers.IO) {
                        val streamUrl = resolveStreamUrlForRoom(track)
                        if (streamUrl == null) {
                            Log.e(TAG, "Host failed to resolve guest-requested track ${track.id}")
                            return@launch
                        }
                        val mediaItem = createMediaItem(track, streamUrl)
                        launch(Dispatchers.Main) {
                            isSyncing = true
                            // Insert next and jump so the host's queue survives the detour.
                            val insertAt = (p.currentMediaItemIndex + 1).coerceAtMost(p.mediaItemCount)
                            p.addMediaItem(insertAt, mediaItem)
                            p.seekTo(insertAt, 0)
                            p.pause()
                            endSyncAfter(300)
                        }
                    }
                }
            }
            PlaybackActions.QUEUE_ADD -> {
                val track = action.trackInfo ?: return
                scope.launch(Dispatchers.IO) {
                    val streamUrl = resolveStreamUrlForRoom(track)
                    if (streamUrl == null) {
                        Log.e(TAG, "Host failed to resolve guest-queued track ${track.id}")
                        return@launch
                    }
                    val mediaItem = createMediaItem(track, streamUrl)
                    launch(Dispatchers.Main) {
                        isSyncing = true
                        if (action.insertNext == true) {
                            p.addMediaItem(p.currentMediaItemIndex + 1, mediaItem)
                        } else {
                            p.addMediaItem(mediaItem)
                        }
                        endSyncAfter(200)
                    }
                }
            }
            PlaybackActions.QUEUE_REMOVE -> {
                val id = action.trackId ?: return
                for (i in 0 until p.mediaItemCount) {
                    if (p.getMediaItemAt(i).mediaId == id) {
                        isSyncing = true
                        p.removeMediaItem(i)
                        endSyncAfter(200)
                        break
                    }
                }
            }
            else -> {}
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

    /**
     * Resolve a playable stream URL for a Listen Together track, honoring the host's
     * audio source + quality so HQ (RemoteAudio) stays HQ on guests.
     *
     *  - The host stamps each track with [TrackInfo.audioSource] / [TrackInfo.sourceTrackId]
     *    / [TrackInfo.audioQuality]. When present we resolve from exactly that backend
     *    instead of guessing from the id format (the old 11-char heuristic mislabeled HQ
     *    tracks that carry a YouTube id as YouTube, silently dropping HQ on guests).
     *  - For HQ we stagger the request across the room (see [resolveHqStaggered]).
     *  - A YouTube/RemoteAudio cross-fallback ensures a guest never stalls the room on an
     *    unresolvable track.
     */
    private suspend fun resolveStreamUrlForRoom(track: TrackInfo): String? {
        val source = track.audioSource?.takeIf { it.isNotBlank() } ?: legacySourceGuess(track.id)
        val resolveId = track.sourceTrackId?.takeIf { it.isNotBlank() } ?: track.id
        val qualityInt = remoteQualityToInt(track.audioQuality)

        // Cache key includes quality so a re-resolve at a different quality isn't served a
        // stale URL; keyed by the backend-specific id we actually resolve with.
        val cacheKey = "$resolveId|${track.audioQuality ?: ""}"
        streamUrlCache.get(cacheKey)?.takeIf {
            System.currentTimeMillis() - it.resolvedAt < streamUrlTtlMs
        }?.let { return it.url }

        val resolved = if (source == "remote_audio") {
            resolveHqStaggered(resolveId, qualityInt)
                // Fallback when HQ is unreachable (rate-limited). For a hybrid track [track.id]
                // is a YouTube id and resolves directly; for a pure-HQ track it isn't, so we
                // also search YouTube by name — that way no HQ track can stall the room.
                ?: youTubeRepository.getStreamUrl(track.id)
                ?: youTubeRepository.getStreamUrlForDownload(track.id)?.first
                ?: resolveYouTubeByName(track)
        } else {
            youTubeRepository.getStreamUrl(track.id)
                ?: youTubeRepository.getStreamUrlForDownload(track.id)?.first
                ?: remoteAudioRepository.getStreamUrl(resolveId, qualityInt)
        }
        if (resolved != null) {
            streamUrlCache.put(cacheKey, CachedStreamUrl(resolved, System.currentTimeMillis()))
        }
        return resolved
    }

    /**
     * Last-ditch fallback: find the song on YouTube by title + artist when its native
     * backend can't serve a stream. Mirrors MusicPlayer's cross-source fallback so a
     * guest on a pure-HQ track — which has no YouTube id to fall back to — still stays in
     * the room when the RemoteAudio backend is rate-limited.
     */
    private suspend fun resolveYouTubeByName(track: TrackInfo): String? {
        val query = "${track.title} ${track.artist}".trim()
        if (query.isBlank()) return null
        return try {
            val ytId = kotlinx.coroutines.withTimeoutOrNull(8_000L) {
                youTubeRepository.search(query, YouTubeRepository.FILTER_SONGS).firstOrNull()?.id
            }
            ytId?.let { id ->
                youTubeRepository.getStreamUrl(id) ?: youTubeRepository.getStreamUrlForDownload(id)?.first
            }
        } catch (e: Exception) {
            Log.w(TAG, "YouTube name fallback failed for ${track.title}: ${e.message}")
            null
        }
    }

    /** Old id-format heuristic, used only when the host didn't stamp an audio source. */
    private fun legacySourceGuess(id: String): String =
        if (id.length == 11 && id.matches(Regex("[a-zA-Z0-9_-]{11}"))) "youtube" else "remote_audio"

    private fun remoteQualityToInt(quality: String?): Int = when (quality?.lowercase()) {
        "low" -> 96
        "high" -> 320
        else -> 160 // medium / auto / unknown
    }

    /**
     * Resolve an HQ (RemoteAudio) stream, staggered across the room so guests don't all
     * hit the rate-limited backend at once. Returns null when HQ can't be obtained — the
     * caller then falls back to YouTube to keep this guest in sync with the room.
     */
    private suspend fun resolveHqStaggered(remoteId: String, qualityInt: Int): String? {
        // Already backing off? Don't wait — fall back to YouTube now so this guest doesn't
        // drift while the whole room's HQ is gated.
        if (remoteAudioRepository.isInBackoff()) {
            Log.d(TAG, "HQ backoff active — skipping HQ for $remoteId, using YouTube")
            return null
        }
        // Deterministic per-guest slot: position in the buffer-coordination waiting list,
        // else a stable hash of our user id. Slot 0 fires immediately, others fan out.
        val me = userId.value
        val waiting = bufferingUsers.value
        val slot = when {
            me != null && waiting.contains(me) -> waiting.indexOf(me)
            me != null -> ((me.hashCode().toLong() and 0x7fffffffL) % STAGGER_SLOTS).toInt()
            else -> 0
        }
        val delayMs = (slot.toLong() * STAGGER_MS).coerceAtMost(STAGGER_MAX_MS)
        if (delayMs > 0) {
            Log.d(TAG, "HQ stagger: slot=$slot delay=${delayMs}ms for $remoteId")
            delay(delayMs)
            // An earlier guest may have tripped a 429 while we waited.
            if (remoteAudioRepository.isInBackoff()) {
                Log.d(TAG, "HQ backoff tripped during stagger — using YouTube for $remoteId")
                return null
            }
        }
        return remoteAudioRepository.getStreamUrl(remoteId, qualityInt)
    }

    private fun syncToTrack(
        track: TrackInfo,
        shouldPlay: Boolean,
        position: Long, 
        bypassBuffer: Boolean = false,
        queue: List<TrackInfo>? = null
    ) {
        bufferingTrackId = track.id
        loadingTrackId = track.id
        pendingBufferReadyTrackId = null
        activeSyncJob?.cancel()

        activeSyncJob = scope.launch(Dispatchers.IO) {
            try {
                // Resolve from the SAME backend + quality the host used (see
                // resolveStreamUrlForRoom) instead of guessing from the id format —
                // this is what keeps an HQ host's audio HQ on every guest.
                val streamUrl = resolveStreamUrlForRoom(track)

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
                        if (loadingTrackId == track.id) loadingTrackId = null
                        pendingBufferReadyTrackId = null
                        isSyncing = false
                    }
                    return@launch
                }

                // 3. Create MediaItem using the available track info metadata (no need for slow details endpoint)
                val currentMediaItem = createMediaItem(track, streamUrl)

                launch(Dispatchers.Main) {
                    val p = player ?: return@launch
                    // The stream is resolved and the MediaItem is built — the load is no
                    // longer "in flight", so release the mismatch guard. (bufferingTrackId
                    // stays set until buffer coordination completes.)
                    if (loadingTrackId == track.id) loadingTrackId = null
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
        if (isSyncing || !broadcastsControl) return
        
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
        // Tell guests which backend + quality this track actually played from on the
        // host, so they resolve identically (HQ stays HQ) instead of guessing from the
        // id format. Null when unknown (e.g. track never went through MusicPlayer) —
        // guests then fall back to the legacy id-length heuristic.
        val resolved = sessionManager.getResolvedPlaybackInfo(mediaItem.mediaId)
        return TrackInfo(
            id = mediaItem.mediaId,
            title = metadata.title?.toString() ?: "Unknown",
            artist = metadata.artist?.toString() ?: "Unknown",
            album = metadata.albumTitle?.toString(),
            duration = realDuration,
            thumbnail = metadata.artworkUri?.toString(),
            audioSource = resolved?.source,
            sourceTrackId = resolved?.sourceTrackId,
            audioQuality = resolved?.quality
        )
    }
    
    fun requestSync() = client.requestSync()
    fun suggestTrack(track: TrackInfo) = client.suggestTrack(track)

    /**
     * Route an "add to queue" (or "play next") while in a Listen Together room —
     * the Jam-style entry point for everyone adding songs to the shared session.
     *
     * Returns true when the room absorbed the request (guest paths) so the caller
     * must NOT also add locally. The host returns false — it adds to its own
     * player through the normal path — but the tracks are still broadcast so the
     * shared room queue stays in sync on every device.
     */
    fun addSongsToRoomQueue(songs: List<Song>, insertNext: Boolean = false): Boolean {
        if (!isInRoom || songs.isEmpty()) return false
        val addedBy = client.currentUsername
        val tracks = songs.map { song ->
            TrackInfo(
                id = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album.takeIf { it.isNotBlank() },
                duration = song.duration,
                thumbnail = song.thumbnailUrl,
                suggestedBy = addedBy
            )
        }
        return when {
            isHost -> {
                tracks.forEach { track ->
                    client.sendPlaybackAction(
                        PlaybackActions.QUEUE_ADD,
                        trackId = track.id,
                        trackInfo = track,
                        insertNext = insertNext
                    )
                }
                false
            }
            canQueue -> {
                tracks.forEach { track ->
                    client.sendPlaybackAction(
                        PlaybackActions.QUEUE_ADD,
                        trackId = track.id,
                        trackInfo = track,
                        insertNext = insertNext
                    )
                }
                com.suvojeet.suvmusic.util.SnackbarUtil.showMessage(
                    if (tracks.size == 1) "Added to the room queue" else "Added ${tracks.size} songs to the room queue"
                )
                true
            }
            else -> {
                tracks.forEach { client.suggestTrack(it) }
                com.suvojeet.suvmusic.util.SnackbarUtil.showMessage(
                    if (tracks.size == 1) "Sent to host for approval" else "Sent ${tracks.size} songs to host for approval"
                )
                true
            }
        }
    }

    /**
     * Guest-side skip. A guest's local player holds only the current track, so a
     * local seekToNext() is a no-op — the skip must be sent to the room, where the
     * host performs it and broadcasts the resulting track change.
     */
    fun sendGuestSkip(next: Boolean) {
        if (!isInRoom || isHost || !canControlPlayback) return
        client.sendPlaybackAction(if (next) PlaybackActions.SKIP_NEXT else PlaybackActions.SKIP_PREV)
    }
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
    suspend fun checkServerHealth() = client.checkServerHealth()

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
