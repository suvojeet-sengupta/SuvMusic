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
import com.suvojeet.suvmusic.data.model.HapticsIntensity
import com.suvojeet.suvmusic.data.model.HapticsMode
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
    val storedAccounts: List<SessionManager.StoredAccount> = emptyList(),
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
    val volumeNormalizationEnabled: Boolean = false,
    val betterLyricsEnabled: Boolean = true,
    val simpMusicEnabled: Boolean = true,
    val playerCacheLimit: Long = -1L, // Default Unlimited
    val playerCacheAutoClearInterval: Int = 5, // Default 5 days
    // Music Haptics
    val musicHapticsEnabled: Boolean = false,
    val hapticsMode: HapticsMode = HapticsMode.BASIC,
    val hapticsIntensity: HapticsIntensity = HapticsIntensity.MEDIUM
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val updateRepository: com.suvojeet.suvmusic.data.repository.YouTubeRepository, // Use YouTubeRepository for account info
    private val updateRepo: UpdateRepository, // Renamed to avoid conflict
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
    val volumeSliderEnabledFlow = sessionManager.volumeSliderEnabledFlow // Renamed to avoid name clash
    
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

        viewModelScope.launch {
            sessionManager.enableBetterLyricsFlow.collect { enabled ->
                _uiState.update { it.copy(betterLyricsEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            sessionManager.enableSimpMusicFlow.collect { enabled ->
                _uiState.update { it.copy(simpMusicEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            sessionManager.playerCacheLimitFlow.collect { limit ->
                _uiState.update { it.copy(playerCacheLimit = limit) }
            }
        }

        viewModelScope.launch {
            sessionManager.playerCacheAutoClearIntervalFlow.collect { interval ->
                _uiState.update { it.copy(playerCacheAutoClearInterval = interval) }
            }
        }

        viewModelScope.launch {
            sessionManager.musicHapticsEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(musicHapticsEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            sessionManager.hapticsModeFlow.collect { mode ->
                _uiState.update { it.copy(hapticsMode = mode) }
            }
        }

        viewModelScope.launch {
            sessionManager.hapticsIntensityFlow.collect { intensity ->
                _uiState.update { it.copy(hapticsIntensity = intensity) }
            }
        }
        
        // Refresh account info if logged in
        viewModelScope.launch {
            if (sessionManager.isLoggedIn()) {
                fetchAndSaveAccountInfo()
            }
        }
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            val isLoggedIn = sessionManager.isLoggedIn()
            val userAvatar = sessionManager.getUserAvatar()
            val storedAccounts = sessionManager.getStoredAccounts()
            val audioQuality = sessionManager.getAudioQuality()
            val downloadQuality = sessionManager.getDownloadQuality()
            val themeMode = sessionManager.getThemeMode()
            val appTheme = sessionManager.getAppTheme()
            val dynamicColorEnabled = sessionManager.isDynamicColorEnabled()
            val gaplessPlaybackEnabled = sessionManager.isGaplessPlaybackEnabled()
            val automixEnabled = sessionManager.isAutomixEnabled()
            val volumeSliderEnabled = sessionManager.isVolumeSliderEnabled()
            val musicSource = sessionManager.getMusicSource()
            val doubleTapSeekSeconds = sessionManager.getDoubleTapSeekSeconds()
            val volumeNormalizationEnabled = sessionManager.isVolumeNormalizationEnabled()
            val betterLyricsEnabled = sessionManager.doesEnableBetterLyrics()
            val simpMusicEnabled = sessionManager.doesEnableSimpMusic()
            val playerCacheLimit = sessionManager.getPlayerCacheLimit()
            val playerCacheAutoClearInterval = sessionManager.getPlayerCacheAutoClearInterval()
            val musicHapticsEnabled = sessionManager.isMusicHapticsEnabled()
            val hapticsMode = sessionManager.getHapticsMode()
            val hapticsIntensity = sessionManager.getHapticsIntensity()

            _uiState.update { 
                it.copy(
                    isLoggedIn = isLoggedIn,
                    userAvatarUrl = userAvatar,
                    storedAccounts = storedAccounts,
                    audioQuality = audioQuality,
                    downloadQuality = downloadQuality,
                    themeMode = themeMode,
                    appTheme = appTheme,
                    dynamicColorEnabled = dynamicColorEnabled,
                    gaplessPlaybackEnabled = gaplessPlaybackEnabled,
                    automixEnabled = automixEnabled,
                    volumeSliderEnabled = volumeSliderEnabled,
                    musicSource = musicSource,
                    currentVersion = updateRepo.getCurrentVersionName(),
                    doubleTapSeekSeconds = doubleTapSeekSeconds,
                    volumeNormalizationEnabled = volumeNormalizationEnabled,
                    betterLyricsEnabled = betterLyricsEnabled,
                    simpMusicEnabled = simpMusicEnabled,
                    playerCacheLimit = playerCacheLimit,
                    playerCacheAutoClearInterval = playerCacheAutoClearInterval,
                    // Music Haptics
                    musicHapticsEnabled = musicHapticsEnabled,
                    hapticsMode = hapticsMode,
                    hapticsIntensity = hapticsIntensity
                )
            }
        }
    }
    
    /**
     * Fetch account info (name, email) and save to history.
     */
    fun fetchAndSaveAccountInfo() {
        viewModelScope.launch {
            val account = updateRepository.fetchAccountInfo()
            if (account != null) {
                sessionManager.saveCurrentAccountToHistory(account.name, account.email, account.avatarUrl)
                _uiState.update { 
                    it.copy(
                        userAvatarUrl = account.avatarUrl,
                        storedAccounts = sessionManager.getStoredAccounts()
                    ) 
                }
            }
        }
    }
    
    /**
     * Switch to a saved account.
     */
    fun switchAccount(account: SessionManager.StoredAccount) {
        viewModelScope.launch {
            // Save current before switching (if we have info)
            // Ideally we should have info if fetchAndSaveAccountInfo ran
            
            sessionManager.switchAccount(account)
            
            _uiState.update { 
                it.copy(
                    isLoggedIn = true,
                    userAvatarUrl = account.avatarUrl,
                    storedAccounts = sessionManager.getStoredAccounts()
                )
            }
            
            // Refresh info to be sure (cookies might be old?)
            fetchAndSaveAccountInfo()
        }
    }
    
    /**
     * Prepare for adding a new account (logout current, save it).
     */
    fun prepareAddAccount() {
        viewModelScope.launch {
            // Current is already saved via fetchAndSaveAccountInfo if it was successful.
            // But let's try to ensure it's saved if possible, though we might not have name/email if offline.
            // Assuming it was saved when loaded.
            
            sessionManager.clearCookies()
            _uiState.update { 
                it.copy(
                    isLoggedIn = false,
                    userAvatarUrl = null
                )
            }
        }
    }
    
    fun removeAccount(email: String) {
        viewModelScope.launch {
            sessionManager.removeAccount(email)
            _uiState.update { 
                it.copy(storedAccounts = sessionManager.getStoredAccounts())
            }
        }
    }

    /**
     * Check for updates from GitHub Releases.
     */
    fun checkForUpdates() {
        viewModelScope.launch {
            _uiState.update { it.copy(updateState = UpdateState.Checking) }
            
            updateRepo.checkForUpdate()
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
            
            updateRepo.downloadApk(
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

    fun setBetterLyricsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setEnableBetterLyrics(enabled)
            _uiState.update { it.copy(betterLyricsEnabled = enabled) }
        }
    }

    fun setSimpMusicEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setEnableSimpMusic(enabled)
            _uiState.update { it.copy(simpMusicEnabled = enabled) }
        }
    }


    fun setPlayerCacheLimit(limit: Long) {
        viewModelScope.launch {
            sessionManager.setPlayerCacheLimit(limit)
            _uiState.update { it.copy(playerCacheLimit = limit) }
        }
    }

    fun setPlayerCacheAutoClearInterval(days: Int) {
        viewModelScope.launch {
            sessionManager.setPlayerCacheAutoClearInterval(days)
            _uiState.update { it.copy(playerCacheAutoClearInterval = days) }
        }
    }

    // --- Music Haptics ---

    fun setMusicHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setMusicHapticsEnabled(enabled)
            _uiState.update { it.copy(musicHapticsEnabled = enabled) }
        }
    }

    fun setHapticsMode(mode: HapticsMode) {
        viewModelScope.launch {
            sessionManager.setHapticsMode(mode)
            _uiState.update { it.copy(hapticsMode = mode) }
        }
    }

    fun setHapticsIntensity(intensity: HapticsIntensity) {
        viewModelScope.launch {
            sessionManager.setHapticsIntensity(intensity)
            _uiState.update { it.copy(hapticsIntensity = intensity) }
        }
    }
}
