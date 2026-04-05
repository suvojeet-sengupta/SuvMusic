package com.suvojeet.suvmusic.ai

enum class AIProvider {
    OPENAI,
    ANTHROPIC,
    GEMINI
}

data class AudioEffectState(
    val eqEnabled: Boolean = true,
    val eqBands: List<Float> = List(10) { 0f }, // 10 bands in dB
    val bassBoost: Float = 0f, // 0.0 to 1.0
    val virtualizer: Float = 0f, // 0.0 to 1.0
    val spatialEnabled: Boolean = false,
    val crossfeedEnabled: Boolean = true,
    val limiterMakeupGain: Float = 0f // dB
)

interface AIClient {
    suspend fun getAudioEffectState(prompt: String, currentStatus: AudioEffectState): Result<AudioEffectState>
}
