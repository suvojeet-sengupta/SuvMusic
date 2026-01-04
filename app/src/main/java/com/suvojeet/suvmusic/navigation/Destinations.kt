package com.suvojeet.suvmusic.navigation

/**
 * Navigation destinations for the app.
 */
sealed class Destination(val route: String) {
    data object Home : Destination("home")
    data object Search : Destination("search")
    data object Library : Destination("library")
    data object Settings : Destination("settings")
    data object Player : Destination("player")
    data object YouTubeLogin : Destination("youtube_login")
    
    data class Playlist(val playlistId: String) : Destination("playlist/$playlistId") {
        companion object {
            const val ROUTE = "playlist/{playlistId}"
            const val ARG_PLAYLIST_ID = "playlistId"
        }
    }

    data class Artist(val artistId: String) : Destination("artist/$artistId") {
        companion object {
            const val ROUTE = "artist/{artistId}"
            const val ARG_ARTIST_ID = "artistId"
        }
    }

    data class Album(val albumId: String) : Destination("album/{albumId}" ) {
        companion object {
            const val ROUTE = "album/{albumId}"
            const val ARG_ALBUM_ID = "albumId"
        }
    }
}
