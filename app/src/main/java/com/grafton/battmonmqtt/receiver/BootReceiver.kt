package com.grafton.battmonmqtt.receiver
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.grafton.battmonmqtt.service.BatteryTelemetryService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, BatteryTelemetryService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
