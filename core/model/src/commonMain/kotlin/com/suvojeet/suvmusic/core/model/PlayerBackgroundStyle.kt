package com.suvojeet.suvmusic.core.model

/**
 * Backdrop of the full player screen.
 *
 * [AMBIENT] keeps the art-derived blurred backdrop. [BLACK] and [LIGHT] pin the
 * player to a solid surface regardless of the app theme, for people who find the
 * ambient wash too busy (or want a true-black AMOLED player). [AUTO] picks between
 * [BLACK] and [LIGHT] from the app/system theme.
 */
enum class PlayerBackgroundStyle(val label: String, val description: String) {
    AMBIENT("Ambient", "Blurred album art fills the screen"),
    CUSTOM("Custom image", "Use your own image as the player background"),
    BLACK("Black", "Solid black — easy on AMOLED screens"),
    LIGHT("Light", "Solid light surface with dark text"),
    AUTO("Black / Light (auto)", "Black in dark mode, light in light mode — follows the app theme"),
}
