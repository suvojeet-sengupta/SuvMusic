package com.suvojeet.suvmusic.ai

enum class AIProvider {
    OPENAI,
    ANTHROPIC,
    GEMINI
}

data class AudioEffectState(
    private val eqEnabled: Boolean? = true,
    private val eqBands: List<Float?>? = null,
    private val bassBoost: Float? = 0f,
    private val virtualizer: Float? = 0f,
    private val spatialEnabled: Boolean? = false,
    private val crossfeedEnabled: Boolean? = true,
    private val limiterMakeupGain: Float? = 0f
) {
    val isEqEnabled get() = eqEnabled ?: true
    val safeEqBands: List<Float> get() = eqBands?.map { it ?: 0f } ?: List(10) { 0f }
    val safeBassBoost get() = bassBoost ?: 0f
    val safeVirtualizer get() = virtualizer ?: 0f
    val isSpatialEnabled get() = spatialEnabled ?: false
    val isCrossfeedEnabled get() = crossfeedEnabled ?: true
    val safeLimiterMakeupGain get() = limiterMakeupGain ?: 0f
}

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
