package com.suvojeet.suvmusic.util

/**
 * YouTube can hand back a different format (itag) for the same video between resolves.
 * Keying the shared cache by video id alone stitched cached bytes of one format onto
 * network bytes of another, producing files that play halfway and then turn to noise.
 */
object StreamCacheKey {
    private val itagRegex = Regex("[?&/]itag[=/](\\d+)")

    fun itagOf(url: String?): String? =
        url?.let { itagRegex.find(it)?.groupValues?.get(1) }

    fun of(base: String, url: String?): String =
        itagOf(url)?.let { "${base}_i$it" } ?: base
}
