package com.suvojeet.suvmusic.data.repository.youtube.playlist

import com.suvojeet.suvmusic.core.domain.repository.LibraryRepository
import com.suvojeet.suvmusic.core.model.Playlist
import com.suvojeet.suvmusic.core.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.repository.youtube.internal.YouTubeApiClient
import com.suvojeet.suvmusic.data.repository.youtube.internal.YouTubeContentParser
import com.suvojeet.suvmusic.data.repository.youtube.internal.YouTubeJsonParser
import com.suvojeet.suvmusic.data.repository.youtube.internal.YouTubeLocale
import com.suvojeet.suvmusic.util.NetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import javax.inject.Inject
import javax.inject.Singleton

/** Outcome of adding songs to a playlist, so callers don't each invent their own messages. */
sealed interface AddToPlaylistResult {
    data class Added(val count: Int) : AddToPlaylistResult
    data class PartiallyAdded(val count: Int, val total: Int) : AddToPlaylistResult
    data class AlreadyPresent(val songTitle: String) : AddToPlaylistResult
    data class Failed(val reason: String) : AddToPlaylistResult
}

/**
 * Everything about YouTube Music playlists: reading them, paginating them, and mutating
 * them — plus the local-library mirror that keeps edits visible before a re-fetch.
 */
@Singleton
class YouTubePlaylistService @Inject constructor(
    private val sessionManager: SessionManager,
    private val apiClient: YouTubeApiClient,
    private val parser: YouTubeContentParser,
    private val jsonParser: YouTubeJsonParser,
    private val networkMonitor: NetworkMonitor,
    private val libraryRepository: LibraryRepository
) {

    // ============================================================================================
    // Reading
    // ============================================================================================

    suspend fun getUserPlaylists(autoSave: Boolean = true): List<PlaylistDisplayItem> = withContext(Dispatchers.IO) {
        if (!networkMonitor.isCurrentlyConnected()) {
            return@withContext sessionManager.getCachedLibraryPlaylistsSync()
        }
        if (!sessionManager.isLoggedIn()) return@withContext emptyList()

        try {
            val hl = YouTubeLocale.hl(sessionManager)
            val response = apiClient.fetchInternalApi("FEmusic_liked_playlists", hl = hl)
            val ytPlaylists = parser.parsePlaylistDisplayItems(response)

            if (ytPlaylists.isNotEmpty()) {
                sessionManager.saveLibraryPlaylistsCache(ytPlaylists)

                // Saving into LibraryRepository duplicates local playlists and makes them
                // uneditable, so callers that just want to list opt out.
                if (autoSave) {
                    ytPlaylists.forEach { item ->
                        libraryRepository.savePlaylist(
                            Playlist(
                                id = item.id,
                                title = item.name,
                                author = item.uploaderName,
                                thumbnailUrl = item.thumbnailUrl,
                                songs = emptyList()
                            )
                        )
                    }
                }
            }
            ytPlaylists
        } catch (e: Exception) {
            e.printStackTrace()
            // Never return an empty library because one fetch failed — the cache is better
            // than nothing and is what the screen was already showing.
            sessionManager.getCachedLibraryPlaylistsSync()
        }
    }

    suspend fun getLikedMusic(fetchAll: Boolean = false): List<Song> = withContext(Dispatchers.IO) {
        // Offline-first: local data wins whenever we have any.
        val localSongs = libraryRepository.getCachedPlaylistSongs(LIKED_ID)
        if (localSongs.isNotEmpty()) return@withContext localSongs

        if (networkMonitor.isCurrentlyConnected() && sessionManager.isLoggedIn()) {
            syncLikedSongs(fetchAll)
            return@withContext libraryRepository.getCachedPlaylistSongs(LIKED_ID)
        }
        emptyList()
    }

    /**
     * Fetches every page into memory before touching the database, so the liked-songs count
     * never visibly counts up (0 → 25 → 50 …) while a sync is in flight.
     */
    suspend fun syncLikedSongs(fetchAll: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        if (!networkMonitor.isCurrentlyConnected() || !sessionManager.isLoggedIn()) return@withContext false

        try {
            val hl = YouTubeLocale.hl(sessionManager)
            val initialResponse = apiClient.fetchInternalApi("FEmusic_liked_videos", hl = hl)
            val initialSongs = parser.parseLikedSongs(initialResponse)
            if (initialSongs.isEmpty()) return@withContext false

            val allSongs = mutableListOf<Song>()
            allSongs.addAll(initialSongs)

            if (fetchAll) {
                allSongs.addAll(paginate(initialResponse, hl, maxPages = 500))
            }

            val distinctSongs = allSongs.distinctBy { it.setVideoId ?: it.id }
            libraryRepository.savePlaylist(
                Playlist(
                    id = LIKED_ID,
                    title = "Your Likes",
                    author = "You",
                    thumbnailUrl = distinctSongs.first().thumbnailUrl,
                    songs = emptyList() // Metadata only
                )
            )
            libraryRepository.replacePlaylistSongs(LIKED_ID, distinctSongs)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun removeFromLikedCache(songId: String) {
        libraryRepository.removeSongFromPlaylist(LIKED_ID, songId)
    }

    suspend fun getCachedPlaylist(playlistId: String): Playlist? = withContext(Dispatchers.IO) {
        val cachedSongs = libraryRepository.getCachedPlaylistSongs(playlistId)
        if (cachedSongs.isEmpty()) return@withContext null

        val cached = libraryRepository.getPlaylistById(playlistId)
        Playlist(
            id = playlistId,
            title = cached?.title ?: "Cached Playlist",
            author = cached?.subtitle ?: "",
            thumbnailUrl = cached?.thumbnailUrl,
            songs = cachedSongs
        )
    }

    suspend fun getPlaylist(playlistId: String, autoSave: Boolean = false): Playlist =
        withContext(Dispatchers.IO) { getPlaylistFlow(playlistId, autoSave).last() }

    /**
     * Emits the playlist as it loads — first page first, then each continuation — so a long
     * playlist shows content immediately instead of after every page has landed.
     */
    fun getPlaylistFlow(playlistId: String, autoSave: Boolean = false): Flow<Playlist> = flow {
        if (!networkMonitor.isCurrentlyConnected()) {
            getCachedPlaylist(playlistId)?.let {
                emit(it)
                return@flow
            }
        }

        if (playlistId == LIKED_ID || playlistId == "VL$LIKED_ID") {
            emitLikedPlaylist(playlistId, autoSave)
            return@flow
        }

        if (playlistId in SUPERMIX_IDS) {
            emitSupermix(playlistId)
            return@flow
        }

        if (emitBrowsePlaylist(playlistId, autoSave)) return@flow

        emitNewPipePlaylist(playlistId, autoSave)
    }.flowOn(Dispatchers.IO)

    private suspend fun kotlinx.coroutines.flow.FlowCollector<Playlist>.emitLikedPlaylist(
        playlistId: String,
        autoSave: Boolean
    ) {
        val empty = Playlist(
            id = playlistId,
            title = "Your Likes",
            author = "You",
            thumbnailUrl = null,
            songs = emptyList()
        )
        if (!sessionManager.isLoggedIn()) {
            emit(empty)
            return
        }

        try {
            val hl = YouTubeLocale.hl(sessionManager)
            val response = apiClient.fetchInternalApi("FEmusic_liked_videos", hl = hl)
            val songs = parser.parseLikedSongs(response).toMutableList()

            var playlist = empty.copy(
                thumbnailUrl = songs.firstOrNull()?.thumbnailUrl,
                songs = songs.toList()
            )
            emit(playlist)

            var currentJson = JSONObject(response)
            var continuationToken = jsonParser.extractContinuationToken(currentJson)
            var pageCount = 0
            while (continuationToken != null && pageCount < 200) { // ~20k songs
                val continuationResponse = apiClient.fetchInternalApiWithContinuation(continuationToken, hl = hl)
                if (continuationResponse.isEmpty()) break
                val newSongs = parser.parseLikedSongs(continuationResponse)
                if (newSongs.isEmpty()) break
                songs.addAll(newSongs)

                // Emitting every page would redraw a huge list constantly; every fourth
                // keeps the UI moving without thrashing it.
                if (pageCount % 4 == 0) {
                    playlist = playlist.copy(songs = songs.toList())
                    emit(playlist)
                }

                currentJson = JSONObject(continuationResponse)
                continuationToken = jsonParser.extractContinuationToken(currentJson)
                pageCount++
            }

            if (songs.isNotEmpty()) {
                playlist = playlist.copy(songs = songs.toList())
                if (autoSave) libraryRepository.savePlaylist(playlist)
                emit(playlist)
                return
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        emit(empty)
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<Playlist>.emitSupermix(playlistId: String) {
        val empty = Playlist(
            id = playlistId,
            title = "My Supermix",
            author = "YouTube Music",
            thumbnailUrl = SUPERMIX_THUMBNAIL,
            songs = emptyList()
        )
        if (sessionManager.isLoggedIn()) {
            try {
                val hl = YouTubeLocale.hl(sessionManager)
                val songs = parser.parseSongs(apiClient.fetchInternalApi("FEmusic_home", hl = hl))
                if (songs.isNotEmpty()) {
                    emit(empty.copy(songs = songs.take(500)))
                    return
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        emit(empty)
    }

    /** Returns true when the internal browse API produced the playlist. */
    private suspend fun kotlinx.coroutines.flow.FlowCollector<Playlist>.emitBrowsePlaylist(
        playlistId: String,
        autoSave: Boolean
    ): Boolean {
        try {
            val browseId = if (playlistId.startsWith("VL")) playlistId else "VL$playlistId"
            val response = apiClient.fetchInternalApi(browseId)
            var playlist = parser.parsePlaylist(response, playlistId)
            emit(playlist)

            val songs = playlist.songs.toMutableList()
            var currentJson = JSONObject(response)
            var continuationToken = jsonParser.extractContinuationToken(currentJson)
            var pageCount = 0
            // Auto-mixes are effectively endless; paging them out stalls Android Auto's
            // callback window, so they get a much shorter leash.
            val isAutoMix = playlistId.startsWith("RD") || playlistId.startsWith("RTM")
            val maxPages = if (isAutoMix) 5 else 100

            while (continuationToken != null && pageCount < maxPages) {
                val continuationResponse = apiClient.fetchInternalApiWithContinuation(continuationToken)
                if (continuationResponse.isEmpty()) break
                val newSongs = parser.parseSongs(continuationResponse)
                if (newSongs.isEmpty()) break
                songs.addAll(newSongs)

                playlist = playlist.copy(songs = songs.distinctBy { it.setVideoId ?: it.id })
                emit(playlist)

                currentJson = JSONObject(continuationResponse)
                continuationToken = jsonParser.extractContinuationToken(currentJson)
                pageCount++
            }

            if (songs.isNotEmpty()) {
                val finalPlaylist = playlist.copy(songs = songs.distinctBy { it.setVideoId ?: it.id })
                if (autoSave) libraryRepository.savePlaylist(finalPlaylist)
                emit(finalPlaylist)
                return true
            }
        } catch (e: Exception) { }
        return false
    }

    /** Public playlists that the authenticated browse endpoint won't serve. */
    private suspend fun kotlinx.coroutines.flow.FlowCollector<Playlist>.emitNewPipePlaylist(
        playlistId: String,
        autoSave: Boolean
    ) {
        try {
            val urlId = playlistId.removePrefix("VL")
            val ytService = ServiceList.all().find { it.serviceInfo.name == "YouTube" }
                ?: run {
                    emit(Playlist(playlistId, "Error", "", null, emptyList()))
                    return
                }

            val extractor = ytService.getPlaylistExtractor("https://www.youtube.com/playlist?list=$urlId")
            extractor.fetchPage()

            var playlist = Playlist(
                id = playlistId,
                title = try { extractor.name } catch (e: Exception) { null } ?: "Unknown Playlist",
                author = try { extractor.uploaderName } catch (e: Exception) { null } ?: "YouTube",
                thumbnailUrl = try { extractor.thumbnails.lastOrNull()?.url } catch (e: Exception) { null },
                songs = emptyList(),
                description = try { extractor.description.content } catch (e: Exception) { null }
            )

            val songs = mutableListOf<Song>()
            var page: ListExtractor.InfoItemsPage<StreamInfoItem>? = extractor.initialPage

            while (page != null) {
                songs.addAll(
                    page.items.filterIsInstance<StreamInfoItem>().mapNotNull { item ->
                        val videoId = jsonParser.extractVideoId(item.url)
                        Song.fromYouTube(
                            videoId = videoId,
                            title = item.name ?: "Unknown",
                            artist = item.uploaderName ?: "Unknown",
                            album = "",
                            duration = item.duration * 1000L,
                            thumbnailUrl = item.thumbnails.lastOrNull()?.url,
                            setVideoId = null
                        )
                    }
                )
                playlist = playlist.copy(songs = songs.toList())
                emit(playlist)

                if (!page.hasNextPage()) break
                page = extractor.getPage(page.nextPage)
            }

            if (autoSave && songs.isNotEmpty()) libraryRepository.savePlaylist(playlist)
        } catch (e: Exception) {
            getCachedPlaylist(playlistId)?.let { emit(it) }
        }
    }

    /**
     * Auto-generated mixes (Mixed For You, Replay Mix, Discover Mix, Supermix) come from
     * the /next endpoint, not /browse.
     */
    suspend fun getAutoMixPlaylist(playlistId: String): Playlist = withContext(Dispatchers.IO) {
        val fallback = Playlist(
            id = playlistId,
            title = com.suvojeet.suvmusic.player.AutoMix.resolveTitle(playlistId),
            author = "YouTube Music",
            thumbnailUrl = null,
            songs = emptyList()
        )
        try {
            val response = apiClient.fetchInternalApiWithBody("next", """{"playlistId": "$playlistId"}""")
            val songs = parser.parseAutoMixQueue(response)
            if (songs.isNotEmpty()) {
                return@withContext fallback.copy(thumbnailUrl = songs.first().thumbnailUrl, songs = songs)
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "getAutoMixPlaylist failed for $playlistId", e)
        }
        fallback
    }

    private suspend fun paginate(initialResponse: String, hl: String, maxPages: Int): List<Song> {
        val songs = mutableListOf<Song>()
        var currentJson = JSONObject(initialResponse)
        var continuationToken = jsonParser.extractContinuationToken(currentJson)
        var pageCount = 0

        while (continuationToken != null && pageCount < maxPages) {
            try {
                val response = apiClient.fetchInternalApiWithContinuation(continuationToken, hl = hl)
                if (response.isEmpty()) break
                val newSongs = parser.parseLikedSongs(response)
                if (newSongs.isEmpty()) break
                songs.addAll(newSongs)

                currentJson = JSONObject(response)
                continuationToken = jsonParser.extractContinuationToken(currentJson)
                pageCount++
            } catch (e: Exception) {
                break
            }
        }
        return songs
    }

    // ============================================================================================
    // Mutating
    // ============================================================================================

    suspend fun createPlaylist(
        title: String,
        description: String = "",
        privacyStatus: String = "PRIVATE"
    ): String? = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("title", title)
            put("description", description)
            put("privacyStatus", privacyStatus)
        }
        // Routed through the API client so it gets the same auth signing, visitor data and
        // rejected-response detection as every other mutation.
        val response = apiClient.performAuthenticatedActionForBody("playlist/create", body.toString())
            ?: return@withContext null
        try {
            JSONObject(response).optString("playlistId").takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun addSongToPlaylist(playlistId: String, videoId: String): Boolean =
        apiClient.editPlaylist(playlistId, listOf(addVideoAction(videoId)))

    suspend fun addSongsToPlaylist(playlistId: String, videoIds: List<String>): Boolean {
        if (videoIds.isEmpty()) return true
        // Chunked to stay under InnerTube's per-request action limit.
        return videoIds.chunked(50).all { chunk ->
            apiClient.editPlaylist(playlistId, chunk.map { addVideoAction(it) })
        }
    }

    suspend fun removeSongFromPlaylist(playlistId: String, setVideoId: String): Boolean =
        apiClient.editPlaylist(playlistId, listOf(removeVideoAction(setVideoId)))

    suspend fun removeSongsFromPlaylist(playlistId: String, setVideoIds: List<String>): Boolean {
        if (setVideoIds.isEmpty()) return false
        return setVideoIds.chunked(50).all { chunk ->
            apiClient.editPlaylist(playlistId, chunk.map { removeVideoAction(it) })
        }
    }

    /**
     * Removes songs from an auto-generated playlist (My Top 50, Discover Mix, …) using the
     * per-item feedback tokens carried on [Song.removalFeedbackToken].
     *
     * These playlists have no stable `setVideoId` and reject `edit_playlist` outright, so
     * this is the only removal path that works on them.
     */
    suspend fun removeSongsFromAutoPlaylist(songs: List<Song>): Boolean {
        val tokens = songs.mapNotNull { it.removalFeedbackToken }.distinct()
        if (tokens.isEmpty()) return false
        return apiClient.sendFeedback(tokens)
    }

    /** True if [playlistId] is one of YouTube's own generated playlists rather than the user's. */
    fun isAutoGeneratedPlaylist(playlistId: String): Boolean =
        playlistId in SUPERMIX_IDS ||
            playlistId == TOP_50_ID ||
            AUTO_GENERATED_PREFIXES.any { playlistId.startsWith(it) }

    suspend fun moveSongInPlaylist(
        playlistId: String,
        setVideoId: String,
        predecessorSetVideoId: String?
    ): Boolean {
        val action = playlistAction("ACTION_MOVE_VIDEO_AFTER") {
            put("setVideoId", setVideoId)
            if (predecessorSetVideoId != null) {
                put("movedSetVideoIdPredecessor", predecessorSetVideoId)
            }
        }
        return apiClient.editPlaylist(playlistId, listOf(action))
    }

    suspend fun renamePlaylist(playlistId: String, newTitle: String, newDescription: String? = null): Boolean {
        val actions = buildList {
            add(playlistAction("ACTION_SET_PLAYLIST_NAME") { put("playlistName", newTitle) })
            if (newDescription != null) {
                add(playlistAction("ACTION_SET_PLAYLIST_DESCRIPTION") { put("playlistDescription", newDescription) })
            }
        }
        return apiClient.editPlaylist(playlistId, actions)
    }

    suspend fun deletePlaylist(playlistId: String): Boolean =
        apiClient.performAuthenticatedAction(
            "playlist/delete",
            JSONObject().put("playlistId", playlistId.removePrefix("VL")).toString()
        )

    /**
     * The playlists a song can actually be added to: the user's local playlists plus their
     * own YouTube playlists.
     *
     * The YouTube filter is deliberately exclusion-based. Requiring the byline to *look
     * like* the user ("You", or the account name) emptied this list whenever YouTube
     * localised the byline or filled it with a track count instead — which is exactly when
     * the add-to-playlist sheet showed nothing to add to.
     */
    suspend fun getUserEditablePlaylists(): List<PlaylistDisplayItem> = withContext(Dispatchers.IO) {
        val playlists = mutableListOf<PlaylistDisplayItem>()

        try {
            libraryRepository.getSavedPlaylists().firstOrNull()?.let { playlists.addAll(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (!sessionManager.isLoggedIn()) return@withContext playlists.distinctBy { it.id }

        try {
            val userName = sessionManager.getUserName().orEmpty()
            getUserPlaylists(autoSave = false)
                .filter { isEditableYouTubePlaylist(it, userName) }
                .forEach { playlists.add(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        playlists.distinctBy { it.id }
    }

    private fun isEditableYouTubePlaylist(item: PlaylistDisplayItem, userName: String): Boolean {
        val id = item.id
        if (id.isEmpty()) return false
        if (isAutoGeneratedPlaylist(id) || id == LIKED_ID || id == "VL$LIKED_ID") return false

        val uploader = item.uploaderName.trim()
        // Own playlists come back bylined "You", as the account name, or as bare metadata
        // ("Playlist • 50 songs") — none of which positively identifies another owner.
        if (uploader.isEmpty()) return true
        if (uploader.equals("You", ignoreCase = true)) return true
        if (userName.isNotBlank() && uploader.contains(userName, ignoreCase = true)) return true
        if (uploader.contains("song", ignoreCase = true) ||
            uploader.contains("track", ignoreCase = true) ||
            uploader.contains("Playlist", ignoreCase = true) ||
            uploader.none { it.isLetter() }
        ) return true

        // A byline naming someone else means it's a saved playlist, not one we can edit.
        return false
    }

    /**
     * Adds [songs] to whichever kind of playlist [playlistId] is, and keeps the local cache
     * in step. Both the player sheet and the playlist screen route through here so their
     * duplicate handling, thumbnail backfill and cache mirroring can't drift apart.
     */
    suspend fun addSongsToAnyPlaylist(playlistId: String, songs: List<Song>): AddToPlaylistResult =
        withContext(Dispatchers.IO) {
            if (songs.isEmpty()) return@withContext AddToPlaylistResult.Failed("No songs selected")

            // Liked Songs is served from the local cache rather than a playlist edit, so it
            // takes the local path even though its id is a YouTube one.
            if (isLocalPlaylist(playlistId) || playlistId == LIKED_ID) {
                return@withContext addToLocalPlaylist(playlistId, songs)
            }

            if (!sessionManager.isLoggedIn()) {
                return@withContext AddToPlaylistResult.Failed("Sign in to edit YouTube playlists")
            }

            val success = addSongsToPlaylist(playlistId, songs.map { it.id })
            if (!success) {
                return@withContext AddToPlaylistResult.Failed("YouTube rejected the change")
            }
            mirrorAddToLocalCacheIfCached(playlistId, songs)
            AddToPlaylistResult.Added(songs.size)
        }

    private suspend fun addToLocalPlaylist(playlistId: String, songs: List<Song>): AddToPlaylistResult {
        var added = 0
        var duplicate: String? = null

        for (song in songs) {
            try {
                if (libraryRepository.isSongInPlaylist(playlistId, song.id)) {
                    duplicate = song.title
                    continue
                }
                libraryRepository.addSongToPlaylist(playlistId, song)

                // A playlist created empty has no art until its first song supplies some.
                val item = libraryRepository.getPlaylistById(playlistId)
                if (item != null && item.thumbnailUrl.isNullOrBlank()) {
                    libraryRepository.updatePlaylistThumbnail(playlistId, song.thumbnailUrl)
                }
                added++
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return when {
            added == songs.size -> AddToPlaylistResult.Added(added)
            added > 0 -> AddToPlaylistResult.PartiallyAdded(added, songs.size)
            duplicate != null -> AddToPlaylistResult.AlreadyPresent(duplicate)
            else -> AddToPlaylistResult.Failed("Couldn't add to playlist")
        }
    }

    /**
     * Appends to the local cache, but only when one already exists. Writing a cache from
     * scratch here would leave the local-first loader showing just the newly added songs
     * and hiding the rest until a full re-fetch.
     */
    private suspend fun mirrorAddToLocalCacheIfCached(playlistId: String, songs: List<Song>) {
        try {
            val cached = libraryRepository.getCachedPlaylistSongs(playlistId)
            if (cached.isEmpty()) return
            val existingIds = cached.map { it.id }.toSet()
            val toAdd = songs.filter { it.id !in existingIds }
            if (toAdd.isNotEmpty()) {
                libraryRepository.appendPlaylistSongs(playlistId, toAdd, cached.size)
            }
        } catch (e: Exception) {
            // Best-effort mirror — the authoritative add already succeeded upstream.
        }
    }

    fun isLocalPlaylist(playlistId: String): Boolean = playlistId.startsWith(LOCAL_PREFIX)

    private fun playlistAction(action: String, build: JSONObject.() -> Unit = {}): JSONObject =
        JSONObject().apply {
            put("action", action)
            build()
        }

    private fun addVideoAction(videoId: String): JSONObject =
        playlistAction("ACTION_ADD_VIDEO") { put("addedVideoId", videoId) }

    private fun removeVideoAction(setVideoId: String): JSONObject =
        playlistAction("ACTION_REMOVE_VIDEO") { put("setVideoId", setVideoId) }

    companion object {
        private const val TAG = "YouTubePlaylistService"

        const val LIKED_ID = "LM"
        const val TOP_50_ID = "TOP_50"
        const val LOCAL_PREFIX = "local_"

        /** Supermix, which "My Top 50" is backed by. */
        val SUPERMIX_IDS = setOf("RTM", "RDTMAK")

        private val AUTO_GENERATED_PREFIXES = listOf("RDAMPL", "RDCLAK", "RDGMUK", "RDTMAK", "RD", "RTM")

        private const val SUPERMIX_THUMBNAIL =
            "https://www.gstatic.com/youtube/media/ytm/images/pbg/liked_music_@576.png"
    }
}
