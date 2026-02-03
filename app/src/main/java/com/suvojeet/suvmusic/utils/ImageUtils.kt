package com.suvojeet.suvmusic.utils

object ImageUtils {
    private val W_H_REGEX = Regex("""w\d+-h\d+""")
    private val W_REGEX = Regex("""=w\d+""")
    private val GOOGLE_W_H_REGEX = Regex("""=w\d+-h\d+""")
    private val GOOGLE_S_REGEX = Regex("""=s\d+""")

    fun getHighResThumbnailUrl(url: String?): String? {
        if (url == null) return null
        
        return when {
            url.contains("ytimg.com") -> url
                .replace("hqdefault", "maxresdefault")
                .replace("mqdefault", "maxresdefault")
                .replace("sddefault", "maxresdefault")
                .replace("default", "maxresdefault")
                .replace(W_H_REGEX, "w544-h544")
            url.contains("lh3.googleusercontent.com") ->
                url.replace(GOOGLE_W_H_REGEX, "=w544-h544")
                    .replace(GOOGLE_S_REGEX, "=s544")
            else -> url.replace(W_H_REGEX, "w544-h544")
                .replace(W_REGEX, "=w544")
        }
    }
}
