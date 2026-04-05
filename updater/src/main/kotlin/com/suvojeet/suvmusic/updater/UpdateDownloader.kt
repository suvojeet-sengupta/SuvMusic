package com.suvojeet.suvmusic.updater

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(val progress: Float, val bytesDownloaded: Long, val totalBytes: Long) : DownloadState()
    data class Completed(val file: File) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

@Singleton
class UpdateDownloader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private var downloadId: Long = -1
    private var progressJob: Job? = null

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    fun downloadAndInstall(url: String, versionName: String) {
        val fileName = "SuvMusic-v$versionName.apk"
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Downloading SuvMusic Update")
            .setDescription("Version $versionName")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            request.setRequiresCharging(false)
            request.setRequiresDeviceIdle(false)
        }

        downloadId = downloadManager.enqueue(request)
        _downloadState.value = DownloadState.Downloading(0f, 0, 0)
        
        startProgressTracking()

        // Register receiver for when download is complete
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    stopProgressTracking()
                    val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                    _downloadState.value = DownloadState.Completed(file)
                    installApk(file)
                    context.unregisterReceiver(this)
                }
            }
        }, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = CoroutineScope(Dispatchers.IO).launch {
            var isDownloading = true
            while (isDownloading) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor.moveToFirst()) {
                    val bytesDownloadedColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val totalBytesColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val statusColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)

                    if (bytesDownloadedColumnIndex != -1 && totalBytesColumnIndex != -1 && statusColumnIndex != -1) {
                        val bytesDownloaded = cursor.getLong(bytesDownloadedColumnIndex)
                        val totalBytes = cursor.getLong(totalBytesColumnIndex)
                        val status = cursor.getInt(statusColumnIndex)

                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            isDownloading = false
                        } else if (status == DownloadManager.STATUS_FAILED) {
                            _downloadState.value = DownloadState.Error("Download failed")
                            isDownloading = false
                        } else if (totalBytes > 0) {
                            val progress = bytesDownloaded.toFloat() / totalBytes.toFloat()
                            _downloadState.value = DownloadState.Downloading(progress, bytesDownloaded, totalBytes)
                        }
                    }
                }
                cursor.close()
                delay(500) // Poll every 500ms
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun installApk(file: File) {
        if (!file.exists()) return

        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } else {
            Uri.fromFile(file)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            _downloadState.value = DownloadState.Error("Error starting installation: ${e.message}")
        }
    }
}
