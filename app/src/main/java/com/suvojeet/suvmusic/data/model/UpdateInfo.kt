package com.suvojeet.suvmusic.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val changelog: String,
    val downloadUrl: String,
    val forceUpdate: Boolean
)
