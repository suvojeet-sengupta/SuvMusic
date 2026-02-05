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
     * Validate password and enable developer mode if correct.
     * Returns true if successful, false otherwise.
     */
    fun tryUnlockDeveloperMode(password: String): Boolean {
        if (com.suvojeet.suvmusic.util.SecureConfig.checkDeveloperPassword(password)) {
            viewModelScope.launch {
                sessionManager.enableDeveloperMode()
            }
            return true
        }
        return false
    }
    
    /**
     * Disable developer mode - hides advanced experimental features.
     */
    fun disableDeveloperMode() {
        viewModelScope.launch {
            sessionManager.disableDeveloperMode()
        }
    }
}
