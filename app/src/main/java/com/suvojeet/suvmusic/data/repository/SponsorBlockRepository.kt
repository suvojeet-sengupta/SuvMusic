package com.suvojeet.suvmusic.data.repository

import android.util.Log
import com.google.gson.annotations.SerializedName
import com.suvojeet.suvmusic.data.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlinx.coroutines.launch

data class SponsorSegment(
    @SerializedName("category") val category: String,
    @SerializedName("segment") val timeRange: List<Float>, // [start, end]
    @SerializedName("UUID") val uuid: String
) {
    val start: Float get() = timeRange.getOrElse(0) { 0f }
    val end: Float get() = timeRange.getOrElse(1) { 0f }
}

interface SponsorBlockApi {
    @GET("api/skipSegments")
    fun getSegments(
        @Query("videoID") videoId: String,
        @Query("categories") categories: String = "[\"sponsor\",\"selfpromo\",\"interaction\",\"intro\",\"outro\",\"music_offtopic\"]"
    ): Call<List<SponsorSegment>>
}

@Singleton
class SponsorBlockRepository @Inject constructor(
    private val sessionManager: SessionManager
) {

    private val api: SponsorBlockApi
    private val _currentSegments = MutableStateFlow<List<SponsorSegment>>(emptyList())
    val currentSegments: StateFlow<List<SponsorSegment>> = _currentSegments.asStateFlow()
    private var lastVideoId: String? = null
    private var lastSkippedSegmentUuid: String? = null
    private var isEnabled: Boolean = true
    
    // Cache for enabled categories to avoid runBlocking
    private var enabledCategories: Set<String> = emptySet()
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://sponsor.ajay.app/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(SponsorBlockApi::class.java)
        
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

        api.getSegments(videoId).enqueue(object : Callback<List<SponsorSegment>> {
            override fun onResponse(call: Call<List<SponsorSegment>>, response: Response<List<SponsorSegment>>) {
                if (response.isSuccessful && response.body() != null) {
                    val segments = response.body()!!
                    _currentSegments.value = segments
                    Log.d("SponsorBlock", "Loaded ${segments.size} segments")
                } else {
                    _currentSegments.value = emptyList()
                }
            }

            override fun onFailure(call: Call<List<SponsorSegment>>, t: Throwable) {
                Log.e("SponsorBlock", "Failed to load: ${t.message}")
            }
        })
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