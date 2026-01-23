package com.suvojeet.suvmusic.navigation

/**
 * Navigation destinations for the app.
 */
sealed class Destination(val route: String) {
    data object Home : Destination("home")
    data object Search : Destination("search")
    data object Library : Destination("library")
    data object Settings : Destination("settings")
    data object PlaybackSettings : Destination("playback_settings")
    data object AppearanceSettings : Destination("appearance_settings")
    data object CustomizationSettings : Destination("customization_settings")
    data object ArtworkShapeSettings : Destination("artwork_shape_settings")
    data object SeekbarStyleSettings : Destination("seekbar_style_settings")
    data object ArtworkSizeSettings : Destination("artwork_size_settings")
    data object Recents : Destination("recents")
    data object Player : Destination("player")
    data object YouTubeLogin : Destination("youtube_login")
    data object About : Destination("about")
    data object Downloads : Destination("downloads")
    data object Storage : Destination("storage")
    data object ListeningStats : Destination("listening_stats")
    data object PlayerCache : Destination("player_cache")
    data object Welcome : Destination("welcome")
    data object HowItWorks : Destination("how_it_works")
    data object Support : Destination("support")
    
    data class Playlist(
        val playlistId: String,
        val name: String? = null,
        val thumbnailUrl: String? = null
    ) : Destination(buildRoute(playlistId, name, thumbnailUrl)) {
        companion object {
            const val ROUTE = "playlist/{playlistId}?name={name}&thumbnail={thumbnail}"
            const val ARG_PLAYLIST_ID = "playlistId"
            const val ARG_NAME = "name"
            const val ARG_THUMBNAIL = "thumbnail"
            
            fun buildRoute(playlistId: String, name: String?, thumbnailUrl: String?): String {
                val encodedName = java.net.URLEncoder.encode(name ?: "", "UTF-8")
                val encodedThumb = java.net.URLEncoder.encode(thumbnailUrl ?: "", "UTF-8")
                return "playlist/$playlistId?name=$encodedName&thumbnail=$encodedThumb"
            }
        }
    }

    data class Artist(val artistId: String) : Destination("artist/$artistId") {
        companion object {
            const val ROUTE = "artist/{artistId}"
            const val ARG_ARTIST_ID = "artistId"
        }
    }

    data class ArtistDiscography(
        val artistId: String,
        val type: String // "albums" or "singles"
    ) : Destination("artist_discography/$artistId/$type") {
        companion object {
            const val ROUTE = "artist_discography/{artistId}/{type}"
            const val ARG_ARTIST_ID = "artistId"
            const val ARG_TYPE = "type"
            const val TYPE_ALBUMS = "albums"
            const val TYPE_SINGLES = "singles"
        }
    }

    data class Album(
        val albumId: String,
        val name: String? = null,
        val thumbnailUrl: String? = null
    ) : Destination(buildRoute(albumId, name, thumbnailUrl)) {
        companion object {
            const val ROUTE = "album/{albumId}?name={name}&thumbnail={thumbnail}"
            const val ARG_ALBUM_ID = "albumId"
            const val ARG_NAME = "name"
            const val ARG_THUMBNAIL = "thumbnail"
            
            fun buildRoute(albumId: String, name: String?, thumbnailUrl: String?): String {
                val encodedName = java.net.URLEncoder.encode(name ?: "", "UTF-8")
                val encodedThumb = java.net.URLEncoder.encode(thumbnailUrl ?: "", "UTF-8")
                return "album/$albumId?name=$encodedName&thumbnail=$encodedThumb"
            }
        }
    }
}
