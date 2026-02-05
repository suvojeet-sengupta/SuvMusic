package com.suvojeet.suvmusic.data.model

import android.media.audiofx.PresetReverb

/**
 * Reverb presets corresponding to [android.media.audiofx.PresetReverb].
 */
enum class ReverbPreset(val label: String, val preset: Short) {
    NONE("None", PresetReverb.PRESET_NONE),
    SMALL_ROOM("Small Room", PresetReverb.PRESET_SMALLROOM),
    MEDIUM_ROOM("Medium Room", PresetReverb.PRESET_MEDIUMROOM),
    LARGE_ROOM("Large Room", PresetReverb.PRESET_LARGEROOM),
    MEDIUM_HALL("Medium Hall", PresetReverb.PRESET_MEDIUMHALL),
    LARGE_HALL("Large Hall", PresetReverb.PRESET_LARGEHALL),
    PLATE("Plate", PresetReverb.PRESET_PLATE);

    companion object {
        fun fromName(name: String?): ReverbPreset {
            return entries.find { it.name == name } ?: NONE
        }
    }
}
