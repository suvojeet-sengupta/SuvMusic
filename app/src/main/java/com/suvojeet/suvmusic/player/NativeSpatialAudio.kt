package com.suvojeet.suvmusic.player

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

    private external fun nProcess(buffer: FloatArray, azimuth: Float, elevation: Float, sampleRate: Int)

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
}
