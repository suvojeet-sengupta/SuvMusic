package com.suvojeet.suvmusic.updater

import kotlinx.serialization.Serializable

@Serializable
data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val changelog: String = "",
    val downloadUrl: String,
    val forceUpdate: Boolean
)

@Serializable
data class ChangelogInfo(
    val releases: List<Release>
)

@Serializable
data class Release(
    val versionName: String,
    val versionCode: Int,
    val date: String,
    val description: String,
    val isMajorUpdate: Boolean = false
)
