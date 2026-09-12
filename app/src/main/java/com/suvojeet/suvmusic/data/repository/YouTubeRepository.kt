package com.suvojeet.suvmusic.data.repository

import com.suvojeet.suvmusic.core.model.Album
import com.suvojeet.suvmusic.core.model.Artist
import com.suvojeet.suvmusic.core.model.BrowseCategory
import com.suvojeet.suvmusic.core.model.HomeSection
import com.suvojeet.suvmusic.core.model.Playlist
import com.suvojeet.suvmusic.core.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.model.VideoQuality
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.SessionManager.StoredAccount
import com.suvojeet.suvmusic.data.repository.youtube.account.YouTubeAccountService
import com.suvojeet.suvmusic.data.repository.youtube.browse.YouTubeBrowseService
import com.suvojeet.suvmusic.data.repository.youtube.catalog.YouTubeCatalogService
import com.suvojeet.suvmusic.data.repository.youtube.internal.YouTubeLocale
import com.suvojeet.suvmusic.data.repository.youtube.library.YouTubeLibraryActionService
import com.suvojeet.suvmusic.data.repository.youtube.lyrics.YouTubeLyricsService
import com.suvojeet.suvmusic.data.repository.youtube.playlist.AddToPlaylistResult
import com.suvojeet.suvmusic.data.repository.youtube.playlist.YouTubePlaylistService
import com.suvojeet.suvmusic.data.repository.youtube.search.YouTubeSearchService
import com.suvojeet.suvmusic.data.repository.youtube.streaming.YouTubeStreamingService
import com.suvojeet.suvmusic.di.ApplicationScope
import com.suvojeet.suvmusic.newpipe.NewPipeDownloaderImpl
import com.suvojeet.suvmusic.providers.lyrics.Lyrics
import com.suvojeet.suvmusic.util.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.Localization
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Entry point for everything the app reads from YouTube Music.
 *
 * The work itself lives in focused services under `data/repository/youtube/` — search,
 * streaming, playlists, browse, catalog, library actions, lyrics and accounts. This class
 * is the seam they're reached through: it owns the one-time NewPipe bootstrap, applies the
 * "don't bother when offline" guard, and forwards each call. Keeping the facade means the
 * ~40 call sites across the app didn't have to learn the new layout.
 */
