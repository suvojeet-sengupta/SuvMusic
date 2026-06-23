package com.suvojeet.suvmusic.core.model

/**
 * Detailed metadata for RemoteAudio songs.
 */
data class RemoteAudioMetadata(
    val label: String? = null,
    val playCount: Long? = null,
    val language: String? = null,
    val explicitContent: Boolean? = null,
    val copyright: String? = null,
    val hasLyrics: Boolean? = null,
    val year: String? = null,
    val releaseDate: String? = null,
    val artists: List<ArtistCreditInfo> = emptyList(),
    val downloadUrls: Map<String, String> = emptyMap()
)
