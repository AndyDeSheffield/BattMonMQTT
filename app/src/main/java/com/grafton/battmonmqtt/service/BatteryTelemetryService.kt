/**
 * BatteryTelemetryService
 *
 * A foreground Android service that monitors the device's battery level,
 * charging state, and temperature, then publishes this information to
 * an MQTT broker. It also announces itself to Home Assistant via MQTT
 * discovery so the battery data can be integrated into automations.
 * Runs continuously in the background and sends updates either when
 * values change or at regular intervals.
 */

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
const val pingpublishdelay :Int = 60  //publish every 60 seconds regardless of power state changes

class BatteryTelemetryService : Service() {

    companion object {
        const val ACTION_SERVICE_STARTED = "com.grafton.battmonmqtt.SERVICE_STARTED"
        const val ACTION_SERVICE_STOPPED = "com.grafton.battmonmqtt.SERVICE_STOPPED"
    }
    private var mqttManager: MqttManager? = null
    private var lastChargingState: String? = null
    private var discoveryPublished = false
    private var pingpublish :Int = pingpublishdelay
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
            //wait for config file to load
            while (isActive) {
                val manager = mqttManager
                if (manager == null) {
                    delay(5_000)
                    continue
                }
                //try an connect if not connected
                if (!manager.connected.get() && !manager.connecting.get()) {
                    manager.connect()
                    while (manager.connecting.get()) {
                        delay(500)
                    }
                }
                //publish discovery data if not published
                if (manager.connected.get()) {
                    if (!discoveryPublished) {
                        manager.publishDiscovery()
                        discoveryPublished = true
                    }

                    //get the battery status variables that we want
                    val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
                    val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    val chargingstate = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
                    val temp = bm.getTemperature(this@BatteryTelemetryService)
                    val allstate= "%02d|%03d|%05.2f".format(pct, chargingstate, temp)
                    //do a publish every <pingpublishdelay> seconds regardless of power state changes
                    if (pingpublish <=0) {
                        manager.publishTelemetry(pct, temp, chargingstate)
                        pingpublish=pingpublishdelay
                    }
                    // do an immediate publish if something changes
                    if (lastChargingState == null || lastChargingState != allstate) {
                        manager.publishTelemetry(pct, temp, chargingstate)
                        lastChargingState = allstate
                        pingpublish=pingpublishdelay
                    }

                    delay(1000)
                    pingpublish--
                } else {  //If we cant connect wait a minute and try again
                    discoveryPublished = false
                    pingpublish=pingpublishdelay
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
