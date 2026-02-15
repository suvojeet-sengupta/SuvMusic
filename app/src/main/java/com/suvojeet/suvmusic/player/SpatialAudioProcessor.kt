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
        if (remaining == 0 || !isActive) return

        val encoding = inputAudioFormat.encoding
        val channelCount = inputAudioFormat.channelCount
        val sampleRate = inputAudioFormat.sampleRate

        if (channelCount <= 0 || sampleRate <= 0) {
            val passthrough = replaceOutputBuffer(remaining)
            passthrough.put(inputBuffer)
            passthrough.flip()
            return
        }

        val bytesPerSample = when (encoding) {
            C.ENCODING_PCM_16BIT -> 2
            C.ENCODING_PCM_FLOAT -> 4
            else -> return
        }

        val frameCount = remaining / (bytesPerSample * channelCount)
        if (frameCount <= 0) return

    val outBuffer = replaceOutputBuffer(frameCount * channelCount * 2)
    outBuffer.order(ByteOrder.LITTLE_ENDIAN)
        outBuffer.clear()
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
        outBuffer.flip()

        val effectsActive = isSpatialEnabled || isLimiterEnabled || (isCrossfeedEnabled && !isSpatialEnabled)
        if (!effectsActive) {
            return
        }

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

        val nativeBuffer = outBuffer.duplicate().apply {
            position(0)
            limit(frameCount * channelCount * 2)
        }

        nativeSpatialAudio.processPcm16(nativeBuffer, frameCount, channelCount, sampleRate, azimuth, elevation)

        outBuffer.position(0)
        outBuffer.limit(frameCount * channelCount * 2)
    }
}
