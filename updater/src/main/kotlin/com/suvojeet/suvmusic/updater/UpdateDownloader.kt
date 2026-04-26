package com.suvojeet.suvmusic.updater

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
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
import java.io.FileInputStream
import java.security.MessageDigest
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

    // Expected SHA-256 of the APK being downloaded (hex, lowercase). When set,
    // the downloaded file is verified before install; mismatch aborts install.
    private var expectedSha256: String? = null

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    /**
     * @param sha256 Optional hex-encoded SHA-256 of the expected APK. If supplied,
     *               the installer refuses to run if the downloaded file's digest
     *               does not match — protects against MITM swap attacks.
     */
    fun downloadAndInstall(url: String, versionName: String, sha256: String? = null) {
        expectedSha256 = sha256?.lowercase()

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

        // Register the completion receiver privately — system DownloadManager
        // dispatches to registered receivers, but NOT_EXPORTED blocks other apps
        // from spoofing ACTION_DOWNLOAD_COMPLETE with a malicious download id.
        val completionReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    stopProgressTracking()
                    val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                    _downloadState.value = DownloadState.Completed(file)
                    installApk(file)
                    try { ctx.unregisterReceiver(this) } catch (_: Exception) {}
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            completionReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
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

        // Refuse to launch the installer if an expected hash was supplied and
        // the downloaded file doesn't match — defends against MITM swaps.
        expectedSha256?.let { expected ->
            val actual = sha256(file)
            if (actual == null || !actual.equals(expected, ignoreCase = true)) {
                try { file.delete() } catch (_: Exception) {}
                _downloadState.value = DownloadState.Error("Update rejected: signature check failed")
                return
            }
        }

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
            _downloadState.value = DownloadState.Error("Error starting installation")
        }
    }

    private fun sha256(file: File): String? = try {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                digest.update(buf, 0, n)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    } catch (_: Exception) {
        null
    }
}
