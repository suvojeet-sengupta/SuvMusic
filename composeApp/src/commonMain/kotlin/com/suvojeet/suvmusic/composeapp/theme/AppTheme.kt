package com.suvojeet.suvmusic.composeapp.theme

/**
 * The user-selectable colour palette. Verbatim port of
 * `app/.../data/model/AppTheme.kt` so that the shared [SuvMusicTheme]
 * builder can switch ColorSchemes from any platform without an Android
 * dependency on the `:app` module.
 *
 * The `:app` consumer still has its own copy at
 * `data/model/AppTheme.kt` for now (Android keeps shipping); Phase B of
 * this round wires `:app` → `:composeApp` and deletes the duplicate.
 */
enum class AppTheme(val label: String) {
    DEFAULT("Electric Purple"),
    OCEAN("Ocean Blue"),
    SUNSET("Sunset Orange"),
    NATURE("Forest Green"),
    LOVE("Passion Pink"),
}
