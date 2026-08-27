package com.suvojeet.suvmusic.lastfm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LastFmRepository @Inject constructor(
    private val lastFmClient: LastFmClient
) {

    suspend fun fetchSession(token: String): Result<Authentication> = withContext(Dispatchers.IO) {
        lastFmClient.fetchSession(token)
    }

    suspend fun getMobileSession(username: String, password: String): Result<Authentication> = withContext(Dispatchers.IO) {
        lastFmClient.getMobileSession(username, password)
    }

    suspend fun updateNowPlaying(sessionKey: String, artist: String, track: String, album: String?, duration: Long) = withContext(Dispatchers.IO) {
        lastFmClient.updateNowPlaying(sessionKey, artist, track, album, duration).also { result ->
            result.onFailure { android.util.Log.w("LastFmRepository", "updateNowPlaying failed for $artist – $track: ${it.message}") }
        }
    }

    suspend fun scrobble(sessionKey: String, artist: String, track: String, album: String?, duration: Long, timestamp: Long) = withContext(Dispatchers.IO) {
        lastFmClient.scrobble(sessionKey, artist, track, album, duration, timestamp).also { result ->
            result.onFailure { android.util.Log.w("LastFmRepository", "scrobble failed for $artist – $track: ${it.message}") }
        }
    }

    suspend fun setLoveStatus(sessionKey: String, artist: String, track: String, love: Boolean) = withContext(Dispatchers.IO) {
        lastFmClient.setLoveStatus(sessionKey, artist, track, love)
    }

    suspend fun getRecommendedArtists(sessionKey: String, limit: Int = 20, page: Int = 1) = withContext(Dispatchers.IO) {
        lastFmClient.getRecommendedArtists(sessionKey, limit, page)
    }

    suspend fun getRecommendedTracks(sessionKey: String, limit: Int = 20, page: Int = 1) = withContext(Dispatchers.IO) {
        lastFmClient.getRecommendedTracks(sessionKey, limit, page)
    }
    
    fun getAuthUrl(): String = lastFmClient.getAuthUrl()
}
