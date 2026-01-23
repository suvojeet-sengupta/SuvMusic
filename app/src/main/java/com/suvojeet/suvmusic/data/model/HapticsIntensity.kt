package com.suvojeet.suvmusic.data.model

/**
 * Defines the vibration intensity for Music Haptics.
 * Controls how strong the haptic feedback feels.
 */
enum class HapticsIntensity(val multiplier: Float, val displayName: String) {
    /**
     * Subtle vibrations, barely noticeable.
     * Best for quiet environments or long listening sessions.
     */
    LOW(0.3f, "Low"),
    
    /**
     * Balanced vibration intensity.
     * Default recommended setting.
     */
    MEDIUM(0.6f, "Medium"),
    
    /**
     * Strong vibrations for an immersive experience.
     * Good for bass-heavy music genres.
     */
    HIGH(0.85f, "High"),
    
    /**
     * Maximum vibration power.
     * Best for EDM, hip-hop, and dance music.
     * Note: May drain battery faster.
     */
    MAXIMUM(1.0f, "Maximum")
}
