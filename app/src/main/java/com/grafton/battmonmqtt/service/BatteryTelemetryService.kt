package com.grafton.battmonmqtt.service

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.grafton.battmonmqtt.R
import com.grafton.battmonmqtt.config.ConfigManager
import com.grafton.battmonmqtt.mqtt.MqttManager
import kotlinx.coroutines.*

class BatteryTelemetryService : Service() {

    companion object {
        const val ACTION_SERVICE_STARTED = "com.grafton.battmonmqtt.SERVICE_STARTED"
        const val ACTION_SERVICE_STOPPED = "com.grafton.battmonmqtt.SERVICE_STOPPED"
    }

    private var mqttManager: MqttManager? = null
    private var lastChargingState: Int? = null
    private var discoveryPublished = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // If ConfigManager.load is suspend, call it inside a coroutine
        serviceScope.launch {
            val config = ConfigManager.load(this@BatteryTelemetryService)
            mqttManager = MqttManager(this@BatteryTelemetryService, config)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, "telemetry_channel")
            .setContentTitle("Battery Telemetry")
            .setContentText("Publishing MQTT updates")
            .setSmallIcon(R.drawable.ic_battery_foreground)
            .build()

        startForeground(1, notification)
        // broadcast service started
        sendBroadcast(Intent(ACTION_SERVICE_STARTED))
        publishTelemetry()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel() // cancel coroutines when service stops
        // Broadcast service stopped
        sendBroadcast(Intent(ACTION_SERVICE_STOPPED))
    }

    private fun publishTelemetry() {
        serviceScope.launch {
            while (isActive) {
                val manager = mqttManager
                if (manager == null) {
                    delay(5_000)
                    continue
                }

                if (!manager.connected.get() && !manager.connecting.get()) {
                    manager.connect()
                    delay(5_000)
                }

                if (manager.connected.get()) {
                    if (!discoveryPublished) {
                        manager.publishDiscovery()
                        discoveryPublished = true
                    }


                    val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
                    val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    val charging = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
                    val temp = bm.getTemperature(this@BatteryTelemetryService)

                    manager.publishTelemetry(pct, temp, charging)

                    if (lastChargingState == null || lastChargingState != charging) {
                        manager.publishTelemetry(pct, temp, charging)
                        lastChargingState = charging
                    }

                    delay(60_000)
                } else {
                    discoveryPublished = false
                    delay(600_000)
                }
            }
        }
    }
}

// helper extensions

fun BatteryManager.getTemperature(context: Service): Float {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
    return if (temp > 0) temp / 10f else 0f
}
