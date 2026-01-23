package com.suvojeet.suvmusic.data.model

/**
 * Defines the haptic feedback mode for Music Haptics feature.
 * Similar to Apple Music's Music Haptics on iOS.
 */
enum class HapticsMode {
    /**
     * Haptics completely disabled.
     */
    OFF,
    
    /**
     * Basic mode - responds only to strong bass hits and obvious beats.
     * Less battery consumption, suitable for most genres.
     */
    BASIC,
    
    /**
     * Advanced mode - full audio spectrum analysis.
     * More responsive, detects subtle rhythms and textures.
     * Higher battery consumption.
     */
    ADVANCED,
    
    /**
     * Custom mode - user-defined sensitivity and intensity.
     * Allows fine-tuning for personal preference.
     */
    CUSTOM
}
