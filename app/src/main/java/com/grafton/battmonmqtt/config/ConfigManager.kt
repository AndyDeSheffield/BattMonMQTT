package com.grafton.battmonmqtt.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

// Serializer for MqttConfig
object MqttConfigSerializer : Serializer<MqttConfig> {
    override val defaultValue: MqttConfig = MqttConfig("localhost", 1883, "", "", "battery/telemetry")

    override suspend fun readFrom(input: InputStream): MqttConfig =
        try {
            Json.decodeFromString(MqttConfig.serializer(), input.readBytes().decodeToString())
        } catch (e: Exception) {
            defaultValue
        }

    override suspend fun writeTo(t: MqttConfig, output: OutputStream) {
        output.write(Json.encodeToString(MqttConfig.serializer(), t).encodeToByteArray())
    }
}

// Extension property for DataStore
private val Context.mqttConfigDataStore: DataStore<MqttConfig> by dataStore(
    fileName = "mqtt_config.json",
    serializer = MqttConfigSerializer
)

object ConfigManager {
    suspend fun load(context: Context): MqttConfig {
        return context.mqttConfigDataStore.data.first()
    }

    suspend fun save(context: Context, config: MqttConfig) {
        context.mqttConfigDataStore.updateData { config }
    }
}
