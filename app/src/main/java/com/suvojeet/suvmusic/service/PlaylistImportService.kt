package com.suvojeet.suvmusic.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.suvojeet.suvmusic.R
import com.suvojeet.suvmusic.core.model.Playlist
import com.suvojeet.suvmusic.core.model.Song
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import com.suvojeet.suvmusic.util.PlaylistImportHelper
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
    val failedSongs: List<Pair<String, String>> = emptyList(),
    val error: String? = null
) {
    enum class State {
        IDLE, PREPARING, PROCESSING, COMPLETED, ERROR, CANCELLED
    }
}

@AndroidEntryPoint
class PlaylistImportService : Service() {

    @Inject
    lateinit var playlistImportHelper: PlaylistImportHelper

    @Inject
    lateinit var spotifyImportHelper: com.suvojeet.suvmusic.util.SpotifyImportHelper

    @Inject
    lateinit var youTubeRepository: YouTubeRepository

    @Inject
    lateinit var libraryRepository: com.suvojeet.suvmusic.core.domain.repository.LibraryRepository

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
                val m3uUri = intent.getParcelableExtra<Uri>(EXTRA_M3U_URI)
                val suvUri = intent.getParcelableExtra<Uri>(EXTRA_SUV_URI)
                if (url != null) {
                    startImport(url = url)
                } else if (m3uUri != null) {
                    startImport(m3uUri = m3uUri)
                } else if (suvUri != null) {
                    startImport(suvUri = suvUri)
                }
            }
            ACTION_CANCEL -> {
                cancelImport()
            }
        }
        return START_NOT_STICKY
    }

    private fun startImport(url: String? = null, m3uUri: Uri? = null, suvUri: Uri? = null) {
        if (currentJob?.isActive == true) return

        startForeground(NOTIFICATION_ID, buildNotification("Preparing import...", 0, 0, true))

        currentJob = serviceScope.launch {
            try {
                _importState.update { it.copy(state = ImportStatus.State.PREPARING, error = null) }
                updateNotification("Fetching playlist details...", 0, 0, true)

                val (playlistName, importTracks) = when {
                    url != null -> {
                        playlistImportHelper.getPlaylistSongs(url) { count ->
                            _importState.update { it.copy(currentSong = "Fetched $count songs...") }
                            updateNotification("Fetching tracks: $count", 0, 0, true)
                        }
                    }
                    m3uUri != null -> {
                        playlistImportHelper.parseM3U(m3uUri)
                    }
                    suvUri != null -> {
                        playlistImportHelper.parseSUV(suvUri)
                    }
                    else -> "Imported Playlist" to emptyList()
                }
                
                if (importTracks.isEmpty()) {
                    _importState.update { it.copy(state = ImportStatus.State.ERROR, error = "No songs found") }
                    stopForeground(true)
                    stopSelf()
                    return@launch
                }

                // Always create a local playlist for imports as requested in Issue #52
                val finalPlaylistId = "local_" + java.util.UUID.randomUUID().toString()
                val isLocal = true
                val firstSongThumb = importTracks.firstOrNull { it.song != null }?.song?.thumbnailUrl
                libraryRepository.savePlaylist(
                    Playlist(
                        id = finalPlaylistId, 
                        title = playlistName, 
                        author = "You", 
                        thumbnailUrl = firstSongThumb, 
                        songs = emptyList()
                    )
                )

                val total = importTracks.size
                var successCount = 0
                val failedSongs = mutableListOf<Pair<String, String>>()

                _importState.update { it.copy(state = ImportStatus.State.PROCESSING, total = total, progress = 0) }

                // Check if all tracks already have song objects (e.g. from .suv import)
                val allHaveSongs = importTracks.isNotEmpty() && importTracks.all { it.song != null }
                
                if (allHaveSongs && isLocal) {
                    // Fast path for native formats
                    val matches = importTracks.mapNotNull { it.song }
                    libraryRepository.appendPlaylistSongs(finalPlaylistId!!, matches, 0)
                    successCount = matches.size
                    _importState.update { 
                        it.copy(
                            progress = total,
                            currentSong = "Imported $successCount songs",
                            thumbnail = matches.firstOrNull()?.thumbnailUrl
                        )
                    }
                    updateNotification("Imported $successCount songs", total, total, false)
                } else {
                    // Regular matching path
                    importTracks.forEachIndexed { index, track ->
                        if (!isActive) return@forEachIndexed
                        val title = track.title
                        val artist = track.artist

                        _importState.update { 
                            it.copy(
                                currentSong = title,
                                currentArtist = artist,
                                progress = index + 1,
                                thumbnail = null
                            )
                        }
                        updateNotification("Importing: $title", index + 1, total, false)

                        // Find Match or use direct song/sourceId
                        val match = when {
                            track.song != null -> track.song
                            track.sourceId != null -> youTubeRepository.getSongDetails(track.sourceId)
                            else -> spotifyImportHelper.findMatch(title, artist, track.durationMs)
                        }
                        
                        if (match != null) {
                             _importState.update { it.copy(thumbnail = match.thumbnailUrl) }
                             val added = if (isLocal) {
                                 libraryRepository.addSongToPlaylist(finalPlaylistId!!, match)
                                 true
                             } else {
                                 youTubeRepository.addSongToPlaylist(finalPlaylistId!!, match.id)
                             }
                             if (added) {
                                 successCount++
                             } else {
                                 failedSongs.add(title to artist)
                             }
                        } else {
                            failedSongs.add(title to artist)
                        }
                        
                        delay(50) 
                    }
                }

                _importState.update { 
                    it.copy(
                        state = ImportStatus.State.COMPLETED,
                        successCount = successCount,
                        failedSongs = failedSongs
                    ) 
                }
                showCompletionNotification(successCount, total)

            } catch (e: CancellationException) {
                _importState.update { it.copy(state = ImportStatus.State.CANCELLED) }
            } catch (e: Exception) {
                _importState.update { it.copy(state = ImportStatus.State.ERROR, error = e.message) }
            } finally {
                stopForeground(true)
                stopSelf()
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
        val cancelIntent = Intent(this, PlaylistImportService::class.java).apply {
            action = ACTION_CANCEL
        }
        val pendingCancelIntent = PendingIntent.getService(
            this, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle("Playlist Import")
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
                "Playlist Import",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of playlist import"
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
        const val CHANNEL_ID = "playlist_import_channel"
        const val NOTIFICATION_ID = 1001
        
        const val ACTION_START = "com.suvojeet.suvmusic.action.START_IMPORT"
        const val ACTION_CANCEL = "com.suvojeet.suvmusic.action.CANCEL_IMPORT"
        const val EXTRA_URL = "com.suvojeet.suvmusic.extra.URL"
        const val EXTRA_M3U_URI = "com.suvojeet.suvmusic.extra.M3U_URI"
        const val EXTRA_SUV_URI = "com.suvojeet.suvmusic.extra.SUV_URI"

        private val _importState = MutableStateFlow(ImportStatus())
        val importState = _importState.asStateFlow()
    }
}
