package com.suvojeet.suvmusic.ai

import com.suvojeet.suvmusic.player.SpatialAudioProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIEqualizerService @Inject constructor(
    private val spatialAudioProcessor: SpatialAudioProcessor
) {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs = _logs.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    fun addLog(message: String) {
        _logs.value = _logs.value + message
    }

    fun clearLogs() {
        _logs.value = emptyList()
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
        addLog("User request: \"$prompt\"")

        try {
            val client = when (provider) {
                AIProvider.OPENAI -> OpenAIClient(apiKey, model)
                AIProvider.ANTHROPIC -> AnthropicClient(apiKey, model)
                AIProvider.GEMINI -> GeminiClient(apiKey, model)
            }

            addLog("Analyzing current audio parameters...")
            
            // In a real app, we'd pull this from the processor or session manager
            val currentStatus = AudioEffectState(
                // For simplicity, we can just pass some defaults or track state here
            )

            addLog("Sending request to AI provider...")
            val result = client.getAudioEffectState(prompt, currentStatus)
            
            result.onSuccess { state ->
                addLog("AI response received. Applying settings:")
                addLog(" - EQ Enabled: ${state.eqEnabled}")
                addLog(" - EQ Bands: ${state.eqBands.joinToString(", ")}")
                addLog(" - Bass Boost: ${state.bassBoost}")
                addLog(" - Virtualizer: ${state.virtualizer}")
                addLog(" - Spatial: ${state.spatialEnabled}")
                
                applySettings(state)
                addLog("Successfully applied AI-optimized settings!")
            }.onFailure { error ->
                addLog("AI Error: ${error.message}")
            }
        } catch (e: Exception) {
            addLog("System Error: ${e.message}")
        } finally {
            _isProcessing.value = false
        }
    }

    private fun applySettings(state: AudioEffectState) {
        spatialAudioProcessor.setEqEnabled(state.eqEnabled)
        state.eqBands.forEachIndexed { index, gain ->
            if (index < 10) {
                spatialAudioProcessor.setEqBand(index, gain)
            }
        }
        spatialAudioProcessor.setBassBoost(state.bassBoost)
        spatialAudioProcessor.setVirtualizer(state.virtualizer)
        spatialAudioProcessor.setSpatialEnabled(state.spatialEnabled)
        spatialAudioProcessor.setCrossfeedEnabled(state.crossfeedEnabled)
        // Adjust limiter for makeup gain
        spatialAudioProcessor.setLimiterConfig(true, 0, false) // Simple trigger
        // In real app, setLimiterConfig would be updated to accept makeupGain directly
    }
}
