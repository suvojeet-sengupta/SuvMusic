package com.suvojeet.suvmusic.updater

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
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
    data object Verifying : DownloadState()
    data class Completed(val file: File) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

@Singleton
class UpdateDownloader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadId: Long = -1
    private var progressJob: Job? = null
    private var targetFile: File? = null

    private var expectedSha256: String? = null

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private fun updatesDir(): File =
        File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir, "updates")
            .apply { mkdirs() }

    /**
     * @param sha256 Optional hex-encoded SHA-256 of the expected APK. If supplied,
     *               the installer refuses to run if the downloaded file's digest
     *               does not match — protects against MITM swap attacks.
     */
    fun downloadAndInstall(url: String, versionName: String, sha256: String? = null) {
        val current = _downloadState.value
        if (current is DownloadState.Downloading || current is DownloadState.Verifying) return

        expectedSha256 = sha256?.lowercase()
        val file = File(updatesDir(), "SuvMusic-v$versionName.apk")
        targetFile = file

        if (file.exists() && file.length() > 0 && isValidApk(file) && hashMatches(file)) {
            _downloadState.value = DownloadState.Completed(file)
            installApk(file)
            return
        }

        updatesDir().listFiles()?.forEach { runCatching { it.delete() } }

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Downloading SuvMusic Update")
            .setDescription("Version $versionName")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationUri(Uri.fromFile(file))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            request.setRequiresCharging(false)
            request.setRequiresDeviceIdle(false)
        }

        downloadId = try {
            downloadManager.enqueue(request)
        } catch (e: Exception) {
            _downloadState.value = DownloadState.Error("Couldn't start download: ${e.message ?: "unknown error"}")
            return
        }
        _downloadState.value = DownloadState.Downloading(0f, 0, 0)
        startProgressTracking(file)
    }

    fun cancel() {
        stopProgressTracking()
        if (downloadId != -1L) runCatching { downloadManager.remove(downloadId) }
        downloadId = -1
        targetFile?.let { runCatching { it.delete() } }
        _downloadState.value = DownloadState.Idle
    }

    /** Re-launch the installer for an already downloaded, verified APK. */
    fun install() {
        (_downloadState.value as? DownloadState.Completed)?.let { installApk(it.file) }
    }

    fun clearError() {
        if (_downloadState.value is DownloadState.Error) _downloadState.value = DownloadState.Idle
    }

    private fun startProgressTracking(file: File) {
        progressJob?.cancel()
        progressJob = scope.launch {
            var stalledPolls = 0
            var lastBytes = -1L
            while (isActive) {
                val snapshot = runCatching {
                    downloadManager.query(DownloadManager.Query().setFilterById(downloadId))?.use { c ->
                        if (!c.moveToFirst()) null
                        else Triple(
                            c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
                            c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                            c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)) to
                                c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                        )
                    }
                }.getOrNull()

                if (snapshot == null) {
                    _downloadState.value = DownloadState.Error("Download was cancelled")
                    return@launch
                }
                val (status, bytes, totalAndReason) = snapshot
                val (total, reason) = totalAndReason
                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        finishDownload(file)
                        return@launch
                    }
                    DownloadManager.STATUS_FAILED -> {
                        runCatching { downloadManager.remove(downloadId) }
                        _downloadState.value = DownloadState.Error(failureMessage(reason))
                        return@launch
                    }
                    else -> {
                        val progress = if (total > 0) (bytes.toFloat() / total).coerceIn(0f, 1f) else 0f
                        _downloadState.value = DownloadState.Downloading(progress, bytes, total.coerceAtLeast(0))
                        stalledPolls = if (bytes == lastBytes) stalledPolls + 1 else 0
                        lastBytes = bytes
                        if (status == DownloadManager.STATUS_PAUSED && stalledPolls > 240) {
                            runCatching { downloadManager.remove(downloadId) }
                            _downloadState.value = DownloadState.Error("Download stalled. Check your connection and retry.")
                            return@launch
                        }
                    }
                }
                delay(400)
            }
        }
    }

    private fun finishDownload(file: File) {
        _downloadState.value = DownloadState.Verifying
        if (!file.exists() || file.length() == 0L) {
            _downloadState.value = DownloadState.Error("Downloaded file is missing. Please retry.")
            return
        }
        if (!hashMatches(file)) {
            runCatching { file.delete() }
            _downloadState.value = DownloadState.Error("Update rejected: checksum mismatch")
            return
        }
        if (!isValidApk(file)) {
            runCatching { file.delete() }
            _downloadState.value = DownloadState.Error("Downloaded update is corrupted. Please retry.")
            return
        }
        _downloadState.value = DownloadState.Completed(file)
        installApk(file)
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun failureMessage(reason: Int): String = when (reason) {
        DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Not enough storage space for the update"
        DownloadManager.ERROR_HTTP_DATA_ERROR, DownloadManager.ERROR_CANNOT_RESUME -> "Connection interrupted. Please retry."
        DownloadManager.ERROR_TOO_MANY_REDIRECTS, DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Update server error. Please retry later."
        else -> "Download failed. Please retry."
    }

    private fun hashMatches(file: File): Boolean {
        val expected = expectedSha256 ?: return true
        return sha256(file)?.equals(expected, ignoreCase = true) == true
    }

    private fun isValidApk(file: File): Boolean = runCatching {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
        info != null && info.packageName == context.packageName
    }.getOrDefault(false)

    private fun installApk(file: File) {
        if (!file.exists()) {
            _downloadState.value = DownloadState.Error("Update file not found. Please retry.")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val settings = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(settings) }
            return
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
            _downloadState.value = DownloadState.Error("Couldn't open the installer")
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
