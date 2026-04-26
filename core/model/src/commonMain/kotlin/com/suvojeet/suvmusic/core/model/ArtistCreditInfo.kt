package com.suvojeet.suvmusic.core.model

/**
 * Data class to hold artist credit information with thumbnail
 */
data class ArtistCreditInfo(
    val name: String,
    val role: String,
    val thumbnailUrl: String?,
    val artistId: String?
)
