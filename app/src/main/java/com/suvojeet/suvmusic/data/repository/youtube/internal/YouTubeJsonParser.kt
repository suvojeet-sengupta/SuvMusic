package com.suvojeet.suvmusic.data.repository.youtube.internal

import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class AccountInfo(
    val name: String,
    val email: String,
    val avatarUrl: String,
    val isSelected: Boolean,
    val authUserIndex: Int = 0,
    val pageId: String = "" // For brand accounts that might need pageId instead of index
)

/**
 * Utility class for parsing YouTube Music JSON responses.
 */
@Singleton
class YouTubeJsonParser @Inject constructor() {

    // --- Core JSON Traversal ---

    fun findAllObjects(node: Any, key: String, results: MutableList<JSONObject>) {
        if (node is JSONObject) {
            if (node.has(key)) {
                results.add(node.getJSONObject(key))
            }
            val keys = node.keys()
            while (keys.hasNext()) {
                val nextKey = keys.next()
                findAllObjects(node.get(nextKey), key, results)
            }
        } else if (node is JSONArray) {
            for (i in 0 until node.length()) {
                findAllObjects(node.get(i), key, results)
            }
        }
    }

    fun getRunText(formattedString: JSONObject?): String? {
        if (formattedString == null) return null
        if (formattedString.has("simpleText")) {
            return formattedString.optString("simpleText")
        }
        val runs = formattedString.optJSONArray("runs") ?: return null
        val sb = StringBuilder()
        for (i in 0 until runs.length()) {
            sb.append(runs.optJSONObject(i)?.optString("text") ?: "")
        }
        return sb.toString()
    }

    fun extractValueFromRuns(item: JSONObject, key: String): String? {
        val endpoints = mutableListOf<JSONObject>()
        findAllObjects(item, "watchEndpoint", endpoints)
        return endpoints.firstOrNull()?.optString("videoId")
    }

    // --- Item Extraction Methods ---

    fun extractFullSubtitle(item: JSONObject): String {
        val flexColumns = item.optJSONArray("flexColumns")
        if (flexColumns != null) {
            val subtitleFormatted = flexColumns.optJSONObject(1)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
            return getRunText(subtitleFormatted) ?: ""
        }
        return getRunText(item.optJSONObject("subtitle")) ?: ""
    }

    fun extractTitle(item: JSONObject): String {
        val flexColumns = item.optJSONArray("flexColumns")
        val titleFormatted = flexColumns?.optJSONObject(0)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
        return getRunText(titleFormatted) ?: getRunText(item.optJSONObject("title")) ?: "Unknown"
    }

    fun extractArtist(item: JSONObject): String {
        val flexColumns = item.optJSONArray("flexColumns")
        val subtitleFormatted = flexColumns?.optJSONObject(1)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
        
        // Use getRunText to get the full subtitle string (concatenated runs)
        // This ensures we get "Artist A & Artist B" instead of just "Artist A"
        val fullSubtitle = getRunText(subtitleFormatted) 
            ?: getRunText(item.optJSONObject("subtitle"))
            ?: return "Unknown Artist"
            
        // Split by " • " separator which divides Metadata (Artist • Album • Year)
        // The first part is usually the Artist(s)
        return fullSubtitle.split(" • ").firstOrNull()?.trim() ?: "Unknown Artist"
    }

    fun extractThumbnail(item: JSONObject?): String? {
        if (item == null) return null
        val thumbnails = item.optJSONObject("thumbnail")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
            ?: item.optJSONObject("thumbnailRenderer")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")
            ?: item.optJSONArray("thumbnails") // For header thumbnail

        return thumbnails?.let { it.optJSONObject(it.length() - 1)?.optString("url") }
    }

    fun extractHeaderThumbnail(header: JSONObject?): String? {
        if (header == null) return null

        // Standard musicThumbnailRenderer path
        val thumbnails = header.optJSONObject("thumbnail")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")

        if (thumbnails != null && thumbnails.length() > 0) {
            return thumbnails.optJSONObject(thumbnails.length() - 1)?.optString("url")
        }

        // Alternative path for some playlists
        val altThumbnails = header.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")

        if (altThumbnails != null && altThumbnails.length() > 0) {
            return altThumbnails.optJSONObject(altThumbnails.length() - 1)?.optString("url")
        }

        return null
    }

