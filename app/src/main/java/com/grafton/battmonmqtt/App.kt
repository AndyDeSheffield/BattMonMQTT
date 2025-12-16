package com.grafton.battmonmqtt

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        // Create your notification channel here
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "telemetry_channel",
                "Battery Telemetry",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            // 🔎 Test notification right here
         /*   val testNotification = NotificationCompat.Builder(this, "telemetry_channel")
                .setContentTitle("Debug Notification")
                .setContentText("This is a test from App.onCreate")
                .setSmallIcon(android.R.drawable.ic_dialog_alert) // use built-in icon for visibility
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            manager.notify(999, testNotification) // arbitrary ID */
        }
    }
}
