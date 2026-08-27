package com.suvojeet.suvmusic.core.domain.settings

import com.suvojeet.suvmusic.core.model.VideoQuality
import kotlinx.coroutines.flow.Flow

/** Shared settings surface used by common UI and feature ViewModels. */
interface AppSettingsStore {
    val videoQuality: Flow<VideoQuality>
    val betterLyricsEnabled: Flow<Boolean>
    val simpMusicEnabled: Flow<Boolean>
    val kuGouEnabled: Flow<Boolean>
    val preferredLyricsProvider: Flow<String>

    suspend fun getVideoQuality(): VideoQuality
    suspend fun setVideoQuality(value: VideoQuality)
    suspend fun isBetterLyricsEnabled(): Boolean
    suspend fun setBetterLyricsEnabled(value: Boolean)
    suspend fun isSimpMusicEnabled(): Boolean
    suspend fun setSimpMusicEnabled(value: Boolean)
    suspend fun isKuGouEnabled(): Boolean
    suspend fun setKuGouEnabled(value: Boolean)
    suspend fun getPreferredLyricsProvider(): String
    suspend fun setPreferredLyricsProvider(value: String)
}
