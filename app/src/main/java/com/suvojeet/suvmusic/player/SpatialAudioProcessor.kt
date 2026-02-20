package com.suvojeet.suvmusic.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reusable direct ByteBuffer for JNI calls.
 * GetDirectBufferAddress requires a direct buffer; replaceOutputBuffer returns heap buffers.
 */
private var directNativeBuffer: ByteBuffer? = null

@Singleton
class SpatialAudioProcessor @Inject constructor(
    private val nativeSpatialAudio: NativeSpatialAudio,
    private val audioARManager: AudioARManager
) : BaseAudioProcessor() {

    private var azimuth = 0f
    private var elevation = 0f
    
    private var isSpatialEnabled = false
    private var isLimiterEnabled = false
    private var isCrossfeedEnabled = true
    private var currentPitch = 1.0f

    fun setSpatialEnabled(enabled: Boolean) {
        if (isSpatialEnabled != enabled) {
            isSpatialEnabled = enabled
            nativeSpatialAudio.setSpatializerEnabled(enabled)
            updateCrossfeed()
            checkActive()
        }
    }

    fun setCrossfeedEnabled(enabled: Boolean) {
        if (isCrossfeedEnabled != enabled) {
            isCrossfeedEnabled = enabled
            updateCrossfeed()
            checkActive()
        }
    }

    private fun updateCrossfeed() {
        // Crossfeed is active if enabled AND spatial is off (to avoid double processing)
        nativeSpatialAudio.setCrossfeedParams(isCrossfeedEnabled && !isSpatialEnabled, 0.15f)
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
    
    fun setLimiterConfig(boostEnabled: Boolean, boostAmount: Int, normEnabled: Boolean) {
        val shouldEnable = boostEnabled || normEnabled
        
        if (shouldEnable) {
             var makeupGainDb = 0f
             if (normEnabled) makeupGainDb += 5.5f
             if (boostEnabled && boostAmount > 0) {
                 makeupGainDb += (boostAmount / 100f) * 15f
             }
             
             // Hard limiter params for protection
             val thresholdDb = -0.1f
             val ratio = 20.0f
             val attackMs = 0.1f
             val releaseMs = 50.0f
             
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

        // Apply balance/spatial positioning
        val currentBalance = audioARManager.stereoBalance.value
        if (isSpatialEnabled) {
            azimuth = currentBalance * (Math.PI.toFloat() / 2f)
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
