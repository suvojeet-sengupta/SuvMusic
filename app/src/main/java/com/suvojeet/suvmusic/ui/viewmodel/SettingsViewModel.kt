package com.suvojeet.suvmusic.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.model.AppTheme
import com.suvojeet.suvmusic.data.model.AudioQuality
import com.suvojeet.suvmusic.data.model.DownloadQuality
import com.suvojeet.suvmusic.data.model.ThemeMode
import com.suvojeet.suvmusic.data.model.UpdateState
import com.suvojeet.suvmusic.data.repository.UpdateRepository
import com.suvojeet.suvmusic.data.MusicSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val isLoggedIn: Boolean = false,
    val userAvatarUrl: String? = null,
    val audioQuality: AudioQuality = AudioQuality.HIGH,
    val downloadQuality: DownloadQuality = DownloadQuality.HIGH,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appTheme: AppTheme = AppTheme.DEFAULT,
    val dynamicColorEnabled: Boolean = true,
    val gaplessPlaybackEnabled: Boolean = true,
    val automixEnabled: Boolean = true,
    val volumeSliderEnabled: Boolean = true,
    val musicSource: MusicSource = MusicSource.YOUTUBE,
    val updateState: UpdateState = UpdateState.Idle,
    val currentVersion: String = "",
    val doubleTapSeekSeconds: Int = 10,
    val volumeNormalizationEnabled: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val updateRepository: UpdateRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    // Developer mode - shows JioSaavn option when enabled
    val isDeveloperMode = sessionManager.developerModeFlow
    
    // Dynamic Island enabled state
    val dynamicIslandEnabled = sessionManager.dynamicIslandEnabledFlow
    
    // Offline Mode enabled state
    val offlineModeEnabled = sessionManager.offlineModeFlow
    
    // Volume Slider enabled state
    val volumeSliderEnabled = sessionManager.volumeSliderEnabledFlow
    
    suspend fun setDynamicIslandEnabled(enabled: Boolean) {
        sessionManager.setDynamicIslandEnabled(enabled)
    }
    
    suspend fun setOfflineMode(enabled: Boolean) {
        sessionManager.setOfflineMode(enabled)
    }
    
    fun setVolumeSliderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setVolumeSliderEnabled(enabled)
            _uiState.update { it.copy(volumeSliderEnabled = enabled) }
        }
    }
    
    private var downloadJob: Job? = null
    private var downloadedApkFile: File? = null
    
    init {
        loadSettings()
        
        viewModelScope.launch {
            sessionManager.audioQualityFlow.collect { quality ->
                _uiState.update { it.copy(audioQuality = quality) }
            }
        }
        
        viewModelScope.launch {
            sessionManager.appThemeFlow.collect { theme ->
                _uiState.update { it.copy(appTheme = theme) }
            }
        }
        
        viewModelScope.launch {
            sessionManager.doubleTapSeekSecondsFlow.collect { seconds ->
                _uiState.update { it.copy(doubleTapSeekSeconds = seconds) }
            }
        }
    }
    
    private fun loadSettings() {
        _uiState.update { 
            it.copy(
                isLoggedIn = sessionManager.isLoggedIn(),
                userAvatarUrl = sessionManager.getUserAvatar(),
                audioQuality = sessionManager.getAudioQuality(),
                downloadQuality = sessionManager.getDownloadQuality(),
                themeMode = sessionManager.getThemeMode(),
                appTheme = sessionManager.getAppTheme(),
                dynamicColorEnabled = sessionManager.isDynamicColorEnabled(),
                gaplessPlaybackEnabled = sessionManager.isGaplessPlaybackEnabled(),
                automixEnabled = sessionManager.isAutomixEnabled(),
                volumeSliderEnabled = sessionManager.isVolumeSliderEnabled(),
                musicSource = sessionManager.getMusicSource(),
                currentVersion = updateRepository.getCurrentVersionName(),
                doubleTapSeekSeconds = sessionManager.getDoubleTapSeekSeconds(),
                volumeNormalizationEnabled = sessionManager.isVolumeNormalizationEnabled()
            )
        }
    }
    
    /**
     * Check for updates from GitHub Releases.
     */
    fun checkForUpdates() {
        viewModelScope.launch {
            _uiState.update { it.copy(updateState = UpdateState.Checking) }
            
            updateRepository.checkForUpdate()
                .onSuccess { update ->
                    if (update != null) {
                        _uiState.update { it.copy(updateState = UpdateState.UpdateAvailable(update)) }
                    } else {
                        _uiState.update { it.copy(updateState = UpdateState.NoUpdate) }
                    }
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(updateState = UpdateState.Error(error.message ?: "Unknown error"))
                    }
                }
        }
    }
    
    /**
     * Download the update APK.
     */
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
                // Auto-trigger install
                installUpdate()
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(updateState = UpdateState.Error(error.message ?: "Download failed"))
                }
            }
        }
    }
    
    /**
     * Install the downloaded APK.
     */
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
    
    /**
     * Cancel ongoing download.
     */
    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _uiState.update { it.copy(updateState = UpdateState.Idle) }
    }
    
    /**
     * Reset update state to idle.
     */
    fun resetUpdateState() {
        _uiState.update { it.copy(updateState = UpdateState.Idle) }
    }
    
    fun setAudioQuality(quality: AudioQuality) {
        viewModelScope.launch {
            sessionManager.setAudioQuality(quality)
            _uiState.update { it.copy(audioQuality = quality) }
        }
    }
    
    fun setDownloadQuality(quality: DownloadQuality) {
        viewModelScope.launch {
            sessionManager.setDownloadQuality(quality)
            _uiState.update { it.copy(downloadQuality = quality) }
        }
    }
    
    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setDynamicColor(enabled)
            _uiState.update { it.copy(dynamicColorEnabled = enabled) }
        }
    }
    
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            sessionManager.setThemeMode(mode)
            _uiState.update { it.copy(themeMode = mode) }
        }
    }
    
    fun setAppTheme(theme: AppTheme) {
        viewModelScope.launch {
            sessionManager.setAppTheme(theme)
            _uiState.update { it.copy(appTheme = theme) }
        }
    }
    
    fun setGaplessPlayback(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setGaplessPlayback(enabled)
            _uiState.update { it.copy(gaplessPlaybackEnabled = enabled) }
        }
    }
    
    fun setAutomix(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setAutomix(enabled)
            _uiState.update { it.copy(automixEnabled = enabled) }
        }
    }
    
    fun setMusicSource(source: MusicSource) {
        viewModelScope.launch {
            sessionManager.setMusicSource(source)
            _uiState.update { it.copy(musicSource = source) }
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            sessionManager.clearCookies()
            _uiState.update { 
                it.copy(
                    isLoggedIn = false,
                    userAvatarUrl = null
                )
            }
        }
    }
    
    fun setDoubleTapSeekSeconds(seconds: Int) {
        viewModelScope.launch {
            sessionManager.setDoubleTapSeekSeconds(seconds)
            _uiState.update { it.copy(doubleTapSeekSeconds = seconds) }
        }
    }

    fun setVolumeNormalizationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setVolumeNormalizationEnabled(enabled)
            _uiState.update { it.copy(volumeNormalizationEnabled = enabled) }
        }
    }
}
