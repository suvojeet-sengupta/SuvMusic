package com.suvojeet.suvmusic.ai

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

import com.google.gson.annotations.Expose

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@Serializable
enum class AIProvider {
    OPENAI,
    ANTHROPIC,
    GEMINI,
    CHAT_PROXY
}

/**
 * Robust data class for Audio Effects.
 * Annotated for ProGuard/Gson safety AND Kotlinx Serialization.
 */
@Serializable
data class AudioEffectState(
    @SerializedName("eqEnabled") @Expose val eqEnabled: Boolean? = true,
    @SerializedName("eqBands") @Expose val eqBands: List<Float>? = null,
    @SerializedName("bassBoost") @Expose val bassBoost: Float? = 0f,
    @SerializedName("virtualizer") @Expose val virtualizer: Float? = 0f,
    @SerializedName("spatialEnabled") @Expose val spatialEnabled: Boolean? = false,
    @SerializedName("crossfeedEnabled") @Expose val crossfeedEnabled: Boolean? = true,
    @SerializedName("limiterMakeupGain") @Expose val limiterMakeupGain: Float? = 0f,
    @SerializedName("limiterThresholdDb") @Expose val limiterThresholdDb: Float? = -0.1f,
    @SerializedName("limiterRatio") @Expose val limiterRatio: Float? = 4.0f,
    @SerializedName("limiterAttackMs") @Expose val limiterAttackMs: Float? = 5.0f,
    @SerializedName("limiterReleaseMs") @Expose val limiterReleaseMs: Float? = 100.0f
) {
    val isEqEnabled get() = eqEnabled ?: true
    val safeEqBands: List<Float> get() = eqBands ?: List(10) { 0f }
    val safeBassBoost get() = bassBoost ?: 0f
    val safeVirtualizer get() = virtualizer ?: 0f
    val isSpatialEnabled get() = spatialEnabled ?: false
    val isCrossfeedEnabled get() = crossfeedEnabled ?: true
    val safeLimiterMakeupGain get() = limiterMakeupGain ?: 0f
    val safeLimiterThresholdDb get() = limiterThresholdDb ?: -0.1f
    val safeLimiterRatio get() = limiterRatio ?: 4.0f
    val safeLimiterAttackMs get() = limiterAttackMs ?: 5.0f
    val safeLimiterReleaseMs get() = limiterReleaseMs ?: 100.0f

    fun toJson(): String {
        return try {
            Json.encodeToString(this)
        } catch (e: Exception) {
            Gson().toJson(this)
        }
    }

    companion object {
        fun fromJson(json: String): AudioEffectState {
            if (json.isBlank()) return AudioEffectState()
            return try {
                Json.decodeFromString<AudioEffectState>(json)
            } catch (e: Exception) {
                try {
                    Gson().fromJson(json, AudioEffectState::class.java) ?: AudioEffectState()
                } catch (e2: Exception) {
                    AudioEffectState()
                }
            }
        }
    }
}

@Serializable
data class PromptHistoryEntry(
    val prompt: String,
    val timestamp: Long,
    val songId: String?,
    val songTitle: String?
)

@Serializable
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
        return try {
            Json.encodeToString(this)
        } catch (e: Exception) {
            Gson().toJson(this)
        }
    }

    companion object {
        fun fromJson(json: String): AIPromptHistory {
            if (json.isBlank()) return AIPromptHistory()
            return try {
                Json.decodeFromString<AIPromptHistory>(json)
            } catch (e: Exception) {
                try {
                    val type = object : TypeToken<AIPromptHistory>() {}.type
                    Gson().fromJson(json, type) ?: AIPromptHistory()
                } catch (e2: Exception) {
                    AIPromptHistory()
                }
            }
        }
    }
}

@Serializable
data class SongAISettings(
    val audioEffectState: AudioEffectState,
    val prompt: String,
    val timestamp: Long
) {
    fun toJson(): String {
        return try {
            Json.encodeToString(this)
        } catch (e: Exception) {
            Gson().toJson(this)
        }
    }

    companion object {
        fun fromJson(json: String): SongAISettings? {
            if (json.isBlank()) return null
            return try {
                Json.decodeFromString<SongAISettings>(json)
            } catch (e: Exception) {
                try {
                    val type = object : TypeToken<SongAISettings>() {}.type
                    Gson().fromJson(json, type)
                } catch (e2: Exception) {
                    null
                }
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
