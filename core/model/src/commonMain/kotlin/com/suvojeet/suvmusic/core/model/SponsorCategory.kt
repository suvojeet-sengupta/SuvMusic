package com.suvojeet.suvmusic.core.model

/**
 * SponsorBlock segment categories. Mirrors the standard SponsorBlock
 * extension's category set — `key` is the wire/persistence value used by
 * the SponsorBlock API and DataStore preferences, `displayName` is the
 * user-facing label.
 *
 * Pure-Kotlin in commonMain — UI concerns (e.g. the color swatch shown in
 * settings/seekbar) live in the UI layer as extension functions, so this
 * module stays free of Compose deps:
 * - Android: `app/.../ui/sponsorblock/SponsorCategoryColors.kt`
 * - Shared (Compose Multiplatform): `composeApp/.../ui/settings/SponsorCategoryColors.kt`
 *
 * Ported from `app/.../data/model/SponsorBlockConfig.kt`.
 */
enum class SponsorCategory(val key: String, val displayName: String) {
    SPONSOR("sponsor", "Sponsor"),
    SELFPROMO("selfpromo", "Self Promo"),
    INTERACTION("interaction", "Interaction (Subscribe)"),
    INTRO("intro", "Intermission/Intro"),
    OUTRO("outro", "Endcards/Credits"),
    MUSIC_OFFTOPIC("music_offtopic", "Non-Music Section");

    companion object {
        fun fromKey(key: String): SponsorCategory? = entries.find { it.key == key }
    }
}
