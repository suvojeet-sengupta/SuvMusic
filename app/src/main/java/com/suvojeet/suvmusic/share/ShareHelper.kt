package com.suvojeet.suvmusic.share

import android.content.Context
import android.content.Intent
import com.suvojeet.suvmusic.deeplink.DeepLinkHandler
import com.suvojeet.suvmusic.deeplink.DeepLinkTarget

/**
 * Builds and dispatches standard Android share intents for SuvMusic entities.
 *
 * The shared text includes both a human-readable title and the suvmusic:// deep link
 * so recipients with the app installed get a tappable link, and others still see useful text.
 */
object ShareHelper {

    fun share(context: Context, target: DeepLinkTarget, title: String) {
        val link = DeepLinkHandler.build(target).toString()
        val text = buildString {
            append(title)
            append("\n\n")
            append(link)
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, title)
        }

        val chooser = Intent.createChooser(sendIntent, "Share via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun shareSong(context: Context, videoId: String, title: String, artist: String) =
        share(context, DeepLinkTarget.Song(videoId), "$title — $artist")

    fun shareAlbum(context: Context, browseId: String, albumTitle: String) =
        share(context, DeepLinkTarget.Album(browseId), albumTitle)

    fun sharePlaylist(context: Context, playlistId: String, playlistName: String) =
        share(context, DeepLinkTarget.Playlist(playlistId), playlistName)

    fun shareArtist(context: Context, channelId: String, artistName: String) =
        share(context, DeepLinkTarget.Artist(channelId), artistName)
}