    fun extractDuration(item: JSONObject): Long {
        // Try fixedColumns (most common for list items)
        val fixedColumns = item.optJSONArray("fixedColumns")
        if (fixedColumns != null) {
            for (i in 0 until fixedColumns.length()) {
                val col = fixedColumns.optJSONObject(i)
                    ?.optJSONObject("musicResponsiveListItemFixedColumnRenderer")
                val text = getRunText(col?.optJSONObject("text"))
                if (text != null) {
                    val duration = parseDurationText(text)
                    if (duration > 0) return duration
                }
            }
        }

        // Try overlay for two-row items
        val overlayText = item.optJSONObject("overlay")
            ?.optJSONObject("musicItemThumbnailOverlayRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("musicPlayButtonRenderer")
            ?.optJSONObject("accessibilityPlayData")
            ?.optJSONObject("accessibilityData")
            ?.optString("label")
        if (overlayText != null) {
            // Try to parse duration from accessibility text like "Play Song Name - 3 minutes, 45 seconds"
            val durationMatch = Regex("(\\d+)\\s*minutes?,?\\s*(\\d+)?\\s*seconds?").find(overlayText)
            if (durationMatch != null) {
                val minutes = durationMatch.groupValues[1].toLongOrNull() ?: 0L
                val seconds = durationMatch.groupValues.getOrNull(2)?.toLongOrNull() ?: 0L
                return (minutes * 60 + seconds) * 1000L
            }
        }

        // Try subtitle runs for duration text
        val subtitleRuns = item.optJSONObject("subtitle")?.optJSONArray("runs")
        if (subtitleRuns != null) {
            for (i in 0 until subtitleRuns.length()) {
                val text = subtitleRuns.optJSONObject(i)?.optString("text") ?: continue
                val duration = parseDurationText(text)
                if (duration > 0) return duration
            }
        }

        return 0L
    }

    fun parseDurationText(text: String): Long {
        // Handle formats like "3:45", "1:23:45", "45"
        val parts = text.trim().split(":")
        return when (parts.size) {
            3 -> {
                // H:MM:SS
                val hours = parts[0].toLongOrNull() ?: return 0L
                val minutes = parts[1].toLongOrNull() ?: return 0L
                val seconds = parts[2].toLongOrNull() ?: return 0L
                (hours * 3600 + minutes * 60 + seconds) * 1000L
            }
            2 -> {
                // M:SS
                val minutes = parts[0].toLongOrNull() ?: return 0L
                val seconds = parts[1].toLongOrNull() ?: return 0L
                (minutes * 60 + seconds) * 1000L
            }
            1 -> {
                // Just seconds
                val seconds = parts[0].toLongOrNull() ?: return 0L
                seconds * 1000L
            }
            else -> 0L
        }
    }

    fun extractVideoId(url: String): String {
        val patterns = listOf(
            Regex("watch\\?v=([a-zA-Z0-9_-]+)"),
            Regex("youtu\\.be/([a-zA-Z0-9_-]+)"),
            Regex("music\\.youtube\\.com/watch\\?v=([a-zA-Z0-9_-]+)")
        )

        for (pattern in patterns) {
            pattern.find(url)?.groupValues?.getOrNull(1)?.let { return it }
        }
        return url
    }

    // --- Continuation Token Extraction ---

    fun extractContinuationToken(json: JSONObject): String? {
        try {
            // 1. Get the sectionListRenderer (common root)
            val sectionListRenderer = json.optJSONObject("contents")
                ?.optJSONObject("singleColumnBrowseResultsRenderer")
                ?.optJSONArray("tabs")
                ?.optJSONObject(0)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("sectionListRenderer")
                ?: json.optJSONObject("continuationContents")
                ?.optJSONObject("sectionListContinuation")

            // 2. Check for direct section continuations (infinite scroll on home/mixed lists)
            val sectionContinuations = sectionListRenderer?.optJSONArray("continuations")
            if (sectionContinuations != null) {
                return sectionContinuations.optJSONObject(0)
                    ?.optJSONObject("nextContinuationData")
                    ?.optString("continuation")
            }

            // 3. Look inside contents for shelf continuations (Playlists, Liked Songs, Shelves)
            val contents = sectionListRenderer?.optJSONArray("contents")
                ?: json.optJSONObject("contents")?.optJSONObject("sectionListRenderer")?.optJSONArray("contents")

            if (contents != null) {
                for (i in 0 until contents.length()) {
                    val item = contents.optJSONObject(i)

                    // Try MusicPlaylistShelfRenderer (Liked Songs, Playlists)
                    val playlistShelf = item?.optJSONObject("musicPlaylistShelfRenderer")
                        ?: json.optJSONObject("continuationContents")?.optJSONObject("musicPlaylistShelfContinuation")

                    var continuations = playlistShelf?.optJSONArray("continuations")
                    if (continuations != null) {
                        return continuations.optJSONObject(0)
                            ?.optJSONObject("nextContinuationData")
                            ?.optString("continuation")
                    }

                    // Try MusicShelfRenderer (Category lists, some playlists)
                    val musicShelf = item?.optJSONObject("musicShelfRenderer")
                        ?: json.optJSONObject("continuationContents")?.optJSONObject("musicShelfContinuation")

                    continuations = musicShelf?.optJSONArray("continuations")
                    if (continuations != null) {
                        return continuations.optJSONObject(0)
                            ?.optJSONObject("nextContinuationData")
                            ?.optString("continuation")
                    }
                }
            }

            // 4. Fallback: check root continuationContents directly (standard for next pages)
            val rootContinuation = json.optJSONObject("continuationContents")
            if (rootContinuation != null) {
                val playlistContinuation = rootContinuation.optJSONObject("musicPlaylistShelfContinuation")
                val shelfContinuation = rootContinuation.optJSONObject("musicShelfContinuation")
                val sectionContinuation = rootContinuation.optJSONObject("sectionListContinuation") // Added this

                val target = playlistContinuation ?: shelfContinuation ?: sectionContinuation
                val continuations = target?.optJSONArray("continuations")
                if (continuations != null) {
                    return continuations.optJSONObject(0)
                        ?.optJSONObject("nextContinuationData")
                        ?.optString("continuation")
                }
            }
            
            // 5. Recursive Fallback (Deep Search) - Matches "nextContinuationData" anywhere
            // This is crucial for deeply nested or varying structures
            return findContinuationTokenRecursive(json)

        } catch (e: Exception) {
            return null
        }
    }
    
    private fun findContinuationTokenRecursive(node: Any): String? {
        if (node is JSONObject) {
            if (node.has("nextContinuationData")) {
                return node.getJSONObject("nextContinuationData").optString("continuation")
            }
            val keys = node.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val res = findContinuationTokenRecursive(node.get(key))
                if (res != null) return res
            }
        } else if (node is JSONArray) {
            for (i in 0 until node.length()) {
                val res = findContinuationTokenRecursive(node.get(i))
                if (res != null) return res
            }
        }
        return null
    }

