package com.suvojeet.suvmusic.ai

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Validates and sanitizes [AudioEffectState] returned from AI providers.
 * Ensures all values are within safe hardware bounds before applying to the audio engine.
 * 
 * IMPROVEMENTS:
 * 1. EQ Normalization: Prevents digital clipping by shifting the entire curve down if net gain is too high.
 * 2. Curve Smoothing: Detects and corrects extreme jumps between adjacent bands.
 * 3. Cross-Parameter Safety: Prevents over-saturation when Bass Boost and Sub-bass EQ are both active.
 * 4. Limiter Relationship: Ensures attack and release times are acoustically sensible.
 */
object AudioEffectStateValidator {

    // Safe ranges for each parameter
    private const val EQ_BAND_MIN = -12f
    private const val EQ_BAND_MAX = 12f
    private const val BASS_BOOST_MIN = 0f
    private const val BASS_BOOST_MAX = 1f
    private const val VIRTUALIZER_MIN = 0f
    private const val VIRTUALIZER_MAX = 1f
    private const val LIMITER_THRESHOLD_MIN = -36f
    private const val LIMITER_THRESHOLD_MAX = 0f
    private const val LIMITER_RATIO_MIN = 1f
    private const val LIMITER_RATIO_MAX = 20f
    private const val LIMITER_ATTACK_MIN = 0.05f
    private const val LIMITER_ATTACK_MAX = 200f
    private const val LIMITER_RELEASE_MIN = 5f
    private const val LIMITER_RELEASE_MAX = 2000f
    private const val LIMITER_MAKEUP_GAIN_MIN = 0f
    private const val LIMITER_MAKEUP_GAIN_MAX = 15f

    // Smart Constraints
    private const val MAX_ADJACENT_DIFF = 10f // Max dB difference between adjacent bands to prevent phase artifacts
    private const val EQ_BAND_COUNT = 10

    data class ValidationResult(
        val isValid: Boolean,
        val sanitizedState: AudioEffectState,
        val warnings: List<String> = emptyList()
    )

