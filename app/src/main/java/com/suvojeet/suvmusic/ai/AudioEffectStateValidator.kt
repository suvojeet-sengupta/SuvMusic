package com.suvojeet.suvmusic.ai

/**
 * Validates and sanitizes [AudioEffectState] returned from AI providers.
 * Ensures all values are within safe hardware bounds before applying to the audio engine.
 */
object AudioEffectStateValidator {

    // Safe ranges for each parameter
    private const val EQ_BAND_MIN = -12f
    private const val EQ_BAND_MAX = 12f
    private const val BASS_BOOST_MIN = 0f
    private const val BASS_BOOST_MAX = 1f
    private const val VIRTUALIZER_MIN = 0f
    private const val VIRTUALIZER_MAX = 1f
    private const val LIMITER_THRESHOLD_MIN = -24f
    private const val LIMITER_THRESHOLD_MAX = 0f
    private const val LIMITER_RATIO_MIN = 1f
    private const val LIMITER_RATIO_MAX = 20f
    private const val LIMITER_ATTACK_MIN = 0.1f
    private const val LIMITER_ATTACK_MAX = 100f
    private const val LIMITER_RELEASE_MIN = 10f
    private const val LIMITER_RELEASE_MAX = 1000f
    private const val LIMITER_MAKEUP_GAIN_MIN = 0f
    private const val LIMITER_MAKEUP_GAIN_MAX = 12f

    private const val EQ_BAND_COUNT = 10

    data class ValidationResult(
        val isValid: Boolean,
        val sanitizedState: AudioEffectState,
        val warnings: List<String> = emptyList()
    )

    /**
     * Validates an [AudioEffectState] and returns a sanitized version with clamped values.
     * Returns warnings for any values that were out of bounds.
     */
    fun validate(state: AudioEffectState): ValidationResult {
        val warnings = mutableListOf<String>()

        // Validate EQ bands
        val rawBands = state.safeEqBands
        if (rawBands.size != EQ_BAND_COUNT) {
            warnings.add("EQ bands count mismatch: expected $EQ_BAND_COUNT, got ${rawBands.size}")
        }

        val sanitizedBands = rawBands.take(EQ_BAND_COUNT).mapIndexed { index, value ->
            clamp(value, EQ_BAND_MIN, EQ_BAND_MAX).also { clamped ->
                if (clamped != value) {
                    warnings.add("EQ band $index clamped: ${"%.1f".format(value)} -> ${"%.1f".format(clamped)} dB")
                }
            }
        }

        // Validate bass boost
        val rawBass = state.safeBassBoost
        val sanitizedBass = clamp(rawBass, BASS_BOOST_MIN, BASS_BOOST_MAX)
        if (sanitizedBass != rawBass) {
            warnings.add("Bass boost clamped: ${"%.2f".format(rawBass)} -> ${"%.2f".format(sanitizedBass)}")
        }

        // Validate virtualizer
        val rawVirt = state.safeVirtualizer
        val sanitizedVirt = clamp(rawVirt, VIRTUALIZER_MIN, VIRTUALIZER_MAX)
        if (sanitizedVirt != rawVirt) {
            warnings.add("Virtualizer clamped: ${"%.2f".format(rawVirt)} -> ${"%.2f".format(sanitizedVirt)}")
        }

        // Validate limiter parameters
        val rawThreshold = state.limiterThresholdDb
        val sanitizedThreshold = clamp(rawThreshold, LIMITER_THRESHOLD_MIN, LIMITER_THRESHOLD_MAX)
        if (sanitizedThreshold != rawThreshold) {
            warnings.add("Limiter threshold clamped: ${"%.1f".format(rawThreshold)} -> ${"%.1f".format(sanitizedThreshold)} dB")
        }

        val rawRatio = state.limiterRatio
        val sanitizedRatio = clamp(rawRatio, LIMITER_RATIO_MIN, LIMITER_RATIO_MAX)
        if (sanitizedRatio != rawRatio) {
            warnings.add("Limiter ratio clamped: ${"%.1f".format(rawRatio)} -> ${"%.1f".format(sanitizedRatio)}")
        }

        val rawAttack = state.limiterAttackMs
        val sanitizedAttack = clamp(rawAttack, LIMITER_ATTACK_MIN, LIMITER_ATTACK_MAX)
        if (sanitizedAttack != rawAttack) {
            warnings.add("Limiter attack clamped: ${"%.1f".format(rawAttack)} -> ${"%.1f".format(sanitizedAttack)} ms")
        }

        val rawRelease = state.limiterReleaseMs
        val sanitizedRelease = clamp(rawRelease, LIMITER_RELEASE_MIN, LIMITER_RELEASE_MAX)
        if (sanitizedRelease != rawRelease) {
            warnings.add("Limiter release clamped: ${"%.1f".format(rawRelease)} -> ${"%.1f".format(sanitizedRelease)} ms")
        }

        val rawMakeup = state.safeLimiterMakeupGain
        val sanitizedMakeup = clamp(rawMakeup, LIMITER_MAKEUP_GAIN_MIN, LIMITER_MAKEUP_GAIN_MAX)
        if (sanitizedMakeup != rawMakeup) {
            warnings.add("Limiter makeup gain clamped: ${"%.1f".format(rawMakeup)} -> ${"%.1f".format(sanitizedMakeup)} dB")
        }

        // Check for suspicious all-zero state
        val allZeroBands = sanitizedBands.all { it == 0f }
        if (allZeroBands && sanitizedBass == 0f && sanitizedVirt == 0f) {
            warnings.add("WARNING: AI returned flat response - no audio changes applied")
        }

        val sanitizedState = AudioEffectState(
            eqEnabled = state.isEqEnabled,
            eqBands = sanitizedBands,
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
            isValid = warnings.none { it.startsWith("WARNING") },
            sanitizedState = sanitizedState,
            warnings = warnings
        )
    }

    private fun clamp(value: Float, min: Float, max: Float): Float {
        return when {
            value < min -> min
            value > max -> max
            else -> value
        }
    }
}
