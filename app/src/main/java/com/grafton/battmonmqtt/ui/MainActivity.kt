package com.grafton.battmonmqtt.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.grafton.battmonmqtt.R
import com.grafton.battmonmqtt.config.ConfigManager
import com.grafton.battmonmqtt.config.MqttConfig
import com.grafton.battmonmqtt.service.BatteryTelemetryService
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ConfigScreen()
        }
    }
}

@Composable
fun ConfigScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("1883") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    // Load existing config once when the screen starts
    LaunchedEffect(Unit) {
        val existing = ConfigManager.load(context)
        existing?.let {
            host = it.host
            port = it.port.toString()
            username = it.username
            password = it.password
            topic = it.topic
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("MQTT Config Screen", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = host,
            onValueChange = { host = it },
            label = { Text("Host (IP)") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )

        OutlinedTextField(
            value = port,
            onValueChange = { port = it },
            label = { Text("Port") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation()
        )

        Row(modifier = Modifier.padding(top = 4.dp)) {
            Checkbox(
                checked = showPassword,
                onCheckedChange = { showPassword = it }
            )
            Text("Show password", modifier = Modifier.padding(start = 8.dp))
        }

        OutlinedTextField(
            value = topic,
            onValueChange = { topic = it },
            label = { Text("Topic") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )

        Button(
            onClick = {
                val cfg = MqttConfig(
                    host = host,
                    port = port.toIntOrNull() ?: 1883,
                    username = username,
                    password = password,
                    topic = topic
                )
                scope.launch {
                    ConfigManager.save(context, cfg)   // ✅ safe suspend call
                }
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Save Config")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Start Service Button
        Button(
            onClick = {
                val intent = Intent(context, BatteryTelemetryService::class.java)
                ContextCompat.startForegroundService(context, intent)
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text("Start BatteryTelemetryService")
        }

        // Stop Service Button
        Button(
            onClick = {
                val intent = Intent(context, BatteryTelemetryService::class.java)
                context.stopService(intent)
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text("Stop BatteryTelemetryService")
        }
    }
}
