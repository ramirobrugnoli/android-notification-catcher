package com.example.notifcollector
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

import android.app.Application
import timber.log.Timber

class NotifApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())

        // Canal para las notificaciones de prueba
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                "test",
                "Pruebas",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }
}


