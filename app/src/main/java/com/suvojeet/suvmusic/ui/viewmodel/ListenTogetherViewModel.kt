package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.listentogether.ConnectionState
import com.suvojeet.suvmusic.listentogether.ListenTogetherManager
import com.suvojeet.suvmusic.listentogether.RoomRole
import com.suvojeet.suvmusic.listentogether.RoomState
import com.suvojeet.suvmusic.listentogether.TrackInfo
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
    val logs = manager.logs
    val events = manager.events
    val pendingJoinRequests = manager.pendingJoinRequests
    
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
        viewModelScope.launch {
            _savedUsername.value = manager.getSavedUsername()
            _serverUrl.value = manager.getServerUrl()
            _autoApproval.value = manager.getAutoApproval()
            _syncVolume.value = manager.getSyncVolume()
            _muteHost.value = manager.getMuteHost()
        }
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
    
    // UI State for the sheet
    val uiState = combine(connectionState, roomState, role) { connection, room, role ->
        ListenTogetherUiState(
            connectionState = connection,
            roomState = room,
            role = role,
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
    
    fun sendChat(message: String) {
        manager.sendChat(message)
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
    
    fun requestSync() {
        manager.requestSync()
    }

    fun getSessionDuration(): Long = manager.getSessionAge()
}

data class ListenTogetherUiState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val roomState: RoomState? = null,
    val role: RoomRole = RoomRole.NONE,
    val isInRoom: Boolean = false
)
