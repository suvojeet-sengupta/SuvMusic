package com.suvojeet.suvmusic.core.domain.notification

data class AppNotification(
    val id: Int,
    val title: String,
    val body: String,
    val progress: Int? = null,
    val ongoing: Boolean = false,
)

/** Platform-neutral user notification boundary. */
interface NotificationSink {
    fun show(notification: AppNotification)
    fun dismiss(id: Int)
}
