package com.suvojeet.suvmusic.deeplink

import android.net.Uri

/**
 * Parses incoming deep links of the form:
 *   suvmusic://play?id={videoId}             (legacy)
 *   suvmusic://song/{videoId}
 *   suvmusic://album/{browseId}
 *   suvmusic://playlist/{playlistId}
 *   suvmusic://artist/{channelId}
 *   suvmusic://search?q={query}
 *
 * Returns null when the URI does not match any known target.
 */
object DeepLinkHandler {

    const val SCHEME = "suvmusic"

    fun parse(uri: Uri?): DeepLinkTarget? {
        if (uri == null) return null
        if (!uri.scheme.equals(SCHEME, ignoreCase = true)) return null

        val host = uri.host?.lowercase() ?: return null
        val segments = uri.pathSegments.orEmpty()

        return when (host) {
            "play" -> uri.getQueryParameter("id")
                ?.takeIf { it.isNotBlank() }
                ?.let { DeepLinkTarget.Song(it) }

            "song" -> segments.firstOrNull()
                ?.takeIf { it.isNotBlank() }
                ?.let { DeepLinkTarget.Song(it) }

            "album" -> segments.firstOrNull()
                ?.takeIf { it.isNotBlank() }
                ?.let { DeepLinkTarget.Album(it) }

            "playlist" -> segments.firstOrNull()
                ?.takeIf { it.isNotBlank() }
                ?.let { DeepLinkTarget.Playlist(it) }

            "artist" -> segments.firstOrNull()
                ?.takeIf { it.isNotBlank() }
                ?.let { DeepLinkTarget.Artist(it) }

            "search" -> uri.getQueryParameter("q")
                ?.takeIf { it.isNotBlank() }
                ?.let { DeepLinkTarget.Search(it) }

            else -> null
        }
    }

    fun build(target: DeepLinkTarget): Uri = when (target) {
        is DeepLinkTarget.Song -> Uri.parse("$SCHEME://song/${target.id}")
        is DeepLinkTarget.Album -> Uri.parse("$SCHEME://album/${target.id}")
        is DeepLinkTarget.Playlist -> Uri.parse("$SCHEME://playlist/${target.id}")
        is DeepLinkTarget.Artist -> Uri.parse("$SCHEME://artist/${target.id}")
        is DeepLinkTarget.Search -> Uri.parse("$SCHEME://search?q=${Uri.encode(target.query)}")
    }
}

sealed class DeepLinkTarget {
    data class Song(val id: String) : DeepLinkTarget()
    data class Album(val id: String) : DeepLinkTarget()
    data class Playlist(val id: String) : DeepLinkTarget()
    data class Artist(val id: String) : DeepLinkTarget()
    data class Search(val query: String) : DeepLinkTarget()
}
