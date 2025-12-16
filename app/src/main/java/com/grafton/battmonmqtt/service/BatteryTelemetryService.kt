package com.grafton.battmonmqtt.service
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.grafton.battmonmqtt.R


class BatteryTelemetryService : Service() {

    override fun onCreate() {
        super.onCreate()
        // Initialize MQTT client here using ConfigManager.load(...)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Build persistent notification
        val notification = NotificationCompat.Builder(this, "telemetry_channel")
            .setContentTitle("Battery Telemetry")
            .setContentText("Publishing MQTT updates")
            .setSmallIcon(R.drawable.ic_battery_foreground)
            .build()

        // Start foreground service
        startForeground(1, notification)

        // Kick off telemetry publishing coroutine
        publishTelemetry()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun publishTelemetry() {
        // Example: launch coroutine to send battery data periodically
    }
}
