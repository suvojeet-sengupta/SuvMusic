package com.suvojeet.suvmusic.data.repository

import android.util.Log
import com.google.gson.annotations.SerializedName
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
class SponsorBlockRepository @Inject constructor() {

    private val api: SponsorBlockApi
    private var currentSegments: List<SponsorSegment> = emptyList()
    private var lastVideoId: String? = null
    private var lastSkippedSegmentUuid: String? = null
    private var isEnabled: Boolean = true

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://sponsor.ajay.app/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(SponsorBlockApi::class.java)
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        if (!enabled) {
            currentSegments = emptyList()
            lastVideoId = null
        }
    }

    fun loadSegments(videoId: String) {
        if (!isEnabled) return
        if (videoId == lastVideoId) return

        lastVideoId = videoId
        currentSegments = emptyList()
        lastSkippedSegmentUuid = null

        Log.d("SponsorBlock", "Loading segments for $videoId")

        api.getSegments(videoId).enqueue(object : Callback<List<SponsorSegment>> {
            override fun onResponse(call: Call<List<SponsorSegment>>, response: Response<List<SponsorSegment>>) {
                if (response.isSuccessful && response.body() != null) {
                    currentSegments = response.body()!!
                    // Log.d("SponsorBlock", "Loaded ${currentSegments.size} segments")
                } else {
                    currentSegments = emptyList()
                }
            }

            override fun onFailure(call: Call<List<SponsorSegment>>, t: Throwable) {
                // Log.e("SponsorBlock", "Failed to load: ${t.message}")
            }
        })
    }

    fun checkSkip(currentSeconds: Float): Float? {
        if (!isEnabled || currentSegments.isEmpty()) return null

        for (segment in currentSegments) {
            if (currentSeconds >= segment.start && currentSeconds < segment.end) {
                if (lastSkippedSegmentUuid == segment.uuid && abs(currentSeconds - segment.start) < 2.0) {
                    continue
                }

                Log.i("SponsorBlock", "Skipping ${segment.category}")
                lastSkippedSegmentUuid = segment.uuid
                return segment.end
            }
        }
        return null
    }
}