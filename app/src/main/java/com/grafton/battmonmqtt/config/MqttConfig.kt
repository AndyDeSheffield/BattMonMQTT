package com.grafton.battmonmqtt.config

import kotlinx.serialization.Serializable

@Serializable
data class MqttConfig(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val topic: String
)