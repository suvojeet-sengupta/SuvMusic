package com.suvojeet.suvmusic.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.suvojeet.suvmusic.util.RingtoneHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RingtoneViewModel @Inject constructor(
    val ringtoneHelper: RingtoneHelper,
    private val youTubeRepository: com.suvojeet.suvmusic.data.repository.YouTubeRepository
) : ViewModel() {
    suspend fun getStreamUrl(videoId: String): String? {
        return youTubeRepository.getStreamUrl(videoId)
    }
}
