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

        com.example.notifcollector.notifications.logRawNotification(sbn)

        val pkg = sbn.packageName ?: return
        val extras = sbn.notification.extras

        val title = (extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "").trim()
        val text  = (extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "").trim()
        val big   = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: "").trim()

        // Parseo normalizado → JSON
        val parsed = NotificationParser.parse(
            packageName = pkg,
            title = title,
            text = text,
            bigText = big,
            occurredAt = sbn.postTime
        )

        if (parsed != null) {
            Timber.i("EVENT_JSON %s", parsed.toJson())
            // Más adelante: mapear a EventEntity y guardar/enviar
            // val entity = ...
            // repo.saveEventLocal(entity); enqueue UploadWorker...
        } else {
            Timber.i("Parser no reconoció formato para pkg=%s title=%s text=%s big=%s", pkg, title, text, big)
        }
    }
}
