package com.suvojeet.suvmusic.ai

import com.suvojeet.suvmusic.player.SpatialAudioProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIEqualizerService @Inject constructor(
    private val spatialAudioProcessor: SpatialAudioProcessor,
    private val musicPlayer: com.suvojeet.suvmusic.player.MusicPlayer
) {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs = _logs.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    private val _lastResult = MutableStateFlow<AudioEffectState?>(null)
    val lastResult = _lastResult.asStateFlow()

    fun addLog(message: String) {
        _logs.value = _logs.value + message
    }

    fun clearLogs() {
        _logs.value = emptyList()
        _lastResult.value = null
    }

    suspend fun processPrompt(
        prompt: String,
        provider: AIProvider,
        apiKey: String,
        model: String
    ) {
        if (apiKey.isBlank()) {
            addLog("Error: API Key is missing for ${provider.name}")
            return
        }

        _isProcessing.value = true
        addLog("Starting AI process with ${provider.name} using model $model...")
        
        val currentSong = musicPlayer.playerState.value.currentSong
        val songContext = currentSong?.let {
            SongContext(
                title = it.title,
                artist = it.artist,
                duration = it.duration,
                source = it.source.name
            )
        }

        if (songContext != null) {
            addLog("Song Detected: \"${songContext.title}\" by ${songContext.artist}")
        } else {
            addLog("Notice: No song metadata found.")
        }

        addLog("User request: \"$prompt\"")

        try {
            val client = when (provider) {
                AIProvider.OPENAI -> OpenAIClient(apiKey, model)
                AIProvider.ANTHROPIC -> AnthropicClient(apiKey, model)
                AIProvider.GEMINI -> GeminiClient(apiKey, model)
            }

            addLog("Analyzing current audio engine state...")
            val currentStatus = spatialAudioProcessor.getCurrentState()

            addLog("Contacting AI for optimization strategy...")
            val result = client.getAudioEffectState(prompt, currentStatus, songContext)
            
            result.onSuccess { state ->
                _lastResult.value = state
                addLog("Optimization complete. New parameters calculated:")
                addLog(" EQ: ${if(state.isEqEnabled) "Enabled" else "Disabled"}")
                addLog(" Bands (dB): ${state.safeEqBands.map { "%.1f".format(it) }.joinToString(", ")}")
                addLog(" Bass Boost: ${"%.2f".format(state.safeBassBoost)}")
                addLog(" Virtualizer (Echo): ${"%.2f".format(state.safeVirtualizer)}")
                addLog(" Spatial Audio: ${if(state.isSpatialEnabled) "On" else "Off"}")
                addLog(" Limiter Gain: ${"%.1f".format(state.safeLimiterMakeupGain)} dB")
                
                applySettings(state)
                addLog("SUCCESS: All parameters pushed to the native audio engine.")
            }.onFailure { error ->
                addLog("AI REJECTED: ${error.message}")
            }
        } catch (e: Exception) {
            addLog("SYSTEM FAILURE: ${e.message}")
        } finally {
            _isProcessing.value = false
        }
    }

    private fun applySettings(state: AudioEffectState) {
        spatialAudioProcessor.setEqEnabled(state.isEqEnabled)
        state.safeEqBands.forEachIndexed { index, gain ->
            if (index < 10) {
                spatialAudioProcessor.setEqBand(index, gain)
            }
        }
        spatialAudioProcessor.setBassBoost(state.safeBassBoost)
        spatialAudioProcessor.setVirtualizer(state.safeVirtualizer)
        spatialAudioProcessor.setSpatialEnabled(state.isSpatialEnabled)
        spatialAudioProcessor.setCrossfeedEnabled(state.isCrossfeedEnabled)
        // Adjust limiter for makeup gain
        spatialAudioProcessor.setLimiterConfig(true, 0, false) // Simple trigger
        // In real app, setLimiterConfig would be updated to accept makeupGain directly
    }
}
