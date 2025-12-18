/**
 * BootReceiver
 *
 * A broadcast receiver that listens for the device finishing its boot process.
 * When the system sends the BOOT_COMPLETED event, this receiver automatically
 * starts the BatteryTelemetryService as a foreground service so battery data
 * begins publishing right after the phone powers on.
 */

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
