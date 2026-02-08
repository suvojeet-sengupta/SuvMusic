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

    fun setSpatialEnabled(enabled: Boolean) {
        if (isSpatialEnabled != enabled) {
            isSpatialEnabled = enabled
            nativeSpatialAudio.setSpatializerEnabled(enabled)
            checkActive()
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
             val attackMs = 5.0f
             val releaseMs = 50.0f
             
             nativeSpatialAudio.setLimiterParams(thresholdDb, ratio, attackMs, releaseMs, makeupGainDb)
        }
        
        if (isLimiterEnabled != shouldEnable) {
            isLimiterEnabled = shouldEnable
            nativeSpatialAudio.setLimiterEnabled(shouldEnable)
            checkActive()
        }
    }
    
    private fun checkActive() {
        if (!isSpatialEnabled && !isLimiterEnabled) {
            // If both off, maybe reset?
            // nativeSpatialAudio.reset() // Optional, keeps buffers clear
        }
    }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        // Accept both 16-bit and Float
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT && inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            return AudioFormat.NOT_SET
        }
        // Always output 16-bit PCM for maximum compatibility
        return AudioFormat(inputAudioFormat.sampleRate, inputAudioFormat.channelCount, C.ENCODING_PCM_16BIT)
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0 || !isActive) return

        // Prepare Float Array for processing
        val floatArray: FloatArray
        if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) {
            val sampleCount = remaining / 2
            floatArray = FloatArray(sampleCount)
            for (i in 0 until sampleCount) {
                floatArray[i] = inputBuffer.getShort() / 32768f
            }
        } else {
            val sampleCount = remaining / 4
            floatArray = FloatArray(sampleCount)
            inputBuffer.asFloatBuffer().get(floatArray)
            inputBuffer.position(inputBuffer.position() + remaining)
        }

        // Process if enabled
        if (isSpatialEnabled || isLimiterEnabled) {
            // Get latest rotation from manager
            val currentBalance = audioARManager.stereoBalance.value
            
            if (isSpatialEnabled) {
                 azimuth = currentBalance * (Math.PI.toFloat() / 2f)
                 nativeSpatialAudio.setLimiterBalance(0f)
            } else {
                 azimuth = 0f
                 elevation = 0f
                 nativeSpatialAudio.setLimiterBalance(currentBalance)
            }
            
            // Native process (in-place)
            nativeSpatialAudio.process(floatArray, azimuth, elevation, inputAudioFormat.sampleRate)
        }

        // Convert back to 16-bit PCM for Output
        val outBuffer = replaceOutputBuffer(floatArray.size * 2) // 2 bytes per sample
        for (sample in floatArray) {
            val clamped = sample.coerceIn(-1.0f, 1.0f)
            val shortSample = (clamped * 32767f).toInt().toShort()
            outBuffer.putShort(shortSample)
        }
        outBuffer.flip()
    }
}
