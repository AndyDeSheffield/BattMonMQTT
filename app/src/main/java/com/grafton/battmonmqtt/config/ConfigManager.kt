package com.grafton.battmonmqtt.config
import com.grafton.battmonmqtt.config.MqttConfig

import android.content.Context

object ConfigManager {
    fun load(context: Context): MqttConfig {
        // Temporary hardcoded values; replace with DataStore later
        return MqttConfig(
            host = "localhost",
            port = 1883,
            username = "",
            password = "",
            topic = "battery/telemetry"
        )
    }
    fun save(context: Context, config: MqttConfig) {
        // Temporary no‑op stub just to compile
        // Later: persist with DataStore or SharedPreferences
    }
}