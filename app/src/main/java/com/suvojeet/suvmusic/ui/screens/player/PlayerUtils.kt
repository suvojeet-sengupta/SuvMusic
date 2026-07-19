package com.suvojeet.suvmusic.ui.screens.player

import com.suvojeet.suvmusic.util.ImageUtils

fun getHighResThumbnail(url: String?): String? {
    return ImageUtils.getHighResThumbnailUrl(url)
}

fun formatDuration(millis: Long): String = com.suvojeet.suvmusic.util.TimeUtil.formatPosition(millis)

