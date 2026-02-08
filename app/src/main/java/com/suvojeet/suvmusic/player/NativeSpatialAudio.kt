package com.suvojeet.suvmusic.player

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NativeSpatialAudio @Inject constructor() {
    init {
        System.loadLibrary("suvmusic_native")
    }

    /**
     * Process audio buffer with spatial audio effects.
     * @param buffer Input/Output audio buffer (float array)
     * @param azimuth Azimuth in radians (-PI to PI)
     * @param elevation Elevation in radians (-PI/2 to PI/2)
     * @param sampleRate The audio sample rate (e.g., 44100)
     */
    external fun process(buffer: FloatArray, azimuth: Float, elevation: Float, sampleRate: Int)

    /**
     * Reset the internal state of the spatializer.
     */
    external fun reset()
}
