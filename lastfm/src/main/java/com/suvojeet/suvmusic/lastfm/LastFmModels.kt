package com.suvojeet.suvmusic.lastfm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Authentication(
    val session: Session
) {
    @Serializable
    data class Session(
        val name: String,       // Username
        val key: String,        // Session Key
        val subscriber: Int,    // Last.fm Pro?
    )
}

@Serializable
data class TokenResponse(
    val token: String
)

@Serializable
data class LastFmError(
    val error: Int,
    val message: String
)

@Serializable
data class RecommendedArtistsResponse(
    val recommendations: Recommendations
)

@Serializable
data class Recommendations(
    val artist: List<RecommendedArtist> = emptyList(),
    @SerialName("@attr") val attr: PageAttr? = null
)

@Serializable
data class RecommendedArtist(
    val name: String,
    val mbid: String? = null,
    val url: String,
    val image: List<LastFmImage> = emptyList()
)

@Serializable
data class RecommendedTracksResponse(
    val tracks: RecommendedTrackList
)

@Serializable
data class RecommendedTrackList(
    val track: List<RecommendedTrack> = emptyList(),
    @SerialName("@attr") val attr: PageAttr? = null
)

@Serializable
data class RecommendedTrack(
    val name: String,
    val mbid: String? = null,
    val url: String,
    val artist: RecommendedArtistShort,
    val image: List<LastFmImage> = emptyList(),
    val duration: String? = null
)

@Serializable
data class RecommendedArtistShort(
    val name: String,
    val mbid: String? = null,
    val url: String
)

@Serializable
data class LastFmImage(
    @SerialName("#text") val url: String,
    val size: String
)

@Serializable
data class PageAttr(
    val user: String,
    val page: String,
    val perPage: String,
    val total: String,
    val totalPages: String
)
