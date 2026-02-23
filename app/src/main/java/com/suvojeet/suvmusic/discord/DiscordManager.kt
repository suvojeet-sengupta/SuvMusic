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
    @ApplicationContext private val context: Context,
    private val sessionManager: com.suvojeet.suvmusic.data.SessionManager
) {
    private var discordRPC: DiscordRPC? = null
    private var token: String? = null
    private var isEnabled: Boolean = false
    private var useDetails: Boolean = false
    private var isPrivacyMode: Boolean = false
    
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    fun initialize(userToken: String, enabled: Boolean, details: Boolean) {
        token = userToken
        isEnabled = enabled
        useDetails = details
        
        if (isEnabled && !userToken.isBlank()) {
            connect()
        } else {
            disconnect()
        }
        
        scope.launch {
            sessionManager.privacyModeEnabledFlow.collect { enabled ->
                isPrivacyMode = enabled
                if (enabled) {
                    // Start Privacy Mode: Clear presence but keep connection? 
                    // Or maybe just show "Listening to Music" without details?
                    // "Stealth Listening" implies hiding completely.
                    // Discord RPC doesn't have a "Hide" method other than clearing, 
                    // but usually we just stop sending updates.
                    // If we want to hide "Playing SuvMusic", we should probably disconnect or send empty.
                    // Let's just stop sending updates for now.
                    // Optionally, we can clear the activity:
                    discordRPC?.updateActivity(name = "SuvMusic", state = null, details = null)
                }
            }
        }
    }
    
    fun updateSettings(userToken: String, enabled: Boolean, details: Boolean) {
        val wasConnected = discordRPC != null && discordRPC!!.isWebSocketConnected()
        token = userToken
        isEnabled = enabled
        useDetails = details
        
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
        val rpc = discordRPC
        discordRPC = null
        _connectionStatus.value = "Disconnected"
        scope.launch {
            rpc?.close()
        }
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
        if (isPrivacyMode) return 
        
        if (isPlaying) {
             val startTime = System.currentTimeMillis() - currentPosition
             // val endTime = startTime + duration // Optional, creates a countdown
            
            val (finalDetails, finalState) = if (useDetails) {
                 title to artist
            } else {
                 artist to title
            }
            
            val formattedImageUrl = when {
                imageUrl.isNullOrBlank() -> null
                imageUrl.startsWith("http") -> imageUrl
                else -> "https:$imageUrl"
            }

            discordRPC?.updateActivity(
                name = "SuvMusic",
                state = finalState,
                details = finalDetails,
                imageUrl = formattedImageUrl,
                largeText = "Listening to $title",
                startTime = startTime,
                // endTime = if (duration > 0) startTime + duration else null
            )
        } else {
             // Clear activity or show paused?
             // Usually showing "Paused" or just clearing relative timestamps is enough.
             
            val (finalDetails, finalState) = if (useDetails) {
                 "$title (Paused)" to artist
            } else {
                 artist to "$title (Paused)"
            }

            val formattedImageUrl = when {
                imageUrl.isNullOrBlank() -> null
                imageUrl.startsWith("http") -> imageUrl
                else -> "https:$imageUrl"
            }

             discordRPC?.updateActivity(
                name = "SuvMusic",
                state = finalState,
                details = finalDetails,
                imageUrl = formattedImageUrl,
                largeText = "Paused: $title"
            )
        }
    }
}
