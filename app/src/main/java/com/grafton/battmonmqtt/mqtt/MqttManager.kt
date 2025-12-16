package com.grafton.battmonmqtt.mqtt
import com.grafton.battmonmqtt.config.MqttConfig
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import java.util.concurrent.atomic.AtomicBoolean


class MqttManager(private val config: MqttConfig) {
    private var client: Mqtt3AsyncClient? = null
    private val connected = AtomicBoolean(false)

    fun connect(onConnected: () -> Unit, onDisconnected: () -> Unit) {
        // Build client, connect with credentials
        // Set callbacks to update connected flag
    }

    fun publish(topic: String, payload: String) {
        if (!connected.get()) {
            // Attempt reconnect, abort if fails
        }
        // Publish message
    }

    fun disconnect() {
        client?.disconnect()
        connected.set(false)
    }
}