    // --- Account Menu Parsing ---

    fun parseAccountMenu(json: JSONObject): List<AccountInfo> {
        val accounts = mutableListOf<AccountInfo>()
        try {
            // Traverse to find 'multiPageMenuRenderer' -> 'sections'
            val actions = json.optJSONArray("actions") ?: return emptyList()
            
            var sections: JSONArray? = null
            
            for (i in 0 until actions.length()) {
                val action = actions.optJSONObject(i)
                val popup = action?.optJSONObject("openPopupAction")?.optJSONObject("popup")
                val multiPageMenu = popup?.optJSONObject("multiPageMenuRenderer")
                if (multiPageMenu != null) {
                    sections = multiPageMenu.optJSONArray("sections")
                    break
                }
            }

            if (sections == null) return emptyList()

            // Iterate through sections to find the one with accounts
            for (i in 0 until sections.length()) {
                val section = sections.optJSONObject(i)?.optJSONObject("multiPageMenuSectionRenderer")
                val items = section?.optJSONArray("items") ?: continue

                for (j in 0 until items.length()) {
                    val item = items.optJSONObject(j)?.optJSONObject("accountItemRenderer") ?: continue
                    
                    val name = getRunText(item.optJSONObject("accountName")) ?: "Unknown"
                    val email = getRunText(item.optJSONObject("datas")) ?: "" // Usually email or "Manage your Google Account"
                    val avatarUrl = item.optJSONObject("accountPhoto")?.optJSONArray("thumbnails")?.let {
                        it.optJSONObject(it.length() - 1)?.optString("url")
                    } ?: ""
                    
                    val isSelected = item.optBoolean("isSelected", false)
                    
                    // Extract authUser index from endpoint
                    // endpoint -> signInEndpoint -> (hack) we usually don't get direct index here easily
                    // But usually, the endpoint command metadata contains it, or we rely on the order
                    // Actually, for Brand Accounts, the 'navigationEndpoint' (which switches account) 
                    // usually contains specific signals. 
                    // However, standard Google switching often uses the 'googleAccountHeader' index.
                    
                    // Let's look for navigationEndpoint -> signInEndpoint -> nextUrl (or similar) to find ?authuser=X
                    val navEndpoint = item.optJSONObject("serviceEndpoint") 
                                    ?: item.optJSONObject("navigationEndpoint")
                                    
                    var authUserIndex = 0
                    var pageId = ""
                    
                    if (navEndpoint != null) {
                        val signInEndpoint = navEndpoint.optJSONObject("signInEndpoint")
                        val selectChannelEndpoint = navEndpoint.optJSONObject("selectChannelEndpoint")
                        
                        if (signInEndpoint != null) {
                            // Try to parse authuser from nextUrl or similar manually if possible?
                            // Actually, mostly the "authuser" index corresponds to the item order or is explicit in some fields
                            // But usually, we can infer it from the 'hacky' way: the `X-Goog-AuthUser` header maps to these.
                        }
                    }
                    
                    // Improved extraction: The `accountItemRenderer` usually doesn't strictly give us "authuser=1".
                    // But in the "account_menu" response, the items are usually listed.
                    // A trick is to check if these are clickable to SWITCH.
                    
                    // For now, we capture basic info. The switching logic might be complex if not using web-based switch.
                    // But if we use 'account_menu', we get a list.
                    
                    // NOTE: Implementing full robust extraction is hard without seeing the exact JSON for multiple brands.
                    // We will try to rely on a simpler assumption:
                    // If we are logged in, we are 'isSelected'=true.
                    
                    // Wait, usually the `actions` contain the list of accounts we *can* switch to.
                    // We need to parse valid `AccountInfo`. 
                    
                    accounts.add(AccountInfo(
                        name = name,
                        email = email,
                        avatarUrl = avatarUrl,
                        isSelected = isSelected,
                        authUserIndex = -1 // We'll fill this in Repository via smart guessing or additional fetches
                    ))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return accounts
    }
}
