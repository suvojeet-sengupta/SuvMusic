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

    init {
        viewModelScope.launch {
            _savedUsername.value = manager.getSavedUsername()
            _serverUrl.value = manager.getServerUrl()
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
}

data class ListenTogetherUiState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val roomState: RoomState? = null,
    val role: RoomRole = RoomRole.NONE,
    val isInRoom: Boolean = false
)
