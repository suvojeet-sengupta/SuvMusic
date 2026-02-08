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
        // Accept both 16-bit and Float, but always output Float for high-quality C++ processing
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT && inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            return AudioFormat.NOT_SET
        }
        return AudioFormat(inputAudioFormat.sampleRate, inputAudioFormat.channelCount, C.ENCODING_PCM_FLOAT)
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0 || !isActive) return

        if (!isSpatialEnabled && !isLimiterEnabled) {
            // Passthrough logic needs to handle format conversion if input is 16-bit but we promised Float output
            if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) {
                // Convert to Float for consistent output format
                val sampleCount = remaining / 2
                val outBuffer = replaceOutputBuffer(sampleCount * 4)
                
                for (i in 0 until sampleCount) {
                    val sample16 = inputBuffer.short
                    outBuffer.putFloat(sample16 / 32768f)
                }
                outBuffer.flip()
            } else {
                // Already Float, just copy
                val outBuffer = replaceOutputBuffer(remaining)
                outBuffer.put(inputBuffer)
                outBuffer.flip()
            }
            return
        }

        // Get latest rotation from manager
        val currentBalance = audioARManager.stereoBalance.value
        
        if (isSpatialEnabled) {
             azimuth = currentBalance * (Math.PI.toFloat() / 2f) // Map -1..1 to -PI/2..PI/2
             nativeSpatialAudio.setLimiterBalance(0f)
        } else {
             azimuth = 0f
             elevation = 0f
             nativeSpatialAudio.setLimiterBalance(currentBalance)
        }
        
        // Prepare Float Array
        val floatArray: FloatArray
        if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) {
            val sampleCount = remaining / 2
            floatArray = FloatArray(sampleCount)
            for (i in 0 until sampleCount) {
                floatArray[i] = inputBuffer.short / 32768f
            }
        } else {
            val sampleCount = remaining / 4
            floatArray = FloatArray(sampleCount)
            inputBuffer.asFloatBuffer().get(floatArray)
            // Move position of inputBuffer (asFloatBuffer doesn't affect original buffer pos automatically? 
            // actually asFloatBuffer().get() advances the float buffer, but not the byte buffer? 
            // documentation says: "The new buffer's position will be zero, its capacity and its limit will be the number of bytes remaining in this buffer divided by four".
            // It shares content.
            // We need to advance inputBuffer manually? Or just inputBuffer.position(inputBuffer.limit())?
            // "The position of this buffer is not changed by this method." (asFloatBuffer)
            // But getting from the view...
            // Safest to just set position to limit after reading.
            inputBuffer.position(inputBuffer.position() + remaining)
        }

        // Native process
        nativeSpatialAudio.process(floatArray, azimuth, elevation, inputAudioFormat.sampleRate)

        // Write back to output
        val outBuffer = replaceOutputBuffer(floatArray.size * 4)
        outBuffer.asFloatBuffer().put(floatArray)
        outBuffer.limit(floatArray.size * 4)
        outBuffer.position(0)
    }
}
