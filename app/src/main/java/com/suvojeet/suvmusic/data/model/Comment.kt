package com.suvojeet.suvmusic.data.model

data class Comment(
    val id: String,
    val authorName: String,
    val authorThumbnailUrl: String?,
    val text: String,
    val timestamp: String,
    val likeCount: String,
    val replyCount: Int = 0
)
