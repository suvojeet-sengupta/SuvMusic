package com.suvojeet.suvmusic.data.repository.youtube.internal

import com.suvojeet.suvmusic.core.model.Album
import com.suvojeet.suvmusic.core.model.Artist
import com.suvojeet.suvmusic.core.model.ArtistPreview
import com.suvojeet.suvmusic.core.model.BrowseCategory
import com.suvojeet.suvmusic.core.model.HomeItem
import com.suvojeet.suvmusic.core.model.HomeSection
import com.suvojeet.suvmusic.core.model.HomeSectionType
import com.suvojeet.suvmusic.core.model.Playlist
import com.suvojeet.suvmusic.core.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.providers.lyrics.Lyrics
import com.suvojeet.suvmusic.providers.lyrics.LyricsLine
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns InnerTube JSON into app models.
 *
 * The layering is deliberate: [YouTubeJsonParser] knows how to dig a *field* out of a
 * renderer (title runs, thumbnails, durations), and this class knows how those fields
 * compose into a Song, Playlist, Artist or home shelf. Everything that used to be a
 * private parser on YouTubeRepository lives here so the playlist, browse and catalog
 * services all share one implementation instead of three near-copies.
 */
@Singleton
class YouTubeContentParser @Inject constructor(
    private val json: YouTubeJsonParser
) {

    // ============================================================================================
    // Songs
    // ============================================================================================

    fun parseSongs(response: String): List<Song> = parseSongs(response, includeTwoRowItems = true)

    /**
     * Parses the user's liked-track feed without collecting musicTwoRowItemRenderer shelves.
     * Those shelves represent albums/playlists and can contain playable related tracks that
     * are not individually liked.
     */
    fun parseLikedSongs(response: String): List<Song> = parseSongs(response, includeTwoRowItems = false)

    private fun parseSongs(response: String, includeTwoRowItems: Boolean): List<Song> {
        val songs = mutableListOf<Song>()
        try {
            val root = JSONObject(response)
            val items = mutableListOf<JSONObject>()
            json.findAllObjects(root, "musicResponsiveListItemRenderer", items)
            if (includeTwoRowItems) {
                json.findAllObjects(root, "musicTwoRowItemRenderer", items)
            }
            json.findAllObjects(root, "videoRenderer", items) // Support for main YouTube history

            items.forEach { item ->
                try {
                    // musicResponsiveListItemRenderer can be many things — a watchEndpoint is
                    // what marks it as playable, a lone browseEndpoint means album or artist.
                    val navEndpoint = item.optJSONObject("navigationEndpoint")
                    val watchEndpoint = navEndpoint?.optJSONObject("watchEndpoint")
                        ?: item.optJSONObject("watchEndpoint")
                        ?: item.optJSONObject("playlistItemData")

                    val browseEndpoint = navEndpoint?.optJSONObject("browseEndpoint")
                    if (browseEndpoint != null && watchEndpoint == null) {
                        return@forEach
                    }

                    var videoId = watchEndpoint?.optString("videoId")
                        ?: item.optString("videoId")

                    if (videoId.isNullOrBlank()) {
                        videoId = json.extractValueFromRuns(item, "videoId")
                    }

                    if (!videoId.isNullOrBlank()) {
                        val duration = json.extractDuration(item)

                        // A two-row (grid) card without a duration is an album or playlist
                        // tile, not a track.
                        val isTwoRow = item.has("title") && item.has("subtitle") && !item.has("flexColumns")
                        if (isTwoRow && duration <= 0) {
                            return@forEach
                        }

                        Song.fromYouTube(
                            videoId = videoId,
                            title = json.extractTitle(item),
                            artist = json.extractArtist(item),
                            album = "",
                            duration = duration,
                            thumbnailUrl = json.extractThumbnail(item),
                            setVideoId = item.optJSONObject("playlistItemData")?.optString("videoId"),
                            releaseDate = json.extractYear(item)
                        )?.copy(removalFeedbackToken = extractRemovalFeedbackToken(item))
                            ?.let { songs.add(it) }
                    }
                } catch (e: Exception) { }
            }
        } catch (e: Exception) { }
        return songs.distinctBy { it.setVideoId ?: it.id }
    }

    /**
     * Pulls the "Remove from playlist" feedback token out of an item's overflow menu.
     *
     * Auto-generated playlists (My Supermix / My Top 50, Discover Mix, …) reject the
     * `edit_playlist` remove action — YouTube expects a one-shot feedback token instead,
     * which is only ever handed out inside the item's own menu. Carrying it on the Song is
     * what makes removal from those playlists possible at all.
     */
    private fun extractRemovalFeedbackToken(item: JSONObject): String? {
        val menu = item.optJSONObject("menu") ?: return null
        val serviceItems = mutableListOf<JSONObject>()
        json.findAllObjects(menu, "menuServiceItemRenderer", serviceItems)

        serviceItems.forEach { entry ->
            val label = json.getRunText(entry.optJSONObject("text")).orEmpty()
            if (!label.contains("remove", ignoreCase = true)) return@forEach
            val token = entry.optJSONObject("serviceEndpoint")
                ?.optJSONObject("feedbackEndpoint")
                ?.optString("feedbackToken")
            if (!token.isNullOrBlank()) return token
        }
        return null
    }

    // ============================================================================================
    // Playlists
    // ============================================================================================

    fun parsePlaylist(response: String, playlistId: String): Playlist {
        val root = JSONObject(response)
        val header = playlistHeader(root)

        val title = json.getRunText(header?.optJSONObject("title"))
            ?: header?.optJSONObject("title")?.optString("simpleText")
            ?: "Unknown Playlist"

        val subtitle = json.getRunText(header?.optJSONObject("subtitle"))
            ?: json.getRunText(header?.optJSONObject("straplineTextOne"))
        val author = subtitle?.split("•")?.firstOrNull()
            ?.replace("Playlist", "")
            ?.replace("Auto playlist", "YouTube Music")
            ?.trim()
            ?.takeIf { it.isNotBlank() && it.lowercase() != "unknown" }
            ?: ""

        val description = json.getRunText(header?.optJSONObject("description"))
            ?: json.getRunText(header?.optJSONObject("descriptionText"))

        var thumbnailUrl = json.extractHeaderThumbnail(header)
            ?: croppedSquareThumbnail(header)
            ?: header?.optJSONObject("foregroundThumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")
                ?.let { arr -> arr.optJSONObject(arr.length() - 1)?.optString("url") }

        val songs = parseSongs(response)
        if (thumbnailUrl == null && songs.isNotEmpty()) {
            thumbnailUrl = songs.firstOrNull()?.thumbnailUrl
        }

        return Playlist(
            id = playlistId,
            title = title,
            author = author,
            thumbnailUrl = thumbnailUrl,
            songs = songs,
            description = description,
            totalSongCount = subtitle?.let { extractSongCount(it) }
        )
    }

    fun parsePlaylistDisplayItems(response: String): List<PlaylistDisplayItem> {
        val playlists = mutableListOf<PlaylistDisplayItem>()
        try {
            val root = JSONObject(response)
            val items = mutableListOf<JSONObject>()
            json.findAllObjects(root, "musicTwoRowItemRenderer", items)

            items.forEach { item ->
                try {
                    val browseId = item.optJSONObject("navigationEndpoint")
                        ?.optJSONObject("browseEndpoint")?.optString("browseId")

                    if (browseId != null && (browseId.startsWith("VL") || browseId.startsWith("PL"))) {
                        val cleanId = browseId.removePrefix("VL")
                        val title = json.getRunText(item.optJSONObject("title")) ?: "Unknown Playlist"
                        val subtitle = json.getRunText(item.optJSONObject("subtitle")) ?: "Unknown"

                        val parts = subtitle.split("•").map { it.trim() }
                        val uploaderName = parts.firstOrNull { !it.contains("song", ignoreCase = true) } ?: subtitle

                        playlists.add(
                            PlaylistDisplayItem(
                                id = cleanId,
                                name = title,
                                url = "https://music.youtube.com/playlist?list=$cleanId",
                                uploaderName = uploaderName,
                                thumbnailUrl = json.extractThumbnail(item),
                                songCount = extractSongCount(subtitle)
                            )
                        )
                    }
                } catch (e: Exception) { }
            }
        } catch (e: Exception) { }
        return playlists
    }

    fun parseAutoMixQueue(response: String): List<Song> {
        val songs = mutableListOf<Song>()
        if (response.isBlank()) return songs
        try {
            val contents = JSONObject(response)
                .optJSONObject("contents")
                ?.optJSONObject("singleColumnMusicWatchNextResultsRenderer")
                ?.optJSONObject("tabbedRenderer")
                ?.optJSONObject("watchNextTabbedResultsRenderer")
                ?.optJSONArray("tabs")
                ?.optJSONObject(0)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("musicQueueRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("playlistPanelRenderer")
                ?.optJSONArray("contents") ?: return songs

            for (i in 0 until contents.length()) {
                val item = contents.optJSONObject(i)
                    ?.optJSONObject("playlistPanelVideoRenderer") ?: continue
                val videoId = item.optString("videoId").takeIf { it.isNotBlank() } ?: continue

                Song.fromYouTube(
                    videoId = videoId,
                    title = json.getRunText(item.optJSONObject("title")) ?: "Unknown",
                    artist = json.getRunText(item.optJSONObject("shortBylineText")) ?: "Unknown Artist",
                    album = "",
                    duration = json.extractDuration(item),
                    thumbnailUrl = json.extractThumbnail(item)
                )?.copy(removalFeedbackToken = extractRemovalFeedbackToken(item))
                    ?.let { songs.add(it) }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "parseAutoMixQueue failed", e)
        }
        return songs
    }

    // ============================================================================================
    // Albums & artists
    // ============================================================================================

    fun parseAlbum(response: String, albumId: String): Album {
        val root = JSONObject(response)
        val header = playlistHeader(root)

        val title = json.getRunText(header?.optJSONObject("title"))
            ?: header?.optJSONObject("title")?.optString("simpleText")
            ?: "Unknown Album"

        val subtitle = json.getRunText(header?.optJSONObject("subtitle"))
            ?: json.getRunText(header?.optJSONObject("straplineTextOne"))

        val description = json.getRunText(header?.optJSONObject("description"))
            ?: json.getRunText(header?.optJSONObject("descriptionText"))
            ?: json.getRunText(header?.optJSONObject("secondSubtitle"))

        var thumbnailUrl = json.extractHeaderThumbnail(header) ?: croppedSquareThumbnail(header)

        val songs = parseSongs(response)
        if (thumbnailUrl == null && songs.isNotEmpty()) {
            thumbnailUrl = songs.first().thumbnailUrl
        }

        return Album(
            id = albumId,
            title = title,
            artist = subtitle?.split("•")?.firstOrNull()?.trim()
                ?: songs.firstOrNull()?.artist
                ?: "Unknown Artist",
            year = subtitle?.split("•")?.find { it.trim().matches(Regex("\\d{4}")) } ?: subtitle,
            thumbnailUrl = thumbnailUrl,
            description = description,
            songs = songs
        )
    }

    fun parseArtist(response: String, artistId: String): Artist {
        val root = JSONObject(response)
        val header = root.optJSONObject("header")?.optJSONObject("musicImmersiveHeaderRenderer")
            ?: root.optJSONObject("header")?.optJSONObject("musicVisualHeaderRenderer")

        val name = json.getRunText(header?.optJSONObject("title")) ?: "Unknown Artist"
        val description = json.getRunText(header?.optJSONObject("description"))

        val thumbnailUrl = run {
            val thumbObj = header?.optJSONObject("thumbnail")
                ?: header?.optJSONObject("foregroundThumbnail")
            val thumbnails = thumbObj?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")
                ?: thumbObj?.optJSONArray("thumbnails")
            thumbnails?.let { arr ->
                if (arr.length() > 0) arr.optJSONObject(arr.length() - 1)?.optString("url") else null
            }
        }

        val subscriptionButton = header?.optJSONObject("subscriptionButton")
            ?.optJSONObject("subscribeButtonRenderer")
        val subscribers = json.getRunText(subscriptionButton?.optJSONObject("subscriberCountText"))
        val isSubscribed = subscriptionButton?.optBoolean("subscribed") ?: false
        val channelId = subscriptionButton?.optString("channelId") ?: artistId

        val songs = mutableListOf<Song>()
        val albums = mutableListOf<Album>()
        val singles = mutableListOf<Album>()
        val videos = mutableListOf<Song>()
        val relatedArtists = mutableListOf<ArtistPreview>()
        val featuredPlaylists = mutableListOf<Playlist>()

        val sections = mutableListOf<JSONObject>()
        json.findAllObjects(root, "musicShelfRenderer", sections)
        json.findAllObjects(root, "musicCarouselShelfRenderer", sections)

        sections.forEach { section ->
            val title = json.getRunText(
                section.optJSONObject("header")
                    ?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
                    ?.optJSONObject("title")
            ) ?: json.getRunText(section.optJSONObject("title"))

            val contents = section.optJSONArray("contents") ?: return@forEach

            when {
                title?.contains("Songs", ignoreCase = true) == true ||
                    title?.contains("Top songs", ignoreCase = true) == true -> {
                    for (i in 0 until contents.length()) {
                        val item = contents.optJSONObject(i)
                            ?.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                        val videoId = json.extractValueFromRuns(item, "videoId") ?: continue
                        Song.fromYouTube(
                            videoId = videoId,
                            title = json.extractTitle(item),
                            artist = json.extractArtist(item),
                            album = "",
                            duration = json.extractDuration(item),
                            thumbnailUrl = json.extractThumbnail(item)
                        )?.let { songs.add(it) }
                    }
                }
                title?.contains("Albums", ignoreCase = true) == true -> parseAlbumCards(contents, albums, name)
                title?.contains("Singles", ignoreCase = true) == true -> parseAlbumCards(contents, singles, name)
                title?.contains("Videos", ignoreCase = true) == true -> {
                    for (i in 0 until contents.length()) {
                        val item = contents.optJSONObject(i)
                            ?.optJSONObject("musicTwoRowItemRenderer") ?: continue
                        val videoId = item.optJSONObject("navigationEndpoint")
                            ?.optJSONObject("watchEndpoint")?.optString("videoId") ?: continue
                        Song.fromYouTube(
                            videoId = videoId,
                            title = json.getRunText(item.optJSONObject("title")) ?: "Unknown",
                            artist = name,
                            album = "",
                            duration = 0L, // Video cards don't carry a duration
                            thumbnailUrl = json.extractThumbnail(item)
                        )?.let { videos.add(it) }
                    }
                }
                title?.contains("Fans might also like", ignoreCase = true) == true ||
                    title?.contains("Related", ignoreCase = true) == true ->
                    parseRelatedArtists(contents, relatedArtists)
                title?.contains("Featured on", ignoreCase = true) == true ->
                    parseFeaturedPlaylists(contents, featuredPlaylists)
            }
        }

        return Artist(
            id = artistId,
            name = name,
            thumbnailUrl = thumbnailUrl,
            description = description,
            subscribers = subscribers,
            songs = songs,
            albums = albums,
            singles = singles,
            isSubscribed = isSubscribed,
            channelId = channelId,
            videos = videos,
            relatedArtists = relatedArtists,
            featuredPlaylists = featuredPlaylists
        )
    }

    fun parseArtistRadioId(response: String): String? = try {
        val root = JSONObject(response)
        val header = root.optJSONObject("header")?.optJSONObject("musicImmersiveHeaderRenderer")
            ?: root.optJSONObject("header")?.optJSONObject("musicVisualHeaderRenderer")
        header?.optJSONObject("startRadioButton")
            ?.optJSONObject("buttonRenderer")
            ?.optJSONObject("navigationEndpoint")
            ?.optJSONObject("watchEndpoint")
            ?.optString("playlistId")
            ?.takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        null
    }

    private fun parseAlbumCards(contents: JSONArray, target: MutableList<Album>, artistName: String) {
        for (i in 0 until contents.length()) {
            val item = contents.optJSONObject(i)?.optJSONObject("musicTwoRowItemRenderer") ?: continue
            val browseId = item.optJSONObject("navigationEndpoint")
                ?.optJSONObject("browseEndpoint")?.optString("browseId") ?: continue

            target.add(
                Album(
                    id = browseId,
                    title = json.getRunText(item.optJSONObject("title")) ?: "Unknown Album",
                    artist = artistName,
                    year = json.getRunText(item.optJSONObject("subtitle")),
                    thumbnailUrl = json.extractThumbnail(item)
                )
            )
        }
    }

    private fun parseRelatedArtists(contents: JSONArray, target: MutableList<ArtistPreview>) {
        for (i in 0 until contents.length()) {
            val item = contents.optJSONObject(i)?.optJSONObject("musicTwoRowItemRenderer") ?: continue
            val browseEndpoint = item.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
            val browseId = browseEndpoint?.optString("browseId") ?: continue

            if (!browseEndpoint.optString("browseEndpointContextSupportedConfigs")
                    .contains("MUSIC_PAGE_TYPE_ARTIST") && !browseId.startsWith("UC")
            ) continue

            target.add(
                ArtistPreview(
                    browseId,
                    json.getRunText(item.optJSONObject("title")) ?: "Unknown",
                    json.extractThumbnail(item),
                    json.getRunText(item.optJSONObject("subtitle"))
                )
            )
        }
    }

    private fun parseFeaturedPlaylists(contents: JSONArray, target: MutableList<Playlist>) {
        for (i in 0 until contents.length()) {
            val item = contents.optJSONObject(i)?.optJSONObject("musicTwoRowItemRenderer") ?: continue
            val browseId = item.optJSONObject("navigationEndpoint")
                ?.optJSONObject("browseEndpoint")?.optString("browseId") ?: continue
            if (!browseId.startsWith("VL")) continue

            target.add(
                Playlist(
                    browseId.removePrefix("VL"),
                    json.getRunText(item.optJSONObject("title")) ?: "Unknown",
                    json.getRunText(item.optJSONObject("subtitle")) ?: "Unknown",
                    json.extractThumbnail(item),
                    emptyList()
                )
            )
        }
    }

    // ============================================================================================
    // Library grids
    // ============================================================================================

    private data class LibraryGridItem(
        val browseId: String,
        val title: String,
        val thumbnailUrl: String?,
        val subtitle: String?
    )

    private fun <T> parseLibraryGrid(response: String, map: (LibraryGridItem) -> T): List<T> {
        val results = mutableListOf<T>()
        try {
            val contents = browseSectionContents(JSONObject(response)) ?: return results

            for (i in 0 until contents.length()) {
                val item = contents.optJSONObject(i)
                val gridItems = item?.optJSONObject("gridRenderer")?.optJSONArray("items")
                    ?: item?.optJSONObject("itemSectionRenderer")?.optJSONArray("contents")
                    ?: continue

                for (j in 0 until gridItems.length()) {
                    val renderer = gridItems.optJSONObject(j)
                        ?.optJSONObject("musicTwoRowItemRenderer") ?: continue
                    val title = json.getRunText(renderer.optJSONObject("title")) ?: continue
                    val browseId = renderer.optJSONObject("navigationEndpoint")
                        ?.optJSONObject("browseEndpoint")?.optString("browseId") ?: ""
                    if (browseId.isEmpty()) continue

                    results.add(
                        map(
                            LibraryGridItem(
                                browseId = browseId,
                                title = title,
                                thumbnailUrl = json.extractThumbnail(renderer),
                                subtitle = json.getRunText(renderer.optJSONObject("subtitle"))
                            )
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return results
    }

    fun parseLibraryArtists(response: String): List<Artist> =
        parseLibraryGrid(response) { item ->
            Artist(
                id = item.browseId,
                name = item.title,
                thumbnailUrl = item.thumbnailUrl,
                subscribers = item.subtitle
            )
        }

    fun parseLibraryAlbums(response: String): List<Album> =
        parseLibraryGrid(response) { item ->
            // Subtitle is usually "Artist • Year".
            val parts = (item.subtitle ?: "").split("•").map { it.trim() }
            Album(
                id = item.browseId,
                title = item.title,
                artist = parts.firstOrNull() ?: "",
                thumbnailUrl = item.thumbnailUrl,
                year = parts.getOrNull(1) ?: "",
                songs = emptyList() // Details fetched separately
            )
        }

    // ============================================================================================
    // Home & browse shelves
    // ============================================================================================

    fun parseHomeSections(response: String): List<HomeSection> {
        val sections = mutableListOf<HomeSection>()
        try {
            val root = JSONObject(response)
            val contents = browseSectionContents(root)
                ?: root.optJSONObject("continuationContents")
                    ?.optJSONObject("sectionListContinuation")
                    ?.optJSONArray("contents")
                ?: return sections

            for (i in 0 until contents.length()) {
                val sectionObj = contents.optJSONObject(i) ?: continue

                // 1. Carousels (horizontal scroll)
                val carouselShelf = sectionObj.optJSONObject("musicCarouselShelfRenderer")
                    ?: sectionObj.optJSONObject("musicImmersiveCarouselShelfRenderer")
                if (carouselShelf != null) {
                    val header = carouselShelf.optJSONObject("header")
                    val title = json.getRunText(
                        header?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")?.optJSONObject("title")
                    ) ?: json.getRunText(
                        header?.optJSONObject("musicImmersiveCarouselShelfBasicHeaderRenderer")?.optJSONObject("title")
                    ) ?: ""

                    val items = parseShelfItems(carouselShelf.optJSONArray("contents"))
                    if (items.isNotEmpty()) {
                        val type = if (title.contains("Community", ignoreCase = true)) {
                            HomeSectionType.CommunityCarousel
                        } else {
                            HomeSectionType.HorizontalCarousel
                        }
                        sections.add(HomeSection(title, items, type))
                    }
                }

                // 2. Shelves (vertical list) — "Your Likes", "Listen again", "Quick picks"
                val shelf = sectionObj.optJSONObject("musicShelfRenderer")
                if (shelf != null) {
                    val title = json.getRunText(shelf.optJSONObject("title")) ?: ""
                    val items = parseShelfItems(shelf.optJSONArray("contents"))
                    if (items.isNotEmpty()) {
                        val type = when {
                            title.contains("Listen again", ignoreCase = true) ||
                                title.contains("Recommended", ignoreCase = true) ||
                                title.contains("Your top", ignoreCase = true) ||
                                title.contains("Mixed for you", ignoreCase = true) ||
                                title.contains("Made for you", ignoreCase = true) -> HomeSectionType.Grid
                            else -> HomeSectionType.VerticalList
                        }
                        sections.add(HomeSection(title, items, type))
                    }
                }

                // 3. Grids
                val grid = sectionObj.optJSONObject("gridRenderer")
                if (grid != null) {
                    val title = json.getRunText(
                        grid.optJSONObject("header")?.optJSONObject("gridHeaderRenderer")?.optJSONObject("title")
                    ) ?: ""
                    val items = parseShelfItems(grid.optJSONArray("items"))
                    if (items.isNotEmpty()) {
                        sections.add(HomeSection(title, items, HomeSectionType.Grid))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return sections
    }

    /** Charts shelves, narrowed to the trending/top-songs carousels we surface on home. */
    fun parseChartsSections(response: String): List<HomeSection> {
        val sections = mutableListOf<HomeSection>()
        try {
            val contents = browseSectionContents(JSONObject(response)) ?: return sections

            for (i in 0 until contents.length()) {
                val carouselShelf = contents.optJSONObject(i)
                    ?.optJSONObject("musicCarouselShelfRenderer") ?: continue
                val title = json.getRunText(
                    carouselShelf.optJSONObject("header")
                        ?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
                        ?.optJSONObject("title")
                ) ?: ""

                if (!title.contains("Trending", ignoreCase = true) &&
                    !title.contains("Top songs", ignoreCase = true)
                ) continue

                val items = parseShelfItems(carouselShelf.optJSONArray("contents"))
                if (items.isNotEmpty()) {
                    sections.add(HomeSection(title, items, homeSectionTypeFor(title)))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return sections
    }

    /** The shelf layout a section title implies. Shared by charts and search-built shelves. */
    fun homeSectionTypeFor(title: String): HomeSectionType = when {
        title.contains("Quick picks", ignoreCase = true) -> HomeSectionType.VerticalList
        title.contains("Fresh finds", ignoreCase = true) ||
            title.contains("Listen again", ignoreCase = true) -> HomeSectionType.Grid
        title.contains("Community", ignoreCase = true) -> HomeSectionType.LargeCardWithList
        else -> HomeSectionType.HorizontalCarousel
    }

    private fun parseShelfItems(itemsArray: JSONArray?): List<HomeItem> {
        if (itemsArray == null) return emptyList()
        val items = mutableListOf<HomeItem>()
        for (j in 0 until itemsArray.length()) {
            parseHomeItem(itemsArray.optJSONObject(j))?.let { items.add(it) }
        }
        return items
    }

    fun parseHomeItem(itemObj: JSONObject?): HomeItem? {
        if (itemObj == null) return null

        // 1. Responsive list item — songs, videos, episodes, sometimes playlists/albums.
        val responsiveItem = itemObj.optJSONObject("musicResponsiveListItemRenderer")
        if (responsiveItem != null) {
            val videoId = json.extractValueFromRuns(responsiveItem, "videoId")
                ?: responsiveItem.optString("videoId").takeIf { it.isNotEmpty() }
                ?: responsiveItem.optJSONObject("playlistItemData")?.optString("videoId")

            val title = json.extractTitle(responsiveItem)
            val artist = json.extractArtist(responsiveItem)
            val thumbnail = json.extractThumbnail(responsiveItem)

            val browseId = responsiveItem.optJSONObject("navigationEndpoint")
                ?.optJSONObject("browseEndpoint")?.optString("browseId")

            if (browseId != null) {
                val subtitle = json.extractFullSubtitle(responsiveItem)
                if (isPlaylistBrowseId(browseId)) {
                    return HomeItem.PlaylistItem(
                        PlaylistDisplayItem(
                            id = browseId,
                            name = title,
                            url = "https://music.youtube.com/playlist?list=$browseId",
                            uploaderName = artist,
                            thumbnailUrl = thumbnail,
                            songCount = extractSongCount(subtitle)
                        )
                    )
                } else if (isAlbumBrowseId(browseId)) {
                    return HomeItem.AlbumItem(
                        Album(
                            id = browseId,
                            title = title,
                            artist = artist,
                            year = "",
                            thumbnailUrl = thumbnail
                        )
                    )
                }
            }

            if (!videoId.isNullOrEmpty()) {
                return Song.fromYouTube(
                    videoId = videoId,
                    title = title,
                    artist = artist,
                    album = "",
                    duration = json.extractDuration(responsiveItem),
                    thumbnailUrl = thumbnail
                )?.let { HomeItem.SongItem(it) }
            }
        }

        // 2. Two-row card — albums, playlists, artists, video cards.
        val twoRowItem = itemObj.optJSONObject("musicTwoRowItemRenderer")
        if (twoRowItem != null) {
            val title = json.getRunText(twoRowItem.optJSONObject("title")) ?: "Unknown"
            val subtitle = json.getRunText(twoRowItem.optJSONObject("subtitle")) ?: ""
            val thumbnail = json.extractThumbnail(twoRowItem)

            val navEndpoint = twoRowItem.optJSONObject("navigationEndpoint")
            val browseId = navEndpoint?.optJSONObject("browseEndpoint")?.optString("browseId")
            val videoId = navEndpoint?.optJSONObject("watchEndpoint")?.optString("videoId")

            if (browseId != null) {
                if (isPlaylistBrowseId(browseId) || browseId.startsWith("RTM")) {
                    return HomeItem.PlaylistItem(
                        PlaylistDisplayItem(
                            id = browseId,
                            name = title,
                            url = "https://music.youtube.com/playlist?list=$browseId",
                            uploaderName = subtitle,
                            thumbnailUrl = thumbnail,
                            songCount = extractSongCount(subtitle)
                        )
                    )
                } else if (isAlbumBrowseId(browseId)) {
                    val parts = subtitle.split("•").map { it.trim() }
                    return HomeItem.AlbumItem(
                        Album(
                            id = browseId,
                            title = title,
                            artist = parts.firstOrNull() ?: "",
                            year = parts.getOrNull(1) ?: "",
                            thumbnailUrl = thumbnail
                        )
                    )
                } else if (browseId.startsWith("UC")) {
                    return HomeItem.ArtistItem(
                        Artist(
                            id = browseId,
                            name = title,
                            thumbnailUrl = thumbnail,
                            description = null,
                            subscribers = subtitle
                        )
                    )
                }
            }

            if (videoId != null) {
                return Song.fromYouTube(
                    videoId = videoId,
                    title = title,
                    artist = subtitle,
                    album = "",
                    duration = json.extractDuration(twoRowItem),
                    thumbnailUrl = thumbnail
                )?.let { HomeItem.SongItem(it) }
            }
        }

        return null
    }

    fun parseMoodsAndGenres(response: String): List<BrowseCategory> {
        val categories = mutableListOf<BrowseCategory>()
        try {
            val buttonRenderers = mutableListOf<JSONObject>()
            json.findAllObjects(JSONObject(response), "musicNavigationButtonRenderer", buttonRenderers)

            buttonRenderers.forEach { renderer ->
                val title = json.getRunText(renderer.optJSONObject("buttonText"))
                    ?: json.getRunText(renderer.optJSONObject("title"))
                    ?: return@forEach

                val browseEndpoint = renderer.optJSONObject("clickCommand")?.optJSONObject("browseEndpoint")
                    ?: renderer.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
                    ?: return@forEach

                val browseId = browseEndpoint.optString("browseId")
                if (browseId.isBlank()) return@forEach

                categories.add(
                    BrowseCategory(
                        title = title,
                        browseId = browseId,
                        params = browseEndpoint.optString("params").takeIf { it.isNotBlank() },
                        thumbnailUrl = null,
                        color = renderer.optJSONObject("solid")
                            ?.optJSONObject("leftStripeColor")
                            ?.optLong("value")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return categories.distinctBy { "${it.browseId}:${it.params ?: ""}:${it.title.lowercase()}" }
    }

    // ============================================================================================
    // Lyrics
    // ============================================================================================

    fun extractLyricsBrowseId(nextJson: JSONObject): String? {
        val tabs = nextJson.optJSONObject("contents")
            ?.optJSONObject("singleColumnMusicWatchNextResultsRenderer")
            ?.optJSONObject("tabbedRenderer")
            ?.optJSONObject("watchNextTabbedResultsRenderer")
            ?.optJSONArray("tabs") ?: return null

        for (i in 0 until tabs.length()) {
            val tab = tabs.optJSONObject(i)?.optJSONObject("tabRenderer") ?: continue
            val title = tab.optString("title") // Often "Lyrics"
            val browseId = tab.optJSONObject("endpoint")
                ?.optJSONObject("browseEndpoint")?.optString("browseId")

            // Confirm it's lyrics — the id is usually "MPLYt…"
            if (browseId != null && (title.equals("Lyrics", ignoreCase = true) || browseId.startsWith("MPLY"))) {
                return browseId
            }
        }
        return null
    }

    fun parseLyrics(browseJson: JSONObject): Lyrics? {
        val contents = browseJson.optJSONObject("contents")
            ?.optJSONObject("sectionListRenderer")
            ?.optJSONArray("contents") ?: return null

        var timedLyrics: JSONObject? = null
        var descriptionShelf: JSONObject? = null

        for (i in 0 until contents.length()) {
            val item = contents.optJSONObject(i)
            if (timedLyrics == null) timedLyrics = item?.optJSONObject("musicTimedLyricsRenderer")
            if (descriptionShelf == null) descriptionShelf = item?.optJSONObject("musicDescriptionShelfRenderer")
        }

        // 1. Synced lyrics.
        val lyricData = timedLyrics?.optJSONArray("timedLyricsData")
        if (lyricData != null) {
            val lines = mutableListOf<LyricsLine>()
            for (i in 0 until lyricData.length()) {
                val lineObj = lyricData.optJSONObject(i)
                val text = lineObj?.optString("lyricLine") ?: ""
                if (text.isNotBlank()) {
                    lines.add(LyricsLine(text = text, startTimeMs = lineObj?.optLong("cueRangeStartMillis") ?: 0L))
                }
            }
            if (lines.isNotEmpty()) {
                val footer = json.getRunText(timedLyrics.optJSONObject("footer"))
                    ?: json.getRunText(descriptionShelf?.optJSONObject("footer"))
                return Lyrics(lines, footer, true)
            }
        }

        // 2. Plain text fallback.
        val description = json.getRunText(descriptionShelf?.optJSONObject("description"))
        if (description != null) {
            return Lyrics(
                description.split("\r\n", "\n").map { LyricsLine(it) },
                json.getRunText(descriptionShelf?.optJSONObject("footer")),
                false
            )
        }

        return null
    }

    // ============================================================================================
    // Shared helpers
    // ============================================================================================

    /**
     * The track count advertised in a subtitle like "Playlist • 1,234 songs • 2024".
     * The *last* match wins because the leading number is often a year or an artist count.
     */
    fun extractSongCount(subtitle: String): Int {
        if (subtitle.isBlank()) return 0

        val songCountRegex = Regex("([\\d,]+)\\+?\\s*(song|track|item|video|music)", RegexOption.IGNORE_CASE)
        val matches = songCountRegex.findAll(subtitle).toList()
        if (matches.isNotEmpty()) {
            matches.last().groupValues.getOrNull(1)?.let { countStr ->
                countStr.replace(",", "").toIntOrNull()?.let { return it }
            }
        }

        // Some compact cards carry the bare number.
        if (subtitle.trim().all { it.isDigit() || it == ',' }) {
            return subtitle.trim().replace(",", "").toIntOrNull() ?: 0
        }

        return 0
    }

    private fun isPlaylistBrowseId(browseId: String): Boolean =
        browseId.startsWith("VL") || browseId.startsWith("PL") ||
            browseId.startsWith("RD") || browseId == "LM"

    private fun isAlbumBrowseId(browseId: String): Boolean =
        browseId.startsWith("MPRE") || browseId.startsWith("OLAK")

    /** Playlist and album pages ship the same header under four different renderer names. */
    private fun playlistHeader(root: JSONObject): JSONObject? {
        val header = root.optJSONObject("header") ?: return null
        return header.optJSONObject("musicDetailHeaderRenderer")
            ?: header.optJSONObject("musicResponsiveHeaderRenderer")
            ?: header.optJSONObject("musicEditablePlaylistDetailHeaderRenderer")
                ?.optJSONObject("header")?.optJSONObject("musicDetailHeaderRenderer")
            ?: header.optJSONObject("musicEditablePlaylistDetailHeaderRenderer")
                ?.optJSONObject("header")?.optJSONObject("musicResponsiveHeaderRenderer")
    }

    private fun croppedSquareThumbnail(header: JSONObject?): String? =
        header?.optJSONObject("thumbnail")
            ?.optJSONObject("croppedSquareThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
            ?.let { arr -> arr.optJSONObject(arr.length() - 1)?.optString("url") }

    private fun browseSectionContents(root: JSONObject): JSONArray? =
        root.optJSONObject("contents")
            ?.optJSONObject("singleColumnBrowseResultsRenderer")
            ?.optJSONArray("tabs")
            ?.optJSONObject(0)
            ?.optJSONObject("tabRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("sectionListRenderer")
            ?.optJSONArray("contents")

    private companion object {
        const val TAG = "YouTubeContentParser"
    }
}
