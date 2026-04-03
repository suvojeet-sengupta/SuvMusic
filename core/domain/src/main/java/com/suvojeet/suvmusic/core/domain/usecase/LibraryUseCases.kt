package com.suvojeet.suvmusic.core.domain.usecase

import com.suvojeet.suvmusic.core.domain.repository.LibraryRepository
import com.suvojeet.suvmusic.core.model.Album
import com.suvojeet.suvmusic.core.model.Artist
import com.suvojeet.suvmusic.core.model.LibraryItem
import com.suvojeet.suvmusic.core.model.Playlist
import com.suvojeet.suvmusic.core.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.core.model.Song
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for saving a playlist to the library.
 */
class SavePlaylistUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : ConsumerUseCase<Playlist> {
    override suspend operator fun invoke(playlist: Playlist) {
        libraryRepository.savePlaylist(playlist)
    }
}

/**
 * Use case for saving songs to a playlist.
 */
class SavePlaylistSongsUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) {
    suspend operator fun invoke(playlistId: String, songs: List<Song>) {
        libraryRepository.savePlaylistSongs(playlistId, songs)
    }
}

/**
 * Use case for retrieving cached playlist songs.
 */
class GetPlaylistSongsUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : UseCase<String, List<Song>> {
    override suspend operator fun invoke(playlistId: String): List<Song> {
        return libraryRepository.getCachedPlaylistSongs(playlistId)
    }
}

/**
 * Use case for retrieving playlist songs as a Flow.
 */
class GetPlaylistSongsFlowUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : UseCase<String, Flow<List<Song>>> {
    override suspend operator fun invoke(playlistId: String): Flow<List<Song>> {
        return libraryRepository.getPlaylistSongCountFlow(playlistId)
            .let { libraryRepository.getCachedPlaylistSongsFlow(playlistId) }
    }
}

/**
 * Use case for checking if a song is in a playlist.
 */
class IsSongInPlaylistUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) {
    suspend operator fun invoke(playlistId: String, songId: String): Boolean {
        return libraryRepository.isSongInPlaylist(playlistId, songId)
    }
}

/**
 * Use case for removing a playlist from the library.
 */
class RemovePlaylistUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : ConsumerUseCase<String> {
    override suspend operator fun invoke(playlistId: String) {
        libraryRepository.removePlaylist(playlistId)
    }
}

/**
 * Use case for adding a song to a playlist.
 */
class AddSongToPlaylistUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) {
    suspend operator fun invoke(playlistId: String, song: Song) {
        libraryRepository.addSongToPlaylist(playlistId, song)
    }
}

/**
 * Use case for removing a song from a playlist.
 */
class RemoveSongFromPlaylistUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) {
    suspend operator fun invoke(playlistId: String, songId: String) {
        libraryRepository.removeSongFromPlaylist(playlistId, songId)
    }
}

/**
 * Use case for checking if an album is saved.
 */
class IsAlbumSavedUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : UseCase<String, Flow<Boolean>> {
    override suspend operator fun invoke(albumId: String): Flow<Boolean> {
        return libraryRepository.isAlbumSaved(albumId)
    }
}

/**
 * Use case for saving an album to the library.
 */
class SaveAlbumUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : ConsumerUseCase<Album> {
    override suspend operator fun invoke(album: Album) {
        libraryRepository.saveAlbum(album)
    }
}

/**
 * Use case for removing an album from the library.
 */
class RemoveAlbumUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : ConsumerUseCase<String> {
    override suspend operator fun invoke(albumId: String) {
        libraryRepository.removeAlbum(albumId)
    }
}

/**
 * Use case for getting saved playlists.
 */
class GetSavedPlaylistsUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : ParameterlessUseCase<Flow<List<PlaylistDisplayItem>>> {
    override suspend operator fun invoke(): Flow<List<PlaylistDisplayItem>> {
        return libraryRepository.getSavedPlaylists()
    }
}

/**
 * Use case for getting saved albums.
 */
class GetSavedAlbumsUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : ParameterlessUseCase<Flow<List<LibraryItem>>> {
    override suspend operator fun invoke(): Flow<List<LibraryItem>> {
        return libraryRepository.getSavedAlbums()
    }
}

/**
 * Use case for getting saved artists.
 */
class GetSavedArtistsUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : ParameterlessUseCase<Flow<List<LibraryItem>>> {
    override suspend operator fun invoke(): Flow<List<LibraryItem>> {
        return libraryRepository.getSavedArtists()
    }
}

/**
 * Use case for saving an artist to the library.
 */
class SaveArtistUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : ConsumerUseCase<Artist> {
    override suspend operator fun invoke(artist: Artist) {
        libraryRepository.saveArtist(artist)
    }
}

/**
 * Use case for removing an artist from the library.
 */
class RemoveArtistUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : ConsumerUseCase<String> {
    override suspend operator fun invoke(artistId: String) {
        libraryRepository.removeArtist(artistId)
    }
}

/**
 * Use case for checking if an artist is saved.
 */
class IsArtistSavedUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : UseCase<String, Flow<Boolean>> {
    override suspend operator fun invoke(artistId: String): Flow<Boolean> {
        return libraryRepository.isArtistSaved(artistId)
    }
}

/**
 * Use case for getting a playlist by ID.
 */
class GetPlaylistByIdUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : UseCase<String, LibraryItem?> {
    override suspend operator fun invoke(id: String): LibraryItem? {
        return libraryRepository.getPlaylistById(id)
    }
}

/**
 * Use case for updating a playlist thumbnail.
 */
class UpdatePlaylistThumbnailUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) {
    suspend operator fun invoke(playlistId: String, thumbnailUrl: String?) {
        libraryRepository.updatePlaylistThumbnail(playlistId, thumbnailUrl)
    }
}

/**
 * Use case for updating a playlist name.
 */
class UpdatePlaylistNameUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) {
    suspend operator fun invoke(playlistId: String, name: String) {
        libraryRepository.updatePlaylistName(playlistId, name)
    }
}

/**
 * Use case for replacing all songs in a playlist.
 */
class ReplacePlaylistSongsUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) {
    suspend operator fun invoke(playlistId: String, songs: List<Song>) {
        libraryRepository.replacePlaylistSongs(playlistId, songs)
    }
}
