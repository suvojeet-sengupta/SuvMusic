package com.suvojeet.suvmusic.ai

/**
 * Fallback preset mapper for when AI API calls fail.
 * Maps common natural language prompts to predefined EQ curves and audio parameters.
 */
object AIFallbackPresets {

    data class FallbackPreset(
        val name: String,
        val eqBands: FloatArray,
        val bassBoost: Float,
        val virtualizer: Float,
        val spatialEnabled: Boolean,
        val crossfeedEnabled: Boolean,
        val limiterMakeupGain: Float,
        val limiterThresholdDb: Float,
        val limiterRatio: Float
    )

    private val presets = mapOf(
        "bass" to FallbackPreset(
            name = "Bass Boost",
            eqBands = floatArrayOf(6f, 5f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f),
            bassBoost = 0.6f,
            virtualizer = 0f,
            spatialEnabled = false,
            crossfeedEnabled = true,
            limiterMakeupGain = 2f,
            limiterThresholdDb = -3f,
            limiterRatio = 4f
        ),
        "treble" to FallbackPreset(
            name = "Treble Boost",
            eqBands = floatArrayOf(0f, 0f, 0f, 0f, 0f, 2f, 4f, 5f, 6f, 7f),
            bassBoost = 0f,
            virtualizer = 0f,
            spatialEnabled = false,
            crossfeedEnabled = true,
            limiterMakeupGain = 1.5f,
            limiterThresholdDb = -2f,
            limiterRatio = 3f
        ),
        "vocal" to FallbackPreset(
            name = "Clear Vocals",
            eqBands = floatArrayOf(-2f, -1f, 0f, 2f, 4f, 4f, 2f, 0f, -1f, -2f),
            bassBoost = 0.1f,
            virtualizer = 0.15f,
            spatialEnabled = false,
            crossfeedEnabled = true,
            limiterMakeupGain = 1f,
            limiterThresholdDb = -1f,
            limiterRatio = 3f
        ),
        "rock" to FallbackPreset(
            name = "Rock",
            eqBands = floatArrayOf(4f, 3f, 2f, -1f, -2f, -1f, 1f, 2f, 3f, 4f),
            bassBoost = 0.4f,
            virtualizer = 0.2f,
            spatialEnabled = false,
            crossfeedEnabled = true,
            limiterMakeupGain = 2.5f,
            limiterThresholdDb = -4f,
            limiterRatio = 5f
        ),
        "pop" to FallbackPreset(
            name = "Pop",
            eqBands = floatArrayOf(-1f, 1f, 2f, 3f, 2f, 0f, -1f, -1f, -1f, -1f),
            bassBoost = 0.3f,
            virtualizer = 0.1f,
            spatialEnabled = false,
            crossfeedEnabled = true,
            limiterMakeupGain = 1.5f,
            limiterThresholdDb = -2f,
            limiterRatio = 3f
        ),
        "jazz" to FallbackPreset(
            name = "Jazz",
            eqBands = floatArrayOf(3f, 2f, 1f, 2f, -1f, -1f, 0f, 1f, 2f, 3f),
            bassBoost = 0.2f,
            virtualizer = 0.15f,
            spatialEnabled = false,
            crossfeedEnabled = true,
            limiterMakeupGain = 1f,
            limiterThresholdDb = -1.5f,
            limiterRatio = 3f
        ),
        "classical" to FallbackPreset(
            name = "Classical",
            eqBands = floatArrayOf(4f, 3f, 2f, 1f, 0f, 0f, 1f, 2f, 3f, 4f),
            bassBoost = 0.1f,
            virtualizer = 0.3f,
            spatialEnabled = true,
            crossfeedEnabled = true,
            limiterMakeupGain = 1f,
            limiterThresholdDb = -1f,
            limiterRatio = 2f
        ),
        "electronic" to FallbackPreset(
            name = "Electronic",
            eqBands = floatArrayOf(5f, 4f, 1f, 0f, -2f, 2f, 1f, 3f, 4f, 5f),
            bassBoost = 0.7f,
            virtualizer = 0.4f,
            spatialEnabled = true,
            crossfeedEnabled = false,
            limiterMakeupGain = 3f,
            limiterThresholdDb = -5f,
            limiterRatio = 6f
        ),
        "spatial" to FallbackPreset(
            name = "Spatial Audio",
            eqBands = floatArrayOf(1f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 1f),
            bassBoost = 0.2f,
            virtualizer = 0.3f,
            spatialEnabled = true,
            crossfeedEnabled = true,
            limiterMakeupGain = 2f,
            limiterThresholdDb = -2f,
            limiterRatio = 4f
        ),
        "flat" to FallbackPreset(
            name = "Flat",
            eqBands = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
            bassBoost = 0f,
            virtualizer = 0f,
            spatialEnabled = false,
            crossfeedEnabled = true,
            limiterMakeupGain = 0f,
            limiterThresholdDb = -0.1f,
            limiterRatio = 4f
        )
    )

    /**
     * Keywords mapping for prompt matching.
     * Keys are keywords to search for in the prompt, values are the preset keys.
     */
    private val keywordMappings = listOf(
        listOf("bass", "bassy", "low", "boom") to "bass",
        listOf("treble", "high", "bright", "crisp") to "treble",
        listOf("vocal", "voice", "singing", "clear vocal") to "vocal",
        listOf("rock") to "rock",
        listOf("pop") to "pop",
        listOf("jazz") to "jazz",
        listOf("classical", "orchestra", "symphony") to "classical",
        listOf("electronic", "edm", "dance", "techno") to "electronic",
        listOf("spatial", "surround", "3d", "wide") to "spatial",
        listOf("flat", "reset", "default", "neutral") to "flat"
    )

    /**
     * Finds the best matching fallback preset for a given prompt.
     * Returns null if no match is found.
     */
    fun findForPrompt(prompt: String): FallbackPreset? {
        val lowerPrompt = prompt.lowercase()

        for ((keywords, presetKey) in keywordMappings) {
            if (keywords.any { lowerPrompt.contains(it) }) {
                return presets[presetKey]
            }
        }

        return null
    }

    /**
     * Converts a [FallbackPreset] to an [AudioEffectState].
     */
    fun toAudioEffectState(preset: FallbackPreset): AudioEffectState {
        return AudioEffectState(
            eqEnabled = true,
            eqBands = preset.eqBands.toList(),
            bassBoost = preset.bassBoost,
            virtualizer = preset.virtualizer,
            spatialEnabled = preset.spatialEnabled,
            crossfeedEnabled = preset.crossfeedEnabled,
            limiterMakeupGain = preset.limiterMakeupGain,
            limiterThresholdDb = preset.limiterThresholdDb,
            limiterRatio = preset.limiterRatio,
            limiterAttackMs = 5f,
            limiterReleaseMs = 100f
        )
    }
}
