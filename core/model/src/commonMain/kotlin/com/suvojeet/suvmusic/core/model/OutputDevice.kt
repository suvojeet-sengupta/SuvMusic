package com.suvojeet.suvmusic.core.model

/**
 * Represents an audio output device.
 */
data class OutputDevice(
    val id: String,
    val name: String,
    val type: DeviceType,
    val isSelected: Boolean = false
)

enum class DeviceType {
    PHONE,
    SPEAKER,
    BLUETOOTH,
    HEADPHONES,
    CAST,
    UNKNOWN
}
