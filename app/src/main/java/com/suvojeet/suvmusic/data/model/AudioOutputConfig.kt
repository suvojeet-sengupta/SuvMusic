package com.suvojeet.suvmusic.data.model

/**
 * Configuration for Advanced Audio Output.
 */
enum class AudioSampleRate(val value: Int, val label: String) {
    AUTO(0, "Auto (Device Default)"),
    RATE_44100(44100, "44.1 kHz"),
    RATE_48000(48000, "48 kHz"),
    RATE_88200(88200, "88.2 kHz"),
    RATE_96000(96000, "96 kHz"),
    RATE_176400(176400, "176.4 kHz"),
    RATE_192000(192000, "192 kHz");

    companion object {
        fun fromValue(value: Int): AudioSampleRate {
            return entries.find { it.value == value } ?: AUTO
        }
    }
}
