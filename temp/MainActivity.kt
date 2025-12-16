package com.grafton.battmonmqtt.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.grafton.battmonmqtt.config.ConfigManager
import com.grafton.battmonmqtt.config.MqttConfig

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ConfigScreen(
                onSave = { cfg ->
                    // Pass a plain Context from Activity scope
                    ConfigManager.save(context = this, config = cfg)
                }
            )
        }
    }
}

@Composable
fun ConfigScreen(
    onSave: (MqttConfig) -> Unit
) {
    val context = LocalContext.current
    var config by remember {
        mutableStateOf(
            MqttConfig(
                host = "broker.example.com",
                port = 1883,
                username = "user",
                password = "pass",
                topic = "battery/telemetry"
            )
        )
    }
    Column {
        Text("MQTT Config Screen")

        // Example: save via the callback (Activity context),
        // or directly using the composable-provided context.
        Button(onClick = {
            // Option A: use the callback injected from Activity (preferred)
            onSave(config)

            // Option B: if you must call directly from here, still safe:
            // ConfigManager.save(context = context, config = config)
        }) {
            Text("Save Config")
        }
    }
}
