package com.suvojeet.suvmusic.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.suvojeet.suvmusic.data.model.AudioQuality
import com.suvojeet.suvmusic.data.model.DownloadQuality
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "suvmusic_session")

/**
 * Manages session data for YouTube Music authentication.
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val COOKIES_KEY = stringPreferencesKey("cookies")
        private val USER_AVATAR_KEY = stringPreferencesKey("user_avatar")
        private val AUDIO_QUALITY_KEY = stringPreferencesKey("audio_quality")
        private val GAPLESS_PLAYBACK_KEY = booleanPreferencesKey("gapless_playback")
        private val AUTOMIX_KEY = booleanPreferencesKey("automix")
        private val CROSSFADE_DURATION_KEY = intPreferencesKey("crossfade_duration")
        private val DOWNLOAD_QUALITY_KEY = stringPreferencesKey("download_quality")
    }
    
    // --- Cookies ---
    
    fun getCookies(): String? = runBlocking {
        context.dataStore.data.first()[COOKIES_KEY]
    }
    
    suspend fun saveCookies(cookies: String) {
        context.dataStore.edit { preferences ->
            preferences[COOKIES_KEY] = cookies
        }
    }
    
    suspend fun clearCookies() {
        context.dataStore.edit { preferences ->
            preferences.remove(COOKIES_KEY)
        }
    }
    
    fun isLoggedIn(): Boolean = !getCookies().isNullOrBlank()
    
    // --- User Avatar ---
    
    fun getUserAvatar(): String? = runBlocking {
        context.dataStore.data.first()[USER_AVATAR_KEY]
    }
    
    suspend fun saveUserAvatar(url: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_AVATAR_KEY] = url
        }
    }
    
    // --- Audio Quality ---
    
    fun getAudioQuality(): AudioQuality = runBlocking {
        val qualityName = context.dataStore.data.first()[AUDIO_QUALITY_KEY]
        qualityName?.let { 
            try { AudioQuality.valueOf(it) } catch (e: Exception) { AudioQuality.HIGH }
        } ?: AudioQuality.HIGH
    }
    
    val audioQualityFlow: Flow<AudioQuality> = context.dataStore.data.map { preferences ->
        preferences[AUDIO_QUALITY_KEY]?.let {
            try { AudioQuality.valueOf(it) } catch (e: Exception) { AudioQuality.HIGH }
        } ?: AudioQuality.HIGH
    }
    
    suspend fun setAudioQuality(quality: AudioQuality) {
        context.dataStore.edit { preferences ->
            preferences[AUDIO_QUALITY_KEY] = quality.name
        }
    }
    
    // --- Playback Settings ---
    
    fun isGaplessPlaybackEnabled(): Boolean = runBlocking {
        context.dataStore.data.first()[GAPLESS_PLAYBACK_KEY] ?: true
    }
    
    suspend fun setGaplessPlayback(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[GAPLESS_PLAYBACK_KEY] = enabled
        }
    }
    
    fun isAutomixEnabled(): Boolean = runBlocking {
        context.dataStore.data.first()[AUTOMIX_KEY] ?: true
    }
    
    suspend fun setAutomix(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTOMIX_KEY] = enabled
        }
    }
    
    fun getCrossfadeDuration(): Int = runBlocking {
        context.dataStore.data.first()[CROSSFADE_DURATION_KEY] ?: 0
    }
    
    suspend fun setCrossfadeDuration(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[CROSSFADE_DURATION_KEY] = seconds
        }
    }
    
    // --- Download Quality ---
    
    fun getDownloadQuality(): DownloadQuality = runBlocking {
        val qualityName = context.dataStore.data.first()[DOWNLOAD_QUALITY_KEY]
        qualityName?.let { 
            try { DownloadQuality.valueOf(it) } catch (e: Exception) { DownloadQuality.HIGH }
        } ?: DownloadQuality.HIGH
    }
    
    val downloadQualityFlow: Flow<DownloadQuality> = context.dataStore.data.map { preferences ->
        preferences[DOWNLOAD_QUALITY_KEY]?.let {
            try { DownloadQuality.valueOf(it) } catch (e: Exception) { DownloadQuality.HIGH }
        } ?: DownloadQuality.HIGH
    }
    
    suspend fun setDownloadQuality(quality: DownloadQuality) {
        context.dataStore.edit { preferences ->
            preferences[DOWNLOAD_QUALITY_KEY] = quality.name
        }
    }
}

