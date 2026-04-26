package com.suvojeet.suvmusic.core.domain.usecase

import com.suvojeet.suvmusic.core.domain.repository.SearchRepository
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.model.Album
import com.suvojeet.suvmusic.core.model.Artist
import com.suvojeet.suvmusic.core.model.Playlist
import kotlinx.coroutines.flow.Flow

/**
 * Use case for searching songs across available music sources.
 */
class SearchSongsUseCase constructor(
    private val searchRepository: SearchRepository
) : UseCase<String, Flow<List<Song>>> {
    override suspend operator fun invoke(query: String): Flow<List<Song>> {
        return searchRepository.searchSongs(query)
    }
}

/**
 * Use case for searching albums.
 */
class SearchAlbumsUseCase constructor(
    private val searchRepository: SearchRepository
) : UseCase<String, Flow<List<Album>>> {
    override suspend operator fun invoke(query: String): Flow<List<Album>> {
        return searchRepository.searchAlbums(query)
    }
}

/**
 * Use case for searching artists.
 */
class SearchArtistsUseCase constructor(
    private val searchRepository: SearchRepository
) : UseCase<String, Flow<List<Artist>>> {
    override suspend operator fun invoke(query: String): Flow<List<Artist>> {
        return searchRepository.searchArtists(query)
    }
}

/**
 * Use case for searching playlists.
 */
class SearchPlaylistsUseCase constructor(
    private val searchRepository: SearchRepository
) : UseCase<String, Flow<List<Playlist>>> {
    override suspend operator fun invoke(query: String): Flow<List<Playlist>> {
        return searchRepository.searchPlaylists(query)
    }
}

/**
 * Use case for getting detailed song information.
 */
class GetSongDetailsUseCase constructor(
    private val searchRepository: SearchRepository
) : UseCase<String, Song?> {
    override suspend operator fun invoke(songId: String): Song? {
        return searchRepository.getSongDetails(songId)
    }
}
