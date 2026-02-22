package com.suvojeet.suvmusic.listentogether

/**
 * Event types for the Listen Together client
 */
sealed class ListenTogetherEvent {
    // Connection events
    data class Connected(val userId: String) : ListenTogetherEvent()
    data object Disconnected : ListenTogetherEvent()
    data class ConnectionError(val error: String) : ListenTogetherEvent()
    data class Reconnecting(val attempt: Int, val maxAttempts: Int) : ListenTogetherEvent()
    
    // Room events
    data class RoomCreated(val roomCode: String, val userId: String) : ListenTogetherEvent()
    data class JoinRequestReceived(val userId: String, val username: String) : ListenTogetherEvent()
    data class JoinApproved(val roomCode: String, val userId: String, val state: RoomState) : ListenTogetherEvent()
    data class JoinRejected(val reason: String) : ListenTogetherEvent()
    data class UserJoined(val userId: String, val username: String) : ListenTogetherEvent()
    data class UserLeft(val userId: String, val username: String) : ListenTogetherEvent()
    data class HostChanged(val newHostId: String, val newHostName: String) : ListenTogetherEvent()
    data class Kicked(val reason: String) : ListenTogetherEvent()
    data class Reconnected(val roomCode: String, val userId: String, val state: RoomState, val isHost: Boolean) : ListenTogetherEvent()
    data class UserReconnected(val userId: String, val username: String) : ListenTogetherEvent()
    data class UserDisconnected(val userId: String, val username: String) : ListenTogetherEvent()
    
    // Playback events
    data class PlaybackSync(val action: PlaybackActionPayload) : ListenTogetherEvent()
    data class BufferWait(val trackId: String, val waitingFor: List<String>) : ListenTogetherEvent()
    data class BufferComplete(val trackId: String) : ListenTogetherEvent()
    data class SyncStateReceived(val state: SyncStatePayload) : ListenTogetherEvent()
    
    // Error events
    data class ServerError(val code: String, val message: String) : ListenTogetherEvent()
}
