package com.suvojeet.suvmusic.ai

enum class AIProvider {
    OPENAI,
    ANTHROPIC,
    GEMINI
}

data class AudioEffectState(
    val eqEnabled: Boolean = true,
    val eqBands: List<Float> = List(10) { 0f },
    val bassBoost: Float = 0f,
    val virtualizer: Float = 0f,
    val spatialEnabled: Boolean = false,
    val crossfeedEnabled: Boolean = true,
    val limiterMakeupGain: Float = 0f
)

data class SongContext(
    val title: String,
    val artist: String,
    val duration: Long,
    val source: String
)

interface AIClient {
    suspend fun getAudioEffectState(
        prompt: String, 
        currentStatus: AudioEffectState,
        songContext: SongContext? = null
    ): Result<AudioEffectState>
}
