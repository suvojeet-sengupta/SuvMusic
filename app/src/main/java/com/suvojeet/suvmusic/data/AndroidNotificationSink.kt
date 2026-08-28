package com.suvojeet.suvmusic.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import com.suvojeet.suvmusic.core.domain.notification.AppNotification
import com.suvojeet.suvmusic.core.domain.notification.NotificationSink
import javax.inject.Inject
import javax.inject.Singleton

/** Android host adapter for shared feature notifications. */
@Singleton
class AndroidNotificationSink @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : NotificationSink {
    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "SuvMusic shared notifications",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Playback, downloads, and library updates"
                },
            )
        }
    }

    override fun show(notification: AppNotification) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(notification.ongoing)
            .setOnlyAlertOnce(true)
        notification.progress?.let { builder.setProgress(100, it.coerceIn(0, 100), false) }
        NotificationManagerCompat.from(context).notify(notification.id, builder.build())
    }

    override fun dismiss(id: Int) {
        NotificationManagerCompat.from(context).cancel(id)
    }

    private companion object {
        const val CHANNEL_ID = "suvmusic_shared"
    }
}
