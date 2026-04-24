package com.suvojeet.suvmusic.updater

import kotlinx.serialization.Serializable

@Serializable
data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val changelog: String = "",
    val downloadUrl: String,
    val forceUpdate: Boolean,
    val size: String = "",
    val releaseNotesUrl: String = "",
    /** Optional hex SHA-256 of the APK. When present, UpdateDownloader aborts
     *  install on mismatch — defends against MITM swap of the downloaded file. */
    val sha256: String? = null
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
