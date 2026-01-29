package com.suvojeet.suvmusic.kugou

import kotlinx.serialization.Serializable

@Serializable
data class SearchSongResponse(
    val status: Int,
    val errcode: Int,
    val data: SearchSongData,
    val error: String? = null
)

@Serializable
data class SearchSongData(
    val info: List<SongInfo>
)

@Serializable
data class SongInfo(
    val hash: String,
    val filename: String,
    val duration: Int,
    val album_name: String
)

@Serializable
data class SearchLyricsResponse(
    val status: Int,
    val candidates: List<Candidate>,
    val proposals: List<Proposal>? = null
)

@Serializable
data class Candidate(
    val id: Long,
    val accesskey: String,
    val duration: Int,
    val sound: String? = null,
    val song: String? = null,
    val singer: String? = null
)

@Serializable
data class Proposal(
    val id: Long,
    val accesskey: String,
    val duration: Int
)

@Serializable
data class DownloadLyricsResponse(
    val status: Int,
    val content: String,
    val fmt: String
)

data class Keyword(
    val title: String,
    val artist: String,
    val album: String? = null
)
