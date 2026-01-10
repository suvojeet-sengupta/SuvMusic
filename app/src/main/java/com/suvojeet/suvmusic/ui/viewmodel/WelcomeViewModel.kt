package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {

    val currentSource = sessionManager.musicSourceFlow
    
    // Developer mode - shows JioSaavn option when enabled
    val isDeveloperMode = sessionManager.developerModeFlow
    
    // Track if user has explicitly selected a source
    private val _sourceSelected = MutableStateFlow(false)
    val sourceSelected: StateFlow<Boolean> = _sourceSelected.asStateFlow()
    
    fun setOnboardingCompleted() {
        viewModelScope.launch {
            sessionManager.setOnboardingCompleted(true)
        }
    }
    
    fun setMusicSource(source: com.suvojeet.suvmusic.data.MusicSource) {
        viewModelScope.launch {
            sessionManager.setMusicSource(source)
            _sourceSelected.value = true
        }
    }
}

