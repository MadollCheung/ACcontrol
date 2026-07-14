package com.example.accontrol.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.accontrol.service.FloatingWindowService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, FloatingWindowService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
