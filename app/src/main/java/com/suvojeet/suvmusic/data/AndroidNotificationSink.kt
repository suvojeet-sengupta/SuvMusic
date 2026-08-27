package com.suvojeet.suvmusic.data

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.suvojeet.suvmusic.core.domain.notification.AppNotification
import com.suvojeet.suvmusic.core.domain.notification.NotificationSink
import javax.inject.Inject
import javax.inject.Singleton

/** Android host adapter for shared feature notifications. */
@Singleton
class AndroidNotificationSink @Inject constructor(
    private val context: Context,
) : NotificationSink {
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
