package com.suvojeet.suvmusic.core.model

/**
 * Detailed metadata for JioSaavn songs.
 */
data class JioSaavnMetadata(
    val label: String? = null,
    val playCount: Long? = null,
    val language: String? = null,
    val explicitContent: Boolean? = null,
    val copyright: String? = null,
    val hasLyrics: Boolean? = null,
    val year: String? = null,
    val releaseDate: String? = null,
    val artists: List<ArtistCreditInfo> = emptyList()
)
