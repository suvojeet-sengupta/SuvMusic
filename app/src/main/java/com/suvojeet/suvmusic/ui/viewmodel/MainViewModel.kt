package com.suvojeet.suvmusic.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.suvojeet.suvmusic.lastfm.LastFmRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class MainEvent {
    data class PlayFromDeepLink(val videoId: String) : MainEvent()
    data class PlayFromLocalUri(val uri: Uri) : MainEvent()
    data class ShowToast(val message: String) : MainEvent()
    data class NavigateToAlbum(val browseId: String) : MainEvent()
    data class NavigateToPlaylist(val playlistId: String) : MainEvent()
    data class NavigateToArtist(val channelId: String) : MainEvent()
    data class NavigateToSearch(val query: String) : MainEvent()
}

data class MainUiState(
    val currentVersion: String = "",
    val isInPictureInPictureMode: Boolean = false,
    val isReady: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val lastFmRepository: LastFmRepository,
    private val playerCache: dagger.Lazy<androidx.media3.datasource.cache.Cache>,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MainEvent>()
    val events: SharedFlow<MainEvent> = _events.asSharedFlow()

    init {
        // App is now "ready" to remove splash screen (basic setup complete)
        viewModelScope.launch {
            // Auto-clear cache if needed
            checkAndClearCache()
            _uiState.update { it.copy(isReady = true) }
        }
    }

    private fun checkAndClearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val intervalDays = sessionManager.getPlayerCacheAutoClearInterval()
            if (intervalDays > 0) {
                val lastCleared = sessionManager.getLastCacheClearedTimestamp()
                val intervalMillis = intervalDays * 24 * 60 * 60 * 1000L
                if (System.currentTimeMillis() - lastCleared > intervalMillis) {
                    try {
                        val cache = playerCache.get()
                        cache.keys.forEach { key ->
                            cache.removeResource(key)
                        }
                        sessionManager.updateLastCacheClearedTimestamp()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun handleDeepLink(uri: Uri?) {
        if (uri == null) return
        
        viewModelScope.launch {
            when {
                uri.scheme == "suvmusic" && uri.host == "lastfm-auth" -> {
                    val token = uri.getQueryParameter("token")
                    if (token != null) {
                        lastFmRepository.fetchSession(token)
                            .onSuccess { auth ->
                                sessionManager.setLastFmSession(auth.session.key, auth.session.name)
                                _events.emit(MainEvent.ShowToast("Connected to Last.fm as ${auth.session.name}"))
                            }.onFailure {
                                _events.emit(MainEvent.ShowToast("Failed to connect to Last.fm"))
                            }
                    }
                }
                uri.scheme == "suvmusic" -> {
                    when (val target = com.suvojeet.suvmusic.deeplink.DeepLinkHandler.parse(uri)) {
                        is com.suvojeet.suvmusic.deeplink.DeepLinkTarget.Song ->
                            _events.emit(MainEvent.PlayFromDeepLink(target.id))
                        is com.suvojeet.suvmusic.deeplink.DeepLinkTarget.Album ->
                            _events.emit(MainEvent.NavigateToAlbum(target.id))
                        is com.suvojeet.suvmusic.deeplink.DeepLinkTarget.Playlist ->
                            _events.emit(MainEvent.NavigateToPlaylist(target.id))
                        is com.suvojeet.suvmusic.deeplink.DeepLinkTarget.Artist ->
                            _events.emit(MainEvent.NavigateToArtist(target.id))
                        is com.suvojeet.suvmusic.deeplink.DeepLinkTarget.Search ->
                            _events.emit(MainEvent.NavigateToSearch(target.query))
                        null -> Unit // unknown suvmusic:// URI — ignore
                    }
                }
                // Handle YouTube links
                isYouTubeLink(uri) -> {
                    val videoId = extractVideoId(uri)
                    if (videoId != null) {
                        _events.emit(MainEvent.PlayFromDeepLink(videoId))
                    }
                }
            }
        }
    }

    fun handleAudioIntent(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            _events.emit(MainEvent.PlayFromLocalUri(uri))
        }
    }

    private fun isYouTubeLink(uri: Uri): Boolean {
        val host = uri.host ?: return false
        return host.contains("youtube.com") || host.contains("youtu.be") || host.contains("music.youtube.com")
    }

    private fun extractVideoId(uri: Uri): String? {
        return try {
            val url = uri.toString()
            when {
                url.contains("youtu.be/") -> {
                    url.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
                }
                url.contains("/shorts/") -> {
                    url.substringAfter("/shorts/").substringBefore("?").substringBefore("&")
                }
                url.contains("v=") -> {
                    uri.getQueryParameter("v")
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun setPictureInPictureMode(inPip: Boolean) {
        _uiState.update { it.copy(isInPictureInPictureMode = inPip) }
    }
}
