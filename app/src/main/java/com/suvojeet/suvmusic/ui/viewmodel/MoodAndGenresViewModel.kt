package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.model.BrowseCategory
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MoodAndGenresUiState(
    val categories: List<BrowseCategory> = emptyList(),
    val isLoadingCategories: Boolean = false,
    
    val categorySongs: List<Song> = emptyList(),
    val isLoadingContent: Boolean = false,
    val selectedCategoryTitle: String = ""
)

@HiltViewModel
class MoodAndGenresViewModel @Inject constructor(
    private val repository: YouTubeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoodAndGenresUiState())
    val uiState: StateFlow<MoodAndGenresUiState> = _uiState.asStateFlow()

    init {
        loadMoodsAndGenres()
    }

    fun loadMoodsAndGenres() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCategories = true) }
            val categories = repository.getMoodsAndGenres()
            _uiState.update { 
                it.copy(
                    categories = categories,
                    isLoadingCategories = false
                ) 
            }
        }
    }

    fun loadCategoryContent(browseId: String, params: String?, title: String) {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isLoadingContent = true,
                    selectedCategoryTitle = title,
                    categorySongs = emptyList() 
                ) 
            }
            
            val songs = repository.getCategoryContent(browseId, params)
            
            _uiState.update { 
                it.copy(
                    categorySongs = songs,
                    isLoadingContent = false
                ) 
            }
        }
    }
}
