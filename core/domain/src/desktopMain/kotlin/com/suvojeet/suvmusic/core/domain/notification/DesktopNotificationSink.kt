package com.suvojeet.suvmusic.core.domain.notification

import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import java.util.concurrent.ConcurrentHashMap

/** Linux/JVM desktop notification adapter using the host SystemTray when available. */
class DesktopNotificationSink : NotificationSink {
    private val icons = ConcurrentHashMap<Int, TrayIcon>()
    private val tray: SystemTray? = runCatching {
        if (SystemTray.isSupported()) SystemTray.getSystemTray() else null
    }.getOrNull()

    override fun show(notification: AppNotification) {
        val systemTray = tray ?: return
        val icon = icons.computeIfAbsent(notification.id) {
            TrayIcon(ICON, "SuvMusic").also { it.isImageAutoSize = true }
        }
        runCatching {
            if (!systemTray.trayIcons.contains(icon)) systemTray.add(icon)
            icon.displayMessage(notification.title, notification.body, TrayIcon.MessageType.INFO)
        }
    }

    override fun dismiss(id: Int) {
        val icon = icons.remove(id) ?: return
        runCatching { tray?.remove(icon) }
    }

    private companion object {
        val ICON = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
    }
}
