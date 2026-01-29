package com.suvojeet.suvmusic.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.SessionManager
import com.suvojeet.suvmusic.data.model.UpdateState
import com.suvojeet.suvmusic.data.model.UpdateChannel
import com.suvojeet.suvmusic.data.repository.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class MainUiState(
    val updateState: UpdateState = UpdateState.Idle,
    val currentVersion: String = ""
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val updateRepository: UpdateRepository,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var downloadJob: Job? = null
    private var downloadedApkFile: File? = null

    init {
        // Load current version
        _uiState.update { it.copy(currentVersion = updateRepository.getCurrentVersionName()) }
        
        // Auto-check for updates on app startup
        checkForUpdates()
    }

    private fun checkForUpdates() {
        viewModelScope.launch {
            // Optional: Delay slightly to not block startup or show dialog instantly
            delay(2000) 
            
            _uiState.update { it.copy(updateState = UpdateState.Checking) }

            val channel = sessionManager.getUpdateChannel()
            updateRepository.checkForUpdate(channel)
                .onSuccess { update ->
                    if (update != null) {
                        _uiState.update { it.copy(updateState = UpdateState.UpdateAvailable(update)) }
                    } else {
                        // Silent failure/idle if no update, don't show "No update" dialog on startup
                        _uiState.update { it.copy(updateState = UpdateState.Idle) }
                    }
                }
                .onFailure {
                    // Silent failure on startup
                    _uiState.update { it.copy(updateState = UpdateState.Idle) }
                }
        }
    }

    fun downloadUpdate(downloadUrl: String, versionName: String) {
        downloadJob = viewModelScope.launch {
            _uiState.update { it.copy(updateState = UpdateState.Downloading(0)) }

            updateRepository.downloadApk(
                downloadUrl = downloadUrl,
                versionName = versionName,
                onProgress = { progress ->
                    _uiState.update { it.copy(updateState = UpdateState.Downloading(progress)) }
                }
            ).onSuccess { file ->
                downloadedApkFile = file
                _uiState.update { it.copy(updateState = UpdateState.Downloaded) }
                installUpdate()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(updateState = UpdateState.Error(error.message ?: "Download failed"))
                }
            }
        }
    }

    fun installUpdate() {
        val apkFile = downloadedApkFile ?: return

        try {
            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    apkFile
                )
            } else {
                Uri.fromFile(apkFile)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            _uiState.update {
                it.copy(updateState = UpdateState.Error("Failed to install: ${e.message}"))
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _uiState.update { it.copy(updateState = UpdateState.Idle) }
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(updateState = UpdateState.Idle) }
    }
}
