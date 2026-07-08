package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.shareplay.ConnectionState
import com.suvojeet.suvmusic.shareplay.ListenTogetherClient
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
    private val manager: ListenTogetherManager
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

    private val _serverHealth = kotlinx.coroutines.flow.MutableStateFlow<ListenTogetherClient.ServerHealth?>(null)
    val serverHealth: StateFlow<ListenTogetherClient.ServerHealth?> = _serverHealth

    private val _isCheckingHealth = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isCheckingHealth: StateFlow<Boolean> = _isCheckingHealth

    private val _autoApproval = kotlinx.coroutines.flow.MutableStateFlow(false)
    val autoApproval: StateFlow<Boolean> = _autoApproval
    
    private val _syncVolume = kotlinx.coroutines.flow.MutableStateFlow(true)
    val syncVolume: StateFlow<Boolean> = _syncVolume
    
    private val _muteHost = kotlinx.coroutines.flow.MutableStateFlow(false)
    val muteHost: StateFlow<Boolean> = _muteHost

    init {
        manager.initialize()
        viewModelScope.launch {
            _savedUsername.value = manager.getSavedUsername()
            _serverUrl.value = manager.getServerUrl()
            _autoApproval.value = manager.getAutoApproval()
            _syncVolume.value = manager.getSyncVolume()
            _muteHost.value = manager.getMuteHost()
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
        viewModelScope.launch {
            manager.joinRoom(code, name)
        }
    }

    fun leaveRoom() {
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
}

data class ListenTogetherUiState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val roomState: RoomState? = null,
    val role: RoomRole = RoomRole.NONE,
    val userId: String? = null,
    val isInRoom: Boolean = false
)
