package com.suvojeet.suvmusic.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.suvojeet.suvmusic.MainActivity
import com.suvojeet.suvmusic.R
import com.suvojeet.suvmusic.data.model.Song
import com.suvojeet.suvmusic.data.repository.DownloadRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service for downloading songs with real-time progress notifications.
 * Supports background downloads even when app is closed.
 */
@AndroidEntryPoint
class DownloadService : Service() {

    companion object {
        private const val TAG = "DownloadService"
        private const val CHANNEL_ID = "download_channel"
        private const val NOTIFICATION_ID = 2001
        private const val COMPLETE_NOTIFICATION_ID = 2002
        
        private const val ACTION_START_DOWNLOAD = "com.suvojeet.suvmusic.START_DOWNLOAD"
        private const val ACTION_CANCEL_DOWNLOAD = "com.suvojeet.suvmusic.CANCEL_DOWNLOAD"
        private const val EXTRA_SONG_JSON = "song_json"
        
        fun startDownload(context: Context, song: Song) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START_DOWNLOAD
                putExtra(EXTRA_SONG_JSON, songToJson(song))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun cancelDownload(context: Context, songId: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_CANCEL_DOWNLOAD
                putExtra("song_id", songId)
            }
            context.startService(intent)
        }
        
        private fun songToJson(song: Song): String {
            return com.google.gson.Gson().toJson(song)
        }
        
        private fun jsonToSong(json: String): Song? {
            return try {
                com.google.gson.Gson().fromJson(json, Song::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    @Inject
    lateinit var downloadRepository: DownloadRepository
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var progressJob: Job? = null
    
    // Track active downloads: ID -> Title
    private val activeDownloads = java.util.concurrent.ConcurrentHashMap<String, String>()
    // Track jobs for cancellation: ID -> Job
    private val downloadJobs = java.util.concurrent.ConcurrentHashMap<String, Job>()
    
    // The song currently being displayed in the notification
    private var primaryNotificationSongId: String? = null
    
    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        observeDownloadProgress()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_DOWNLOAD -> {
                val songJson = intent.getStringExtra(EXTRA_SONG_JSON)
                val song = songJson?.let { jsonToSong(it) }
                if (song != null) {
                    startDownloadWithNotification(song)
                }
            }
            ACTION_CANCEL_DOWNLOAD -> {
                val songId = intent.getStringExtra("song_id")
                if (songId != null) {
                    cancelDownload(songId)
                }
            }
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Song download progress"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun startDownloadWithNotification(song: Song) {
        // Add to active list
        activeDownloads[song.id] = song.title
        primaryNotificationSongId = song.id
        
        // Start foreground with initial notification
        updateForegroundNotification()
        
        val job = serviceScope.launch {
            try {
                Log.d(TAG, "Starting download for: ${song.title}")
                val success = downloadRepository.downloadSong(song)
                
                if (success) {
                    showCompleteNotification(song.title, true)
                } else {
                    showCompleteNotification(song.title, false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download error", e)
                showCompleteNotification(song.title, false)
            } finally {
                // Cleanup
                activeDownloads.remove(song.id)
                downloadJobs.remove(song.id)
                
                // If this was the primary song, pick another one or clear
                if (primaryNotificationSongId == song.id) {
                    primaryNotificationSongId = activeDownloads.keys.firstOrNull()
                }
                
                // Check if service should stop
                if (activeDownloads.isEmpty()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    updateForegroundNotification()
                }
            }
        }
        
        downloadJobs[song.id] = job
    }
    
    private fun observeDownloadProgress() {
        progressJob = serviceScope.launch {
            downloadRepository.downloadProgress.collectLatest { progressMap ->
                val primaryId = primaryNotificationSongId ?: return@collectLatest
                val title = activeDownloads[primaryId] ?: return@collectLatest
                val progress = progressMap[primaryId] ?: 0f
                
                val progressPercent = (progress * 100).toInt()
                updateProgressNotification(title, progressPercent)
            }
        }
    }
    
    private fun cancelDownload(songId: String) {
        val job = downloadJobs[songId]
        if (job != null) {
            job.cancel()
            // Cleanup will happen in finally block of the job
        }
    }
    
    private fun createProgressNotification(songTitle: String, progress: Int): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val cancelIntent = Intent(this, DownloadService::class.java).apply {
            action = ACTION_CANCEL_DOWNLOAD
            // If multiple, canceling notification action could cancel the primary one
            putExtra("song_id", primaryNotificationSongId)
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            1,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val extraCount = activeDownloads.size - 1
        val contentText = if (extraCount > 0) {
            "$songTitle (+ $extraCount others)"
        } else {
            songTitle
        }
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Downloading")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Cancel",
                cancelPendingIntent
            )
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun updateForegroundNotification() {
        val primaryId = primaryNotificationSongId
        if (primaryId != null) {
            val title = activeDownloads[primaryId] ?: "Unknown"
            // We might not have progress immediately, allow observeDownloadProgress to catch up
            // But we must startForeground immediately
            try {
                startForeground(NOTIFICATION_ID, createProgressNotification(title, 0))
            } catch (e: Exception) {
                // If service is not foreground-allowed etc.
            }
        }
    }
    
    private fun updateProgressNotification(songTitle: String, progress: Int) {
        val notification = createProgressNotification(songTitle, progress)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun showCompleteNotification(songTitle: String, success: Boolean) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (success) "Download complete" else "Download failed")
            .setContentText(songTitle)
            .setSmallIcon(
                if (success) android.R.drawable.stat_sys_download_done 
                else android.R.drawable.stat_notify_error
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        notificationManager.notify(COMPLETE_NOTIFICATION_ID, notification)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        progressJob?.cancel()
        downloadJobs.values.forEach { it.cancel() }
        serviceScope.cancel()
    }
}
