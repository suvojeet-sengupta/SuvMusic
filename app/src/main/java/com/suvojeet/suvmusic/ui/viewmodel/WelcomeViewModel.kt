package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {

    val currentSource = sessionManager.musicSourceFlow
    
    fun setOnboardingCompleted() {
        viewModelScope.launch {
            sessionManager.setOnboardingCompleted(true)
        }
    }
    
    fun setMusicSource(source: com.suvojeet.suvmusic.data.MusicSource) {
        viewModelScope.launch {
            sessionManager.setMusicSource(source)
        }
    }
}
