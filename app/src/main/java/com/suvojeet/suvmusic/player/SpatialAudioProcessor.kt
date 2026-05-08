package com.suvojeet.suvmusic.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpatialAudioProcessor @Inject constructor(
    private val nativeSpatialAudio: NativeSpatialAudio,
    private val audioARManager: AudioARManager
) : BaseAudioProcessor() {

    private var directNativeBuffer: ByteBuffer? = null
    private var azimuth = 0f
    private var elevation = 0f
    
    private var isSpatialEnabled = false
    private var isLimiterEnabled = false
    private var isCrossfeedEnabled = true
    private var currentPitch = 1.0f
    @Volatile private var spatialStrength = 0.7f // 0..1; user-controlled spatial intensity

    fun setSpatialEnabled(enabled: Boolean) {
        if (isSpatialEnabled != enabled) {
            isSpatialEnabled = enabled
            nativeSpatialAudio.setSpatializerEnabled(enabled)
            updateCrossfeed()
            checkActive()
        }
    }

    fun setSpatialStrength(strength01: Float) {
        spatialStrength = strength01.coerceIn(0f, 1f)
        // Crossfeed strength scales with spatial strength when spatial is
        // off, so the "Soundstage Depth" slider keeps having an audible
        // effect for users on speakers / wired headphones.
        updateCrossfeed()
    }

    fun setCrossfeedEnabled(enabled: Boolean) {
        if (isCrossfeedEnabled != enabled) {
            isCrossfeedEnabled = enabled
            updateCrossfeed()
            checkActive()
        }
    }

    private fun updateCrossfeed() {
        // Crossfeed is active if enabled AND spatial is off (to avoid double processing).
        // Strength varies 0.05..0.30 so the slider has a perceivable but
        // bounded effect on the stereo image.
        val crossfeedStrength = (0.05f + spatialStrength * 0.25f).coerceIn(0f, 0.5f)
        nativeSpatialAudio.setCrossfeedParams(isCrossfeedEnabled && !isSpatialEnabled, crossfeedStrength)
    }
    
    fun setEqEnabled(enabled: Boolean) {
        nativeSpatialAudio.setEqEnabled(enabled)
    }
    
    fun setEqBand(bandIndex: Int, gainDb: Float) {
        nativeSpatialAudio.setEqBand(bandIndex, gainDb)
    }

    fun setEqPreamp(gainDb: Float) {
        nativeSpatialAudio.setEqPreamp(gainDb)
    }

    fun setBassBoost(strength: Float) {
        nativeSpatialAudio.setBassBoost(strength)
    }

    fun setVirtualizer(strength: Float) {
        nativeSpatialAudio.setVirtualizer(strength)
    }

    fun resetEqBands() {
        for (i in 0 until 10) {
            nativeSpatialAudio.setEqBand(i, 0f)
        }
    }
    
    /**
     * Configure the limiter / makeup gain.
     *
     * @param boostEnabled User-enabled "Volume Boost" — adds up to 12 dB
     *   makeup gain at 100% (down from the previous 15 dB ceiling that
     *   could clip on already-loud sources).
     * @param boostAmount  0..100 boost slider position.
     * @param normEnabled  User-enabled "Volume Normalization".
     * @param normGainDb   Per-track gain offset coming from
     *   [LoudnessAnalyzer]. Only used when [normEnabled] is true; if no
     *   measurement is available yet, the caller passes 0 and we fall back
     *   to a small flat boost so quiet tracks still get a nudge.
     */
    fun setLimiterConfig(
        boostEnabled: Boolean,
        boostAmount: Int,
        normEnabled: Boolean,
        normGainDb: Float = 0f,
    ) {
        val shouldEnable = boostEnabled || normEnabled

        if (shouldEnable) {
            var makeupGainDb = 0f
            if (normEnabled) {
                // Use the per-track measurement when present; fall back to a
                // mild +3 dB so the user still hears a difference on the
                // very first play of an unmeasured track.
                makeupGainDb += if (normGainDb != 0f) normGainDb else 3f
            }
            if (boostEnabled && boostAmount > 0) {
                // Cap volume boost at +12 dB (was +15 dB). Beyond this the
                // limiter has to work hard on already-mastered audio and
                // distortion becomes audible.
                makeupGainDb += (boostAmount / 100f) * 12f
            }
            // Final safety clamp — prevent the combined gain from driving
            // the limiter into a regime where it has to attenuate every
            // sample.
            makeupGainDb = makeupGainDb.coerceIn(-8f, 14f)

            // Slightly softer protection params than before so the limiter
            // colours the signal less while still preventing clipping.
            val thresholdDb = -0.3f
            val ratio = 12.0f
            val attackMs = 1.0f
            val releaseMs = 80.0f

            nativeSpatialAudio.setLimiterParams(thresholdDb, ratio, attackMs, releaseMs, makeupGainDb)
        }

        if (isLimiterEnabled != shouldEnable) {
            isLimiterEnabled = shouldEnable
            nativeSpatialAudio.setLimiterEnabled(shouldEnable)
            checkActive()
        }
    }

    fun setPlaybackParams(pitch: Float) {
        currentPitch = pitch
        nativeSpatialAudio.setPlaybackParams(pitch)
        checkActive()
    }

    fun applyAIState(state: com.suvojeet.suvmusic.ai.AudioEffectState) {
        nativeSpatialAudio.applyAIState(state)
        // Sync local flags
        isSpatialEnabled = state.isSpatialEnabled
        isCrossfeedEnabled = state.isCrossfeedEnabled
        checkActive()
    }

    fun getCurrentState(): com.suvojeet.suvmusic.ai.AudioEffectState {
        return com.suvojeet.suvmusic.ai.AudioEffectState(
            eqEnabled = nativeSpatialAudio.isEqEnabled(),
            eqBands = List(10) { nativeSpatialAudio.getEqBand(it) },
            bassBoost = nativeSpatialAudio.getBassBoost(),
            virtualizer = nativeSpatialAudio.getVirtualizer(),
            spatialEnabled = isSpatialEnabled,
            crossfeedEnabled = isCrossfeedEnabled
        )
    }
    
    fun getSignalStats(): com.suvojeet.suvmusic.ai.SignalStats {
        return com.suvojeet.suvmusic.ai.SignalStats(
            peakLevel = nativeSpatialAudio.getPeakLevel(),
            rmsLevel = nativeSpatialAudio.getRmsLevel()
        )
    }

    private fun checkActive() {
        if (!isSpatialEnabled && !isLimiterEnabled && currentPitch == 1.0f) {
            // All effects off
        }
    }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        // Accept both 16-bit and Float
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT && inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            return AudioFormat.NOT_SET
        }
        
        // Initialize effects with current state
        nativeSpatialAudio.setCrossfeedParams(isCrossfeedEnabled && !isSpatialEnabled, 0.15f)
        nativeSpatialAudio.setPlaybackParams(currentPitch)

        // Always output 16-bit PCM for maximum compatibility
        return AudioFormat(inputAudioFormat.sampleRate, inputAudioFormat.channelCount, C.ENCODING_PCM_16BIT)
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        // If processor is NOT active, just pass through (ExoPlayer should handle this via BaseAudioProcessor, 
        // but explicit handling is safer for our custom buffer management)
        if (!isActive) {
            val out = replaceOutputBuffer(remaining)
            out.put(inputBuffer)
            out.flip()
            return
        }

        val encoding = inputAudioFormat.encoding
        val channelCount = inputAudioFormat.channelCount
        val sampleRate = inputAudioFormat.sampleRate

        // Safety: Prevent division by zero or invalid formats causing silence
        if (channelCount <= 0 || sampleRate <= 0) {
            val passthrough = replaceOutputBuffer(remaining)
            passthrough.put(inputBuffer)
            passthrough.flip()
            return
        }

        val bytesPerSample = when (encoding) {
            C.ENCODING_PCM_16BIT -> 2
            C.ENCODING_PCM_FLOAT -> 4
            else -> {
                // Unknown encoding, just pass through to avoid silence
                val passthrough = replaceOutputBuffer(remaining)
                passthrough.put(inputBuffer)
                passthrough.flip()
                return
            }
        }

        val frameCount = remaining / (bytesPerSample * channelCount)
        if (frameCount <= 0) return

        // 1. Prepare output buffer (we always output PCM 16-bit as per onConfigure)
        val requiredBytes = frameCount * channelCount * 2
        val outBuffer = replaceOutputBuffer(requiredBytes)
        outBuffer.order(ByteOrder.LITTLE_ENDIAN)
        outBuffer.clear()

        // 2. Convert input to PCM 16-bit if necessary
        try {
            when (encoding) {
                C.ENCODING_PCM_16BIT -> outBuffer.put(inputBuffer)
                C.ENCODING_PCM_FLOAT -> {
                    inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
                    repeat(frameCount * channelCount) {
                        val clamped = inputBuffer.getFloat().coerceIn(-1f, 1f)
                        val sample = (clamped * 32767f).toInt().toShort()
                        outBuffer.putShort(sample)
                    }
                }
            }
        } catch (e: Exception) {
            // Conversion failed, return empty to avoid noise, but log it
            android.util.Log.e("SpatialAudioProcessor", "Buffer conversion error", e)
            return
        }
        outBuffer.flip()

        // 3. Process with Native JNI if effects are active
        val effectsActive = isSpatialEnabled || isLimiterEnabled || (isCrossfeedEnabled && !isSpatialEnabled) || currentPitch != 1.0f
        
        if (!effectsActive) {
            // No effects active, output buffer already contains the converted data
            return
        }

        // Apply balance/spatial positioning. The user-configurable
        // [spatialStrength] (0..1) attenuates the azimuth so that the
        // "soundstage depth" slider in Playback Settings has a clear
        // audible effect — at 0% the spatializer is a no-op even when
        // toggled on, at 100% we use the full π/2 sweep.
        val currentBalance = audioARManager.stereoBalance.value
        if (isSpatialEnabled) {
            azimuth = currentBalance * (Math.PI.toFloat() / 2f) * spatialStrength
            elevation = 0f
            nativeSpatialAudio.setLimiterBalance(0f)
        } else {
            azimuth = 0f
            elevation = 0f
            nativeSpatialAudio.setLimiterBalance(currentBalance)
        }

        // Use direct native buffer for JNI
        var nativeBuffer = directNativeBuffer
        if (nativeBuffer == null || nativeBuffer.capacity() < requiredBytes) {
            nativeBuffer = ByteBuffer.allocateDirect(requiredBytes).order(ByteOrder.LITTLE_ENDIAN)
            directNativeBuffer = nativeBuffer
        }
        
        try {
            nativeBuffer.clear()
            outBuffer.position(0)
            nativeBuffer.put(outBuffer)
            nativeBuffer.flip()

            // JNI Call
            nativeSpatialAudio.processPcm16(nativeBuffer, frameCount, channelCount, sampleRate, azimuth, elevation)

            // Copy processed data back to output buffer
            nativeBuffer.position(0)
            outBuffer.clear()
            outBuffer.put(nativeBuffer)
            outBuffer.flip()
        } catch (e: Exception) {
            android.util.Log.e("SpatialAudioProcessor", "Native processing error", e)
            // On error, the outBuffer already has the (unprocessed) data, 
            // so we just return it rather than silence.
            outBuffer.position(0)
            outBuffer.limit(requiredBytes)
        }
    }
}
