package com.suvojeet.suvmusic.ai

import com.google.gson.annotations.SerializedName

enum class AIProvider {
    OPENAI,
    ANTHROPIC,
    GEMINI
}

data class AudioEffectState(
    @SerializedName("eqEnabled") private val eqEnabled: Boolean? = true,
    @SerializedName("eqBands") private val eqBands: List<Float?>? = null,
    @SerializedName("bassBoost") private val bassBoost: Float? = 0f,
    @SerializedName("virtualizer") private val virtualizer: Float? = 0f,
    @SerializedName("spatialEnabled") private val spatialEnabled: Boolean? = false,
    @SerializedName("crossfeedEnabled") private val crossfeedEnabled: Boolean? = true,
    @SerializedName("limiterMakeupGain") private val limiterMakeupGain: Float? = 0f,
    @SerializedName("limiterThresholdDb") private val _limiterThresholdDb: Float? = -0.1f,
    @SerializedName("limiterRatio") private val _limiterRatio: Float? = 4.0f,
    @SerializedName("limiterAttackMs") private val _limiterAttackMs: Float? = 5.0f,
    @SerializedName("limiterReleaseMs") private val _limiterReleaseMs: Float? = 100.0f
) {
    val isEqEnabled get() = eqEnabled ?: true
    val safeEqBands: List<Float> get() = (0 until 10).map { eqBands?.getOrNull(it) ?: 0f }
    val safeBassBoost get() = bassBoost ?: 0f
    val safeVirtualizer get() = virtualizer ?: 0f
    val isSpatialEnabled get() = spatialEnabled ?: false
    val isCrossfeedEnabled get() = crossfeedEnabled ?: true
    val safeLimiterMakeupGain get() = limiterMakeupGain ?: 0f
    
    val limiterThresholdDb get() = _limiterThresholdDb ?: -0.1f
    val limiterRatio get() = _limiterRatio ?: 4.0f
    val limiterAttackMs get() = _limiterAttackMs ?: 5.0f
    val limiterReleaseMs get() = _limiterReleaseMs ?: 100.0f
}

data class SignalStats(
    val peakLevel: Float,
    val rmsLevel: Float
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
