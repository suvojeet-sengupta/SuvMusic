package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.model.Artist
import com.suvojeet.suvmusic.data.model.BrowseCategory
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.repository.JioSaavnRepository
import com.suvojeet.suvmusic.data.repository.LocalAudioRepository
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import com.suvojeet.suvmusic.data.MusicSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SearchTab {
    YOUTUBE_MUSIC,
    JIOSAAVN,
    YOUR_LIBRARY
}

data class SearchUiState(
    val query: String = "",
    val filter: String = YouTubeRepository.FILTER_SONGS,
    val results: List<Song> = emptyList(),
    val artistResults: List<Artist> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val browseCategories: List<BrowseCategory> = emptyList(),
    val selectedCategory: BrowseCategory? = null,
    val recentSearches: List<Song> = emptyList(),
    val selectedTab: SearchTab = SearchTab.YOUTUBE_MUSIC,
    val showSuggestions: Boolean = false,
    val isLoading: Boolean = false,
    val isCategoriesLoading: Boolean = true,
    val isSuggestionsLoading: Boolean = false,
    val isSearchActive: Boolean = false,
    val error: String? = null,
    val currentSource: MusicSource = MusicSource.YOUTUBE
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
    private val jioSaavnRepository: JioSaavnRepository,
    private val localAudioRepository: LocalAudioRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    // Developer mode - shows JioSaavn tab when enabled
    val isDeveloperMode = sessionManager.developerModeFlow
    
    private val _searchQuery = MutableStateFlow("")
    private var suggestionJob: Job? = null
    private var searchJob: Job? = null
    
    init {
        // Load browse categories on init
        loadBrowseCategories()
        
        // Load recent searches
        loadRecentSearches()
        
        // Observe music source
        observeMusicSource()
        
        // Observe query changes for debounced suggestions and search
        viewModelScope.launch {
            _searchQuery
                .debounce(300) // 300ms debounce
                .distinctUntilChanged()
                .filter { it.isNotBlank() }
                .collect { query ->
                    fetchSuggestions(query)
                    // Auto-search while typing
                    searchInternal(query)
                }
        }
    }
    
    private fun observeMusicSource() {
        viewModelScope.launch {
            sessionManager.musicSourceFlow.collect { source ->
                val defaultTab = when (source) {
                    MusicSource.JIOSAAVN -> SearchTab.JIOSAAVN
                    else -> SearchTab.YOUTUBE_MUSIC
                }
                _uiState.update { 
                    it.copy(
                        currentSource = source,
                        selectedTab = defaultTab
                    ) 
                }
            }
        }
    }
    
    private fun loadRecentSearches() {
        val recentSearches = sessionManager.getRecentSearches()
        _uiState.update { it.copy(recentSearches = recentSearches) }
    }
    
    private fun loadBrowseCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCategoriesLoading = true) }
            try {
                val categories = youTubeRepository.getMoodsAndGenres()
                _uiState.update { 
                    it.copy(
                        browseCategories = categories,
                        isCategoriesLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isCategoriesLoading = false) }
            }
        }
    }
    
    fun onCategoryClick(category: BrowseCategory) {
        _uiState.update { 
            it.copy(
                selectedCategory = category,
                query = category.title,
                showSuggestions = false,
                isSearchActive = true
            )
        }
        
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val results = youTubeRepository.getCategoryContent(
                    browseId = category.browseId,
                    params = category.params
                )
                _uiState.update { 
                    it.copy(
                        results = results,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
            }
        }
    }
    
    fun clearCategorySelection() {
        _uiState.update { 
            it.copy(
                selectedCategory = null,
                query = "",
                results = emptyList(),
                suggestions = emptyList(),
                showSuggestions = false,
                isSearchActive = false
            )
        }
        _searchQuery.value = ""
    }
    
    fun onQueryChange(query: String) {
        _uiState.update { 
            it.copy(
                query = query,
                showSuggestions = query.isNotBlank(),
                selectedCategory = null,
                isSearchActive = query.isNotBlank()
            )
        }
        _searchQuery.value = query
        
        // Clear suggestions and results if query is empty
        if (query.isBlank()) {
            _uiState.update { 
                it.copy(
                    suggestions = emptyList(),
                    showSuggestions = false,
                    results = emptyList(),
                    isSearchActive = false
                )
            }
        }
    }
    
    fun onSearchFocusChange(focused: Boolean) {
        if (focused && _uiState.value.query.isBlank()) {
            _uiState.update { it.copy(isSearchActive = true) }
        }
    }
    
    fun onBackPressed() {
        clearCategorySelection()
    }
    
    private fun fetchSuggestions(query: String) {
        suggestionJob?.cancel()
        suggestionJob = viewModelScope.launch {
            _uiState.update { it.copy(isSuggestionsLoading = true) }
            try {
                val suggestions = youTubeRepository.getSearchSuggestions(query)
                _uiState.update { 
                    it.copy(
                        suggestions = suggestions,
                        isSuggestionsLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSuggestionsLoading = false) }
            }
        }
    }
    
    fun onSuggestionClick(suggestion: String) {
        _uiState.update { 
            it.copy(
                query = suggestion,
                showSuggestions = false
            )
        }
        search()
    }
    
    fun hideSuggestions() {
        _uiState.update { it.copy(showSuggestions = false) }
    }
    
    fun setFilter(filter: String) {
        _uiState.update { it.copy(filter = filter) }
        if (_uiState.value.query.isNotBlank()) {
            search()
        }
    }
    
    fun onTabChange(tab: SearchTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        if (_uiState.value.query.isNotBlank()) {
            search()
        }
    }
    
    fun search() {
        val query = _uiState.value.query
        if (query.isBlank()) return
        
        // Hide suggestions when searching
        _uiState.update { it.copy(showSuggestions = false) }
        
        searchInternal(query)
    }
    
    private fun searchInternal(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val currentTab = _uiState.value.selectedTab
                
                when (currentTab) {
                    SearchTab.YOUTUBE_MUSIC -> {
                        // Search songs and artists in parallel
                        coroutineScope {
                            val songsDeferred = async { 
                                youTubeRepository.search(query, _uiState.value.filter) 
                            }
                            val artistsDeferred = async { 
                                youTubeRepository.searchArtists(query) 
                            }
                            
                            val songs = songsDeferred.await()
                            val artists = artistsDeferred.await()
                            
                            _uiState.update { 
                                it.copy(
                                    results = songs,
                                    artistResults = artists,
                                    isLoading = false
                                )
                            }
                        }
                    }
                    SearchTab.JIOSAAVN -> {
                        // Search JioSaavn (320kbps)
                        val results = jioSaavnRepository.search(query)
                        _uiState.update { 
                            it.copy(
                                results = results,
                                artistResults = emptyList(),
                                isLoading = false
                            )
                        }
                    }
                    SearchTab.YOUR_LIBRARY -> {
                        val results = localAudioRepository.searchLocalSongs(query)
                        _uiState.update { 
                            it.copy(
                                results = results,
                                artistResults = emptyList(),
                                isLoading = false
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
            }
        }
    }
    
    fun addToRecentSearches(song: Song) {
        viewModelScope.launch {
            sessionManager.addRecentSearch(song)
            loadRecentSearches()
        }
    }
    
    fun clearRecentSearches() {
        viewModelScope.launch {
            sessionManager.clearRecentSearches()
            _uiState.update { it.copy(recentSearches = emptyList()) }
        }
    }
    
    fun onRecentSearchClick(song: Song) {
        // Update query to show the song title
        _uiState.update { 
            it.copy(
                query = song.title,
                isSearchActive = true,
                showSuggestions = false
            )
        }
        search()
    }
}

