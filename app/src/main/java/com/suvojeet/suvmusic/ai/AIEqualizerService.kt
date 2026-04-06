package com.suvojeet.suvmusic.ai

import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.player.SpatialAudioProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIEqualizerService @Inject constructor(
    private val spatialAudioProcessor: SpatialAudioProcessor,
    private val musicPlayer: com.suvojeet.suvmusic.player.MusicPlayer,
    private val sessionManager: SessionManager
) {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs = _logs.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    private val _lastResult = MutableStateFlow<AudioEffectState?>(null)
    val lastResult = _lastResult.asStateFlow()

    private var stateBeforeAI: AudioEffectState? = null
    private var currentPrompt: String? = null

    // A/B Compare state
    private var _isABCompareActive = MutableStateFlow(false)
    val isABCompareActive = _isABCompareActive.asStateFlow()
    private var aiState: AudioEffectState? = null
    private var originalState: AudioEffectState? = null

    // Prompt history
    private val _promptHistory = MutableStateFlow(AIPromptHistory())
    val promptHistory = _promptHistory.asStateFlow()

    init {
        // Load prompt history on init
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            _promptHistory.value = sessionManager.getAIPromptHistory()
        }
    }

    fun addLog(message: String) {
        _logs.value = _logs.value + message
    }

    fun clearLogs() {
        _logs.value = emptyList()
        _lastResult.value = null
    }

    fun revertAIChanges() {
        stateBeforeAI?.let { previousState ->
            addLog("Reverting to pre-AI state...")
            applySettings(previousState)
            addLog("SUCCESS: Reverted to previous audio settings")
            stateBeforeAI = null
            _lastResult.value = null
            aiState = null
            originalState = null
            _isABCompareActive.value = false
        } ?: run {
            addLog("Notice: No AI changes to revert")
        }
    }

    fun toggleABCompare() {
        if (aiState == null || originalState == null) {
            addLog("Notice: No AI settings to compare")
            return
        }

        _isABCompareActive.value = !_isABCompareActive.value
        if (_isABCompareActive.value) {
            addLog("A/B Compare: Switched to ORIGINAL settings")
            applySettings(originalState!!)
        } else {
            addLog("A/B Compare: Switched to AI-optimized settings")
            applySettings(aiState!!)
        }
    }

    fun setCompareMode(showAI: Boolean) {
        if (aiState == null || originalState == null) return
        
        _isABCompareActive.value = !showAI
        if (_isABCompareActive.value) {
            applySettings(originalState!!)
        } else {
            applySettings(aiState!!)
        }
    }

    fun disableABCompare() {
        _isABCompareActive.value = false
        aiState = null
        originalState = null
    }

    suspend fun loadPromptHistory() {
        _promptHistory.value = sessionManager.getAIPromptHistory()
    }

    suspend fun savePromptToHistory(prompt: String) {
        val currentSong = musicPlayer.playerState.value.currentSong
        sessionManager.saveAIPromptHistory(prompt, currentSong?.id, currentSong?.title)
        _promptHistory.value = sessionManager.getAIPromptHistory()
    }

    suspend fun clearPromptHistory() {
        sessionManager.clearAIPromptHistory()
        _promptHistory.value = AIPromptHistory()
    }

    suspend fun checkAndAutoApplyAIForSong(songId: String): Boolean {
        val savedSettings = sessionManager.getSongAISettings(songId)
        if (savedSettings != null) {
            addLog("Auto-applying saved AI settings for this song...")
            applySettings(savedSettings.audioEffectState)
            _lastResult.value = savedSettings.audioEffectState
            stateBeforeAI = spatialAudioProcessor.getCurrentState()
            aiState = savedSettings.audioEffectState
            currentPrompt = savedSettings.prompt
            return true
        }
        return false
    }

    suspend fun saveCurrentAISettings(prompt: String) {
        val currentSong = musicPlayer.playerState.value.currentSong ?: return
        val aiState = _lastResult.value ?: return
        
        val songSettings = SongAISettings(
            audioEffectState = aiState,
            prompt = prompt,
            timestamp = System.currentTimeMillis()
        )
        
        sessionManager.saveSongAISettings(currentSong.id, songSettings)
        addLog("AI settings saved for: ${currentSong.title}")
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
            
            // Save original state for A/B compare ONLY if not already in A/B mode
            if (originalState == null) {
                originalState = currentStatus
            }
            stateBeforeAI = currentStatus // Save for potential revert
            currentPrompt = prompt

            val stats = spatialAudioProcessor.getSignalStats()
            addLog("Live Signal: Peak ${"%.2f".format(stats.peakLevel)}, RMS ${"%.2f".format(stats.rmsLevel)}")

            addLog("Contacting AI for hardware-level tuning...")
            val result = client.getAudioEffectState(prompt, currentStatus, songContext)

            result.onSuccess { state ->
                aiState = state // Save for A/B compare
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
                
                // Save to history and persistence
                savePromptToHistory(prompt)
                if (currentSong != null) {
                    saveCurrentAISettings(prompt)
                }
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
        spatialAudioProcessor.applyAIState(state)
    }
}
