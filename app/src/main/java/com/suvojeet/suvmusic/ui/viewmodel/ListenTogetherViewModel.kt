package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.shareplay.ConnectionState
import com.suvojeet.suvmusic.shareplay.ListenTogetherManager
import com.suvojeet.suvmusic.shareplay.RoomRole
import com.suvojeet.suvmusic.shareplay.RoomState
import com.suvojeet.suvmusic.shareplay.TrackInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
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

    fun updateServerUrl(url: String) {
        _serverUrl.value = url
        viewModelScope.launch {
            manager.setServerUrl(url)
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
        viewModelScope.launch {
            manager.createRoom(username)
        }
    }

    fun joinRoom(roomCode: String, username: String) {
        viewModelScope.launch {
            manager.joinRoom(roomCode, username)
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
