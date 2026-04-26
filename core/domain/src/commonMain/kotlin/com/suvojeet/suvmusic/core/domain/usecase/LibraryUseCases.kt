package com.suvojeet.suvmusic.core.domain.usecase

import com.suvojeet.suvmusic.core.domain.repository.LibraryRepository
import com.suvojeet.suvmusic.core.model.Album
import com.suvojeet.suvmusic.core.model.Artist
import com.suvojeet.suvmusic.core.model.LibraryItem
import com.suvojeet.suvmusic.core.model.Playlist
import com.suvojeet.suvmusic.core.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.core.model.Song
import kotlinx.coroutines.flow.Flow

/**
 * Use case for saving a playlist to the library.
 */
class SavePlaylistUseCase constructor(
    private val libraryRepository: LibraryRepository
) : ConsumerUseCase<Playlist> {
    override suspend operator fun invoke(playlist: Playlist) {
        libraryRepository.savePlaylist(playlist)
    }
}

/**
 * Use case for saving songs to a playlist.
 */
class SavePlaylistSongsUseCase constructor(
    private val libraryRepository: LibraryRepository
) {
    suspend operator fun invoke(playlistId: String, songs: List<Song>) {
        libraryRepository.savePlaylistSongs(playlistId, songs)
    }
}

/**
 * Use case for retrieving cached playlist songs.
 */
class GetPlaylistSongsUseCase constructor(
    private val libraryRepository: LibraryRepository
) : UseCase<String, List<Song>> {
    override suspend operator fun invoke(playlistId: String): List<Song> {
        return libraryRepository.getCachedPlaylistSongs(playlistId)
    }
}

/**
 * Use case for retrieving playlist songs as a Flow.
 */
class GetPlaylistSongsFlowUseCase constructor(
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
class IsSongInPlaylistUseCase constructor(
    private val libraryRepository: LibraryRepository
) {
    suspend operator fun invoke(playlistId: String, songId: String): Boolean {
        return libraryRepository.isSongInPlaylist(playlistId, songId)
    }
}

/**
 * Use case for removing a playlist from the library.
 */
class RemovePlaylistUseCase constructor(
    private val libraryRepository: LibraryRepository
) : ConsumerUseCase<String> {
    override suspend operator fun invoke(playlistId: String) {
        libraryRepository.removePlaylist(playlistId)
    }
}

/**
 * Use case for adding a song to a playlist.
 */
class AddSongToPlaylistUseCase constructor(
    private val libraryRepository: LibraryRepository
) {
    suspend operator fun invoke(playlistId: String, song: Song) {
        libraryRepository.addSongToPlaylist(playlistId, song)
    }
}

/**
 * Use case for removing a song from a playlist.
 */
class RemoveSongFromPlaylistUseCase constructor(
    private val libraryRepository: LibraryRepository
) {
    suspend operator fun invoke(playlistId: String, songId: String) {
        libraryRepository.removeSongFromPlaylist(playlistId, songId)
    }
}

/**
 * Use case for checking if an album is saved.
 */
class IsAlbumSavedUseCase constructor(
    private val libraryRepository: LibraryRepository
) : UseCase<String, Flow<Boolean>> {
    override suspend operator fun invoke(albumId: String): Flow<Boolean> {
        return libraryRepository.isAlbumSaved(albumId)
    }
}

/**
 * Use case for saving an album to the library.
 */
class SaveAlbumUseCase constructor(
    private val libraryRepository: LibraryRepository
) : ConsumerUseCase<Album> {
    override suspend operator fun invoke(album: Album) {
        libraryRepository.saveAlbum(album)
    }
}

/**
 * Use case for removing an album from the library.
 */
class RemoveAlbumUseCase constructor(
    private val libraryRepository: LibraryRepository
) : ConsumerUseCase<String> {
    override suspend operator fun invoke(albumId: String) {
        libraryRepository.removeAlbum(albumId)
    }
}

/**
 * Use case for getting saved playlists.
 */
class GetSavedPlaylistsUseCase constructor(
    private val libraryRepository: LibraryRepository
) : ParameterlessUseCase<Flow<List<PlaylistDisplayItem>>> {
    override suspend operator fun invoke(): Flow<List<PlaylistDisplayItem>> {
        return libraryRepository.getSavedPlaylists()
    }
}

/**
 * Use case for getting saved albums.
 */
class GetSavedAlbumsUseCase constructor(
    private val libraryRepository: LibraryRepository
) : ParameterlessUseCase<Flow<List<LibraryItem>>> {
    override suspend operator fun invoke(): Flow<List<LibraryItem>> {
        return libraryRepository.getSavedAlbums()
    }
}

/**
 * Use case for getting saved artists.
 */
class GetSavedArtistsUseCase constructor(
    private val libraryRepository: LibraryRepository
) : ParameterlessUseCase<Flow<List<LibraryItem>>> {
    override suspend operator fun invoke(): Flow<List<LibraryItem>> {
        return libraryRepository.getSavedArtists()
    }
}

/**
 * Use case for saving an artist to the library.
 */
class SaveArtistUseCase constructor(
    private val libraryRepository: LibraryRepository
) : ConsumerUseCase<Artist> {
    override suspend operator fun invoke(artist: Artist) {
        libraryRepository.saveArtist(artist)
    }
}

/**
 * Use case for removing an artist from the library.
 */
class RemoveArtistUseCase constructor(
    private val libraryRepository: LibraryRepository
) : ConsumerUseCase<String> {
    override suspend operator fun invoke(artistId: String) {
        libraryRepository.removeArtist(artistId)
    }
}

/**
 * Use case for checking if an artist is saved.
 */
class IsArtistSavedUseCase constructor(
    private val libraryRepository: LibraryRepository
) : UseCase<String, Flow<Boolean>> {
    override suspend operator fun invoke(artistId: String): Flow<Boolean> {
        return libraryRepository.isArtistSaved(artistId)
    }
}

/**
 * Use case for getting a playlist by ID.
 */
class GetPlaylistByIdUseCase constructor(
    private val libraryRepository: LibraryRepository
) : UseCase<String, LibraryItem?> {
    override suspend operator fun invoke(id: String): LibraryItem? {
        return libraryRepository.getPlaylistById(id)
    }
}

/**
 * Use case for updating a playlist thumbnail.
 */
class UpdatePlaylistThumbnailUseCase constructor(
    private val libraryRepository: LibraryRepository
) {
    suspend operator fun invoke(playlistId: String, thumbnailUrl: String?) {
        libraryRepository.updatePlaylistThumbnail(playlistId, thumbnailUrl)
    }
}

/**
 * Use case for updating a playlist name.
 */
class UpdatePlaylistNameUseCase constructor(
    private val libraryRepository: LibraryRepository
) {
    suspend operator fun invoke(playlistId: String, name: String) {
        libraryRepository.updatePlaylistName(playlistId, name)
    }
}

/**
 * Use case for replacing all songs in a playlist.
 */
class ReplacePlaylistSongsUseCase constructor(
    private val libraryRepository: LibraryRepository
) {
    suspend operator fun invoke(playlistId: String, songs: List<Song>) {
        libraryRepository.replacePlaylistSongs(playlistId, songs)
    }
}
