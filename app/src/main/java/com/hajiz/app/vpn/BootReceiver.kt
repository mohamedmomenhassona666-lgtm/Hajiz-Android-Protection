package com.hajiz.app.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.hajiz.app.HajizApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // Settings are stored in credential-protected DataStore, so restore only
        // after the user has unlocked the device.
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = (context.applicationContext as HajizApplication)
                    .settingsRepository.settings.first()
                if (settings.protectionEnabled) {
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, HajizVpnService::class.java),
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}