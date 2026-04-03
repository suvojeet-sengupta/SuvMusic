package com.suvojeet.suvmusic.core.domain.usecase

import com.suvojeet.suvmusic.core.domain.repository.SearchRepository
import com.suvojeet.suvmusic.core.model.SearchResult
import com.suvojeet.suvmusic.core.model.Song
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for searching songs across available music sources.
 */
class SearchSongsUseCase @Inject constructor(
    private val searchRepository: SearchRepository
) : UseCase<String, Flow<List<Song>>> {
    override suspend operator fun invoke(query: String): Flow<List<Song>> {
        return searchRepository.searchSongs(query)
    }
}

/**
 * Use case for searching albums.
 */
class SearchAlbumsUseCase @Inject constructor(
    private val searchRepository: SearchRepository
) : UseCase<String, Flow<List<SearchResult>>> {
    override suspend operator fun invoke(query: String): Flow<List<SearchResult>> {
        return searchRepository.searchAlbums(query)
    }
}

/**
 * Use case for searching artists.
 */
class SearchArtistsUseCase @Inject constructor(
    private val searchRepository: SearchRepository
) : UseCase<String, Flow<List<SearchResult>>> {
    override suspend operator fun invoke(query: String): Flow<List<SearchResult>> {
        return searchRepository.searchArtists(query)
    }
}

/**
 * Use case for searching playlists.
 */
class SearchPlaylistsUseCase @Inject constructor(
    private val searchRepository: SearchRepository
) : UseCase<String, Flow<List<SearchResult>>> {
    override suspend operator fun invoke(query: String): Flow<List<SearchResult>> {
        return searchRepository.searchPlaylists(query)
    }
}

/**
 * Use case for getting detailed song information.
 */
class GetSongDetailsUseCase @Inject constructor(
    private val searchRepository: SearchRepository
) : UseCase<String, Song?> {
    override suspend operator fun invoke(songId: String): Song? {
        return searchRepository.getSongDetails(songId)
    }
}
