package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.shareplay.ConnectionState
import com.suvojeet.suvmusic.shareplay.ListenTogetherClient
import com.suvojeet.suvmusic.shareplay.ListenTogetherEvent
import com.suvojeet.suvmusic.shareplay.ListenTogetherManager
import com.suvojeet.suvmusic.shareplay.ListenTogetherServer
import com.suvojeet.suvmusic.shareplay.ListenTogetherServers
import com.suvojeet.suvmusic.shareplay.RoomRole
import com.suvojeet.suvmusic.shareplay.RoomSettings
import com.suvojeet.suvmusic.shareplay.RoomState
import com.suvojeet.suvmusic.shareplay.TrackInfo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Maximum username length accepted when hosting/joining a room. */
const val LISTEN_TOGETHER_MAX_USERNAME = 24
/** Room codes are 6-character uppercase alphanumeric (see server `generate_room_code`). */
const val LISTEN_TOGETHER_ROOM_CODE_LENGTH = 6

/** Validate a profile name. Returns null when valid, otherwise a user-facing reason. */
fun validateListenTogetherUsername(name: String): String? {
    val trimmed = name.trim()
    return when {
        trimmed.isEmpty() -> "Enter a name so others can recognize you"
        trimmed.length > LISTEN_TOGETHER_MAX_USERNAME -> "Name must be $LISTEN_TOGETHER_MAX_USERNAME characters or fewer"
        else -> null
    }
}

