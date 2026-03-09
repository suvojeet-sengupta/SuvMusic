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
    data object Checking : UpdateState()
    data class UpdateAvailable(val info: UpdateInfo) : UpdateState()
    data class NoUpdate(val info: UpdateInfo) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val checker: UpdateChecker
) : ViewModel() {

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _changelog = MutableStateFlow<ChangelogInfo?>(null)
    val changelog: StateFlow<ChangelogInfo?> = _changelog.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _lastUpdated = MutableStateFlow<Long?>(null)
    val lastUpdated: StateFlow<Long?> = _lastUpdated.asStateFlow()

    init {
        loadChangelog()
    }

    fun loadChangelog() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val info = checker.fetchChangelog()
            _changelog.value = info
            _lastUpdated.value = System.currentTimeMillis()
            _isRefreshing.value = false
        }
    }

    fun checkForUpdate(currentVersionCode: Int, silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _updateState.value = UpdateState.Checking
            
            // Fetch both update info and changelog to ensure everything is fresh
            val updateJob = launch {
                val updateInfo = checker.checkForUpdate()
                if (updateInfo != null) {
                    _lastUpdated.value = System.currentTimeMillis()
                    if (updateInfo.versionCode > currentVersionCode) {
                        _updateState.value = UpdateState.UpdateAvailable(updateInfo)
                    } else {
                        _updateState.value = UpdateState.NoUpdate(updateInfo)
                    }
                } else {
                    if (!silent) _updateState.value = UpdateState.Error("Could not check for updates")
                }
            }
            
            val changelogJob = launch {
                loadChangelog()
            }
            
            updateJob.join()
            changelogJob.join()
        }
    }

    fun dismissDialog() {
        _updateState.value = UpdateState.Idle
    }
    
    fun resetUpdateState() {
        _updateState.value = UpdateState.Idle
    }
}
