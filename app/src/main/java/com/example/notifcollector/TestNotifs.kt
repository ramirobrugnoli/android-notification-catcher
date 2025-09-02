package com.example.notifcollector

import android.Manifest
import android.R
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlin.random.Random

object TestNotifs {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun sendExample(context: Context) {
        val title = "¡Acreditamos tu dinero!"
        val big   = "Recibiste $ 12.450,00 de Juan Perez. Referencia: TRANS-8942"

        val notif = NotificationCompat.Builder(context, "test")
            .setSmallIcon(R.drawable.stat_notify_more)
            .setContentTitle(title)
            .setContentText(big.take(40)) // preview corto
            .setStyle(NotificationCompat.BigTextStyle().bigText(big)) // BigText real
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(Random.nextInt(), notif)
    }
}
