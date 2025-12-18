/**
 * MainActivity
 *
 * The main configuration screen for the app, built with Jetpack Compose.
 * It lets the user enter and save MQTT connection details (host, port,
 * username, password, topic) and shows the device ID that will be used
 * for telemetry. From here the user can start or stop the BatteryTelemetryService,
 * with the UI updating automatically to reflect whether the service is running.
 * This activity is the entry point of the app and provides a simple way
 * to manage settings and control the background service.
 */


package com.grafton.battmonmqtt.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.grafton.battmonmqtt.R
import com.grafton.battmonmqtt.config.ConfigManager
import com.grafton.battmonmqtt.config.MqttConfig
import com.grafton.battmonmqtt.service.BatteryTelemetryService
import kotlinx.coroutines.launch
import java.security.MessageDigest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ConfigScreen()
        }
    }
}

// Helper to generate a short hashed device ID from ANDROID_ID
fun getShortDeviceId(context: Context): String {
    val androidId = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID
    ) ?: "unknown"

    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(androidId.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }.take(8)
}

@Composable
fun ConfigScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("1883") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    // Device ID (read-only)
    val deviceId = remember { getShortDeviceId(context) }

    // Track service running state
    var serviceRunning by remember { mutableStateOf(false) }

    // Broadcast receiver to update serviceRunning state
    val receiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    BatteryTelemetryService.ACTION_SERVICE_STARTED -> serviceRunning = true
                    BatteryTelemetryService.ACTION_SERVICE_STOPPED -> serviceRunning = false
                }
            }
        }
    }

    // Register/unregister receiver with lifecycle
    DisposableEffect(Unit) {
        val filter = IntentFilter().apply {
            addAction(BatteryTelemetryService.ACTION_SERVICE_STARTED)
            addAction(BatteryTelemetryService.ACTION_SERVICE_STOPPED)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                focusManager.clearFocus() // 👈 dismiss keyboard when tapping outside
            }
    ) {
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

            // Read-only Device ID field
            OutlinedTextField(
                value = deviceId,
                onValueChange = {},
                label = { Text("Device ID") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                enabled = false
            )

            Button(
                onClick = {
                    val cfg = MqttConfig(
                        host = host,
                        port = port.toIntOrNull() ?: 1883,
                        username = username,
                        password = password,
                        topic = topic,
                        deviceId = deviceId // save it in config
                    )
                    scope.launch {
                        ConfigManager.save(context, cfg)
                    }
                },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Save Config")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val intent = Intent(context, BatteryTelemetryService::class.java)
                    ContextCompat.startForegroundService(context, intent)
                },
                enabled = !serviceRunning,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text("Start BatteryTelemetryService")
            }

            Button(
                onClick = {
                    val intent = Intent(context, BatteryTelemetryService::class.java)
                    context.stopService(intent)
                },
                enabled = serviceRunning,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text("Stop BatteryTelemetryService")
            }
        }
    }
}
