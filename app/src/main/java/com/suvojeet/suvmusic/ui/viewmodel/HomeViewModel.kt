package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.model.PlaylistDisplayItem
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import com.suvojeet.suvmusic.data.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val homeSections: List<com.suvojeet.suvmusic.data.model.HomeSection> = emptyList(),
    val userAvatarUrl: String? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadData()
        observeSession()
    }
    
    private fun observeSession() {
        viewModelScope.launch {
            sessionManager.userAvatarFlow.collect { avatarUrl ->
                _uiState.update { it.copy(userAvatarUrl = avatarUrl) }
            }
        }
    }
    
    private fun loadData() {
        viewModelScope.launch {
            // 1. Load cache immediately
            val cachedSections = sessionManager.getCachedHomeSectionsSync()
            if (cachedSections.isNotEmpty()) {
                _uiState.update { 
                    it.copy(
                        homeSections = cachedSections, 
                        isLoading = false,
                        isRefreshing = true
                    ) 
                }
            } else {
                 _uiState.update { it.copy(isLoading = true) }
            }

            try {
                // 2. Fetch fresh data
                val sections = youTubeRepository.getHomeSections()
                
                // 3. Update cached and UI
                sessionManager.saveHomeCache(sections)
                
                _uiState.update { 
                    it.copy(
                        homeSections = sections,
                        isLoading = false,
                        isRefreshing = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = if (it.homeSections.isEmpty()) e.message else null, // Only show error if no content
                        isLoading = false,
                        isRefreshing = false
                    )
                }
            }
        }
    }
    
    fun refresh() {
        loadData()
    }
}
