package com.grafton.battmonmqtt.mqtt

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import com.grafton.battmonmqtt.config.MqttConfig
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import android.os.BatteryManager

class MqttManager(private val context: Context, private val config: MqttConfig) {
    private var client: Mqtt3AsyncClient? = null
    val connected = AtomicBoolean(false)   // public so service can check
    val connecting = AtomicBoolean(false) // true while a connect attempt is in progress
    @SuppressLint("HardwareIds")
    @Suppress("DEPRECATION")
    private fun getBaseTopic(): String {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val mac = wifiManager.connectionInfo.macAddress ?: "00:00:00:00:00:00"
        return "Battery_${mac.lowercase().replace(":", "_")}"
    }

    fun connect() {
        if (connecting.get()) return // avoid overlapping attempts
        connecting.set(true)
        client = MqttClient.builder()
            .useMqttVersion3()
            .serverHost(config.host)
            .serverPort(config.port)
            .addConnectedListener { onConnected() }
            .addDisconnectedListener { onDisconnected() }
            .buildAsync()

        client?.connectWith()
            ?.simpleAuth()
            ?.username(config.username)
            ?.password(config.password.toByteArray())
            ?.applySimpleAuth()
            ?.send()
            ?.whenComplete { _, throwable ->
                if (throwable == null) {
                    onConnected()
                } else {
                    onDisconnected()
                }

            }
    }

    fun publish(topic: String, payload: String, retained: Boolean = true) {
        if (!connected.get()) return
        client?.publish(
            Mqtt3Publish.builder()
                .topic(topic)
                .payload(payload.toByteArray(StandardCharsets.UTF_8))
                .qos(com.hivemq.client.mqtt.datatypes.MqttQos.AT_LEAST_ONCE)
                .retain(retained)
                .build()
        )
    }

    fun disconnect() {
        client?.disconnect()
        onDisconnected()
    }

    // 🔎 Explicit lifecycle functions
    private fun onConnected() {
        connected.set(true)
        connecting.set(false) // ensure cleared if broker drops

    }

    private fun onDisconnected() {
        connected.set(false)
        connecting.set(false) // ensure cleared if broker drops
        // optional: log, retry, or notify service
    }

    fun publishDiscovery() {
        val baseTopic = getBaseTopic()

        publish(
            "homeassistant/sensor/${baseTopic}/charge/config",
            """
            {
              "name": "Battery Charge",
              "state_topic": "${baseTopic}/charge",
              "unit_of_measurement": "%",
              "device_class": "battery"
            }
            """.trimIndent()
        )

        publish(
            "homeassistant/sensor/${baseTopic}/temperature/config",
            """
            {
              "name": "Battery Temperature",
              "state_topic": "${baseTopic}/temperature",
              "unit_of_measurement": "°C",
              "device_class": "temperature"
            }
            """.trimIndent()
        )

        publish(
            "homeassistant/binary_sensor/${baseTopic}/status/config",
            """
            {
              "name": "Battery Status",
              "state_topic": "${baseTopic}/status",
              "icon: "mdi:battery",
              "payload_off": "discharging",
              "device_class": "battery"
            }
            """.trimIndent()
        )
    }

    fun publishTelemetry(batteryPct: Int, batteryTemp: Float, status: Int) {
        val baseTopic = getBaseTopic()
        publish("${baseTopic}/charge", batteryPct.toString())
        publish("${baseTopic}/temperature", batteryTemp.toString())

        val statusStr = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "not_charging"
            else -> "unknown"
        }
        publish("${baseTopic}/status", statusStr)
    }
}