    /**
     * Validates an [AudioEffectState] and returns a sanitized version with clamped and normalized values.
     */
    fun validate(state: AudioEffectState): ValidationResult {
        val warnings = mutableListOf<String>()

        // 1. Initial Clamping & Basic Validation
        var rawBands = state.safeEqBands.take(EQ_BAND_COUNT).toMutableList()
        if (rawBands.size < EQ_BAND_COUNT) {
            val padding = List(EQ_BAND_COUNT - rawBands.size) { 0f }
            rawBands.addAll(padding)
        }

        // Apply basic range clamping
        rawBands = rawBands.mapIndexed { index, value ->
            if (value.isNaN() || value.isInfinite()) {
                warnings.add("EQ band $index was invalid (NaN/Inf), reset to 0")
                0f
            } else {
                clamp(value, EQ_BAND_MIN, EQ_BAND_MAX).also { clamped ->
                    if (abs(clamped - value) > 0.01f) {
                        warnings.add("EQ band $index clamped: ${"%.1f".format(value)} -> ${"%.1f".format(clamped)} dB")
                    }
                }
            }
        }.toMutableList()

        // 2. EQ Curve Smoothing (Prevent drastic phase shifts)
        for (i in 0 until rawBands.size - 1) {
            val current = rawBands[i]
            val next = rawBands[i + 1]
            if (abs(current - next) > MAX_ADJACENT_DIFF) {
                val corrected = if (next > current) current + MAX_ADJACENT_DIFF else current - MAX_ADJACENT_DIFF
                warnings.add("Smoothed erratic jump between bands $i and ${i+1}")
                rawBands[i + 1] = corrected
            }
        }

        // 3. EQ Normalization (Negative EQ Strategy)
        // If the AI boosts everything, it's just increasing volume (bad for headroom).
        // We shift the curve so the max boost is more reasonable.
        val maxBoost = rawBands.maxOrNull() ?: 0f
        var normalizationOffset = 0f
        if (maxBoost > 3f) {
            // If any band is boosted > 3dB, we shift everything down slightly to preserve headroom
            normalizationOffset = (maxBoost - 3f) * 0.5f 
            rawBands = rawBands.map { it - normalizationOffset }.toMutableList()
            if (normalizationOffset > 0.1f) {
                warnings.add("EQ normalized to preserve headroom (-${"%.1f".format(normalizationOffset)} dB)")
            }
        }

        // 4. Cross-Parameter: Bass Safety
        var sanitizedBass = clamp(state.safeBassBoost, BASS_BOOST_MIN, BASS_BOOST_MAX)
        val lowFreqBoost = (rawBands[0] + rawBands[1]) / 2f // Average of 31Hz and 62Hz
        
        if (lowFreqBoost > 6f && sanitizedBass > 0.7f) {
            val oldBass = sanitizedBass
            sanitizedBass = 0.7f // Cap bass boost if EQ is already heavy on bass
            warnings.add("Bass boost reduced to prevent sub-woofer saturation")
        }

        // 5. Virtualizer & Others
        val sanitizedVirt = clamp(state.safeVirtualizer, VIRTUALIZER_MIN, VIRTUALIZER_MAX)
        
        // 6. Limiter Logic (Professional Tuning)
        val rawThreshold = state.limiterThresholdDb
        val sanitizedThreshold = clamp(rawThreshold, LIMITER_THRESHOLD_MIN, LIMITER_THRESHOLD_MAX)

        val rawRatio = state.limiterRatio
        val sanitizedRatio = clamp(rawRatio, LIMITER_RATIO_MIN, LIMITER_RATIO_MAX)

        var sanitizedAttack = clamp(state.limiterAttackMs, LIMITER_ATTACK_MIN, LIMITER_ATTACK_MAX)
        var sanitizedRelease = clamp(state.limiterReleaseMs, LIMITER_RELEASE_MIN, LIMITER_RELEASE_MAX)

        // Ensure release is at least 10x attack for transparency
        if (sanitizedRelease < sanitizedAttack * 10f) {
            sanitizedRelease = min(LIMITER_RELEASE_MAX, sanitizedAttack * 10f)
            warnings.add("Limiter release adjusted for acoustic transparency")
        }

        val rawMakeup = state.safeLimiterMakeupGain
        // Compensate makeup gain slightly for our normalization offset if there's headroom
        val suggestedMakeup = rawMakeup + (normalizationOffset * 0.5f)
        val sanitizedMakeup = clamp(suggestedMakeup, LIMITER_MAKEUP_GAIN_MIN, LIMITER_MAKEUP_GAIN_MAX)

        // 7. Detection of generic/unresponsive states
        val isFlat = rawBands.all { abs(it) < 0.1f }
        if (isFlat && sanitizedBass < 0.05f && sanitizedVirt < 0.05f) {
            warnings.add("AI suggests flat response (Neutral)")
        }

        val sanitizedState = AudioEffectState(
            eqEnabled = state.isEqEnabled,
            eqBands = rawBands.toFloatArray(),
            bassBoost = sanitizedBass,
            virtualizer = sanitizedVirt,
            spatialEnabled = state.isSpatialEnabled,
            crossfeedEnabled = state.isCrossfeedEnabled,
            limiterMakeupGain = sanitizedMakeup,
            _limiterThresholdDb = sanitizedThreshold,
            _limiterRatio = sanitizedRatio,
            _limiterAttackMs = sanitizedAttack,
            _limiterReleaseMs = sanitizedRelease
        )

        return ValidationResult(
            isValid = true, // We always try to make it valid via sanitization
            sanitizedState = sanitizedState,
            warnings = warnings
        )
    }

    private fun clamp(value: Float, minVal: Float, maxVal: Float): Float {
        return if (value.isNaN()) minVal else max(minVal, min(maxVal, value))
    }
}
