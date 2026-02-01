package com.suvojeet.suvmusic.discord

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiscordManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var discordRPC: DiscordRPC? = null
    private var token: String? = null
    private var isEnabled: Boolean = false
    
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    fun initialize(userToken: String, enabled: Boolean) {
        token = userToken
        isEnabled = enabled
        
        if (isEnabled && !userToken.isBlank()) {
            connect()
        } else {
            disconnect()
        }
    }
    
    fun updateSettings(userToken: String, enabled: Boolean) {
        val wasConnected = discordRPC != null && discordRPC!!.isWebSocketConnected()
        token = userToken
        isEnabled = enabled
        
        if (isEnabled && !userToken.isBlank()) {
            if (!wasConnected || discordRPC == null) {
                // Reconnect if not connected or instance missing
                 connect()
            }
            // If already connected and token changed, we might need to reconnect. 
            // For simplicity, we can just reconnect.
             disconnect()
            connect()
        } else {
            disconnect()
        }
    }

    private fun connect() {
        if (token.isNullOrBlank()) return
        
        try {
            if (discordRPC == null) {
                discordRPC = DiscordRPC(token!!)
            }
            discordRPC?.connect()
            _connectionStatus.value = "Connecting..."
        } catch (e: Exception) {
            Log.e("DiscordManager", "Failed to connect", e)
            _connectionStatus.value = "Error: ${e.message}"
        }
    }

    private fun disconnect() {
        discordRPC?.close()
        discordRPC = null
        _connectionStatus.value = "Disconnected"
    }
    
    fun updatePresence(
        title: String,
        artist: String,
        imageUrl: String?,
        isPlaying: Boolean,
        duration: Long,
        currentPosition: Long
    ) {
        if (!isEnabled || discordRPC == null) return
        
        if (isPlaying) {
             val startTime = System.currentTimeMillis() - currentPosition
             // val endTime = startTime + duration // Optional, creates a countdown
            
            discordRPC?.updateActivity(
                name = "SuvMusic",
                state = artist,
                details = title,
                imageUrl = "https:$imageUrl", // Ensure https prefix if needed, mostly cover art utils handle this.
                largeText = "Listening to $title",
                startTime = startTime,
                // endTime = if (duration > 0) startTime + duration else null
            )
        } else {
             // Clear activity or show paused?
             // Usually showing "Paused" or just clearing relative timestamps is enough.
             discordRPC?.updateActivity(
                name = "SuvMusic",
                state = artist,
                details = "$title (Paused)",
                imageUrl = "https:$imageUrl",
                largeText = "Paused: $title"
            )
        }
    }
}
