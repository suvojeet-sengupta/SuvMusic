package com.suvojeet.suvmusic.data

import com.suvojeet.suvmusic.core.domain.settings.AppSettingsStore
import com.suvojeet.suvmusic.core.model.VideoQuality
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Android host adapter for the shared typed settings contract. */
@Singleton
class AndroidAppSettingsStore @Inject constructor(
    private val sessionManager: SessionManager,
) : AppSettingsStore {
    override val videoQuality: Flow<VideoQuality> = sessionManager.videoQualityFlow
    override val betterLyricsEnabled: Flow<Boolean> = sessionManager.enableBetterLyricsFlow
    override val simpMusicEnabled: Flow<Boolean> = sessionManager.enableSimpMusicFlow
    override val kuGouEnabled: Flow<Boolean> = sessionManager.enableKuGouFlow
    override val preferredLyricsProvider: Flow<String> = sessionManager.preferredLyricsProviderFlow

    override suspend fun getVideoQuality(): VideoQuality = sessionManager.getVideoQuality()
    override suspend fun setVideoQuality(value: VideoQuality) = sessionManager.setVideoQuality(value)
    override suspend fun isBetterLyricsEnabled(): Boolean = sessionManager.doesEnableBetterLyrics()
    override suspend fun setBetterLyricsEnabled(value: Boolean) = sessionManager.setEnableBetterLyrics(value)
    override suspend fun isSimpMusicEnabled(): Boolean = sessionManager.doesEnableSimpMusic()
    override suspend fun setSimpMusicEnabled(value: Boolean) = sessionManager.setEnableSimpMusic(value)
    override suspend fun isKuGouEnabled(): Boolean = sessionManager.doesEnableKuGou()
    override suspend fun setKuGouEnabled(value: Boolean) = sessionManager.setEnableKuGou(value)
    override suspend fun getPreferredLyricsProvider(): String = sessionManager.getPreferredLyricsProvider()
    override suspend fun setPreferredLyricsProvider(value: String) = sessionManager.setPreferredLyricsProvider(value)
}
