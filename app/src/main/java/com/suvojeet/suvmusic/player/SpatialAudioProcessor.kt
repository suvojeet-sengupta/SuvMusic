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
    private var isEnabled = false

    fun setEnabled(enabled: Boolean) {
        if (isEnabled != enabled) {
            isEnabled = enabled
            if (!enabled) {
                nativeSpatialAudio.reset()
            }
        }
    }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT && inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            return AudioFormat.NOT_SET
        }
        // We prefer Float processing for better quality and easier math
        return AudioFormat(inputAudioFormat.sampleRate, inputAudioFormat.channelCount, C.ENCODING_PCM_FLOAT)
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!isEnabled) {
            replaceOutputBuffer(inputBuffer.remaining())
            outputBuffer!!.put(inputBuffer)
            outputBuffer!!.flip()
            return
        }

        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        // Get latest rotation from manager
        azimuth = audioARManager.stereoBalance.value * (Math.PI.toFloat() / 2f) // Map -1..1 to -PI/2..PI/2
        
        // Convert input to float array for native processing
        // Note: inputBuffer is likely PCM_FLOAT because of onConfigure return value
        val floatCount = remaining / 4
        val floatArray = FloatArray(floatCount)
        inputBuffer.asFloatBuffer().get(floatArray)

        // Native process
        nativeSpatialAudio.process(floatArray, azimuth, elevation, inputAudioFormat.sampleRate)

        // Write back to output
        val outBuffer = replaceOutputBuffer(remaining)
        outBuffer.asFloatBuffer().put(floatArray)
        outBuffer.limit(remaining)
        outBuffer.position(0)
    }
}
