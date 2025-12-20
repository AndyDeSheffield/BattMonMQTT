/**
 * MqttManager
 *
 * Handles all MQTT communication for the app. It manages connecting to the broker,
 * tracking connection state, and publishing messages. This includes sending battery
 * telemetry values (charge, temperature, status) and publishing Home Assistant
 * discovery data so the sensors appear automatically. Acts as the bridge between
 * the Android service and the MQTT broker.
 */


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
          return "Battery_${config.deviceId}"
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

        // Battery charge percentage
        // was "object_id": "${baseTopic}.battery.charge",
        publish(
            "homeassistant/sensor/${baseTopic}/charge/config",
            """
        {
          "name": "${baseTopic} Battery Charge",
          "state_topic": "${baseTopic}/charge",
          "default_entity_id": "sensor.${baseTopic}.battery.charge",
          "unit_of_measurement": "%",
          "device_class": "battery",
          "unique_id": "${baseTopic}_battery_charge"
        }
        """.trimIndent()
        )

        // Battery temperature
        // was "object_id": "${baseTopic}.battery.temperature"
        publish(
            "homeassistant/sensor/${baseTopic}/temperature/config",
            """
        {
          "name": "${baseTopic} Battery Temperature",
          "state_topic": "${baseTopic}/temperature",
          "default_entity_id": "sensor.${baseTopic}.battery.temperature",
          "unit_of_measurement": "°C",
          "device_class": "temperature",
          "unique_id": "${baseTopic}_battery_temperature"
        }
        """.trimIndent()
        )

        // Battery status (multi-state string sensor)
        //was "object_id": "${baseTopic}.battery.status",
        publish(
            "homeassistant/sensor/${baseTopic}/status/config",
            """
        {
          "name": "${baseTopic} Battery Status",
          "state_topic": "${baseTopic}/status",
          "default_entity_id": "sensor.${baseTopic}.battery.status",
          "icon": "mdi:battery",
          "unique_id": "${baseTopic}_battery_status"
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
