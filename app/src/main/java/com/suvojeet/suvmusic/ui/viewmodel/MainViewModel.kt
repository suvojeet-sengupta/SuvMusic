package com.suvojeet.suvmusic.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.model.UpdateState
import com.suvojeet.suvmusic.data.model.UpdateChannel
import com.suvojeet.suvmusic.data.repository.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

import androidx.media3.datasource.cache.Cache
import com.suvojeet.suvmusic.lastfm.LastFmRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

sealed class MainEvent {
    data class PlayFromDeepLink(val videoId: String) : MainEvent()
    data class PlayFromLocalUri(val uri: Uri) : MainEvent()
    data class ShowToast(val message: String) : MainEvent()
}

data class MainUiState(
    val updateState: UpdateState = UpdateState.Idle,
    val currentVersion: String = ""
)

@HiltViewModel
class MainViewModel @OptIn(androidx.media3.common.util.UnstableApi::class)
@Inject constructor(
    private val updateRepository: UpdateRepository,
    private val sessionManager: SessionManager,
    private val lastFmRepository: LastFmRepository,
    private val playerCache: Cache,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MainEvent>()
    val events: SharedFlow<MainEvent> = _events.asSharedFlow()

    private var downloadJob: Job? = null
    private var downloadedApkFile: File? = null

    init {
        // Load current version
        _uiState.update { it.copy(currentVersion = updateRepository.getCurrentVersionName()) }
        
        // Auto-check for updates on app startup
        checkForUpdates()
        
        // Auto-clear cache if needed
        checkAndClearCache()
    }

    @OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun checkAndClearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val intervalDays = sessionManager.getPlayerCacheAutoClearInterval()
            if (intervalDays > 0) {
                val lastCleared = sessionManager.getLastCacheClearedTimestamp()
                val intervalMillis = intervalDays * 24 * 60 * 60 * 1000L
                if (System.currentTimeMillis() - lastCleared > intervalMillis) {
                    try {
                        playerCache.keys.forEach { key ->
                            playerCache.removeResource(key)
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
                uri.scheme == "suvmusic" && uri.host == "play" -> {
                    val id = uri.getQueryParameter("id")
                    if (id != null) {
                        _events.emit(MainEvent.PlayFromDeepLink(id))
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

    private fun checkForUpdates() {
        viewModelScope.launch {
            // Optional: Delay slightly to not block startup or show dialog instantly
            delay(2000) 
            
            _uiState.update { it.copy(updateState = UpdateState.Checking) }

            val channel = sessionManager.getUpdateChannel()
            updateRepository.checkForUpdate(channel)
                .onSuccess { update ->
                    if (update != null) {
                        _uiState.update { it.copy(updateState = UpdateState.UpdateAvailable(update)) }
                    } else {
                        // Silent failure/idle if no update, don't show "No update" dialog on startup
                        _uiState.update { it.copy(updateState = UpdateState.Idle) }
                    }
                }
                .onFailure {
                    // Silent failure on startup
                    _uiState.update { it.copy(updateState = UpdateState.Idle) }
                }
        }
    }

    fun downloadUpdate(downloadUrl: String, versionName: String) {
        downloadJob = viewModelScope.launch {
            _uiState.update { it.copy(updateState = UpdateState.Downloading(0)) }

            updateRepository.downloadApk(
                downloadUrl = downloadUrl,
                versionName = versionName,
                onProgress = { progress ->
                    _uiState.update { it.copy(updateState = UpdateState.Downloading(progress)) }
                }
            ).onSuccess { file ->
                downloadedApkFile = file
                _uiState.update { it.copy(updateState = UpdateState.Downloaded) }
                installUpdate()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(updateState = UpdateState.Error(error.message ?: "Download failed"))
                }
            }
        }
    }

    fun installUpdate() {
        val apkFile = downloadedApkFile ?: return

        try {
            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    apkFile
                )
            } else {
                Uri.fromFile(apkFile)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            _uiState.update {
                it.copy(updateState = UpdateState.Error("Failed to install: ${e.message}"))
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _uiState.update { it.copy(updateState = UpdateState.Idle) }
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(updateState = UpdateState.Idle) }
    }
}
