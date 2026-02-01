package com.suvojeet.suvmusic.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.suvojeet.suvmusic.data.model.Album
import com.suvojeet.suvmusic.data.model.AppTheme
import com.suvojeet.suvmusic.data.model.Artist
import com.suvojeet.suvmusic.data.model.AudioQuality
import com.suvojeet.suvmusic.data.model.VideoQuality
import com.suvojeet.suvmusic.data.model.DownloadQuality
import com.suvojeet.suvmusic.data.model.HapticsIntensity
import com.suvojeet.suvmusic.data.model.HapticsMode
import com.suvojeet.suvmusic.data.model.HomeItem
import com.suvojeet.suvmusic.data.model.HomeSection
import com.suvojeet.suvmusic.data.model.HomeSectionType
import com.suvojeet.suvmusic.data.model.MiniPlayerStyle
import com.suvojeet.suvmusic.data.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.data.model.RecentlyPlayed
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.model.SongSource
import com.suvojeet.suvmusic.data.model.SponsorCategory
import com.suvojeet.suvmusic.data.model.ThemeMode
import com.suvojeet.suvmusic.providers.lyrics.LyricsAnimationType
import com.suvojeet.suvmusic.providers.lyrics.LyricsProviderType
import com.suvojeet.suvmusic.providers.lyrics.LyricsTextPosition
import com.suvojeet.suvmusic.data.model.UpdateChannel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
        private val VIDEO_QUALITY_KEY = stringPreferencesKey("video_quality")
        private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color")
        
        private val LAST_SONG_ID_KEY = stringPreferencesKey("last_song_id")
        private val LAST_POSITION_KEY = longPreferencesKey("last_position")
        private val LAST_QUEUE_KEY = stringPreferencesKey("last_queue")
        private val LAST_INDEX_KEY = intPreferencesKey("last_index")
        
        private val RECENT_SEARCHES_KEY = stringPreferencesKey("recent_searches")
        private const val MAX_RECENT_SEARCHES = 20
        
        private val RECENTLY_PLAYED_KEY = stringPreferencesKey("recently_played")
        private const val MAX_RECENTLY_PLAYED = 50
        
        private val HOME_CACHE_KEY = stringPreferencesKey("home_cache")
        private val JIOSAAVN_HOME_CACHE_KEY = stringPreferencesKey("jiosaavn_home_cache")
        private val LAST_FETCH_TIME_YOUTUBE_KEY = longPreferencesKey("last_fetch_time_youtube")
        private val LAST_FETCH_TIME_JIOSAAVN_KEY = longPreferencesKey("last_fetch_time_jiosaavn")
        
        private val LIBRARY_PLAYLISTS_CACHE_KEY = stringPreferencesKey("library_playlists_cache")
        private val LIBRARY_LIKED_SONGS_CACHE_KEY = stringPreferencesKey("library_liked_songs_cache")
        
        private val MUSIC_SOURCE_KEY = stringPreferencesKey("music_source")
        private val DEV_MODE_KEY = stringPreferencesKey("_dx_mode")
        private val DYNAMIC_ISLAND_ENABLED_KEY = booleanPreferencesKey("dynamic_island_enabled")
        
        private val SEEKBAR_STYLE_KEY = stringPreferencesKey("seekbar_style")
        private val ARTWORK_SHAPE_KEY = stringPreferencesKey("artwork_shape")
        private val ARTWORK_SIZE_KEY = stringPreferencesKey("artwork_size")
        
        private val APP_THEME_KEY = stringPreferencesKey("app_theme")
        private val ENDLESS_QUEUE_ENABLED_KEY = booleanPreferencesKey("endless_queue_enabled")
        private val OFFLINE_MODE_ENABLED_KEY = booleanPreferencesKey("offline_mode_enabled")
        private val VOLUME_SLIDER_ENABLED_KEY = booleanPreferencesKey("volume_slider_enabled")
        private val VOLUME_NORMALIZATION_ENABLED_KEY = booleanPreferencesKey("volume_normalization_enabled")
        private val MINI_PLAYER_ALPHA_KEY = floatPreferencesKey("mini_player_alpha")
        private val NAV_BAR_ALPHA_KEY = floatPreferencesKey("nav_bar_alpha")
        private val DOUBLE_TAP_SEEK_SECONDS_KEY = intPreferencesKey("double_tap_seek_seconds")
        
        private val ENABLE_BETTER_LYRICS_KEY = booleanPreferencesKey("enable_better_lyrics")
        private val ENABLE_SIMPMUSIC_KEY = booleanPreferencesKey("enable_simpmusic")
        private val ENABLE_KUGOU_KEY = booleanPreferencesKey("enable_kugou")
        private val PREFERRED_LYRICS_PROVIDER_KEY = stringPreferencesKey("preferred_lyrics_provider")
        private val LYRICS_TEXT_POSITION_KEY = stringPreferencesKey("lyrics_text_position")
        private val LYRICS_ANIMATION_TYPE_KEY = stringPreferencesKey("lyrics_animation_type")

        private val PLAYER_CACHE_LIMIT_KEY = longPreferencesKey("player_cache_limit")
        private val PLAYER_CACHE_AUTO_CLEAR_INTERVAL_KEY = intPreferencesKey("player_cache_auto_clear_interval")
        private val PLAYER_CACHE_LAST_CLEARED_TIMESTAMP_KEY = longPreferencesKey("player_cache_last_cleared_timestamp")
        
        private val MUSIC_HAPTICS_ENABLED_KEY = booleanPreferencesKey("music_haptics_enabled")
        private val HAPTICS_MODE_KEY = stringPreferencesKey("haptics_mode")
        private val HAPTICS_INTENSITY_KEY = stringPreferencesKey("haptics_intensity")

        private val STOP_MUSIC_ON_TASK_CLEAR_KEY = booleanPreferencesKey("stop_music_on_task_clear")
        private val PAUSE_MUSIC_ON_MEDIA_MUTED_KEY = booleanPreferencesKey("pause_music_on_media_muted")
        private val KEEP_SCREEN_ON_KEY = booleanPreferencesKey("keep_screen_on")
        private val PURE_BLACK_KEY = booleanPreferencesKey("pure_black_enabled")
        private val MINI_PLAYER_STYLE_KEY = stringPreferencesKey("mini_player_style")
        private val AUDIO_OFFLOAD_ENABLED_KEY = booleanPreferencesKey("audio_offload_enabled")
        private val VOLUME_BOOST_ENABLED_KEY = booleanPreferencesKey("volume_boost_enabled")
        private val VOLUME_BOOST_AMOUNT_KEY = intPreferencesKey("volume_boost_amount")
        private val SPONSOR_BLOCK_ENABLED_KEY = booleanPreferencesKey("sponsor_block_enabled")
        private val SPONSOR_BLOCK_CATEGORIES_KEY = stringSetPreferencesKey("sponsor_block_categories_v2")

        private val LAST_FM_SESSION_KEY = stringPreferencesKey("last_fm_session_key")
        private val LAST_FM_USERNAME_KEY = stringPreferencesKey("last_fm_username")
        private val LAST_FM_SCROBBLING_ENABLED_KEY = booleanPreferencesKey("last_fm_scrobbling_enabled")
        private val LAST_FM_RECOMMENDATIONS_ENABLED_KEY = booleanPreferencesKey("last_fm_recommendations_enabled")
        private val LAST_FM_USE_NOW_PLAYING_KEY = booleanPreferencesKey("last_fm_use_now_playing")
        private val LAST_FM_SEND_LIKES_KEY = booleanPreferencesKey("last_fm_send_likes")
        
        private val SCROBBLE_DELAY_PERCENT_KEY = floatPreferencesKey("scrobble_delay_percent")
        private val SCROBBLE_MIN_DURATION_KEY = intPreferencesKey("scrobble_min_duration")
        private val SCROBBLE_DELAY_SECONDS_KEY = intPreferencesKey("scrobble_delay_seconds")
        private val UPDATE_CHANNEL_KEY = stringPreferencesKey("update_channel")
        private val PREFERRED_LANGUAGES_KEY = stringSetPreferencesKey("preferred_languages")
        private val YOUTUBE_HISTORY_SYNC_ENABLED_KEY = booleanPreferencesKey("youtube_history_sync_enabled")
        private val IGNORE_AUDIO_FOCUS_DURING_CALLS_KEY = booleanPreferencesKey("ignore_audio_focus_during_calls")
        
        private val BLUETOOTH_AUTOPLAY_ENABLED_KEY = booleanPreferencesKey("bluetooth_autoplay_enabled")
        private val SPEAK_SONG_DETAILS_ENABLED_KEY = booleanPreferencesKey("speak_song_details_enabled")
        
        private val DISCORD_RPC_ENABLED_KEY = booleanPreferencesKey("discord_rpc_enabled")
        private val DISCORD_TOKEN_KEY = stringPreferencesKey("discord_token")
    }
    
    // --- Developer Mode (Hidden) ---
    
    suspend fun isDeveloperMode(): Boolean = 
        context.dataStore.data.first()[DEV_MODE_KEY] == "unlocked"
    
    val developerModeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DEV_MODE_KEY] == "unlocked"
    }
    
    suspend fun enableDeveloperMode() {
        context.dataStore.edit { preferences ->
            preferences[DEV_MODE_KEY] = "unlocked"
        }
    }
    
    suspend fun disableDeveloperMode() {
        context.dataStore.edit { preferences ->
            preferences.remove(DEV_MODE_KEY)
        }
    }

    val sponsorBlockCategoriesFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[SPONSOR_BLOCK_CATEGORIES_KEY] ?: SponsorCategory.entries.map { it.key }.toSet()
    }

    suspend fun getEnabledSponsorCategories(): Set<String> {
        return context.dataStore.data.first()[SPONSOR_BLOCK_CATEGORIES_KEY]
            ?: SponsorCategory.entries.map { it.key }.toSet()
    }

    suspend fun toggleSponsorCategory(categoryKey: String, isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            val currentSet = preferences[SPONSOR_BLOCK_CATEGORIES_KEY]
                ?: SponsorCategory.entries.map { it.key }.toSet()

            val newSet = if (isEnabled) {
                currentSet + categoryKey
            } else {
                currentSet - categoryKey
            }
            preferences[SPONSOR_BLOCK_CATEGORIES_KEY] = newSet
        }
    }

    // --- Floating Player ---
    
    suspend fun isDynamicIslandEnabled(): Boolean = 
        context.dataStore.data.first()[DYNAMIC_ISLAND_ENABLED_KEY] ?: false

    suspend fun isSponsorBlockEnabled(): Boolean =
        context.dataStore.data.first()[SPONSOR_BLOCK_ENABLED_KEY] ?: true

    val sponsorBlockEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SPONSOR_BLOCK_ENABLED_KEY] ?: true
    }

    suspend fun setSponsorBlockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SPONSOR_BLOCK_ENABLED_KEY] = enabled
        }
    }
    
    // --- Last.fm ---

    suspend fun setLastFmSession(sessionKey: String, username: String) {
        encryptedPrefs.edit().putString("last_fm_session", sessionKey).apply()
        context.dataStore.edit { preferences ->
            preferences[LAST_FM_USERNAME_KEY] = username
        }
    }

    fun getLastFmSessionKey(): String? {
        return encryptedPrefs.getString("last_fm_session", null)
    }

    fun getLastFmUsername(): String? = runBlocking {
         context.dataStore.data.first()[LAST_FM_USERNAME_KEY]
    }

    fun clearLastFmSession() {
        encryptedPrefs.edit().remove("last_fm_session").apply()
        runBlocking {
            context.dataStore.edit { it.remove(LAST_FM_USERNAME_KEY) }
        }
    }

    val lastFmUsernameFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[LAST_FM_USERNAME_KEY]
    }

    suspend fun isLastFmScrobblingEnabled(): Boolean = 
        context.dataStore.data.first()[LAST_FM_SCROBBLING_ENABLED_KEY] ?: false

    val lastFmScrobblingEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[LAST_FM_SCROBBLING_ENABLED_KEY] ?: false
    }

    suspend fun setLastFmScrobblingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LAST_FM_SCROBBLING_ENABLED_KEY] = enabled
        }
    }

    suspend fun isLastFmRecommendationsEnabled(): Boolean = 
        context.dataStore.data.first()[LAST_FM_RECOMMENDATIONS_ENABLED_KEY] ?: true

    val lastFmRecommendationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[LAST_FM_RECOMMENDATIONS_ENABLED_KEY] ?: true
    }

    suspend fun setLastFmRecommendationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LAST_FM_RECOMMENDATIONS_ENABLED_KEY] = enabled
        }
    }

    suspend fun isLastFmUseNowPlayingEnabled(): Boolean = 
        context.dataStore.data.first()[LAST_FM_USE_NOW_PLAYING_KEY] ?: true

    val lastFmUseNowPlayingFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[LAST_FM_USE_NOW_PLAYING_KEY] ?: true
    }

    suspend fun setLastFmUseNowPlaying(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LAST_FM_USE_NOW_PLAYING_KEY] = enabled
        }
    }

    suspend fun isLastFmSendLikesEnabled(): Boolean = 
        context.dataStore.data.first()[LAST_FM_SEND_LIKES_KEY] ?: false
        
    val lastFmSendLikesFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[LAST_FM_SEND_LIKES_KEY] ?: false
    }

    suspend fun setLastFmSendLikes(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LAST_FM_SEND_LIKES_KEY] = enabled
        }
    }

    suspend fun getScrobbleDelayPercent(): Float = 
        context.dataStore.data.first()[SCROBBLE_DELAY_PERCENT_KEY] ?: 0.5f

    suspend fun setScrobbleDelayPercent(percent: Float) {
        context.dataStore.edit { preferences ->
            preferences[SCROBBLE_DELAY_PERCENT_KEY] = percent
        }
    }

    suspend fun getScrobbleMinDuration(): Int = 
        context.dataStore.data.first()[SCROBBLE_MIN_DURATION_KEY] ?: 30

    suspend fun setScrobbleMinDuration(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[SCROBBLE_MIN_DURATION_KEY] = seconds
        }
    }

    suspend fun getScrobbleDelaySeconds(): Int = 
        context.dataStore.data.first()[SCROBBLE_DELAY_SECONDS_KEY] ?: 180

    suspend fun setScrobbleDelaySeconds(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[SCROBBLE_DELAY_SECONDS_KEY] = seconds
        }
    }

    val dynamicIslandEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DYNAMIC_ISLAND_ENABLED_KEY] ?: false
    }
    
    suspend fun setDynamicIslandEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DYNAMIC_ISLAND_ENABLED_KEY] = enabled
        }
    }
    
    suspend fun getSeekbarStyle(): String = 
        context.dataStore.data.first()[SEEKBAR_STYLE_KEY] ?: "WAVEFORM"
    
    val seekbarStyleFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SEEKBAR_STYLE_KEY] ?: "WAVEFORM"
    }
    
    suspend fun setSeekbarStyle(style: String) {
        context.dataStore.edit { preferences ->
            preferences[SEEKBAR_STYLE_KEY] = style
        }
    }
    
    suspend fun getArtworkShape(): String = 
        context.dataStore.data.first()[ARTWORK_SHAPE_KEY] ?: "ROUNDED_SQUARE"
    
    val artworkShapeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[ARTWORK_SHAPE_KEY] ?: "ROUNDED_SQUARE"
    }
    
    suspend fun setArtworkShape(shape: String) {
        context.dataStore.edit { preferences ->
            preferences[ARTWORK_SHAPE_KEY] = shape
        }
    }
    
    suspend fun getArtworkSize(): String = 
        context.dataStore.data.first()[ARTWORK_SIZE_KEY] ?: "LARGE"
    
    val artworkSizeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[ARTWORK_SIZE_KEY] ?: "LARGE"
    }
    
    suspend fun setArtworkSize(size: String) {
        context.dataStore.edit { preferences ->
            preferences[ARTWORK_SIZE_KEY] = size
        }
    }
    
    suspend fun isEndlessQueueEnabled(): Boolean = 
        context.dataStore.data.first()[ENDLESS_QUEUE_ENABLED_KEY] ?: true
    
    val endlessQueueFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ENDLESS_QUEUE_ENABLED_KEY] ?: true
    }
    
    suspend fun setEndlessQueue(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENDLESS_QUEUE_ENABLED_KEY] = enabled
        }
    }
    
    suspend fun isOfflineModeEnabled(): Boolean = 
        context.dataStore.data.first()[OFFLINE_MODE_ENABLED_KEY] ?: false
    
    val offlineModeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[OFFLINE_MODE_ENABLED_KEY] ?: false
    }
    
    suspend fun setOfflineMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[OFFLINE_MODE_ENABLED_KEY] = enabled
        }
    }
    
    suspend fun isVolumeSliderEnabled(): Boolean = 
        context.dataStore.data.first()[VOLUME_SLIDER_ENABLED_KEY] ?: true
    
    val volumeSliderEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[VOLUME_SLIDER_ENABLED_KEY] ?: true
    }
    
    suspend fun setVolumeSliderEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VOLUME_SLIDER_ENABLED_KEY] = enabled
        }
    }

    suspend fun getMiniPlayerAlpha(): Float = 
        context.dataStore.data.first()[MINI_PLAYER_ALPHA_KEY] ?: 1f

    val miniPlayerAlphaFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[MINI_PLAYER_ALPHA_KEY] ?: 1f
    }

    suspend fun setMiniPlayerAlpha(alpha: Float) {
        context.dataStore.edit { preferences ->
            preferences[MINI_PLAYER_ALPHA_KEY] = alpha
        }
    }

    suspend fun getNavBarAlpha(): Float = 
        context.dataStore.data.first()[NAV_BAR_ALPHA_KEY] ?: 0.9f

    val navBarAlphaFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[NAV_BAR_ALPHA_KEY] ?: 0.9f
    }

    suspend fun setNavBarAlpha(alpha: Float) {
        context.dataStore.edit { preferences ->
            preferences[NAV_BAR_ALPHA_KEY] = alpha
        }
    }
    
    suspend fun getMiniPlayerStyle(): MiniPlayerStyle {
        val styleName = context.dataStore.data.first()[MINI_PLAYER_STYLE_KEY]
        return styleName?.let {
            try { MiniPlayerStyle.valueOf(it) } catch (e: Exception) { MiniPlayerStyle.STANDARD }
        } ?: MiniPlayerStyle.STANDARD
    }
    
    val miniPlayerStyleFlow: Flow<MiniPlayerStyle> = context.dataStore.data.map { preferences ->
        preferences[MINI_PLAYER_STYLE_KEY]?.let {
            try { MiniPlayerStyle.valueOf(it) } catch (e: Exception) { MiniPlayerStyle.STANDARD }
        } ?: MiniPlayerStyle.STANDARD
    }
    
    suspend fun setMiniPlayerStyle(style: MiniPlayerStyle) {
        context.dataStore.edit { preferences ->
            preferences[MINI_PLAYER_STYLE_KEY] = style.name
        }
    }
    
    suspend fun getDoubleTapSeekSeconds(): Int = 
        context.dataStore.data.first()[DOUBLE_TAP_SEEK_SECONDS_KEY] ?: 10
    
    val doubleTapSeekSecondsFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[DOUBLE_TAP_SEEK_SECONDS_KEY] ?: 10
    }
    
    suspend fun setDoubleTapSeekSeconds(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[DOUBLE_TAP_SEEK_SECONDS_KEY] = seconds
        }
    }

    suspend fun getVideoQuality(): VideoQuality {
        val qualityName = context.dataStore.data.first()[VIDEO_QUALITY_KEY]
        return qualityName?.let {
            try { VideoQuality.valueOf(it) } catch (e: Exception) { VideoQuality.MEDIUM }
        } ?: VideoQuality.MEDIUM
    }

    val videoQualityFlow: Flow<VideoQuality> = context.dataStore.data.map { preferences ->
        preferences[VIDEO_QUALITY_KEY]?.let {
            try { VideoQuality.valueOf(it) } catch (e: Exception) { VideoQuality.MEDIUM }
        } ?: VideoQuality.MEDIUM
    }

    suspend fun setVideoQuality(quality: VideoQuality) {
        context.dataStore.edit { preferences ->
            preferences[VIDEO_QUALITY_KEY] = quality.name
        }
    }
    
    suspend fun doesEnableBetterLyrics(): Boolean = 
            context.dataStore.data.first()[ENABLE_BETTER_LYRICS_KEY] ?: true
    
    val enableBetterLyricsFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ENABLE_BETTER_LYRICS_KEY] ?: true
    }
    
    suspend fun setEnableBetterLyrics(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENABLE_BETTER_LYRICS_KEY] = enabled
        }
    }
    
    suspend fun doesEnableSimpMusic(): Boolean = 
            context.dataStore.data.first()[ENABLE_SIMPMUSIC_KEY] ?: true
    
    val enableSimpMusicFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ENABLE_SIMPMUSIC_KEY] ?: true
    }
    
    suspend fun setEnableSimpMusic(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENABLE_SIMPMUSIC_KEY] = enabled
        }
    }
    
    suspend fun doesEnableKuGou(): Boolean = 
            context.dataStore.data.first()[ENABLE_KUGOU_KEY] ?: true
    
    val enableKuGouFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ENABLE_KUGOU_KEY] ?: true
    }
    
    suspend fun setEnableKuGou(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENABLE_KUGOU_KEY] = enabled
        }
    }
    
    suspend fun getPreferredLyricsProvider(): String = 
        context.dataStore.data.first()[PREFERRED_LYRICS_PROVIDER_KEY] ?: "BetterLyrics"
    
    val preferredLyricsProviderFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PREFERRED_LYRICS_PROVIDER_KEY] ?: "BetterLyrics"
    }
    
    suspend fun setPreferredLyricsProvider(provider: String) {
        context.dataStore.edit { preferences ->
            preferences[PREFERRED_LYRICS_PROVIDER_KEY] = provider
        }
    }
    suspend fun getLyricsTextPosition(): LyricsTextPosition {
        return context.dataStore.data.map { preferences ->
            preferences[LYRICS_TEXT_POSITION_KEY]?.let {
                try { LyricsTextPosition.valueOf(it) } catch (e: Exception) { LyricsTextPosition.CENTER }
            } ?: LyricsTextPosition.CENTER
        }.first()
    }
    
    val lyricsTextPositionFlow: Flow<LyricsTextPosition> = context.dataStore.data.map { preferences ->
        preferences[LYRICS_TEXT_POSITION_KEY]?.let {
            try { LyricsTextPosition.valueOf(it) } catch (e: Exception) { LyricsTextPosition.CENTER }
        } ?: LyricsTextPosition.CENTER
    }
    suspend fun setLyricsTextPosition(position: LyricsTextPosition) {
        context.dataStore.edit { preferences ->
            preferences[LYRICS_TEXT_POSITION_KEY] = position.name
        }
    }
    suspend fun getLyricsAnimationType(): LyricsAnimationType {
        return context.dataStore.data.map { preferences ->
            preferences[LYRICS_ANIMATION_TYPE_KEY]?.let {
                try { LyricsAnimationType.valueOf(it) } catch (e: Exception) { LyricsAnimationType.WORD }
            } ?: LyricsAnimationType.WORD
        }.first()
    }
    
    val lyricsAnimationTypeFlow: Flow<LyricsAnimationType> = context.dataStore.data.map { preferences ->
        preferences[LYRICS_ANIMATION_TYPE_KEY]?.let {
            try { LyricsAnimationType.valueOf(it) } catch (e: Exception) { LyricsAnimationType.WORD }
        } ?: LyricsAnimationType.WORD
    }
    suspend fun setLyricsAnimationType(type: LyricsAnimationType) {
        context.dataStore.edit { preferences ->
            preferences[LYRICS_ANIMATION_TYPE_KEY] = type.name
        }
    }

    suspend fun getPlayerCacheLimit(): Long = 
        context.dataStore.data.first()[PLAYER_CACHE_LIMIT_KEY] ?: -1L

    val playerCacheLimitFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[PLAYER_CACHE_LIMIT_KEY] ?: -1L
    }

    suspend fun setPlayerCacheLimit(limitBytes: Long) {
        context.dataStore.edit { preferences ->
            preferences[PLAYER_CACHE_LIMIT_KEY] = limitBytes
        }
    }
    
    suspend fun getPlayerCacheAutoClearInterval(): Int = 
        context.dataStore.data.first()[PLAYER_CACHE_AUTO_CLEAR_INTERVAL_KEY] ?: 5

    val playerCacheAutoClearIntervalFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PLAYER_CACHE_AUTO_CLEAR_INTERVAL_KEY] ?: 5
    }

    suspend fun setPlayerCacheAutoClearInterval(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[PLAYER_CACHE_AUTO_CLEAR_INTERVAL_KEY] = days
        }
    }

    suspend fun getLastCacheClearedTimestamp(): Long = 
        context.dataStore.data.first()[PLAYER_CACHE_LAST_CLEARED_TIMESTAMP_KEY] ?: 0L

    suspend fun updateLastCacheClearedTimestamp() {
        context.dataStore.edit { preferences ->
            preferences[PLAYER_CACHE_LAST_CLEARED_TIMESTAMP_KEY] = System.currentTimeMillis()
        }
    }
    
    suspend fun isMusicHapticsEnabled(): Boolean = 
        context.dataStore.data.first()[MUSIC_HAPTICS_ENABLED_KEY] ?: false
    
    val musicHapticsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[MUSIC_HAPTICS_ENABLED_KEY] ?: false
    }
    
    suspend fun setMusicHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MUSIC_HAPTICS_ENABLED_KEY] = enabled
        }
    }
    
    suspend fun getHapticsMode(): HapticsMode {
        val modeName = context.dataStore.data.first()[HAPTICS_MODE_KEY]
        return modeName?.let {
            try { HapticsMode.valueOf(it) } catch (e: Exception) { HapticsMode.BASIC }
        } ?: HapticsMode.BASIC
    }
    
    val hapticsModeFlow: Flow<HapticsMode> = context.dataStore.data.map { preferences ->
        preferences[HAPTICS_MODE_KEY]?.let {
            try { HapticsMode.valueOf(it) } catch (e: Exception) { HapticsMode.BASIC }
        } ?: HapticsMode.BASIC
    }
    
    suspend fun setHapticsMode(mode: HapticsMode) {
        context.dataStore.edit { preferences ->
            preferences[HAPTICS_MODE_KEY] = mode.name
        }
    }
    
    suspend fun getHapticsIntensity(): HapticsIntensity {
        val intensityName = context.dataStore.data.first()[HAPTICS_INTENSITY_KEY]
        return intensityName?.let {
            try { HapticsIntensity.valueOf(it) } catch (e: Exception) { HapticsIntensity.MEDIUM }
        } ?: HapticsIntensity.MEDIUM
    }
    
    val hapticsIntensityFlow: Flow<HapticsIntensity> = context.dataStore.data.map { preferences ->
        preferences[HAPTICS_INTENSITY_KEY]?.let {
            try { HapticsIntensity.valueOf(it) } catch (e: Exception) { HapticsIntensity.MEDIUM }
        } ?: HapticsIntensity.MEDIUM
    }
    
    suspend fun setHapticsIntensity(intensity: HapticsIntensity) {
        context.dataStore.edit { preferences ->
            preferences[HAPTICS_INTENSITY_KEY] = intensity.name
        }
    }

    suspend fun isStopMusicOnTaskClearEnabled(): Boolean = 
        context.dataStore.data.first()[STOP_MUSIC_ON_TASK_CLEAR_KEY] ?: false

    val stopMusicOnTaskClearEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[STOP_MUSIC_ON_TASK_CLEAR_KEY] ?: false
    }

    suspend fun setStopMusicOnTaskClearEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[STOP_MUSIC_ON_TASK_CLEAR_KEY] = enabled
        }
    }

    suspend fun isPauseMusicOnMediaMutedEnabled(): Boolean = 
        context.dataStore.data.first()[PAUSE_MUSIC_ON_MEDIA_MUTED_KEY] ?: false

    val pauseMusicOnMediaMutedEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PAUSE_MUSIC_ON_MEDIA_MUTED_KEY] ?: false
    }

    suspend fun setPauseMusicOnMediaMutedEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PAUSE_MUSIC_ON_MEDIA_MUTED_KEY] = enabled
        }
    }

    suspend fun isKeepScreenOnEnabled(): Boolean =
        context.dataStore.data.first()[KEEP_SCREEN_ON_KEY] ?: false

    val keepScreenOnEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEEP_SCREEN_ON_KEY] ?: false
    }

    suspend fun setKeepScreenOnEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEEP_SCREEN_ON_KEY] = enabled
        }
    }

    suspend fun isPureBlackEnabled(): Boolean = 
        context.dataStore.data.first()[PURE_BLACK_KEY] ?: false

    val pureBlackEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PURE_BLACK_KEY] ?: false
    }

    suspend fun setPureBlackEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PURE_BLACK_KEY] = enabled
        }
    }

    // --- Content Preferences ---

    suspend fun getPreferredLanguages(): Set<String> =
        context.dataStore.data.first()[PREFERRED_LANGUAGES_KEY] ?: emptySet()

    val preferredLanguagesFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[PREFERRED_LANGUAGES_KEY] ?: emptySet()
    }

    suspend fun setPreferredLanguages(languages: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[PREFERRED_LANGUAGES_KEY] = languages
        }
    }
    
    // --- History Sync ---

    suspend fun isYouTubeHistorySyncEnabled(): Boolean = 
        context.dataStore.data.first()[YOUTUBE_HISTORY_SYNC_ENABLED_KEY] ?: false

    val youtubeHistorySyncEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[YOUTUBE_HISTORY_SYNC_ENABLED_KEY] ?: false
    }

    suspend fun setYouTubeHistorySyncEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[YOUTUBE_HISTORY_SYNC_ENABLED_KEY] = enabled
        }
    }

    suspend fun isIgnoreAudioFocusDuringCallsEnabled(): Boolean =
        context.dataStore.data.first()[IGNORE_AUDIO_FOCUS_DURING_CALLS_KEY] ?: false

    val ignoreAudioFocusDuringCallsFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IGNORE_AUDIO_FOCUS_DURING_CALLS_KEY] ?: false
    }

    suspend fun setIgnoreAudioFocusDuringCallsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IGNORE_AUDIO_FOCUS_DURING_CALLS_KEY] = enabled
        }
    }

    // --- Bluetooth & Hands-Free ---

    suspend fun isBluetoothAutoplayEnabled(): Boolean =
        context.dataStore.data.first()[BLUETOOTH_AUTOPLAY_ENABLED_KEY] ?: false

    val bluetoothAutoplayEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BLUETOOTH_AUTOPLAY_ENABLED_KEY] ?: false
    }

    suspend fun setBluetoothAutoplayEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BLUETOOTH_AUTOPLAY_ENABLED_KEY] = enabled
        }
    }

    suspend fun isSpeakSongDetailsEnabled(): Boolean =
        context.dataStore.data.first()[SPEAK_SONG_DETAILS_ENABLED_KEY] ?: false

    val speakSongDetailsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SPEAK_SONG_DETAILS_ENABLED_KEY] ?: false
    }

    suspend fun setSpeakSongDetailsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SPEAK_SONG_DETAILS_ENABLED_KEY] = enabled
        }
    }

    // --- Discord RPC ---

    suspend fun isDiscordRpcEnabled(): Boolean =
        context.dataStore.data.first()[DISCORD_RPC_ENABLED_KEY] ?: false

    val discordRpcEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DISCORD_RPC_ENABLED_KEY] ?: false
    }

    suspend fun setDiscordRpcEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DISCORD_RPC_ENABLED_KEY] = enabled
        }
    }

    suspend fun getDiscordToken(): String =
        encryptedPrefs.getString("discord_token_enc", null) ?: ""

    val discordTokenFlow: Flow<String> = context.dataStore.data.map { 
        getDiscordToken() // This might not be reactive for encrypted prefs change, but usually fine
    }

    suspend fun setDiscordToken(token: String) {
        encryptedPrefs.edit().putString("discord_token_enc", token).apply()
        // We trigger a datastore update to notify listeners, even if we store in encrypted prefs
        context.dataStore.edit { preferences ->
            preferences[DISCORD_TOKEN_KEY] = "stored" 
        }
    }
    
    // --- User Accounts ---
    
    data class StoredAccount(
        val name: String,
        val email: String,
        val avatarUrl: String,
        val cookies: String
    )
    
    private val SAVED_ACCOUNTS_KEY = "saved_accounts"
    
    fun getStoredAccounts(): List<StoredAccount> {
        val json = encryptedPrefs.getString(SAVED_ACCOUNTS_KEY, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                StoredAccount(
                    name = obj.optString("name"),
                    email = obj.optString("email"),
                    avatarUrl = obj.optString("avatarUrl"),
                    cookies = obj.optString("cookies")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun saveStoredAccounts(accounts: List<StoredAccount>) {
        val array = JSONArray()
        accounts.forEach { account ->
            array.put(JSONObject().apply {
                put("name", account.name)
                put("email", account.email)
                put("avatarUrl", account.avatarUrl)
                put("cookies", account.cookies)
            })
        }
        encryptedPrefs.edit().putString(SAVED_ACCOUNTS_KEY, array.toString()).apply()
    }
    
    suspend fun saveCurrentAccountToHistory(name: String, email: String, avatarUrl: String) {
        val currentCookies = getCookies() ?: return
        val newAccount = StoredAccount(name, email, avatarUrl, currentCookies)
        
        val accounts = getStoredAccounts().toMutableList()
        accounts.removeAll { it.email == email }
        accounts.add(0, newAccount)
        
        saveStoredAccounts(accounts)
        saveUserAvatar(avatarUrl)
    }
    
    suspend fun switchAccount(account: StoredAccount) {
        encryptedPrefs.edit().putString("cookies", account.cookies).apply()
        saveUserAvatar(account.avatarUrl)
        saveCurrentAccountToHistory(account.name, account.email, account.avatarUrl)
    }
    
    fun removeAccount(email: String) {
        val accounts = getStoredAccounts().toMutableList()
        accounts.removeAll { it.email == email }
        saveStoredAccounts(accounts)
    }
    
    private val migrationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        migrationScope.launch {
            migrateCookies()
        }
    }

    private suspend fun migrateCookies() {
        val oldCookies = context.dataStore.data.first()[COOKIES_KEY]
        if (oldCookies != null) {
            encryptedPrefs.edit().putString("cookies", oldCookies).apply()
            context.dataStore.edit { it.remove(COOKIES_KEY) }
        }
    }
    
    fun getCookies(): String? {
        return encryptedPrefs.getString("cookies", null)
    }
    
    suspend fun saveCookies(cookies: String) {
        encryptedPrefs.edit().putString("cookies", cookies).apply()
        context.dataStore.edit { it.remove(COOKIES_KEY) }
    }
    
    suspend fun clearCookies() {
        encryptedPrefs.edit().remove("cookies").apply()
        context.dataStore.edit { preferences ->
            preferences.remove(COOKIES_KEY)
        }
    }
    
    fun isLoggedIn(): Boolean = !getCookies().isNullOrBlank()
    
    suspend fun getUserAvatar(): String? = 
        context.dataStore.data.first()[USER_AVATAR_KEY]
    
    suspend fun saveUserAvatar(url: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_AVATAR_KEY] = url
        }
    }

    val userAvatarFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_AVATAR_KEY]
    }
    
    suspend fun getAudioQuality(): AudioQuality {
        val qualityName = context.dataStore.data.first()[AUDIO_QUALITY_KEY]
        return qualityName?.let { 
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
    
    suspend fun isGaplessPlaybackEnabled(): Boolean = 
        context.dataStore.data.first()[GAPLESS_PLAYBACK_KEY] ?: true
    
    suspend fun setGaplessPlayback(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[GAPLESS_PLAYBACK_KEY] = enabled
        }
    }
    
    suspend fun isAutomixEnabled(): Boolean = 
        context.dataStore.data.first()[AUTOMIX_KEY] ?: true
    
    suspend fun setAutomix(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTOMIX_KEY] = enabled
        }
    }

    suspend fun isVolumeNormalizationEnabled(): Boolean = 
        context.dataStore.data.first()[VOLUME_NORMALIZATION_ENABLED_KEY] ?: true

    val volumeNormalizationEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[VOLUME_NORMALIZATION_ENABLED_KEY] ?: true
    }

    suspend fun setVolumeNormalizationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VOLUME_NORMALIZATION_ENABLED_KEY] = enabled
        }
    }
    
    suspend fun getDownloadQuality(): DownloadQuality {
        val qualityName = context.dataStore.data.first()[DOWNLOAD_QUALITY_KEY]
        return qualityName?.let { 
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

    suspend fun isAudioOffloadEnabled(): Boolean = 
        context.dataStore.data.first()[AUDIO_OFFLOAD_ENABLED_KEY] ?: false

    val audioOffloadEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUDIO_OFFLOAD_ENABLED_KEY] ?: false
    }

    suspend fun setAudioOffloadEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUDIO_OFFLOAD_ENABLED_KEY] = enabled
        }
    }

    suspend fun isVolumeBoostEnabled(): Boolean = 
        context.dataStore.data.first()[VOLUME_BOOST_ENABLED_KEY] ?: false

    val volumeBoostEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[VOLUME_BOOST_ENABLED_KEY] ?: false
    }

    suspend fun setVolumeBoostEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VOLUME_BOOST_ENABLED_KEY] = enabled
        }
    }

    suspend fun getVolumeBoostAmount(): Int = 
        context.dataStore.data.first()[VOLUME_BOOST_AMOUNT_KEY] ?: 0

    val volumeBoostAmountFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[VOLUME_BOOST_AMOUNT_KEY] ?: 0
    }

    suspend fun setVolumeBoostAmount(amount: Int) {
        context.dataStore.edit { preferences ->
            preferences[VOLUME_BOOST_AMOUNT_KEY] = amount
        }
    }

    suspend fun isOnboardingCompleted(): Boolean = 
        context.dataStore.data.first()[ONBOARDING_COMPLETED_KEY] ?: false
        
    val onboardingCompletedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETED_KEY] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] = completed
        }
    }
    
    suspend fun getThemeMode(): ThemeMode {
        val modeName = context.dataStore.data.first()[THEME_MODE_KEY]
        return modeName?.let { 
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

    suspend fun getAppTheme(): AppTheme {
        val themeName = context.dataStore.data.first()[APP_THEME_KEY]
        return themeName?.let {
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
    
    suspend fun isDynamicColorEnabled(): Boolean = 
        context.dataStore.data.first()[DYNAMIC_COLOR_KEY] ?: true
    
    val dynamicColorFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DYNAMIC_COLOR_KEY] ?: true
    }
    
    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR_KEY] = enabled
        }
    }
    
    suspend fun getMusicSource(): MusicSource {
        val sourceName = context.dataStore.data.first()[MUSIC_SOURCE_KEY]
        return sourceName?.let { 
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
    
    suspend fun savePlaybackState(songId: String, position: Long, queueJson: String, index: Int) {
        context.dataStore.edit { preferences ->
            preferences[LAST_SONG_ID_KEY] = songId
            preferences[LAST_POSITION_KEY] = position
            preferences[LAST_QUEUE_KEY] = queueJson
            preferences[LAST_INDEX_KEY] = index
        }
    }
    
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
    
    suspend fun clearPlaybackState() {
        context.dataStore.edit { preferences ->
            preferences.remove(LAST_SONG_ID_KEY)
            preferences.remove(LAST_POSITION_KEY)
            preferences.remove(LAST_QUEUE_KEY)
            preferences.remove(LAST_INDEX_KEY)
        }
    }
    
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
    
    suspend fun addRecentSearch(song: Song) {
        val currentSearches = getRecentSearches().toMutableList()
        currentSearches.removeAll { it.id == song.id }
        currentSearches.add(0, song)
        val trimmed = currentSearches.take(MAX_RECENT_SEARCHES)
        
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
    
    suspend fun clearRecentSearches() {
        context.dataStore.edit { preferences ->
            preferences.remove(RECENT_SEARCHES_KEY)
        }
    }
    
    val recentlyPlayedFlow: Flow<List<RecentlyPlayed>> = context.dataStore.data.map { preferences ->
        parseRecentlyPlayed(preferences[RECENTLY_PLAYED_KEY])
    }
    
    suspend fun addToRecentlyPlayed(song: Song) {
        context.dataStore.edit { preferences ->
            val existing = parseRecentlyPlayed(preferences[RECENTLY_PLAYED_KEY]).toMutableList()
            existing.removeAll { it.song.id == song.id }
            existing.add(0, RecentlyPlayed(song, System.currentTimeMillis()))
            val limited = existing.take(MAX_RECENTLY_PLAYED)
            preferences[RECENTLY_PLAYED_KEY] = serializeRecentlyPlayed(limited)
        }
    }
    
    suspend fun clearRecentlyPlayed() {
        context.dataStore.edit { preferences ->
            preferences.remove(RECENTLY_PLAYED_KEY)
        }
    }
    
    private fun parseRecentlyPlayed(json: String?): List<RecentlyPlayed> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                val songObj = obj.optJSONObject("song") ?: return@mapNotNull null
                val playedAt = obj.optLong("playedAt", System.currentTimeMillis())
                
                val song = jsonToSong(songObj) ?: return@mapNotNull null
                RecentlyPlayed(song, playedAt)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun serializeRecentlyPlayed(list: List<RecentlyPlayed>): String {
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
    
    suspend fun saveHomeCache(sections: List<HomeSection>) {
        val json = withContext(Dispatchers.Default) {
            serializeHomeSections(sections)
        }
        context.dataStore.edit { preferences ->
            preferences[HOME_CACHE_KEY] = json
        }
    }
    
    fun getCachedHomeSections(): Flow<List<HomeSection>> = context.dataStore.data
        .map { preferences -> preferences[HOME_CACHE_KEY] }
        .flowOn(Dispatchers.IO)
        .map { json -> 
            withContext(Dispatchers.Default) {
                parseHomeSections(json)
            }
        }
    
    suspend fun getCachedHomeSectionsSync(): List<HomeSection> = withContext(Dispatchers.IO) {
        val prefs = context.dataStore.data.first()
        val json = prefs[HOME_CACHE_KEY]
        withContext(Dispatchers.Default) {
            parseHomeSections(json)
        }
    }
    
    suspend fun saveJioSaavnHomeCache(sections: List<HomeSection>) {
        val json = withContext(Dispatchers.Default) {
            serializeHomeSections(sections)
        }
        context.dataStore.edit { preferences ->
            preferences[JIOSAAVN_HOME_CACHE_KEY] = json
        }
    }
    
    fun getCachedJioSaavnHomeSections(): Flow<List<HomeSection>> = context.dataStore.data
        .map { preferences -> preferences[JIOSAAVN_HOME_CACHE_KEY] }
        .flowOn(Dispatchers.IO)
        .map { json ->
            withContext(Dispatchers.Default) {
                parseHomeSections(json)
            }
        }
    
    suspend fun getCachedJioSaavnHomeSectionsSync(): List<HomeSection> = withContext(Dispatchers.IO) {
        val prefs = context.dataStore.data.first()
        val json = prefs[JIOSAAVN_HOME_CACHE_KEY]
        withContext(Dispatchers.Default) {
            parseHomeSections(json)
        }
    }

    suspend fun getLastHomeFetchTime(source: MusicSource): Long {
        val key = if (source == MusicSource.JIOSAAVN) LAST_FETCH_TIME_JIOSAAVN_KEY else LAST_FETCH_TIME_YOUTUBE_KEY
        return context.dataStore.data.first()[key] ?: 0L
    }

    suspend fun updateLastHomeFetchTime(source: MusicSource) {
        val key = if (source == MusicSource.JIOSAAVN) LAST_FETCH_TIME_JIOSAAVN_KEY else LAST_FETCH_TIME_YOUTUBE_KEY
        context.dataStore.edit { preferences ->
            preferences[key] = System.currentTimeMillis()
        }
    }
    
    suspend fun saveLibraryPlaylistsCache(playlists: List<PlaylistDisplayItem>) {
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
    
    suspend fun getCachedLibraryPlaylistsSync(): List<PlaylistDisplayItem> = withContext(Dispatchers.IO) {
        val prefs = context.dataStore.data.first()
        val json = prefs[LIBRARY_PLAYLISTS_CACHE_KEY] ?: return@withContext emptyList()
        withContext(Dispatchers.Default) {
            try {
                val array = JSONArray(json)
                (0 until array.length()).mapNotNull { i ->
                    val obj = array.optJSONObject(i) ?: return@mapNotNull null
                    PlaylistDisplayItem(
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
    
    private fun parseHomeSections(json: String?): List<HomeSection> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                val title = obj.optString("title")
                val typeStr = obj.optString("type")
                val type = try {
                    if (typeStr.isNotEmpty()) HomeSectionType.valueOf(typeStr)
                    else HomeSectionType.HorizontalCarousel
                } catch (e: Exception) {
                    HomeSectionType.HorizontalCarousel
                }
                
                val itemsArray = obj.optJSONArray("items") ?: JSONArray()
                val items = (0 until itemsArray.length()).mapNotNull { j ->
                    parseHomeItem(itemsArray.optJSONObject(j))
                }
                
                HomeSection(title, items, type)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun serializeHomeSections(sections: List<HomeSection>): String {
        val array = JSONArray()
        sections.forEach { section ->
            val obj = JSONObject().apply {
                put("title", section.title)
                put("type", section.type.name)
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
    
    private fun parseHomeItem(obj: JSONObject?): HomeItem? {
        if (obj == null) return null
        val type = obj.optString("type")
        val data = obj.optJSONObject("data") ?: return null
        
        return when (type) {
            "song" -> {
                val song = jsonToSong(data) ?: return null
                HomeItem.SongItem(song)
            }
            "playlist" -> {
                val playlist = PlaylistDisplayItem(
                    id = data.optString("id"),
                    name = data.optString("name"),
                    url = data.optString("url"),
                    uploaderName = data.optString("uploaderName"),
                    thumbnailUrl = data.optString("thumbnailUrl").takeIf { it.isNotBlank() },
                    songCount = data.optInt("songCount", 0)
                )
                HomeItem.PlaylistItem(playlist)
            }
            "album" -> {
                val album = Album(
                    id = data.optString("id"),
                    title = data.optString("title"),
                    artist = data.optString("artist"),
                    year = data.optString("year").takeIf { it.isNotBlank() },
                    thumbnailUrl = data.optString("thumbnailUrl").takeIf { it.isNotBlank() },
                    description = data.optString("description").takeIf { it.isNotBlank() }
                )
                HomeItem.AlbumItem(album)
            }
            "artist" -> {
                val artist = Artist(
                    id = data.optString("id"),
                    name = data.optString("name"),
                    thumbnailUrl = data.optString("thumbnailUrl").takeIf { it.isNotBlank() },
                    description = data.optString("description").takeIf { it.isNotBlank() },
                    subscribers = data.optString("subscribers").takeIf { it.isNotBlank() }
                )
                HomeItem.ArtistItem(artist)
            }
            "explore" -> {
                HomeItem.ExploreItem(
                    title = data.optString("title"),
                    iconRes = data.optInt("iconRes"),
                    browseId = data.optString("browseId", "")
                )
            }
            else -> null
        }
    }
    
    private fun serializeHomeItem(item: HomeItem): JSONObject {
        val obj = JSONObject()
        when (item) {
            is HomeItem.SongItem -> {
                obj.put("type", "song")
                obj.put("data", songToJson(item.song))
            }
            is HomeItem.PlaylistItem -> {
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
            is HomeItem.AlbumItem -> {
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
            is HomeItem.ArtistItem -> {
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
            is HomeItem.ExploreItem -> {
                obj.put("type", "explore")
                val data = JSONObject().apply {
                    put("title", item.title)
                    put("iconRes", item.iconRes)
                    put("browseId", item.browseId)
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
                localUri = songObj.optString("localUri").takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
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
    suspend fun getUpdateChannel(): UpdateChannel {
        val channelName = context.dataStore.data.first()[UPDATE_CHANNEL_KEY]
        return channelName?.let {
            try { UpdateChannel.valueOf(it) } catch (e: Exception) { UpdateChannel.STABLE }
        } ?: UpdateChannel.STABLE
    }

    val updateChannelFlow: Flow<UpdateChannel> = context.dataStore.data.map { preferences ->
        preferences[UPDATE_CHANNEL_KEY]?.let {
            try { UpdateChannel.valueOf(it) } catch (e: Exception) { UpdateChannel.STABLE }
        } ?: UpdateChannel.STABLE
    }

    suspend fun setUpdateChannel(channel: UpdateChannel) {
        context.dataStore.edit { preferences ->
            preferences[UPDATE_CHANNEL_KEY] = channel.name
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
