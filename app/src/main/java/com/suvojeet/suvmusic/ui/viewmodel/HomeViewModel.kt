package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.repository.JioSaavnRepository
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.recommendation.RecommendationEngine
import com.suvojeet.suvmusic.data.MusicSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val homeSections: List<com.suvojeet.suvmusic.data.model.HomeSection> = emptyList(),
    val recommendations: List<Song> = emptyList(),
    val userAvatarUrl: String? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val currentSource: MusicSource = MusicSource.YOUTUBE
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
    private val jioSaavnRepository: JioSaavnRepository,
    private val sessionManager: SessionManager,
    private val recommendationEngine: RecommendationEngine
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadData()
        observeSession()
        observeMusicSource()
        loadRecommendations()
    }
    
    private fun observeSession() {
        viewModelScope.launch {
            sessionManager.userAvatarFlow.collect { avatarUrl ->
                _uiState.update { it.copy(userAvatarUrl = avatarUrl) }
            }
        }
    }
    
    private fun observeMusicSource() {
        viewModelScope.launch {
            sessionManager.musicSourceFlow.collect { source ->
                val currentSource = _uiState.value.currentSource
                if (source != currentSource) {
                    _uiState.update { it.copy(currentSource = source) }
                    loadData() // Reload when source changes
                }
            }
        }
    }
    
    private fun loadData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val source = sessionManager.getMusicSource()
            _uiState.update { it.copy(currentSource = source) }
            
            // 1. Load cache immediately based on source
            val cachedSections = if (source == MusicSource.JIOSAAVN) {
                sessionManager.getCachedJioSaavnHomeSectionsSync()
            } else {
                sessionManager.getCachedHomeSectionsSync()
            }
            
            // Display cache if available
            if (cachedSections.isNotEmpty()) {
                _uiState.update { 
                    it.copy(
                        homeSections = cachedSections, 
                        isLoading = false,
                        isRefreshing = false, // Not explicitly refreshing unless we decide to fetch
                        error = null
                    ) 
                }
            }
            
            // 2. Determine if we need to fetch fresh data
            val lastFetchTime = sessionManager.getLastHomeFetchTime(source)
            val currentTime = System.currentTimeMillis()
            val timeSinceLastFetch = currentTime - lastFetchTime
            val cacheExpired = timeSinceLastFetch > 30 * 60 * 1000 // 30 minutes
            
            val shouldFetch = forceRefresh || cachedSections.isEmpty() || cacheExpired
            
            // Show loading indicators
            if (shouldFetch) {
                 if (cachedSections.isNotEmpty()) {
                     _uiState.update { it.copy(isRefreshing = true) }
                 } else {
                     _uiState.update { it.copy(isLoading = true, error = null) }
                 }
            } else {
                // No need to fetch, we are done
                return@launch
            }

            try {
                // 3. Fetch fresh data based on source
                val sections = when (source) {
                    MusicSource.JIOSAAVN -> jioSaavnRepository.getHomeSections()
                    else -> youTubeRepository.getHomeSections()
                }
                
                // 4. Update cache and UI
                if (sections.isNotEmpty()) {
                    if (source == MusicSource.JIOSAAVN) {
                        sessionManager.saveJioSaavnHomeCache(sections)
                    } else {
                        sessionManager.saveHomeCache(sections)
                    }
                    
                    // Update timestamp on successful fetch
                    sessionManager.updateLastHomeFetchTime(source)
                    
                    _uiState.update { 
                        it.copy(
                            homeSections = sections,
                            isLoading = false,
                            isRefreshing = false,
                            error = null
                        )
                    }
                } else if (cachedSections.isEmpty()) {
                     // Only show error if both cache and fresh fetch are empty
                     throw Exception("No content available")
                } else {
                    // Fetch failed but we have cache, just stop refreshing
                    _uiState.update { it.copy(isRefreshing = false) }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = if (it.homeSections.isEmpty()) e.message ?: "Failed to load content" else null,
                        isLoading = false,
                        isRefreshing = false
                    )
                }
            }
        }
    }
    
    fun refresh() {
        loadData(forceRefresh = true)
        loadRecommendations()
    }
    
    private fun loadRecommendations() {
        viewModelScope.launch {
            try {
                // Only load recommendations if user has listening history
                val recommendations = recommendationEngine.getPersonalizedRecommendations(20)
                _uiState.update { it.copy(recommendations = recommendations) }
            } catch (e: Exception) {
                // Silently fail - recommendations are optional
                _uiState.update { it.copy(recommendations = emptyList()) }
            }
        }
    }
}
