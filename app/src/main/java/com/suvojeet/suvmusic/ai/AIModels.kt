package com.suvojeet.suvmusic.ai

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

import com.google.gson.annotations.Expose

enum class AIProvider {
    OPENAI,
    ANTHROPIC,
    GEMINI,
    CHAT_PROXY
}

/**
 * Robust data class for Audio Effects.
 * Annotated for ProGuard/Gson safety.
 */
data class AudioEffectState(
    @SerializedName("eqEnabled") @Expose val eqEnabled: Boolean? = true,
    @SerializedName("eqBands") @Expose val eqBands: FloatArray? = null,
    @SerializedName("bassBoost") @Expose val bassBoost: Float? = 0f,
    @SerializedName("virtualizer") @Expose val virtualizer: Float? = 0f,
    @SerializedName("spatialEnabled") @Expose val spatialEnabled: Boolean? = false,
    @SerializedName("crossfeedEnabled") @Expose val crossfeedEnabled: Boolean? = true,
    @SerializedName("limiterMakeupGain") @Expose val limiterMakeupGain: Float? = 0f,
    @SerializedName("limiterThresholdDb") @Expose val _limiterThresholdDb: Float? = -0.1f,
    @SerializedName("limiterRatio") @Expose val _limiterRatio: Float? = 4.0f,
    @SerializedName("limiterAttackMs") @Expose val _limiterAttackMs: Float? = 5.0f,
    @SerializedName("limiterReleaseMs") @Expose val _limiterReleaseMs: Float? = 100.0f
) {
    val isEqEnabled get() = eqEnabled ?: true
    val safeEqBands: List<Float> get() = eqBands?.toList() ?: List(10) { 0f }
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AudioEffectState
        if (eqEnabled != other.eqEnabled) return false
        if (eqBands != null) {
            if (other.eqBands == null) return false
            if (!eqBands.contentEquals(other.eqBands)) return false
        } else if (other.eqBands != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = eqEnabled?.hashCode() ?: 0
        result = 31 * result + (eqBands?.contentHashCode() ?: 0)
        return result
    }

    companion object {
        fun fromJson(json: String): AudioEffectState {
            return try {
                Gson().fromJson(json, AudioEffectState::class.java) ?: AudioEffectState()
            } catch (e: Exception) {
                // If it fails with LinkedTreeMap cast error, it might be due to generic erasure
                // but for a concrete class like AudioEffectState, this is rare.
                AudioEffectState()
            }
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
                val result: AIPromptHistory = Gson().fromJson(json, type)
                result
            } catch (e: Exception) {
                // Return default state if deserialization fails (e.g., ClassCastException)
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
        fun fromJson(json: String): SongAISettings? {
            return try {
                val type = object : TypeToken<SongAISettings>() {}.type
                val result: SongAISettings = Gson().fromJson(json, type)
                result
            } catch (e: Exception) {
                // Return null if deserialization fails
                null
            }
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
