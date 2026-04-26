package com.suvojeet.suvmusic.data.repository

import android.util.Log
import com.suvojeet.suvmusic.data.SessionManager
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Serializable
data class SponsorSegment(
    @SerialName("category") val category: String,
    @SerialName("segment") val timeRange: List<Float>, // [start, end]
    @SerialName("UUID") val uuid: String,
) {
    val start: Float get() = timeRange.getOrElse(0) { 0f }
    val end: Float get() = timeRange.getOrElse(1) { 0f }
}

@Singleton
class SponsorBlockRepository @Inject constructor(
    private val sessionManager: SessionManager,
) {

    // Ktor replaces Retrofit (KMP phase 3a). CIO engine works on Android + Desktop
    // JVM; lenient JSON config matches the previous GsonConverterFactory behaviour
    // of accepting unknown extra fields the API may add later.
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                },
            )
        }
    }

    private val _currentSegments = MutableStateFlow<List<SponsorSegment>>(emptyList())
    val currentSegments: StateFlow<List<SponsorSegment>> = _currentSegments.asStateFlow()
    private var lastVideoId: String? = null
    private var lastSkippedSegmentUuid: String? = null
    private var isEnabled: Boolean = true

    // Cache for enabled categories to avoid runBlocking
    private var enabledCategories: Set<String> = emptySet()
    private val scope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob(),
    )

    init {
        // Monitor enabled categories changes
        scope.launch {
            sessionManager.sponsorBlockCategoriesFlow.collect {
                enabledCategories = it
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        if (!enabled) {
            _currentSegments.value = emptyList()
            lastVideoId = null
        }
    }

    fun loadSegments(videoId: String) {
        if (!isEnabled) return
        if (videoId == lastVideoId) return

        lastVideoId = videoId
        _currentSegments.value = emptyList()
        lastSkippedSegmentUuid = null

        Log.d("SponsorBlock", "Loading segments for $videoId")

        scope.launch {
            try {
                val response = httpClient.get("https://sponsor.ajay.app/api/skipSegments") {
                    parameter("videoID", videoId)
                    parameter(
                        "categories",
                        "[\"sponsor\",\"selfpromo\",\"interaction\",\"intro\",\"outro\",\"music_offtopic\"]",
                    )
                }
                val segments: List<SponsorSegment> = response.body()
                _currentSegments.value = segments
                Log.d("SponsorBlock", "Loaded ${segments.size} segments")
            } catch (t: Throwable) {
                Log.e("SponsorBlock", "Failed to load: ${t.message}")
                _currentSegments.value = emptyList()
            }
        }
    }

    /**
     * Finds the start time of the next segment that should be skipped.
     * Returns null if no segments are ahead.
     */
    fun getNextSegmentStart(currentSeconds: Float): Float? {
        if (!isEnabled) return null

        val segments = _currentSegments.value
        if (segments.isEmpty()) return null

        val currentEnabledCategories = enabledCategories

        return segments
            .filter { currentEnabledCategories.contains(it.category) && it.start > currentSeconds }
            .minOfOrNull { it.start }
    }

    /**
     * Checks if the current position falls within a segment that should be skipped.
     * Only skips if the category is enabled in settings.
     */
    fun checkSkip(currentSeconds: Float): Float? {
        if (!isEnabled) return null

        val segments = _currentSegments.value
        if (segments.isEmpty()) return null

        // Use cached categories - no blocking
        val currentEnabledCategories = enabledCategories

        for (segment in segments) {
            // Only skip if this specific category is enabled by the user
            if (currentEnabledCategories.contains(segment.category)) {
                if (currentSeconds >= segment.start && currentSeconds < segment.end) {
                    if (lastSkippedSegmentUuid == segment.uuid && abs(currentSeconds - segment.start) < 2.0) {
                        continue
                    }

                    Log.i("SponsorBlock", "Skipping ${segment.category}")
                    lastSkippedSegmentUuid = segment.uuid
                    return segment.end
                }
            }
        }
        return null
    }
}
