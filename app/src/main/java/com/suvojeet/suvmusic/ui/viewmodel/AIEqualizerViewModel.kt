package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.suvojeet.suvmusic.ai.AIEqualizerService
import javax.inject.Inject

class AIEqualizerViewModel @Inject constructor(
    val aiService: AIEqualizerService
) : ViewModel()
