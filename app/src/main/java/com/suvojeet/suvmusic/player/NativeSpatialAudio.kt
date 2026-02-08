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
}
