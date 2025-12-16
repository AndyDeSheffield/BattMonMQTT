package com.grafton.battmonmqtt.service
import android.app.Service
import android.content.Intent
import android.os.IBinder

class BatteryTelemetryService : Service() {
    override fun onCreate() {
        super.onCreate()
        // Start foreground notification
        // Launch coroutine scope for MQTT loop
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Load config, start MqttManager, schedule BatteryMonitor
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // Disconnect MQTT cleanly
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
