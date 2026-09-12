package com.suvojeet.suvmusic.data.repository.youtube.catalog

import com.suvojeet.suvmusic.core.domain.repository.LibraryRepository
import com.suvojeet.suvmusic.core.model.Album
import com.suvojeet.suvmusic.core.model.Artist
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.repository.youtube.internal.YouTubeApiClient
import com.suvojeet.suvmusic.data.repository.youtube.internal.YouTubeContentParser
import com.suvojeet.suvmusic.data.repository.youtube.search.YouTubeSearchService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Artist and album pages, plus the artist/album grids in the user's library. */
@Singleton
class YouTubeCatalogService @Inject constructor(
    private val sessionManager: SessionManager,
    private val apiClient: YouTubeApiClient,
    private val parser: YouTubeContentParser,
    private val searchService: YouTubeSearchService,
    private val libraryRepository: LibraryRepository
) {

    suspend fun getArtist(browseId: String): Artist? = withContext(Dispatchers.IO) {
        try {
            val artist = parser.parseArtist(apiClient.fetchInternalApi(browseId), browseId)
            // A locally-followed artist stays followed even when YouTube says otherwise
            // (following works offline and without an account).
            val isLocallySubscribed = libraryRepository.isArtistSaved(browseId).first()
            artist.copy(isSubscribed = artist.isSubscribed || isLocallySubscribed)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getAlbum(browseId: String): Album? = withContext(Dispatchers.IO) {
        // Album ids come in two shapes: OLAK… from search (needs the VL prefix) and MPRE…
        // from browse (already addressable).
        val effectiveBrowseId = if (browseId.startsWith("OLAK")) "VL$browseId" else browseId
        try {
            parser.parseAlbum(apiClient.fetchInternalApi(effectiveBrowseId), browseId)
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                parser.parseAlbum(apiClient.fetchInternalApi(browseId), browseId)
            } catch (e2: Exception) {
                e2.printStackTrace()
                null
            }
        }
    }

    suspend fun getLibraryArtists(): List<Artist> = withContext(Dispatchers.IO) {
        if (!sessionManager.isLoggedIn()) return@withContext emptyList()
        try {
            parser.parseLibraryArtists(apiClient.fetchInternalApi("FEmusic_library_corpus_track_artists"))
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Albums saved to the YouTube Music library.
     *
     * `FEmusic_liked_albums` is the shelf the "saved albums" grid is actually built from.
     * `FEmusic_library_corpus_track_albums` only lists albums inferred from library *tracks*
     * and is empty for most accounts, so it is kept purely as a fallback.
     */
    suspend fun getLibraryAlbums(): List<Album> = withContext(Dispatchers.IO) {
        if (!sessionManager.isLoggedIn()) return@withContext emptyList<Album>()
        for (browseId in listOf("FEmusic_liked_albums", "FEmusic_library_corpus_track_albums")) {
            val albums: List<Album> = try {
                parser.parseLibraryAlbums(apiClient.fetchInternalApi(browseId))
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
            if (albums.isNotEmpty()) return@withContext albums
        }
        emptyList<Album>()
    }

    suspend fun getArtistRadioId(artistId: String): String? = withContext(Dispatchers.IO) {
        try {
            parser.parseArtistRadioId(apiClient.fetchInternalApi(artistId))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** The artist's own shelves widened with a targeted search, since neither alone is deep. */
    suspend fun getArtistTopSongs(artistName: String, artistId: String): List<Song> = withContext(Dispatchers.IO) {
        val allSongs = mutableListOf<Song>()
        try {
            getArtist(artistId)?.let { artist ->
                allSongs.addAll(artist.songs)
                allSongs.addAll(artist.videos)
            }

            allSongs.addAll(
                searchService.search(artistName, YouTubeSearchService.FILTER_SONGS)
                    .filter { it.artist.contains(artistName, ignoreCase = true) }
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        allSongs.distinctBy { it.id }
    }
}
