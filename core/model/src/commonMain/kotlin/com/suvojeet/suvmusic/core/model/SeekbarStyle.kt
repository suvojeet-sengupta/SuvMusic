package com.suvojeet.suvmusic.core.model

/**
 * Visual style of the player seekbar.
 *
 * Lifted from `app/.../ui/components/WaveformSeeker.kt` so commonMain
 * settings (SeekbarStyleScreen) can read/write the choice without
 * depending on Android-only seeker UI.
 *
 * Note: composeApp's local Seekbar implementation defines its own 5-value
 * SeekbarStyle (CLASSIC / DOTS / GRADIENT_BAR / WAVE_LINE / WAVEFORM).
 * That copy is internal to the seekbar drawing code and stays put — this
 * one is the canonical 9-value enum the user picks from in settings.
 */
enum class SeekbarStyle {
    WAVEFORM,
    WAVE_LINE,
    CLASSIC,
    DOTS,
    GRADIENT_BAR,
    NEON,
    BLOCKS,
    MATERIAL,
    M3E_WAVY,
}
