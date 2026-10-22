package com.example.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class HexzieNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (instance == this) instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {}

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}

    fun getActiveNotificationSummaries(): List<String> {
        val active = activeNotifications ?: return emptyList()
        val list = mutableListOf<String>()
        for (n in active) {
            val title = n.notification.extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString()
            val text = n.notification.extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString()
            if (!title.isNullOrBlank() || !text.isNullOrBlank()) {
                list.add("[${n.packageName}] $title: $text")
            }
        }
        return list
    }

    companion object {
        var instance: HexzieNotificationListener? = null
            private set
    }
}
