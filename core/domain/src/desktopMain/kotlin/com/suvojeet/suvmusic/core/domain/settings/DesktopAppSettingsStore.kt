package com.suvojeet.suvmusic.core.domain.settings

import com.suvojeet.suvmusic.core.model.VideoQuality
import java.util.prefs.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Linux/JVM settings adapter; values persist in the user preferences store. */
class DesktopAppSettingsStore : AppSettingsStore {
    private val preferences = Preferences.userRoot().node("SuvMusic")
    private val _videoQuality = MutableStateFlow(readVideoQuality())
    private val _betterLyrics = MutableStateFlow(preferences.getBoolean("better_lyrics", true))
    private val _simpMusic = MutableStateFlow(preferences.getBoolean("simp_music", true))
    private val _kuGou = MutableStateFlow(preferences.getBoolean("kugou", true))
    private val _provider = MutableStateFlow(preferences.get("lyrics_provider", "BetterLyrics"))

    override val videoQuality: Flow<VideoQuality> = _videoQuality.asStateFlow()
    override val betterLyricsEnabled: Flow<Boolean> = _betterLyrics.asStateFlow()
    override val simpMusicEnabled: Flow<Boolean> = _simpMusic.asStateFlow()
    override val kuGouEnabled: Flow<Boolean> = _kuGou.asStateFlow()
    override val preferredLyricsProvider: Flow<String> = _provider.asStateFlow()

    override suspend fun getVideoQuality(): VideoQuality = _videoQuality.value
    override suspend fun setVideoQuality(value: VideoQuality) {
        preferences.put("video_quality", value.name)
        _videoQuality.value = value
    }

    override suspend fun isBetterLyricsEnabled(): Boolean = _betterLyrics.value
    override suspend fun setBetterLyricsEnabled(value: Boolean) {
        preferences.putBoolean("better_lyrics", value)
        _betterLyrics.value = value
    }

    override suspend fun isSimpMusicEnabled(): Boolean = _simpMusic.value
    override suspend fun setSimpMusicEnabled(value: Boolean) {
        preferences.putBoolean("simp_music", value)
        _simpMusic.value = value
    }

    override suspend fun isKuGouEnabled(): Boolean = _kuGou.value
    override suspend fun setKuGouEnabled(value: Boolean) {
        preferences.putBoolean("kugou", value)
        _kuGou.value = value
    }

    override suspend fun getPreferredLyricsProvider(): String = _provider.value
    override suspend fun setPreferredLyricsProvider(value: String) {
        preferences.put("lyrics_provider", value)
        _provider.value = value
    }

    private fun readVideoQuality(): VideoQuality = preferences.get("video_quality", VideoQuality.MEDIUM.name)
        .let { runCatching { VideoQuality.valueOf(it) }.getOrDefault(VideoQuality.MEDIUM) }
}
