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
    private var downloadJob: Job? = null
    private var currentDownloadingSong: Song? = null
    
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
                    cancelCurrentDownload(songId)
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
        currentDownloadingSong = song
        
        // Start foreground with initial notification
        startForeground(NOTIFICATION_ID, createProgressNotification(song.title, 0))
        
        downloadJob = serviceScope.launch {
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
                currentDownloadingSong = null
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }
    
    private fun observeDownloadProgress() {
        progressJob = serviceScope.launch {
            downloadRepository.downloadProgress.collectLatest { progressMap ->
                val song = currentDownloadingSong ?: return@collectLatest
                val progress = progressMap[song.id] ?: return@collectLatest
                
                val progressPercent = (progress * 100).toInt()
                updateProgressNotification(song.title, progressPercent)
            }
        }
    }
    
    private fun cancelCurrentDownload(songId: String) {
        if (currentDownloadingSong?.id == songId) {
            downloadJob?.cancel()
            currentDownloadingSong = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
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
            putExtra("song_id", currentDownloadingSong?.id)
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            1,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Downloading")
            .setContentText(songTitle)
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
        downloadJob?.cancel()
        serviceScope.cancel()
    }
}
