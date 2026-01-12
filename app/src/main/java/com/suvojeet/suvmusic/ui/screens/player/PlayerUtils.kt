package com.suvojeet.suvmusic.ui.screens.player

fun getHighResThumbnail(url: String?): String? {
    return url?.let {
        when {
            it.contains("ytimg.com") -> it
                .replace("hqdefault", "maxresdefault")
                .replace("mqdefault", "maxresdefault")
                .replace("sddefault", "maxresdefault")
                .replace("default", "maxresdefault")
                .replace(Regex("w\\d+-h\\d+"), "w544-h544")
            it.contains("lh3.googleusercontent.com") ->
                it.replace(Regex("=w\\d+-h\\d+"), "=w544-h544")
                    .replace(Regex("=s\\d+"), "=s544")
            else -> it
        }
    }
}

fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

