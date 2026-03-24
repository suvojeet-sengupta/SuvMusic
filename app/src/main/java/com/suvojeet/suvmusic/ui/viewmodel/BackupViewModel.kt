package com.suvojeet.suvmusic.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suvojeet.suvmusic.data.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupUiState(
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val lastError: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupManager: BackupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun createBackup(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackingUp = true, lastError = null, successMessage = null) }
            
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val result = backupManager.createBackup(outputStream)
                    if (result.isSuccess) {
                        _uiState.update { it.copy(isBackingUp = false, successMessage = "Backup created successfully") }
                    } else {
                        _uiState.update { it.copy(isBackingUp = false, lastError = result.exceptionOrNull()?.message ?: "Unknown error") }
                    }
                } ?: run {
                    _uiState.update { it.copy(isBackingUp = false, lastError = "Could not open output stream") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isBackingUp = false, lastError = e.message ?: "Unknown error") }
            }
        }
    }

    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true, lastError = null, successMessage = null) }
            
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val result = backupManager.restoreBackup(inputStream)
                    if (result.isSuccess) {
                        _uiState.update { it.copy(isRestoring = false, successMessage = "Restore completed successfully. Please restart the app for all changes to take effect.") }
                    } else {
                        _uiState.update { it.copy(isRestoring = false, lastError = result.exceptionOrNull()?.message ?: "Unknown error") }
                    }
                } ?: run {
                    _uiState.update { it.copy(isRestoring = false, lastError = "Could not open input stream") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isRestoring = false, lastError = e.message ?: "Unknown error") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(lastError = null, successMessage = null) }
    }
}
