package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.suvojeet.suvmusic.data.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AboutViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {
    // Developer mode related logic removed
}
