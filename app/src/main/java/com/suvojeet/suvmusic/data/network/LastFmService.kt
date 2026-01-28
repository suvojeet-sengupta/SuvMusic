package com.suvojeet.suvmusic.data.network

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface LastFmService {
    
    @FormUrlEncoded
    @POST("2.0/")
    suspend fun getMobileSession(
        @Field("method") method: String = "auth.getMobileSession",
        @Field("username") username: String, // Note: Not used in Web Auth flow, but kept for reference
        @Field("password") password: String,
        @Field("api_key") apiKey: String,
        @Field("api_sig") apiSig: String,
        @Field("format") format: String = "json"
    ): LastFmSessionResponse

    @FormUrlEncoded
    @POST("2.0/")
    suspend fun getSession(
        @Field("method") method: String = "auth.getSession",
        @Field("token") token: String,
        @Field("api_key") apiKey: String,
        @Field("api_sig") apiSig: String,
        @Field("format") format: String = "json"
    ): LastFmSessionResponse

    @FormUrlEncoded
    @POST("2.0/")
    suspend fun updateNowPlaying(
        @Field("method") method: String = "track.updateNowPlaying",
        @Field("artist") artist: String,
        @Field("track") track: String,
        @Field("album") album: String?,
        @Field("duration") duration: Int?, // in seconds
        @Field("api_key") apiKey: String,
        @Field("api_sig") apiSig: String,
        @Field("sk") sessionKey: String,
        @Field("format") format: String = "json"
    ): LastFmBaseResponse

    @FormUrlEncoded
    @POST("2.0/")
    suspend fun scrobble(
        @Field("method") method: String = "track.scrobble",
        @Field("artist") artist: String,
        @Field("track") track: String,
        @Field("album") album: String?,
        @Field("timestamp") timestamp: Long, // UNIX timestamp
        @Field("duration") duration: Int?, // in seconds
        @Field("api_key") apiKey: String,
        @Field("api_sig") apiSig: String,
        @Field("sk") sessionKey: String,
        @Field("format") format: String = "json"
    ): LastFmBaseResponse
}

data class LastFmSessionResponse(
    val session: LastFmSession
)

data class LastFmSession(
    val name: String,
    val key: String,
    val subscriber: Int
)

data class LastFmBaseResponse(
    val status: String? = null // Often "ok" or ignored as errors throw generic exceptions in some setups
)
