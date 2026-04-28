package com.suvojeet.suvmusic.core.model

/**
 * Primary music source preference (YouTube Music vs JioSaavn HQ vs both).
 *
 * Lifted from `app/.../data/SessionManager.kt` to commonMain so the new
 * commonMain PlaybackSettingsScreen can reference it without depending on
 * the Android DataStore-backed SessionManager.
 */
enum class MusicSource {
    YOUTUBE,
    JIOSAAVN,
    BOTH,
}
