package com.example.notifcollector.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import timber.log.Timber

class TransactionListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        Timber.i("🔌 NotificationListener CONNECTED")
    }
    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Timber.w("🔌 NotificationListener DISCONNECTED")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {

        val pkg = sbn.packageName ?: return
        val extras = sbn.notification.extras

        val title = (extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "").trim()
        val text  = (extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "").trim()
        val big   = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: "").trim()

        Timber.i("📬 pkg=%s | title=%s | text=%s | big=%s",
            pkg, title, text, big)
        val body = listOf(title, if (big.isNotEmpty()) big else text)
            .filter { it.isNotEmpty() }
            .joinToString(" | ")

        Timber.i("📬 Notif pkg=%s | %s", pkg, body)

        // TODO: más adelante llamamos a los parsers y persistimos/enviamos
    }
}
