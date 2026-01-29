package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.model.HomeSection
import com.suvojeet.suvmusic.data.model.HomeItem
import com.suvojeet.suvmusic.data.model.HomeSectionType
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
    val homeSections: List<HomeSection> = emptyList(),
    val recommendations: List<Song> = emptyList(),
    val userAvatarUrl: String? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val currentSource: MusicSource = MusicSource.YOUTUBE,
    val selectedMood: String? = null
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

    fun onMoodSelected(mood: String) {
        val currentMood = _uiState.value.selectedMood
        if (currentMood == mood) {
            // Deselect and reload default home
            _uiState.update { it.copy(selectedMood = null) }
            loadData(forceRefresh = true) 
        } else {
            // Select new mood
            _uiState.update { it.copy(selectedMood = mood) }
            fetchMoodContent(mood)
        }
    }

    private fun fetchMoodContent(mood: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Determine source (JioSaavn usually doesn't have this granular mood flow implemented yet, but fallback to search works)
                val sections = if (_uiState.value.currentSource == MusicSource.JIOSAAVN) {
                     // Simple fallback for JioSaavn
                     val songs = jioSaavnRepository.search(mood)
                     listOf(
                         HomeSection(
                             title = "$mood Music",
                             items = songs.map { HomeItem.SongItem(it) },
                             type = HomeSectionType.VerticalList
                         )
                     )
                } else {
                    youTubeRepository.getHomeSectionsForMood(mood)
                }

                _uiState.update { 
                    it.copy(
                        homeSections = sections,
                        isLoading = false,
                        isRefreshing = false,
                        error = if (sections.isEmpty()) "No content found for $mood" else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = e.message
                    ) 
                }
            }
        }
    }
    
    private fun loadData(forceRefresh: Boolean = false) {
        // If a mood is selected, ignore standard loadData (unless we want to support refresh for mood)
        // But logic in onMoodSelected handles deselecting.
        if (_uiState.value.selectedMood != null && !forceRefresh) return

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
