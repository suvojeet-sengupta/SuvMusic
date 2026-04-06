package com.suvojeet.suvmusic.ai

import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.player.SpatialAudioProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
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

    private val _validationWarnings = MutableStateFlow<List<String>>(emptyList())
    val validationWarnings: StateFlow<List<String>> = _validationWarnings.asStateFlow()

    private var stateBeforeAI: AudioEffectState? = null
    private var currentPrompt: String? = null

    // A/B Compare state
    private var _isABCompareActive = MutableStateFlow(false)
    val isABCompareActive = _isABCompareActive.asStateFlow()
    private var aiState: AudioEffectState? = null
    private var originalState: AudioEffectState? = null

    // Auto AI Mode state
    private val _isAutoModeEnabled = MutableStateFlow(false)
    val isAutoModeEnabled = _isAutoModeEnabled.asStateFlow()

    // Prompt history
    private val _promptHistory = MutableStateFlow(AIPromptHistory())
    val promptHistory = _promptHistory.asStateFlow()

    // Track current song ID to detect song changes
    private var currentSongId: String? = null

    init {
        CoroutineScope(Dispatchers.IO).launch {
            _promptHistory.value = sessionManager.getAIPromptHistory()
            _isAutoModeEnabled.value = sessionManager.isAutoAIEnabled()
        }
        // Listen for song changes to clear A/B compare state or auto-apply AI
        CoroutineScope(Dispatchers.Main).launch {
            musicPlayer.playerState.filterNotNull().collect { state ->
                val newSongId = state.currentSong?.id
                if (newSongId != currentSongId && newSongId != null) {
                    val oldId = currentSongId
                    currentSongId = newSongId
                    
                    if (oldId != null) {
                        clearABCompareState()
                    }

                    if (_isAutoModeEnabled.value) {
                        autoProcessCurrentSong()
                    }
                }
            }
        }
    }

    fun setAutoModeEnabled(enabled: Boolean) {
        _isAutoModeEnabled.value = enabled
        CoroutineScope(Dispatchers.IO).launch {
            sessionManager.setAutoAIEnabled(enabled)
        }
        if (enabled && currentSongId != null) {
            CoroutineScope(Dispatchers.Main).launch {
                autoProcessCurrentSong()
            }
        }
    }

    private suspend fun autoProcessCurrentSong() {
        val currentSong = musicPlayer.playerState.value.currentSong ?: return
        
        addLog("AUTO: Song change detected - \"${currentSong.title}\"")
        
        // 1. Check if we already have saved AI settings for this song
        if (checkAndAutoApplyAIForSong(currentSong.id)) {
            addLog("AUTO: Applied previously saved AI profile")
            return
        }

        // 2. If not, generate new AI profile based on genre/metadata
        addLog("AUTO: Generating intelligent profile for \"${currentSong.artist}\"...")
        
        val autoPrompt = "Analyze this song: '${currentSong.title}' by '${currentSong.artist}'. " +
                "Provide a professional audio engineer's EQ and DSP setup that best fits its genre and characteristics. " +
                "Optimize for clarity, depth, and appropriate energy."

        // Use a default reliable provider/model for auto mode
        // In a real app, these would come from settings
        processPrompt(
            prompt = autoPrompt,
            provider = AIProvider.CHAT_PROXY,
            apiKey = "",
            model = "gpt-4o-mini" // Fast and reliable for auto-tuning
        )
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

    /**
     * Clears all A/B compare related state.
     * Called when song changes or user navigates away.
     */
    private fun clearABCompareState() {
        aiState = null
        originalState = null
        stateBeforeAI = null
        _isABCompareActive.value = false
        _validationWarnings.value = emptyList()
        addLog("AI compare state cleared (song change)")
    }

    /**
     * Call this when navigating away from the AI Equalizer screen
     * to prevent memory leaks.
     */
    fun cleanup() {
        clearABCompareState()
        currentPrompt = null
        _lastResult.value = null
        _logs.value = emptyList()
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
        if (provider != AIProvider.CHAT_PROXY && apiKey.isBlank()) {
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
                AIProvider.CHAT_PROXY -> {
                    // Build fallback list: exclude current model, shuffle remaining
                    val resolved = ChatProxyModels.resolve(model)
                    val fallbacks = ChatProxyModels.ALL
                        .filter { it != resolved }
                        .shuffled()
                        .take(2) // Try 2 fallbacks after primary fails

                    if (model == ChatProxyModels.RANDOM) {
                        addLog("Random model selected: $resolved")
                        addLog("Fallback models ready: ${fallbacks.joinToString(", ")}")
                    }
                    ChatProxyClient(model, fallbacks)
                }
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

            result.onSuccess { rawState ->
                // === IMPROVEMENT #1: Validate and sanitize AI response ===
                val validationResult = AudioEffectStateValidator.validate(rawState)
                val state = validationResult.sanitizedState

                if (validationResult.warnings.isNotEmpty()) {
                    addLog("Validation: ${validationResult.warnings.size} adjustment(s) made")
                    validationResult.warnings.take(3).forEach { warning ->
                        addLog("  ⚠ $warning")
                    }
                    if (validationResult.warnings.size > 3) {
                        addLog("  ... and ${validationResult.warnings.size - 3} more")
                    }
                }

                _validationWarnings.value = validationResult.warnings
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
                addLog("AI API FAILED: ${error.message}")

                // === IMPROVEMENT #2: Fallback to local presets ===
                addLog("Attempting local preset matching as fallback...")
                val fallbackPreset = AIFallbackPresets.findForPrompt(prompt)

                if (fallbackPreset != null) {
                    val fallbackState = AIFallbackPresets.toAudioEffectState(fallbackPreset)
                    aiState = fallbackState
                    _lastResult.value = fallbackState
                    addLog("FALLBACK: Applied '${fallbackPreset.name}' preset locally")
                    addLog(" Bands (dB): ${fallbackState.safeEqBands.joinToString(", ") { "%.1f".format(it) }}")
                    addLog(" Bass Boost: ${"%.2f".format(fallbackState.safeBassBoost)}")
                    addLog(" Virtualizer: ${"%.2f".format(fallbackState.safeVirtualizer)}")

                    applySettings(fallbackState)
                    addLog("SUCCESS: Fallback preset applied to native audio engine.")

                    savePromptToHistory(prompt)
                    if (currentSong != null) {
                        saveCurrentAISettings(prompt)
                    }
                } else {
                    addLog("FALLBACK: No matching preset found for: \"$prompt\"")
                    addLog("Tip: Try keywords like 'bass', 'treble', 'vocals', 'rock', 'jazz', etc.")
                }
            }
        } catch (e: Exception) {
            addLog("SYSTEM FAILURE: ${e.message}")

            // Fallback for system-level failures too
            addLog("Attempting local preset matching as fallback...")
            val fallbackPreset = AIFallbackPresets.findForPrompt(prompt)

            if (fallbackPreset != null) {
                val fallbackState = AIFallbackPresets.toAudioEffectState(fallbackPreset)
                aiState = fallbackState
                _lastResult.value = fallbackState
                addLog("FALLBACK: Applied '${fallbackPreset.name}' preset locally")
                applySettings(fallbackState)
                addLog("SUCCESS: Fallback preset applied to native audio engine.")
            } else {
                addLog("FALLBACK: No matching preset found for: \"$prompt\"")
            }
        } finally {
            _isProcessing.value = false
        }
    }

    private fun applySettings(state: AudioEffectState) {
        spatialAudioProcessor.applyAIState(state)
    }
}
