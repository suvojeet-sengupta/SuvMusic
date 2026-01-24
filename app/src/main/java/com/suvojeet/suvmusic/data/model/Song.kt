package com.suvojeet.suvmusic.data.model

import android.net.Uri

/**
 * Represents a song/track that can be played.
 * Can originate from YouTube, YouTube Music, or local storage.
 */
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long, // in milliseconds
    val thumbnailUrl: String?,
    val source: SongSource,
    val streamUrl: String? = null, // For YouTube, this is resolved at playback time
    val localUri: Uri? = null, // For local files
    val setVideoId: String? = null, // Unique ID for this song instance in a playlist (for reordering)
    val artistId: String? = null, // Artist browse ID for navigation to artist screen
    val originalSource: SongSource? = null, // Original source before download (for credits display)
    val isVideo: Boolean = false, // Whether this item is a video (vs official song)
    val customFolderPath: String? = null, // Subfolder path for downloads (e.g. "My Playlist")
    val collectionId: String? = null, // ID of the collection (album/playlist) this download belongs to
    val collectionName: String? = null // Name of the collection
) {
    companion object {
        /**
         * Create a Song from YouTube/YouTube Music data.
         */
        fun fromYouTube(
            videoId: String,
            title: String,
            artist: String,
            album: String,
            duration: Long,
            thumbnailUrl: String?,
            setVideoId: String? = null,
            artistId: String? = null,
            isVideo: Boolean = false
        ): Song? {
            if (videoId.isBlank()) return null
            return Song(
                id = videoId,
                title = title,
                artist = artist,
                album = album,
                duration = duration,
                thumbnailUrl = thumbnailUrl ?: "https://img.youtube.com/vi/$videoId/maxresdefault.jpg",
                source = SongSource.YOUTUBE,
                setVideoId = setVideoId,
                artistId = artistId,
                isVideo = isVideo
            )
        }
        
        /**
         * Create a Song from local audio file.
         */
        fun fromLocal(
            id: Long,
            title: String,
            artist: String,
            album: String,
            duration: Long,
            albumArtUri: Uri?,
            contentUri: Uri
        ): Song {
            return Song(
                id = id.toString(),
                title = title,
                artist = artist,
                album = album,
                duration = duration,
                thumbnailUrl = albumArtUri?.toString(),
                source = SongSource.LOCAL,
                localUri = contentUri
            )
        }
        
        /**
         * Create a Song from JioSaavn data.
         */
        fun fromJioSaavn(
            songId: String,
            title: String,
            artist: String,
            album: String,
            duration: Long,
            thumbnailUrl: String?,
            streamUrl: String? = null
        ): Song? {
            if (songId.isBlank()) return null
            return Song(
                id = songId,
                title = title,
                artist = artist,
                album = album,
                duration = duration,
                thumbnailUrl = thumbnailUrl,
                source = SongSource.JIOSAAVN,
                streamUrl = streamUrl
            )
        }
    }
}

/**
 * Source of the song.
 */
enum class SongSource {
    YOUTUBE,
    YOUTUBE_MUSIC,
    LOCAL,
    DOWNLOADED,
    JIOSAAVN
}
