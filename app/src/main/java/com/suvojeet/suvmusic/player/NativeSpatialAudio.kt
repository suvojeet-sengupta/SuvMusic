package com.suvojeet.suvmusic.player

import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NativeSpatialAudio @Inject constructor() {
    private var isLibraryLoaded = false

    init {
        try {
            System.loadLibrary("suvmusic_native")
            isLibraryLoaded = true
        } catch (e: UnsatisfiedLinkError) {
            android.util.Log.e("NativeSpatialAudio", "Failed to load native library", e)
        }
    }

    /**
     * Process audio buffer with spatial audio effects.
     */
    fun process(buffer: FloatArray, azimuth: Float, elevation: Float, sampleRate: Int) {
        if (isLibraryLoaded) {
            nProcess(buffer, azimuth, elevation, sampleRate)
        }
    }

    fun processPcm16(buffer: ByteBuffer, frameCount: Int, channelCount: Int, sampleRate: Int, azimuth: Float, elevation: Float) {
        if (!isLibraryLoaded) return
        if (!buffer.isDirect) {
            throw IllegalArgumentException("processPcm16 requires a direct ByteBuffer")
        }
        nProcessPcm16(buffer, frameCount, channelCount, sampleRate, azimuth, elevation)
    }

    private external fun nProcess(buffer: FloatArray, azimuth: Float, elevation: Float, sampleRate: Int)
    private external fun nProcessPcm16(
        buffer: ByteBuffer,
        frameCount: Int,
        channelCount: Int,
        sampleRate: Int,
        azimuth: Float,
        elevation: Float
    )

    /**
     * Reset the internal state of the spatializer.
     */
    fun reset() {
        if (isLibraryLoaded) {
            nReset()
        }
    }

    private external fun nReset()

    fun setSpatializerEnabled(enabled: Boolean) {
        if (isLibraryLoaded) {
            nSetSpatializerEnabled(enabled)
        }
    }

    private external fun nSetSpatializerEnabled(enabled: Boolean)

    fun setLimiterEnabled(enabled: Boolean) {
        if (isLibraryLoaded) {
            nSetLimiterEnabled(enabled)
        }
    }

    private external fun nSetLimiterEnabled(enabled: Boolean)

    fun setLimiterParams(thresholdDb: Float, ratio: Float, attackMs: Float, releaseMs: Float, makeupGainDb: Float) {
        if (isLibraryLoaded) {
            nSetLimiterParams(thresholdDb, ratio, attackMs, releaseMs, makeupGainDb)
        }
    }

    private external fun nSetLimiterParams(thresholdDb: Float, ratio: Float, attackMs: Float, releaseMs: Float, makeupGainDb: Float)

    fun setLimiterBalance(balance: Float) {
        if (isLibraryLoaded) {
            nSetLimiterBalance(balance)
        }
    }

    private external fun nSetLimiterBalance(balance: Float)

    fun setCrossfeedParams(enabled: Boolean, strength: Float) {
        if (isLibraryLoaded) {
            nSetCrossfeedParams(enabled, strength)
        }
    }

    private external fun nSetCrossfeedParams(enabled: Boolean, strength: Float)

    fun setEqEnabled(enabled: Boolean) {
        if (isLibraryLoaded) {
            nSetEqEnabled(enabled)
        }
    }

    private external fun nSetEqEnabled(enabled: Boolean)

    fun setEqBand(bandIndex: Int, gainDb: Float) {
        if (isLibraryLoaded) {
            nSetEqBand(bandIndex, gainDb)
        }
    }

    private external fun nSetEqBand(bandIndex: Int, gainDb: Float)

    fun setPlaybackParams(speed: Float, pitch: Float) {
        if (isLibraryLoaded) {
            nSetPlaybackParams(speed, pitch)
        }
    }

    private external fun nSetPlaybackParams(speed: Float, pitch: Float)

    /**
     * Extracts waveform data from a file using high-performance Memory-Mapped IO (mmap).
     * @param filePath Path to the local file.
     * @param numPoints Number of points to return in the waveform.
     * @return FloatArray of peak amplitudes.
     */
    fun extractWaveform(filePath: String, numPoints: Int): FloatArray? {
        return if (isLibraryLoaded) {
            nExtractWaveform(filePath, numPoints)
        } else null
    }

    private external fun nExtractWaveform(filePath: String, numPoints: Int): FloatArray?
}
