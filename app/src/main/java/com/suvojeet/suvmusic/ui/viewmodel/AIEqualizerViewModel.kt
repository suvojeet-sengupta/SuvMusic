package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.suvojeet.suvmusic.ai.AIEqualizerService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AIEqualizerViewModel @Inject constructor(
    val aiService: AIEqualizerService
) : ViewModel()
