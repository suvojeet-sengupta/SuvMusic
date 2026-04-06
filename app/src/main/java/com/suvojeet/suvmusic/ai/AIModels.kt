package com.suvojeet.suvmusic.ai

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

enum class AIProvider {
    OPENAI,
    ANTHROPIC,
    GEMINI,
    CHAT_PROXY
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

    fun toJson(): String {
        return Gson().toJson(this)
    }

    companion object {
        fun fromJson(json: String): AudioEffectState {
            val type = object : TypeToken<AudioEffectState>() {}.type
            return Gson().fromJson(json, type)
        }
    }
}

data class PromptHistoryEntry(
    val prompt: String,
    val timestamp: Long,
    val songId: String?,
    val songTitle: String?
)

data class AIPromptHistory(
    val entries: List<PromptHistoryEntry> = emptyList(),
    val maxEntries: Int = 20
) {
    fun addEntry(prompt: String, songId: String?, songTitle: String?): AIPromptHistory {
        val newEntry = PromptHistoryEntry(prompt, System.currentTimeMillis(), songId, songTitle)
        val updatedEntries = listOf(newEntry) + entries.filter { it.prompt != prompt }.take(maxEntries - 1)
        return copy(entries = updatedEntries)
    }

    fun removeEntry(index: Int): AIPromptHistory {
        return copy(entries = entries.filterIndexed { i, _ -> i != index })
    }

    fun clear(): AIPromptHistory {
        return copy(entries = emptyList())
    }

    fun toJson(): String {
        return Gson().toJson(this)
    }

    companion object {
        fun fromJson(json: String): AIPromptHistory {
            return try {
                val type = object : TypeToken<AIPromptHistory>() {}.type
                Gson().fromJson(json, type)
            } catch (e: Exception) {
                AIPromptHistory()
            }
        }
    }
}

data class SongAISettings(
    val audioEffectState: AudioEffectState,
    val prompt: String,
    val timestamp: Long
) {
    fun toJson(): String {
        return Gson().toJson(this)
    }

    companion object {
        fun fromJson(json: String): SongAISettings {
            val type = object : TypeToken<SongAISettings>() {}.type
            return Gson().fromJson(json, type)
        }
    }
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
