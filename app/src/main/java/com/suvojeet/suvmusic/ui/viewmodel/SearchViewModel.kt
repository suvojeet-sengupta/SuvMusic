package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.model.Album
import com.suvojeet.suvmusic.data.model.Artist
import com.suvojeet.suvmusic.data.model.BrowseCategory
import com.suvojeet.suvmusic.data.model.Playlist
import com.suvojeet.suvmusic.data.model.RecentSearchItem
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.repository.JioSaavnRepository
import com.suvojeet.suvmusic.data.repository.LocalAudioRepository
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import com.suvojeet.suvmusic.data.MusicSource
import com.suvojeet.suvmusic.player.MusicPlayer
import com.suvojeet.suvmusic.data.repository.DownloadRepository
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

enum class ResultFilter {
    ALL,
    SONGS,
    VIDEOS,
    ALBUMS,
    ARTISTS,
    COMMUNITY_PLAYLISTS,
    FEATURED_PLAYLISTS
}

data class SearchUiState(
    val query: String = "",
    val filter: String = YouTubeRepository.FILTER_SONGS,
    val results: List<Song> = emptyList(),
    val artistResults: List<Artist> = emptyList(),
    val albumResults: List<Album> = emptyList(),
    val playlistResults: List<Playlist> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val browseCategories: List<BrowseCategory> = emptyList(),
    val selectedCategory: BrowseCategory? = null,
    val recentSearches: List<RecentSearchItem> = emptyList(),
    val selectedTab: SearchTab = SearchTab.YOUTUBE_MUSIC,
    val showSuggestions: Boolean = false,
    val isLoading: Boolean = false,
    val isCategoriesLoading: Boolean = true,
    val isSuggestionsLoading: Boolean = false,
    val isSearchActive: Boolean = false,
    val error: String? = null,
    val currentSource: MusicSource = MusicSource.YOUTUBE,
    val resultFilter: ResultFilter = ResultFilter.ALL,
    val trendingSearches: List<String> = listOf(
        "Arijit Singh",
        "Trending 2024",
        "Lo-fi beats", 
        "Workout music",
        "Party songs",
        "Bollywood hits",
        "English songs"
    )
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
    private val jioSaavnRepository: JioSaavnRepository,
    private val localAudioRepository: LocalAudioRepository,
    private val sessionManager: SessionManager,
    private val musicPlayer: MusicPlayer,
    private val downloadRepository: DownloadRepository,
    private val playlistMgmtViewModel: PlaylistManagementViewModel
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
        viewModelScope.launch {
            loadRecentSearches()
        }
        
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
    
    private suspend fun loadRecentSearches() {
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
    
    fun setResultFilter(filter: ResultFilter) {
        _uiState.update { it.copy(resultFilter = filter) }
    }
    
    fun onTrendingSearchClick(query: String) {
        _uiState.update { 
            it.copy(
                query = query,
                showSuggestions = false,
                isSearchActive = true
            )
        }
        search()
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
                        coroutineScope {
                            val filter = _uiState.value.resultFilter
                            
                            when (filter) {
                                ResultFilter.ALL -> {
                                    val songsDeferred = async { youTubeRepository.search(query, YouTubeRepository.FILTER_SONGS) }
                                    val videosDeferred = async { youTubeRepository.search(query, YouTubeRepository.FILTER_VIDEOS) }
                                    val artistsDeferred = async { youTubeRepository.searchArtists(query) }
                                    val playlistsDeferred = async { youTubeRepository.searchPlaylists(query) }
                                    val albumsDeferred = async { youTubeRepository.searchAlbums(query) }
                                    
                                    val songs = songsDeferred.await()
                                    val videos = videosDeferred.await()
                                    val artists = artistsDeferred.await()
                                    val playlists = playlistsDeferred.await()
                                    val albums = albumsDeferred.await()
                                    
                                    val combinedResults = (songs + videos).distinctBy { it.id }
                                    
                                    _uiState.update { 
                                        it.copy(
                                            results = combinedResults,
                                            artistResults = artists,
                                            playlistResults = playlists,
                                            albumResults = albums,
                                            isLoading = false
                                        )
                                    }
                                }
                                ResultFilter.SONGS -> {
                                    val results = youTubeRepository.search(query, YouTubeRepository.FILTER_SONGS)
                                    _uiState.update { it.copy(results = results, isLoading = false) }
                                }
                                ResultFilter.VIDEOS -> {
                                    val results = youTubeRepository.search(query, YouTubeRepository.FILTER_VIDEOS)
                                    _uiState.update { it.copy(results = results, isLoading = false) }
                                }
                                ResultFilter.ALBUMS -> {
                                    val results = youTubeRepository.searchAlbums(query)
                                    _uiState.update { it.copy(albumResults = results, isLoading = false) }
                                }
                                ResultFilter.ARTISTS -> {
                                    val results = youTubeRepository.searchArtists(query)
                                    _uiState.update { it.copy(artistResults = results, isLoading = false) }
                                }
                                ResultFilter.COMMUNITY_PLAYLISTS -> {
                                    val results = youTubeRepository.searchPlaylists(query)
                                    val filtered = results.filter { 
                                        !it.author.equals("YouTube Music", ignoreCase = true) && 
                                        !it.author.equals("YouTube", ignoreCase = true) 
                                    }
                                    _uiState.update { it.copy(playlistResults = filtered, isLoading = false) }
                                }
                                ResultFilter.FEATURED_PLAYLISTS -> {
                                    val results = youTubeRepository.searchPlaylists(query)
                                    val filtered = results.filter { 
                                        it.author.equals("YouTube Music", ignoreCase = true) || 
                                        it.author.equals("YouTube", ignoreCase = true) 
                                    }
                                    _uiState.update { it.copy(playlistResults = filtered, isLoading = false) }
                                }
                            }
                        }
                    }
                    SearchTab.JIOSAAVN -> {
                        // Search JioSaavn (320kbps) - Comprehensive search
                        val results = jioSaavnRepository.searchAll(query)
                        _uiState.update { 
                            it.copy(
                                results = results.songs,
                                artistResults = results.artists,
                                albumResults = results.albums,
                                playlistResults = results.playlists,
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
            sessionManager.addRecentSearch(RecentSearchItem.SongItem(song))
            loadRecentSearches()
        }
    }

    fun addToRecentSearches(album: Album) {
        viewModelScope.launch {
            sessionManager.addRecentSearch(RecentSearchItem.AlbumItem(album))
            loadRecentSearches()
        }
    }

    fun addToRecentSearches(playlist: Playlist) {
        viewModelScope.launch {
            sessionManager.addRecentSearch(RecentSearchItem.PlaylistItem(playlist))
            loadRecentSearches()
        }
    }
    
    fun clearRecentSearches() {
        viewModelScope.launch {
            sessionManager.clearRecentSearches()
            _uiState.update { it.copy(recentSearches = emptyList()) }
        }
    }
    
    fun onRecentSearchClick(item: RecentSearchItem) {
        // Update query to show the title
        _uiState.update { 
            it.copy(
                query = item.title,
                isSearchActive = true,
                showSuggestions = false
            )
        }
        search()
        
        // Move to top of recent logic is handled by adding it again
        viewModelScope.launch {
            sessionManager.addRecentSearch(item)
            loadRecentSearches()
        }
    }

    fun playNext(song: Song) {
        musicPlayer.playNext(listOf(song))
    }

    fun addToQueue(song: Song) {
        musicPlayer.addToQueue(listOf(song))
    }

    fun downloadSong(song: Song) {
        viewModelScope.launch {
            downloadRepository.downloadSong(song)
        }
    }
    
    fun addToPlaylist(song: Song) {
        playlistMgmtViewModel.showAddToPlaylistSheet(song)
    }
}

