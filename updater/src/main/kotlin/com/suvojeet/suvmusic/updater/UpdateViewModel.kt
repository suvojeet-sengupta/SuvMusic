package com.suvojeet.suvmusic.updater

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UpdateState {
    data object Idle : UpdateState()
    data class UpdateAvailable(val info: UpdateInfo) : UpdateState()
    data object NoUpdate : UpdateState()
}

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val checker: UpdateChecker
) : ViewModel() {

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    fun checkForUpdate(currentVersionCode: Int) {
        viewModelScope.launch {
            val updateInfo = checker.checkForUpdate()
            if (updateInfo != null && updateInfo.versionCode > currentVersionCode) {
                _updateState.value = UpdateState.UpdateAvailable(updateInfo)
            } else if (updateInfo != null) {
                _updateState.value = UpdateState.NoUpdate
            }
        }
    }

    fun dismissDialog() {
        _updateState.value = UpdateState.Idle
    }
}
