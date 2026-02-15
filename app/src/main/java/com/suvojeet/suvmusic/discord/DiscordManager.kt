package com.suvojeet.suvmusic.discord

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var privacyCollectorJob: Job? = null
    
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
        
        privacyCollectorJob?.cancel()
        privacyCollectorJob = scope.launch {
            sessionManager.privacyModeEnabledFlow.collect { privacyEnabled ->
                isPrivacyMode = privacyEnabled
                if (privacyEnabled) {
                    // Fully clear presence to hide from Discord profile
                    discordRPC?.clearActivity()
                }
            }
        }
    }
    
    fun updateSettings(userToken: String, enabled: Boolean, details: Boolean) {
        val previousToken = token
        token = userToken
        isEnabled = enabled
        useDetails = details
        
        if (isEnabled && !userToken.isBlank()) {
            // Only reconnect if token changed or not connected
            val tokenChanged = previousToken != userToken
            val isConnected = discordRPC != null && discordRPC!!.isWebSocketConnected()
            
            if (tokenChanged || !isConnected) {
                disconnect()
                connect()
            }
            // If already connected with same token, no action needed — just update local flags
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
    
    /** Clear presence and disconnect cleanly. Called when app is going away. */
    fun cleanup() {
        discordRPC?.clearActivity()
        disconnect()
        privacyCollectorJob?.cancel()
        scope.cancel()
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
            
            val (finalDetails, finalState) = if (useDetails) {
                 title to artist
            } else {
                 artist to title
            }

            discordRPC?.updateActivity(
                name = "SuvMusic",
                state = finalState,
                details = finalDetails,
                imageUrl = imageUrl,
                largeText = "Listening to $title",
                startTime = startTime,
            )
        } else {
            val (finalDetails, finalState) = if (useDetails) {
                 "$title (Paused)" to artist
            } else {
                 artist to "$title (Paused)"
            }

             discordRPC?.updateActivity(
                name = "SuvMusic",
                state = finalState,
                details = finalDetails,
                imageUrl = imageUrl,
                largeText = "Paused: $title"
            )
        }
    }
}
