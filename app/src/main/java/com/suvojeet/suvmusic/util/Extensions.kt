package com.suvojeet.suvmusic.util

import java.net.URLEncoder

fun String.encodeUrl(): String {
    return URLEncoder.encode(this, "UTF-8")
}

fun String.toHighResImage(): String {
    // JioSaavn images: replace resolution suffix for higher quality
    return this.replace("150x150", "500x500")
        .replace("50x50", "500x500")
}

fun String.decodeHtml(): String {
    return this.replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#039;", "'")
        .replace("&apos;", "'")
}
