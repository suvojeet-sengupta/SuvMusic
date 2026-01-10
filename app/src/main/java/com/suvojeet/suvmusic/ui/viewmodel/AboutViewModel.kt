package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AboutViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {
    
    val isDeveloperMode = sessionManager.developerModeFlow
    
    /**
     * Enable developer mode - unlocks hidden JioSaavn feature.
     */
    fun enableDeveloperMode() {
        viewModelScope.launch {
            sessionManager.enableDeveloperMode()
        }
    }
    
    /**
     * Disable developer mode - hides JioSaavn feature.
     */
    fun disableDeveloperMode() {
        viewModelScope.launch {
            sessionManager.disableDeveloperMode()
        }
    }
}
