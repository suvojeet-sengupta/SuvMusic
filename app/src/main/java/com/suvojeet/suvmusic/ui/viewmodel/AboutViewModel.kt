package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.suvojeet.suvmusic.data.SessionManager
import javax.inject.Inject

class AboutViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {
    // Developer mode related logic removed
}
