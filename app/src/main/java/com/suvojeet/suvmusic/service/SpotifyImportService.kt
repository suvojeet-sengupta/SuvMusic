package com.suvojeet.suvmusic.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.suvojeet.suvmusic.MainActivity
import com.suvojeet.suvmusic.R
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import com.suvojeet.suvmusic.util.SpotifyImportHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class ImportStatus(
    val state: State = State.IDLE,
    val currentSong: String = "",
    val currentArtist: String = "",
    val thumbnail: String? = null,
    val total: Int = 0,
    val progress: Int = 0,
    val successCount: Int = 0,
    val error: String? = null
) {
    enum class State {
        IDLE, PREPARING, PROCESSING, COMPLETED, ERROR, CANCELLED
    }
}

@AndroidEntryPoint
class SpotifyImportService : Service() {

    @Inject
    lateinit var spotifyImportHelper: SpotifyImportHelper

    @Inject
    lateinit var youTubeRepository: YouTubeRepository

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private var currentJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val url = intent.getStringExtra(EXTRA_URL)
                if (url != null) {
                    startImport(url)
                }
            }
            ACTION_CANCEL -> {
                cancelImport()
            }
        }
        return START_NOT_STICKY
    }

    private fun startImport(url: String) {
        if (currentJob?.isActive == true) return

        startForeground(NOTIFICATION_ID, buildNotification("Preparing import...", 0, 0, true))

        currentJob = serviceScope.launch {
            try {
                _importState.update { it.copy(state = ImportStatus.State.PREPARING, error = null) }
                updateNotification("Fetching playlist details...", 0, 0, true)

                val (playlistName, spotifySongs) = spotifyImportHelper.getPlaylistSongs(url)
                if (spotifySongs.isEmpty()) {
                    _importState.update { it.copy(state = ImportStatus.State.ERROR, error = "No songs found") }
                    stopSelf()
                    return@launch
                }

                // Append timestamp to avoid duplicates if importing same playlist multiple times, 
                // or just use the name if preferred. User asked for "same name".
                // We'll stick to the exact name as requested, or maybe append "(Imported)" if really needed, 
                // but user said "same name as Spotify playlist".
                // To be safe against duplicates, maybe we should check if it exists? 
                // But the requirement is "same name".
                val playlistTitle = playlistName 
                val playlistId = youTubeRepository.createPlaylist(playlistTitle, "Imported from Spotify via SuvMusic")

                if (playlistId == null) {
                    _importState.update { it.copy(state = ImportStatus.State.ERROR, error = "Failed to create playlist") }
                    stopSelf()
                    return@launch
                }

                val total = spotifySongs.size
                var successCount = 0

                _importState.update { it.copy(state = ImportStatus.State.PROCESSING, total = total, progress = 0) }

                spotifySongs.forEachIndexed { index, (title, artist) ->
                    if (!isActive) return@forEachIndexed

                    _importState.update { 
                        it.copy(
                            currentSong = title,
                            currentArtist = artist,
                            progress = index + 1,
                            thumbnail = null // Could be updated if we had art
                        )
                    }
                    updateNotification("Importing: $title", index + 1, total, false)

                    // Find Match
                    val match = spotifyImportHelper.findMatch(title, artist)
                    
                    if (match != null) {
                         _importState.update { it.copy(thumbnail = match.thumbnailUrl) } // Update thumbnail
                         val added = youTubeRepository.addSongToPlaylist(playlistId, match.id)
                         if (added) successCount++
                    }
                    
                    // Small artificial delay for very fast loops to allow UI/Notif to update
                    delay(50) 
                }

                _importState.update { 
                    it.copy(
                        state = ImportStatus.State.COMPLETED,
                        successCount = successCount
                    ) 
                }
                showCompletionNotification(successCount, total)

            } catch (e: CancellationException) {
                _importState.update { it.copy(state = ImportStatus.State.CANCELLED) }
            } catch (e: Exception) {
                _importState.update { it.copy(state = ImportStatus.State.ERROR, error = e.message) }
            } finally {
                stopForeground(true)
                if (_importState.value.state != ImportStatus.State.PROCESSING) {
                    stopSelf()
                }
            }
        }
    }

    private fun cancelImport() {
        currentJob?.cancel()
        stopForeground(true)
        stopSelf()
        _importState.update { it.copy(state = ImportStatus.State.CANCELLED) }
    }

    private fun buildNotification(title: String, progress: Int, max: Int, indeterminate: Boolean): android.app.Notification {
        val cancelIntent = Intent(this, SpotifyImportService::class.java).apply {
            action = ACTION_CANCEL
        }
        val pendingCancelIntent = PendingIntent.getService(
            this, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle("Spotify Import")
            .setContentText(title)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .addAction(R.drawable.ic_close, "Cancel", pendingCancelIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (max > 0) {
            builder.setProgress(max, progress, indeterminate)
        } else {
             builder.setProgress(0, 0, true)
        }

        return builder.build()
    }

    private fun updateNotification(title: String, progress: Int, max: Int, indeterminate: Boolean) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(title, progress, max, indeterminate))
    }
    
    private fun showCompletionNotification(successCount: Int, total: Int) {
         val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
         val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle("Import Completed")
            .setContentText("Successfully imported $successCount of $total songs.")
            .setAutoCancel(true)
            .build()
         notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Spotify Import",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of Spotify playlist import"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    companion object {
        const val CHANNEL_ID = "spotify_import_channel"
        const val NOTIFICATION_ID = 1001
        
        const val ACTION_START = "com.suvojeet.suvmusic.action.START_IMPORT"
        const val ACTION_CANCEL = "com.suvojeet.suvmusic.action.CANCEL_IMPORT"
        const val EXTRA_URL = "com.suvojeet.suvmusic.extra.URL"

        private val _importState = MutableStateFlow(ImportStatus())
        val importState = _importState.asStateFlow()
    }
}