@Singleton
class YouTubeRepository @Inject constructor(
    private val sessionManager: SessionManager,
    private val streamingService: YouTubeStreamingService,
    private val searchService: YouTubeSearchService,
    private val accountService: YouTubeAccountService,
    private val playlistService: YouTubePlaylistService,
    private val browseService: YouTubeBrowseService,
    private val catalogService: YouTubeCatalogService,
    private val libraryActionService: YouTubeLibraryActionService,
    private val lyricsService: YouTubeLyricsService,
    private val networkMonitor: NetworkMonitor,
    @ApplicationScope private val externalScope: CoroutineScope
) {
    companion object {
        private var isInitialized = false

        // One source of truth: the search service owns the filter tokens, and these aliases
        // keep the long-standing `YouTubeRepository.FILTER_*` call sites working.
        const val FILTER_SONGS = YouTubeSearchService.FILTER_SONGS
        const val FILTER_VIDEOS = YouTubeSearchService.FILTER_VIDEOS
        const val FILTER_ALBUMS = YouTubeSearchService.FILTER_ALBUMS
        const val FILTER_PLAYLISTS = YouTubeSearchService.FILTER_PLAYLISTS
        const val FILTER_ARTISTS = YouTubeSearchService.FILTER_ARTISTS

        /** Map language names to YouTube Music ISO codes (hl). */
        fun getLanguageCode(languageName: String): String = YouTubeLocale.languageCode(languageName)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    init {
        externalScope.launch { initializeNewPipe() }
    }

    private suspend fun initializeNewPipe() = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext
        try {
            NewPipe.init(
                NewPipeDownloaderImpl(okHttpClient) { sessionManager.getCookies() ?: "" },
                Localization.DEFAULT
            )
        } catch (e: Exception) {
            // A failed init must not be retried on every call — NewPipe-backed paths will
            // fail and fall through to the InnerTube ones.
        }
        isInitialized = true
    }

    fun isOnline(): Boolean = networkMonitor.isCurrentlyConnected()

    fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()

    // ============================================================================================
    // Account
    // ============================================================================================

    suspend fun fetchAccountInfo(): StoredAccount? = accountService.fetchAccountInfo()

    suspend fun getAvailableAccounts(): List<StoredAccount> = accountService.getAvailableAccounts()

    suspend fun switchAccount(account: StoredAccount) = accountService.switchAccount(account)

    // ============================================================================================
    // Search & streams
    // ============================================================================================

    suspend fun search(query: String, filter: String = FILTER_SONGS): List<Song> =
        if (isOnline()) searchService.search(query, filter) else emptyList()

    /** Null when the search could not be performed (offline / throttled), as opposed to no results. */
    suspend fun searchOrNull(query: String, filter: String = FILTER_SONGS): List<Song>? =
        if (isOnline()) searchService.searchOrNull(query, filter) else null

    suspend fun searchArtists(query: String): List<Artist> =
        if (isOnline()) searchService.searchArtists(query) else emptyList()

    suspend fun searchPlaylists(query: String): List<Playlist> =
        if (isOnline()) searchService.searchPlaylists(query) else emptyList()

    suspend fun searchAlbums(query: String): List<Album> =
        if (isOnline()) searchService.searchAlbums(query) else emptyList()

    suspend fun getSearchSuggestions(query: String): List<String> =
        if (isOnline()) searchService.getSearchSuggestions(query) else emptyList()

    suspend fun getStreamUrl(videoId: String, forceLow: Boolean = false): String? =
        if (isOnline()) streamingService.getStreamUrl(videoId, forceLow) else null

    suspend fun getVideoStreamUrl(videoId: String, quality: VideoQuality? = null, forceLow: Boolean = false): String? =
        streamingService.getVideoStreamUrl(videoId, quality, forceLow)

    suspend fun getVideoStreamResult(videoId: String, quality: VideoQuality? = null, forceLow: Boolean = false) =
        streamingService.getVideoStreamResult(videoId, quality, forceLow)

    suspend fun getStreamUrlForDownload(videoId: String): Pair<String, String>? =
        streamingService.getStreamUrlForDownload(videoId)

    /** @param maxResolution caps the muxed (video+audio) quality, e.g. 360 / 720 / 1080. */
    suspend fun getMuxedVideoStreamUrlForDownload(videoId: String, maxResolution: Int = 720): String? =
        streamingService.getMuxedVideoStreamUrlForDownload(videoId, maxResolution)

    suspend fun getSongDetails(videoId: String): Song? = streamingService.getSongDetails(videoId)

    /**
     * Related tracks for autoplay.
     *
     * YouTube Music's "next" API is the good source — real music tracks. The vanilla
     * YouTube watch-page sidebar is only a fallback, and even then only its music-looking
     * uploaders, because it otherwise leaks lyric edits, fan covers and vlogs into the queue.
     */
    suspend fun getRelatedSongs(videoId: String): List<Song> {
        val internalResults = try { searchService.getRelatedSongs(videoId) } catch (e: Exception) { emptyList() }

        val candidates = internalResults.ifEmpty {
            try {
                streamingService.getRelatedItems(videoId).filter { song ->
                    val artist = song.artist.lowercase()
                    artist.contains(" - topic") ||
                        artist.contains("vevo") ||
                        artist.contains("records") ||
                        artist.contains(" music")
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

        // Dedupe on id *and* on a title/artist fingerprint — the same track routinely comes
        // back under several video ids.
        val seenIds = mutableSetOf<String>()
        val seenFingerprints = mutableSetOf<String>()
        val fingerprintRegex = Regex("[^a-z0-9]")

        return candidates.filter { song ->
            val title = song.title.lowercase().replace(fingerprintRegex, "")
            val artist = song.artist.lowercase().replace(fingerprintRegex, "")
            val isNewId = seenIds.add(song.id)
            val isNewFingerprint = seenFingerprints.add("$title|$artist")
            isNewId && isNewFingerprint
        }
    }

    /**
     * Finds the official music video for a song, for switching into video mode — an art
     * track would otherwise just show a static image.
     */
    suspend fun getBestVideoId(song: Song): String = withContext(Dispatchers.IO) {
        if (!isOnline()) return@withContext song.id

        if (song.title.contains("Official Video", ignoreCase = true) ||
            song.title.contains("Music Video", ignoreCase = true)
        ) return@withContext song.id

        try {
            search("${song.title} ${song.artist} Official Video", FILTER_VIDEOS).firstOrNull()?.id ?: song.id
        } catch (e: Exception) {
            e.printStackTrace()
            song.id
        }
    }

    // ============================================================================================
    // Browse
    // ============================================================================================

    suspend fun getRecommendations(): List<Song> = browseService.getRecommendations()

    suspend fun getHomeSections(): List<HomeSection> = browseService.getHomeSections()

    suspend fun getHomeSectionsForMood(moodTitle: String): List<HomeSection> =
        browseService.getHomeSectionsForMood(moodTitle)

    suspend fun getBrowseSections(browseId: String): List<HomeSection> =
        browseService.getBrowseSections(browseId)

    suspend fun getMoodsAndGenres(): List<BrowseCategory> = browseService.getMoodsAndGenres()

    suspend fun getCategoryContent(browseId: String, params: String? = null, title: String? = null): List<Song> =
        browseService.getCategoryContent(browseId, params, title)

    // ============================================================================================
    // Playlists
    // ============================================================================================

    suspend fun getUserPlaylists(autoSave: Boolean = true): List<PlaylistDisplayItem> =
        playlistService.getUserPlaylists(autoSave)

    suspend fun getUserEditablePlaylists(): List<PlaylistDisplayItem> =
        playlistService.getUserEditablePlaylists()

    suspend fun getLikedMusic(fetchAll: Boolean = false): List<Song> = playlistService.getLikedMusic(fetchAll)

    suspend fun syncLikedSongs(fetchAll: Boolean = false): Boolean = playlistService.syncLikedSongs(fetchAll)

    suspend fun removeFromLikedCache(songId: String) = playlistService.removeFromLikedCache(songId)

    suspend fun getCachedPlaylist(playlistId: String): Playlist? = playlistService.getCachedPlaylist(playlistId)

    suspend fun getPlaylist(playlistId: String, autoSave: Boolean = false): Playlist =
        playlistService.getPlaylist(playlistId, autoSave)

    fun getPlaylistFlow(playlistId: String, autoSave: Boolean = false): Flow<Playlist> =
        playlistService.getPlaylistFlow(playlistId, autoSave)

    suspend fun getAutoMixPlaylist(playlistId: String): Playlist = playlistService.getAutoMixPlaylist(playlistId)

    suspend fun createPlaylist(
        title: String,
        description: String = "",
        privacyStatus: String = "PRIVATE"
    ): String? = playlistService.createPlaylist(title, description, privacyStatus)

    suspend fun addSongToPlaylist(playlistId: String, videoId: String): Boolean =
        playlistService.addSongToPlaylist(playlistId, videoId)

    suspend fun addSongsToPlaylist(playlistId: String, videoIds: List<String>): Boolean =
        playlistService.addSongsToPlaylist(playlistId, videoIds)

    /** Adds to a local or YouTube playlist, whichever [playlistId] names, and mirrors the cache. */
    suspend fun addSongsToAnyPlaylist(playlistId: String, songs: List<Song>): AddToPlaylistResult =
        playlistService.addSongsToAnyPlaylist(playlistId, songs)

    suspend fun removeSongFromPlaylist(playlistId: String, setVideoId: String): Boolean =
        playlistService.removeSongFromPlaylist(playlistId, setVideoId)

    suspend fun removeSongsFromPlaylist(playlistId: String, setVideoIds: List<String>): Boolean =
        playlistService.removeSongsFromPlaylist(playlistId, setVideoIds)

    /** Removes from an auto-generated playlist (My Top 50, Discover Mix, …) via feedback tokens. */
    suspend fun removeSongsFromAutoPlaylist(songs: List<Song>): Boolean =
        playlistService.removeSongsFromAutoPlaylist(songs)

    fun isAutoGeneratedPlaylist(playlistId: String): Boolean =
        playlistService.isAutoGeneratedPlaylist(playlistId)

    fun isLocalPlaylist(playlistId: String): Boolean = playlistService.isLocalPlaylist(playlistId)

    suspend fun moveSongInPlaylist(
        playlistId: String,
        setVideoId: String,
        predecessorSetVideoId: String?
    ): Boolean = playlistService.moveSongInPlaylist(playlistId, setVideoId, predecessorSetVideoId)

    suspend fun renamePlaylist(playlistId: String, newTitle: String, newDescription: String? = null): Boolean =
        playlistService.renamePlaylist(playlistId, newTitle, newDescription)

    suspend fun deletePlaylist(playlistId: String): Boolean = playlistService.deletePlaylist(playlistId)

    // ============================================================================================
    // Artists & albums
    // ============================================================================================

    suspend fun getArtist(browseId: String): Artist? = catalogService.getArtist(browseId)

    suspend fun getAlbum(browseId: String): Album? = catalogService.getAlbum(browseId)

    suspend fun getLibraryArtists(): List<Artist> = catalogService.getLibraryArtists()

    suspend fun getLibraryAlbums(): List<Album> = catalogService.getLibraryAlbums()

    suspend fun getArtistRadioId(artistId: String): String? = catalogService.getArtistRadioId(artistId)

    suspend fun getArtistTopSongs(artistName: String, artistId: String): List<Song> =
        catalogService.getArtistTopSongs(artistName, artistId)

    // ============================================================================================
    // Library actions
    // ============================================================================================

    /** @param rating one of LIKE, DISLIKE, INDIFFERENT. */
    suspend fun rateSong(videoId: String, rating: String): Boolean =
        libraryActionService.rateSong(videoId, rating)

    /** @param rating one of LIKE, DISLIKE, INDIFFERENT. */
    suspend fun ratePlaylist(playlistId: String, rating: String): Boolean =
        libraryActionService.ratePlaylist(playlistId, rating)

    suspend fun subscribe(channelId: String, isSubscribe: Boolean): Boolean =
        libraryActionService.subscribe(channelId, isSubscribe)

    suspend fun fetchAndSyncHistory() = libraryActionService.fetchAndSyncHistory()

    suspend fun fetchYouTubeMusicHistory(): List<Song> = libraryActionService.fetchYouTubeMusicHistory()

    suspend fun fetchYouTubeHistory(musicOnly: Boolean = true): List<Song> =
        libraryActionService.fetchYouTubeHistory(musicOnly)

    suspend fun markAsWatched(videoId: String, durationSeconds: Int = 30) =
        libraryActionService.markAsWatched(videoId, durationSeconds)

    // ============================================================================================
    // Lyrics
    // ============================================================================================

    suspend fun getLyrics(videoId: String): Lyrics? = lyricsService.getLyrics(videoId)
}
