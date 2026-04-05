package com.suvojeet.suvmusic.navigation

import kotlinx.serialization.Serializable

/**
 * Navigation destinations for the app.
 */
@Serializable
sealed class Destination {
    @Serializable
    data object Home : Destination()
    @Serializable
    data object Search : Destination()
    @Serializable
    data object Library : Destination()
    @Serializable
    data object Settings : Destination()
    @Serializable
    data object PlaybackSettings : Destination()
    @Serializable
    data object AppearanceSettings : Destination()
    @Serializable
    data object CustomizationSettings : Destination()
    @Serializable
    data object ArtworkShapeSettings : Destination()
    @Serializable
    data object SeekbarStyleSettings : Destination()
    @Serializable
    data object ArtworkSizeSettings : Destination()
    @Serializable
    data object Recents : Destination()
    @Serializable
    data object YouTubeLogin : Destination()
    @Serializable
    data object LastFmLogin : Destination()
    @Serializable
    data object About : Destination()
    @Serializable
    data object Downloads : Destination()
    @Serializable
    data object Storage : Destination()
    @Serializable
    data object ListeningStats : Destination()
    @Serializable
    data object PlayerCache : Destination()
    @Serializable
    data object HowItWorks : Destination()
    @Serializable
    data object Support : Destination()
    @Serializable
    data object Misc : Destination()
    @Serializable
    data object LyricsProviders : Destination()
    @Serializable
    data object SponsorBlockSettings : Destination()
    @Serializable
    data object DiscordSettings : Destination()
    @Serializable
    data object AIEqualizer : Destination()
    @Serializable
    data object AISettings : Destination()
    @Serializable
    data object Credits : Destination()
    @Serializable
    data object Updater : Destination()
    @Serializable
    data object ListenTogether : Destination()
    @Serializable
    data object Changelog : Destination()

    @Serializable
    data class Playlist(
        val playlistId: String,
        val name: String? = null,
        val thumbnailUrl: String? = null
    ) : Destination() {
        companion object {
            const val ARG_PLAYLIST_ID = "playlistId"
            const val ARG_NAME = "name"
            const val ARG_THUMBNAIL = "thumbnailUrl"
        }
    }

    @Serializable
    data class Artist(val artistId: String) : Destination() {
        companion object {
            const val ARG_ARTIST_ID = "artistId"
        }
    }

    @Serializable
    data class ArtistDiscography(
        val artistId: String,
        val type: String // "albums" or "singles"
    ) : Destination() {
        companion object {
            const val ARG_ARTIST_ID = "artistId"
            const val ARG_TYPE = "type"
            const val TYPE_ALBUMS = "albums"
            const val TYPE_SINGLES = "singles"
        }
    }

    @Serializable
    data class Album(
        val albumId: String,
        val name: String? = null,
        val thumbnailUrl: String? = null
    ) : Destination() {
        companion object {
            const val ARG_ALBUM_ID = "albumId"
            const val ARG_NAME = "name"
            const val ARG_THUMBNAIL = "thumbnailUrl"
        }
    }

    @Serializable
    data class Explore(val browseId: String, val title: String) : Destination() {
        companion object {
            const val ARG_BROWSE_ID = "browseId"
            const val ARG_TITLE = "title"
        }
    }

    @Serializable
    data object MoodAndGenres : Destination()

    @Serializable
    data class MoodAndGenresDetail(
        val browseId: String,
        val params: String? = null,
        val title: String
    ) : Destination() {
        companion object {
            const val ARG_BROWSE_ID = "browseId"
            const val ARG_PARAMS = "params"
            const val ARG_TITLE = "title"
        }
    }

    @Serializable
    data object PickMusic : Destination()
}
