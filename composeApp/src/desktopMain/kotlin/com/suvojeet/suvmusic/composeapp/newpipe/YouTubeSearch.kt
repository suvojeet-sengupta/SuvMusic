package com.suvojeet.suvmusic.composeapp.newpipe

import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.core.model.SongSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/**
 * Tiny façade over NewPipe Extractor for the Desktop window. Initialises
 * NewPipe lazily on first use so we don't pay the cost on app launch
 * (Mozilla Rhino warm-up + service descriptor build).
 *
 * Exposes two methods:
 *  - [search] returns up to ~20 YouTube results for a query
 *  - [resolveStreamSong] turns a result item's URL into a playable [Song]
 *    with [Song.streamUrl] populated by the best-quality audio-only
 *    stream NewPipe surfaces.
 *
 * All blocking work runs on [Dispatchers.IO]. NewPipe's calls are
 * synchronous network/parse operations — wrapping in withContext keeps
 * them off the Compose UI thread.
 */
object YouTubeSearch {

    @Volatile
    private var initialized = false

    private fun ensureInitialized() {
        if (initialized) return
        synchronized(this) {
            if (!initialized) {
                NewPipe.init(JvmDownloader())
                initialized = true
            }
        }
    }

    suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        ensureInitialized()
        if (query.isBlank()) return@withContext emptyList()
        val service = ServiceList.YouTube
        val info: SearchInfo = SearchInfo.getInfo(
            service,
            service.searchQHFactory.fromQuery(query),
        )
        info.relatedItems
            .filterIsInstance<StreamInfoItem>()
            .map { item ->
                SearchResult(
                    title = item.name.orEmpty(),
                    uploader = item.uploaderName.orEmpty(),
                    durationSeconds = item.duration,
                    url = item.url,
                    thumbnailUrl = item.thumbnails?.firstOrNull()?.url,
                )
            }
    }

    suspend fun resolveStreamSong(result: SearchResult): Song? = withContext(Dispatchers.IO) {
        ensureInitialized()
        val info: StreamInfo = StreamInfo.getInfo(result.url)
        val audioStream = info.audioStreams
            .filterNotNull()
            .maxByOrNull { it.averageBitrate }
            ?: return@withContext null
        Song(
            id = info.id ?: result.url.hashCode().toString(),
            title = info.name ?: result.title,
            artist = info.uploaderName ?: result.uploader,
            album = "YouTube",
            duration = info.duration * 1000L,
            thumbnailUrl = result.thumbnailUrl,
            source = SongSource.YOUTUBE,
            streamUrl = audioStream.content,
        )
    }
}

data class SearchResult(
    val title: String,
    val uploader: String,
    val durationSeconds: Long,
    val url: String,
    val thumbnailUrl: String?,
)
