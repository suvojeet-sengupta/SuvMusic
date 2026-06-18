package com.suvojeet.suvmusic.data.repository.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object RemoteAudioApiStatus {
    private val _isPrimaryApiWorking = MutableStateFlow(true)
    val isPrimaryApiWorking: StateFlow<Boolean> = _isPrimaryApiWorking

    fun setPrimaryApiWorking(working: Boolean) {
        _isPrimaryApiWorking.value = working
    }
}
