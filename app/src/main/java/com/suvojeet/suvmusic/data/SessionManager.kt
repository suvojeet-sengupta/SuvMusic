package com.suvojeet.suvmusic.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.suvojeet.suvmusic.data.model.AppTheme
import com.suvojeet.suvmusic.data.model.AudioQuality
import com.suvojeet.suvmusic.data.model.DownloadQuality
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.model.SongSource
import com.suvojeet.suvmusic.data.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
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
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        "suvmusic_secure_session",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private val COOKIES_KEY = stringPreferencesKey("cookies")
        private val USER_AVATAR_KEY = stringPreferencesKey("user_avatar")
        private val AUDIO_QUALITY_KEY = stringPreferencesKey("audio_quality")
        private val GAPLESS_PLAYBACK_KEY = booleanPreferencesKey("gapless_playback")
        private val AUTOMIX_KEY = booleanPreferencesKey("automix")
        private val DOWNLOAD_QUALITY_KEY = stringPreferencesKey("download_quality")
        private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
        // Resume playback
        private val LAST_SONG_ID_KEY = stringPreferencesKey("last_song_id")
        private val LAST_POSITION_KEY = androidx.datastore.preferences.core.longPreferencesKey("last_position")
        private val LAST_QUEUE_KEY = stringPreferencesKey("last_queue")
        private val LAST_INDEX_KEY = intPreferencesKey("last_index")
        // Recent searches
        private val RECENT_SEARCHES_KEY = stringPreferencesKey("recent_searches")
        private const val MAX_RECENT_SEARCHES = 20
        
        // Recently Played
        private val RECENTLY_PLAYED_KEY = stringPreferencesKey("recently_played")
        private const val MAX_RECENTLY_PLAYED = 50
        
        // Home Cache
        private val HOME_CACHE_KEY = stringPreferencesKey("home_cache")
        private val JIOSAAVN_HOME_CACHE_KEY = stringPreferencesKey("jiosaavn_home_cache")
        
        // Library Cache
        private val LIBRARY_PLAYLISTS_CACHE_KEY = stringPreferencesKey("library_playlists_cache")
        private val LIBRARY_LIKED_SONGS_CACHE_KEY = stringPreferencesKey("library_liked_songs_cache")
        
        // Music Source
        private val MUSIC_SOURCE_KEY = stringPreferencesKey("music_source")
        
        // Developer Mode (Hidden feature)
        private val DEV_MODE_KEY = stringPreferencesKey("_dx_mode")
        
        // Floating Player (overlay when minimized)
        private val DYNAMIC_ISLAND_ENABLED_KEY = booleanPreferencesKey("dynamic_island_enabled")
        
        // Player Customization
        private val SEEKBAR_STYLE_KEY = stringPreferencesKey("seekbar_style")
        private val ARTWORK_SHAPE_KEY = stringPreferencesKey("artwork_shape")
        private val ARTWORK_SIZE_KEY = stringPreferencesKey("artwork_size")
        
        // Static Theme
        private val APP_THEME_KEY = stringPreferencesKey("app_theme")
        
        // Radio / Endless Queue
        private val ENDLESS_QUEUE_ENABLED_KEY = booleanPreferencesKey("endless_queue_enabled")
        
        // Offline Mode
        private val OFFLINE_MODE_ENABLED_KEY = booleanPreferencesKey("offline_mode_enabled")
        
        // Volume Slider
        private val VOLUME_SLIDER_ENABLED_KEY = booleanPreferencesKey("volume_slider_enabled")

        // Volume Normalization
        private val VOLUME_NORMALIZATION_ENABLED_KEY = booleanPreferencesKey("volume_normalization_enabled")

        // Mini Player Visibility
        private val MINI_PLAYER_ALPHA_KEY = androidx.datastore.preferences.core.floatPreferencesKey("mini_player_alpha")
        
        // Double Tap Seek
        private val DOUBLE_TAP_SEEK_SECONDS_KEY = intPreferencesKey("double_tap_seek_seconds")
    }
    
    // --- Developer Mode (Hidden) ---
    
    /**
     * Check if developer mode is enabled.
     * When enabled, JioSaavn option becomes visible.
     */
    fun isDeveloperMode(): Boolean = runBlocking {
        context.dataStore.data.first()[DEV_MODE_KEY] == "unlocked"
    }
    
    val developerModeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DEV_MODE_KEY] == "unlocked"
    }
    
    /**
     * Enable developer mode (unlocks JioSaavn).
     */
    suspend fun enableDeveloperMode() {
        context.dataStore.edit { preferences ->
            preferences[DEV_MODE_KEY] = "unlocked"
        }
    }
    
    /**
     * Disable developer mode (hides JioSaavn).
     */
    suspend fun disableDeveloperMode() {
        context.dataStore.edit { preferences ->
            preferences.remove(DEV_MODE_KEY)
        }
    }
    
    // --- Floating Player ---
    
    fun isDynamicIslandEnabled(): Boolean = runBlocking {
        context.dataStore.data.first()[DYNAMIC_ISLAND_ENABLED_KEY] ?: false
    }
    
    val dynamicIslandEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DYNAMIC_ISLAND_ENABLED_KEY] ?: false
    }
    
    suspend fun setDynamicIslandEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DYNAMIC_ISLAND_ENABLED_KEY] = enabled
        }
    }
    
    // --- Player Customization ---
    
    /**
     * Get saved seekbar style (defaults to WAVEFORM)
     */
    fun getSeekbarStyle(): String = runBlocking {
        context.dataStore.data.first()[SEEKBAR_STYLE_KEY] ?: "WAVEFORM"
    }
    
    val seekbarStyleFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SEEKBAR_STYLE_KEY] ?: "WAVEFORM"
    }
    
    suspend fun setSeekbarStyle(style: String) {
        context.dataStore.edit { preferences ->
            preferences[SEEKBAR_STYLE_KEY] = style
        }
    }
    
    /**
     * Get saved artwork shape (defaults to ROUNDED_SQUARE)
     */
    fun getArtworkShape(): String = runBlocking {
        context.dataStore.data.first()[ARTWORK_SHAPE_KEY] ?: "ROUNDED_SQUARE"
    }
    
    val artworkShapeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[ARTWORK_SHAPE_KEY] ?: "ROUNDED_SQUARE"
    }
    
    suspend fun setArtworkShape(shape: String) {
        context.dataStore.edit { preferences ->
            preferences[ARTWORK_SHAPE_KEY] = shape
        }
    }
    
    /**
     * Get saved artwork size (defaults to LARGE)
     */
    fun getArtworkSize(): String = runBlocking {
        context.dataStore.data.first()[ARTWORK_SIZE_KEY] ?: "LARGE"
    }
    
    val artworkSizeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[ARTWORK_SIZE_KEY] ?: "LARGE"
    }
    
    suspend fun setArtworkSize(size: String) {
        context.dataStore.edit { preferences ->
            preferences[ARTWORK_SIZE_KEY] = size
        }
    }
    
    // --- Endless Queue / Radio Mode ---
    
    fun isEndlessQueueEnabled(): Boolean = runBlocking {
        context.dataStore.data.first()[ENDLESS_QUEUE_ENABLED_KEY] ?: true // Enabled by default
    }
    
    val endlessQueueFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ENDLESS_QUEUE_ENABLED_KEY] ?: true
    }
    
    suspend fun setEndlessQueue(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENDLESS_QUEUE_ENABLED_KEY] = enabled
        }
    }
    
    // --- Offline Mode ---
    
    fun isOfflineModeEnabled(): Boolean = runBlocking {
        context.dataStore.data.first()[OFFLINE_MODE_ENABLED_KEY] ?: false
    }
    
    val offlineModeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[OFFLINE_MODE_ENABLED_KEY] ?: false
    }
    
    suspend fun setOfflineMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[OFFLINE_MODE_ENABLED_KEY] = enabled
        }
    }
    
    // --- Volume Slider ---
    
    fun isVolumeSliderEnabled(): Boolean = runBlocking {
        context.dataStore.data.first()[VOLUME_SLIDER_ENABLED_KEY] ?: true
    }
    
    val volumeSliderEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[VOLUME_SLIDER_ENABLED_KEY] ?: true
    }
    
    suspend fun setVolumeSliderEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VOLUME_SLIDER_ENABLED_KEY] = enabled
        }
    }

    // --- Mini Player Transparency ---

    fun getMiniPlayerAlpha(): Float = runBlocking {
        context.dataStore.data.first()[MINI_PLAYER_ALPHA_KEY] ?: 1f
    }

    val miniPlayerAlphaFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[MINI_PLAYER_ALPHA_KEY] ?: 1f
    }

    suspend fun setMiniPlayerAlpha(alpha: Float) {
        context.dataStore.edit { preferences ->
            preferences[MINI_PLAYER_ALPHA_KEY] = alpha
        }
    }
    
    // --- Double Tap Seek ---
    
    fun getDoubleTapSeekSeconds(): Int = runBlocking {
        context.dataStore.data.first()[DOUBLE_TAP_SEEK_SECONDS_KEY] ?: 10
    }
    
    val doubleTapSeekSecondsFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[DOUBLE_TAP_SEEK_SECONDS_KEY] ?: 10
    }
    
    suspend fun setDoubleTapSeekSeconds(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[DOUBLE_TAP_SEEK_SECONDS_KEY] = seconds
        }
    }
    
    // --- Cookies ---
    
    fun getCookies(): String? {
        // Try getting from encrypted prefs first
        val secureCookies = encryptedPrefs.getString("cookies", null)
        if (secureCookies != null) return secureCookies
        
        // Migration: Check if cookies exist in DataStore
        val oldCookies = runBlocking {
            context.dataStore.data.first()[COOKIES_KEY]
        }
        
        if (oldCookies != null) {
            // Save to secure prefs and clear from DataStore
            encryptedPrefs.edit().putString("cookies", oldCookies).apply()
            runBlocking {
                context.dataStore.edit { it.remove(COOKIES_KEY) }
            }
            return oldCookies
        }
        
        return null
    }
    
    suspend fun saveCookies(cookies: String) {
        encryptedPrefs.edit().putString("cookies", cookies).apply()
        // Also ensure cleared from DataStore
        context.dataStore.edit { it.remove(COOKIES_KEY) }
    }
    
    suspend fun clearCookies() {
        encryptedPrefs.edit().remove("cookies").apply()
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

    val userAvatarFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_AVATAR_KEY]
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

    // --- Volume Normalization ---

    fun isVolumeNormalizationEnabled(): Boolean = runBlocking {
        context.dataStore.data.first()[VOLUME_NORMALIZATION_ENABLED_KEY] ?: false
    }

    val volumeNormalizationEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[VOLUME_NORMALIZATION_ENABLED_KEY] ?: false
    }

    suspend fun setVolumeNormalizationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VOLUME_NORMALIZATION_ENABLED_KEY] = enabled
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

    // --- Onboarding ---

    fun isOnboardingCompleted(): Boolean = runBlocking {
        context.dataStore.data.first()[ONBOARDING_COMPLETED_KEY] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] = completed
        }
    }
    
    // --- Theme Mode ---
    
    fun getThemeMode(): ThemeMode = runBlocking {
        val modeName = context.dataStore.data.first()[THEME_MODE_KEY]
        modeName?.let { 
            try { ThemeMode.valueOf(it) } catch (e: Exception) { ThemeMode.SYSTEM }
        } ?: ThemeMode.SYSTEM
    }
    
    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE_KEY]?.let {
            try { ThemeMode.valueOf(it) } catch (e: Exception) { ThemeMode.SYSTEM }
        } ?: ThemeMode.SYSTEM
    }
    
    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.name
        }
    }

    // --- App Theme (Static Colors) ---

    fun getAppTheme(): AppTheme = runBlocking {
        val themeName = context.dataStore.data.first()[APP_THEME_KEY]
        themeName?.let {
            try { AppTheme.valueOf(it) } catch (e: Exception) { AppTheme.DEFAULT }
        } ?: AppTheme.DEFAULT
    }

    val appThemeFlow: Flow<AppTheme> = context.dataStore.data.map { preferences ->
        preferences[APP_THEME_KEY]?.let {
            try { AppTheme.valueOf(it) } catch (e: Exception) { AppTheme.DEFAULT }
        } ?: AppTheme.DEFAULT
    }

    suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[APP_THEME_KEY] = theme.name
        }
    }
    
    // --- Dynamic Color ---
    
    fun isDynamicColorEnabled(): Boolean = runBlocking {
        context.dataStore.data.first()[DYNAMIC_COLOR_KEY] ?: true
    }
    
    val dynamicColorFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DYNAMIC_COLOR_KEY] ?: true
    }
    
    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR_KEY] = enabled
        }
    }
    
    // --- Music Source ---
    
    fun getMusicSource(): MusicSource = runBlocking {
        val sourceName = context.dataStore.data.first()[MUSIC_SOURCE_KEY]
        sourceName?.let { 
            try { MusicSource.valueOf(it) } catch (e: Exception) { MusicSource.YOUTUBE }
        } ?: MusicSource.YOUTUBE
    }
    
    val musicSourceFlow: Flow<MusicSource> = context.dataStore.data.map { preferences ->
        preferences[MUSIC_SOURCE_KEY]?.let {
            try { MusicSource.valueOf(it) } catch (e: Exception) { MusicSource.YOUTUBE }
        } ?: MusicSource.YOUTUBE
    }
    
    suspend fun setMusicSource(source: MusicSource) {
        context.dataStore.edit { preferences ->
            preferences[MUSIC_SOURCE_KEY] = source.name
        }
    }
    
    // --- Resume Playback ---
    
    /**
     * Save last playback state for resume functionality.
     */
    suspend fun savePlaybackState(songId: String, position: Long, queueJson: String, index: Int) {
        context.dataStore.edit { preferences ->
            preferences[LAST_SONG_ID_KEY] = songId
            preferences[LAST_POSITION_KEY] = position
            preferences[LAST_QUEUE_KEY] = queueJson
            preferences[LAST_INDEX_KEY] = index
        }
    }
    
    /**
     * Get last saved playback state.
     * @return Quadruple of (songId, position, queueJson, index) or null if not saved.
     */
    suspend fun getLastPlaybackState(): LastPlaybackState? = withContext(Dispatchers.IO) {
        val prefs = context.dataStore.data.first()
        val songId = prefs[LAST_SONG_ID_KEY]
        val position = prefs[LAST_POSITION_KEY]
        val queueJson = prefs[LAST_QUEUE_KEY]
        val index = prefs[LAST_INDEX_KEY]
        
        if (songId != null && position != null && queueJson != null && index != null) {
            LastPlaybackState(songId, position, queueJson, index)
        } else null
    }
    
    /**
     * Clear saved playback state.
     */
    suspend fun clearPlaybackState() {
        context.dataStore.edit { preferences ->
            preferences.remove(LAST_SONG_ID_KEY)
            preferences.remove(LAST_POSITION_KEY)
            preferences.remove(LAST_QUEUE_KEY)
            preferences.remove(LAST_INDEX_KEY)
        }
    }
    
    // --- Recent Searches ---
    
    /**
     * Get recent searches list.
     */
    /**
     * Get recent searches list.
     */
    suspend fun getRecentSearches(): List<Song> = withContext(Dispatchers.IO) {
        val json = context.dataStore.data.first()[RECENT_SEARCHES_KEY] ?: return@withContext emptyList()
        withContext(Dispatchers.Default) {
            parseRecentSearchesJson(json)
        }
    }
    
    val recentSearchesFlow: Flow<List<Song>> = context.dataStore.data.map { preferences ->
        val json = preferences[RECENT_SEARCHES_KEY] ?: return@map emptyList()
        parseRecentSearchesJson(json)
    }
    
    private fun parseRecentSearchesJson(json: String): List<Song> {
        return try {
            val jsonArray = JSONArray(json)
            val songs = mutableListOf<Song>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                songs.add(
                    Song(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        artist = obj.getString("artist"),
                        album = obj.optString("album", ""),
                        thumbnailUrl = obj.optString("thumbnailUrl", null),
                        duration = obj.optLong("duration", 0L),
                        source = try {
                            SongSource.valueOf(obj.optString("source", "YOUTUBE"))
                        } catch (e: Exception) {
                            SongSource.YOUTUBE
                        }
                    )
                )
            }
            songs
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Add a song to recent searches.
     */
    suspend fun addRecentSearch(song: Song) {
        val currentSearches = getRecentSearches().toMutableList()
        
        // Remove if already exists (to move to top)
        currentSearches.removeAll { it.id == song.id }
        
        // Add to beginning
        currentSearches.add(0, song)
        
        // Keep only max items
        val trimmed = currentSearches.take(MAX_RECENT_SEARCHES)
        
        // Save
        val jsonArray = JSONArray()
        trimmed.forEach { s ->
            jsonArray.put(JSONObject().apply {
                put("id", s.id)
                put("title", s.title)
                put("artist", s.artist)
                put("album", s.album ?: "")
                put("thumbnailUrl", s.thumbnailUrl ?: "")
                put("duration", s.duration)
                put("source", s.source.name)
            })
        }
        
        context.dataStore.edit { preferences ->
            preferences[RECENT_SEARCHES_KEY] = jsonArray.toString()
        }
    }
    
    /**
     * Clear all recent searches.
     */
    suspend fun clearRecentSearches() {
        context.dataStore.edit { preferences ->
            preferences.remove(RECENT_SEARCHES_KEY)
        }
    }
    
    // --- Recently Played ---
    
    /**
     * Get recently played songs as a Flow.
     */
    val recentlyPlayedFlow: Flow<List<com.suvojeet.suvmusic.data.model.RecentlyPlayed>> = context.dataStore.data.map { preferences ->
        parseRecentlyPlayed(preferences[RECENTLY_PLAYED_KEY])
    }
    
    /**
     * Add a song to recently played.
     */
    suspend fun addToRecentlyPlayed(song: Song) {
        context.dataStore.edit { preferences ->
            val existing = parseRecentlyPlayed(preferences[RECENTLY_PLAYED_KEY]).toMutableList()
            
            // Remove if already exists (will re-add at top)
            existing.removeAll { it.song.id == song.id }
            
            // Add at beginning
            existing.add(0, com.suvojeet.suvmusic.data.model.RecentlyPlayed(song, System.currentTimeMillis()))
            
            // Limit size
            val limited = existing.take(MAX_RECENTLY_PLAYED)
            
            // Serialize
            preferences[RECENTLY_PLAYED_KEY] = serializeRecentlyPlayed(limited)
        }
    }
    
    /**
     * Clear recently played history.
     */
    suspend fun clearRecentlyPlayed() {
        context.dataStore.edit { preferences ->
            preferences.remove(RECENTLY_PLAYED_KEY)
        }
    }
    
    private fun parseRecentlyPlayed(json: String?): List<com.suvojeet.suvmusic.data.model.RecentlyPlayed> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                val songObj = obj.optJSONObject("song") ?: return@mapNotNull null
                val playedAt = obj.optLong("playedAt", System.currentTimeMillis())
                
                val song = jsonToSong(songObj) ?: return@mapNotNull null
                com.suvojeet.suvmusic.data.model.RecentlyPlayed(song, playedAt)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun serializeRecentlyPlayed(list: List<com.suvojeet.suvmusic.data.model.RecentlyPlayed>): String {
        val array = JSONArray()
        list.forEach { recent ->
            val songObj = songToJson(recent.song)
            val obj = JSONObject().apply {
                put("song", songObj)
                put("playedAt", recent.playedAt)
            }
            array.put(obj)
        }
        return array.toString()
    }
    
    // --- Home Cache ---
    
    suspend fun saveHomeCache(sections: List<com.suvojeet.suvmusic.data.model.HomeSection>) {
        val json = withContext(Dispatchers.Default) {
            serializeHomeSections(sections)
        }
        context.dataStore.edit { preferences ->
            preferences[HOME_CACHE_KEY] = json
        }
    }
    
    fun getCachedHomeSections(): Flow<List<com.suvojeet.suvmusic.data.model.HomeSection>> = context.dataStore.data
        .map { preferences -> preferences[HOME_CACHE_KEY] }
        .flowOn(Dispatchers.IO)
        .map { json -> 
            withContext(Dispatchers.Default) {
                parseHomeSections(json)
            }
        }
    
    suspend fun getCachedHomeSectionsSync(): List<com.suvojeet.suvmusic.data.model.HomeSection> = withContext(Dispatchers.IO) {
        val prefs = context.dataStore.data.first()
        val json = prefs[HOME_CACHE_KEY]
        withContext(Dispatchers.Default) {
            parseHomeSections(json)
        }
    }
    
    suspend fun saveJioSaavnHomeCache(sections: List<com.suvojeet.suvmusic.data.model.HomeSection>) {
        val json = withContext(Dispatchers.Default) {
            serializeHomeSections(sections)
        }
        context.dataStore.edit { preferences ->
            preferences[JIOSAAVN_HOME_CACHE_KEY] = json
        }
    }
    
    fun getCachedJioSaavnHomeSections(): Flow<List<com.suvojeet.suvmusic.data.model.HomeSection>> = context.dataStore.data
        .map { preferences -> preferences[JIOSAAVN_HOME_CACHE_KEY] }
        .flowOn(Dispatchers.IO)
        .map { json ->
            withContext(Dispatchers.Default) {
                parseHomeSections(json)
            }
        }
    
    suspend fun getCachedJioSaavnHomeSectionsSync(): List<com.suvojeet.suvmusic.data.model.HomeSection> = withContext(Dispatchers.IO) {
        val prefs = context.dataStore.data.first()
        val json = prefs[JIOSAAVN_HOME_CACHE_KEY]
        withContext(Dispatchers.Default) {
            parseHomeSections(json)
        }
    }
    
    // --- Library Cache ---
    
    suspend fun saveLibraryPlaylistsCache(playlists: List<com.suvojeet.suvmusic.data.model.PlaylistDisplayItem>) {
        val json = withContext(Dispatchers.Default) {
             val array = JSONArray()
             playlists.forEach { playlist ->
                 array.put(JSONObject().apply {
                     put("id", playlist.id)
                     put("name", playlist.name)
                     put("url", playlist.url)
                     put("uploaderName", playlist.uploaderName)
                     put("thumbnailUrl", playlist.thumbnailUrl ?: "")
                     put("songCount", playlist.songCount)
                 })
             }
             array.toString()
        }
        context.dataStore.edit { preferences ->
            preferences[LIBRARY_PLAYLISTS_CACHE_KEY] = json
        }
    }
    
    suspend fun getCachedLibraryPlaylistsSync(): List<com.suvojeet.suvmusic.data.model.PlaylistDisplayItem> = withContext(Dispatchers.IO) {
        val prefs = context.dataStore.data.first()
        val json = prefs[LIBRARY_PLAYLISTS_CACHE_KEY] ?: return@withContext emptyList()
        withContext(Dispatchers.Default) {
            try {
                val array = JSONArray(json)
                (0 until array.length()).mapNotNull { i ->
                    val obj = array.optJSONObject(i) ?: return@mapNotNull null
                    com.suvojeet.suvmusic.data.model.PlaylistDisplayItem(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        url = obj.optString("url"),
                        uploaderName = obj.optString("uploaderName"),
                        thumbnailUrl = obj.optString("thumbnailUrl").takeIf { it.isNotBlank() },
                        songCount = obj.optInt("songCount", 0)
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    suspend fun saveLibraryLikedSongsCache(songs: List<Song>) {
        val json = withContext(Dispatchers.Default) {
            val array = JSONArray()
            songs.forEach { song -> array.put(songToJson(song)) }
            array.toString()
        }
        context.dataStore.edit { preferences ->
            preferences[LIBRARY_LIKED_SONGS_CACHE_KEY] = json
        }
    }
    
    suspend fun getCachedLibraryLikedSongsSync(): List<Song> = withContext(Dispatchers.IO) {
        val prefs = context.dataStore.data.first()
        val json = prefs[LIBRARY_LIKED_SONGS_CACHE_KEY] ?: return@withContext emptyList()
        withContext(Dispatchers.Default) {
            try {
                val array = JSONArray(json)
                (0 until array.length()).mapNotNull { i ->
                    jsonToSong(array.optJSONObject(i) ?: return@mapNotNull null)
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    // --- Helpers ---

    private fun parseHomeSections(json: String?): List<com.suvojeet.suvmusic.data.model.HomeSection> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                val title = obj.optString("title")
                val itemsArray = obj.optJSONArray("items") ?: JSONArray()
                
                val items = (0 until itemsArray.length()).mapNotNull { j ->
                    parseHomeItem(itemsArray.optJSONObject(j))
                }
                
                com.suvojeet.suvmusic.data.model.HomeSection(title, items)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun serializeHomeSections(sections: List<com.suvojeet.suvmusic.data.model.HomeSection>): String {
        val array = JSONArray()
        sections.forEach { section ->
            val obj = JSONObject().apply {
                put("title", section.title)
                val itemsArray = JSONArray()
                section.items.forEach { item ->
                    itemsArray.put(serializeHomeItem(item))
                }
                put("items", itemsArray)
            }
            array.put(obj)
        }
        return array.toString()
    }
    
    private fun parseHomeItem(obj: JSONObject?): com.suvojeet.suvmusic.data.model.HomeItem? {
        if (obj == null) return null
        val type = obj.optString("type")
        val data = obj.optJSONObject("data") ?: return null
        
        return when (type) {
            "song" -> {
                val song = jsonToSong(data) ?: return null
                com.suvojeet.suvmusic.data.model.HomeItem.SongItem(song)
            }
            "playlist" -> {
                val playlist = com.suvojeet.suvmusic.data.model.PlaylistDisplayItem(
                    id = data.optString("id"),
                    name = data.optString("name"),
                    url = data.optString("url"),
                    uploaderName = data.optString("uploaderName"),
                    thumbnailUrl = data.optString("thumbnailUrl").takeIf { it.isNotBlank() },
                    songCount = data.optInt("songCount", 0)
                )
                com.suvojeet.suvmusic.data.model.HomeItem.PlaylistItem(playlist)
            }
            "album" -> {
                val album = com.suvojeet.suvmusic.data.model.Album(
                    id = data.optString("id"),
                    title = data.optString("title"),
                    artist = data.optString("artist"),
                    year = data.optString("year").takeIf { it.isNotBlank() },
                    thumbnailUrl = data.optString("thumbnailUrl").takeIf { it.isNotBlank() },
                    description = data.optString("description").takeIf { it.isNotBlank() }
                )
                com.suvojeet.suvmusic.data.model.HomeItem.AlbumItem(album)
            }
            "artist" -> {
                val artist = com.suvojeet.suvmusic.data.model.Artist(
                    id = data.optString("id"),
                    name = data.optString("name"),
                    thumbnailUrl = data.optString("thumbnailUrl").takeIf { it.isNotBlank() },
                    description = data.optString("description").takeIf { it.isNotBlank() },
                    subscribers = data.optString("subscribers").takeIf { it.isNotBlank() }
                )
                com.suvojeet.suvmusic.data.model.HomeItem.ArtistItem(artist)
            }
            else -> null
        }
    }
    
    private fun serializeHomeItem(item: com.suvojeet.suvmusic.data.model.HomeItem): JSONObject {
        val obj = JSONObject()
        when (item) {
            is com.suvojeet.suvmusic.data.model.HomeItem.SongItem -> {
                obj.put("type", "song")
                obj.put("data", songToJson(item.song))
            }
            is com.suvojeet.suvmusic.data.model.HomeItem.PlaylistItem -> {
                obj.put("type", "playlist")
                val data = JSONObject().apply {
                    put("id", item.playlist.id)
                    put("name", item.playlist.name)
                    put("url", item.playlist.url)
                    put("uploaderName", item.playlist.uploaderName)
                    put("thumbnailUrl", item.playlist.thumbnailUrl ?: "")
                    put("songCount", item.playlist.songCount)
                }
                obj.put("data", data)
            }
            is com.suvojeet.suvmusic.data.model.HomeItem.AlbumItem -> {
                obj.put("type", "album")
                val data = JSONObject().apply {
                    put("id", item.album.id)
                    put("title", item.album.title)
                    put("artist", item.album.artist)
                    put("year", item.album.year ?: "")
                    put("thumbnailUrl", item.album.thumbnailUrl ?: "")
                    put("description", item.album.description ?: "")
                }
                obj.put("data", data)
            }
            is com.suvojeet.suvmusic.data.model.HomeItem.ArtistItem -> {
                obj.put("type", "artist")
                val data = JSONObject().apply {
                    put("id", item.artist.id)
                    put("name", item.artist.name)
                    put("thumbnailUrl", item.artist.thumbnailUrl ?: "")
                    put("description", item.artist.description ?: "")
                    put("subscribers", item.artist.subscribers ?: "")
                }
                obj.put("data", data)
            }
        }
        return obj
    }

    private fun jsonToSong(songObj: JSONObject): Song? {
        return try {
            Song(
                id = songObj.optString("id"),
                title = songObj.optString("title"),
                artist = songObj.optString("artist"),
                album = songObj.optString("album"),
                thumbnailUrl = songObj.optString("thumbnailUrl").takeIf { it.isNotBlank() },
                duration = songObj.optLong("duration"),
                source = try { 
                    SongSource.valueOf(songObj.optString("source", "YOUTUBE")) 
                } catch (e: Exception) { 
                    SongSource.YOUTUBE 
                },
                localUri = songObj.optString("localUri").takeIf { it.isNotBlank() }?.let { android.net.Uri.parse(it) }
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun songToJson(song: Song): JSONObject {
        return JSONObject().apply {
            put("id", song.id)
            put("title", song.title)
            put("artist", song.artist)
            put("album", song.album)
            put("thumbnailUrl", song.thumbnailUrl ?: "")
            put("duration", song.duration)
            put("source", song.source.name)
            put("localUri", song.localUri?.toString() ?: "")
        }
    }
}

/**
 * Data class for last playback state.
 */
data class LastPlaybackState(
    val songId: String,
    val position: Long,
    val queueJson: String,
    val index: Int
)

/**
 * Music source preference.
 */
enum class MusicSource {
    YOUTUBE,
    JIOSAAVN,
    BOTH
}
