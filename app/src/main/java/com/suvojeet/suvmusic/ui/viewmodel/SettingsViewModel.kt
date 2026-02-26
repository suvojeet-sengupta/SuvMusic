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
import com.suvojeet.suvmusic.data.model.VideoQuality
import com.suvojeet.suvmusic.data.model.DownloadQuality
import com.suvojeet.suvmusic.data.model.HapticsIntensity
import com.suvojeet.suvmusic.data.model.HapticsMode
import com.suvojeet.suvmusic.providers.lyrics.LyricsTextPosition
import com.suvojeet.suvmusic.providers.lyrics.LyricsAnimationType
import com.suvojeet.suvmusic.data.model.ThemeMode
import com.suvojeet.suvmusic.data.model.UpdateState
import com.suvojeet.suvmusic.data.repository.UpdateRepository
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import com.suvojeet.suvmusic.lastfm.LastFmRepository
import com.suvojeet.suvmusic.data.MusicSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val isLoggedIn: Boolean = false,
    val userAvatarUrl: String? = null,
    val storedAccounts: List<SessionManager.StoredAccount> = emptyList(),
    val availableAccounts: List<SessionManager.StoredAccount> = emptyList(),
    val audioQuality: AudioQuality = AudioQuality.HIGH,
    val videoQuality: VideoQuality = VideoQuality.MEDIUM,
    val downloadQuality: DownloadQuality = DownloadQuality.HIGH,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appTheme: AppTheme = AppTheme.DEFAULT,
    val dynamicColorEnabled: Boolean = true,
    val gaplessPlaybackEnabled: Boolean = false,
    val automixEnabled: Boolean = true,
    val volumeSliderEnabled: Boolean = true,
    val musicSource: MusicSource = MusicSource.YOUTUBE,
    val updateState: UpdateState = UpdateState.Idle,
    val currentVersion: String = "",
    val doubleTapSeekSeconds: Int = 10,
    val volumeNormalizationEnabled: Boolean = true,
    val betterLyricsEnabled: Boolean = true,
    val simpMusicEnabled: Boolean = true,
    val kuGouEnabled: Boolean = true,
    val playerCacheLimit: Long = -1L, // Default Unlimited
    val playerCacheAutoClearInterval: Int = 5, // Default 5 days
    // Music Haptics
    val musicHapticsEnabled: Boolean = false,
    val hapticsMode: HapticsMode = HapticsMode.BASIC,
    val hapticsIntensity: HapticsIntensity = HapticsIntensity.MEDIUM,
    // Misc
    val stopMusicOnTaskClear: Boolean = false,
    val pauseMusicOnMediaMuted: Boolean = false,
    val keepScreenOn: Boolean = false,
    // Appearance
    val pureBlackEnabled: Boolean = false,
    val playerAnimatedBackgroundEnabled: Boolean = true,
    // Lyrics
    val preferredLyricsProvider: String = "BetterLyrics",
    val lyricsTextPosition: LyricsTextPosition = LyricsTextPosition.CENTER,
    val lyricsAnimationType: LyricsAnimationType = LyricsAnimationType.WORD,
    val lyricsLineSpacing: Float = 1.5f,
    val lyricsFontSize: Float = 26f,
    // Audio Offload
    val audioOffloadEnabled: Boolean = false,
    // Volume Boost
    val volumeBoostEnabled: Boolean = false,
    val volumeBoostAmount: Int = 0,
    // SponsorBlock
    val sponsorBlockEnabled: Boolean = true,
    // Last.fm
    val lastFmUsername: String? = null,
    val lastFmScrobblingEnabled: Boolean = false,
    val lastFmRecommendationsEnabled: Boolean = true,
    val lastFmUseNowPlaying: Boolean = true,
    val lastFmSendLikes: Boolean = false,
    val scrobbleDelayPercent: Float = 0.5f,
    val scrobbleMinDuration: Int = 30, // seconds
    val scrobbleDelaySeconds: Int = 180, // seconds
    // Updates
    val updateChannel: com.suvojeet.suvmusic.data.model.UpdateChannel = com.suvojeet.suvmusic.data.model.UpdateChannel.STABLE,
    // Content Preferences
    val preferredLanguages: Set<String> = emptySet(),
    val youtubeHistorySyncEnabled: Boolean = false,
    val ignoreAudioFocusDuringCalls: Boolean = false,
    // Bluetooth
    val bluetoothAutoplayEnabled: Boolean = false,
    val speakSongDetailsEnabled: Boolean = false,
    // Discord RPC
    val discordRpcEnabled: Boolean = false,
    val discordToken: String = "", // Empty means not set
    val discordUseDetails: Boolean = false,
    val privacyModeEnabled: Boolean = false,
    val audioArEnabled: Boolean = false,
    val audioArSensitivity: Float = 1.0f,
    val audioArAutoCalibrate: Boolean = true,
    // Preloading
    val nextSongPreloadingEnabled: Boolean = true,
    val nextSongPreloadDelay: Int = 3, // seconds
    // Crossfeed
    val crossfeedEnabled: Boolean = true,
    // Equalizer
    val eqEnabled: Boolean = false,
    val eqBands: FloatArray = FloatArray(10) { 0f },
    val forceMaxRefreshRateEnabled: Boolean = true,
    val navBarAlpha: Float = 0.8f
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val youtubeRepository: YouTubeRepository,
    private val updateRepo: UpdateRepository,
    private val lastFmRepository: LastFmRepository,
    private val audioARManager: com.suvojeet.suvmusic.player.AudioARManager,
    @param:ApplicationContext private val context: Context
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

    // SponsorBlock
    val sponsorBlockEnabled = sessionManager.sponsorBlockEnabledFlow
    val sponsorBlockCategories = sessionManager.sponsorBlockCategoriesFlow

    // Last.fm
    val lastFmUsername = sessionManager.lastFmUsernameFlow
    
    // Discord RPC
    val discordRpcEnabled = sessionManager.discordRpcEnabledFlow

    // Privacy Mode
    val privacyModeEnabled = sessionManager.privacyModeEnabledFlow

    // Audio AR
    val audioArEnabled = sessionManager.audioArEnabledFlow
    
    // Lyrics Settings Flows
    val lyricsLineSpacing = sessionManager.lyricsLineSpacingFlow
    val lyricsFontSize = sessionManager.lyricsFontSizeFlow

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
            sessionManager.videoQualityFlow.collect { quality ->
                _uiState.update { it.copy(videoQuality = quality) }
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
            sessionManager.enableKuGouFlow.collect { enabled ->
                _uiState.update { it.copy(kuGouEnabled = enabled) }
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

        viewModelScope.launch {
            sessionManager.stopMusicOnTaskClearEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(stopMusicOnTaskClear = enabled) }
            }
        }

        viewModelScope.launch {
            sessionManager.pauseMusicOnMediaMutedEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(pauseMusicOnMediaMuted = enabled) }
            }
        }

        viewModelScope.launch {
            sessionManager.keepScreenOnEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(keepScreenOn = enabled) }
            }
        }

        viewModelScope.launch {
            sessionManager.pureBlackEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(pureBlackEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            sessionManager.playerAnimatedBackgroundFlow.collect { enabled ->
                _uiState.update { it.copy(playerAnimatedBackgroundEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            sessionManager.preferredLyricsProviderFlow.collect { provider ->
                _uiState.update { it.copy(preferredLyricsProvider = provider) }
            }
        }

        viewModelScope.launch {
            sessionManager.lyricsTextPositionFlow.collect { position ->
                _uiState.update { it.copy(lyricsTextPosition = position) }
            }
        }

        viewModelScope.launch {
            sessionManager.lyricsAnimationTypeFlow.collect { type ->
                _uiState.update { it.copy(lyricsAnimationType = type) }
            }
        }
        
        viewModelScope.launch {
            sessionManager.lyricsLineSpacingFlow.collect { spacing ->
                _uiState.update { it.copy(lyricsLineSpacing = spacing) }
            }
        }
        
        viewModelScope.launch {
            sessionManager.lyricsFontSizeFlow.collect { size ->
                _uiState.update { it.copy(lyricsFontSize = size) }
            }
        }

        viewModelScope.launch {
            sessionManager.audioOffloadEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(audioOffloadEnabled = enabled) }
            }
        }
        
        viewModelScope.launch {
            sessionManager.volumeBoostEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(volumeBoostEnabled = enabled) }
            }
        }
        
        viewModelScope.launch {
            sessionManager.volumeBoostAmountFlow.collect { amount ->
                _uiState.update { it.copy(volumeBoostAmount = amount) }
            }
        }

        viewModelScope.launch {
            sessionManager.sponsorBlockEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(sponsorBlockEnabled = enabled) }
            }
        }
        
        viewModelScope.launch {
            sessionManager.lastFmUsernameFlow.collect { username ->
                _uiState.update { it.copy(lastFmUsername = username) }
            }
        }
        
        viewModelScope.launch {
            sessionManager.preferredLanguagesFlow.collect { languages ->
                _uiState.update { it.copy(preferredLanguages = languages) }
            }
        }

        viewModelScope.launch {
            sessionManager.youtubeHistorySyncEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(youtubeHistorySyncEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            sessionManager.ignoreAudioFocusDuringCallsFlow.collect { enabled ->
                _uiState.update { it.copy(ignoreAudioFocusDuringCalls = enabled) }
            }
        }

        viewModelScope.launch {
            sessionManager.bluetoothAutoplayEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(bluetoothAutoplayEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            sessionManager.speakSongDetailsEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(speakSongDetailsEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            sessionManager.discordRpcEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(discordRpcEnabled = enabled) }
            }
        }
        
        viewModelScope.launch {
            sessionManager.discordTokenFlow.collect { token ->
                _uiState.update { it.copy(discordToken = token) }
            }
        }
        
        viewModelScope.launch {
            sessionManager.discordUseDetailsFlow.collect { enabled ->
                _uiState.update { it.copy(discordUseDetails = enabled) }
            }
        }

        viewModelScope.launch {
            sessionManager.privacyModeEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(privacyModeEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            sessionManager.audioArEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(audioArEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            sessionManager.audioArSensitivityFlow.collect { value ->
                _uiState.update { it.copy(audioArSensitivity = value) }
            }
        }

        viewModelScope.launch {
            sessionManager.audioArAutoCalibrateFlow.collect { enabled ->
                _uiState.update { it.copy(audioArAutoCalibrate = enabled) }
            }
        }

        viewModelScope.launch {
            sessionManager.nextSongPreloadingEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(nextSongPreloadingEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            sessionManager.nextSongPreloadDelayFlow.collect { delay ->
                _uiState.update { it.copy(nextSongPreloadDelay = delay) }
            }
        }

        viewModelScope.launch {
            sessionManager.crossfeedEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(crossfeedEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            sessionManager.eqEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(eqEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            sessionManager.eqBandsFlow.collect { bands ->
                _uiState.update { it.copy(eqBands = bands) }
            }
        }

        viewModelScope.launch {
            sessionManager.forceMaxRefreshRateFlow.collect { enabled ->
                _uiState.update { it.copy(forceMaxRefreshRateEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            sessionManager.navBarAlphaFlow.collect { alpha ->
                _uiState.update { it.copy(navBarAlpha = alpha) }
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
            val videoQuality = sessionManager.getVideoQuality()
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
            val kuGouEnabled = sessionManager.doesEnableKuGou()
            val playerCacheLimit = sessionManager.getPlayerCacheLimit()
            val playerCacheAutoClearInterval = sessionManager.getPlayerCacheAutoClearInterval()
            val musicHapticsEnabled = sessionManager.isMusicHapticsEnabled()
            val hapticsMode = sessionManager.getHapticsMode()
            val hapticsIntensity = sessionManager.getHapticsIntensity()
            val stopMusicOnTaskClear = sessionManager.isStopMusicOnTaskClearEnabled()
            val pauseMusicOnMediaMuted = sessionManager.isPauseMusicOnMediaMutedEnabled()
            val keepScreenOn = sessionManager.isKeepScreenOnEnabled()
            val pureBlackEnabled = sessionManager.isPureBlackEnabled()
            val playerAnimatedBackgroundEnabled = sessionManager.isPlayerAnimatedBackgroundEnabled()
            val sponsorBlockEnabled = sessionManager.isSponsorBlockEnabled()
            val lastFmUsername = sessionManager.getLastFmUsername()
            val lastFmScrobblingEnabled = sessionManager.isLastFmScrobblingEnabled()
            val lastFmRecommendationsEnabled = sessionManager.isLastFmRecommendationsEnabled()
            val lastFmUseNowPlaying = sessionManager.isLastFmUseNowPlayingEnabled()
            val lastFmSendLikes = sessionManager.isLastFmSendLikesEnabled()
            val scrobbleDelayPercent = sessionManager.getScrobbleDelayPercent()
            val scrobbleMinDuration = sessionManager.getScrobbleMinDuration()
            val scrobbleDelaySeconds = sessionManager.getScrobbleDelaySeconds()
            val preferredLanguages = sessionManager.getPreferredLanguages()
            val ignoreAudioFocusDuringCalls = sessionManager.isIgnoreAudioFocusDuringCallsEnabled()
            val bluetoothAutoplayEnabled = sessionManager.isBluetoothAutoplayEnabled()
            val speakSongDetailsEnabled = sessionManager.isSpeakSongDetailsEnabled()
            val discordToken = sessionManager.getDiscordToken()
            val discordUseDetails = sessionManager.isDiscordUseDetailsEnabled()
            val lyricsLineSpacing = sessionManager.getLyricsLineSpacing()
            val lyricsFontSize = sessionManager.getLyricsFontSize()
            val preferredLyricsProvider = sessionManager.getPreferredLyricsProvider()
            val lyricsTextPosition = sessionManager.getLyricsTextPosition()
            val lyricsAnimationType = sessionManager.getLyricsAnimationType()
            val audioOffloadEnabled = sessionManager.isAudioOffloadEnabled()
            val volumeBoostEnabled = sessionManager.isVolumeBoostEnabled()
            val volumeBoostAmount = sessionManager.getVolumeBoostAmount()
            val updateChannel = sessionManager.getUpdateChannel()
            val youtubeHistorySyncEnabled = sessionManager.isYouTubeHistorySyncEnabled()
            val discordRpcEnabled = sessionManager.isDiscordRpcEnabled()
            val privacyModeEnabled = sessionManager.isPrivacyModeEnabled()
            val audioArEnabled = sessionManager.isAudioArEnabled()
            val nextSongPreloadingEnabled = sessionManager.isNextSongPreloadingEnabled()
            val nextSongPreloadDelay = sessionManager.getNextSongPreloadDelay()
            val crossfeedEnabled = sessionManager.isCrossfeedEnabled()
            val eqEnabled = sessionManager.isEqEnabled()
            val eqBands = sessionManager.getEqBands()
            val forceMaxRefreshRate = sessionManager.forceMaxRefreshRateFlow.first()
            val navBarAlpha = sessionManager.getNavBarAlpha()


            _uiState.update { 
                it.copy(
                    isLoggedIn = isLoggedIn,
                    userAvatarUrl = userAvatar,
                    storedAccounts = storedAccounts,
                    audioQuality = audioQuality,
                    videoQuality = videoQuality,
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
                    kuGouEnabled = kuGouEnabled,
                    playerCacheLimit = playerCacheLimit,
                    playerCacheAutoClearInterval = playerCacheAutoClearInterval,
                    // Music Haptics
                    musicHapticsEnabled = musicHapticsEnabled,
                    hapticsMode = hapticsMode,
                    hapticsIntensity = hapticsIntensity,
                    stopMusicOnTaskClear = stopMusicOnTaskClear,
                    pauseMusicOnMediaMuted = pauseMusicOnMediaMuted,
                    keepScreenOn = keepScreenOn,
                    pureBlackEnabled = pureBlackEnabled,
                    playerAnimatedBackgroundEnabled = playerAnimatedBackgroundEnabled,
                    preferredLyricsProvider = preferredLyricsProvider,
                    lyricsTextPosition = lyricsTextPosition,
                    lyricsAnimationType = lyricsAnimationType,
                    lyricsLineSpacing = lyricsLineSpacing,
                    lyricsFontSize = lyricsFontSize,
                    audioOffloadEnabled = audioOffloadEnabled,
                    volumeBoostEnabled = volumeBoostEnabled,
                    volumeBoostAmount = volumeBoostAmount,
                    sponsorBlockEnabled = sponsorBlockEnabled,
                    lastFmUsername = lastFmUsername,
                    lastFmScrobblingEnabled = lastFmScrobblingEnabled,
                    lastFmRecommendationsEnabled = lastFmRecommendationsEnabled,
                    lastFmUseNowPlaying = lastFmUseNowPlaying,
                    lastFmSendLikes = lastFmSendLikes,
                    scrobbleDelayPercent = scrobbleDelayPercent,
                    scrobbleMinDuration = scrobbleMinDuration,
                    scrobbleDelaySeconds = scrobbleDelaySeconds,

                    updateChannel = updateChannel,
                    preferredLanguages = preferredLanguages,
                    youtubeHistorySyncEnabled = youtubeHistorySyncEnabled,
                    ignoreAudioFocusDuringCalls = ignoreAudioFocusDuringCalls,
                    bluetoothAutoplayEnabled = bluetoothAutoplayEnabled,
                    speakSongDetailsEnabled = speakSongDetailsEnabled,
                    discordRpcEnabled = discordRpcEnabled,
                    discordToken = discordToken,
                    discordUseDetails = discordUseDetails,
                    privacyModeEnabled = privacyModeEnabled,
                    audioArEnabled = audioArEnabled,
                    audioArSensitivity = sessionManager.getAudioArSensitivity(),
                    audioArAutoCalibrate = sessionManager.isAudioArAutoCalibrateEnabled(),
                    nextSongPreloadingEnabled = nextSongPreloadingEnabled,
                    nextSongPreloadDelay = nextSongPreloadDelay,
                    crossfeedEnabled = crossfeedEnabled,
                    eqEnabled = eqEnabled,
                    eqBands = eqBands,
                    forceMaxRefreshRateEnabled = forceMaxRefreshRate,
                    navBarAlpha = navBarAlpha
                )
            }
        }
    }

    fun setCrossfeedEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setCrossfeedEnabled(enabled)
            _uiState.update { it.copy(crossfeedEnabled = enabled) }
        }
    }

    fun setEqEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setEqEnabled(enabled)
            _uiState.update { it.copy(eqEnabled = enabled) }
        }
    }

    fun setEqBandGain(index: Int, gain: Float) {
        viewModelScope.launch {
            sessionManager.setEqBand(index, gain)
            val currentBands = _uiState.value.eqBands.copyOf()
            if (index in currentBands.indices) {
                currentBands[index] = gain
                _uiState.update { it.copy(eqBands = currentBands) }
            }
        }
    }

    fun resetEqBands() {
        viewModelScope.launch {
            sessionManager.resetEqBands()
            _uiState.update { it.copy(eqBands = FloatArray(10) { 0f }) }
        }
    }

    fun getLastFmAuthUrl(): String {
        return lastFmRepository.getAuthUrl()
    }

    fun disconnectLastFm() {
        viewModelScope.launch {
            sessionManager.setLastFmSession("", "")
            _uiState.update { it.copy(lastFmUsername = null) }
        }
    }

    fun processLastFmToken(token: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = lastFmRepository.fetchSession(token)
            result.onSuccess { auth ->
                val username = auth.session.name
                val sessionKey = auth.session.key
                sessionManager.setLastFmSession(sessionKey, username)
                _uiState.update { it.copy(lastFmUsername = username) }
                onSuccess(username)
            }.onFailure { error ->
                onError("Failed: ${error.message ?: "Unknown Error"}")
            }
        }
    }

    fun loginLastFmMobile(username: String, password: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            lastFmRepository.getMobileSession(username, password)
                .onSuccess { auth ->
                    sessionManager.setLastFmSession(auth.session.key, auth.session.name)
                    _uiState.update { it.copy(lastFmUsername = auth.session.name) }
                    onSuccess(auth.session.name)
                }
                .onFailure { error ->
                    onError(error.message ?: "Login failed")
                }
        }
    }

    fun setLastFmScrobblingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setLastFmScrobblingEnabled(enabled)
            _uiState.update { it.copy(lastFmScrobblingEnabled = enabled) }
        }
    }

    fun setLastFmRecommendationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setLastFmRecommendationsEnabled(enabled)
            _uiState.update { it.copy(lastFmRecommendationsEnabled = enabled) }
        }
    }

    fun setLastFmUseNowPlaying(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setLastFmUseNowPlaying(enabled)
            _uiState.update { it.copy(lastFmUseNowPlaying = enabled) }
        }
    }

    fun setLastFmSendLikes(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setLastFmSendLikes(enabled)
            _uiState.update { it.copy(lastFmSendLikes = enabled) }
        }
    }

    fun setScrobbleDelayPercent(percent: Float) {
        viewModelScope.launch {
            sessionManager.setScrobbleDelayPercent(percent)
            _uiState.update { it.copy(scrobbleDelayPercent = percent) }
        }
    }

    fun setScrobbleMinDuration(seconds: Int) {
        viewModelScope.launch {
            sessionManager.setScrobbleMinDuration(seconds)
            _uiState.update { it.copy(scrobbleMinDuration = seconds) }
        }
    }

    fun setScrobbleDelaySeconds(seconds: Int) {
        viewModelScope.launch {
            sessionManager.setScrobbleDelaySeconds(seconds)
            _uiState.update { it.copy(scrobbleDelaySeconds = seconds) }
        }
    }

    fun setYouTubeHistorySyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setYouTubeHistorySyncEnabled(enabled)
            _uiState.update { it.copy(youtubeHistorySyncEnabled = enabled) }
        }
    }

    fun setIgnoreAudioFocusDuringCalls(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setIgnoreAudioFocusDuringCallsEnabled(enabled)
            _uiState.update { it.copy(ignoreAudioFocusDuringCalls = enabled) }
        }
    }

    fun setBluetoothAutoplayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setBluetoothAutoplayEnabled(enabled)
            _uiState.update { it.copy(bluetoothAutoplayEnabled = enabled) }
        }
    }

    fun setSpeakSongDetailsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setSpeakSongDetailsEnabled(enabled)
            _uiState.update { it.copy(speakSongDetailsEnabled = enabled) }
        }
    }
    
    fun setDiscordRpcEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setDiscordRpcEnabled(enabled)
            _uiState.update { it.copy(discordRpcEnabled = enabled) }
        }
    }
    
    fun setDiscordToken(token: String) {
        viewModelScope.launch {
            sessionManager.setDiscordToken(token)
            _uiState.update { it.copy(discordToken = token) }
        }
    }
    
    fun setDiscordUseDetails(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setDiscordUseDetails(enabled)
            _uiState.update { it.copy(discordUseDetails = enabled) }
        }
    }

    fun setPrivacyModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setPrivacyModeEnabled(enabled)
            _uiState.update { it.copy(privacyModeEnabled = enabled) }
        }
    }

    fun setAudioArEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setAudioArEnabled(enabled)
            _uiState.update { it.copy(audioArEnabled = enabled) }
        }
    }

    fun setAudioArSensitivity(sensitivity: Float) {
        viewModelScope.launch {
            sessionManager.setAudioArSensitivity(sensitivity)
            _uiState.update { it.copy(audioArSensitivity = sensitivity) }
        }
    }

    fun setAudioArAutoCalibrate(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setAudioArAutoCalibrate(enabled)
            _uiState.update { it.copy(audioArAutoCalibrate = enabled) }
        }
    }

    fun setNextSongPreloadingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setNextSongPreloadingEnabled(enabled)
            _uiState.update { it.copy(nextSongPreloadingEnabled = enabled) }
        }
    }
    
    fun setNextSongPreloadDelay(seconds: Int) {
        viewModelScope.launch {
            sessionManager.setNextSongPreloadDelay(seconds)
            _uiState.update { it.copy(nextSongPreloadDelay = seconds) }
        }
    }

    fun setLyricsLineSpacing(multiplier: Float) {
        viewModelScope.launch {
            sessionManager.setLyricsLineSpacing(multiplier)
            _uiState.update { it.copy(lyricsLineSpacing = multiplier) }
        }
    }

    fun setLyricsFontSize(size: Float) {
        viewModelScope.launch {
            sessionManager.setLyricsFontSize(size)
            _uiState.update { it.copy(lyricsFontSize = size) }
        }
    }
    
    fun calibrateAudioAr() {
        audioARManager.calibrate()
    }

    /**
     * Fetch account info (name, email) and save to history.
     */
    fun fetchAndSaveAccountInfo() {
        viewModelScope.launch {
            val account = youtubeRepository.fetchAccountInfo()
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
     * Fetch available brand accounts.
     */
    fun fetchAvailableAccounts() {
        viewModelScope.launch {
            val accounts = youtubeRepository.getAvailableAccounts()
            _uiState.update { it.copy(availableAccounts = accounts) }
        }
    }

    /**
     * Switch to a saved account.
     */
    fun switchAccount(account: SessionManager.StoredAccount) {
        viewModelScope.launch {
            youtubeRepository.switchAccount(account)
            _uiState.update { 
                it.copy(
                    isLoggedIn = true,
                    userAvatarUrl = account.avatarUrl,
                    storedAccounts = sessionManager.getStoredAccounts()
                )
            }
            fetchAndSaveAccountInfo()
            // Clear WebView cookies to force fresh login if needed or just to be safe
            // clearWebViewCookies() 
        }
    }
    
    /**
     * Prepare for adding a new account (logout current, save it).
     */
    fun prepareAddAccount() {
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

    /**
     * Clear only WebView cookies to allow signing into a new account.
     * Does NOT clear the current app session, so previous account remains active if login is cancelled.
     */
    fun clearWebViewCookies() {
        viewModelScope.launch {
            android.webkit.CookieManager.getInstance().removeAllCookies(null)
            android.webkit.CookieManager.getInstance().flush()
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
            
            val channel = sessionManager.getUpdateChannel()
            
            updateRepo.checkForUpdate(channel)
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
    
    fun setUpdateChannel(channel: com.suvojeet.suvmusic.data.model.UpdateChannel) {
        viewModelScope.launch {
            sessionManager.setUpdateChannel(channel)
            _uiState.update { it.copy(updateChannel = channel) }
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
        if (!apkFile.exists()) {
             _uiState.update { it.copy(updateState = UpdateState.Error("APK file not found")) }
             return
        }
        
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        apkFile
                    )
                } else {
                    Uri.fromFile(apkFile)
                }
                
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            
            // On Android 11+ we can check if we have the permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    // Start the settings activity for user to grant the permission
                    val settingsIntent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                    // The user will have to return and click "Install" again or 
                    // we could rely on the system's own prompt
                }
            }
            
            context.startActivity(intent)
        } catch (e: Exception) {
            _uiState.update { 
                it.copy(updateState = UpdateState.Error("Installation failed: ${e.localizedMessage}"))
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

    fun setVideoQuality(quality: VideoQuality) {
        viewModelScope.launch {
            sessionManager.setVideoQuality(quality)
            _uiState.update { it.copy(videoQuality = quality) }
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

    fun setKuGouEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setEnableKuGou(enabled)
            _uiState.update { it.copy(kuGouEnabled = enabled) }
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

    // --- Misc Settings ---

    fun setStopMusicOnTaskClear(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setStopMusicOnTaskClearEnabled(enabled)
            _uiState.update { it.copy(stopMusicOnTaskClear = enabled) }
        }
    }

    fun setPauseMusicOnMediaMuted(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setPauseMusicOnMediaMutedEnabled(enabled)
            _uiState.update { it.copy(pauseMusicOnMediaMuted = enabled) }
        }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setKeepScreenOnEnabled(enabled)
            _uiState.update { it.copy(keepScreenOn = enabled) }
        }
    }

    fun setPureBlackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setPureBlackEnabled(enabled)
            _uiState.update { it.copy(pureBlackEnabled = enabled) }
        }
    }

    fun setPlayerAnimatedBackgroundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setPlayerAnimatedBackground(enabled)
            _uiState.update { it.copy(playerAnimatedBackgroundEnabled = enabled) }
        }
    }

    fun setPreferredLanguages(languages: Set<String>) {
        viewModelScope.launch {
            sessionManager.setPreferredLanguages(languages)
            _uiState.update { it.copy(preferredLanguages = languages) }
        }
    }

    fun setPreferredLyricsProvider(provider: String) {
        viewModelScope.launch {
            sessionManager.setPreferredLyricsProvider(provider)
            _uiState.update { it.copy(preferredLyricsProvider = provider) }
        }
    }

    fun setLyricsTextPosition(position: LyricsTextPosition) {
        viewModelScope.launch {
            sessionManager.setLyricsTextPosition(position)
            _uiState.update { it.copy(lyricsTextPosition = position) }
        }
    }

    fun setLyricsAnimationType(type: LyricsAnimationType) {
        viewModelScope.launch {
            sessionManager.setLyricsAnimationType(type)
            _uiState.update { it.copy(lyricsAnimationType = type) }
        }
    }

    fun setAudioOffloadEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setAudioOffloadEnabled(enabled)
            _uiState.update { it.copy(audioOffloadEnabled = enabled) }
        }
    }

    fun setVolumeBoostEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setVolumeBoostEnabled(enabled)
            _uiState.update { it.copy(volumeBoostEnabled = enabled) }
        }
    }

    fun setVolumeBoostAmount(amount: Int) {
        viewModelScope.launch {
            sessionManager.setVolumeBoostAmount(amount)
            _uiState.update { it.copy(volumeBoostAmount = amount) }
        }
    }
    fun setForceMaxRefreshRate(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setForceMaxRefreshRate(enabled)
            _uiState.update { it.copy(forceMaxRefreshRateEnabled = enabled) }
        }
    }

    fun setNavBarAlpha(alpha: Float) {
        viewModelScope.launch {
            sessionManager.setNavBarAlpha(alpha)
            _uiState.update { it.copy(navBarAlpha = alpha) }
        }
    }

    fun setSponsorBlockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setSponsorBlockEnabled(enabled)
            _uiState.update { it.copy(sponsorBlockEnabled = enabled) }
        }
    }
    fun toggleSponsorCategory(categoryKey: String, isEnabled: Boolean) {
        viewModelScope.launch {
            sessionManager.toggleSponsorCategory(categoryKey, isEnabled)
        }
    }
}
