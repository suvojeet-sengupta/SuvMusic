package com.suvojeet.suvmusic.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.util.RingtoneHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RingtoneUiState(
    val targetSong: Song? = null,
    val showTrimmer: Boolean = false,
    val showProgress: Boolean = false,
    val progress: Float = 0f,
    val statusMessage: String = "",
    val isComplete: Boolean = false,
    val isSuccess: Boolean = false,
    val ringtoneUri: Uri? = null
)

@HiltViewModel
class RingtoneViewModel @Inject constructor(
    val ringtoneHelper: RingtoneHelper,
    private val youTubeRepository: com.suvojeet.suvmusic.data.repository.YouTubeRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(RingtoneUiState())
    val uiState: StateFlow<RingtoneUiState> = _uiState.asStateFlow()

    fun showTrimmer(song: Song) {
        _uiState.update { it.copy(targetSong = song, showTrimmer = true) }
    }

    fun hideTrimmer() {
        _uiState.update { it.copy(showTrimmer = false) }
    }

    fun setAsRingtone(context: Context, song: Song, startMs: Long, endMs: Long) {
        _uiState.update { 
            it.copy(
                showTrimmer = false,
                showProgress = true,
                progress = 0f,
                statusMessage = "Starting...",
                isComplete = false,
                isSuccess = false,
                ringtoneUri = null
            ) 
        }

        viewModelScope.launch {
            ringtoneHelper.downloadAndTrimAsRingtone(
                context = context,
                song = song,
                startMs = startMs,
                endMs = endMs,
                onProgress = { p, msg ->
                    _uiState.update { it.copy(progress = p, statusMessage = msg) }
                },
                onComplete = { success, msg, uri ->
                    _uiState.update { 
                        it.copy(
                            isComplete = true,
                            isSuccess = success,
                            statusMessage = msg,
                            ringtoneUri = uri
                        ) 
                    }
                }
            )
        }
    }

    fun dismissProgress() {
        _uiState.update { it.copy(showProgress = false) }
    }

    suspend fun getStreamUrl(videoId: String): String? {
        return youTubeRepository.getStreamUrl(videoId)
    }
}
