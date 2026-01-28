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

@androidx.annotation.Keep
data class LastFmSessionResponse(
    @com.google.gson.annotations.SerializedName("session") val session: LastFmSession?,
    @com.google.gson.annotations.SerializedName("error") val error: Int? = null,
    @com.google.gson.annotations.SerializedName("message") val message: String? = null
)

@androidx.annotation.Keep
data class LastFmSession(
    @com.google.gson.annotations.SerializedName("name") val name: String,
    @com.google.gson.annotations.SerializedName("key") val key: String,
    @com.google.gson.annotations.SerializedName("subscriber") val subscriber: Int
)

@androidx.annotation.Keep
data class LastFmBaseResponse(
    @com.google.gson.annotations.SerializedName("status") val status: String? = null
)
