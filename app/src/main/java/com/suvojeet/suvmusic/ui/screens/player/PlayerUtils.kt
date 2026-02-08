package com.suvojeet.suvmusic.ui.screens.player

import com.suvojeet.suvmusic.util.ImageUtils

fun getHighResThumbnail(url: String?): String? {
    return ImageUtils.getHighResThumbnailUrl(url)
}

fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

