package com.suvojeet.suvmusic.workers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.suvojeet.suvmusic.MainActivity
import com.suvojeet.suvmusic.R
import com.suvojeet.suvmusic.data.repository.LibraryRepository
import com.suvojeet.suvmusic.data.repository.YouTubeRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class NewReleaseWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val libraryRepository: LibraryRepository,
    private val youTubeRepository: YouTubeRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val followedArtists = libraryRepository.getSavedArtists().first()
            if (followedArtists.isEmpty()) return Result.success()

            val prefs = applicationContext.getSharedPreferences("new_release_prefs", Context.MODE_PRIVATE)
            val editor = prefs.edit()

            // Shuffle to vary checks, take 5 to save bandwidth
            val artistsToCheck = followedArtists.shuffled().take(5)

            artistsToCheck.forEach { artistEntity ->
                try {
                    val artistDetails = youTubeRepository.getArtist(artistEntity.id) ?: return@forEach
                    
                    // Check for new Albums or Singles
                    val latestAlbum = artistDetails.albums.firstOrNull() ?: artistDetails.singles.firstOrNull()
                    
                    if (latestAlbum != null) {
                        val lastKnownId = prefs.getString("last_release_${artistEntity.id}", "")
                        
                        // Heuristic: Check if this is truly "new". 
                        // YouTube Music doesn't give exact release date easily in standard browse, 
                        // but usually "Year" is available.
                        // We will notify IF the ID is different from last known AND year is recent (e.g. 2024+)
                        // Note: For now, we trust the "Last Known ID" check mainly.
                        
                        // Condition: ID changed AND ID is not empty AND we had a previous record (or it's the first run, we might skip to avoid spamming on first install).
                        // To avoid spamming on first run:
                        if (lastKnownId == "") {
                            // First time checking this artist: just save current as known, don't notify
                            editor.putString("last_release_${artistEntity.id}", latestAlbum.id)
                        } else if (latestAlbum.id != lastKnownId) {
                            // New release found!
                            showNotification(artistEntity.title, latestAlbum.title, latestAlbum.id, latestAlbum.thumbnailUrl)
                            editor.putString("last_release_${artistEntity.id}", latestAlbum.id)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            editor.apply()
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
    
    private fun showNotification(artistName: String, releaseTitle: String, albumId: String, thumbnailUrl: String?) {
        val channelId = "new_releases_channel"
        val notificationId = albumId.hashCode()

        // exact navigation intent
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // We pass data to navigate to the album
            // Deep link handling might be needed in MainActivity or simple logic
            putExtra("navigation_route", "album/$albumId") 
        }
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_music_note) // Ensure this icon exists
            .setContentTitle("New Release from $artistName")
            .setContentText(releaseTitle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            
        // Note: Image loading for LargeIcon (thumbnail) would require Coil/Glide sync loading, 
        // omitted here for brevity but recommended for polish.

        // Create Channel (Safe to call repeatedly)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "New Releases"
            val descriptionText = "Notifications for new music releases"
            val importance = android.app.NotificationManager.IMPORTANCE_DEFAULT
            val channel = android.app.NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: android.app.NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        with(NotificationManagerCompat.from(applicationContext)) {
            try {
                notify(notificationId, builder.build())
            } catch (e: SecurityException) {
                // Handle missing permission
            }
        }
    }
}
