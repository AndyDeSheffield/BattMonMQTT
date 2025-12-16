package com.grafton.battmonmqtt.monitor
import android.content.Context
class BatteryMonitor(private val context: Context) {
    fun readBatteryState(): BatteryState {
        // Use BatteryManager to get level, temp, charging
        return BatteryState(
            level = 100,
            charging = false,
            temperature = 25.0f,
            voltage = 4000
        )    }

    fun registerChargeStateListener(onChange: (Boolean) -> Unit) {
        // BroadcastReceiver for ACTION_BATTERY_CHANGED
    }
}

data class BatteryState(
    val level: Int,
    val charging: Boolean,
    val temperature: Float,
    val voltage: Int
)
