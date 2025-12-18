/**
 * MqttConfig
 *
 * A data class that holds all the MQTT connection details for the app:
 * broker host, port, username, password, topic, and device ID. It is
 * marked @Serializable so it can be easily saved and loaded using
 * Kotlin serialization with DataStore.
 */


package com.grafton.battmonmqtt.config

import kotlinx.serialization.Serializable

@Serializable
data class MqttConfig(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val topic: String,
    val deviceId: String
)