class ListenTogetherViewModel @Inject constructor(
    private val manager: ListenTogetherManager,
    private val youTubeRepository: com.suvojeet.suvmusic.data.repository.YouTubeRepository
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = manager.connectionState
    val roomState: StateFlow<RoomState?> = manager.roomState
    val role: StateFlow<RoomRole> = manager.role
    val userId: StateFlow<String?> = manager.userId
    val pendingJoinRequests = manager.pendingJoinRequests
    val bufferingUsers = manager.bufferingUsers
    val pendingSuggestions = manager.pendingSuggestions
    val logs = manager.logs
    val isLogActive = manager.isLogActive
    val events = manager.events
    val blockedUsers = manager.blockedUsers
    val hasPersistedSession: Boolean get() = manager.hasPersistedSession
    
    private val _savedUsername = kotlinx.coroutines.flow.MutableStateFlow("")
    val savedUsername: StateFlow<String> = _savedUsername
    
    private val _serverUrl = kotlinx.coroutines.flow.MutableStateFlow("")
    val serverUrl: StateFlow<String> = _serverUrl

    /** Descriptor (name/location/operator) of the currently configured server, if known. */
    val serverInfo: StateFlow<ListenTogetherServer?> = serverUrl
        .map { ListenTogetherServers.findByUrl(it) ?: ListenTogetherServers.servers.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // HTTP /health poll result — used before we're connected (or as a fallback).
    private val _serverHealth = kotlinx.coroutines.flow.MutableStateFlow<ListenTogetherClient.ServerHealth?>(null)

    /** Live server stats over the WebSocket (PONG). Null until the first pong. */
    val serverStats: StateFlow<ListenTogetherClient.ServerStats?> = manager.serverStats

    /**
     * The status shown in the UI. While connected we prefer the live WebSocket
     * stats (always fresh, and from the exact server process the user is on) so the
     * figures can't go stale like the one-shot HTTP poll did; otherwise we fall back
     * to the /health poll.
     */
    val serverHealth: StateFlow<ListenTogetherClient.ServerHealth?> = combine(
        _serverHealth, manager.serverStats, connectionState
    ) { http, live, conn ->
        if (conn == ConnectionState.CONNECTED && live != null) {
            ListenTogetherClient.ServerHealth(
                online = true,
                activeRooms = live.activeRooms,
                activeSessions = live.activeConnections,
                uptimeSeconds = live.uptimeSeconds,
                version = http?.version ?: ""
            )
        } else {
            http
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isCheckingHealth = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isCheckingHealth: StateFlow<Boolean> = _isCheckingHealth

    private val _autoApproval = kotlinx.coroutines.flow.MutableStateFlow(false)
    val autoApproval: StateFlow<Boolean> = _autoApproval
    
    private val _syncVolume = kotlinx.coroutines.flow.MutableStateFlow(true)
    val syncVolume: StateFlow<Boolean> = _syncVolume
    
    private val _muteHost = kotlinx.coroutines.flow.MutableStateFlow(false)
    val muteHost: StateFlow<Boolean> = _muteHost

    /** Exact-sync toggle (default on). Off = faster song switches, looser sync. */
    val exactSyncEnabled: StateFlow<Boolean> = manager.exactSyncEnabled

    // True from the moment a guest taps "Join" until the host approves/rejects, the
    // room turns out not to exist, or the guest cancels. Drives the "waiting for the
    // host" screen so a pending join no longer looks like nothing happened.
    private val _joinInProgress = kotlinx.coroutines.flow.MutableStateFlow(false)
    val joinInProgress: StateFlow<Boolean> = _joinInProgress

    init {
        manager.initialize()
        viewModelScope.launch {
            _savedUsername.value = manager.getSavedUsername()
            _serverUrl.value = manager.getServerUrl()
            _autoApproval.value = manager.getAutoApproval()
            _syncVolume.value = manager.getSyncVolume()
            _muteHost.value = manager.getMuteHost()
        }
        // Drive the join flow off server events so a pending/failed join shows the
        // right message instead of silently doing nothing.
        viewModelScope.launch {
            events.collect { event ->
                when (event) {
                    is ListenTogetherEvent.JoinApproved -> _joinInProgress.value = false
                    is ListenTogetherEvent.JoinRejected -> {
                        _joinInProgress.value = false
                        com.suvojeet.suvmusic.util.SnackbarUtil.showWarning(
                            event.reason.takeIf { it.isNotBlank() } ?: "The host declined your request to join"
                        )
                    }
                    is ListenTogetherEvent.ServerError -> {
                        // Surface join failures (room gone, full, backlog) as clear text.
                        val joinMessage = when (event.code) {
                            "room_not_found" -> "Room not found — check the code and try again"
                            "room_full" -> "That room is full"
                            "too_many_pending" -> "Too many people are waiting to join — try again in a moment"
                            else -> null
                        }
                        if (joinMessage != null) {
                            _joinInProgress.value = false
                            com.suvojeet.suvmusic.util.SnackbarUtil.showError(joinMessage)
                        }
                    }
                    is ListenTogetherEvent.Kicked -> _joinInProgress.value = false
                    is ListenTogetherEvent.ConnectionError -> {
                        if (_joinInProgress.value) {
                            _joinInProgress.value = false
                            com.suvojeet.suvmusic.util.SnackbarUtil.showError("Couldn't reach the server — check your connection")
                        }
                    }
                    else -> {}
                }
            }
        }
    }
    
    fun connectToServer() {
        manager.connect()
    }

    fun disconnectFromServer() {
        manager.disconnect()
    }

    fun setLogActive(active: Boolean) {
        manager.setLogActive(active)
    }

    fun clearLogs() {
        manager.clearLogs()
    }

    fun blockUser(userId: String) {
        manager.blockUser(userId)
    }

    fun unblockUser(userId: String) {
        manager.unblockUser(userId)
    }
    
    fun updateSavedUsername(name: String) {
        _savedUsername.value = name
        viewModelScope.launch {
            manager.saveUsername(name)
        }
    }

    /** Probe the configured server's /health endpoint and publish the result. */
    fun refreshServerHealth() {
        if (_isCheckingHealth.value) return
        viewModelScope.launch {
            _isCheckingHealth.value = true
            try {
                _serverHealth.value = manager.checkServerHealth()
            } finally {
                _isCheckingHealth.value = false
            }
        }
    }
    
    fun updateAutoApproval(enabled: Boolean) {
        _autoApproval.value = enabled
        viewModelScope.launch {
            manager.setAutoApproval(enabled)
        }
    }
    
    fun updateSyncVolume(enabled: Boolean) {
        _syncVolume.value = enabled
        viewModelScope.launch {
            manager.setSyncVolume(enabled)
        }
    }
    
    fun updateMuteHost(enabled: Boolean) {
        _muteHost.value = enabled
        viewModelScope.launch {
            manager.setMuteHost(enabled)
        }
    }

    fun updateExactSync(enabled: Boolean) {
        manager.setExactSync(enabled)
    }

    val uiState: StateFlow<ListenTogetherUiState> = combine(
        connectionState,
        roomState,
        role,
        userId
    ) { conn, room, role, uid ->
        ListenTogetherUiState(
            connectionState = conn,
            roomState = room,
            role = role,
            userId = uid,
            isInRoom = room != null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ListenTogetherUiState()
    )
    fun createRoom(username: String) {
        val name = username.trim()
        // Defensive guard: the UI disables Host until the name is valid, but never
        // let a blank/oversized name reach the server.
        if (validateListenTogetherUsername(name) != null) return
        viewModelScope.launch {
            manager.createRoom(name)
        }
    }

    fun joinRoom(roomCode: String, username: String) {
        val name = username.trim()
        val code = roomCode.trim().uppercase()
        if (validateListenTogetherUsername(name) != null) return
        if (code.length != LISTEN_TOGETHER_ROOM_CODE_LENGTH) return
        _joinInProgress.value = true
        viewModelScope.launch {
            manager.joinRoom(code, name)
        }
    }

    /** Abandon a pending join (while waiting for host approval) and return to setup. */
    fun cancelJoin() {
        _joinInProgress.value = false
        // Drop the socket so the server discards our pending request; the guest can
        // reconnect and try again. (There's no room membership to leave yet.)
        manager.disconnect()
    }

    fun leaveRoom() {
        _joinInProgress.value = false
        viewModelScope.launch {
            manager.leaveRoom()
        }
    }
    
    fun kickUser(userId: String) {
        manager.kickUser(userId)
    }
    
    fun approveJoin(userId: String) {
        manager.approveJoin(userId)
    }

    fun rejectJoin(userId: String) {
        manager.rejectJoin(userId)
    }
    
    fun transferHost(newHostId: String) {
        manager.transferHost(newHostId)
    }

    /** Current Jam permissions of the room, live from the room state. */
    val roomSettings: StateFlow<RoomSettings> = roomState
        .map { it?.settings ?: RoomSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RoomSettings())

    /** Host-only: toggle whether guests can add songs to the shared queue. */
    fun setGuestsCanQueue(enabled: Boolean) {
        manager.setRoomSettings(roomSettings.value.copy(guestsCanQueue = enabled))
    }

    /** Host-only: toggle whether guests can control playback. */
    fun setGuestsCanControl(enabled: Boolean) {
        manager.setRoomSettings(roomSettings.value.copy(guestsCanControl = enabled))
    }
    
    fun forceReconnect() {
        manager.forceReconnect()
    }
    
    fun getPersistedRoomCode(): String? = manager.getPersistedRoomCode()
    
    fun getSessionDuration(): Long = manager.getSessionDuration()
    
    fun requestSync() {
        manager.requestSync()
    }

    // ─── Song suggestions ────────────────────────────────────────────────────
    // A guest searches here and taps a song; it's sent to the host who can play
    // it whenever they approve. The host sees pending suggestions via
    // `pendingSuggestions`.

    private val _suggestQuery = kotlinx.coroutines.flow.MutableStateFlow("")
    val suggestQuery: StateFlow<String> = _suggestQuery

    private val _suggestResults = kotlinx.coroutines.flow.MutableStateFlow<List<com.suvojeet.suvmusic.core.model.Song>>(emptyList())
    val suggestResults: StateFlow<List<com.suvojeet.suvmusic.core.model.Song>> = _suggestResults

    private val _isSearchingSuggest = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isSearchingSuggest: StateFlow<Boolean> = _isSearchingSuggest

    private var suggestSearchJob: kotlinx.coroutines.Job? = null

    fun onSuggestQueryChange(query: String) {
        _suggestQuery.value = query
        suggestSearchJob?.cancel()
        if (query.isBlank()) {
            _suggestResults.value = emptyList()
            _isSearchingSuggest.value = false
            return
        }
        suggestSearchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(350)  // debounce keystrokes
            _isSearchingSuggest.value = true
            try {
                _suggestResults.value = youTubeRepository.search(query.trim())
            } catch (e: Exception) {
                _suggestResults.value = emptyList()
            } finally {
                _isSearchingSuggest.value = false
            }
        }
    }

    fun clearSuggestSearch() {
        suggestSearchJob?.cancel()
        _suggestQuery.value = ""
        _suggestResults.value = emptyList()
        _isSearchingSuggest.value = false
    }

    /** Guest taps a search result — send it to the host as a suggestion. */
    fun suggestSong(song: com.suvojeet.suvmusic.core.model.Song) {
        val addedBy = manager.currentUsername
        manager.suggestTrack(
            TrackInfo(
                id = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album.takeIf { it.isNotBlank() },
                duration = song.duration,
                thumbnail = song.thumbnailUrl,
                suggestedBy = addedBy
            )
        )
        com.suvojeet.suvmusic.util.SnackbarUtil.showMessage("Suggested \"${song.title}\" — the host can play it once approved")
    }

    fun approveSuggestion(id: String) = manager.approveSuggestion(id)
    fun rejectSuggestion(id: String) = manager.rejectSuggestion(id)
}

data class ListenTogetherUiState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val roomState: RoomState? = null,
    val role: RoomRole = RoomRole.NONE,
    val userId: String? = null,
    val isInRoom: Boolean = false
)
