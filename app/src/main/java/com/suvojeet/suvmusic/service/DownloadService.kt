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
import com.suvojeet.suvmusic.core.model.Song
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
        private const val ACTION_PROCESS_QUEUE = "com.suvojeet.suvmusic.PROCESS_QUEUE"
        private const val ACTION_CANCEL_DOWNLOAD = "com.suvojeet.suvmusic.CANCEL_DOWNLOAD"
        private const val EXTRA_SONG_JSON = "song_json"
        
        fun startDownload(context: Context, song: Song) {
            // For single download, we just add to queue and start batch processing
            // preventing parallel logic issues.
            // However, we need to access repository which we can't from static context easily
            // except via the Service instance or if caller uses repository.
            
            // To be safe and minimal change for existing calls not using repository directly (if any):
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
        
        fun startBatchDownload(context: Context) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_PROCESS_QUEUE
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
    private var batchJob: Job? = null
    
    // Track active downloads: ID -> Title
    private val activeDownloads = java.util.concurrent.ConcurrentHashMap<String, String>()
    
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
        // Fix for Android 12+ ForegroundServiceDidNotStartInTimeException
        // We must call startForeground immediately
        startForeground(NOTIFICATION_ID, createProgressNotification("Starting service...", 0, 0, 0))

        when (intent?.action) {
            ACTION_START_DOWNLOAD -> {
                val songJson = intent.getStringExtra(EXTRA_SONG_JSON)
                val song = songJson?.let { jsonToSong(it) }
                if (song != null) {
                    // Treat single download as adding to queue
                    downloadRepository.downloadSongToQueue(song)
                    processQueue()
                }
            }
            ACTION_PROCESS_QUEUE -> {
                processQueue()
            }
            ACTION_CANCEL_DOWNLOAD -> {
                val songId = intent.getStringExtra("song_id")
                if (songId != null) {
                    cancelDownload(songId)
                }
            }
        }
        return START_NOT_STICKY
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

    private fun processQueue() {
        if (batchJob?.isActive == true) return
        
        batchJob = serviceScope.launch {
            Log.d(TAG, "Starting queue processing")
            
            while (true) {
                val song = downloadRepository.popFromQueue()
                if (song == null) {
                    Log.d(TAG, "Queue empty, stopping service")
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    break
                }
                
                // Update batch progress tracking
                val (done, total) = downloadRepository.batchProgress.value
                val newDone = done + 1
                downloadRepository.updateBatchProgress(newDone, total)
                
                // Active tracking
                activeDownloads[song.id] = song.title
                primaryNotificationSongId = song.id
                
                // Initial notification
                updateForegroundNotification(song.title, 0, newDone, total)

                try {
                    Log.d(TAG, "Downloading: ${song.title}")
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
                     activeDownloads.remove(song.id)
                     if (primaryNotificationSongId == song.id) {
                         primaryNotificationSongId = null
                     }
                }
            }
        }
    }
    
    private fun observeDownloadProgress() {
        serviceScope.launch {
            downloadRepository.downloadProgress.collectLatest { progressMap ->
                val primaryId = primaryNotificationSongId ?: return@collectLatest
                val title = activeDownloads[primaryId] ?: return@collectLatest
                val progress = progressMap[primaryId] ?: 0f
                
                val (done, total) = downloadRepository.batchProgress.value
                // done is the one we are working on (1-based index roughly for UI if we treat done as 'current index')
                // Actually done includes the one we just finished? 
                // In loop: we grabbed popFromQueue, then incremented done. So done is "current song number".
                
                val progressPercent = (progress * 100).toInt()
                updateForegroundNotification(title, progressPercent, done, total)
            }
        }
    }
    
    private fun cancelDownload(songId: String) {
        // Fix: Broken Download Cancellation -> Delegate to Repository
        downloadRepository.cancelDownload(songId)
    }
    
    private fun createProgressNotification(songTitle: String, progress: Int, current: Int, total: Int): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val contentText = if (total > 1) {
            "($current/$total) $songTitle"
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
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun updateForegroundNotification(title: String, progress: Int, current: Int, total: Int) {
        try {
            val notification = createProgressNotification(title, progress, current, total)
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            // Service restart/foreground issues
        }
    }
    
    private fun updateProgressNotification(songTitle: String, progress: Int) {
        // Unused legacy helper, keeping for safety if needed or remove
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
        batchJob?.cancel()
        serviceScope.cancel()
    }
}
